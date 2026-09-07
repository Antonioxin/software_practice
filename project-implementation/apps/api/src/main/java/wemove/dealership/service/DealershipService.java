package wemove.dealership.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wemove.catalog.platform.CatalogPort;
import wemove.dealership.api.DealershipDtos.*;
import wemove.dealership.domain.*;
import wemove.dealership.platform.DealershipMetricsPort;
import wemove.dealership.repository.DealershipRepository;
import wemove.platform.*;
import wemove.platform.api.ApiException;
import wemove.platform.idempotency.IdempotencyExecutor;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class DealershipService implements DealershipMetricsPort {
    private static final Set<String> APPLICATION_STATES = Set.of("PENDING", "APPROVED", "REJECTED");
    private static final Set<String> INQUIRY_STATES = Set.of("NEW", "PROCESSING", "REPLIED", "CLOSED");
    private final DealershipRepository repository;
    private final CatalogPort catalog;
    private final DealerAccess access;
    private final IdempotencyExecutor idempotency;
    private final UnitOfWork work;
    private final IdentityPort identity;
    private final AuditPort audit;
    private final RateLimitPort rateLimit;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DealershipService(DealershipRepository repository, CatalogPort catalog, DealerAccess access,
            IdempotencyExecutor idempotency, UnitOfWork work, IdentityPort identity,
            AuditPort audit, RateLimitPort rateLimit) {
        this(repository, catalog, access, idempotency, work, identity, audit, rateLimit, Clock.systemUTC());
    }

    DealershipService(DealershipRepository repository, CatalogPort catalog, DealerAccess access,
            IdempotencyExecutor idempotency, UnitOfWork work, IdentityPort identity,
            AuditPort audit, RateLimitPort rateLimit, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.access = access;
        this.idempotency = idempotency;
        this.work = work;
        this.identity = identity;
        this.audit = audit;
        this.rateLimit = rateLimit;
        this.clock = clock;
    }

    public IdempotencyExecutor.Result<ApplicationView> createApplication(UUID actor, UUID key, ApplicationRequest request) {
        return idempotency.execute(actor, "dealership.application.create", key, "/dealer-applications",
                request, ApplicationView.class, 201, idempotency.hash(request), () -> {
                    rateLimit.consume(actor.toString(), RateLimitPort.Bucket.CUSTOMER_CONTACT_WRITES);
                    if (repository.applicationByUser(actor, true) != null || repository.companyByOwner(actor, true) != null)
                        throw DealershipRules.state("当前账户已有申请或合作记录，不能重复提交。");
                    Instant now = clock.instant();
                    DealerApplication application = new DealerApplication();
                    application.id = UUID.randomUUID();
                    application.applicationNumber = number("APP", now);
                    application.userId = actor;
                    application.status = "PENDING";
                    application.currentContentVersion = 1;
                    application.version = 1;
                    application.createdAt = now;
                    application.updatedAt = now;
                    repository.save(application);
                    repository.save(applicationVersion(application.id, 1, request, now));
                    audit(actor, "DEALER_APPLICATION_SUBMITTED", "DEALER_APPLICATION", application.id, null, now);
                    repository.flush();
                    return applicationView(application, false);
                });
    }

    @Transactional(readOnly = true)
    public PageResult<ApplicationView> ownApplications(UUID actor) {
        DealerApplication application = repository.applicationByUser(actor, false);
        List<ApplicationView> values = application == null ? List.of() : List.of(applicationView(application, false));
        return new PageResult<>(values, 1, 20, values.size());
    }

    @Transactional(readOnly = true)
    public ApplicationView ownApplication(UUID actor, UUID id) {
        DealerApplication application = repository.application(id, false);
        if (!application.userId.equals(actor)) throw DealershipRules.notFound();
        return applicationView(application, false);
    }

    public IdempotencyExecutor.Result<ApplicationView> resubmit(UUID actor, UUID id, UUID key, ResubmitRequest request) {
        return idempotency.execute(actor, "dealership.application.resubmit", key,
                "/dealer-applications/" + id + "/resubmit", request, ApplicationView.class, 200,
                idempotency.hash(request), () -> {
                    rateLimit.consume(actor.toString(), RateLimitPort.Bucket.CUSTOMER_CONTACT_WRITES);
                    DealerApplication application = repository.application(id, true);
                    if (!application.userId.equals(actor)) throw DealershipRules.notFound();
                    if (!"REJECTED".equals(application.status)) throw DealershipRules.state("只有已驳回申请可以修订重提。");
                    if (request.applicationVersion() != application.currentContentVersion)
                        throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "申请已有新版本，请刷新后重试。");
                    int next = application.currentContentVersion + 1;
                    Instant now = clock.instant();
                    repository.save(applicationVersion(application.id, next, request, now));
                    application.currentContentVersion = next;
                    application.status = "PENDING";
                    application.publicReason = null;
                    application.internalNote = null;
                    application.reviewedBy = null;
                    application.updatedAt = now;
                    audit(actor, "DEALER_APPLICATION_RESUBMITTED", "DEALER_APPLICATION", id, null, now);
                    repository.flush();
                    return applicationView(application, false);
                });
    }

    @Transactional(readOnly = true)
    public PageResult<ApplicationView> adminApplications(String status, int page, int pageSize) {
        page(page, pageSize);
        String normalized = state(status, APPLICATION_STATES);
        List<ApplicationView> values = repository.applications(normalized, (page - 1) * pageSize, pageSize).stream()
                .map(value -> applicationView(value, true)).toList();
        return new PageResult<>(values, page, pageSize, repository.countApplications(normalized));
    }

    @Transactional(readOnly = true)
    public ApplicationView adminApplication(UUID id) { return applicationView(repository.application(id, false), true); }

    public IdempotencyExecutor.Result<ApplicationView> review(UUID actor, UUID id, UUID key, ReviewRequest request) {
        return idempotency.execute(actor, "dealership.application.review", key,
                "/admin/dealer-applications/" + id + "/review", request, ApplicationView.class, 200,
                idempotency.hash(request), () -> {
                    DealerApplication application = repository.application(id, true);
                    if (!"PENDING".equals(application.status)) throw DealershipRules.state("该申请已经处理。");
                    if (request.applicationVersion() != application.currentContentVersion)
                        throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "申请已有新版本，请重新查看后审核。");
                    String decision = DealershipRules.text("decision", request.decision(), 1, 16).toUpperCase(Locale.ROOT);
                    if (!Set.of("APPROVE", "REJECT").contains(decision))
                        throw DealershipRules.invalid("decision", "审核决定必须为 APPROVE 或 REJECT。");
                    String reason = "REJECT".equals(decision)
                            ? DealershipRules.text("publicReason", request.publicReason(), 2, 500)
                            : DealershipRules.optional("publicReason", request.publicReason(), 500);
                    String note = DealershipRules.optional("internalNote", request.internalNote(), 2000);
                    Instant now = clock.instant();
                    DealerApplicationVersion version = repository.currentVersion(application);
                    if ("APPROVE".equals(decision)) approve(application, version, request.existingCompanyId(), now);
                    application.status = "APPROVE".equals(decision) ? "APPROVED" : "REJECTED";
                    application.publicReason = reason;
                    application.internalNote = note;
                    application.reviewedBy = actor;
                    application.updatedAt = now;
                    DealerReview review = new DealerReview();
                    review.id = UUID.randomUUID();
                    review.applicationId = application.id;
                    review.contentVersion = version.contentVersion;
                    review.decision = decision;
                    review.publicReason = reason;
                    review.internalNote = note;
                    review.reviewerId = actor;
                    review.createdAt = now;
                    repository.save(review);
                    audit(actor, "APPROVE".equals(decision) ? "DEALER_APPLICATION_APPROVED" : "DEALER_APPLICATION_REJECTED",
                            "DEALER_APPLICATION", id, reason, now);
                    repository.flush();
                    return applicationView(application, true);
                });
    }

    @Transactional(readOnly = true)
    public List<DealerProductView> dealerCatalog(UUID actor, Collection<UUID> ids) {
        access.requireActive(actor);
        List<CatalogPort.DealerProductProjection> products = ids == null
                ? catalog.getDealerProducts()
                : catalog.getDealerProducts(ids);
        return products.stream().map(this::productView).toList();
    }

    public IdempotencyExecutor.Result<InquiryView> createInquiry(UUID actor, UUID key, InquiryRequest request) {
        return idempotency.execute(actor, "dealership.inquiry.create", key, "/inquiries", request,
                InquiryView.class, 201, idempotency.hash(request), () -> {
                    rateLimit.consume(actor.toString(), RateLimitPort.Bucket.CUSTOMER_CONTACT_WRITES);
                    DealerCompany company = repository.companyByOwner(actor, true);
                    if (company == null || !"ACTIVE".equals(company.cooperationStatus))
                        throw new ApiException(HttpStatus.FORBIDDEN, "DEALER_ACCESS_REQUIRED", "当前账户没有有效经销合作资格。");
                    validateInquiryRequest(request);
                    List<UUID> requestedIds = request.items().stream().map(InquiryLineRequest::productId).toList();
                    Map<UUID, CatalogPort.DealerProductProjection> available = catalog.getDealerProducts(requestedIds).stream()
                            .collect(Collectors.toMap(CatalogPort.DealerProductProjection::id, value -> value));
                    if (available.size() != requestedIds.size())
                        throw DealershipRules.invalid("items", "询价包含不存在、已下架或未配置完整的经销商品。");
                    Instant now = clock.instant();
                    DealerInquiry inquiry = new DealerInquiry();
                    inquiry.id = UUID.randomUUID();
                    inquiry.inquiryNumber = number("INQ", now);
                    inquiry.companyId = company.id;
                    inquiry.userId = actor;
                    inquiry.status = "NEW";
                    inquiry.expectedDeliveryDate = request.expectedDeliveryDate();
                    inquiry.deliveryNotes = DealershipRules.optional("deliveryNotes", request.deliveryNotes(), 2000);
                    inquiry.purpose = DealershipRules.optional("purpose", request.purpose(), 2000);
                    inquiry.remark = DealershipRules.optional("remark", request.remark(), 2000);
                    inquiry.version = 1;
                    inquiry.createdAt = now;
                    inquiry.updatedAt = now;
                    repository.save(inquiry);
                    for (InquiryLineRequest line : request.items()) {
                        CatalogPort.DealerProductProjection product = available.get(line.productId());
                        if (line.quantity() < product.minInquiryQuantity())
                            throw DealershipRules.invalid("items", product.name() + " 最少询价数量为 " + product.minInquiryQuantity() + " 件。");
                        DealerInquiryItem item = new DealerInquiryItem();
                        item.id = UUID.randomUUID();
                        item.inquiryId = inquiry.id;
                        item.productId = product.id();
                        item.skuSnapshot = product.sku();
                        item.nameSnapshot = product.name();
                        item.referenceUnitPriceFenSnapshot = product.dealerReferenceUnitPriceFen();
                        item.minInquiryQuantitySnapshot = product.minInquiryQuantity();
                        item.quantity = line.quantity();
                        repository.save(item);
                    }
                    history(inquiry, "SUBMIT", null, "NEW", actor, null, now);
                    audit(actor, "DEALER_INQUIRY_SUBMITTED", "DEALER_INQUIRY", inquiry.id, null, now);
                    repository.flush();
                    return inquiryView(inquiry);
                });
    }

    @Transactional(readOnly = true)
    public PageResult<InquiryView> inquiries(UUID actor, boolean admin, String status, int page, int pageSize) {
        page(page, pageSize);
        String normalized = state(status, INQUIRY_STATES);
        UUID owner = admin ? null : actor;
        List<InquiryView> values = repository.inquiries(owner, normalized, (page - 1) * pageSize, pageSize).stream()
                .map(this::inquiryView).toList();
        return new PageResult<>(values, page, pageSize, repository.countInquiries(owner, normalized));
    }

    @Transactional(readOnly = true)
    public InquiryView inquiry(UUID actor, UUID id, boolean admin) {
        DealerInquiry inquiry = repository.inquiry(id, false);
        if (!admin && !inquiry.userId.equals(actor)) throw DealershipRules.notFound();
        return inquiryView(inquiry);
    }

    public IdempotencyExecutor.Result<InquiryView> startInquiry(UUID actor, UUID id, UUID key, long expectedVersion) {
        Map<String, Long> body = Map.of("expectedVersion", expectedVersion);
        return inquiryCommand(actor, id, key, "start", body, () -> {
            DealerInquiry inquiry = repository.inquiry(id, true);
            DealershipRules.version(expectedVersion, inquiry.version);
            if (!"NEW".equals(inquiry.status)) throw DealershipRules.state("只有待处理询价可以开始处理。");
            changeInquiry(inquiry, "START", "PROCESSING", actor, null);
            return inquiry;
        });
    }

    public IdempotencyExecutor.Result<InquiryView> replyInquiry(UUID actor, UUID id, UUID key, InquiryReplyRequest request) {
        return inquiryCommand(actor, id, key, "reply", request, () -> {
            DealerInquiry inquiry = repository.inquiry(id, true);
            DealershipRules.version(request.expectedVersion(), inquiry.version);
            if ("CLOSED".equals(inquiry.status)) throw DealershipRules.state("已关闭询价不能继续回复。");
            String body = DealershipRules.text("body", request.body(), 1, 2000);
            List<DealerInquiryItem> items = repository.inquiryItems(id);
            Map<UUID, DealerInquiryItem> itemMap = items.stream().collect(Collectors.toMap(value -> value.id, value -> value));
            Set<UUID> seen = new HashSet<>();
            for (InquiryReplyLine line : request.items() == null ? List.<InquiryReplyLine>of() : request.items()) {
                DealerInquiryItem item = itemMap.get(line.itemId());
                if (item == null || !seen.add(line.itemId())) throw DealershipRules.invalid("items", "回复行必须属于当前询价且不能重复。");
                item.replyReferenceUnitPriceFen = line.referenceUnitPriceFen();
                item.replyLeadTimeText = DealershipRules.optional("items.leadTimeText", line.leadTimeText(), 500);
            }
            inquiry.publicReply = inquiry.publicReply == null ? body : inquiry.publicReply + "\n\n" + body;
            changeInquiry(inquiry, "REPLY", "REPLIED", actor, null);
            return inquiry;
        });
    }

    public IdempotencyExecutor.Result<InquiryView> closeInquiry(UUID actor, UUID id, UUID key, VersionCommand request, boolean admin) {
        return inquiryCommand(actor, id, key, admin ? "admin-close" : "close", request, () -> {
            DealerInquiry inquiry = repository.inquiry(id, true);
            if (!admin && !inquiry.userId.equals(actor)) throw DealershipRules.notFound();
            DealershipRules.version(request.expectedVersion(), inquiry.version);
            if ("CLOSED".equals(inquiry.status)) throw DealershipRules.state("询价已经关闭。");
            inquiry.closeReason = DealershipRules.text("reason", request.reason(), 2, 500);
            changeInquiry(inquiry, "CLOSE", "CLOSED", actor, inquiry.closeReason);
            return inquiry;
        });
    }

    @Transactional(readOnly = true)
    public PageResult<CompanyView> companies(String status, int page, int pageSize) {
        page(page, pageSize);
        String normalized = state(status, Set.of("ACTIVE", "SUSPENDED"));
        return new PageResult<>(repository.companies(normalized, (page - 1) * pageSize, pageSize).stream().map(this::companyView).toList(),
                page, pageSize, repository.countCompanies(normalized));
    }

    @Transactional(readOnly = true)
    public CompanyView company(UUID id) { return companyView(repository.company(id, false)); }

    public CompanyView updateCompany(UUID actor, UUID id, CompanyUpdateRequest request) {
        return work.run(() -> {
            identity.lockActiveActor(actor);
            DealerCompany company = repository.company(id, true);
            DealershipRules.version(request.expectedVersion(), company.version);
            company.companyName = DealershipRules.text("companyName", request.companyName(), 2, 100);
            company.businessType = DealershipRules.businessType(request.businessType());
            company.countryOrRegion = DealershipRules.text("countryOrRegion", request.countryOrRegion(), 2, 100);
            company.city = DealershipRules.text("city", request.city(), 2, 100);
            company.contactName = DealershipRules.text("contactName", request.contactName(), 2, 50);
            company.phone = DealershipRules.phone(request.phone());
            company.cooperationEmail = DealershipRules.email(request.cooperationEmail());
            company.website = DealershipRules.website(request.website());
            company.internalNote = DealershipRules.optional("internalNote", request.internalNote(), 2000);
            company.updatedAt = clock.instant();
            audit(actor, "DEALER_COMPANY_UPDATED", "DEALER_COMPANY", id,
                    request.reason() + "；依据工单 " + request.basisTicketId(), company.updatedAt);
            repository.flush();
            return companyView(company);
        });
    }

    public IdempotencyExecutor.Result<CompanyView> companyStatus(UUID actor, UUID id, UUID key, VersionCommand request, boolean restore) {
        String action = restore ? "restore" : "suspend";
        return idempotency.execute(actor, "dealership.company." + action, key, "/admin/companies/" + id + "/" + action,
                request, CompanyView.class, 200, idempotency.hash(request), () -> {
                    DealerCompany company = repository.company(id, true);
                    DealershipRules.version(request.expectedVersion(), company.version);
                    String expected = restore ? "SUSPENDED" : "ACTIVE";
                    if (!expected.equals(company.cooperationStatus)) throw DealershipRules.state("企业合作状态不允许该操作。");
                    company.cooperationStatus = restore ? "ACTIVE" : "SUSPENDED";
                    company.updatedAt = clock.instant();
                    if (!restore) repository.unpublishCompanyChannels(company.id);
                    audit(actor, restore ? "DEALER_COMPANY_RESTORED" : "DEALER_COMPANY_SUSPENDED",
                            "DEALER_COMPANY", id, request.reason(), company.updatedAt);
                    repository.flush();
                    return companyView(company);
                });
    }

    @Transactional(readOnly = true)
    public PageResult<ChannelView> publicChannels(String country, String city, int page, int pageSize) {
        page(page, pageSize);
        String c = blank(country);
        String cityValue = blank(city);
        return new PageResult<>(repository.publicChannels(c, cityValue, (page - 1) * pageSize, pageSize).stream().map(this::channelView).toList(),
                page, pageSize, repository.countPublicChannels(c, cityValue));
    }

    @Override
    @Transactional(readOnly = true)
    public DealershipMetricsPort.Metrics read() {
        return new DealershipMetricsPort.Metrics(
                repository.countApplications("PENDING"), repository.countPendingInquiries());
    }

    @Transactional(readOnly = true)
    public ChannelView publicChannel(UUID id) {
        DealerChannel channel = repository.channel(id, false);
        if (!channel.published) throw DealershipRules.notFound();
        if (channel.companyId != null && !"ACTIVE".equals(repository.company(channel.companyId, false).cooperationStatus))
            throw DealershipRules.notFound();
        return channelView(channel);
    }

    @Transactional(readOnly = true)
    public PageResult<ChannelView> adminChannels(Boolean published, int page, int pageSize) {
        page(page, pageSize);
        return new PageResult<>(repository.channels(published, (page - 1) * pageSize, pageSize).stream().map(this::channelView).toList(),
                page, pageSize, repository.countChannels(published));
    }

    @Transactional(readOnly = true)
    public ChannelView adminChannel(UUID id) { return channelView(repository.channel(id, false)); }

    public IdempotencyExecutor.Result<ChannelView> createChannel(UUID actor, UUID key, ChannelRequest request) {
        return idempotency.execute(actor, "dealership.channel.create", key, "/admin/channels", request,
                ChannelView.class, 201, idempotency.hash(request), () -> {
                    DealerChannel channel = new DealerChannel();
                    channel.id = UUID.randomUUID();
                    applyChannel(channel, request, false);
                    channel.published = false;
                    channel.version = 1;
                    channel.createdAt = clock.instant();
                    channel.updatedAt = channel.createdAt;
                    repository.save(channel);
                    audit(actor, "DEALER_CHANNEL_CREATED", "DEALER_CHANNEL", channel.id, null, channel.createdAt);
                    repository.flush();
                    return channelView(channel);
                });
    }

    public ChannelView updateChannel(UUID actor, UUID id, ChannelRequest request) {
        return work.run(() -> {
            identity.lockActiveActor(actor);
            DealerChannel channel = repository.channel(id, true);
            if (request.expectedVersion() == null) throw DealershipRules.invalid("expectedVersion", "请提供当前记录版本。");
            DealershipRules.version(request.expectedVersion(), channel.version);
            applyChannel(channel, request, true);
            channel.updatedAt = clock.instant();
            audit(actor, "DEALER_CHANNEL_UPDATED", "DEALER_CHANNEL", id, null, channel.updatedAt);
            repository.flush();
            return channelView(channel);
        });
    }

    public IdempotencyExecutor.Result<ChannelView> channelStatus(UUID actor, UUID id, UUID key, VersionCommand request, boolean publish) {
        String action = publish ? "publish" : "unpublish";
        return idempotency.execute(actor, "dealership.channel." + action, key, "/admin/channels/" + id + "/" + action,
                request, ChannelView.class, 200, idempotency.hash(request), () -> {
                    DealerChannel channel = repository.channel(id, true);
                    DealershipRules.version(request.expectedVersion(), channel.version);
                    if (channel.published == publish) throw DealershipRules.state("渠道已经处于目标状态。");
                    if (publish && channel.companyId != null) requirePublishableCompany(channel.companyId);
                    channel.published = publish;
                    channel.updatedAt = clock.instant();
                    audit(actor, publish ? "DEALER_CHANNEL_PUBLISHED" : "DEALER_CHANNEL_UNPUBLISHED",
                            "DEALER_CHANNEL", id, request.reason(), channel.updatedAt);
                    repository.flush();
                    return channelView(channel);
                });
    }

    private IdempotencyExecutor.Result<InquiryView> inquiryCommand(UUID actor, UUID id, UUID key,
            String action, Object body, Supplier<DealerInquiry> command) {
        return idempotency.execute(actor, "dealership.inquiry." + action, key,
                "/inquiries/" + id + "/" + action, body, InquiryView.class, 200,
                idempotency.hash(body), () -> {
                    DealerInquiry inquiry = command.get();
                    audit(actor, "DEALER_INQUIRY_" + action.toUpperCase(Locale.ROOT).replace('-', '_'),
                            "DEALER_INQUIRY", id, inquiry.closeReason, clock.instant());
                    repository.flush();
                    return inquiryView(inquiry);
                });
    }

    private void changeInquiry(DealerInquiry inquiry, String action, String next, UUID actor, String reason) {
        String previous = inquiry.status;
        inquiry.status = next;
        inquiry.updatedAt = clock.instant();
        history(inquiry, action, previous, next, actor, reason, inquiry.updatedAt);
    }

    private void history(DealerInquiry inquiry, String action, String from, String to, UUID actor, String reason, Instant now) {
        DealerInquiryHistory history = new DealerInquiryHistory();
        history.id = UUID.randomUUID();
        history.inquiryId = inquiry.id;
        history.action = action;
        history.fromStatus = from;
        history.toStatus = to;
        history.inquiryVersion = inquiry.version;
        history.actorId = actor;
        history.reason = reason;
        history.createdAt = now;
        repository.save(history);
    }

    private void approve(DealerApplication application, DealerApplicationVersion version, UUID existingId, Instant now) {
        DealerCompany existing = repository.companyByOwner(application.userId, true);
        if (existingId != null) {
            DealerCompany selected = repository.company(existingId, true);
            if (!selected.ownerUserId.equals(application.userId))
                throw new ApiException(HttpStatus.CONFLICT, "COMPANY_OWNERSHIP_CONFLICT", "不能关联其他账户的企业记录。");
            if (existing != null && !existing.id.equals(selected.id)) throw DealershipRules.state("该账户已有其他企业记录。");
            if (!selected.sourceApplicationId.equals(application.id)) throw DealershipRules.state("现有企业并非来自当前申请。");
            selected.cooperationStatus = "ACTIVE";
            selected.updatedAt = now;
            return;
        }
        if (existing != null) throw DealershipRules.state("该账户已有企业记录。");
        DealerCompany company = new DealerCompany();
        company.id = UUID.randomUUID();
        company.ownerUserId = application.userId;
        company.sourceApplicationId = application.id;
        company.sourcePublicConsent = version.publicChannelConsent;
        company.companyName = version.companyName;
        company.businessType = version.businessType;
        company.countryOrRegion = version.countryOrRegion;
        company.city = version.city;
        company.contactName = version.contactName;
        company.phone = version.phone;
        company.cooperationEmail = version.cooperationEmail;
        company.website = version.website;
        company.cooperationStatus = "ACTIVE";
        company.version = 1;
        company.createdAt = now;
        company.updatedAt = now;
        repository.save(company);
    }

    private DealerApplicationVersion applicationVersion(UUID applicationId, int number, ApplicationRequest request, Instant now) {
        return applicationVersion(applicationId, number, request.companyName(), request.businessType(), request.countryOrRegion(),
                request.city(), request.contactName(), request.phone(), request.cooperationEmail(), request.businessChannels(),
                request.website(), request.cooperationIntent(), request.publicChannelConsent(), now);
    }

    private DealerApplicationVersion applicationVersion(UUID applicationId, int number, ResubmitRequest request, Instant now) {
        return applicationVersion(applicationId, number, request.companyName(), request.businessType(), request.countryOrRegion(),
                request.city(), request.contactName(), request.phone(), request.cooperationEmail(), request.businessChannels(),
                request.website(), request.cooperationIntent(), request.publicChannelConsent(), now);
    }

    private DealerApplicationVersion applicationVersion(UUID applicationId, int number, String companyName,
            String businessType, String country, String city, String contact, String phone, String email,
            String channels, String website, String intent, Boolean consent, Instant now) {
        DealerApplicationVersion version = new DealerApplicationVersion();
        version.id = UUID.randomUUID();
        version.applicationId = applicationId;
        version.contentVersion = number;
        version.companyName = DealershipRules.text("companyName", companyName, 2, 100);
        version.businessType = DealershipRules.businessType(businessType);
        version.countryOrRegion = DealershipRules.text("countryOrRegion", country, 2, 100);
        version.city = DealershipRules.text("city", city, 2, 100);
        version.contactName = DealershipRules.text("contactName", contact, 2, 50);
        version.phone = DealershipRules.phone(phone);
        version.cooperationEmail = DealershipRules.email(email);
        version.businessChannels = DealershipRules.text("businessChannels", channels, 1, 2000);
        version.website = DealershipRules.website(website);
        version.cooperationIntent = DealershipRules.text("cooperationIntent", intent, 10, 2000);
        version.publicChannelConsent = Boolean.TRUE.equals(consent);
        version.submittedAt = now;
        return version;
    }

    private void validateInquiryRequest(InquiryRequest request) {
        if (request.items() == null || request.items().isEmpty() || request.items().size() > 20)
            throw DealershipRules.invalid("items", "请选择 1—20 个经销商品。");
        Set<UUID> ids = new HashSet<>();
        for (InquiryLineRequest item : request.items())
            if (item == null || item.productId() == null || item.quantity() < 1 || item.quantity() > 9999 || !ids.add(item.productId()))
                throw DealershipRules.invalid("items", "商品不能重复，数量必须为 1—9999 的整数。");
        if (request.expectedDeliveryDate() != null
                && request.expectedDeliveryDate().isBefore(LocalDate.now(clock.withZone(ZoneId.of("Asia/Shanghai")))))
            throw DealershipRules.invalid("expectedDeliveryDate", "期望日期不能早于今天。");
    }

    private void applyChannel(DealerChannel channel, ChannelRequest request, boolean editing) {
        channel.name = DealershipRules.text("name", request.name(), 2, 100);
        channel.countryOrRegion = DealershipRules.text("countryOrRegion", request.countryOrRegion(), 2, 100);
        channel.city = DealershipRules.text("city", request.city(), 2, 100);
        channel.address = DealershipRules.text("address", request.address(), 5, 200);
        channel.phone = DealershipRules.phone(request.phone());
        channel.website = DealershipRules.website(request.website());
        if (request.companyId() != null) requirePublishableCompany(request.companyId());
        channel.companyId = request.companyId();
        if (editing && channel.published && channel.companyId != null) requirePublishableCompany(channel.companyId);
    }

    private DealerCompany requirePublishableCompany(UUID id) {
        DealerCompany company = repository.company(id, false);
        if (!company.sourcePublicConsent)
            throw new ApiException(HttpStatus.CONFLICT, "PUBLIC_CONSENT_REQUIRED", "该企业申请未授权公开渠道信息。");
        if (!"ACTIVE".equals(company.cooperationStatus))
            throw new ApiException(HttpStatus.CONFLICT, "COMPANY_SUSPENDED", "暂停合作企业的渠道不能发布。");
        return company;
    }

    private ApplicationView applicationView(DealerApplication value, boolean admin) {
        DealerApplicationVersion current = repository.currentVersion(value);
        boolean duplicate = repository.duplicateCompanies(current.companyName, current.countryOrRegion, current.city, value.userId) > 0;
        return new ApplicationView(value.id, value.applicationNumber, value.userId, value.status,
                value.currentContentVersion, value.version, value.publicReason, admin ? value.internalNote : null,
                duplicate,
                repository.applicationVersions(value.id).stream().map(v -> new ApplicationVersionView(v.contentVersion,
                        v.companyName, v.businessType, v.countryOrRegion, v.city, v.contactName, v.phone,
                        v.cooperationEmail, v.businessChannels, v.website, v.cooperationIntent,
                        v.publicChannelConsent, v.submittedAt)).toList(),
                repository.reviews(value.id).stream().map(r -> new ReviewView(r.contentVersion, r.decision,
                        r.publicReason, admin ? r.internalNote : null, admin ? r.reviewerId : null, r.createdAt)).toList(),
                value.createdAt, value.updatedAt);
    }

    private InquiryView inquiryView(DealerInquiry value) {
        return new InquiryView(value.id, value.inquiryNumber, value.companyId, value.userId, value.status,
                value.expectedDeliveryDate, value.deliveryNotes, value.purpose, value.remark, value.publicReply,
                value.closeReason, value.version,
                repository.inquiryItems(value.id).stream().map(i -> new InquiryItemView(i.id, i.productId,
                        i.skuSnapshot, i.nameSnapshot, i.referenceUnitPriceFenSnapshot,
                        i.minInquiryQuantitySnapshot, i.quantity, i.replyReferenceUnitPriceFen,
                        i.replyLeadTimeText)).toList(),
                repository.inquiryHistory(value.id).stream().map(h -> new InquiryHistoryView(h.action,
                        h.fromStatus, h.toStatus, h.inquiryVersion, h.actorId, h.reason, h.createdAt)).toList(),
                value.createdAt, value.updatedAt);
    }

    private DealerProductView productView(CatalogPort.DealerProductProjection value) {
        return new DealerProductView(value.id(), value.sku(), value.name(), value.retailUnitPriceFen(),
                value.dealerReferenceUnitPriceFen(), "CNY", value.minInquiryQuantity(),
                value.availableQuantity(), value.leadTimeText(), "参考单价，实际合作以回复为准");
    }

    private CompanyView companyView(DealerCompany c) {
        return new CompanyView(c.id, c.ownerUserId, c.sourceApplicationId, c.sourcePublicConsent,
                c.companyName, c.businessType, c.countryOrRegion, c.city, c.contactName, c.phone,
                c.cooperationEmail, c.website, c.cooperationStatus, c.internalNote, c.version,
                c.createdAt, c.updatedAt);
    }

    private ChannelView channelView(DealerChannel c) {
        return new ChannelView(c.id, c.name, c.countryOrRegion, c.city, c.address, c.phone,
                c.website, c.companyId, c.published, c.version, c.updatedAt);
    }

    private void audit(UUID actor, String action, String type, UUID id, String reason, Instant at) {
        audit.append(new AuditPort.AuditEvent(actor, action, type, id, "SUCCESS", reason, at));
    }

    private String number(String prefix, Instant now) {
        return prefix + "-" + DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(now)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String state(String value, Set<String> allowed) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw DealershipRules.invalid("status", "状态筛选值无效。");
        return normalized;
    }

    private void page(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 50)
            throw DealershipRules.invalid("page", "页码必须大于 0，单页数量为 1—50。");
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}

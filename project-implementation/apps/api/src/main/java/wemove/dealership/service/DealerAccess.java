package wemove.dealership.service;

import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wemove.dealership.domain.DealerCompany;
import wemove.dealership.repository.DealershipRepository;
import wemove.platform.DealerIdentityPort;
import wemove.platform.api.ApiException;
import java.util.UUID;

@Service
@Primary
public class DealerAccess implements DealerIdentityPort {
    private final DealershipRepository repository;

    public DealerAccess(DealershipRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public String derivedIdentity(UUID userId) {
        DealerCompany company = repository.companyByOwner(userId, false);
        return company != null && "ACTIVE".equals(company.cooperationStatus) ? "DEALER" : "USER";
    }

    @Transactional(readOnly = true)
    public CompanyContext requireActive(UUID userId) {
        DealerCompany company = repository.companyByOwner(userId, false);
        if (company == null || !"ACTIVE".equals(company.cooperationStatus))
            throw new ApiException(HttpStatus.FORBIDDEN, "DEALER_ACCESS_REQUIRED", "当前账户没有有效经销合作资格。");
        return new CompanyContext(company.id, company.ownerUserId, company.companyName);
    }

    public record CompanyContext(UUID companyId, UUID ownerUserId, String companyName) {}
}

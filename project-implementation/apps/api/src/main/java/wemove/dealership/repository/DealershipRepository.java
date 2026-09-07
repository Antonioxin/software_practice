package wemove.dealership.repository;

import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import wemove.dealership.domain.*;
import java.util.*;

@Repository
public class DealershipRepository {
    @PersistenceContext private EntityManager em;

    public void save(Object entity) { em.persist(entity); }
    public void flush() { em.flush(); }

    public DealerApplication application(UUID id, boolean lock) {
        DealerApplication value = lock
                ? em.find(DealerApplication.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(DealerApplication.class, id);
        if (value == null) throw wemove.dealership.service.DealershipRules.notFound();
        return value;
    }

    public DealerApplication applicationByUser(UUID userId, boolean lock) {
        var query = em.createQuery(
                "select a from DealerApplication a where a.userId=:user", DealerApplication.class)
                .setParameter("user", userId);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultStream().findFirst().orElse(null);
    }

    public List<DealerApplicationVersion> applicationVersions(UUID id) {
        return em.createQuery("select v from DealerApplicationVersion v where v.applicationId=:id order by v.contentVersion", DealerApplicationVersion.class)
                .setParameter("id", id).getResultList();
    }

    public DealerApplicationVersion currentVersion(DealerApplication application) {
        return em.createQuery("select v from DealerApplicationVersion v where v.applicationId=:id and v.contentVersion=:version", DealerApplicationVersion.class)
                .setParameter("id", application.id)
                .setParameter("version", application.currentContentVersion)
                .getSingleResult();
    }

    public List<DealerReview> reviews(UUID id) {
        return em.createQuery("select r from DealerReview r where r.applicationId=:id order by r.createdAt,r.id", DealerReview.class)
                .setParameter("id", id).getResultList();
    }

    public List<DealerApplication> applications(String status, int offset, int limit) {
        String where = status == null ? "" : " where a.status=:status";
        var q = em.createQuery("select a from DealerApplication a" + where + " order by a.updatedAt desc,a.id", DealerApplication.class);
        if (status != null) q.setParameter("status", status);
        return q.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long countApplications(String status) {
        String where = status == null ? "" : " where a.status=:status";
        var q = em.createQuery("select count(a) from DealerApplication a" + where, Long.class);
        if (status != null) q.setParameter("status", status);
        return q.getSingleResult();
    }

    public long duplicateCompanies(String name, String country, String city, UUID ownerToIgnore) {
        return em.createQuery("select count(c) from DealerCompany c where lower(c.companyName)=:name and lower(c.countryOrRegion)=:country and lower(c.city)=:city and c.ownerUserId<>:owner", Long.class)
                .setParameter("name", name.toLowerCase(Locale.ROOT))
                .setParameter("country", country.toLowerCase(Locale.ROOT))
                .setParameter("city", city.toLowerCase(Locale.ROOT))
                .setParameter("owner", ownerToIgnore)
                .getSingleResult();
    }

    public DealerCompany company(UUID id, boolean lock) {
        DealerCompany value = lock
                ? em.find(DealerCompany.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(DealerCompany.class, id);
        if (value == null) throw wemove.dealership.service.DealershipRules.notFound();
        return value;
    }

    public DealerCompany companyByOwner(UUID userId, boolean lock) {
        var query = em.createQuery("select c from DealerCompany c where c.ownerUserId=:user", DealerCompany.class)
                .setParameter("user", userId);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultStream().findFirst().orElse(null);
    }

    public List<DealerCompany> companies(String status, int offset, int limit) {
        String where = status == null ? "" : " where c.cooperationStatus=:status";
        var q = em.createQuery("select c from DealerCompany c" + where + " order by c.updatedAt desc,c.id", DealerCompany.class);
        if (status != null) q.setParameter("status", status);
        return q.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long countCompanies(String status) {
        String where = status == null ? "" : " where c.cooperationStatus=:status";
        var q = em.createQuery("select count(c) from DealerCompany c" + where, Long.class);
        if (status != null) q.setParameter("status", status);
        return q.getSingleResult();
    }

    public DealerChannel channel(UUID id, boolean lock) {
        DealerChannel value = lock
                ? em.find(DealerChannel.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(DealerChannel.class, id);
        if (value == null) throw wemove.dealership.service.DealershipRules.notFound();
        return value;
    }

    public List<DealerChannel> publicChannels(String country, String city, int offset, int limit) {
        String filters = (country == null ? "" : " and lower(c.countryOrRegion)=:country")
                + (city == null ? "" : " and lower(c.city)=:city");
        var q = em.createQuery("select c from DealerChannel c where c.published=true and (c.companyId is null or exists (select x.id from DealerCompany x where x.id=c.companyId and x.cooperationStatus='ACTIVE'))" + filters + " order by c.countryOrRegion,c.city,c.name,c.id", DealerChannel.class);
        if (country != null) q.setParameter("country", country.toLowerCase(Locale.ROOT));
        if (city != null) q.setParameter("city", city.toLowerCase(Locale.ROOT));
        return q.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long countPublicChannels(String country, String city) {
        String filters = (country == null ? "" : " and lower(c.countryOrRegion)=:country")
                + (city == null ? "" : " and lower(c.city)=:city");
        var q = em.createQuery("select count(c) from DealerChannel c where c.published=true and (c.companyId is null or exists (select x.id from DealerCompany x where x.id=c.companyId and x.cooperationStatus='ACTIVE'))" + filters, Long.class);
        if (country != null) q.setParameter("country", country.toLowerCase(Locale.ROOT));
        if (city != null) q.setParameter("city", city.toLowerCase(Locale.ROOT));
        return q.getSingleResult();
    }

    public List<DealerChannel> channels(Boolean published, int offset, int limit) {
        String where = published == null ? "" : " where c.published=:published";
        var q = em.createQuery("select c from DealerChannel c" + where + " order by c.updatedAt desc,c.id", DealerChannel.class);
        if (published != null) q.setParameter("published", published);
        return q.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long countChannels(Boolean published) {
        String where = published == null ? "" : " where c.published=:published";
        var q = em.createQuery("select count(c) from DealerChannel c" + where, Long.class);
        if (published != null) q.setParameter("published", published);
        return q.getSingleResult();
    }

    public int unpublishCompanyChannels(UUID companyId) {
        return em.createQuery("update DealerChannel c set c.published=false,c.version=c.version+1,c.updatedAt=CURRENT_TIMESTAMP where c.companyId=:company and c.published=true")
                .setParameter("company", companyId).executeUpdate();
    }

    public DealerInquiry inquiry(UUID id, boolean lock) {
        DealerInquiry value = lock
                ? em.find(DealerInquiry.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(DealerInquiry.class, id);
        if (value == null) throw wemove.dealership.service.DealershipRules.notFound();
        return value;
    }

    public List<DealerInquiryItem> inquiryItems(UUID id) {
        return em.createQuery("select i from DealerInquiryItem i where i.inquiryId=:id order by i.id", DealerInquiryItem.class)
                .setParameter("id", id).getResultList();
    }

    public List<DealerInquiryHistory> inquiryHistory(UUID id) {
        return em.createQuery("select h from DealerInquiryHistory h where h.inquiryId=:id order by h.createdAt,h.id", DealerInquiryHistory.class)
                .setParameter("id", id).getResultList();
    }

    public List<DealerInquiry> inquiries(UUID userId, String status, int offset, int limit) {
        String where = " where 1=1" + (userId == null ? "" : " and i.userId=:user") + (status == null ? "" : " and i.status=:status");
        var q = em.createQuery("select i from DealerInquiry i" + where + " order by i.updatedAt desc,i.id", DealerInquiry.class);
        if (userId != null) q.setParameter("user", userId);
        if (status != null) q.setParameter("status", status);
        return q.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long countInquiries(UUID userId, String status) {
        String where = " where 1=1" + (userId == null ? "" : " and i.userId=:user") + (status == null ? "" : " and i.status=:status");
        var q = em.createQuery("select count(i) from DealerInquiry i" + where, Long.class);
        if (userId != null) q.setParameter("user", userId);
        if (status != null) q.setParameter("status", status);
        return q.getSingleResult();
    }

    public long countPendingInquiries() {
        return em.createQuery("select count(i) from DealerInquiry i where i.status in ('NEW','PROCESSING')", Long.class)
                .getSingleResult();
    }
}

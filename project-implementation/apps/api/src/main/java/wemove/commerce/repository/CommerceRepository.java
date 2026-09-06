package wemove.commerce.repository;

import static wemove.commerce.domain.CommerceRules.notFound;

import jakarta.persistence.*;

import org.springframework.stereotype.Repository;

import wemove.commerce.domain.*;

import java.util.*;

@Repository
public class CommerceRepository {
    @PersistenceContext private EntityManager em;

    public void save(Object entity) {
        em.persist(entity);
    }

    public void remove(Object entity) {
        em.remove(entity);
    }

    public void flush() {
        em.flush();
    }

    public Cart cart(UUID user, boolean lock) {
        var q =
                em.createQuery("select c from CommerceCart c where c.userId=:user", Cart.class)
                        .setParameter("user", user);
        if (lock) q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return q.getResultStream().findFirst().orElse(null);
    }

    public List<CartItem> cartItems(UUID cart) {
        return em.createQuery(
                        "select i from CommerceCartItem i where i.cartId=:id order by i.productId",
                        CartItem.class)
                .setParameter("id", cart)
                .getResultList();
    }

    public CheckoutPreview preview(String hash, UUID user) {
        return em.createQuery(
                        "select p from CommerceCheckoutPreview p where p.tokenHash=:hash and"
                                + " p.userId=:user",
                        CheckoutPreview.class)
                .setParameter("hash", hash)
                .setParameter("user", user)
                .getResultStream()
                .findFirst()
                .orElseThrow(CommerceRules::notFound);
    }

    public Order order(UUID id, boolean lock) {
        Order result =
                lock
                        ? em.find(Order.class, id, LockModeType.PESSIMISTIC_WRITE)
                        : em.find(Order.class, id);
        if (result == null) throw notFound();
        return result;
    }

    public boolean consumed(UUID preview) {
        return em.createQuery(
                                "select count(o) from CommerceOrder o where o.previewId=:id",
                                Long.class)
                        .setParameter("id", preview)
                        .getSingleResult()
                > 0;
    }

    public List<OrderItem> items(UUID order) {
        return em.createQuery(
                        "select i from CommerceOrderItem i where i.orderId=:id order by"
                                + " i.productId",
                        OrderItem.class)
                .setParameter("id", order)
                .getResultList();
    }

    public List<PaymentAttempt> attempts(UUID order) {
        return em.createQuery(
                        "select p from CommercePaymentAttempt p where p.orderId=:id order by"
                                + " p.createdAt,p.id",
                        PaymentAttempt.class)
                .setParameter("id", order)
                .getResultList();
    }

    public List<Refund> refunds(UUID order) {
        return em.createQuery("select r from CommerceRefund r where r.orderId=:id", Refund.class)
                .setParameter("id", order)
                .getResultList();
    }

    public List<OrderHistory> history(UUID order) {
        return em.createQuery(
                        "select h from CommerceOrderHistory h where h.orderId=:id order by"
                                + " h.orderVersion",
                        OrderHistory.class)
                .setParameter("id", order)
                .getResultList();
    }

    public List<Order> list(
            UUID user,
            String status,
            java.time.Instant start,
            java.time.Instant end,
            int offset,
            int limit) {
        var q =
                em.createQuery(
                        "select o from CommerceOrder o"
                                + filter(user, status, start, end)
                                + " order by o.createdAt desc,o.id",
                        Order.class);
        bind(q, user, status, start, end);
        return q.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long count(UUID user, String status, java.time.Instant start, java.time.Instant end) {
        var q =
                em.createQuery(
                        "select count(o) from CommerceOrder o" + filter(user, status, start, end),
                        Long.class);
        bind(q, user, status, start, end);
        return q.getSingleResult();
    }

    private String filter(UUID u, String s, java.time.Instant a, java.time.Instant b) {
        return " where 1=1"
                + (u == null ? "" : " and o.userId=:user")
                + (s == null ? "" : " and o.status=:status")
                + (a == null ? "" : " and o.createdAt>=:start")
                + (b == null ? "" : " and o.createdAt<:end");
    }

    private void bind(Query q, UUID u, String s, java.time.Instant a, java.time.Instant b) {
        if (u != null) q.setParameter("user", u);
        if (s != null) q.setParameter("status", s);
        if (a != null) q.setParameter("start", a);
        if (b != null) q.setParameter("end", b);
    }
}

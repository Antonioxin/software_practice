package com.wemove.identity.platform;

public interface RateLimitPort {
    void consume(String subject, Bucket bucket);
    void recordLoginFailure(String normalizedEmail);
    void clearLoginFailures(String normalizedEmail);
    void assertLoginAllowed(String normalizedEmail);

    enum Bucket { REGISTER, CUSTOMER_CONTACT_WRITES }
}

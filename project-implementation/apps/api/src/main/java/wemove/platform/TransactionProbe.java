package wemove.platform;

import org.springframework.stereotype.Component;

/**
 * Inert in production. Tests may replace this bean; no HTTP/configuration switch enables faults.
 */
@Component
public class TransactionProbe {
    public void hit(String point) {}
}

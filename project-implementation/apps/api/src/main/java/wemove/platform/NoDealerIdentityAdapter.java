package wemove.platform;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NoDealerIdentityAdapter implements DealerIdentityPort {
    @Override public String derivedIdentity(UUID userId) { return "USER"; }
}

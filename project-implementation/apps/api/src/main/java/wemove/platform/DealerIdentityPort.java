package wemove.platform;

import java.util.UUID;

public interface DealerIdentityPort {
    String derivedIdentity(UUID userId);
}

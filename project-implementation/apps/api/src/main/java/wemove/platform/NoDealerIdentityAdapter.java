package wemove.platform;

import java.util.UUID;

public class NoDealerIdentityAdapter implements DealerIdentityPort {
    @Override public String derivedIdentity(UUID userId) { return "USER"; }
}

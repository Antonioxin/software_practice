package com.wemove.identity.platform;

import java.util.UUID;

public interface DealerIdentityPort {
    String derivedIdentity(UUID userId);
}

package com.wemove.identity.platform;

import com.wemove.identity.domain.*;
import java.util.UUID;

public record ActorContext(UUID actorId, String email, BaseRole baseRole, AccountStatus accountStatus) {}

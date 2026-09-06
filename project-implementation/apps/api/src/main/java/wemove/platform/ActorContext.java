package wemove.platform;

import wemove.identity.domain.*;
import java.util.UUID;

public record ActorContext(UUID actorId, String email, BaseRole baseRole, AccountStatus accountStatus) {}

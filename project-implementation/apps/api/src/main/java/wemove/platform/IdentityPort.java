package wemove.platform;

import org.springframework.security.core.Authentication;

public interface IdentityPort {
    ActorContext requireActiveActor(Authentication authentication);
}

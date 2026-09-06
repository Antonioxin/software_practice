package wemove.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import wemove.identity.domain.*;
import wemove.identity.repository.UserRepository;
import wemove.identity.security.UserPrincipal;
import wemove.platform.*;
import wemove.platform.api.ApiException;

@Service
public class IdentityService implements IdentityPort {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final UserRepository users;

    public IdentityService(UserRepository users) {
        this.users = users;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public ActorContext lockActiveActor(java.util.UUID actorId) {
        UserEntity current =
                users.findForUpdateById(actorId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.UNAUTHORIZED,
                                                "SESSION_INVALID",
                                                "会话已失效。"));
        entityManager.refresh(current, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (current.getAccountStatus() != AccountStatus.ACTIVE)
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", "账户已停用。");
        return new ActorContext(
                current.getId(),
                current.getEmailNormalized(),
                current.getBaseRole(),
                current.getAccountStatus());
    }

    @Override
    public ActorContext requireActiveActor(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请先登录。");
        }
        UserEntity current =
                users.findById(principal.id())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.UNAUTHORIZED,
                                                "SESSION_INVALID",
                                                "会话已失效。"));
        if (current.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", "账户已停用。");
        }
        return new ActorContext(
                current.getId(),
                current.getEmailNormalized(),
                current.getBaseRole(),
                current.getAccountStatus());
    }
}

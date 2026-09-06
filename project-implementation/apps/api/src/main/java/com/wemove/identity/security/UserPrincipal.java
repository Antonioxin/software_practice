package com.wemove.identity.security;

import com.wemove.identity.domain.*;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(UUID id, String email, String passwordHash, String nickname,
                            BaseRole role, AccountStatus status, long version) implements UserDetails {
    public static UserPrincipal from(UserEntity user) {
        return new UserPrincipal(user.getId(), user.getEmailNormalized(), user.getPasswordHash(),
            user.getNickname(), user.getBaseRole(), user.getAccountStatus(), user.getVersion());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return status == AccountStatus.ACTIVE; }
}

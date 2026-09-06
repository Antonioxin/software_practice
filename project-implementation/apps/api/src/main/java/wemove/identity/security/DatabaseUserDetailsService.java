package wemove.identity.security;

import wemove.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalized = email == null ? "" : email.strip().toLowerCase(java.util.Locale.ROOT);
        return users.findByEmailNormalized(normalized).map(UserPrincipal::from)
            .orElseThrow(() -> new UsernameNotFoundException("邮箱或密码错误"));
    }
}

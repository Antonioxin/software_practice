package wemove.identity.api;

import wemove.platform.api.ApiException;
import wemove.platform.api.ApiEnvelope;
import wemove.config.WemoveProperties;
import wemove.platform.*;
import wemove.identity.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserAccountService accounts;
    private final IdentityPort identity;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository contexts;
    private final RateLimitPort limits;
    private final WemoveProperties properties;

    public AuthController(UserAccountService accounts, IdentityPort identity,
                          AuthenticationManager authenticationManager,
                          HttpSessionSecurityContextRepository contexts,
                          RateLimitPort limits, WemoveProperties properties) {
        this.accounts = accounts;
        this.identity = identity;
        this.authenticationManager = authenticationManager;
        this.contexts = contexts;
        this.limits = limits;
        this.properties = properties;
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiEnvelope<Dtos.CsrfPayload>> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiEnvelope.of(new Dtos.CsrfPayload(token.getToken(), token.getHeaderName())));
    }

    @GetMapping("/registration-policy")
    public ApiEnvelope<Dtos.RegistrationPolicy> policy() {
        return ApiEnvelope.of(new Dtos.RegistrationPolicy(
            "我确认已年满 18 岁，本账户用于成年消费或采购活动。",
            properties.registration().termsVersion(), "/terms",
            properties.registration().privacyVersion(), "/privacy"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiEnvelope<Dtos.Actor>> register(@Valid @RequestBody Dtos.RegisterRequest request,
                                                             HttpServletRequest servletRequest) {
        limits.consume(servletRequest.getRemoteAddr(), RateLimitPort.Bucket.REGISTER);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
            .body(ApiEnvelope.of(accounts.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiEnvelope<Dtos.Actor>> login(@Valid @RequestBody Dtos.LoginRequest request,
                                                          HttpServletRequest servletRequest,
                                                          HttpServletResponse servletResponse) {
        String email = UserAccountService.normalizeEmail(request.email());
        limits.assertLoginAllowed(email);
        try {
            Authentication result = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
            if (servletRequest.getSession(false) == null) servletRequest.getSession(true);
            else servletRequest.changeSessionId();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(result);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, servletRequest, servletResponse);
            limits.clearLoginFailures(email);
            ActorContext actor = identity.requireActiveActor(result);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.of(accounts.actor(actor.actorId())));
        } catch (DisabledException ex) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "账户已停用，如有疑问请联系客服。");
        } catch (AuthenticationException ex) {
            limits.recordLoginFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "邮箱或密码错误。");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiEnvelope<Dtos.Actor>> me(Authentication authentication) {
        ActorContext actor = identity.requireActiveActor(authentication);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiEnvelope.of(accounts.actor(actor.actorId())));
    }
}

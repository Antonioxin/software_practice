package com.wemove.identity.api;

import com.wemove.identity.platform.*;
import com.wemove.identity.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final IdentityPort identity;
    private final UserAccountService accounts;

    public AccountController(IdentityPort identity, UserAccountService accounts) {
        this.identity = identity;
        this.accounts = accounts;
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiEnvelope<Dtos.Actor>> update(@Valid @RequestBody Dtos.ProfileRequest request,
                                                           Authentication authentication) {
        ActorContext actor = identity.requireActiveActor(authentication);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiEnvelope.of(accounts.updateProfile(actor.actorId(), request)));
    }
}

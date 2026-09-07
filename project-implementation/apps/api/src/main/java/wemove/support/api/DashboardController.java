package wemove.support.api;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import wemove.identity.domain.BaseRole;
import wemove.platform.ActorContext;
import wemove.platform.IdentityPort;
import wemove.platform.api.ApiEnvelope;
import wemove.platform.api.ApiException;
import wemove.support.api.SupportDtos.DashboardSnapshot;
import wemove.support.service.DashboardService;

import java.time.Instant;

/** FR-32 后台总览（管理员）。 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {
    private final IdentityPort identity;
    private final DashboardService dashboard;

    public DashboardController(IdentityPort identity, DashboardService dashboard) {
        this.identity = identity;
        this.dashboard = dashboard;
    }

    @GetMapping
    public ResponseEntity<ApiEnvelope<DashboardSnapshot>> read(
            Authentication a,
            @RequestParam Instant start,
            @RequestParam Instant end) {
        requireAdmin(a);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiEnvelope.of(dashboard.read(start, end)));
    }

    private void requireAdmin(Authentication a) {
        ActorContext actor = identity.requireActiveActor(a);
        if (actor.baseRole() != BaseRole.ADMIN)
            throw new ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "FORBIDDEN", "您无权访问管理后台。");
    }
}

package wemove.operations.audit;

import jakarta.persistence.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.request.*;

import wemove.platform.AuditPort;

@Component
public class DatabaseAuditAdapter implements AuditPort {
    @PersistenceContext private EntityManager entities;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(AuditEvent event) {
        String requestId = event.requestId();
        if (requestId == null
                && RequestContextHolder.getRequestAttributes()
                        instanceof ServletRequestAttributes attrs)
            requestId = (String) attrs.getRequest().getAttribute("requestId");
        entities.persist(new AuditRecord(event, requestId));
        entities.flush();
    }
}

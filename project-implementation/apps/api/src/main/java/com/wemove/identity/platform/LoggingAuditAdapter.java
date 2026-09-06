package com.wemove.identity.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingAuditAdapter implements AuditPort {
    private static final Logger log = LoggerFactory.getLogger(LoggingAuditAdapter.class);
    @Override public void append(AuditEvent event) {
        log.info("AUDIT action={} objectType={} objectId={} actorId={} result={}",
            event.action(), event.objectType(), event.objectId(), event.actorId(), event.result());
    }
}

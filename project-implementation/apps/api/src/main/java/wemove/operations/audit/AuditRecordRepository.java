package wemove.operations.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/** F 拥有的审计检索入口；只读，无修改/删除方法。 */
public interface AuditRecordRepository
        extends JpaRepository<AuditRecord, UUID>, JpaSpecificationExecutor<AuditRecord> {}

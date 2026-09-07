package wemove.support.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** 内部备注：仅管理员读写，绝不出现在用户响应中。 */
@Entity(name = "SupportInternalNote")
@Table(name = "support_internal_notes")
public class SupportInternalNoteEntity {
    @Id public UUID id;
    public UUID ticketId;
    @Column(length = 2000) public String content;
    public UUID actorId;
    public Instant createdAt;
}

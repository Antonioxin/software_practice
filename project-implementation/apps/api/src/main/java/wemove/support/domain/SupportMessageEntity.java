package wemove.support.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** 公开对话：用户补充与管理员公开回复，append-only。 */
@Entity(name = "SupportMessage")
@Table(name = "support_messages")
public class SupportMessageEntity {
    @Id public UUID id;
    public UUID ticketId;
    @Column(length = 20) public String kind;
    @Column(length = 2000) public String content;
    public UUID actorId;
    public Instant createdAt;
}

package wemove.content;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="content_site_settings")
public class SiteSettings {
    static final UUID ID = UUID.fromString("e0000000-0000-4000-8000-000000000001");
    @Id UUID id = ID;
    @Column(name="brand_name", nullable=false, length=100) String brandName = "WEMOVE";
    @Column(name="brand_description", nullable=false, columnDefinition="LONGTEXT") String brandDescription = "<p>让陪伴发生，让成长可见。WEMOVE 与家庭一起，在运动和游戏中发现成长的可能。</p>";
    @Column(name="contact_email", nullable=false, length=250) String contactEmail = "";
    @Column(name="contact_phone", nullable=false, length=50) String contactPhone = "";
    @Column(name="contact_address", nullable=false, length=250) String contactAddress = "";
    @Column(name="logo_media_id") UUID logoMediaId;
    @Column(name="terms_text", nullable=false, columnDefinition="LONGTEXT") String termsText = "<p>请在使用 WEMOVE 服务前阅读服务说明。注册信息应真实准确；课程演示订单不构成真实支付。</p>";
    @Column(name="terms_version", nullable=false, length=80) String termsVersion = "2026-09-05";
    @Column(name="privacy_text", nullable=false, columnDefinition="LONGTEXT") String privacyText = "<p>我们仅为账户、订单与客户服务处理必要信息。请勿在课程演示站点提交真实敏感资料。</p>";
    @Column(name="privacy_version", nullable=false, length=80) String privacyVersion = "2026-09-05";
    @Version long version;
    public SiteSettings() {}
}

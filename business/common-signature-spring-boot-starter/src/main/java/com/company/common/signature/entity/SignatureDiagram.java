package com.company.common.signature.entity;

import com.company.common.jpa.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * 電子簽名資料。
 *
 * <p>每筆簽名透過 {@code ownerType + ownerId} 組合鍵與業務資料關聯，
 * 例如 ownerType="CASE", ownerId=123 表示「案件 123 的簽名」。</p>
 *
 * <p>{@code content} 儲存 Fabric.js Canvas 的 JSON 序列化，可在前端完整還原畫布狀態。</p>
 */
@Getter
@Setter
@Entity
@Table(name = "SIGNATURE_DIAGRAM", indexes = {
        @Index(name = "idx_sign_owner", columnList = "OWNER_TYPE, OWNER_ID", unique = true)
})
public class SignatureDiagram extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OBJID")
    private Long id;

    /** 業務表名（如 CASE, ORDER, CONTRACT）。 */
    @Column(name = "OWNER_TYPE", nullable = false, length = 50)
    private String ownerType;

    /** 業務資料 ID。 */
    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    /** Fabric.js Canvas JSON 序列化內容。 */
    @Column(name = "CONTENT", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    /** 關聯的附件 ID（簽名截圖 PNG）。 */
    @Column(name = "ATTACHMENT_ID")
    private Long attachmentId;

    @Version
    @Column(name = "VERSION")
    private Integer version;
}

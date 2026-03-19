package com.company.common.signature.dto;

import java.time.LocalDateTime;

/**
 * 簽名查詢回應。
 *
 * @param id           簽名 ID
 * @param ownerType    業務表名
 * @param ownerId      業務資料 ID
 * @param content      Fabric.js Canvas JSON
 * @param attachmentId 簽名截圖附件 ID（可用於下載預覽圖）
 * @param createdBy    建立者
 * @param createdDate  建立時間
 * @param lastModifiedBy  最後修改者
 * @param lastModifiedDate 最後修改時間
 */
public record SignatureResponse(
        Long id,
        String ownerType,
        Long ownerId,
        String content,
        Long attachmentId,
        String createdBy,
        LocalDateTime createdDate,
        String lastModifiedBy,
        LocalDateTime lastModifiedDate
) {
}

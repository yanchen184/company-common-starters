package com.company.common.signature.dto;

/**
 * 簽名存檔請求。
 *
 * @param ownerType 業務表名（如 CASE, ORDER）
 * @param ownerId   業務資料 ID
 * @param json      Fabric.js Canvas JSON 序列化
 */
public record SignatureSaveRequest(
        String ownerType,
        Long ownerId,
        String json
) {
}

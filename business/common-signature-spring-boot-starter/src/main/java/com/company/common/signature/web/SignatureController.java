package com.company.common.signature.web;

import com.company.common.signature.dto.SignatureResponse;
import com.company.common.signature.dto.SignatureSaveRequest;
import com.company.common.signature.service.SignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 電子簽名 REST API。
 *
 * <p>提供簽名的存檔、查詢、刪除功能。
 * 前端透過 Canvas (Fabric.js) 繪製簽名後，將 JSON + 截圖 PNG 一起送到此 API。</p>
 */
@RestController
@RequestMapping("${common.signature.api-prefix:/api/signatures}")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    /**
     * 儲存簽名（新增或更新）。
     *
     * <p>接收 multipart/form-data：
     * <ul>
     *   <li>ownerType — 業務表名</li>
     *   <li>ownerId — 業務資料 ID</li>
     *   <li>json — Fabric.js Canvas JSON</li>
     *   <li>image — Canvas 截圖 PNG（可選）</li>
     * </ul>
     */
    @PostMapping
    public ResponseEntity<SignatureResponse> save(
            @RequestParam String ownerType,
            @RequestParam Long ownerId,
            @RequestParam String json,
            @RequestPart(required = false) MultipartFile image) throws IOException {

        SignatureSaveRequest request = new SignatureSaveRequest(ownerType, ownerId, json);
        SignatureResponse response = signatureService.save(request, image);
        return ResponseEntity.ok(response);
    }

    /**
     * 查詢簽名。
     *
     * @param ownerType 業務表名
     * @param ownerId   業務資料 ID
     */
    @GetMapping
    public ResponseEntity<SignatureResponse> find(
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {

        SignatureResponse response = signatureService.findByOwner(ownerType, ownerId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 刪除簽名（連同附件）。
     *
     * @param ownerType 業務表名
     * @param ownerId   業務資料 ID
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {

        signatureService.delete(ownerType, ownerId);
        return ResponseEntity.noContent().build();
    }
}

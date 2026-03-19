package com.company.common.signature.service;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.core.model.AttachmentOwnerRef;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import com.company.common.signature.dto.SignatureResponse;
import com.company.common.signature.dto.SignatureSaveRequest;
import com.company.common.signature.entity.SignatureDiagram;
import com.company.common.signature.repository.SignatureDiagramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 電子簽名核心服務。
 *
 * <p>負責簽名資料的 CRUD，同時管理簽名截圖附件。
 * 簽名截圖以 {@code SIGNATURE} 作為附件的 ownerType。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SignatureService {

    /** 附件 ownerType 前綴，與業務 ownerType 組合成唯一標識。 */
    private static final String ATTACHMENT_OWNER_PREFIX = "SIGN_";

    private final SignatureDiagramRepository repository;
    private final AttachmentService attachmentService;

    /**
     * 儲存簽名（新增或更新）。
     *
     * <p>流程：
     * <ol>
     *   <li>依 ownerType + ownerId 查詢是否已有簽名，沒有就新建</li>
     *   <li>更新 Canvas JSON</li>
     *   <li>如果有圖片，先軟刪除舊附件，再上傳新附件</li>
     * </ol>
     *
     * @param request 簽名資料（ownerType, ownerId, json）
     * @param image   Canvas 截圖 PNG（可為 null）
     * @return 簽名回應
     */
    @Transactional
    public SignatureResponse save(SignatureSaveRequest request, MultipartFile image) throws IOException {
        SignatureDiagram diagram = repository
                .findByOwnerTypeAndOwnerId(request.ownerType(), request.ownerId())
                .orElseGet(() -> {
                    SignatureDiagram sd = new SignatureDiagram();
                    sd.setOwnerType(request.ownerType());
                    sd.setOwnerId(request.ownerId());
                    return sd;
                });

        diagram.setContent(request.json());

        // 處理簽名截圖附件
        if (image != null && !image.isEmpty()) {
            // 軟刪除舊附件
            if (diagram.getAttachmentId() != null) {
                try {
                    attachmentService.softDelete(diagram.getAttachmentId());
                } catch (Exception e) {
                    log.warn("刪除舊簽名附件失敗: attachmentId={}", diagram.getAttachmentId(), e);
                }
            }

            // 上傳新附件
            String attachOwnerType = ATTACHMENT_OWNER_PREFIX + request.ownerType();
            AttachmentUploadRequest uploadRequest = new AttachmentUploadRequest(
                    attachOwnerType,
                    request.ownerId(),
                    image.getOriginalFilename(),
                    "簽名圖片",
                    image.getInputStream(),
                    image.getSize(),
                    image.getContentType()
            );
            AttachmentUploadResponse uploadResponse = attachmentService.upload(uploadRequest);
            diagram.setAttachmentId(uploadResponse.id());
        }

        diagram = repository.save(diagram);
        log.info("簽名儲存完成: id={}, ownerType={}, ownerId={}",
                diagram.getId(), diagram.getOwnerType(), diagram.getOwnerId());

        return toResponse(diagram);
    }

    /**
     * 查詢簽名。
     *
     * @param ownerType 業務表名
     * @param ownerId   業務資料 ID
     * @return 簽名回應，若不存在回傳 null
     */
    @Transactional(readOnly = true)
    public SignatureResponse findByOwner(String ownerType, Long ownerId) {
        return repository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * 刪除簽名（連同附件一起軟刪除）。
     *
     * @param ownerType 業務表名
     * @param ownerId   業務資料 ID
     */
    @Transactional
    public void delete(String ownerType, Long ownerId) {
        repository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .ifPresent(diagram -> {
                    // 軟刪除附件
                    if (diagram.getAttachmentId() != null) {
                        try {
                            attachmentService.softDelete(diagram.getAttachmentId());
                        } catch (Exception e) {
                            log.warn("刪除簽名附件失敗: attachmentId={}", diagram.getAttachmentId(), e);
                        }
                    }
                    repository.delete(diagram);
                    log.info("簽名已刪除: ownerType={}, ownerId={}", ownerType, ownerId);
                });
    }

    /**
     * 查詢某業務類型下所有簽名的附件列表。
     *
     * @param ownerType 業務表名
     * @param ownerId   業務資料 ID
     * @return 附件列表
     */
    @Transactional(readOnly = true)
    public List<AttachmentUploadResponse> getAttachments(String ownerType, Long ownerId) {
        String attachOwnerType = ATTACHMENT_OWNER_PREFIX + ownerType;
        return attachmentService.findByOwner(new AttachmentOwnerRef(attachOwnerType, ownerId));
    }

    private SignatureResponse toResponse(SignatureDiagram diagram) {
        return new SignatureResponse(
                diagram.getId(),
                diagram.getOwnerType(),
                diagram.getOwnerId(),
                diagram.getContent(),
                diagram.getAttachmentId(),
                diagram.getCreatedBy(),
                diagram.getCreatedDate(),
                diagram.getLastModifiedBy(),
                diagram.getLastModifiedDate()
        );
    }
}

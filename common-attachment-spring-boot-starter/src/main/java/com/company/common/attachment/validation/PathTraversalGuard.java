package com.company.common.attachment.validation;

import com.company.common.attachment.core.model.AttachmentUploadRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class PathTraversalGuard implements AttachmentValidator {

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".sh", ".ps1", ".vbs", ".js", ".jar", ".war", ".class",
            ".msi", ".dll", ".so", ".py", ".rb", ".php", ".asp", ".aspx", ".jsp", ".cgi",
            ".com", ".scr", ".pif", ".hta", ".wsf", ".mjs"
    );

    @Override
    public void validate(AttachmentUploadRequest request) {
        String filename = request.originalFilename();
        if (filename == null || filename.isBlank()) {
            throw new AttachmentValidationException("檔案名稱不得為空");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new AttachmentValidationException(
                    "檔案名稱包含不合法字元（路徑穿越風險）: " + filename
            );
        }

        // 檢查 null bytes
        if (filename.indexOf('\0') >= 0) {
            throw new AttachmentValidationException("檔案名稱包含 null byte");
        }

        // 檢查 trailing dot（Windows 檔名攻擊）
        String trimmedFilename = filename.trim();
        if (trimmedFilename.endsWith(".")) {
            throw new AttachmentValidationException("檔案名稱不得以句點結尾: " + filename);
        }

        // 副檔名 blocklist（檢查所有副檔名，防止雙重副檔名攻擊如 malware.exe.pdf）
        String lowerFilename = trimmedFilename.toLowerCase();
        for (String blocked : BLOCKED_EXTENSIONS) {
            if (lowerFilename.endsWith(blocked) || lowerFilename.contains(blocked + ".")) {
                throw new AttachmentValidationException("不允許的副檔名: " + filename);
            }
        }

        log.debug("路徑穿越與副檔名驗證通過: {}", filename);
    }

    @Override
    public int getOrder() {
        return 50;
    }
}

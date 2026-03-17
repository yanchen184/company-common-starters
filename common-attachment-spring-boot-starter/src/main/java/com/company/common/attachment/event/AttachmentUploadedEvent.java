package com.company.common.attachment.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AttachmentUploadedEvent extends ApplicationEvent {

    private final Long attachmentId;
    private final String storedFilename;
    private final String mimeType;
    private final long fileSize;

    public AttachmentUploadedEvent(Object source, Long attachmentId,
                                   String storedFilename, String mimeType, long fileSize) {
        super(source);
        this.attachmentId = attachmentId;
        this.storedFilename = storedFilename;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
    }
}

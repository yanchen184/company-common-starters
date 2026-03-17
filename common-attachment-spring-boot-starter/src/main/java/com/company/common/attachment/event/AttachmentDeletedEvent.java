package com.company.common.attachment.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AttachmentDeletedEvent extends ApplicationEvent {

    private final Long attachmentId;
    private final String storedFilename;

    public AttachmentDeletedEvent(Object source, Long attachmentId, String storedFilename) {
        super(source);
        this.attachmentId = attachmentId;
        this.storedFilename = storedFilename;
    }
}

package com.ticketing.attachment.model;

import com.fasterxml.jackson.annotation.*;
import java.time.LocalDateTime;

public class AttachmentRow {
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long id;
    public String uploadToken;
    @JsonIgnore public Long uploaderId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long ticketId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long ticketEventId;
    @JsonIgnore public String bucketName;
    @JsonIgnore public String objectKey;
    public String originalFilename;
    public String mediaType;
    public Long sizeBytes;
    @JsonIgnore public String sha256;
    public Integer imageWidth;
    public Integer imageHeight;
    @JsonIgnore public String storageState;
    public LocalDateTime expiresAt;
    public LocalDateTime createdAt;
    public LocalDateTime boundAt;
}

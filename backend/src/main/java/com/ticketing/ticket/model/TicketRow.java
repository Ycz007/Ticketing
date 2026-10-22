package com.ticketing.ticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class TicketRow {
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long id;
    public String requestId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long requesterId;
    public String requesterName;
    public String originalText;
    @com.fasterxml.jackson.annotation.JsonIgnore public String creationDigest;
    public String initialCategory;
    public String category;
    public String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long assigneeId;
    public String assigneeName;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long relatedTicketId;
    public String resolutionCode;
    public Long version;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime completedAt;
}

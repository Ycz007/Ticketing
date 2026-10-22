package com.ticketing.notification.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class NotificationRow {
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long ticketId;
    public String requestId;
    public String type;
    public LocalDateTime createdAt;
    public LocalDateTime readAt;
}

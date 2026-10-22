package com.ticketing.ticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class EventRow {
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long id;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long ticketId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long actorId;
    public String actorName;
    public String eventType;
    public String content;
    public String changes;
    public LocalDateTime createdAt;
}

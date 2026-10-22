package com.ticketing.ticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class UserPermissionRow {
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long ticketId;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long userId;
    public String permissionName;
    public String permissionCode;
    public String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING) public Long reviewedBy;
    public LocalDateTime reviewedAt;
}

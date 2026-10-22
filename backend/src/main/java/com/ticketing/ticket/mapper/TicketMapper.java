package com.ticketing.ticket.mapper;

import com.ticketing.ticket.model.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TicketMapper {
    TicketRow find(@Param("id") Long id);
    TicketRow lock(@Param("id") Long id);
    TicketRow findRequest(@Param("requestId") String requestId);
    int insert(TicketRow ticket);
    int update(TicketRow ticket);
    List<TicketRow> list(@Param("userId") Long userId, @Param("scope") String scope,
                         @Param("status") String status, @Param("category") String category,
                         @Param("requestId") String requestId, @Param("unassigned") boolean unassigned,
                         @Param("offset") int offset, @Param("limit") int limit);
    long count(@Param("userId") Long userId, @Param("scope") String scope,
               @Param("status") String status, @Param("category") String category,
               @Param("requestId") String requestId, @Param("unassigned") boolean unassigned);
    List<HandlerOption> handlers();
    HandlerOption handler(@Param("id") Long id);
    List<Long> coordinators();
    List<PermissionOption> permissions();
    PermissionOption permission(@Param("code") String code);
    UserPermissionRow permissionRequest(@Param("ticketId") Long ticketId);
    int insertPermissionRequest(@Param("ticketId") Long ticketId, @Param("userId") Long userId, @Param("permissionCode") String permissionCode);
    int insertLegacyPermissionRequest(@Param("ticketId") Long ticketId, @Param("userId") Long userId, @Param("permissionName") String permissionName);
    int reviewPermissionRequest(@Param("ticketId") Long ticketId, @Param("status") String status, @Param("reviewedBy") Long reviewedBy, @Param("permissionCode") String permissionCode);
    int addEvent(EventRow event);
    List<EventRow> events(@Param("ticketId") Long ticketId);
}

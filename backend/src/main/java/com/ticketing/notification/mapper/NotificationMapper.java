package com.ticketing.notification.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
    long countUnread(@Param("userId") Long userId);
    java.util.List<com.ticketing.notification.model.NotificationRow> list(@Param("userId") Long userId,
            @Param("offset") int offset, @Param("limit") int limit);
    int markRead(@Param("userId") Long userId, @Param("id") Long id);
    int insert(@Param("recipientId") Long recipientId, @Param("ticketId") Long ticketId,
               @Param("eventId") Long eventId, @Param("type") String type);
    int invalidate(@Param("ticketId") Long ticketId, @Param("userId") Long userId);
}

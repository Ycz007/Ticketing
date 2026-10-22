package com.ticketing.attachment.mapper;

import com.ticketing.attachment.model.AttachmentRow;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AttachmentMapper {
    AttachmentRow find(@Param("id") Long id);
    AttachmentRow lockToken(@Param("token") String token);
    int insert(AttachmentRow row);
    int ready(@Param("id") Long id);
    int pending(@Param("id") Long id);
    int bind(@Param("id") Long id, @Param("ticketId") Long ticketId, @Param("eventId") Long eventId);
    int countBound(@Param("ticketId") Long ticketId);
    int countTemporary(@Param("userId") Long userId);
    Long lockUploader(@Param("id") Long id);
    List<AttachmentRow> list(@Param("ticketId") Long ticketId);
    List<AttachmentRow> expired();
    int claimExpired(@Param("id") Long id);
    int deletePending(@Param("id") Long id);
}

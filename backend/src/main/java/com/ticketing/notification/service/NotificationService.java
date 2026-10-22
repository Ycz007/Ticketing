package com.ticketing.notification.service;

import com.ticketing.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationMapper mapper;
    public NotificationService(NotificationMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) { return mapper.countUnread(userId); }

    @Transactional(readOnly = true)
    public java.util.List<com.ticketing.notification.model.NotificationRow> list(Long userId, int page) {
        if (page < 1 || page > 100000) throw com.ticketing.common.api.BusinessException.invalid("页码不合法");
        return mapper.list(userId, (page - 1) * 20, 20);
    }

    @Transactional
    public void read(Long userId, Long id) {
        if (mapper.markRead(userId, id) == 0) throw com.ticketing.common.api.BusinessException.notFound();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void publish(Long ticketId, Long eventId, String type, Long actorId, java.util.Collection<Long> recipients) {
        recipients.stream().filter(java.util.Objects::nonNull).distinct().filter(id -> !id.equals(actorId))
                .forEach(id -> mapper.insert(id, ticketId, eventId, type));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void invalidate(Long ticketId, Long oldHandler) {
        if (oldHandler != null) mapper.invalidate(ticketId, oldHandler);
    }
}

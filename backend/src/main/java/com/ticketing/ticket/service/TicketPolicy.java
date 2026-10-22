package com.ticketing.ticket.service;

import com.ticketing.auth.security.SessionUser;
import com.ticketing.common.api.BusinessException;
import com.ticketing.ticket.model.TicketRow;
import java.util.Objects;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class TicketPolicy {
    public boolean has(SessionUser user, String permission) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(permission));
    }
    public void require(SessionUser user, String permission) {
        if (!has(user, permission)) throw new AccessDeniedException("Not permitted");
    }
    public boolean canRead(SessionUser user, TicketRow ticket) {
        return ticket != null && (has(user, "ticket:read:all")
                || has(user, "ticket:read:own") && Objects.equals(user.getId(), ticket.requesterId)
                || has(user, "ticket:read:assigned") && Objects.equals(user.getId(), ticket.assigneeId));
    }
    public void readable(SessionUser user, TicketRow ticket) {
        if (!canRead(user, ticket)) throw BusinessException.notFound();
    }
    public void writable(SessionUser user, TicketRow ticket, Long version) {
        readable(user, ticket);
        if ("COMPLETED".equals(ticket.status)) throw BusinessException.conflict("已完成工单只读");
        if (!Objects.equals(ticket.version, version)) throw BusinessException.conflict("工单已更新，请刷新后再操作");
    }
    public void handler(SessionUser user, TicketRow ticket) {
        if (!Objects.equals(user.getId(), ticket.assigneeId)) throw new AccessDeniedException("Not current handler");
    }
    public void requester(SessionUser user, TicketRow ticket) {
        if (!Objects.equals(user.getId(), ticket.requesterId)) throw new AccessDeniedException("Not requester");
    }
    public void state(TicketRow ticket, String... allowed) {
        for (String state : allowed) if (state.equals(ticket.status)) return;
        throw BusinessException.conflict("当前状态不支持此操作");
    }
}

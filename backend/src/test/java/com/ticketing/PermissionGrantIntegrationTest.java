package com.ticketing;

import com.ticketing.auth.mapper.AccountMapper;
import com.ticketing.auth.security.*;
import com.ticketing.common.api.BusinessException;
import com.ticketing.ticket.domain.TicketCategory;
import com.ticketing.ticket.dto.TicketRequests.*;
import com.ticketing.ticket.service.TicketService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real MySQL check. All users, applications, grants and notifications roll back. */
@SpringBootTest(properties={"app.demo.enabled=false"})
@EnabledIfEnvironmentVariable(named="TICKETING_DB_INTEGRATION",matches="true")
@Transactional
class PermissionGrantIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired TicketService tickets;
    @Autowired AccountMapper accounts;
    @Autowired com.ticketing.notification.service.NotificationService notifications;
    @Autowired DatabaseUserDetailsService users;

    SessionUser account(String suffix,String role) {
        String username="grantcheck_"+suffix+UUID.randomUUID().toString().replace("-","").substring(0,8);
        jdbc.update("INSERT INTO users(username,display_name,password_hash) VALUES(?,?,?)",username,"授权流程验证","unused-test-hash");
        Long id=jdbc.queryForObject("SELECT id FROM users WHERE username=?",Long.class,username);
        accounts.assignRole(id,role);
        return (SessionUser)users.loadUserByUsername(username);
    }
    @Test void approveGrantsRealAccessAndRefreshesExistingSessionWhileRejectionDoesNotGrant() throws Exception {
        var requester=account("requester","REQUESTER");
        var other=account("other","REQUESTER");
        var coordinator=account("coordinator","COORDINATOR");
        assertTrue(coordinator.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ticket:access:approve")));
        var unrelated=tickets.create(other,new Create(UUID.randomUUID().toString(),"其他人的工单",TicketCategory.USAGE_QUESTION,null,List.of()));
        assertThrows(BusinessException.class,() -> tickets.detail(requester,unrelated.id));
        assertFalse(accounts.findPermissions(requester.getId()).contains("ticket:read:all"));
        var application=tickets.create(requester,new Create(UUID.randomUUID().toString(),"申请查看全部工单",TicketCategory.ACCESS_REQUEST,null,List.of(),"ticket:read:all"));
        assertFalse(accounts.findPermissions(requester.getId()).contains("ticket:read:all"));
        tickets.approve(coordinator,application.id,new Result(application.version,"APPROVED","同意授予查看权限"));
        assertTrue(accounts.findPermissions(requester.getId()).contains("ticket:read:all"));
        assertEquals("APPROVED",jdbc.queryForObject("SELECT status FROM user_permission WHERE ticket_id=?",String.class,application.id));
        assertEquals("COMPLETED",tickets.detail(requester,application.id).ticket().status);
        try {
            var context=SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(requester,null,requester.getAuthorities()));
            SecurityContextHolder.setContext(context);
            new PermissionRefreshFilter(users).doFilter(new MockHttpServletRequest(),new MockHttpServletResponse(),(req,res) -> {
                var refreshed=(SessionUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                assertEquals(unrelated.id,tickets.detail(refreshed,unrelated.id).ticket().id);
                assertEquals("ALL",tickets.scope(refreshed));
            });
        } finally { SecurityContextHolder.clearContext(); }
        var rejected=tickets.create(other,new Create(UUID.randomUUID().toString(),"申请分派权限",TicketCategory.ACCESS_REQUEST,null,List.of(),"ticket:assign"));
        tickets.approve(coordinator,rejected.id,new Result(rejected.version,"REJECTED","无需分派权限"));
        assertFalse(accounts.findPermissions(other.getId()).contains("ticket:assign"));
        assertEquals("COMPLETED",tickets.detail(other,rejected.id).ticket().status);
        assertTrue(notifications.countUnread(requester.getId())>0);
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE recipient_id=? AND ticket_id=? AND type='TICKET_COMPLETED'",Integer.class,requester.getId(),application.id));
    }
}

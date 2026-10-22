package com.ticketing.ticket;

import com.ticketing.common.api.BusinessException;
import com.ticketing.ticket.service.TicketPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static com.ticketing.support.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class TicketPolicyTest {
    final TicketPolicy policy=new TicketPolicy();
    @Test void requesterOnlyReadsOwnTicket() {
        assertTrue(policy.canRead(user(1,"ticket:read:own"),ticket()));
        assertFalse(policy.canRead(user(3,"ticket:read:own"),ticket()));
    }
    @Test void coordinatorReadsAllButMustBeHandlerToProcess() {
        var coordinator=user(4,"ticket:read:all","ticket:start");
        assertTrue(policy.canRead(coordinator,ticket()));
        assertThrows(AccessDeniedException.class,() -> policy.handler(coordinator,ticket()));
    }
    @Test void supportLosesAccessAfterTransfer() {
        var row=ticket(); var support=user(2,"ticket:read:assigned");
        assertTrue(policy.canRead(support,row)); row.assigneeId=3L;
        assertFalse(policy.canRead(support,row));
    }
    @Test void unreadableIsNotFound() {
        var error=assertThrows(BusinessException.class,() -> policy.readable(user(9,"ticket:read:own"),ticket()));
        assertEquals(404,error.status());
    }
    @Test void completedIsImmutable() {
        var row=ticket(); row.status="COMPLETED";
        assertEquals(409,assertThrows(BusinessException.class,() -> policy.writable(user(1,"ticket:read:own"),row,0L)).status());
    }
    @Test void staleVersionIsRejected() {
        assertEquals(409,assertThrows(BusinessException.class,() -> policy.writable(user(1,"ticket:read:own"),ticket(),8L)).status());
    }
    @Test void permissionAndDataScopeAreBothRequired() {
        assertThrows(AccessDeniedException.class,() -> policy.require(user(1,"ticket:read:own"),"ticket:assign"));
    }
}

package com.ticketing.support;

import com.ticketing.auth.model.Account;
import com.ticketing.auth.security.SessionUser;
import com.ticketing.ticket.model.TicketRow;
import java.util.Arrays;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public final class TestFixtures {
    private TestFixtures() { }
    public static SessionUser user(long id,String... permissions) {
        return new SessionUser(new Account(id,"user"+id,"用户"+id,"hash",true),
                Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList());
    }
    public static TicketRow ticket() {
        var row=new TicketRow(); row.id=10L; row.requestId="R001"; row.requesterId=1L;
        row.assigneeId=2L; row.originalText="导出失败"; row.initialCategory="SYSTEM_FAILURE";
        row.category="SYSTEM_FAILURE"; row.status="PENDING"; row.version=0L;
        return row;
    }
    public static PlatformTransactionManager transactions() {
        var manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(inv -> new SimpleTransactionStatus());
        return manager;
    }
}

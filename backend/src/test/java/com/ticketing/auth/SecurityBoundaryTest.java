package com.ticketing.auth;

import com.ticketing.auth.controller.AuthController;
import com.ticketing.auth.model.Account;
import com.ticketing.auth.security.SecurityConfig;
import com.ticketing.auth.security.SessionUser;
import com.ticketing.common.api.HealthController;
import com.ticketing.notification.controller.NotificationController;
import com.ticketing.notification.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HealthController.class, AuthController.class, NotificationController.class})
@Import({SecurityConfig.class, SecurityBoundaryTest.TestUsers.class})
class SecurityBoundaryTest {
    @Autowired MockMvc mvc;
    @MockitoBean NotificationService notifications;

    private static SessionUser principal(boolean permitted) {
        var account = new Account(7L, permitted ? "alice" : "restricted", "Alice", new BCryptPasswordEncoder().encode("test-password"), true);
        return new SessionUser(account, permitted ? List.of(new SimpleGrantedAuthority("notification:read")) : List.of());
    }

    @TestConfiguration
    static class TestUsers {
        @Bean UserDetailsService users() {
            return username -> {
                if (username.equals("alice")) return principal(true);
                if (username.equals("restricted")) return principal(false);
                throw new UsernameNotFoundException("not found");
            };
        }
    }

    @Test void livenessIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void anonymousCannotReadIdentity() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
    @Test void loginRequiresCsrf() throws Exception {
        mvc.perform(post("/api/auth/login").param("username", "alice").param("password", "test-password"))
                .andExpect(status().isForbidden());
    }
    @Test void validLoginCreatesSession() throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(csrf())
                .param("username", "alice").param("password", "test-password"))
                .andExpect(status().isNoContent()).andReturn();
        mvc.perform(get("/api/me").session((org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("7"));
    }
    @Test void badPasswordDoesNotLogin() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("username", "alice").param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }
    @Test void notificationQueryIgnoresSpoofedUserId() throws Exception {
        when(notifications.countUnread(7L)).thenReturn(2L);
        mvc.perform(get("/api/notifications/unread-count?userId=99").with(user(principal(true))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(2));
        verify(notifications).countUnread(7L);
        verifyNoMoreInteractions(notifications);
    }
    @Test void missingPermissionIsDenied() throws Exception {
        mvc.perform(get("/api/notifications/unread-count").with(user(principal(false))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notifications);
    }

    @Test void existingSessionReceivesNewGrantOnNextRequestWithoutLogin() throws Exception {
        var stale=new SessionUser(new Account(7L,"alice","Alice","hash",true),List.of());
        mvc.perform(get("/api/notifications/unread-count").with(user(stale)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me").with(user(stale)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorities[0]").value("notification:read"));
    }
    @Test void staleSessionPermissionDoesNotOverrideCurrentDatabaseDenial() throws Exception {
        var stale=new SessionUser(new Account(7L,"restricted","Alice","hash",true),List.of(new SimpleGrantedAuthority("notification:read")));
        mvc.perform(get("/api/notifications/unread-count").with(user(stale)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(notifications);
    }
}

package com.ticketing.auth.controller;

import com.ticketing.auth.security.SessionUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @GetMapping("/api/auth/csrf")
    public CsrfView csrf(CsrfToken token) { return new CsrfView(token.getHeaderName(), token.getToken()); }

    @GetMapping("/api/me")
    public Me me(@AuthenticationPrincipal SessionUser user) {
        return new Me(user.getId().toString(), user.getUsername(), user.getDisplayName(),
                user.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

    public record CsrfView(String headerName, String token) { }
    public record Me(String id, String username, String displayName, List<String> authorities) { }
}

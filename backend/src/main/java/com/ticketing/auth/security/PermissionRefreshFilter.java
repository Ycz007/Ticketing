package com.ticketing.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reload effective permissions for existing sessions before authorization. */
public class PermissionRefreshFilter extends OncePerRequestFilter {
    private final UserDetailsService users;
    public PermissionRefreshFilter(UserDetailsService users) { this.users=users; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if (authentication!=null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof SessionUser previous) {
            try {
                var fresh=users.loadUserByUsername(previous.getUsername());
                var context=SecurityContextHolder.createEmptyContext();
                if (fresh instanceof SessionUser current && current.isEnabled() && current.getId().equals(previous.getId())) {
                    var updated=UsernamePasswordAuthenticationToken.authenticated(current,null,current.getAuthorities());
                    updated.setDetails(authentication.getDetails());
                    context.setAuthentication(updated);
                }
                SecurityContextHolder.setContext(context);
            } catch (UsernameNotFoundException ex) {
                SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
            }
        }
        chain.doFilter(request,response);
    }
}

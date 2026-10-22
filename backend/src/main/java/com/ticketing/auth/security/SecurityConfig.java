package com.ticketing.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.common.api.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper json, org.springframework.security.core.userdetails.UserDetailsService users) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/health", "/api/auth/csrf", "/api/auth/login").permitAll()
                    .anyRequest().authenticated())
            .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((req, res, ex) -> error(res, json, 401, "UNAUTHENTICATED", "请先登录"))
                    .accessDeniedHandler((req, res, ex) -> error(res, json, 403, "FORBIDDEN", "无权执行操作或安全令牌失效")))
            .formLogin(login -> login.loginProcessingUrl("/api/auth/login")
                    .successHandler((req, res, auth) -> res.setStatus(204))
                    .failureHandler((req, res, ex) -> error(res, json, 401, "LOGIN_FAILED", "账号或密码错误")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID", "XSRF-TOKEN")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
        http.addFilterBefore(new PermissionRefreshFilter(users),org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        return http.build();
    }

    private static void error(HttpServletResponse response, ObjectMapper json, int status,
                              String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getOutputStream(), new ApiError(code, message));
    }
}

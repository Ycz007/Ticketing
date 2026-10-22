package com.ticketing.auth.security;

import com.ticketing.auth.model.Account;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class SessionUser extends User {
    private final Long id;
    private final String displayName;

    public SessionUser(Account account, Collection<? extends GrantedAuthority> authorities) {
        super(account.username(), account.passwordHash(), account.enabled(), true, true, true, authorities);
        this.id = account.id();
        this.displayName = account.displayName();
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
}

package com.ticketing.auth.security;

import com.ticketing.auth.mapper.AccountMapper;
import java.util.ArrayList;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final AccountMapper mapper;
    public DatabaseUserDetailsService(AccountMapper mapper) { this.mapper = mapper; }

    @Override
    public UserDetails loadUserByUsername(String username) {
        var account = mapper.findByUsername(username);
        if (account == null) throw new UsernameNotFoundException("账号或密码错误");
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        mapper.findRoles(account.id()).forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        mapper.findPermissions(account.id()).forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
        return new SessionUser(account, authorities);
    }
}

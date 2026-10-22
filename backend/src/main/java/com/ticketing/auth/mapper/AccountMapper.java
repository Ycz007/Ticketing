package com.ticketing.auth.mapper;

import com.ticketing.auth.model.Account;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {
    Account findByUsername(@Param("username") String username);
    List<String> findRoles(@Param("userId") Long userId);
    List<String> findPermissions(@Param("userId") Long userId);
    int insertDemoUser(@Param("username") String username, @Param("displayName") String displayName,
                       @Param("passwordHash") String passwordHash);
    int assignRole(@Param("userId") Long userId, @Param("role") String role);
}

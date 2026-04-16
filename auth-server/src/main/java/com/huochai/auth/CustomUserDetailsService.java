package com.huochai.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库加载用户信息
        // 这里使用内存模拟
        if ("admin".equals(username)) {
            return User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .authorities(List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("SCOPE_message.read"),
                    new SimpleGrantedAuthority("SCOPE_message.write")
                ))
                .build();
        }

        if ("user".equals(username)) {
            return User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .authorities(List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("SCOPE_message.read")
                ))
                .build();
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
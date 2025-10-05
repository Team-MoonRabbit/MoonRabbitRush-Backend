package com.moon.rabbit.global.security.details;
import com.moon.rabbit.global.security.jwt.dto.UserCredential;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class AuthDetails implements UserDetails {
    private final UserCredential credential;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // null 대신 빈 리스트 반환
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return credential.email();
    }
}

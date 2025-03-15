package com.moemoe.core.security;

import com.moemoe.mongo.constant.UserRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MoeUser implements UserDetails {
    private String id;
    private String email;
    private UserRole role;

    public static MoeUser of(String id, String email, UserRole userRole) {
        return new MoeUser(id, email, userRole);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return this.id;
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}

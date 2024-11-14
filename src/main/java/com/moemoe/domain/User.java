package com.moemoe.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Document(collection = "users")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class User implements UserDetails {
    @Id
    private String id;
    private UserRole role;
    private String socialId;
    private String name;
    @Indexed(unique = true)
    private String email;
    private String gender;
    private String birthyear;
    private String birthday;
    private String thumbnailImageUrl;
    private String profileImageUrl;

    @Builder
    public User(String socialId,
                String name,
                UserRole role,
                String email,
                String gender,
                String birthyear,
                String birthday,
                String thumbnailImageUrl,
                String profileImageUrl) {
        this.socialId = socialId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.gender = gender;
        this.birthyear = birthyear;
        this.birthday = birthday;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.profileImageUrl = profileImageUrl;
    }


    // Spring Security UserDetails Area
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("user"));
    }

    @Override
    public String getPassword() {
        return String.valueOf(socialId);
    }

    @Override
    public String getUsername() {
        return email;
    }
}

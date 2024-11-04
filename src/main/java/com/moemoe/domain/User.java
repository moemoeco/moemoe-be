package com.moemoe.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "users")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    @Id
    private String id;
    private long socialId;
    private String name;
    @Indexed(unique = true)
    private String email;
    private String gender;
    private String birthyear;
    private String birthday;
    private String thumbnailImageUrl;
    private String profileImageUrl;

    @Builder
    public User(long socialId,
                String name,
                String email,
                String gender,
                String birthyear,
                String birthday,
                String thumbnailImageUrl,
                String profileImageUrl) {
        this.socialId = socialId;
        this.name = name;
        this.email = email;
        this.gender = gender;
        this.birthyear = birthyear;
        this.birthday = birthday;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.profileImageUrl = profileImageUrl;
    }
}

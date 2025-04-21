package com.moemoe.mongo.entity;

import com.moemoe.mongo.constant.UserRole;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "users")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserEntity extends BaseTimeEntity {
    @Id
    @Getter(AccessLevel.NONE)
    private ObjectId id;
    private UserRole role;
    private String socialId;
    private String name;
    @Indexed(unique = true)
    private String email;
    private String gender;
    private String birthyear;
    private String birthday;
    private String profileImageUrl;

    @Builder
    public UserEntity(String socialId,
                      String name,
                      UserRole role,
                      String email,
                      String gender,
                      String birthyear,
                      String birthday,
                      String profileImageUrl) {
        this.socialId = socialId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.gender = gender;
        this.birthyear = birthyear;
        this.birthday = birthday;
        this.profileImageUrl = profileImageUrl;
    }

    public String getId() {
        return id.toHexString();
    }
}

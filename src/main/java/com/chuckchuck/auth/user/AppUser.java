package com.chuckchuck.auth.user;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String googleId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImageUrl;

    @Column(nullable = false)
    private String role;

    protected AppUser() {
    }

    private AppUser(String googleId, String email, String name, String profileImageUrl) {
        this.id = UUID.randomUUID().toString();
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.role = "USER";
    }

    public static AppUser create(String googleId, String email, String name, String profileImageUrl) {
        return new AppUser(googleId, email, name, profileImageUrl);
    }

    public void updateProfile(String email, String name, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public String getId() {
        return id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getRole() {
        return role;
    }
}

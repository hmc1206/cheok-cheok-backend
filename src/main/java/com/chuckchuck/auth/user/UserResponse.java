package com.chuckchuck.auth.user;

public record UserResponse(
        String userId,
        String name,
        String email,
        String profileImageUrl
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getProfileImageUrl()
        );
    }
}

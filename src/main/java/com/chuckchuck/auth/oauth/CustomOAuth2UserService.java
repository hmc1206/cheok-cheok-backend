package com.chuckchuck.auth.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chuckchuck.auth.user.AppUser;
import com.chuckchuck.auth.user.AppUserRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AppUserRepository userRepository;

    public CustomOAuth2UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return saveOrUpdate(super.loadUser(userRequest));
    }

    OAuthUserPrincipal saveOrUpdate(OAuth2User oauthUser) {
        String googleId = requiredAttribute(oauthUser, "sub");
        String email = requiredAttribute(oauthUser, "email");
        String name = requiredAttribute(oauthUser, "name");
        String picture = oauthUser.getAttribute("picture");

        AppUser existing = userRepository.findByGoogleId(googleId).orElse(null);
        boolean isNewUser = existing == null;
        AppUser user = isNewUser
                ? AppUser.create(googleId, email, name, picture)
                : existing;
        if (!isNewUser) {
            user.updateProfile(email, name, picture);
        }
        userRepository.save(user);
        return new OAuthUserPrincipal(user, oauthUser.getAttributes(), isNewUser);
    }

    private String requiredAttribute(OAuth2User user, String name) {
        String value = user.getAttribute(name);
        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"));
        }
        return value;
    }
}

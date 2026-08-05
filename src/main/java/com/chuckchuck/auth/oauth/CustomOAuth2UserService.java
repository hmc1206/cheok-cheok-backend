package com.chuckchuck.auth.oauth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chuckchuck.auth.user.AppUser;
import com.chuckchuck.auth.user.AppUserRepository;

@Service
public class CustomOAuth2UserService extends OidcUserService {

    private final AppUserRepository userRepository;

    public CustomOAuth2UserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);

        return saveOrUpdate(oidcUser);
    }

    private OAuthUserPrincipal saveOrUpdate(OidcUser oidcUser) {

        String googleId = requiredAttribute(oidcUser, "sub");
        String email = requiredAttribute(oidcUser, "email");
        String name = requiredAttribute(oidcUser, "name");
        String picture = oidcUser.getAttribute("picture");

        AppUser existing = userRepository
                .findByGoogleId(googleId)
                .orElse(null);

        boolean isNewUser = existing == null;

        AppUser user = isNewUser
                ? AppUser.create(googleId, email, name, picture)
                : existing;

        if (!isNewUser) {
            user.updateProfile(email, name, picture);
        }

        userRepository.save(user);

        return new OAuthUserPrincipal(
                user,
                oidcUser,
                isNewUser
        );
    }

    private String requiredAttribute(
            OidcUser user,
            String name
    ) {
        String value = user.getAttribute(name);

        if (value == null || value.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info")
            );
        }

        return value;
    }
}
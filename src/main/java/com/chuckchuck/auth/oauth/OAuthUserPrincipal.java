package com.chuckchuck.auth.oauth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.chuckchuck.auth.user.AppUser;

public class OAuthUserPrincipal implements OidcUser {

    private final AppUser user;
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;
    private final boolean newUser;

    public OAuthUserPrincipal(
            AppUser user,
            OidcUser oidcUser,
            boolean newUser
    ) {
        this.user = user;
        this.attributes = Map.copyOf(oidcUser.getAttributes());
        this.idToken = oidcUser.getIdToken();
        this.userInfo = oidcUser.getUserInfo();
        this.newUser = newUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );
    }

    @Override
    public String getName() {
        // JWT의 subject로 사용할 AppUser.id
        return user.getId();
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    public boolean isNewUser() {
        return newUser;
    }
}
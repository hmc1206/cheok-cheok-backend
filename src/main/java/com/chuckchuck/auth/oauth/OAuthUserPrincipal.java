package com.chuckchuck.auth.oauth;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.chuckchuck.auth.user.AppUser;

public class OAuthUserPrincipal implements OAuth2User {
    private final AppUser user;
    private final Map<String, Object> attributes;
    private final boolean newUser;

    public OAuthUserPrincipal(AppUser user, Map<String, Object> attributes, boolean newUser) {
        this.user = user;
        this.attributes = Map.copyOf(attributes);
        this.newUser = newUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getName() {
        return user.getId();
    }

    public boolean isNewUser() {
        return newUser;
    }
}

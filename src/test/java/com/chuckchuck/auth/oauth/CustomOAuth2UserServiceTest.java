package com.chuckchuck.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.chuckchuck.auth.user.AppUser;
import com.chuckchuck.auth.user.AppUserRepository;

class CustomOAuth2UserServiceTest {

    @Test
    void storesNewGoogleUser() {
        AppUserRepository repository = mock(AppUserRepository.class);
        when(repository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        CustomOAuth2UserService service = new CustomOAuth2UserService(repository);
        OAuth2User googleUser = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "google-123",
                        "email", "hong@gmail.com",
                        "name", "홍길동",
                        "picture", "https://example.com/profile.png"
                ),
                "sub"
        );

        OAuthUserPrincipal principal = service.saveOrUpdate(googleUser);

        assertThat(principal.isNewUser()).isTrue();
        assertThat(principal.getName()).isNotBlank();
        verify(repository).save(any(AppUser.class));
    }
}

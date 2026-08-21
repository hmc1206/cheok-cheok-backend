package com.chuckchuck.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.chuckchuck.auth.user.AppUser;
import com.chuckchuck.auth.user.AppUserRepository;

class CustomOAuth2UserServiceTest {

    @Test
    void storesNewGoogleUser() {
        AppUserRepository repository = mock(AppUserRepository.class);
        when(repository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        CustomOAuth2UserService service = new CustomOAuth2UserService(repository);
        OidcUser googleUser = mock(OidcUser.class);
        when(googleUser.getAttribute("sub")).thenReturn("google-123");
        when(googleUser.getAttribute("email")).thenReturn("hong@gmail.com");
        when(googleUser.getAttribute("name")).thenReturn("홍길동");
        when(googleUser.getAttribute("picture")).thenReturn("https://example.com/profile.png");
        when(googleUser.getAttributes()).thenReturn(Map.of(
                "sub", "google-123",
                "email", "hong@gmail.com",
                "name", "홍길동",
                "picture", "https://example.com/profile.png"
        ));

        OAuthUserPrincipal principal = ReflectionTestUtils.invokeMethod(
                service,
                "saveOrUpdate",
                googleUser
        );

        assertThat(principal.isNewUser()).isTrue();
        assertThat(principal.getName()).isNotBlank();
        verify(repository).save(any(AppUser.class));
    }
}

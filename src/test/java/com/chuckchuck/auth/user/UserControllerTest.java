package com.chuckchuck.auth.user;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.chuckchuck.common.exception.GlobalExceptionHandler;

class UserControllerTest {
    private AppUserRepository userRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepository.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAuthenticatedUser() throws Exception {
        AppUser user = AppUser.create("google-123", "hong@gmail.com", "홍길동", "profile.png");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        Principal principal = user::getId;

        mockMvc.perform(get("/api/users/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.email").value("hong@gmail.com"));
    }
}

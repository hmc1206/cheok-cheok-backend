package com.chuckchuck.auth.user;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chuckchuck.common.exception.ApiException;
import com.chuckchuck.common.exception.ErrorCode;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository userRepository;

    public UserController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {

        if (principal == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        AppUser user = userRepository.findById(principal.getName())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        return UserResponse.from(user);
    }
}
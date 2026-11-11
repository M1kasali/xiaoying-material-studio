package com.xiaoying.studio.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SessionController {
    private final SessionService sessions;
    public SessionController(SessionService sessions) { this.sessions = sessions; }

    @PostMapping("/demo/session")
    public ResponseEntity<User> login(@Valid @RequestBody LoginRequest input, HttpServletRequest request) {
        var login = sessions.login(input.account(), SessionService.token(request));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, login.cookie()).body(login.user());
    }

    @GetMapping("/me")
    public User me(@RequestAttribute(AuthenticationFilter.USER) User user) { return user; }

    @DeleteMapping("/session")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sessions.logout(SessionService.token(request))).build();
    }

    public record LoginRequest(@NotBlank String account) {}
}

package com.xiaoying.studio.auth;

import com.xiaoying.studio.config.StudioProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessionService {
    public static final String COOKIE = "studio_session";
    public static final Duration LIFETIME = Duration.ofDays(1);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, String> ACCOUNTS = Map.of(
            "alice", "00000000-0000-0000-0000-000000000001",
            "bob", "00000000-0000-0000-0000-000000000002");
    private final AuthMapper mapper;
    private final StudioProperties properties;

    public SessionService(AuthMapper mapper, StudioProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    @Transactional
    public Login login(String account, String previousToken) {
        if (!properties.demoEnabled()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "演示登录未启用");
        var userId = ACCOUNTS.get(account);
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的演示账户");
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        if (previousToken != null) mapper.deleteSession(hash(previousToken));
        mapper.deleteExpiredSessions();
        mapper.insertSession(hash(token), userId, Instant.now().plus(LIFETIME));
        return new Login(mapper.findUser(userId), cookie(token, LIFETIME));
    }

    public User resolve(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return null;
        return mapper.findSession(hash(token));
    }

    public String logout(String token) {
        if (token != null) mapper.deleteSession(hash(token));
        return cookie("", Duration.ZERO);
    }

    public static String token(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) if (COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String cookie(String token, Duration lifetime) {
        return ResponseCookie.from(COOKIE, token).path("/").httpOnly(true)
                .secure(properties.secureCookie()).sameSite("Lax").maxAge(lifetime).build().toString();
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Login(User user, String cookie) {}
}

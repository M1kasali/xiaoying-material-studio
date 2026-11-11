package com.xiaoying.studio.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoying.studio.config.StudioProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    public static final String USER = "currentUser";
    private final SessionService sessions;
    private final StudioProperties properties;
    private final ObjectMapper json;

    public AuthenticationFilter(SessionService sessions, StudioProperties properties, ObjectMapper json) {
        this.sessions = sessions;
        this.properties = properties;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) { chain.doFilter(request, response); return; }
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        boolean read = request.getMethod().equals("GET") || request.getMethod().equals("HEAD");
        if (!read) {
            String origin = request.getHeader("Origin");
            if ((origin != null && !properties.allowedOrigins().contains(origin))
                    || "cross-site".equals(request.getHeader("Sec-Fetch-Site"))) {
                reject(response, HttpStatus.FORBIDDEN, "请求来源不受信任");
                return;
            }
        }
        boolean publicCatalog = read && (path.equals("/api/categories") || path.equals("/api/templates")
                || path.matches("/api/templates/[A-Za-z0-9_-]+"));
        boolean demoLogin = path.equals("/api/demo/session") && request.getMethod().equals("POST");
        if (!publicCatalog && !demoLogin) {
            User user = sessions.resolve(SessionService.token(request));
            if (user == null) { reject(response, HttpStatus.UNAUTHORIZED, "请先选择演示账户"); return; }
            request.setAttribute(USER, user);
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        json.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, message));
    }
}

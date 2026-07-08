package com.evently.web;

import com.evently.config.CookieProperties;
import com.evently.config.JwtProperties;
import com.evently.domain.User;
import com.evently.security.AuthPrincipal;
import com.evently.service.auth.AuthResult;
import com.evently.service.auth.AuthService;
import com.evently.web.dto.auth.AuthResponse;
import com.evently.web.dto.auth.LoginRequest;
import com.evently.web.dto.auth.SignupRequest;
import com.evently.web.dto.auth.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Authentication endpoints: signup, login, refresh, logout, and current-user.
 * <p>
 * Access tokens are returned in the response body; refresh tokens are delivered
 * as an httpOnly, SameSite cookie scoped to {@code /api/v1/auth} so they are only
 * sent to these endpoints and are inaccessible to JavaScript.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    /**
     * @param authService      authentication orchestration
     * @param cookieProperties refresh-cookie attributes
     * @param jwtProperties    supplies the refresh-cookie max-age
     */
    public AuthController(AuthService authService,
                          CookieProperties cookieProperties,
                          JwtProperties jwtProperties) {
        this.authService = authService;
        this.cookieProperties = cookieProperties;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Registers a new user and authenticates them.
     *
     * @param request the signup payload
     * @param httpRequest the servlet request (for audit metadata)
     * @return 201 with the access token and user; sets the refresh cookie
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request,
                                               HttpServletRequest httpRequest) {
        AuthResult result = authService.signup(request, userAgent(httpRequest), clientIp(httpRequest));
        return authResponse(result, HttpStatus.CREATED);
    }

    /**
     * Authenticates an existing user.
     *
     * @param request the login payload
     * @param httpRequest the servlet request (for audit metadata)
     * @return 200 with the access token and user; sets the refresh cookie
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResult result = authService.login(request, userAgent(httpRequest), clientIp(httpRequest));
        return authResponse(result, HttpStatus.OK);
    }

    /**
     * Rotates the refresh cookie and returns a new access token.
     *
     * @param httpRequest the servlet request (carries the refresh cookie)
     * @return 200 with a fresh access token; sets a rotated refresh cookie
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest) {
        String rawRefresh = readRefreshCookie(httpRequest);
        AuthResult result = authService.refresh(rawRefresh, userAgent(httpRequest), clientIp(httpRequest));
        return authResponse(result, HttpStatus.OK);
    }

    /**
     * Logs out by revoking the refresh-token family and clearing the cookie.
     *
     * @param httpRequest the servlet request (carries the refresh cookie)
     * @return 204 with a cleared refresh cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(readRefreshCookie(httpRequest));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    /**
     * Returns the currently authenticated user.
     *
     * @param principal the authenticated principal from the access token
     * @return 200 with the user's public profile
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = authService.currentUser(principal.userId());
        return UserResponse.from(user);
    }

    /** Builds the token response body and attaches the refresh cookie. */
    private ResponseEntity<AuthResponse> authResponse(AuthResult result, HttpStatus status) {
        AuthResponse body = new AuthResponse(
                result.accessToken(),
                "Bearer",
                result.expiresIn(),
                UserResponse.from(result.user()));
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()).toString())
                .body(body);
    }

    /** Creates the refresh cookie carrying the raw token. */
    private ResponseCookie buildRefreshCookie(String rawToken) {
        return ResponseCookie.from(cookieProperties.refreshName(), rawToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/api/v1/auth")
                .maxAge(jwtProperties.refreshTtl())
                .build();
    }

    /** Creates an expired, empty refresh cookie to clear it on logout. */
    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(cookieProperties.refreshName(), "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    /** Reads the raw refresh token from the request cookies, or {@code null}. */
    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieProperties.refreshName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

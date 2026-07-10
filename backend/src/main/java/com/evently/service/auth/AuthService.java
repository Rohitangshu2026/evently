package com.evently.service.auth;

import com.evently.domain.User;
import com.evently.repo.UserRepository;
import com.evently.security.JwtService;
import com.evently.security.RefreshTokenService;
import com.evently.web.dto.auth.LoginRequest;
import com.evently.web.dto.auth.SignupRequest;
import com.evently.web.error.ConflictException;
import com.evently.web.error.ResourceNotFoundException;
import com.evently.web.error.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Coordinates registration and authentication: password hashing, user
 * persistence, access-token minting, and refresh-token issuance/rotation.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * A valid Argon2 hash of a throwaway value, compared against when the email
     * is unknown so login takes the same time whether or not the account exists
     * (prevents account enumeration via response timing).
     */
    private final String timingEqualizationHash;

    /**
     * @param userRepository      user persistence
     * @param passwordEncoder     Argon2id encoder
     * @param jwtService          access-token minting
     * @param refreshTokenService refresh-token lifecycle
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.timingEqualizationHash = passwordEncoder.encode("timing-equalization-dummy");
    }

    /**
     * Registers a new user and immediately authenticates them.
     *
     * @param request   the signup payload
     * @param userAgent originating user-agent (audit)
     * @param ip        originating IP (audit)
     * @return access + refresh tokens and the created user
     * @throws ConflictException if the email is already registered
     */
    @Transactional
    public AuthResult signup(SignupRequest request, String userAgent, String ip){
        if(userRepository.existsByEmail(request.email())){
            throw new ConflictException("An account with that email already exists.");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(EnumSet.of(request.role()));
        userRepository.save(user);

        return issueTokens(user, userAgent, ip);
    }

    /**
     * Authenticates an existing user by email and password.
     *
     * @param request   the login payload
     * @param userAgent originating user-agent (audit)
     * @param ip        originating IP (audit)
     * @return access + refresh tokens and the user
     * @throws UnauthorizedException if the credentials are invalid (message is
     *                               deliberately generic to avoid account enumeration)
     */
    @Transactional
    public AuthResult login(LoginRequest request, String userAgent, String ip){
        User user = userRepository.findByEmail(request.email()).orElse(null);

        // Always run the (expensive) hash comparison — against a dummy hash when
        // the account doesn't exist — so response time doesn't reveal whether
        // the email is registered.
        String hashToCheck = user != null ? user.getPasswordHash() : timingEqualizationHash;
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if(user == null || !passwordMatches){
            throw new UnauthorizedException("Invalid email or password.");
        }
        return issueTokens(user, userAgent, ip);
    }

    /**
     * Rotates a refresh token and mints a fresh access token.
     *
     * @param rawRefreshToken the raw token from the client's cookie
     * @param userAgent       originating user-agent (audit)
     * @param ip              originating IP (audit)
     * @return a new access token, a rotated refresh token, and the user
     * @throws UnauthorizedException if the refresh token is missing, expired, or reused
     */
    @Transactional
    public AuthResult refresh(String rawRefreshToken, String userAgent, String ip){
        if(rawRefreshToken == null || rawRefreshToken.isBlank()){
            throw new UnauthorizedException("You need to sign in to continue.");
        }
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken, userAgent, ip);
        User user = rotation.user();
        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResult(accessToken, jwtService.getAccessTtlSeconds(), rotation.rawToken(), user);
    }

    /**
     * Logs out by revoking the presented refresh token's whole family.
     *
     * @param rawRefreshToken the raw token from the client's cookie (may be null)
     */
    @Transactional
    public void logout(String rawRefreshToken){
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }

    /**
     * Loads the current user for {@code /auth/me}.
     *
     * @param userId the authenticated user's id
     * @return the user
     * @throws ResourceNotFoundException if the user no longer exists
     */
    @Transactional(readOnly = true)
    public User currentUser(UUID userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    /** Mints an access token and issues a new refresh-token family for the user. */
    private AuthResult issueTokens(User user, String userAgent, String ip){
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user, userAgent, ip);
        return new AuthResult(accessToken, jwtService.getAccessTtlSeconds(), refreshToken, user);
    }
}

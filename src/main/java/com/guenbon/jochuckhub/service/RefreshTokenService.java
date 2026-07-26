package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.config.jwt.JwtTokenProvider;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.RefreshToken;
import com.guenbon.jochuckhub.exception.InvalidRefreshTokenException;
import com.guenbon.jochuckhub.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public TokenPair issue(Member member) {
        refreshTokenRepository.deleteByMember_Id(member.getId());
        return createTokenPair(member);
    }

    @Transactional
    public TokenPair rotate(String presentedRefreshToken) {
        if (presentedRefreshToken == null || presentedRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Invalid refresh token.");
        }
        String tokenHash = hash(presentedRefreshToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token."));

        if (refreshToken.isExpired(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidRefreshTokenException("Expired refresh token.");
        }

        Member member = refreshToken.getMember();
        refreshTokenRepository.delete(refreshToken);
        return createTokenPair(member);
    }

    @Transactional
    public void revoke(String presentedRefreshToken) {
        if (presentedRefreshToken != null && !presentedRefreshToken.isBlank()) {
            refreshTokenRepository.deleteByTokenHash(hash(presentedRefreshToken));
        }
    }

    private TokenPair createTokenPair(Member member) {
        String refreshToken = createRefreshToken();
        refreshTokenRepository.save(RefreshToken.create(
                member,
                hash(refreshToken),
                Instant.now().plusMillis(refreshExpiration)));
        return new TokenPair(jwtTokenProvider.generateAccessToken(member.getUsername()), refreshToken);
    }

    private String createRefreshToken() {
        byte[] tokenBytes = new byte[64];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }
}

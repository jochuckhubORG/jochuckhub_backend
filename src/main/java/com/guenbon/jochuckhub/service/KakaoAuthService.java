package com.guenbon.jochuckhub.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guenbon.jochuckhub.config.logging.ExternalApiLogger;
import com.guenbon.jochuckhub.dto.response.LoginResponse;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.exception.KakaoAuthenticationException;
import com.guenbon.jochuckhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private static final String PROVIDER = "kakao";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;
    private final ExternalApiLogger externalApiLogger;
    private final RestClient restClient = RestClient.create();

    @Transactional
    public LoginResponse kakaoLogin(String kakaoAccessToken) {
        KakaoUserInfo userInfo = getKakaoUserInfo(kakaoAccessToken);
        if (userInfo.id() == null) {
            throw new KakaoAuthenticationException("invalid_user_info", "카카오 사용자 식별자가 없습니다.");
        }

        String kakaoId = String.valueOf(userInfo.id());
        String nickname = userInfo.kakaoAccount() != null
                && userInfo.kakaoAccount().profile() != null
                && userInfo.kakaoAccount().profile().nickname() != null
                ? userInfo.kakaoAccount().profile().nickname()
                : "사용자";

        boolean[] isNewMemberRef = {false};
        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    isNewMemberRef[0] = true;
                    return memberRepository.save(Member.builder()
                            .kakaoId(kakaoId)
                            .name(nickname)
                            .mainPosition(Position.GK)
                            .subPositions(Collections.emptySet())
                            .build());
                });

        RefreshTokenService.TokenPair tokenPair = refreshTokenService.issue(member);
        return new LoginResponse(
                tokenPair.accessToken(), tokenPair.refreshToken(), member.getId(), isNewMemberRef[0]);
    }

    private KakaoUserInfo getKakaoUserInfo(String kakaoAccessToken) {
        long startedAt = System.nanoTime();
        externalApiLogger.requested(PROVIDER, "user_info");
        try {
            ResponseEntity<KakaoUserInfo> response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .toEntity(KakaoUserInfo.class);
            externalApiLogger.succeeded(
                    PROVIDER, "user_info", response.getStatusCode().value(), elapsedMillis(startedAt));

            KakaoUserInfo body = response.getBody();
            if (body == null) {
                externalApiLogger.failed(
                        PROVIDER, "user_info", response.getStatusCode().value(),
                        "invalid_response", elapsedMillis(startedAt));
                throw new KakaoAuthenticationException(
                        "invalid_user_info", "카카오 사용자 정보를 받을 수 없습니다.");
            }
            return body;
        } catch (RestClientResponseException e) {
            String reason = classifyHttpFailure(e.getStatusCode().value());
            externalApiLogger.failed(
                    PROVIDER, "user_info", e.getStatusCode().value(), reason, elapsedMillis(startedAt));
            throw new KakaoAuthenticationException(reason, "카카오 사용자 정보 조회에 실패했습니다.", e);
        } catch (RestClientException e) {
            externalApiLogger.failed(
                    PROVIDER, "user_info", null, "network_error", elapsedMillis(startedAt));
            throw new KakaoAuthenticationException(
                    "network_error", "카카오 인증 서버에 연결할 수 없습니다.", e);
        }
    }

    private String classifyHttpFailure(int status) {
        if (status == 401 || status == 403) return "invalid_kakao_access_token";
        if (status == 429) return "rate_limited";
        if (status >= 500) return "provider_unavailable";
        return "provider_error";
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record KakaoUserInfo(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {}

    private record KakaoAccount(KakaoProfile profile) {}

    private record KakaoProfile(String nickname) {}
}

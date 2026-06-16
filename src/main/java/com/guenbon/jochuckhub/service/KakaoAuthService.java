package com.guenbon.jochuckhub.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.guenbon.jochuckhub.config.jwt.JwtTokenProvider;
import com.guenbon.jochuckhub.dto.response.LoginResponse;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestClient restClient = RestClient.create();

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Transactional
    public LoginResponse kakaoLogin(String code) {
        String kakaoAccessToken = getKakaoAccessToken(code);
        KakaoUserInfo userInfo = getKakaoUserInfo(kakaoAccessToken);

        String kakaoId = String.valueOf(userInfo.id());
        String nickname = userInfo.kakaoAccount() != null
                && userInfo.kakaoAccount().profile() != null
                ? userInfo.kakaoAccount().profile().nickname()
                : "사용자";

        boolean isNewMember = !memberRepository.findByKakaoId(kakaoId).isPresent();

        Member member = memberRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .kakaoId(kakaoId)
                                .name(nickname)
                                .mainPosition(Position.GK)
                                .subPositions(Collections.emptySet())
                                .build()
                ));

        String token = jwtTokenProvider.generateToken(member.getUsername());
        return new LoginResponse(token, member.getId(), isNewMember);
    }

    private String getKakaoAccessToken(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        KakaoTokenResponse response = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalArgumentException("카카오 토큰 발급에 실패했습니다.");
        }
        return response.accessToken();
    }

    private KakaoUserInfo getKakaoUserInfo(String kakaoAccessToken) {
        KakaoUserInfo userInfo = restClient.get()
                .uri(USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .body(KakaoUserInfo.class);

        if (userInfo == null) {
            throw new IllegalArgumentException("카카오 사용자 정보 조회에 실패했습니다.");
        }
        return userInfo;
    }

    // --- Internal Kakao API response records ---

    private record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    private record KakaoUserInfo(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {}

    private record KakaoAccount(
            KakaoProfile profile
    ) {}

    private record KakaoProfile(
            String nickname
    ) {}
}

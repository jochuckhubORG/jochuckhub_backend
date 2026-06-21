package com.guenbon.jochuckhub.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 메시지 API 서비스.
 *
 * <ul>
 *   <li>이미지 업로드: POST /v2/api/talk/memo/images/upload</li>
 *   <li>나에게 보내기: POST /v2/api/talk/memo/send</li>
 * </ul>
 *
 * 두 API 모두 사용자의 카카오 액세스 토큰이 필요하며,
 * talk_message 동의 항목이 허용되어야 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoMessageService {

    private static final String IMAGE_UPLOAD_URL = "https://kapi.kakao.com/v2/api/talk/memo/images/upload";
    private static final String SEND_ME_URL      = "https://kapi.kakao.com/v2/api/talk/memo/send";

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    /**
     * 이미지를 카카오 서버에 업로드하고 CDN URL을 반환한다.
     * 업로드는 요청자(OWNER/MANAGER)의 토큰으로 1회만 수행한다.
     */
    public String uploadImage(String accessToken, byte[] imageBytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() { return "lineup.png"; }
        });

        KakaoImageUploadResponse response = restClient.post()
                .uri(IMAGE_UPLOAD_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(KakaoImageUploadResponse.class);

        if (response == null || response.infos() == null || response.infos().original() == null) {
            throw new IllegalStateException("카카오 이미지 업로드에 실패했습니다.");
        }
        return response.infos().original().url();
    }

    /**
     * 사용자에게 '나에게 보내기' 피드 메시지를 전송한다.
     * 실패 시 RestClientException 을 그대로 던진다 (호출자가 개별 처리).
     */
    public void sendSelfMessage(String accessToken, String imageUrl,
                                String title, String description) {
        FeedTemplate template = new FeedTemplate(
                "feed",
                new FeedContent(title, description, imageUrl, 600, 900, new FeedLink(""))
        );

        String templateJson;
        try {
            templateJson = objectMapper.writeValueAsString(template);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("메시지 템플릿 직렬화 실패", e);
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("template_object", templateJson);

        restClient.post()
                .uri(SEND_ME_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .toBodilessEntity();
    }

    // ── Kakao API 응답/요청 내부 레코드 ──────────────────────────────

    private record KakaoImageUploadResponse(ImageInfos infos) {}
    private record ImageInfos(ImageInfo original) {}
    private record ImageInfo(String url, int width, int height) {}

    private record FeedTemplate(
            @JsonProperty("object_type") String objectType,
            FeedContent content
    ) {}

    private record FeedContent(
            String title,
            String description,
            @JsonProperty("image_url")    String imageUrl,
            @JsonProperty("image_width")  int    imageWidth,
            @JsonProperty("image_height") int    imageHeight,
            FeedLink link
    ) {}

    private record FeedLink(
            @JsonProperty("web_url") String webUrl
    ) {}
}

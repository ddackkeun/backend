package com.idea5.four_cut_photos_map.domain.shop.service.kakao;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.KakaoMapSearchDto;
import com.idea5.four_cut_photos_map.global.common.RedisDao;
import com.idea5.four_cut_photos_map.global.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapSearchApi {

    @Value("${REST_API_KEY}")
    private String FIRST_API_KEY;
    @Value("${oauth2.kakao.client-id}")
    private String SECOND_API_KEY;
    private final RestTemplate restTemplate;
    private final RedisDao redisDao;
    private final ObjectMapper objectMapper;
    public final int radius = 2000;
    public static final String DEFAULT_QUERY_WORD = "즉석사진";


    public List<KakaoMapSearchDto> searchByQueryWord(String queryWord, Double longitude, Double latitude, boolean hasRadius) {
        List<KakaoMapSearchDto> resultList = new ArrayList<>();

        // 1. API 호출을 위한 요청 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + FIRST_API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String apiURL = "https://dapi.kakao.com/v2/local/search/keyword.JSON?"
                + "query=" + queryWord + DEFAULT_QUERY_WORD
                + "&x=" + longitude
                + "&y=" + latitude;

        if (hasRadius) apiURL
                += "&sort=distance" // 거리순 정렬
                + "&radius=" + radius; // 반경 2km 이내

        // 2. API 호출
        JsonNode documents = restTemplate.exchange(apiURL, HttpMethod.GET, entity, JsonNode.class)
                .getBody()
                .get("documents");

        // 3. JSON -> DTO 역직렬화
        return deserialize(resultList, documents);
    }

    public String[] searchByRoadAddressName(String roadAddressName) {
        // 1. Redis에서 조회
        String cacheKey = redisDao.getRoadAddressKey(roadAddressName);
        String cachedData = redisDao.getValues(cacheKey);

        if (cachedData != null) {
            log.info("=====RoadAddressName Cache Hit=====");
            return cachedData.split(",");
        }
        log.info("=====RoadAddressName Cache Miss=====");

        // 2. API 호출을 위한 요청 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + FIRST_API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String apiURL = "https://dapi.kakao.com/v2/local/search/keyword.JSON?"
                + "query=" + roadAddressName + DEFAULT_QUERY_WORD;

        // 3. API 호출
        JsonNode document = restTemplate.exchange(apiURL, HttpMethod.GET, entity, JsonNode.class)
                .getBody()
                .get("documents");

        // 4. JSON -> String 역직렬화
        // 도로명주소와 DEFAULT_QUERY_WORD로 검색 시
        // 100% 일치하는 데이터가 항상 상단에 노출되지 않음
        // 따라서, 여러 데이터 중 요청 도로명 주소와 일치하는 데이터 1개만 찾아서 반환
        String[] result = matchAndDeserialize(document,roadAddressName);
        if(result != null){
            // 5. Redis에 데이터 저장
            redisDao.setValues(cacheKey, String.join(",", result), Duration.ofDays(1));
            return result;
        } else {
            return null;
        }
    }

    public String[] searchByRoadAddressName(String roadAddressName, Double curLnt, Double curLat) {
        // 1. API 호출을 위한 요청 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + SECOND_API_KEY);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String apiURL = "https://dapi.kakao.com/v2/local/search/keyword.JSON?"
                + "query=" + roadAddressName + DEFAULT_QUERY_WORD
                + "&x=" + curLnt
                + "&y=" + curLat;

        // 2. API 호출
        JsonNode document = restTemplate.exchange(apiURL, HttpMethod.GET, entity, JsonNode.class)
                .getBody()
                .get("documents");

        // 3. JSON -> String 역직렬화
        // 도로명주소와 DEFAULT_QUERY_WORD로 검색 시
        // 100% 일치하는 데이터가 항상 상단에 노출되지 않음
        // 따라서, 여러 데이터 중 요청 도로명 주소와 일치하는 데이터 1개만 찾아서 반환
        String[] result = matchAndDeserializeWithCurLocation(document,roadAddressName);
        if(result != null) return result;
        else return null;
    }

    private List<KakaoMapSearchDto> deserialize(List<KakaoMapSearchDto> resultList, JsonNode documents) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (JsonNode document : documents) {
            try {
                KakaoMapSearchDto dto = objectMapper.treeToValue(document, KakaoMapSearchDto.class);
                dto.setDistance(Util.distanceFormatting(dto.getDistance()));
                resultList.add(dto);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return resultList;
    }

    private String[] matchAndDeserialize(JsonNode documents, String roadAddressName) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (JsonNode document : documents) {
            if (document.get("road_address_name").asText().equals(roadAddressName)) {
                return new String[]{
                        document.get("place_name").asText(), document.get("place_url").asText(),
                        document.get("x").asText(), document.get("y").asText()
                };
            }
        }
        return null;
    }

    private String[] matchAndDeserializeWithCurLocation(JsonNode documents, String roadAddressName) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (JsonNode document : documents) {
            if (document.get("road_address_name").asText().equals(roadAddressName)) {
                return new String[]{
                        document.get("place_name").asText(), Util.distanceFormatting(document.get("distance").asText())
                };
            }
        }
        return null;
    }
}
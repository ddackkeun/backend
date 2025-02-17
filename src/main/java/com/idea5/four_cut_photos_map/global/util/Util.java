package com.idea5.four_cut_photos_map.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.idea5.four_cut_photos_map.AppConfig;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
public class Util {
    private static ObjectMapper getObjectMapper() {
        return (ObjectMapper) AppConfig.getContext().getBean("objectMapper");
    }

    public static class json {
        // map(json) -> String 변환
        public static Object toStr(Map<String, Object> map) {
            try {
                return getObjectMapper().writeValueAsString(map);
            } catch (JsonProcessingException e) {
                return null;
            }
        }

        // String -> map(json) 변환
        public static Map<String, Object> toMap(String jsonStr) {
            try {
                return getObjectMapper().readValue(jsonStr, LinkedHashMap.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
    }

    /**
     * 인자 값들을 map 으로 반환하는 메서드
     * @param args key, value 쌍들
     * @return key, value 들을 Map 으로 변환
     * @param <K> key 타입
     * @param <V> value 타입
     */
    public static<K, V> Map<K, V> mapOf(Object... args) {
        Map<K, V> map = new LinkedHashMap<>();

        int size = args.length / 2;

        for (int i = 0; i < size; i++) {
            int keyIndex = i * 2;
            int valueIndex = keyIndex + 1;

            K key = (K) args[keyIndex];
            V value = (V) args[valueIndex];

            map.put(key, value);
        }
        return map;
    }

    public static String distanceFormatting(String distance){
        if (distance.isEmpty()) {
            return distance;
        }

        int length = distance.length();
        double dkm = Integer.parseInt(distance) / 1000.0; // m -> km

        if (length < 4) { // distance -> m
            return distance + "m";
        } else if (length < 6) { // distance -> km
            if(dkm % 1 == 0)
                return String.format("%.0fkm", dkm);
            // 소수점 둘째 자리에서 반올림
            return String.format("%.1fkm", dkm);
        }
        // 소수점 첫째 자리에서 반올림
        return String.format("%.0fkm", dkm);
    }

    // 두 좌표간의 거리 계산
    public static String calculateDist(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 지구 반지름(km)

        // 두 지점 사이의 위도와 경도 차이를 라디안 단위로 계산
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceInMeters = R * c * 1000;

        return distanceFormatting((String.format("%.0f", distanceInMeters)));
    }


    // 난수 생성
    public static String generateRandomNumber(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++) {
            sb.append(random.nextInt(9));
        }
        return sb.toString();
    }

    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    // 도로명 주소 상세주소 제거
    public static String getRoadAddressName(String address) {
        int find = -1;
        for(int i = address.length() - 1; i >= 0; i--) {
            if(Character.isDigit(address.charAt(i))) {
                find = i;
                break;
            }
        }
        return (find == -1) ? address.trim() : address.substring(0, find + 1);
    }

    public static String removeSpace(String str) {
        return str.replaceAll("\\s+", "");
    }

}

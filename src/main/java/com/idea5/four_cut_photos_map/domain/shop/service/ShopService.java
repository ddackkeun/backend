package com.idea5.four_cut_photos_map.domain.shop.service;

import com.idea5.four_cut_photos_map.domain.favorite.dto.response.FavoriteResponse;
import com.idea5.four_cut_photos_map.domain.favorite.entity.Favorite;
import com.idea5.four_cut_photos_map.domain.favorite.repository.FavoriteRepository;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.*;
import com.idea5.four_cut_photos_map.domain.shop.entity.Shop;
import com.idea5.four_cut_photos_map.domain.shop.entity.ShopMatchPriority;
import com.idea5.four_cut_photos_map.domain.shop.repository.ShopRepository;
import com.idea5.four_cut_photos_map.domain.shop.service.kakao.KakaoMapSearchApi;
import com.idea5.four_cut_photos_map.global.common.RedisDao;
import com.idea5.four_cut_photos_map.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.idea5.four_cut_photos_map.global.error.ErrorCode.INVALID_SHOP_ID;
import static com.idea5.four_cut_photos_map.global.error.ErrorCode.SHOP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j

public class ShopService {
    private final ShopRepository shopRepository;
    private final FavoriteRepository favoriteRepository;
    private final KakaoMapSearchApi kakaoMapSearchApi;
    private final RedisDao redisDao;


    @Transactional(readOnly = true)
    public <T extends ResponseShop> List<T> findMatchingShops(List<KakaoMapSearchDto> apiShops, Class<T> responseClass) {
        List<T> resultShop = new ArrayList<>();
        for (KakaoMapSearchDto apiShop : apiShops) {
            // 도로명주소 비교로 반환하는 지점 없을 시, 지번주소로 비교
            Shop dbShop = compareWithPlaceNameOrAddress(apiShop.getPlaceName(), apiShop.getRoadAddressName(), apiShop.getAddressName());

            if (dbShop != null) {
                log.info("Matched: DB shop ({} - {}), Kakao API shop ({} - {} - {})",
                        dbShop.getPlaceName(), dbShop.getAddress(), apiShop.getPlaceName(),
                        apiShop.getRoadAddressName(), apiShop.getAddressName()
                );

                cacheShopInfoById(dbShop, apiShop);

                if (responseClass.equals(ResponseShopKeyword.class)) {
                    ResponseShopKeyword responseShop = ResponseShopKeyword.of(dbShop, apiShop, dbShop.getBrand());
                    resultShop.add(responseClass.cast(responseShop));
                } else if (responseClass.equals(ResponseShopBrand.class)) {
                    ResponseShopBrand responseShop = ResponseShopBrand.of(dbShop, apiShop, dbShop.getBrand());
                    resultShop.add(responseClass.cast(responseShop));
                }
            }
        }
        return resultShop;
    }

    /**
     * 지점명 일치여부나 주소명 포함여부로 비교하여 Kakao API Shop과 일치하는 DB Shop 객체 반환하는 메서드입니다.
     * 중복 발생 시 주소 중복일 확률이 높으므로, 지점명과 브랜드명을 기준으로 우선순위를 결정합니다.
     * @param placeName 카카오 API 지점명
     * @param addresses 카카오 API 도로명주소, 지번주소
     * @return DB Shop
     */
    @Transactional(readOnly = true)
    public Shop compareWithPlaceNameOrAddress(String placeName, String... addresses) {
        for (String address : addresses) {
            List<Shop> matchedShops = shopRepository.findDistinctByPlaceNameOrAddressContaining(
                    placeName,
                    address
            );
            if (matchedShops.size() == 1) {
                return matchedShops.get(0);
            } else if (matchedShops.size() > 1) {
                Shop matchedShop = compareMatchingShops(placeName, matchedShops);
                if (matchedShop != null) return matchedShop;
            }
            log.info("Not Matched: DB shops ({} - {}), Kakao API shop ({} - {})",
                    matchedShops.stream().map(Shop::getPlaceName).collect(Collectors.toList()),
                    matchedShops.stream().map(Shop::getAddress).collect(Collectors.toList()),
                    placeName,
                    address
            );
        }
        return null;
    }

    private Shop compareMatchingShops(String apiPlaceName, List<Shop> dbShops) {
        return Arrays.stream(ShopMatchPriority.values())
                .flatMap(priority -> dbShops.stream()
                        .filter(dbShop -> priority.isMatchedShop(dbShop.getPlaceName(), apiPlaceName)))
                .findFirst()
                .orElse(null);
    }

    private void cacheShopInfoById(Shop dbShop, KakaoMapSearchDto apiShop) {
        String cacheKey = redisDao.getShopInfoKey(dbShop.getId());
        redisDao.setValues(
                cacheKey,
                String.join(",", apiShop.getPlaceUrl(), apiShop.getLatitude(), apiShop.getLongitude()),
                Duration.ofDays(1)
        );
    }

    private void cacheInvalidShopId(long shopId) {
        String cacheKey = redisDao.getInvalidShopIdKey();
        redisDao.setValues(cacheKey, String.valueOf(shopId));
    }

    public List<KakaoMapSearchDto> searchKakaoMapByKeyword(String keyword, Double userLat, Double userLng) {
        return kakaoMapSearchApi.searchByQueryWord(keyword, userLat, userLng);
    }

    public List<KakaoMapSearchDto> searchKakaoMapByBrand(String brand, Integer radius, Double userLat, Double userLng, Double mapLat, Double mapLng) {
            return kakaoMapSearchApi.searchByQueryWord(brand, radius, userLat, userLng, mapLat, mapLng);
    }

    public String convertMapCenterCoordToAddress(Double mapLat, Double mapLng) {
        return kakaoMapSearchApi.convertCoordinateToAddress(mapLat, mapLng);
    }

    public String calcDistFromUserLocation(Shop dbShop, Double userLat, Double userLng) {
        String[] cachedArr = kakaoMapSearchApi.getShopInfoFromCacheAndCalcDist(dbShop, userLat, userLng);
        if (cachedArr != null) {
            return cachedArr[3];
        } else {
            if (dbShop.getAddress() != null) {
                return kakaoMapSearchApi.convertAddressToCoordAndCalcDist(dbShop, userLat, userLng);
            } else {
                String[] apiShop = kakaoMapSearchApi.searchSingleShopByQueryWord(dbShop, userLat, userLng, dbShop.getPlaceName());
                if (apiShop != null) {
                    return apiShop[3];
                }
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Shop findById(Long id) {
        return shopRepository.findById(id).orElseThrow(() -> new BusinessException(SHOP_NOT_FOUND));
    }

    public ResponseShopDetail setResponseDto(Shop dbShop, Double userLat, Double userLng) {
        // 지점명으로 반환하는 지점 없을 시, 주소로 비교
        String[] queryWords = dbShop.getAddress() == null ?
                    new String[]{dbShop.getPlaceName()} : new String[]{dbShop.getPlaceName(), dbShop.getAddress()};

        String[] apiShop = kakaoMapSearchApi.searchSingleShopByQueryWord(dbShop, userLat, userLng, queryWords);

        if (apiShop == null) {
            cacheInvalidShopId(dbShop.getId());
            throw new BusinessException(INVALID_SHOP_ID);
        }
        String placeUrl = apiShop[0];
        String placeLat = apiShop[1];
        String placeLng = apiShop[2];
        String distance = apiShop[3]; // userLat 또는 userLng가 null이면 Empty String 반환

        return ResponseShopDetail.of(dbShop, placeUrl, placeLat, placeLng, distance);
    }

    public FavoriteResponse setResponseDto(Favorite favorite, Double userLat, Double userLng) {
        String distance = calcDistFromUserLocation(favorite.getShop(), userLat, userLng);
        return FavoriteResponse.from(favorite, distance == null ? "" : distance);
    }

    @Transactional(readOnly = true)
    public ResponseShopBriefInfo setResponseDto(long id, String placeName, String distance) {
        Shop dbShop = findById(id);
        return ResponseShopBriefInfo.of(dbShop, placeName, distance);
    }

    public void reduceFavoriteCnt(Shop shop) {
        shop.setFavoriteCnt(shop.getFavoriteCnt() <= 0 ? 0 : shop.getFavoriteCnt() - 1);
        shopRepository.save(shop);
    }

    public void increaseFavoriteCnt(Shop shop){
        shop.setFavoriteCnt(shop.getFavoriteCnt()+1);
        shopRepository.save(shop);
    }
}
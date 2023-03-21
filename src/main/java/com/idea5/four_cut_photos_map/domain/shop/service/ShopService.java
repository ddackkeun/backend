package com.idea5.four_cut_photos_map.domain.shop.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.idea5.four_cut_photos_map.domain.shop.dto.KakaoKeywordResponseDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.KakaoResponseDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.ShopDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestBrandSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestKeywordSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestShop;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShop;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopDetail;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopMarker;
import com.idea5.four_cut_photos_map.domain.shop.entity.Shop;
import com.idea5.four_cut_photos_map.domain.shop.repository.ShopRepository;
import com.idea5.four_cut_photos_map.domain.shop.service.kakao.KeywordSearchKakaoApi;
import com.idea5.four_cut_photos_map.domain.shoptitlelog.service.ShopTitleLogService;
import com.idea5.four_cut_photos_map.global.common.data.Brand;
import com.idea5.four_cut_photos_map.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.idea5.four_cut_photos_map.global.error.ErrorCode.SHOP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {
    private final ShopRepository shopRepository;
    private final KeywordSearchKakaoApi keywordSearchKakaoApi;

    private final ShopTitleLogService shopTitleLogService;

    public List<ShopDto> findByBrand(String brandName){
        List<Shop> shops = shopRepository.findDistinctByPlaceNameStartingWith(brandName);

        return shops
                .stream()
                .map(shop -> ShopDto.of(shop))
                .collect(Collectors.toList());
    }

    public List<ResponseShop> findShops(List<KakaoKeywordResponseDto> apiShops) {
        List<ResponseShop> responseShops = new ArrayList<>();

        // 카카오 맵 API 데이터와 DB Shop 비교
        for (KakaoKeywordResponseDto apiShop: apiShops) {
            List<Shop> dbShops = shopRepository.findDistinctByRoadAddressName(apiShop.getRoadAddressName());

            if(dbShops.isEmpty()) continue;

            // 도로명주소 중복 데이터 존재 시 장소명으로 2차 필터링
            Shop dbShop = dbShops.size() == 1 ?  dbShops.get(0) : dbShops.stream()
                    .filter(db -> apiShop.getPlaceName().contains(db.getPlaceName()))
                    .findFirst()
                    .orElse(null);

            if(dbShop != null) {
                ResponseShop responseShop = ResponseShop.from(dbShop, apiShop);
                responseShops.add(responseShop);
            }
        }
        return responseShops;
    }

    public ResponseShopDetail findShopById(Long id, String distance) {
        Shop shop = shopRepository.findById(id).orElseThrow(() -> new BusinessException(SHOP_NOT_FOUND));
        ResponseShopDetail shopDto = ResponseShopDetail.of(shop, distance);
        return shopDto;

    }

    public Shop findById(Long id) {
        return shopRepository.findById(id).orElseThrow(() -> new BusinessException(SHOP_NOT_FOUND));
    }

    public List<KakaoKeywordResponseDto> searchByKeyword(RequestKeywordSearch requestKeywordSearch) throws JsonProcessingException {
        return keywordSearchKakaoApi.searchByKeyword(requestKeywordSearch);
    }

    public List<ResponseShopMarker> searchMarkers(RequestShop shop, String brandName) {
        List<KakaoResponseDto> kakaoShops = keywordSearchKakaoApi.searchMarkers(shop, brandName);
        List<ShopDto> dbShops = findByBrand(brandName);
        List<ResponseShopMarker> resultShops = new ArrayList<>();

        for (KakaoResponseDto kakaoShop : kakaoShops) {
            for (ShopDto dbShop : dbShops) {
                if (kakaoShop.getRoadAddressName().equals(dbShop.getRoadAddressName())) {
                    ResponseShopMarker responseShopMarker = ResponseShopMarker.of(kakaoShop);
                    responseShopMarker.setId(dbShop.getId());
                    // 상점이 칭호를 보유했으면 추가
                    if(shopTitleLogService.existShopTitles(dbShop.getId())){
                        responseShopMarker.setShopTitles(shopTitleLogService.getShopTitles(dbShop.getId()));
                    }
                    resultShops.add(responseShopMarker);
                    break;
                }
            }
        }
        return resultShops;
    }

    public List<KakaoResponseDto> searchBrand(RequestBrandSearch brandSearch) {
        List<KakaoResponseDto> list = new ArrayList<>();
        for (int i = 0; i < Brand.Names.length; i++) {
            list.addAll(keywordSearchKakaoApi.searchByBrand(brandSearch, i));
        }
        return list;
    }
    public boolean isRepresentativeBrand(String requestBrand) {
        return Arrays.stream(Brand.Names).anyMatch(representative -> representative.equals(requestBrand));
    }
}
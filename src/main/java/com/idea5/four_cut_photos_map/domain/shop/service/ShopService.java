package com.idea5.four_cut_photos_map.domain.shop.service;

import com.idea5.four_cut_photos_map.domain.brand.dto.response.ResponseBrandDto;
import com.idea5.four_cut_photos_map.domain.brand.entity.MajorBrand;
import com.idea5.four_cut_photos_map.domain.brand.service.BrandService;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestBrandSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestKeywordSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.KakaoMapSearchDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShop;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopBriefInfo;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopDetail;
import com.idea5.four_cut_photos_map.domain.shop.entity.Shop;
import com.idea5.four_cut_photos_map.domain.shop.repository.ShopRepository;
import com.idea5.four_cut_photos_map.domain.shop.service.kakao.KakaoMapSearchApi;
import com.idea5.four_cut_photos_map.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.idea5.four_cut_photos_map.global.error.ErrorCode.INVALID_SHOP_ID;
import static com.idea5.four_cut_photos_map.global.error.ErrorCode.SHOP_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j

public class ShopService {
    private final ShopRepository shopRepository;
    private final KakaoMapSearchApi kakaoMapSearchApi;
    private final BrandService brandService;


    public List<ResponseShop> compareWithDbShops(List<KakaoMapSearchDto> apiShops) {
        List<ResponseShop> resultShop = new ArrayList<>();
        for (KakaoMapSearchDto apiShop: apiShops) {
            List<Shop> dbShops = compareRoadAddressName(apiShop);
            if (dbShops.isEmpty()) continue;

            Shop dbShop = dbShops.size() == 1 ? dbShops.get(0) : comparePlaceName(apiShop, dbShops);

            if(dbShop != null) {
                ResponseBrandDto brandDto = brandService.getBrandById(dbShop.getBrand().getId());
                ResponseShop responseShop = ResponseShop.of(dbShop, apiShop, brandDto);
                resultShop.add(responseShop);
            }
        }
        return resultShop;
    }

    private List<Shop> compareRoadAddressName(KakaoMapSearchDto apiShop) {
        List<Shop> dbShops = shopRepository.findDistinctByRoadAddressName(apiShop.getRoadAddressName());
        return dbShops;
    }

    private Shop comparePlaceName(KakaoMapSearchDto apiShop, List<Shop> dbShops) {
        return dbShops.stream()
                .filter(dbShop -> apiShop.getPlaceName().contains(dbShop.getPlaceName()))
                .findFirst()
                .orElse(null);
    }

    public List<KakaoMapSearchDto> searchKakaoMapByKeyword(RequestKeywordSearch keywordSearch) {
        return kakaoMapSearchApi.searchByQueryWord (
                keywordSearch.getKeyword(),
                keywordSearch.getLongitude(),
                keywordSearch.getLatitude(),
                false
        );
    }

    public List<KakaoMapSearchDto> searchKakaoMapByBrand(RequestBrandSearch brandSearch) {
        return kakaoMapSearchApi.searchByQueryWord (
                brandSearch.getBrand(),
                brandSearch.getLongitude(),
                brandSearch.getLatitude(),
                true
        );
    }

    public Shop findById(Long id) {
        return shopRepository.findById(id).orElseThrow(() -> new BusinessException(SHOP_NOT_FOUND));
    }

    public ResponseShopDetail renameShopAndSetResponseDto(Shop dbShop, String distance) {
        String[] apiShop = kakaoMapSearchApi.searchByRoadAddressName(dbShop.getRoadAddressName());

        if(apiShop == null) throw new BusinessException(INVALID_SHOP_ID);
        String placeName = apiShop[0];
        String placeUrl = apiShop[1];
        String longitude = apiShop[2];
        String latitude = apiShop[3];

        return ResponseShopDetail.of(dbShop, placeName, placeUrl, longitude, latitude, distance);
    }


    public ResponseShopBriefInfo setResponseDto (long id, String placeName, String placeUrl, String distance) {
        Shop dbShop = findById(id);
        return ResponseShopBriefInfo.of(dbShop, placeName, placeUrl, distance);
    }

    public boolean isRepresentativeBrand(String requestBrand) {
        return Arrays.stream(MajorBrand.values()).anyMatch(
                representative -> representative.getBrandName().equals(requestBrand.trim())
        );
    }


    // 브랜드별 Map Marker
//    public List<ResponseShopMarker> searchMarkers(RequestShop shop, String brandName) {
//        List<KakaoKeywordResponseDto> kakaoShops = keywordSearchKakaoApi.searchByQueryWord(shop, brandName);
//        List<ShopDto> dbShops = findByBrand(brandName);
//        List<ResponseShopMarker> resultShops = new ArrayList<>();
//
//        for (KakaoKeywordResponseDto kakaoShop : kakaoShops) {
//            for (ShopDto dbShop : dbShops) {
//                if (kakaoShop.getRoadAddressName().equals(dbShop.getRoadAddressName())) {
//                    ResponseShopMarker responseShopMarker = ResponseShopMarker.of(kakaoShop);
//                    responseShopMarker.setId(dbShop.getId());
//                    // 상점이 칭호를 보유했으면 추가
//                    if(shopTitleLogService.existShopTitles(dbShop.getId())){
//                        responseShopMarker.setShopTitles(shopTitleLogService.getShopTitles(dbShop.getId()));
//                    }
//                    resultShops.add(responseShopMarker);
//                    break;
//                }
//            }
//        }
//        return resultShops;
//    }
}
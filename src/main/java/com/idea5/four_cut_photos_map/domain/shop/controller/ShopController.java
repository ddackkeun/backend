package com.idea5.four_cut_photos_map.domain.shop.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.idea5.four_cut_photos_map.domain.favorite.entity.Favorite;
import com.idea5.four_cut_photos_map.domain.favorite.service.FavoriteService;
import com.idea5.four_cut_photos_map.domain.shop.dto.KakaoResponseDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.ShopDto;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestBrandSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.request.RequestKeywordSearch;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopKeyword;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopBrand;
import com.idea5.four_cut_photos_map.domain.shop.dto.response.ResponseShopDetail;
import com.idea5.four_cut_photos_map.domain.shop.service.ShopService;
import com.idea5.four_cut_photos_map.domain.shoptitlelog.service.ShopTitleLogService;
import com.idea5.four_cut_photos_map.global.common.response.RsData;
import com.idea5.four_cut_photos_map.global.error.exception.BusinessException;
import com.idea5.four_cut_photos_map.security.jwt.dto.MemberContext;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

import static com.idea5.four_cut_photos_map.global.error.ErrorCode.DISTANCE_IS_EMPTY;
import static com.idea5.four_cut_photos_map.global.error.ErrorCode.INVALID_BRAND;


@RequestMapping("/shops")
@RestController
@RequiredArgsConstructor
@Slf4j
public class ShopController {

    private final ShopService shopService;
    private final FavoriteService favoriteService;
    private final ShopTitleLogService shopTitleLogService;


    @GetMapping(value = "")
    public ResponseEntity<RsData<List<ResponseShopKeyword>>> showSearchesByKeyword(@ModelAttribute @Valid RequestKeywordSearch requestKeywordSearch) throws JsonProcessingException {
        // todo: 키워드 유효성 검사(유도한 키워드가 맞는지)

        // 1. 카카오맵 api 응답 데이터 받아오기
        List<KakaoResponseDto> apiShop = shopService.searchByKeyword(requestKeywordSearch);
        if(apiShop.isEmpty())
            return ResponseEntity.ok(
                    new RsData<>(true,
                            String.format("키워드(%s)에 해당하는 지점이 존재하지 않습니다.", requestKeywordSearch.getKeyword()))
            );

        // 2. db 데이터와 비교
        List<ResponseShopKeyword> resultShops = shopService.compareWithDbShops(apiShop);
        if(resultShops.isEmpty())
            return ResponseEntity.ok(
                    new RsData<>(true,
                            String.format("키워드(%s)에 해당하는 지점이 존재하지 않습니다.", requestKeywordSearch.getKeyword()))
            );

        return ResponseEntity.ok(
                new RsData<>(true, "키워드로 지점 조회 성공", resultShops)
        );
    }

    @GetMapping("/brand")
    public ResponseEntity<RsData<List<ResponseShopBrand>>> showSearchesByBrand(@ModelAttribute @Valid RequestBrandSearch requestBrandSearch) {
        // 1. 대표브랜드 여부 확인
        boolean hasBrand = false;
        if(!ObjectUtils.isEmpty(requestBrandSearch.getBrand())) {
            if (!shopService.isRepresentativeBrand(requestBrandSearch.getBrand()))
                throw new BusinessException(INVALID_BRAND);
            else hasBrand = true;
        }

        // 2. 카카오맵 api 응답 데이터 받아오기
        List<KakaoResponseDto> apiShop = shopService.searchByBrand(requestBrandSearch);
        if(apiShop.isEmpty())
            return ResponseEntity.ok(
                    new RsData<>(true,
                            String.format("반경 2km 이내에 %s 지점이 존재하지 않습니다.", requestBrandSearch.getBrand()))
            );

        // 3. db 데이터와 비교
        List<ResponseShopBrand> resultShops = new ArrayList<>();
        resultShops = shopService.compareWithDbShops(apiShop, resultShops);
        if(resultShops.isEmpty())
            return ResponseEntity.ok(
                    new RsData<>(true,
                            String.format("반경 2km 이내에 %s 지점이 존재하지 않습니다.", hasBrand? requestBrandSearch.getBrand():"전체"))
            );

        return ResponseEntity.ok(
                new RsData<>(true, "반경 2km 이내 지점 조회 성공", resultShops)
        );
    }

    // todo : @Validated 유효성 검사 시, httpstatus code 전달하는 방법
    @GetMapping("/{shopId}")
    public ResponseEntity<ResponseShopDetail> detail(@PathVariable(name = "shopId") Long id,
                                                     @RequestParam(name = "distance", required = false, defaultValue = "") String distance,
                                                     @AuthenticationPrincipal MemberContext memberContext) {
        if (distance.isEmpty()) {
            throw new BusinessException(DISTANCE_IS_EMPTY);
        }
        ResponseShopDetail shopDetailDto = shopService.findShopById(id, distance);

        if (memberContext != null) {
            Favorite favorite = favoriteService.findByShopIdAndMemberId(shopDetailDto.getId(), memberContext.getId());

            if (favorite == null) {
                shopDetailDto.setCanBeAddedToFavorites(true);
            } else {
                shopDetailDto.setCanBeAddedToFavorites(false);
            }
        }

        if (shopTitleLogService.existShopTitles(id)) {
            List<String> shopTitles = shopTitleLogService.getShopTitles(id);
            shopDetailDto.setShopTitles(shopTitles);
        }

        return ResponseEntity.ok(shopDetailDto); // todo: 응답구조에 맞게 처리
    }

    // 브랜드별 Map Marker
    // 현재 위치 기준, 반경 2km
//    @GetMapping("/marker")
//    public ResponseEntity<RsData<Map<String, List<ResponseShopMarker>>>> currentLocationSearch(@ModelAttribute @Valid RequestShop requestShop) {
//
//        String[] names = Brand.Names; // 브랜드명 ( 하루필름, 인생네컷 ... )
//
//        Map<String, List<ResponseShopMarker>> maps = new HashMap<>();
//        for (String brandName : names) {
//            List<ResponseShopMarker> list = shopService.searchMarkers(requestShop, brandName);
//            maps.put(brandName, list);
//        }
////
//        return ResponseEntity.ok(
//                new RsData<Map<String, List<ResponseShopMarker>>>(true, "반경 2km 이내 Shop 조회 성공", maps)
//        );
}
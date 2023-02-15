package com.idea5.four_cut_photos_map.domain.shoptitle.service;

import com.idea5.four_cut_photos_map.domain.shoptitle.dto.ShopTitleDto;
import com.idea5.four_cut_photos_map.domain.shoptitle.entity.ShopTitle;
import com.idea5.four_cut_photos_map.domain.shoptitle.repository.ShopTitleRepository;
import com.idea5.four_cut_photos_map.global.error.ErrorCode;
import com.idea5.four_cut_photos_map.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopTitleService {

    private ShopTitleRepository shopTitleRepository;

    public ShopTitleDto findShopTitle(Long id) {
        ShopTitle shopTitle = shopTitleRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.SHOP_TITLE_NOT_FOUND));
        return ShopTitleDto.of(shopTitle);
    }
}

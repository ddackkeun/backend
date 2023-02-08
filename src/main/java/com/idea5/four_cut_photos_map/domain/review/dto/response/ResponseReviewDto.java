package com.idea5.four_cut_photos_map.domain.review.dto.response;

import com.idea5.four_cut_photos_map.domain.member.entity.Member;
import com.idea5.four_cut_photos_map.domain.review.entity.Review;
import com.idea5.four_cut_photos_map.domain.review.entity.score.ItemScore;
import com.idea5.four_cut_photos_map.domain.review.entity.score.PurityScore;
import com.idea5.four_cut_photos_map.domain.review.entity.score.RetouchScore;
import com.idea5.four_cut_photos_map.domain.shop.entity.Shop;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ResponseReviewDto {

    // Review 관련 정보
    private Long id;                    // 리뷰 번호
    private LocalDateTime createDate;   // 리뷰 생성 일자
    private LocalDateTime modifyDate;   // 리뷰 수정 일자
    private int startRating;            // 별점
    private String content;             // 내용
    private PurityScore purity;         // 청결도
    private RetouchScore retouch;       // 보정
    private ItemScore item;             // 소품

    // Member 관련 정보
    private ReviewMemberDto reviewMemberDto;

    // Shop 관련 정보
    private ReviewShopDto reviewShopDto;


    public static ResponseReviewDto from(Review review) {
        Member writer = review.getWriter();
        Shop shop = review.getShop();


        ReviewMemberDto reviewMemberDto = ReviewMemberDto.builder()
                .id(writer.getId())
                .nickname(writer.getNickname())
                .build();

        ReviewShopDto reviewShopDto = ReviewShopDto.builder()
                .id(shop.getId())
                .brand(shop.getBrand())
                .name(shop.getName())
                .address(shop.getAddress())
                .build();


        return ResponseReviewDto.builder()
                .id(review.getId())
                .createDate(review.getCreateDate())
                .modifyDate(review.getModifyDate())
                .startRating(review.getStarRating())
                .content(review.getContent())
                .purity(review.getPurity())
                .retouch(review.getRetouch())
                .item(review.getItem())
                .reviewMemberDto(reviewMemberDto)
                .reviewShopDto(reviewShopDto)
                .build();
    }
}

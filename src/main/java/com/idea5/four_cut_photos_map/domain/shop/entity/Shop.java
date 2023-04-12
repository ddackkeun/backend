package com.idea5.four_cut_photos_map.domain.shop.entity;

import com.idea5.four_cut_photos_map.domain.brand.entity.Brand;
import com.idea5.four_cut_photos_map.global.base.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@Table(indexes = {@Index(name = "idx_shop_address",columnList = "roadAddressName")})
@DynamicUpdate
public class Shop extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;
    private String placeName;
    private String roadAddressName;
    private Integer favoriteCnt;
    private Integer reviewCnt;
    private Double starRatingAvg;


    public Shop(String placeName, String roadAddressName) {
        this.placeName = placeName;
        this.roadAddressName = roadAddressName;
    }

    public Shop(Long id, int favoriteCnt) {
        super.setId(id);
        this.favoriteCnt = favoriteCnt;
    }

    public Shop(Long id, int reviewCnt, double starRatingAvg) {
        super.setId(id);
        this.reviewCnt = reviewCnt;
        this.starRatingAvg = starRatingAvg;
    }
}
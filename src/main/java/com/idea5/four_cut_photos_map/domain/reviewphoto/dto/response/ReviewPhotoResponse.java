package com.idea5.four_cut_photos_map.domain.reviewphoto.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.idea5.four_cut_photos_map.domain.reviewphoto.enums.ReviewPhotoStatus;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReviewPhotoResponse {
    private Long id;
    private String createDate;
    private String modifyDate;
    private Long reviewId;
    private String fileName;
    private String filePath;
    private String fileType;
    private long fileSize;
    private ReviewPhotoStatus status;
}

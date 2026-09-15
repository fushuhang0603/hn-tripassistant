package com.hanghang.tripassistant.mapper;

import com.hanghang.tripassistant.business.domain.entity.GuideImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GuideImageMapper {
    void insertBatch(List<GuideImage> guideImages);

    List<String> selectUrlsByGuideId(@Param("guideId") Long guideId);
}

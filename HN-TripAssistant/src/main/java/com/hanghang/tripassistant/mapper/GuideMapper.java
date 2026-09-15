package com.hanghang.tripassistant.mapper;

import com.github.pagehelper.Page;
import com.hanghang.tripassistant.business.domain.dto.GuidePageQueryDTO;
import com.hanghang.tripassistant.business.domain.entity.Guide;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GuideMapper {

    void insert(Guide guide);

    Page<Guide> pageSelect(GuidePageQueryDTO dto);

    Guide selectById(@Param("id") Long id);

}

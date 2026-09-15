package com.hanghang.tripassistant.service;

import com.hanghang.tripassistant.business.domain.dto.GuidePageQueryDTO;
import com.hanghang.tripassistant.business.domain.dto.GuidePublishDTO;
import com.hanghang.tripassistant.business.domain.vo.GuideCardVO;
import com.hanghang.tripassistant.business.domain.vo.GuideDetailVO;
import com.hanghang.tripassistant.business.domain.vo.PageResult;

import java.util.List;

public interface GuideService {

    Long publish(GuidePublishDTO dto);

    PageResult<GuideCardVO> page(GuidePageQueryDTO dto);

    GuideDetailVO detail(Long id);
}

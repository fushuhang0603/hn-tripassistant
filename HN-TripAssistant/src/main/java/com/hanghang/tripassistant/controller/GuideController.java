package com.hanghang.tripassistant.controller;

import com.hanghang.tripassistant.business.common.Result;
import com.hanghang.tripassistant.business.domain.dto.GuidePageQueryDTO;
import com.hanghang.tripassistant.business.domain.dto.GuidePublishDTO;
import com.hanghang.tripassistant.business.domain.vo.GuideCardVO;
import com.hanghang.tripassistant.business.domain.vo.GuideDetailVO;
import com.hanghang.tripassistant.business.domain.vo.PageResult;
import com.hanghang.tripassistant.service.GuideService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/guide")
@Slf4j
public class GuideController {
    @Autowired
    private GuideService guideService;

    @PostMapping("/publish")
    public Result<Long> publish(@RequestBody GuidePublishDTO dto){
        log.info("发布攻略：{}",dto);
        Long guideId = guideService.publish(dto);
        return Result.success("攻略发布成功",guideId);
    }

    @PostMapping("/page")
    public Result<PageResult<GuideCardVO>> page(@RequestBody GuidePageQueryDTO dto){
        log.info("分页查询攻略：{}",dto);

        PageResult<GuideCardVO> pageResult = guideService.page(dto);
        return Result.success(pageResult);
    }

    @GetMapping("/detail/{id}")
    public Result<GuideDetailVO> detail(@PathVariable Long id) {
        log.info("查询攻略详情：{}", id);
        GuideDetailVO vo = guideService.detail(id);
        return Result.success(vo);
    }





}

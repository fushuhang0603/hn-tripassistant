package com.hanghang.tripassistant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.hanghang.tripassistant.business.common.BusinessException;
import com.hanghang.tripassistant.business.domain.dto.GuidePageQueryDTO;
import com.hanghang.tripassistant.business.domain.dto.GuidePublishDTO;
import com.hanghang.tripassistant.business.domain.entity.Guide;
import com.hanghang.tripassistant.business.domain.entity.GuideImage;
import com.hanghang.tripassistant.business.domain.vo.GuideCardVO;
import com.hanghang.tripassistant.business.domain.vo.GuideDetailVO;
import com.hanghang.tripassistant.business.domain.vo.PageResult;
import com.hanghang.tripassistant.business.utils.UserContext;
import com.hanghang.tripassistant.mapper.GuideImageMapper;
import com.hanghang.tripassistant.mapper.GuideMapper;
import com.hanghang.tripassistant.service.GuideService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GuideServiceImpl implements GuideService {
    @Autowired
    private GuideMapper guideMapper;
    @Autowired
    private GuideImageMapper guideImageMapper;

    @Transactional
    @Override
    public Long publish(GuidePublishDTO dto) {
        if (StringUtils.isBlank(dto.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (StringUtils.isBlank(dto.getCity())) {
            throw new BusinessException("城市不能为空");
        }
        if (StringUtils.isBlank(dto.getContent())) {
            throw new BusinessException("正文不能为空");
        }
        List<String> images = dto.getImages();
        if (images != null && images.size() > 9) {
            throw new BusinessException("图片最多9张");
        }

        Guide guide = BeanUtil.copyProperties(dto, Guide.class);
        guide.setUserId(UserContext.getUserId());
        guide.setTags(joinTags(dto.getTags()));
        guide.setCreateTime(LocalDateTime.now());
        guide.setUpdateTime(LocalDateTime.now());
        guide.setLikes(0);
        guide.setStatus(1);
        guide.setAuthorName(StrUtil.blankToDefault(UserContext.getUsername(), ""));

        // 封面兜底：cover 为空时取图集第一张
        if (StrUtil.isBlank(guide.getCover()) && images != null && !images.isEmpty()) {
            guide.setCover(images.get(0));
        }

        // 先插攻略主体，回填 id
        guideMapper.insert(guide);

        // 再用回填的 id 批量写图集（AI 一键发布 images 为空时自动跳过）
        if (images != null && !images.isEmpty()) {
            List<GuideImage> guideImages = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                guideImages.add(GuideImage.builder()
                        .guideId(guide.getId())
                        .url(images.get(i))
                        .sort(i)
                        .createTime(LocalDateTime.now())
                        .build());
            }
            guideImageMapper.insertBatch(guideImages);
        }

        return guide.getId();
    }

    @Override
    public PageResult<GuideCardVO> page(GuidePageQueryDTO dto) {
        int page = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int size = dto.getSize() == null || dto.getSize() < 1 ? 10 : dto.getSize();

        PageHelper.startPage(page, size);
        Page<Guide> pageResult = guideMapper.pageSelect(dto);
        return new PageResult<>(toCardVOs(pageResult.getResult()), pageResult.getTotal());
    }



    private List<GuideCardVO> toCardVOs(List<Guide> guides) {
        return guides.stream().map(g -> {
            GuideCardVO vo = BeanUtil.copyProperties(g, GuideCardVO.class);
            vo.setTags(splitTags(g.getTags()));
            return vo;
        }).toList();
    }

    private List<String> splitTags(String tags) {
        return StrUtil.isBlank(tags) ? List.of() : List.of(tags.split(","));
    }

    private String joinTags(List<String> tags) {
        return tags == null || tags.isEmpty() ? "" : String.join(",", tags);
    }

    @Override
    public GuideDetailVO detail(Long id) {
        Guide guide = guideMapper.selectById(id);
        if (guide == null) {
            throw new BusinessException("攻略不存在");
        }
        GuideDetailVO vo = BeanUtil.copyProperties(guide, GuideDetailVO.class);
        vo.setTags(splitTags(guide.getTags()));

        List<String> images = guideImageMapper.selectUrlsByGuideId(id);
        if (images == null || images.isEmpty()) {
            vo.setImages(List.of());
            // 点赞功能未做前返回 false 占位
            vo.setLiked(false);
            return vo;
        }
        vo.setImages(images);
        //TODO 点赞功能未做前返回 false 占位
        vo.setLiked(false);
        return vo;
    }


}

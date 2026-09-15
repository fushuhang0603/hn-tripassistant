package com.hanghang.tripassistant.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.hanghang.tripassistant.business.common.BusinessException;
import com.hanghang.tripassistant.business.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/upload")
@Slf4j
public class UploadController {

    @Value("${aliyun.oss.bucket}")
    private String bucket;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Autowired
    private OSS ossClient;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = uploadOne(file);
        return Result.success("上传成功", url);
    }

    @PostMapping("/images")
    public Result<List<String>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }
        if (files.length > 9) {
            throw new BusinessException("一次最多上传9张图片");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadOne(file));
        }
        return Result.success("上传成功", urls);
    }

    private String uploadOne(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 扩展名白名单：仅 jpg/jpeg/png/webp
        String originalName = file.getOriginalFilename();
        if (StrUtil.isBlank(originalName)) {
            throw new BusinessException("文件名不能为空");
        }
        String ext = StrUtil.subAfter(originalName, '.', true).toLowerCase();
        if (!Set.of("jpg", "jpeg", "png", "webp").contains(ext)) {
            throw new BusinessException("仅支持 jpg/jpeg/png/webp 格式");
        }

        // 大小校验 ≤ 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        // 对象路径：guide/yyyyMMdd/uuid.ext（专用攻略图，前缀固定）
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String objectKey = "guide/" + date + "/" + IdUtil.fastSimpleUUID() + "." + ext;

        try {
            ossClient.putObject(bucket, objectKey, file.getInputStream());
        } catch (Exception e) {
            log.error("上传图片到OSS失败：{}", e.getMessage(), e);
            throw new BusinessException("图片上传失败，请重试");
        }

        // 拼完整访问 URL 返回给前端
        return "https://" + bucket + "." + endpoint + "/" + objectKey;
    }
}
package com.virtuallearn.knowledge.controller;

import com.virtuallearn.knowledge.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 图片上传：保存到本地 uploads 目录，返回可访问的相对路径
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final List<String> ALLOWED_EXT = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error("仅支持 jpg / jpeg / png / gif / webp 格式");
        }

        File dir = new File(uploadDir).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.error("上传目录创建失败");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        // 必须传绝对路径，否则 transferTo 会解析到 Tomcat 临时目录
        file.transferTo(new File(dir, filename));

        return Result.success("/uploads/" + filename);
    }
}

package com.light.file.controller;

import com.light.common.result.Result;
import com.light.file.entity.FileMetadata;
import com.light.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.light.file.mapper.FileMetadataMapper;
import java.util.List;
import com.light.common.auth.CurrentUser;

@RestController
@RequestMapping({"/api/file", "/api/upload"})
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService storageService;
    private final FileMetadataMapper metadataMapper;
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileMetadata> upload(@RequestPart("file") MultipartFile file, @RequestParam(required = false) Long entryId) {
        return Result.success(storageService.store(file, entryId));
    }
    @GetMapping("/{id}") public Result<FileMetadata> metadata(@PathVariable Long id) { return Result.success(storageService.get(id)); }
    @GetMapping("/list")
    public Result<List<FileMetadata>> list(@RequestParam Long entryId) {
        return Result.success(metadataMapper.selectList(new LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getEntryId, entryId).eq(FileMetadata::getUserId, CurrentUser.requireId()).orderByDesc(FileMetadata::getCreateTime)));
    }
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileMetadata metadata = storageService.get(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(metadata.getOriginalName()).build().toString())
                .body(storageService.resource(id));
    }
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { storageService.delete(id); return Result.success(); }
}

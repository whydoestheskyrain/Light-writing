package com.light.file.service;

import com.light.common.exception.BusinessException;
import com.light.common.result.ErrorCode;
import com.light.common.auth.CurrentUser;
import com.light.file.entity.FileMetadata;
import com.light.file.mapper.FileMetadataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileMetadataMapper mapper;
    @Value("${light.file.storage-path:./data/uploads}") private String storagePath;

    public FileMetadata store(MultipartFile file, Long entryId) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename());
        if (original.contains("..")) throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不合法");
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        Path target = root.resolve(UUID.randomUUID() + "-" + original).normalize();
        if (!target.startsWith(root)) throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不合法");
        try {
            Files.createDirectories(root);
            Files.copy(file.getInputStream(), target);
            FileMetadata metadata = new FileMetadata();
            metadata.setUserId(CurrentUser.requireId());
            metadata.setEntryId(entryId); metadata.setOriginalName(original); metadata.setObjectKey(root.relativize(target).toString());
            metadata.setContentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream");
            metadata.setFileSize(file.getSize()); metadata.setSha256(sha256(target)); metadata.setCreateTime(LocalDateTime.now());
            mapper.insert(metadata);
            return metadata;
        } catch (IOException ex) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败"); }
    }

    public FileMetadata get(Long id) {
        FileMetadata metadata = mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileMetadata>()
                .eq(FileMetadata::getId, id).eq(FileMetadata::getUserId, CurrentUser.requireId()));
        if (metadata == null) throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        return metadata;
    }

    public Resource resource(Long id) {
        FileMetadata metadata = get(id);
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        Path path = root.resolve(metadata.getObjectKey()).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) throw new BusinessException(ErrorCode.NOT_FOUND, "文件内容不存在");
        return new FileSystemResource(path);
    }

    public void delete(Long id) {
        FileMetadata metadata = get(id);
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        try { Files.deleteIfExists(root.resolve(metadata.getObjectKey()).normalize()); }
        catch (IOException ex) { throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件删除失败"); }
        mapper.deleteById(id);
    }

    /**
     * 删除当前用户指定条目下的全部附件（磁盘文件 + 元数据）。
     * 供 knowledge 服务删除条目/模块时跨服务调用。返回删除的文件数。
     */
    public int deleteByEntry(Long entryId) {
        Long uid = CurrentUser.requireId();
        List<FileMetadata> files = mapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileMetadata>()
                        .eq(FileMetadata::getUserId, uid)
                        .eq(FileMetadata::getEntryId, entryId));
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        int deleted = 0;
        for (FileMetadata f : files) {
            try {
                Files.deleteIfExists(root.resolve(f.getObjectKey()).normalize());
                mapper.deleteById(f.getId());
                deleted++;
            } catch (IOException ex) {
                // 单个文件删除失败不阻断其余清理；元数据仍尽量删除，磁盘残留留待排查
                log.warn("附件磁盘文件删除失败 id={} key={}: {}", f.getId(), f.getObjectKey(), ex.getMessage());
                mapper.deleteById(f.getId());
            }
        }
        return deleted;
    }

    private String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("JDK不支持SHA-256", ex); }
    }
}

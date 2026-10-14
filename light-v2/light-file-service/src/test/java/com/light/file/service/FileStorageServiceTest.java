package com.light.file.service;

import com.light.common.auth.CurrentUser;
import com.light.common.auth.JwtService;
import com.light.common.exception.BusinessException;
import com.light.file.entity.FileMetadata;
import com.light.file.mapper.FileMetadataMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock FileMetadataMapper mapper;

    @InjectMocks FileStorageService service;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "storagePath", tempDir.toString());
        CurrentUser.set(new JwtService.UserPrincipal(1L, "light", "USER"));
        // 预注册实体的 TableInfo，否则 LambdaQueryWrapper 抛 "can not find lambda cache"
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, FileMetadata.class);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});
    }

    @Test
    void store_shouldWriteFileAndSaveMetadata() throws IOException {
        FileMetadata saved = service.store(png(), null);

        assertNotNull(saved);
        assertEquals(1L, saved.getUserId());
        assertEquals("avatar.png", saved.getOriginalName());
        assertEquals("image/png", saved.getContentType());
        assertEquals(4, saved.getFileSize());
        assertNotNull(saved.getSha256());
        // 文件确实落盘在配置的存储目录下
        assertTrue(Files.exists(tempDir.resolve(saved.getObjectKey())));
        verify(mapper).insert(any(FileMetadata.class));
    }

    @Test
    void store_emptyFile_shouldThrowBadRequest() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.store(empty, null));
        assertEquals(400, ex.getErrorCode().getCode());
    }

    @Test
    void store_pathTraversal_shouldThrowBadRequest() {
        MockMultipartFile evil = new MockMultipartFile(
                "file", "../../../etc/passwd", "text/plain", new byte[]{1});

        BusinessException ex = assertThrows(BusinessException.class, () -> service.store(evil, null));
        assertEquals(400, ex.getErrorCode().getCode());
    }

    @Test
    void get_notFound_shouldThrowNotFound() {
        when(mapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(99L));
        assertEquals(404, ex.getErrorCode().getCode());
    }

    @Test
    void resource_shouldReturnExistingFile() throws IOException {
        FileMetadata meta = new FileMetadata();
        meta.setId(1L);
        meta.setUserId(1L);
        meta.setObjectKey("key-1.png");
        Files.write(tempDir.resolve("key-1.png"), new byte[]{9, 9});
        when(mapper.selectOne(any())).thenReturn(meta);

        Resource resource = service.resource(1L);

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void resource_missingContent_shouldThrowNotFound() {
        FileMetadata meta = new FileMetadata();
        meta.setId(1L);
        meta.setUserId(1L);
        meta.setObjectKey("not-exist.png");
        when(mapper.selectOne(any())).thenReturn(meta);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.resource(1L));
        assertEquals(404, ex.getErrorCode().getCode());
    }

    @Test
    void delete_shouldDeleteFileAndRecord() throws IOException {
        FileMetadata meta = new FileMetadata();
        meta.setId(1L);
        meta.setUserId(1L);
        meta.setObjectKey("key-2.png");
        Files.write(tempDir.resolve("key-2.png"), new byte[]{1});
        when(mapper.selectOne(any())).thenReturn(meta);

        service.delete(1L);

        assertFalse(Files.exists(tempDir.resolve("key-2.png")));
        verify(mapper).deleteById(1L);
    }
}

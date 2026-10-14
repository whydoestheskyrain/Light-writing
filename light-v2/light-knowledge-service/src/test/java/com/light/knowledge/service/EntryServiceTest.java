package com.light.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.light.common.auth.CurrentUser;
import com.light.common.auth.JwtService;
import com.light.common.exception.BusinessException;
import com.light.common.result.PageResult;
import com.light.knowledge.entity.Entry;
import com.light.knowledge.mapper.EntryMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryServiceTest {

    @Mock EntryMapper entryMapper;
    @Mock ModuleService moduleService;

    @InjectMocks EntryService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        CurrentUser.set(new JwtService.UserPrincipal(1L, "light", "USER"));
        // 预注册实体的 TableInfo，否则 LambdaQueryWrapper 抛 "can not find lambda cache"
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, Entry.class);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private Entry entry() {
        Entry e = new Entry();
        e.setId(1L);
        e.setUserId(1L);
        e.setModuleId(10L);
        e.setTitle("Synchronized 原理");
        e.setTags("Java,并发");
        return e;
    }

    @Test
    void page_shouldReturnPageResult() {
        Page<Entry> page = new Page<>(1, 20);
        page.setRecords(List.of(entry()));
        page.setTotal(1);
        when(entryMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<Entry> result = service.page(1, 20, null, null, null, null);

        assertEquals(1, result.getRecords().size());
        assertEquals(1, result.getTotal());
    }

    @Test
    void tags_shouldAggregateDistinctSortedTags() {
        Entry a = new Entry();
        a.setTags("Java,并发");
        Entry b = new Entry();
        b.setTags(" Spring ,Java");
        Entry empty = new Entry();
        when(entryMapper.selectList(any())).thenReturn(List.of(a, b, empty));

        List<String> tags = service.tags();

        assertEquals(List.of("Java", "Spring", "并发"), tags);
    }

    @Test
    void save_missingModuleId_shouldThrowBadRequest() {
        Entry e = new Entry();
        e.setTitle("无模块条目");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(e));
        assertEquals(400, ex.getErrorCode().getCode());
    }

    @Test
    void save_invalidJson_shouldThrowBadRequest() {
        when(moduleService.getById(10L)).thenReturn(new com.light.knowledge.entity.Module());

        Entry e = new Entry();
        e.setModuleId(10L);
        e.setTitle("坏 JSON");
        e.setDataJson("{not-valid-json");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(e));
        assertEquals(400, ex.getErrorCode().getCode());
    }

    @Test
    void save_success_shouldNormalizeTagsAndSetOwner() {
        when(moduleService.getById(10L)).thenReturn(new com.light.knowledge.entity.Module());

        Entry e = new Entry();
        e.setModuleId(10L);
        e.setTitle("笔记");
        e.setTags("#Java, Spring ,Java");

        service.save(e);

        assertEquals("Java,Spring", e.getTags());   // 去 #、去重、去空白（distinct 大小写敏感）
        assertEquals("note", e.getType());           // 默认类型
        assertEquals(0, e.getStatus());              // 默认状态
        assertEquals(1L, e.getUserId());             // 归属当前用户
        verify(entryMapper).insert(e);
    }

    @Test
    void updateStatus_invalidStatus_shouldThrowBadRequest() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateStatus(1L, 99));
        assertEquals(400, ex.getErrorCode().getCode());
    }

    @Test
    void delete_shouldDeleteOwnedEntry() {
        when(entryMapper.selectOne(any())).thenReturn(entry());

        service.delete(1L);

        verify(entryMapper).delete(any());
    }
}

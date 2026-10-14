package com.light.knowledge.service;

import com.light.common.auth.CurrentUser;
import com.light.common.auth.JwtService;
import com.light.common.exception.BusinessException;
import com.light.knowledge.entity.Module;
import com.light.knowledge.mapper.ModuleMapper;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleServiceTest {

    @Mock ModuleMapper moduleMapper;

    @InjectMocks ModuleService service;

    @BeforeEach
    void setUp() {
        CurrentUser.set(new JwtService.UserPrincipal(1L, "light", "USER"));
        // 预注册实体的 TableInfo，否则 LambdaQueryWrapper 抛 "can not find lambda cache"
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, Module.class);
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    private Module module() {
        Module m = new Module();
        m.setId(10L);
        m.setUserId(1L);
        m.setName("Java");
        m.setSortOrder(0);
        return m;
    }

    @Test
    void list_shouldQueryOnlyCurrentUser() {
        when(moduleMapper.selectList(any())).thenReturn(List.of(module()));

        List<Module> result = service.list();

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getName());
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(moduleMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(999L));
        assertEquals(404, ex.getErrorCode().getCode());
    }

    @Test
    void save_blankName_shouldThrowBadRequest() {
        Module m = new Module();
        m.setName("  ");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(m));
        assertEquals(400, ex.getErrorCode().getCode());
    }

    @Test
    void save_success_shouldAssignOwner() {
        Module m = new Module();
        m.setName("Java");

        service.save(m);

        assertEquals(1L, m.getUserId());
        assertEquals(0, m.getSortOrder());
        verify(moduleMapper).insert(m);
    }

    @Test
    void update_shouldNullifyUserIdToPreventOwnershipTransfer() {
        when(moduleMapper.selectOne(any())).thenReturn(module());

        Module update = new Module();
        update.setName("Java 并发");
        service.update(10L, update);

        assertEquals(10L, update.getId());
        assertEquals(null, update.getUserId());
        verify(moduleMapper).updateById(update);
    }

    @Test
    void delete_shouldDeleteOwnedModule() {
        when(moduleMapper.selectOne(any())).thenReturn(module());

        service.delete(10L);

        verify(moduleMapper).deleteById(10L);
    }
}

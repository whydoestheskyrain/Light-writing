package com.light.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.light.common.exception.BusinessException;
import com.light.common.auth.CurrentUser;
import com.light.common.result.ErrorCode;
import com.light.knowledge.entity.Module;
import com.light.knowledge.mapper.ModuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {
    private final ModuleMapper moduleMapper;

    /**
     * 当前用户模块列表，按 userId 缓存（Redis），写操作时整缓存失效。
     * 个人知识库场景下模块列表量小、一致性要求高，allEntries 失效足够且最简单。
     */
    @Cacheable(cacheNames = "moduleList", key = "T(com.light.common.auth.CurrentUser).get().id()")
    public List<Module> list() {
        return moduleMapper.selectList(new LambdaQueryWrapper<Module>().eq(Module::getUserId, CurrentUser.requireId())
                .orderByAsc(Module::getSortOrder)
                .orderByDesc(Module::getCreateTime));
    }

    public Module getById(Long id) {
        Module module = moduleMapper.selectOne(new LambdaQueryWrapper<Module>()
                .eq(Module::getId, id).eq(Module::getUserId, CurrentUser.requireId()));
        if (module == null) throw new BusinessException(ErrorCode.NOT_FOUND, "模块不存在");
        return module;
    }

    @CacheEvict(cacheNames = "moduleList", allEntries = true)
    public void save(Module module) {
        if (!StringUtils.hasText(module.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模块名称不能为空");
        }
        if (module.getSortOrder() == null) module.setSortOrder(0);
        module.setUserId(CurrentUser.requireId());
        moduleMapper.insert(module);
    }

    @CacheEvict(cacheNames = "moduleList", allEntries = true)
    public void update(Long id, Module module) {
        getById(id);
        module.setId(id);
        module.setUserId(null);
        moduleMapper.updateById(module);
    }

    @CacheEvict(cacheNames = "moduleList", allEntries = true)
    public void delete(Long id) {
        getById(id);
        moduleMapper.deleteById(id);
    }
}

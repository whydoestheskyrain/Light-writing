package com.virtuallearn.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtuallearn.knowledge.entity.Module;
import com.virtuallearn.knowledge.mapper.ModuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleMapper moduleMapper;

    public List<Module> list() {
        return moduleMapper.selectList(
                new LambdaQueryWrapper<Module>()
                        .orderByAsc(Module::getSortOrder)
                        .orderByDesc(Module::getCreateTime)
        );
    }

    public Module getById(Long id) {
        return moduleMapper.selectById(id);
    }

    public void save(Module module) {
        if (module.getSortOrder() == null) {
            module.setSortOrder(0);
        }
        moduleMapper.insert(module);
    }

    public void update(Module module) {
        moduleMapper.updateById(module);
    }

    public void delete(Long id) {
        moduleMapper.deleteById(id);
    }
}

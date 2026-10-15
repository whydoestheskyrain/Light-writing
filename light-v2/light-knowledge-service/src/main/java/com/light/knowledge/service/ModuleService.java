package com.light.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.light.common.exception.BusinessException;
import com.light.common.auth.CurrentUser;
import com.light.common.result.ErrorCode;
import com.light.knowledge.client.FileServiceClient;
import com.light.knowledge.entity.Entry;
import com.light.knowledge.entity.Module;
import com.light.knowledge.mapper.EntryMapper;
import com.light.knowledge.mapper.ModuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleService {
    private final ModuleMapper moduleMapper;
    private final EntryMapper entryMapper;
    private final FileServiceClient fileServiceClient;

    /** 从 coverImage 稳定路径 /api/file/{id}/download 中解析文件 id */
    private static final Pattern COVER_FILE_ID = Pattern.compile("/api/file/(\\d+)/download");

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

    /**
     * 删除模块（级联清理）：
     * 1. 查出模块下全部条目，逐个通知 file 服务清理附件
     * 2. 删除全部条目
     * 3. 通知 file 服务删除模块封面（entryId 为空的独立文件）
     * 4. 删除模块本身
     * 文件服务调用失败不阻断删除流程（FileServiceClient 内部已吞异常并记录）。
     */
    @CacheEvict(cacheNames = "moduleList", allEntries = true)
    public void delete(Long id) {
        Module module = getById(id);
        Long uid = CurrentUser.requireId();

        List<Entry> entries = entryMapper.selectList(new LambdaQueryWrapper<Entry>()
                .select(Entry::getId)
                .eq(Entry::getUserId, uid)
                .eq(Entry::getModuleId, id));
        for (Entry e : entries) {
            fileServiceClient.deleteAttachments(e.getId());
        }
        entryMapper.delete(new LambdaQueryWrapper<Entry>()
                .eq(Entry::getUserId, uid)
                .eq(Entry::getModuleId, id));

        Long coverFileId = parseCoverFileId(module.getCoverImage());
        if (coverFileId != null) {
            fileServiceClient.deleteFile(coverFileId);
        }

        moduleMapper.deleteById(id);
    }

    private Long parseCoverFileId(String coverImage) {
        if (!StringUtils.hasText(coverImage)) return null;
        Matcher m = COVER_FILE_ID.matcher(coverImage);
        if (!m.find()) return null;
        try {
            return Long.valueOf(m.group(1));
        } catch (NumberFormatException ex) {
            log.warn("模块封面路径中的文件 id 非法: {}", coverImage);
            return null;
        }
    }
}

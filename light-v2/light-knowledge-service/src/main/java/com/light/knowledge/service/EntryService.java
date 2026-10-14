package com.light.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.light.common.result.PageResult;
import com.light.common.exception.BusinessException;
import com.light.common.auth.CurrentUser;
import com.light.common.result.ErrorCode;
import com.light.knowledge.entity.Entry;
import com.light.knowledge.mapper.EntryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntryService {
    private final EntryMapper entryMapper;
    private final ModuleService moduleService;
    private final ObjectMapper objectMapper;

    public PageResult<Entry> page(long current, long size, Long moduleId, Integer status, String keyword, String tag) {
        Page<Entry> page = new Page<>(current, size);
        LambdaQueryWrapper<Entry> wrapper = new LambdaQueryWrapper<Entry>()
                .eq(Entry::getUserId, CurrentUser.requireId())
                .orderByDesc(Entry::getCreateTime);
        if (moduleId != null) wrapper.eq(Entry::getModuleId, moduleId);
        if (status != null) wrapper.eq(Entry::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Entry::getTitle, keyword)
                    .or().like(Entry::getContentMd, keyword));
        }
        if (StringUtils.hasText(tag)) {
            wrapper.apply("FIND_IN_SET({0}, tags)", tag.trim().replaceFirst("^#", ""));
        }
        Page<Entry> result = entryMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(),
                result.getSize(), result.getPages());
    }

    /**
     * 当前用户标签聚合，按 userId 缓存（Redis），Entry 任何写操作时整缓存失效。
     */
    @Cacheable(cacheNames = "entryTags", key = "T(com.light.common.auth.CurrentUser).get().id()")
    public List<String> tags() {
        return entryMapper.selectList(new LambdaQueryWrapper<Entry>().select(Entry::getTags)
                .eq(Entry::getUserId, CurrentUser.requireId())).stream()
                .map(Entry::getTags)
                .filter(StringUtils::hasText)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Entry getById(Long id) {
        Entry entry = entryMapper.selectOne(new LambdaQueryWrapper<Entry>()
                .eq(Entry::getId, id).eq(Entry::getUserId, CurrentUser.requireId()));
        if (entry == null) throw new BusinessException(ErrorCode.NOT_FOUND, "条目不存在");
        return entry;
    }

    @CacheEvict(cacheNames = "entryTags", allEntries = true)
    public void save(Entry entry) {
        if (entry.getModuleId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "必须指定模块");
        moduleService.getById(entry.getModuleId());
        if (!StringUtils.hasText(entry.getTitle())) throw new BusinessException(ErrorCode.BAD_REQUEST, "标题不能为空");
        if (!StringUtils.hasText(entry.getType())) entry.setType("note");
        if (entry.getStatus() == null) entry.setStatus(0);
        entry.setTags(normalizeTags(entry.getTags()));
        entry.setUserId(CurrentUser.requireId());
        validateJson(entry.getDataJson());
        entryMapper.insert(entry);
    }

    @CacheEvict(cacheNames = "entryTags", allEntries = true)
    public void update(Long id, Entry entry) {
        getById(id);
        if (entry.getModuleId() != null) moduleService.getById(entry.getModuleId());
        if (entry.getTitle() != null && !StringUtils.hasText(entry.getTitle())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "标题不能为空");
        }
        entry.setId(id);
        entry.setUserId(null);
        if (entry.getTags() != null) entry.setTags(normalizeTags(entry.getTags()));
        validateJson(entry.getDataJson());
        entryMapper.updateById(entry);
    }

    private void validateJson(String value) {
        if (!StringUtils.hasText(value)) return;
        try { objectMapper.readTree(value); }
        catch (Exception ex) { throw new BusinessException(ErrorCode.BAD_REQUEST, "dataJson必须是合法JSON"); }
    }

    private String normalizeTags(String tags) {
        if (!StringUtils.hasText(tags)) return null;
        return Arrays.stream(tags.replace('#', ',').split("[,，\\s]+"))
                .map(String::trim).filter(StringUtils::hasText).distinct()
                .limit(30).collect(Collectors.joining(","));
    }

    @CacheEvict(cacheNames = "entryTags", allEntries = true)
    public void delete(Long id) {
        getById(id);
        entryMapper.delete(new LambdaQueryWrapper<Entry>().eq(Entry::getId, id)
                .eq(Entry::getUserId, CurrentUser.requireId()));
    }

    @CacheEvict(cacheNames = "entryTags", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        if (status == null || status < 0 || status > 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态必须是 0、1 或 2");
        }
        getById(id);
        Entry entry = new Entry();
        entry.setId(id);
        entry.setStatus(status);
        entryMapper.update(entry, new LambdaQueryWrapper<Entry>().eq(Entry::getId, id)
                .eq(Entry::getUserId, CurrentUser.requireId()));
    }
}

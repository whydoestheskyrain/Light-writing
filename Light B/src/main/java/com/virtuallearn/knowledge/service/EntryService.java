package com.virtuallearn.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.virtuallearn.knowledge.entity.Entry;
import com.virtuallearn.knowledge.mapper.EntryMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntryService {

    private final EntryMapper entryMapper;

    public List<Entry> list(Long moduleId, Integer status, String keyword) {
        LambdaQueryWrapper<Entry> wrapper = new LambdaQueryWrapper<Entry>()
                .orderByDesc(Entry::getCreateTime);
        if (moduleId != null) {
            wrapper.eq(Entry::getModuleId, moduleId);
        }
        if (status != null) {
            wrapper.eq(Entry::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            // 标题或内容任一命中即返回
            wrapper.and(w -> w.like(Entry::getTitle, keyword).or().like(Entry::getContentMd, keyword));
        }
        return entryMapper.selectList(wrapper);
    }

    public Entry getById(Long id) {
        return entryMapper.selectById(id);
    }

    public void save(Entry entry) {
        if (StringUtils.hasText(entry.getContentMd())) {
            entry.setContentMd(Jsoup.clean(entry.getContentMd(), Safelist.relaxed()));
        }
        if (entry.getStatus() == null) {
            entry.setStatus(0);
        }
        entryMapper.insert(entry);
    }

    public void update(Entry entry) {
        if (StringUtils.hasText(entry.getContentMd())) {
            entry.setContentMd(Jsoup.clean(entry.getContentMd(), Safelist.relaxed()));
        }
        entryMapper.updateById(entry);
    }

    public void delete(Long id) {
        entryMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        Entry entry = new Entry();
        entry.setId(id);
        entry.setStatus(status);
        entryMapper.updateById(entry);
    }
}

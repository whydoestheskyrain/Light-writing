package com.light.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.light.knowledge.entity.Entry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EntryMapper extends BaseMapper<Entry> {
}

package com.light.identity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.light.identity.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {}

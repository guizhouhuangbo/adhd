package com.huangbo.adhd.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huangbo.adhd.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}

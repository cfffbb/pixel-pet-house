package com.pet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pet.entity.PomodoroTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PomodoroTaskMapper extends BaseMapper<PomodoroTask> {
}

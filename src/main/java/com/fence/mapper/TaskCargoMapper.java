package com.fence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fence.entity.TaskCargo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskCargoMapper extends BaseMapper<TaskCargo> {

    List<TaskCargo> selectByTaskId(@Param("taskId") Long taskId);

    TaskCargo selectByCargoId(@Param("cargoId") Long cargoId);

    boolean existsByCargoIdAndTaskId(@Param("cargoId") Long cargoId, @Param("taskId") Long taskId);

    int deleteByTaskId(@Param("taskId") Long taskId);

    int deleteByCargoId(@Param("cargoId") Long cargoId);
}
package com.fence.mapper;

import com.fence.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface LogMapper {

    int insert(OperationLog log);

    int deleteById(Long id);

    OperationLog selectById(Long id);

    List<OperationLog> selectList(Map<String, Object> params);

    int count(Map<String, Object> params);

    // 清理旧日志
    int deleteOldLogs(@Param("days") int days);

    // 统计日志数量
    int countByType(@Param("logType") String logType);
}

package com.fence.mapper;

import com.fence.entity.Task;
import com.fence.entity.TaskCargo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper {

    // Task 主表操作
    int insert(Task task);
    int update(Task task);
    int deleteById(Long id);
    Task selectById(Long id);
    Task selectByTaskNo(@Param("taskNo") String taskNo);
    List<Task> selectList(Map<String, Object> params);
    int count(Map<String, Object> params);

    // 任务状态更新
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    int updateById(Task task);
    int updateActualStartTime(@Param("id") Long id);
    int updateActualEndTime(@Param("id") Long id);

    // 任务分配
    int assignVehicleAndDriver(@Param("id") Long id,
                               @Param("vehicleId") Long vehicleId,
                               @Param("vehiclePlate") String vehiclePlate,
                               @Param("driverId") Long driverId,
                               @Param("driverName") String driverName);

    // TaskCargo 货物关联操作
    int insertTaskCargo(TaskCargo taskCargo);
    int batchInsertTaskCargos(@Param("cargos") List<TaskCargo> cargos);
    List<TaskCargo> selectCargosByTaskId(@Param("taskId") Long taskId);
    int deleteCargosByTaskId(@Param("taskId") Long taskId);

    // 查询超时任务
    List<Task> selectTimeoutTasks();
    int countByStatus(@Param("status") String status);
    Map<String, Object> statisticsByStatus();
}

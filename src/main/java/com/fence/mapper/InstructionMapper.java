package com.fence.mapper;

import com.fence.entity.Instruction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface InstructionMapper {

    int insert(Instruction instruction);

    int update(Instruction instruction);

    int deleteById(Long id);

    Instruction selectById(Long id);

    Instruction selectByInstructionNo(@Param("instructionNo") String instructionNo);

    Instruction selectLatestByVehicleAndType(@Param("vehicleId") Long vehicleId,
                                             @Param("instructionType") String instructionType);


    Instruction selectByCommandId(@Param("commandId") String commandId);

    List<Instruction> selectList(Map<String, Object> params);

    int count(Map<String, Object> params);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateCommandId(@Param("id") Long id, @Param("commandId") String commandId);

    int updateSent(@Param("id") Long id);

    int updateReceived(@Param("id") Long id);

    int updateExecuting(@Param("id") Long id);

    int updateCompleted(@Param("id") Long id, @Param("feedback") String feedback);

    int updateFailed(@Param("id") Long id, @Param("failReason") String failReason);

    List<Instruction> selectPendingInstructions();

    List<Instruction> selectByTaskId(@Param("taskId") Long taskId);

    List<Instruction> selectByVehicleId(@Param("vehicleId") Long vehicleId);
}

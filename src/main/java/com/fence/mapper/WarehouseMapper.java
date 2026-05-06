package com.fence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fence.entity.Warehouse;
import com.fence.entity.WarehouseAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {

    Warehouse selectByWarehouseCode(@Param("warehouseCode") String warehouseCode);

    List<Warehouse> selectByStatus(@Param("status") Integer status);

    List<Warehouse> selectByManagerPhone(@Param("managerPhone") String managerPhone);

    List<Warehouse> selectAllWithAdmin();

    WarehouseAdmin selectAdminById(@Param("id") Long id);
}


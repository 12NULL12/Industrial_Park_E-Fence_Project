package com.fence.entity;
//wzj
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class User implements Serializable {
    private String id;
    private String username;
    private String password;
    private String phone;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

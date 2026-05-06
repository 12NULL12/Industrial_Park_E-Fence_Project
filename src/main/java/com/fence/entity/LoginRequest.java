package com.fence.entity;
//wzj
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}

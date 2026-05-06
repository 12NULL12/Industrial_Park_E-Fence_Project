package com.fence.controller;
//wzj
import com.fence.service.RedisLoginService;
import com.fence.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;
    private final RedisLoginService redisLoginService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // 认证
        String userId = userService.authenticate(username, password);
        if (userId == null) {
            return Map.of("code", 401, "message", "用户名或密码错误");
        }

        // 生成Token
        String token = redisLoginService.createToken(userId);

        // 获取用户信息
        UserService.UserDTO user = userService.getUserById(userId);

        return Map.of(
                "code", 200,
                "message", "登录成功",
                "data", Map.of(
                        "token", token,
                        "userId", userId,
                        "username", user.getUsername(),
                        "role", user.getRole()
                )
        );
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        redisLoginService.logout(token);
        return Map.of("code", 200, "message", "登出成功");
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userinfo")
    public Map<String, Object> getUserInfo(@RequestHeader("Authorization") String auth) {
        String token = extractToken(auth);
        String userId = redisLoginService.getUserIdByToken(token);

        if (userId == null) {
            return Map.of("code", 401, "message", "未登录");
        }

        UserService.UserDTO user = userService.getUserById(userId);
        redisLoginService.refreshToken(token);

        return Map.of("code", 200, "data", user);
    }

    private String extractToken(String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return auth;
    }
}
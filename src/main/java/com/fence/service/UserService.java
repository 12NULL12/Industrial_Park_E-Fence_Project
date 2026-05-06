package com.fence.service;
//wzj
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String USER_PREFIX = "user:";
    private static final String USER_NAME_PREFIX = "username:";

    /**
     * 用户认证
     * @param username 用户名
     * @param password 密码
     * @return 用户ID，认证失败返回null
     */
    public String authenticate(String username, String password) {
        Object userIdObj = redisTemplate.opsForValue().get(USER_NAME_PREFIX + username);
        if (userIdObj == null) {
            return null;
        }

        String userId = userIdObj.toString();

        Object passwordObj = redisTemplate.opsForHash().get(USER_PREFIX + userId, "password");
        if (passwordObj == null) {
            return null;
        }

        String storedPassword = passwordObj.toString();
        if (!storedPassword.equals(password)) {
            return null;
        }

        return userId;
    }

    /**
     * 获取用户信息
     */
    public UserDTO getUserById(String userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);

        Object usernameObj = redisTemplate.opsForHash().get(USER_PREFIX + userId, "username");
        user.setUsername(usernameObj != null ? usernameObj.toString() : null);

        Object roleObj = redisTemplate.opsForHash().get(USER_PREFIX + userId, "role");
        user.setRole(roleObj != null ? roleObj.toString() : null);

        return user;
    }

    /**
     * 保存用户
     */
    public void saveUser(String userId, String username, String password, String role) {
        // 存用户Hash
        redisTemplate.opsForHash().put(USER_PREFIX + userId, "username", username);
        redisTemplate.opsForHash().put(USER_PREFIX + userId, "password", password);
        redisTemplate.opsForHash().put(USER_PREFIX + userId, "role", role);

        // 存用户名索引
        redisTemplate.opsForValue().set(USER_NAME_PREFIX + username, userId);
    }

    /**
     * 用户DTO
     */
    public static class UserDTO {
        private String id;
        private String username;
        private String role;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
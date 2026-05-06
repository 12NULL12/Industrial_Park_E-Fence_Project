package com.fence.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class Dispatcher extends User {
    private static volatile Dispatcher instance;

    private Dispatcher() {
        this.setId("1");
        this.setUsername("dispatcher");
        this.setPassword("wzjwzj11");
        this.setPhone("123");
        this.setCreateTime(LocalDateTime.now());
        this.setUpdateTime(LocalDateTime.now());
    }

    public static Dispatcher getInstance() {
        if (instance == null) {
            synchronized (Dispatcher.class) {
                if (instance == null) {
                    instance = new Dispatcher();
                }
            }
        }
        return instance;
    }
}

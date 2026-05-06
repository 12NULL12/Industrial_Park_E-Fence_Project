package com.fence;
//wzj
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ElderlyCareUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElderlyCareUserServiceApplication.class, args);
    }

}

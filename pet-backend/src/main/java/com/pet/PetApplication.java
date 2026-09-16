package com.pet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 桌面 AI 宠物助手 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.pet.mapper")
@EnableScheduling
public class PetApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetApplication.class, args);
    }
}

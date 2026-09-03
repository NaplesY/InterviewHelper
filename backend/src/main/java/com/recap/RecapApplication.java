package com.recap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.recap.mapper")
public class RecapApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecapApplication.class, args);
    }
}

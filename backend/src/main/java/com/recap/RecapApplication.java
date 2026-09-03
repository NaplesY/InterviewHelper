package com.recap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RecapApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecapApplication.class, args);
    }
}

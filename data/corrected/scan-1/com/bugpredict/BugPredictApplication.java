package com.bugpredict;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BugPredictApplication {
    public static void main(String[] args) {
        SpringApplication.run(BugPredictApplication.class, args);
    }
}
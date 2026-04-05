package com.payments.clearing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Entry point of clearing service
        SpringApplication.run(Application.class, args);
    }
}

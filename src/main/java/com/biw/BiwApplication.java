package com.biw;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.biw.mapper")
public class BiwApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiwApplication.class, args);
    }
}

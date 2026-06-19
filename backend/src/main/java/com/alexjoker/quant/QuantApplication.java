package com.alexjoker.quant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.alexjoker.quant.**.mapper")
public class QuantApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantApplication.class, args);
    }

}

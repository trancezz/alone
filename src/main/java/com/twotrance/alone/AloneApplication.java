package com.twotrance.alone;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
//@EnableDiscoveryClient
@MapperScan("com.twotrance.alone.mapper")
public class AloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(AloneApplication.class, args);
    }

}

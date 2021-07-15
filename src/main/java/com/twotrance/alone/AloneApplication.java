package com.twotrance.alone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class AloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(AloneApplication.class, args);
    }

}

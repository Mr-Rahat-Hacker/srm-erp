package com.srm.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SrmErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SrmErpApplication.class, args);
    }
}

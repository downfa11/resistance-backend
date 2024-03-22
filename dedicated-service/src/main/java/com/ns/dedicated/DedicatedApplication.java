package com.ns.dedicated;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class DedicatedApplication {

    public static void main(String[] args){
        SpringApplication.run(DedicatedApplication.class,args);
    }
}
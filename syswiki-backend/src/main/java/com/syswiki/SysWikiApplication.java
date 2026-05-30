package com.syswiki;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.syswiki.mapper")
@EnableAsync
public class SysWikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SysWikiApplication.class, args);
    }
}

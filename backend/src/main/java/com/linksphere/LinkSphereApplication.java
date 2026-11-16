package com.linksphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LinkSphereApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkSphereApplication.class, args);
    }
}

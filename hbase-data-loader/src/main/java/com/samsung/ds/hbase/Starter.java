package com.samsung.ds.hbase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;

@SpringBootApplication
@Slf4j
public class Starter {
    public static void main(String[] args) throws IOException {
        System.setProperty("spring.profiles.default", "local");
        System.setProperty("java.net.preferIPv4Stack", "true");

        ConfigurableApplicationContext context = SpringApplication.run(Starter.class, args);
    }
}

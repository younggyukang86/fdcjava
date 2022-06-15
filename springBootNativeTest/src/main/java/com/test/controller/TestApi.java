package com.test.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestApi {
    private final Logger logger = LoggerFactory.getLogger(TestApi.class);
    @GetMapping(path = "/api/v1/test")
    public String test() {
        logger.info("TEST OK!! LOG");
        logger.info("TEST OK!! LOG");

        return "TEST OK!!";
    }
}



package com.xiaoying.studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.xiaoying.studio.config.StudioProperties;

@SpringBootApplication
@EnableConfigurationProperties(StudioProperties.class)
public class StudioApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudioApplication.class, args);
    }
}

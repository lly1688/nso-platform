package com.nso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


// NSO平台应用启动类 Spring Boot应用主入口
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan

public class NsoApplication {
    public static void main(String[] args) {
        SpringApplication.run(NsoApplication.class, args);
    }
}

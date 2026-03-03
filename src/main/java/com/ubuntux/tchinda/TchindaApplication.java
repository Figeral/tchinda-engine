package com.ubuntux.tchinda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan("com.ubuntux.tchinda.config")
public class TchindaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TchindaApplication.class, args);
    }
}

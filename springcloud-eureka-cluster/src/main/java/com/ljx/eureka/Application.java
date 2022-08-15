package com.ljx.eureka;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication (exclude = {DataSourceAutoConfiguration.class})
@EnableEurekaServer //start registration center
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }
}

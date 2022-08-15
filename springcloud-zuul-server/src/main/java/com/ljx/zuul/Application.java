package com.ljx.zuul;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication (exclude = DataSourceAutoConfiguration.class)
@ComponentScan(basePackages = {"com.ljx","org.n3r.idworker"})
@EnableZuulProxy //start zuul proxy
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }
}

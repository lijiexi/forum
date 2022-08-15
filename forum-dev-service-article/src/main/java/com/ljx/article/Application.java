package com.ljx.article;

import com.rule.MyRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = "com.ljx.article.mapper")
@ComponentScan(basePackages = {"com.ljx","org.n3r.idworker"})
@EnableEurekaClient
//@RibbonClient(name = "service-user",configuration = MyRule.class)
@EnableFeignClients({"com.ljx"})
@EnableHystrix
public class Application {
    public static void main(String[] args) {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
        SpringApplication.run(Application.class,args);
    }
}

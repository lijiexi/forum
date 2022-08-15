package com.ljx.eureka.controller;

import com.ljx.api.controller.user.HelloControllerApi;
import com.ljx.grace.result.GraceJSONResult;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController implements HelloControllerApi {



    public Object hello(){
        return GraceJSONResult.ok();
    }
}

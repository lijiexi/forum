package com.ljx.exception;

import com.ljx.grace.result.GraceJSONResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AOP,统一自定义异常拦截处理，针对异常类型进行捕获，返回json到前端
 */
@ControllerAdvice
public class GraceExceptionHandler {

    @ExceptionHandler(MyCustomException.class)
    @ResponseBody
    public GraceJSONResult returnMyException(MyCustomException e){
        e.printStackTrace();
        return GraceJSONResult.exception(e.getResponseStatusEnum());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public GraceJSONResult returnMyException(MethodArgumentNotValidException e){
        BindingResult result = e.getBindingResult();
        Map<String, String> map = getErrors(result);
        //return GraceJSONResult.errorMap(map);
        return GraceJSONResult.errorMap(map);
    }
    public Map<String,String> getErrors(BindingResult result) {
        Map<String,String> map = new HashMap<>();
        List<FieldError> errorList = result.getFieldErrors();
        for(FieldError error : errorList){
            //发生验证错误时，对应的某个属性
            String field = error.getField();
            //验证的错误消息
            String msg = error.getDefaultMessage();
            map.put(field,msg);
        }
        return map;
    }
}

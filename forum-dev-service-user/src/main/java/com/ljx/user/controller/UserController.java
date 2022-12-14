package com.ljx.user.controller;

import com.ljx.api.BaseController;
import com.ljx.api.controller.user.HelloControllerApi;
import com.ljx.api.controller.user.UserControllerApi;
import com.ljx.grace.result.GraceJSONResult;
import com.ljx.grace.result.ResponseStatusEnum;
import com.ljx.pojo.AppUser;
import com.ljx.pojo.bo.UpdateUserInfoBO;
import com.ljx.pojo.vo.AppUserVO;
import com.ljx.pojo.vo.UserAccountInfoVO;
import com.ljx.user.service.UserService;
import com.ljx.utils.JsonUtils;
import com.ljx.utils.RedisOperator;
import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@DefaultProperties(defaultFallback = "defaultFallback")
public class UserController extends BaseController implements UserControllerApi {

    final static Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private UserService userService;


    public GraceJSONResult defaultFallback() {
        System.out.println("global hystrix");
        return GraceJSONResult.errorCustom(ResponseStatusEnum.SYSTEM_ERROR_GLOBAL);
    }

    @Override
    public GraceJSONResult getAccountInfo(String userId) {
        //1.判断参数不为空
        if(StringUtils.isBlank(userId)){
            return GraceJSONResult.errorCustom(ResponseStatusEnum.UN_LOGIN);
        }
        //2.根据userId查询用户信息
        AppUser user = getUser(userId);
        //3.返回用户信息
        UserAccountInfoVO accountInfoVO = new UserAccountInfoVO();
        //将属性信息考到vo
        BeanUtils.copyProperties(user,accountInfoVO);
        return GraceJSONResult.ok(accountInfoVO);
    }
    private AppUser getUser(String userId) {
        String userJson = redis.get(REDIS_USER_INFO+":"+userId);
        AppUser user = null;
        if(StringUtils.isNotBlank(userJson)) {
            user = JsonUtils.jsonToPojo(userJson,AppUser.class);
        }else{
            user = userService.getUser(userId);
            redis.set(REDIS_USER_INFO+":"+userId, JsonUtils.objectToJson(user));
        }
        return user;
    }

    @Override
    public GraceJSONResult updateUserInfo(@Valid UpdateUserInfoBO updateUserInfoBO){
//                                          BindingResult result) {
//        // 0.判断BindingResult中是否保存了错误的验证信息，如果有，则需要返回
//        if (result.hasErrors()) {
//            Map<String, String> map = getErrors(result);
//            return GraceJSONResult.errorMap(map);
//        }
        //1.执行更新操作
        userService.updateUserInfo(updateUserInfoBO);
        return GraceJSONResult.ok();
    }

    @Override
    public GraceJSONResult getUserInfo(String userId) {
        //1.判断参数不为空
        if(StringUtils.isBlank(userId)){
            return GraceJSONResult.errorCustom(ResponseStatusEnum.UN_LOGIN);
        }
        //2.根据userId查询用户信息
        AppUser user = getUser(userId);
        //3.返回用户信息
        AppUserVO userVO = new AppUserVO();
        BeanUtils.copyProperties(user,userVO);
        //4.查询Redis中用户关注数和粉丝数到，放入VO中，传给前端
        userVO.setMyFansCounts(getCountsFromRedis(REDIS_WRITER_FANS_COUNTS+":"+userId));
        userVO.setMyFollowCounts(getCountsFromRedis(REDIS_MY_FOLLOW_COUNTS+":"+userId));

        return GraceJSONResult.ok(userVO);
    }

    @Value("${server.port}")
    private String myPort;
    //远程调用
    //@HystrixCommand
    @HystrixCommand(fallbackMethod = "queryByIdsFallback")
    @Override
    public GraceJSONResult queryByIds(String userIds) {
        //1.trigger exception
        //int a = 1/0;
        //2.timeout exception
//        try {
//            Thread.sleep(6000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        System.out.println(myPort);
        if (StringUtils.isBlank(userIds)) {
            //为空
            return GraceJSONResult.errorCustom(ResponseStatusEnum.USER_NOT_EXIST_ERROR);
        }
        List<AppUserVO> publishList = new ArrayList<>();
        //将String转换成用户id的list
        List<String> userIdList = JsonUtils.jsonToList(userIds,String.class);

        //dev test
//        if(userIdList.size() > 1) {
//            System.out.println("appear exception");
//            throw new RuntimeException("appear exception");
//        }

        for (String userId : userIdList) {
            //获得基本信息
            AppUserVO appUserVO = getBasicUserInfo(userId);
            publishList.add(appUserVO);
        }

        return GraceJSONResult.ok(publishList);
    }
    public GraceJSONResult queryByIdsFallback(String userIds) {
        System.out.println("fallback method: queryByIdsFallback");
        if (StringUtils.isBlank(userIds)) {
            //为空
            return GraceJSONResult.errorCustom(ResponseStatusEnum.USER_NOT_EXIST_ERROR);
        }
        List<AppUserVO> publishList = new ArrayList<>();
        //将String转换成用户id的list
        List<String> userIdList = JsonUtils.jsonToList(userIds,String.class);
        for (String userId : userIdList) {
            //create empty object for detail site
            AppUserVO appUserVO = new AppUserVO();
            publishList.add(appUserVO);
        }

        return GraceJSONResult.ok(publishList);
    }
    /**
     *
     * @param userId 根据id在redis、数据库中查user信息
     * @return 返回UserVo
     */
    private AppUserVO getBasicUserInfo (String userId) {
        //1.根据userId查询用户信息
        AppUser user = getUser(userId);
        //2.返回用户信息
        AppUserVO userVO = new AppUserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }

}

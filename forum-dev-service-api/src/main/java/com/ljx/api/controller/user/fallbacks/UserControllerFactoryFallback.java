package com.ljx.api.controller.user.fallbacks;

import com.ljx.api.controller.user.UserControllerApi;
import com.ljx.grace.result.GraceJSONResult;
import com.ljx.grace.result.ResponseStatusEnum;
import com.ljx.pojo.bo.UpdateUserInfoBO;
import com.ljx.pojo.vo.AppUserVO;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserControllerFactoryFallback implements FallbackFactory<UserControllerApi> {
    @Override
    public UserControllerApi create(Throwable throwable) {
        return new UserControllerApi() {
            @Override
            public GraceJSONResult getAccountInfo(String userId) {
                return GraceJSONResult.errorCustom(ResponseStatusEnum.SYSTEM_ERROR_FEIGN);
            }

            @Override
            public GraceJSONResult updateUserInfo(UpdateUserInfoBO updateUserInfoBO) {
                return GraceJSONResult.errorCustom(ResponseStatusEnum.SYSTEM_ERROR_FEIGN);
            }

            @Override
            public GraceJSONResult getUserInfo(String userId) {
                return GraceJSONResult.errorCustom(ResponseStatusEnum.SYSTEM_ERROR_FEIGN);
            }

            @Override
            public GraceJSONResult queryByIds(String userIds) {
                System.out.println("enter client hystrix");
                List<AppUserVO> res = new ArrayList<>();
                return GraceJSONResult.ok(res);
            }
        };
    }
}

package com.ljx.zuul.filters;

import com.ljx.grace.result.GraceJSONResult;
import com.ljx.grace.result.ResponseStatusEnum;
import com.ljx.utils.IPUtil;
import com.ljx.utils.JsonUtils;
import com.ljx.utils.RedisOperator;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

//personalized gateway filter
@Component
public class BlackIPFilter extends ZuulFilter {
    @Value("${blackIp.continueCounts}")
    private Integer continueCounts;
    @Value("${blackIp.timeInterval}")
    private Integer timeInterval;
    @Value("${blackIp.limitTimes}")
    private Integer limitTimes;

    @Autowired
    private RedisOperator redis;

    /**
     * pre: before routing
     * route: route request
     * post: after routing
     * error: handle errors
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * order of filter execution
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return 3;
    }

    /**
     * Whether to enable the filter
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * The business implementation of the filter
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        System.out.println("Execute [IP blacklist] Zuul filter...");

        // get context object requestContext
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();

        // get request ip
        String ip = IPUtil.getRequestIp(request);

        /**
         * 需求：
         * Determine whether the number of ip requests exceeds 10 times within 10 seconds,
         * If it exceeds, the access will be restricted for 15 seconds, and the access will be released after 15 seconds.
         */
        final String ipRedisKey = "zuul-ip:" + ip;
        final String ipRedisLimitKey = "zuul-ip-limit:" + ip;

        // get the remaining time limit
        long limitLeftTime = redis.ttl(ipRedisLimitKey);
        // If the remaining time still exists, it means that the ip cannot be accessed, continue to wait
        if (limitLeftTime > 0) {
            stopRequest(requestContext);
            return null;
        }

        // Accumulate the number of requested accesses of ip in redis
        long requestCounts = redis.increment(ipRedisKey, 1);

        // The number of requests is counted from 0, and the initial access is 1, then the expiration time is set, that is, the interval time between consecutive requests
        if (requestCounts == 1) {
            redis.expire(ipRedisKey, timeInterval);
        }

        // If the number of requests can still be obtained, it means that the number of consecutive requests by the user falls within 10 seconds.
        // Once the number of requests exceeds the number of consecutive accesses, you need to limit this ip
        if (requestCounts > continueCounts) {
            // Restrict ip access for a period of time
            redis.set(ipRedisLimitKey, ipRedisLimitKey, limitTimes);

            stopRequest(requestContext);
        }

        return null;
    }

    private void stopRequest(RequestContext requestContext){
        // Stop further down-routing, prohibit request communication
        requestContext.setSendZuulResponse(false);
        requestContext.setResponseStatusCode(200);
        String result = JsonUtils.objectToJson(
                GraceJSONResult.errorCustom(
                        ResponseStatusEnum.SYSTEM_ERROR_BLACK_IP));
        requestContext.setResponseBody(result);
        requestContext.getResponse().setCharacterEncoding("utf-8");
        requestContext.getResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

}

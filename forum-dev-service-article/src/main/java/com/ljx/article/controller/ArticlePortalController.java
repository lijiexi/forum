package com.ljx.article.controller;

import com.ljx.api.BaseController;
import com.ljx.api.controller.article.ArticlePortalControllerApi;
import com.ljx.api.controller.user.UserControllerApi;
import com.ljx.article.service.ArticlePortalService;
import com.ljx.grace.result.GraceJSONResult;
import com.ljx.pojo.Article;
import com.ljx.pojo.eo.ArticleEO;
import com.ljx.pojo.vo.AppUserVO;
import com.ljx.pojo.vo.ArticleDetailVO;
import com.ljx.pojo.vo.IndexArticleVO;
import com.ljx.utils.IPUtil;
import com.ljx.utils.JsonUtils;
import com.ljx.utils.PagedGridResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
public class ArticlePortalController extends BaseController implements ArticlePortalControllerApi {

    final static Logger logger = LoggerFactory.getLogger(ArticlePortalController.class);
    @Autowired
    private ArticlePortalService articlePortalService;
    @Autowired
    private RestTemplate restTemplate;
    //@Autowired
    //private ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public GraceJSONResult eslist(String keyword,
                                  Integer category,
                                  Integer page,
                                  Integer pageSize) {
//        /**
//         * es?????????
//         *      1.??????????????????????????????
//         *      2.??????????????????
//         *      3.????????????????????????
//         */
//        //es???????????????0???????????????
//        if (page < 1) return null;
//        page--;
//        //??????
//        Pageable pageable = PageRequest.of(page,pageSize);
//        SearchQuery query = null;
//        //1.??????????????????????????????
//        if (StringUtils.isBlank(keyword) && category == null) {
//             query = new NativeSearchQueryBuilder()
//                    .withQuery(QueryBuilders.matchAllQuery())
//                    .withPageable(pageable) //????????????
//                    .build();
//        }
//        //2.???????????????????????????term?????????
//        if (StringUtils.isBlank(keyword) && category != null) {
//            query = new NativeSearchQueryBuilder()
//                    .withQuery(QueryBuilders.termQuery("categoryId",category))
//                    .withPageable(pageable) //????????????
//                    .build();
//
//
//        }
//        //3.????????????????????????
//        if (StringUtils.isNotBlank(keyword) && category == null) {
//            query = new NativeSearchQueryBuilder()
//                    .withQuery(QueryBuilders.matchQuery("title",keyword))
//                    .withPageable(pageable) //????????????
//                    .build();
//        }
//        AggregatedPage<ArticleEO> pagedArticle = elasticsearchTemplate.queryForPage(query, ArticleEO.class);
//        List<ArticleEO> articleList = pagedArticle.getContent();
//
//        // ???????????????grid??????
//        PagedGridResult gridResult = new PagedGridResult();
//        gridResult.setRows(articleList);
//        gridResult.setPage(page + 1);
//        gridResult.setTotal(pagedArticle.getTotalPages());
//        gridResult.setRecords(pagedArticle.getTotalElements());
//
//        gridResult = rebuildArticleGrid(gridResult);
//
//        return GraceJSONResult.ok(gridResult);
        return null;
    }

    @Override
    public GraceJSONResult mysqlList(String keyword,
                                Integer category,
                                Integer page,
                                Integer pageSize) {
        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGESIZE;
        }
        //service????????????
        PagedGridResult res = articlePortalService.queryIndexArticleList(keyword,category,page,pageSize);
        /**
         * ??????????????????
         */
        //start
        List<Article> list = (List<Article>)res.getRows();
        //??????list???????????????ID?????????ID???????????????
        //1.???????????????id??????,??????hashset??????
        Set<String> idSet = new HashSet<>();
        List<String> idList = new ArrayList<>();
        for (Article a : list) {
            //1.??????????????????set
            idSet.add(a.getPublishUserId());
            //2.????????????id???list
            idList.add(REDIS_ARTICLE_READ_COUNTS+":"+a.getId());
        }
        //??????redis???mget????????????api?????????????????????
        List<String> readCountsRedisList = redis.mget(idList);
        List<AppUserVO> publishList = null;

        publishList = getPublisherList(idSet);
        //3.??????list?????????????????????
        List<IndexArticleVO> indexArticleVOS = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            IndexArticleVO indexArticleVO = new IndexArticleVO();
            Article a = list.get(i);
            //??????article?????????vo
            BeanUtils.copyProperties(a,indexArticleVO);
            //??????publishList????????????????????????
            AppUserVO appUserVO = getUserInfo(a.getPublishUserId(),publishList);
            indexArticleVO.setPublisherVO(appUserVO);
            //??????????????????????????????????????????VO,??????????????????????????????mget??????
//            int readCounts = getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS+":"+a.getId());
//            indexArticleVO.setReadCounts(readCounts);
            String redisCountsStr = readCountsRedisList.get(i);
            int readCounts = 0;
            if (StringUtils.isNotBlank(redisCountsStr)) {
                readCounts = Integer.valueOf(redisCountsStr);
            }
            indexArticleVO.setReadCounts(readCounts);
            indexArticleVOS.add(indexArticleVO);
        }
        res.setRows(indexArticleVOS);
        return GraceJSONResult.ok(res);
    }



    private AppUserVO getUserInfo(String publisherId,
                                  List<AppUserVO> publishList) {
        for (AppUserVO user : publishList) {
            if (publisherId.equals(user.getId())) {
                return user;
            }
        }
        return null;
    }

    @Override
    public GraceJSONResult hotList() {
        List<Article> list = articlePortalService.queryHotList();
        return GraceJSONResult.ok(list);
    }

    @Override
    public GraceJSONResult queryArticleListOfWriter(String writerId,
                                                    Integer page,
                                                    Integer pageSize) {
        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGESIZE;
        }
        //service????????????
        PagedGridResult res
                = articlePortalService.queryIndexqueryArticleListOfWriterArticleList(writerId,page,pageSize);

        /**
         * ??????????????????
         */
        //start
        List<Article> list = (List<Article>)res.getRows();
        //??????list???????????????ID?????????ID???????????????
        //1.???????????????id??????,??????hashset??????
        Set<String> idSet = new HashSet<>();
        List<String> idList = new ArrayList<>();
        for (Article a : list) {
            //1.??????????????????set
            idSet.add(a.getPublishUserId());
            //2.????????????id???list
            idList.add(REDIS_ARTICLE_READ_COUNTS+":"+a.getId());
        }
        //??????redis???mget????????????api?????????????????????
        List<String> readCountsRedisList = redis.mget(idList);
        List<AppUserVO> publishList = null;
//        //2.??????resttemplate????????????????????????????????????????????????????????????
//        String userServerUrlExecute
//                = "http://user.news.com:8003/user/queryByIds?userIds="+JsonUtils.objectToJson(idSet);
//        ResponseEntity<GraceJSONResult> resultResponseEntity
//                = restTemplate.getForEntity(userServerUrlExecute,GraceJSONResult.class);
//        //??????????????????
//        GraceJSONResult bodyResult = resultResponseEntity.getBody();
//        //System.out.println(bodyResult);
//        List<AppUserVO> publishList = null;
//        if (bodyResult.getStatus() == 200) {
//            String userJson = JsonUtils.objectToJson(bodyResult.getData());
//            publishList = JsonUtils.jsonToList(userJson,AppUserVO.class);
//        }
        publishList = getPublisherList(idSet);
        //3.??????list?????????????????????
        List<IndexArticleVO> indexArticleVOS = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            IndexArticleVO indexArticleVO = new IndexArticleVO();
            Article a = list.get(i);
            //??????article?????????vo
            BeanUtils.copyProperties(a,indexArticleVO);
            //??????publishList????????????????????????
            AppUserVO appUserVO = getUserInfo(a.getPublishUserId(),publishList);
            indexArticleVO.setPublisherVO(appUserVO);
            //??????????????????????????????????????????VO,??????????????????????????????mget??????
//            int readCounts = getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS+":"+a.getId());
//            indexArticleVO.setReadCounts(readCounts);
            String redisCountsStr = readCountsRedisList.get(i);
            int readCounts = 0;
            if (StringUtils.isNotBlank(redisCountsStr)) {
                readCounts = Integer.valueOf(redisCountsStr);
            }
            indexArticleVO.setReadCounts(readCounts);
            indexArticleVOS.add(indexArticleVO);
        }
        res.setRows(indexArticleVOS);

        return GraceJSONResult.ok(res);
    }

    @Override
    public GraceJSONResult queryGoodArticleListOfWriter(String writerId) {
        PagedGridResult res
                = articlePortalService.queryGoodArticleListOfWriter(writerId);
        return GraceJSONResult.ok(res);
    }

    @Override
    public GraceJSONResult detail(String articleId) {

        ArticleDetailVO detailVO = articlePortalService.queryDetail(articleId);
        //??????????????????????????????
        //???????????????????????????????????????
        Set<String> idSet = new HashSet<>();
        idSet.add(detailVO.getPublishUserId());
        List<AppUserVO> publisherList = getPublisherList(idSet);
        if (!publisherList.isEmpty()) {
            detailVO.setPublishUserName(publisherList.get(0).getNickname());
        }

        detailVO.setReadCounts(getCountsFromRedis(REDIS_ARTICLE_READ_COUNTS+":"+articleId));
        return GraceJSONResult.ok(detailVO);
    }
    //get microservices info
    @Autowired
    private DiscoveryClient discoveryClient;
    @Autowired
    private UserControllerApi userControllerApi;

    //Use remote call to get userInfo
    private List<AppUserVO> getPublisherList(Set idSet) {

        String serviceId = "SERVICE-USER";
//        List<ServiceInstance> instanceList = discoveryClient.getInstances(serviceId);
//        ServiceInstance userService = instanceList.get(0);
//        String userServerUrlExecute
//                = "http://"+serviceId+
//                "/user/queryByIds?userIds="+JsonUtils.objectToJson(idSet);

        GraceJSONResult bodyResult = userControllerApi.queryByIds(JsonUtils.objectToJson(idSet));
//        String userServerUrlExecute
//                = "http://"+userService.getHost()+":"+userService.getPort()+
//                "/user/queryByIds?userIds="+JsonUtils.objectToJson(idSet);


        //2.Initiate resttemplate to initiate a remote call, request user services, and get a list of users

//        String userServerUrlExecute
//                = "http://127.0.0.1:8003/user/queryByIds?userIds="+JsonUtils.objectToJson(idSet);
//        ResponseEntity<GraceJSONResult> resultResponseEntity
//                = restTemplate.getForEntity(userServerUrlExecute,GraceJSONResult.class);
        //get query results
//        GraceJSONResult bodyResult = resultResponseEntity.getBody();
        List<AppUserVO> publishList = null;
        if (bodyResult.getStatus() == 200) {
            String userJson = JsonUtils.objectToJson(bodyResult.getData());
            publishList = JsonUtils.jsonToList(userJson,AppUserVO.class);
        } else {
            publishList = new ArrayList<>();
        }
        return publishList;
    }

    @Override
    public GraceJSONResult readArticle(String articleId,
                                       HttpServletRequest request) {
        //Prevent reading counts from being brushed
        String userIp = IPUtil.getRequestIp(request);
        redis.setnx(REDIS_ALREADY_READ+":"+articleId+":"+userIp,userIp);
        redis.increment(REDIS_ARTICLE_READ_COUNTS+":"+articleId,1);
        return GraceJSONResult.ok();
    }


    /**
     * Get the reading volume from redis,
     * get the author's nickname and avatar by remote call, and reorganize the article information
     */
    private PagedGridResult rebuildArticleGrid(PagedGridResult gridResult) {
        // START

        List<ArticleEO> list = (List<ArticleEO>)gridResult.getRows();

        // 1. Build a list of publisher ids
        Set<String> idSet = new HashSet<>();
        List<String> idList = new ArrayList<>();
        for (ArticleEO a : list) {
            // 1.1 ??????????????????set
            idSet.add(a.getPublishUserId());
            // 1.2 ????????????id???list
            idList.add(REDIS_ARTICLE_READ_COUNTS + ":" + a.getId());
        }
        // ??????redis???mget????????????api?????????????????????
        List<String> readCountsRedisList = redis.mget(idList);
        //???????????????????????????userInfo
        List<AppUserVO> publisherList = getPublisherList(idSet);

        // 3. ????????????list?????????????????????
        List<IndexArticleVO> indexArticleList = new ArrayList<>();

        for (int i = 0 ; i < list.size() ; i ++) {
            IndexArticleVO indexArticleVO = new IndexArticleVO();
            ArticleEO a = list.get(i);
            BeanUtils.copyProperties(a, indexArticleVO);

            // 3.1 ???publisherList?????????????????????????????????
            AppUserVO publisher  = getUserIfPublisher(a.getPublishUserId(), publisherList);
            indexArticleVO.setPublisherVO(publisher);

            // 3.2 ?????????????????????????????????????????????
            String redisCountsStr = readCountsRedisList.get(i);
            int readCounts = 0;
            if (StringUtils.isNotBlank(redisCountsStr)) {
                readCounts = Integer.valueOf(redisCountsStr);
            }
            indexArticleVO.setReadCounts(readCounts);

            indexArticleList.add(indexArticleVO);
        }


        gridResult.setRows(indexArticleList);
// END
        return gridResult;
    }
    private AppUserVO getUserIfPublisher(String publisherId,
                                         List<AppUserVO> publisherList) {
        for (AppUserVO user : publisherList) {
            if (user.getId().equalsIgnoreCase(publisherId)) {
                return user;
            }
        }
        return null;
    }


}

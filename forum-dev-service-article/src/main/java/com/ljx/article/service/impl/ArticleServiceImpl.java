package com.ljx.article.service.impl;

import com.github.pagehelper.PageHelper;
import com.ljx.api.config.RabbitMQDelayConfig;
import com.ljx.api.service.BaseService;
import com.ljx.article.mapper.ArticleMapper;
import com.ljx.article.mapper.ArticleMapperCustom;
import com.ljx.article.service.ArticleService;
import com.ljx.enums.ArticleAppointType;
import com.ljx.enums.ArticleReviewStatus;
import com.ljx.enums.YesOrNo;
import com.ljx.exception.GraceException;
import com.ljx.grace.result.ResponseStatusEnum;
import com.ljx.pojo.Article;
import com.ljx.pojo.Category;
import com.ljx.pojo.bo.NewArticleBO;
import com.ljx.pojo.eo.ArticleEO;
import com.ljx.utils.PagedGridResult;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;


@Service
public class ArticleServiceImpl extends BaseService implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private Sid sid;
    @Autowired
    private ArticleMapperCustom articleMapperCustom;
    //@Autowired
    //private RabbitTemplate rabbitTemplate;
    //@Autowired
    //private ElasticsearchTemplate elasticsearchTemplate;

    @Transactional
    @Override
    public void createArticle(NewArticleBO newArticleBO, Category category) {
        String articleId = sid.nextShort();
        Article  article = new Article();
        BeanUtils.copyProperties(newArticleBO,article);
        article.setId(articleId);
        article.setCategoryId(category.getId());
        //??????????????????:????????????
        article.setArticleStatus(ArticleReviewStatus.REVIEWING.type);
        article.setCommentCounts(0);
        article.setReadCounts(0);
        article.setIsDelete(YesOrNo.NO.type);
        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());
        //??????????????????
        //?????????????????????????????????
        if (newArticleBO.getIsAppoint() == ArticleAppointType.TIMING.type) {
            article.setPublishTime(newArticleBO.getPublishTime());
        } else if (newArticleBO.getIsAppoint() == ArticleAppointType.IMMEDIATELY.type) {
            article.setPublishTime(new Date());
        }
        int res = articleMapper.insert(article);
        if (res != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_CREATE_ERROR);
        }
        //?????????????????????rabbitmq????????????????????????????????????????????????????????????????????????????????????ms
//        if (article.getIsAppoint() == ArticleAppointType.TIMING.type) {
//            //??????????????????-????????????=????????????
//            Date futureDate = newArticleBO.getPublishTime();
//            Date nowDate = new Date();
//            int delayTimes = (int)(futureDate.getTime() - nowDate.getTime());
//            //System.out.println(delayTimes);
//
//            MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {
//                @Override
//                public Message postProcessMessage(Message message) throws AmqpException {
//                    // ?????????????????????
//                    message.getMessageProperties()
//                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
//                    // ????????????????????????????????????ms??????
//                    message.getMessageProperties()
//                            .setDelay(delayTimes);
//                    return message;
//                }
//            };
//            //??????????????????id???????????????
//            rabbitTemplate.convertAndSend(
//                    RabbitMQDelayConfig.EXCHANGE_DELAY,
//                    "publish.delay.do",
//                    articleId,
//                    messagePostProcessor);
//
//            System.out.println("????????????????????????????????????" + new Date());
//        }

        //TODO??????AI??????????????????
        //??????AI?????????????????????????????????
        this.updateArticleStatus(articleId,ArticleReviewStatus.WAITING_MANUAL.type);
    }
    @Transactional
    @Override
    public void updateArticleStatus(String articleId, Integer pendingStatus) {
        Example example = new Example(Article.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id",articleId);
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if(pendingStatus.equals(3))
        criteria.andLessThanOrEqualTo("publishTime",new Date());

        Article article = new Article();
        //????????????
        article.setArticleStatus(pendingStatus);
        int res = articleMapper.updateByExampleSelective(article,example);
        if (res != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_REVIEW_ERROR);
        }
        //??????????????????????????????artilce?????????????????????????????????es???
//        if (pendingStatus == ArticleReviewStatus.SUCCESS.type) {
//            Article result = articleMapper.selectByPrimaryKey(articleId);
//            //????????????????????????????????????????????????es
//            if (result.getIsAppoint() == ArticleAppointType.IMMEDIATELY.type) {
//                ArticleEO articleEO = new ArticleEO();
//                //???article?????????????????????eo???
//                BeanUtils.copyProperties(result,articleEO);
//                IndexQuery iq = new IndexQueryBuilder().withObject(articleEO).build();
//                //??????document
//                elasticsearchTemplate.index(iq);
//            }
//
//        }

    }

    @Transactional
    @Override
    public void updateAppointTopublish() {
        articleMapperCustom.updateAppointToPublish();
    }

    /**
     * ????????????????????????????????????mysql???????????????????????????????????????appoint?????????0
     */
    @Transactional
    @Override
    public void updateArticleTopublish(String articleId) {
        Article article = new Article();
        article.setId(articleId);
        //???????????????????????????
        article.setIsAppoint(ArticleAppointType.IMMEDIATELY.type);
        articleMapper.updateByPrimaryKeySelective(article);
    }

    @Override
    public PagedGridResult queryMyArticleList(String userId,
                                              String keyword,
                                              Integer status,
                                              Date startDate,
                                              Date endDate,
                                              Integer page,
                                              Integer pageSize) {
        Example example = new Example(Article.class);
        example.orderBy("createTime").desc();
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("publishUserId",userId);
        if (StringUtils.isNotBlank(keyword)) {
            //????????????????????????
            criteria.andLike("title","%"+keyword+"%");
        }
        if (ArticleReviewStatus.isArticleStatusValid(status)) {
            criteria.andEqualTo("articleStatus",status);
        }
        if (status != null && status == 12) {
            criteria.andEqualTo("articleStatus",ArticleReviewStatus.REVIEWING.type)
                    .orEqualTo("articleStatus",ArticleReviewStatus.WAITING_MANUAL.type);
        }
        criteria.andEqualTo("isDelete",YesOrNo.NO.type);
        if (startDate != null) {
            criteria.andGreaterThanOrEqualTo("publishTime",startDate);
        }
        if (endDate != null) {
            criteria.andLessThanOrEqualTo("publishTime",endDate);
        }
        //????????????
        PageHelper.startPage(page,pageSize);
        List<Article> list = articleMapper.selectByExample(example);
        return setterPagedGrid(list,page);
    }

    @Override
    public PagedGridResult queryAllArticleList(Integer status, Integer page, Integer pageSize) {
        Example example = new Example(Article.class);
        example.orderBy("createTime").desc();
        Example.Criteria criteria = example.createCriteria();
        //????????????????????????
        if (ArticleReviewStatus.isArticleStatusValid(status)) {
            criteria.andEqualTo("articleStatus",status);
        }
        //????????????1 2????????????
        if (status != null && status == 12) {
            criteria.andEqualTo("articleStatus",ArticleReviewStatus.REVIEWING.type)
                    .orEqualTo("articleStatus",ArticleReviewStatus.WAITING_MANUAL.type);
        }
        //??????????????????????????????????????????????????????
        criteria.andEqualTo("isDelete",YesOrNo.NO.type);
        //????????????
        PageHelper.startPage(page,pageSize);
        List<Article> list = articleMapper.selectByExample(example);
        return setterPagedGrid(list,page);
    }

    @Override
    @Transactional
    public void delete(String userId, String articleId) {
        Example articleExample = makeExampleCriteria(userId, articleId);

        Article pending = new Article();
        pending.setIsDelete(YesOrNo.YES.type);
        //???mysql??????????????????????????????
        int result = articleMapper.updateByExampleSelective(pending, articleExample);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_DELETE_ERROR);
        }
        //???es?????????delete??????
        //elasticsearchTemplate.delete(ArticleEO.class,articleId);
    }
    @Transactional
    @Override
    public void withdraw(String userId, String articleId) {
        Example articleExample = makeExampleCriteria(userId, articleId);
        Article pending = new Article();
        pending.setArticleStatus(ArticleReviewStatus.WITHDRAW.type);
        int result = articleMapper.updateByExampleSelective(pending, articleExample);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.ARTICLE_WITHDRAW_ERROR);
        }
        //???es?????????delete??????
        //elasticsearchTemplate.delete(ArticleEO.class,articleId);
    }

    private Example makeExampleCriteria(String userId, String articleId) {
        Example articleExample = new Example(Article.class);
        Example.Criteria criteria = articleExample.createCriteria();
        criteria.andEqualTo("publishUserId", userId);
        criteria.andEqualTo("id", articleId);
        return articleExample;
    }
}

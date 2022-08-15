package com.ljx.article.controller;

import com.ljx.api.BaseController;
import com.ljx.api.controller.article.CommentControllerApi;
import com.ljx.article.service.CommentPortalService;
import com.ljx.grace.result.GraceJSONResult;
import com.ljx.pojo.bo.CommentReplyBO;
import com.ljx.utils.PagedGridResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.ljx.api.service.BaseService.REDIS_ARTICLE_COMMENT_COUNTS;

@RestController
public class CommentController extends BaseController implements CommentControllerApi {

    final static Logger logger = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CommentPortalService commentPortalService;

    @Override
    public GraceJSONResult createComment(@Valid CommentReplyBO commentReplyBO, BindingResult result) {

        System.out.println(commentReplyBO.toString());

        // 0. 判断BindingResult是否保存错误的验证信息，如果有，则直接return
        if (result.hasErrors()) {
            Map<String, String> errorMap = getErrors(result);
            return GraceJSONResult.errorMap(errorMap);
        }

        // 1. 根据留言用户id查询他的昵称，用于冗余存入留言记录，避免多表管理查询的性能开支
        String userId = commentReplyBO.getCommentUserId();


        // 2. 发起restTemplate调用获得用户基本信息
        Set<String> idSet = new HashSet<>();
        idSet.add(userId);
        String nickname = getBasicUserList(idSet).get(0).getNickname();
        String face = getBasicUserList(idSet).get(0).getFace();

        // 3. 保存评论信息
        commentPortalService.createComment(commentReplyBO.getArticleId(),
                commentReplyBO.getFatherId(),
                commentReplyBO.getContent(),
                userId,
                nickname,face);

        return GraceJSONResult.ok();
    }
    @Override
    public GraceJSONResult commentCounts(String articleId) {

        Integer counts =
                getCountsFromRedis(REDIS_ARTICLE_COMMENT_COUNTS + ":" + articleId);

        return GraceJSONResult.ok(counts);
    }
    @Override
    public GraceJSONResult list(String articleId, Integer page, Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGESIZE;
        }

        PagedGridResult gridResult =
                commentPortalService.queryArticleComments(articleId,
                        page,
                        pageSize);
        return GraceJSONResult.ok(gridResult);
    }
    @Override
    public GraceJSONResult mng(String writerId, Integer page, Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGESIZE;
        }

        PagedGridResult gridResult = commentPortalService.queryWriterCommentsMng(writerId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }

    @Override
    public GraceJSONResult delete(String writerId, String commentId) {
        commentPortalService.deleteComment(writerId, commentId);
        return GraceJSONResult.ok();
    }

}

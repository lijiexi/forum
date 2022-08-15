package com.ljx.article.service;


import com.ljx.utils.PagedGridResult;

import java.util.List;

public interface CommentPortalService {

    /**
     * create comment
     */
    public void createComment(String articleId,
                              String fatherCommentId,
                              String content,
                              String userId,
                              String nickname,
                              String face);
    /**
     * search all comments list
     */
    public PagedGridResult queryArticleComments(String articleId,
                                                Integer page,
                                                Integer pageSize);

    /**
     * 查询我的评论管理列表
     */
    public PagedGridResult queryWriterCommentsMng(String writerId, Integer page, Integer pageSize);

    /**
     * 删除评论
     */
    public void deleteComment(String writerId, String commentId);
}

package com.ljx.api.controller.article;


import com.ljx.grace.result.GraceJSONResult;
import com.ljx.pojo.bo.CommentReplyBO;
import com.ljx.pojo.bo.NewArticleBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

@Api(value = "comment's controller", tags = {"comment's controller"})
@RequestMapping("comment")
public interface CommentControllerApi {
    @PostMapping("/createComment")
    @ApiOperation(value = "user comment", notes = "user comment", httpMethod = "POST")
    public GraceJSONResult createComment(@RequestBody @Valid CommentReplyBO commentReplyBO, BindingResult result);

    @GetMapping("counts")
    @ApiOperation(value = "comment count", notes = "comment count", httpMethod = "GET")
    public GraceJSONResult commentCounts(@RequestParam String articleId);

    @GetMapping("list")
    @ApiOperation(value = "search all comments", notes = "search all comments", httpMethod = "GET")
    public GraceJSONResult list(@RequestParam String articleId,
                                @ApiParam(name = "page", value = "查询下一页的第几页", required = false)
                                @RequestParam Integer page,
                                @ApiParam(name = "pageSize", value = "count per page", required = false)
                                @RequestParam Integer pageSize);

    @PostMapping("mng")
    @ApiOperation(value = "查询我的评论管理列表", notes = "查询我的评论管理列表", httpMethod = "POST")
    public GraceJSONResult mng(@RequestParam String writerId,
                               @ApiParam(name = "page", value = "查询下一页的第几页", required = false)
                               @RequestParam Integer page,
                               @ApiParam(name = "pageSize", value = "分页的每一页显示的条数", required = false)
                               @RequestParam Integer pageSize);


    @PostMapping("/delete")
    @ApiOperation(value = "作者删除评论", notes = "作者删除评论", httpMethod = "POST")
    public GraceJSONResult delete(@RequestParam String writerId,
                                  @RequestParam String commentId);

}

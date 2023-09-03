package com.easybbs.controller;

import com.easybbs.controller.base.ABaseController;
import com.easybbs.entity.vo.ResponseVO;
import com.easybbs.service.ForumBoardService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

//板块列表
@RestController
@RequestMapping("/board")
public class ForumBoardController extends ABaseController {
    @Resource
    private ForumBoardService forumBoardService;

    @RequestMapping("/loadBoard")
    public ResponseVO loadBoard(){
        return getSuccessResponseVO(forumBoardService.getBoardTree(null));
    }
}

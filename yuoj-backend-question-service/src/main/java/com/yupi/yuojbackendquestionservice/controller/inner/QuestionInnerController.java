package com.yupi.yuojbackendquestionservice.controller.inner;

import com.yupi.yuojbackendquestionservice.service.QuestionService;
import com.yupi.yuojbackendquestionservice.service.QuestionSubmitService;
import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;


import com.yupi.yuojbackmodel.model.entity.Question;
import com.yupi.yuojbackmodel.model.entity.QuestionSubmit;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/**
 * 该服务仅内部调用
 */
@RestController()
@RequestMapping("/inner")
public class QuestionInnerController implements QuestionFeignClient {

     @Resource
     private QuestionService questionService;

     @Resource
     private QuestionSubmitService questionSubmitService;

    @Override
    @GetMapping("/get/id")
    public Question getQuestionById(@RequestParam("questionId") long questionId){
        return questionService.getById(questionId);
    }

    @Override
    @GetMapping("/question_submit/get/id")
    public QuestionSubmit getQuestionSubmitById(@RequestParam("questionSubmitId") long questionSubmitId){
        return questionSubmitService.getById(questionSubmitId);
    }

    @Override
    @PostMapping("/question_submit/update")
    public boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit){
        return questionSubmitService.updateById(questionSubmit);
    }

    @Override
    @PostMapping("/question_submit/compare_and_set")
    public boolean compareAndSetStatus(@RequestParam("questionSubmitId") long questionSubmitId,
                                       @RequestParam("expectStatus") Integer expectStatus,
                                       @RequestParam("newStatus") Integer newStatus) {
        // 返回受影响行数 > 0 才算抢占成功，这样调用方拿到的一定是"我改成功了"
        return questionSubmitService.compareAndSetStatus(questionSubmitId, expectStatus, newStatus);
    }
}

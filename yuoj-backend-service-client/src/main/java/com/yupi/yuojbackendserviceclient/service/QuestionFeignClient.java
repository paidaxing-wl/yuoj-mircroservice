package com.yupi.yuojbackendserviceclient.service;

import com.yupi.yuojbackmodel.model.entity.Question;
import com.yupi.yuojbackmodel.model.entity.QuestionSubmit;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


/**
* @author lenovo
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2026-09-14 19:51:20
*/
@FeignClient(name = "yuoj-backend-question-service",path = "/api/question/inner")
public interface QuestionFeignClient {


    @GetMapping("/get/id")
    Question getQuestionById(@RequestParam("questionId") long questionId);

    @GetMapping("/question_submit/get/id")
    QuestionSubmit getQuestionSubmitById(@RequestParam("questionSubmitId") long questionSubmitId);

    @PostMapping("/question_submit/update")
    boolean updateQuestionSubmitById(@RequestBody QuestionSubmit questionSubmit);

    /**
     * 状态原子更新（CAS）：只有当前状态等于 expectStatus 时才会更新为 newStatus。
     * <p>
     * 用于判题任务抢占，避免"先查询状态、再无条件更新"的竞态：
     * 两个消费者可能同时读到 WAITING，但只有一个能更新成功。
     *
     * @return true 表示抢占成功
     */
    @PostMapping("/question_submit/compare_and_set")
    boolean compareAndSetStatus(@RequestParam("questionSubmitId") long questionSubmitId,
                                @RequestParam("expectStatus") Integer expectStatus,
                                @RequestParam("newStatus") Integer newStatus);
}

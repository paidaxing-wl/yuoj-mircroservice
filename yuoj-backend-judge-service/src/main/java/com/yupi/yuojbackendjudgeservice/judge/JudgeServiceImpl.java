package com.yupi.yuojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.yupi.yuojbackendjudgeservice.exception.NonRetryableJudgeException;
import com.yupi.yuojbackendjudgeservice.exception.RetryableJudgeException;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.CodeSandBox;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.CodeSandBoxFactory;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.CodeSandboxProperties;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.SandboxUnavailableException;
import com.yupi.yuojbackendjudgeservice.judge.strategy.JudgeContext;

import com.yupi.yuojbackendserviceclient.service.QuestionFeignClient;
import com.yupi.yuojbackmodel.model.codesandbox.ExecuteCodeRequest;
import com.yupi.yuojbackmodel.model.codesandbox.ExecuteCodeResponse;
import com.yupi.yuojbackmodel.model.codesandbox.JudgeInfo;
import com.yupi.yuojbackmodel.model.dto.question.JudgeCase;
import com.yupi.yuojbackmodel.model.entity.Question;
import com.yupi.yuojbackmodel.model.entity.QuestionSubmit;
import com.yupi.yuojbackmodel.model.enums.JudgeInfoMessageEnum;
import com.yupi.yuojbackmodel.model.enums.QuestionSubmitStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 判题服务。
 * <p>
 * CAS 抢占保证重复消费安全；沙箱故障抛可重试异常交给上层重试；永久性失败写入 FAILED 状态。
 *
 * @author lenovo
 */
@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;

    @Resource
    private JudgeManager judgeManager;

    @Resource
    private CodeSandboxProperties codeSandboxProperties;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new NonRetryableJudgeException("提交信息不存在，questionSubmitId=" + questionSubmitId);
        }
        Integer status = questionSubmit.getStatus();

        // 已判完：重复消息，直接返回
        if (QuestionSubmitStatusEnum.SUCCEED.getValue().equals(status)
                || QuestionSubmitStatusEnum.FAILED.getValue().equals(status)) {
            log.info("提交 {} 状态已是 {}，判定为重复消息，直接跳过", questionSubmitId,
                    QuestionSubmitStatusEnum.getEnumByValue(status));
            return questionSubmit;
        }

        // 正在判题：交给重试，不要当未知异常把消息丢掉
        if (QuestionSubmitStatusEnum.RUNNING.getValue().equals(status)) {
            throw new RetryableJudgeException("提交 " + questionSubmitId + " 正在判题中，可能是重复消息或上一次消费中断");
        }

        // CAS 抢占，避免两个消费者同时判同一条
        boolean claimed = questionFeignClient.compareAndSetStatus(questionSubmitId,
                QuestionSubmitStatusEnum.WAITING.getValue(),
                QuestionSubmitStatusEnum.RUNNING.getValue());
        if (!claimed) {
            throw new RetryableJudgeException("提交 " + questionSubmitId + " 抢占失败，可能已被其它消费者处理");
        }

        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            markFailed(questionSubmitId, "题目不存在，questionId=" + questionId);
            throw new NonRetryableJudgeException("题目不存在，questionId=" + questionId);
        }

        List<JudgeCase> judgeCaseList = JSONUtil.toList(question.getJudgeCase(), JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());

        ExecuteCodeResponse executeCodeResponse;
        try {
            CodeSandBox codeSandBox = CodeSandBoxFactory.newInstance(codeSandboxProperties.getType(), codeSandboxProperties);
            ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                    .code(questionSubmit.getCode())
                    .language(questionSubmit.getLanguage())
                    .inputList(inputList)
                    .build();
            executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        } catch (SandboxUnavailableException e) {
            // 沙箱暂时不可用，交给重试，不要把提交永久卡在 RUNNING
            throw new RetryableJudgeException("代码沙箱不可用，提交 " + questionSubmitId + " 将重新入队", e);
        }

        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        judgeContext.setOutputList(executeCodeResponse.getOutList());
        judgeContext.setInputList(inputList);

        JudgeInfo judgeInfo;
        try {
            judgeInfo = judgeManager.doJudge(judgeContext);
        } catch (Exception e) {
            // 判题策略出错
            throw new RetryableJudgeException("判题策略执行失败，提交 " + questionSubmitId, e);
        }
        if (judgeInfo == null) {
            throw new RetryableJudgeException("判题策略未返回结果，提交 " + questionSubmitId);
        }

        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new RetryableJudgeException("判题结果写回失败，提交 " + questionSubmitId);
        }
        log.info("提交 {} 判题完成，结果={}", questionSubmitId, judgeInfo.getMessage());
        return questionFeignClient.getQuestionSubmitById(questionSubmitId);
    }

    private List<String> parseInputList(Question question) {
        return JSONUtil.toList(question.getJudgeCase(), JudgeCase.class).stream()
                .map(JudgeCase::getInput)
                .collect(Collectors.toList());
    }

    /**
     * 永久性失败：写入此前从未被使用的 FAILED 状态
     */
    private void markFailed(long questionSubmitId, String reason) {
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getValue());
        QuestionSubmit update = new QuestionSubmit();
        update.setId(questionSubmitId);
        update.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        update.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        questionFeignClient.updateQuestionSubmitById(update);
        log.error("提交 {} 标记为永久失败，原因={}", questionSubmitId, reason);
    }
}

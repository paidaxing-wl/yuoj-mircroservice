package com.yupi.yuojbackendjudgeservice.judge.codesandbox;

import com.yupi.yuojbackmodel.model.codesandbox.ExecuteCodeRequest;
import com.yupi.yuojbackmodel.model.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码沙箱日志代理：只负责记录请求与响应，异常与重试交给 CodeSandboxExecutor。
 *
 * @author lenovo
 */
@Slf4j
public class CodeSandboxProxy implements CodeSandBox {

    private final CodeSandBox codeSandBox;

    public CodeSandboxProxy(CodeSandBox codeSandBox) {
        this.codeSandBox = codeSandBox;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("代码沙箱请求信息：{}", executeCodeRequest);
        ExecuteCodeResponse executeCodeResponse = codeSandBox.executeCode(executeCodeRequest);
        if (executeCodeResponse == null) {
            log.warn("代码沙箱返回了 null 响应，请求信息：{}", executeCodeRequest);
        } else {
            log.info("代码沙箱响应信息：{}", executeCodeResponse);
        }
        return executeCodeResponse;
    }
}

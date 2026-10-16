package com.yupi.yuojbackendjudgeservice.judge.codesandbox;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.yupi.yuojbackmodel.model.codesandbox.ExecuteCodeRequest;
import com.yupi.yuojbackmodel.model.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 远程代码沙箱。网络异常与空响应统一抛 SandboxUnavailableException，由上层识别为可重试故障。
 *
 * @author lenovo
 */
@Slf4j
public class RemoteCodeSandbox implements CodeSandBox {

    private final CodeSandboxProperties properties;

    public RemoteCodeSandbox(CodeSandboxProperties properties) {
        this.properties = properties;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String url = properties.getRemoteUrl();
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        String responseStr;
        try {
            responseStr = HttpUtil.createPost(url)
                    .header(properties.getAuthHeader(), properties.getAuthSecret())
                    .body(json)
                    .timeout(properties.getHttpTimeoutMs())
                    .execute()
                    .body();
        } catch (Exception e) {
            throw new SandboxUnavailableException("调用远程代码沙箱失败，url=" + url, e);
        }
        if (StrUtil.isBlank(responseStr)) {
            throw new SandboxUnavailableException("远程代码沙箱返回空响应，url=" + url);
        }
        try {
            return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
        } catch (Exception e) {
            throw new SandboxUnavailableException("远程代码沙箱响应无法解析，url=" + url, e);
        }
    }
}

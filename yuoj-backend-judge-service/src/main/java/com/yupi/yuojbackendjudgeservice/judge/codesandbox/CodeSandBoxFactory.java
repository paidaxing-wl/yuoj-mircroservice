package com.yupi.yuojbackendjudgeservice.judge.codesandbox;

import com.yupi.yuojbackendjudgeservice.judge.codesandbox.impl.ExampleCodeSandbox;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.impl.RemoteCodeSandbox;
import com.yupi.yuojbackendjudgeservice.judge.codesandbox.impl.ThirdPartyCodeSandbox;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码沙箱工厂（根据字符串参数创建指定的代码沙箱实例）
 * <p>
 * 现在的创建链路是：具体沙箱 → 日志代理 → 兜底执行器（超时 + 重试 + 错误分类）。
 * 外层拿到的不再是一个"裸"沙箱，任何一次 executeCode 调用都会自动带上兜底能力。
 * <p>
 * 已知局限：分支仍然是 switch，新增沙箱类型必须改这个类，不满足开闭原则。
 * 想彻底解决可以换成 Map 注册表，由 Spring 把所有 CodeSandBox 实现注入进来。
 *
 * @author lenovo
 */
@Slf4j
public class CodeSandBoxFactory {

    /**
     * 创建带兜底能力的代码沙箱
     *
     * @param type       沙箱类型
     * @param properties 沙箱兜底配置
     */
    public static CodeSandBox newInstance(String type, CodeSandboxProperties properties) {
        CodeSandBox codeSandBox = createRawSandbox(type, properties);
        // 日志
        codeSandBox = new CodeSandboxProxy(codeSandBox);
        // 超时 + 重试 + 可重试/永久错误分类
        codeSandBox = new CodeSandboxExecutor(codeSandBox, properties);
        return codeSandBox;
    }

    private static CodeSandBox createRawSandbox(String type, CodeSandboxProperties properties) {
        if (type == null) {
            log.warn("未指定沙箱类型，回退到示例沙箱");
            return new ExampleCodeSandbox();
        }
        switch (type) {
            case "remote":
                return new RemoteCodeSandbox(properties);
            case "thirdParty":
                return new ThirdPartyCodeSandbox();
            case "example":
                return new ExampleCodeSandbox();
            default:
                log.warn("未知的沙箱类型 {}，回退到示例沙箱", type);
                return new ExampleCodeSandbox();
        }
    }
}

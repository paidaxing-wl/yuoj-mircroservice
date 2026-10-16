package com.yupi.yuojbackendjudgeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 判题服务启动类。
 * <p>
 * 交换机与队列由 RabbitMqConfig 以 Bean 形式声明，RabbitAdmin 启动时自动声明，声明失败会直接暴露。
 */
@SpringBootApplication()
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.yupi")  //扫描到全局异常处理器
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.yupi.yuojbackendserviceclient.service"})
public class YuojBackendJudgeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuojBackendJudgeServiceApplication.class, args);
    }

}

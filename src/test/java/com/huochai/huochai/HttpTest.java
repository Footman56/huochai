package com.huochai.huochai;

import com.huochai.huochai.demos.web.User;
import com.huochai.huochai.utils.EnhancedWebClientClient;
import com.huochai.huochai.utils.JsonUtil;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.Data;
import reactor.core.publisher.Mono;

/**
 *
 *@author peilizhi 
 *@date 2026/3/27 14:52
 **/
@Data
@SpringBootTest
public class HttpTest {


    @Test
    public  void test(){
        // 创建客户端（使用默认配置）
        EnhancedWebClientClient client = new EnhancedWebClientClient.Builder()
                .disableSSL(true).build();

        // ========== 同步调用（阻塞） ==========
        // GET 请求
        User user  = client.get("https://127.0.0.1:8443/user", User.class, null, null);

        System.out.println("user = " + JsonUtil.toJson(user, false));



        // ========== 异步调用（响应式） ==========
        // 返回 Mono，可以链式处理
        Mono<User> mono = client.getMono("https://127.0.0.1:8443/user",
                User.class, null, null);
        //
        mono
                .map(res -> res.getName())
                .subscribe(name -> System.out.println("User name: " + name));
    }
}

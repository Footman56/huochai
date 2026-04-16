package com.huochai.web;

import com.huochai.domain.demo.User;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

/**
 *
 *@author peilizhi 
 *@date 2026/4/16 15:15
 **/
@Data

@RestController
@RequestMapping("/user")
@Tag(name = "用户接口", description = "用户相关操作")
public class UserController {


    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public User getUser(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        return new User(id, "test", 12);
    }

    @Operation(summary = "创建用户")
    @PostMapping("/create")
    public User create(
            @RequestBody @Parameter(description = "用户对象") User user) {
        user.setId(222L);
        user.setName("test");
        user.setAge(18);
        return user;
    }


    @GetMapping("/info")
    public String info(Authentication auth) {
        return "当前用户: " + auth.getName();
    }
}

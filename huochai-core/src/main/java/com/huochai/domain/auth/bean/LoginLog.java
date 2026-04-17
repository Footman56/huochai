package com.huochai.domain.auth.bean;

import com.baomidou.mybatisplus.annotation.*;
import com.huochai.common.enums.LoginResult;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体 - 审计
 *
 * @author huochai
 */
@Data
@TableName("sys_login_log")
public class LoginLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 客户端类型
     */
    private String clientType;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 登录地点
     */
    private String location;

    /**
     * 登录结果: 1成功 0失败
     */
    private Integer loginResult;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 登录时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime loginTime;

    // ========== 静态工厂方法 ==========

    /**
     * 创建成功日志
     */
    public static LoginLog success(Long userId, String username, DeviceInfo deviceInfo) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginResult(LoginResult.SUCCESS.getCode());
        fillDeviceInfo(log, deviceInfo);
        return log;
    }

    /**
     * 创建失败日志
     */
    public static LoginLog fail(String username, DeviceInfo deviceInfo, String reason) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setLoginResult(LoginResult.FAIL.getCode());
        log.setFailReason(reason);
        fillDeviceInfo(log, deviceInfo);
        return log;
    }

    /**
     * 填充设备信息
     */
    private static void fillDeviceInfo(LoginLog log, DeviceInfo deviceInfo) {
        if (deviceInfo != null) {
            log.setClientType(deviceInfo.getClientType() != null ? deviceInfo.getClientType().getCode() : null);
            log.setDeviceId(deviceInfo.getDeviceId());
            log.setDeviceName(deviceInfo.getDeviceName());
            log.setOs(deviceInfo.getOs());
            log.setBrowser(deviceInfo.getBrowser());
            log.setIp(deviceInfo.getIp());
            log.setLocation(deviceInfo.getLocation());
        }
    }
}
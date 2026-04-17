package com.huochai.domain.auth.bean;

import com.huochai.common.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备信息 值对象
 *
 * @author huochai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 客户端类型
     */
    private ClientType clientType;

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
     * 创建默认设备信息
     */
    public static DeviceInfo defaultDevice() {
        return DeviceInfo.builder()
                .deviceId("default")
                .clientType(ClientType.WEB)
                .build();
    }

    /**
     * 创建Web端设备信息
     */
    public static DeviceInfo webDevice(String deviceId, String ip) {
        return DeviceInfo.builder()
                .deviceId(deviceId)
                .clientType(ClientType.WEB)
                .ip(ip)
                .build();
    }

    /**
     * 创建App端设备信息
     */
    public static DeviceInfo appDevice(String deviceId, String deviceName) {
        return DeviceInfo.builder()
                .deviceId(deviceId)
                .clientType(ClientType.APP)
                .deviceName(deviceName)
                .build();
    }
}
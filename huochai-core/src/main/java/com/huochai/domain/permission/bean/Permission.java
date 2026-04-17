package com.huochai.domain.permission.bean;

import com.baomidou.mybatisplus.annotation.*;
import com.huochai.common.enums.ResourceType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限值对象
 *
 * @author huochai
 */
@Data
@TableName("sys_permission")
public class Permission {

    /**
     * 权限ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 资源类型: MENU/BUTTON/API
     */
    private String resourceType;

    /**
     * 资源URL
     */
    private String resourceUrl;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 1正常 0禁用
     */
    private Integer status;

    /**
     * 删除标记
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 检查权限是否有效
     */
    public boolean isValid() {
        return this.status != null && this.status == 1 && this.deleted == 0;
    }

    /**
     * 获取资源类型枚举
     */
    public ResourceType getResourceTypeEnum() {
        return ResourceType.fromCode(this.resourceType);
    }
}
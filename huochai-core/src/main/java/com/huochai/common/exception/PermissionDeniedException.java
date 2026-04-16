package com.huochai.common.exception;

/**
 * 权限拒绝异常
 *
 * @author huochai
 */
public class PermissionDeniedException extends AuthException {

    private static final long serialVersionUID = 1L;

    public PermissionDeniedException() {
        super(AuthErrorCode.PERMISSION_DENIED);
    }

    public PermissionDeniedException(String message) {
        super(AuthErrorCode.PERMISSION_DENIED, message);
    }

    public PermissionDeniedException(String resource, String operation) {
        super(AuthErrorCode.PERMISSION_DENIED, 
              String.format("无权限访问资源: %s, 操作: %s", resource, operation));
    }
}
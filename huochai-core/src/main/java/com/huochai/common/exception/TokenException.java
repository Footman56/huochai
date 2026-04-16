package com.huochai.common.exception;

/**
 * Token异常
 *
 * @author huochai
 */
public class TokenException extends AuthException {

    private static final long serialVersionUID = 1L;

    public TokenException() {
        super(AuthErrorCode.TOKEN_INVALID);
    }

    public TokenException(AuthErrorCode errorCode) {
        super(errorCode);
    }

    public TokenException(String message) {
        super(AuthErrorCode.TOKEN_INVALID, message);
    }

    /**
     * Token过期异常
     */
    public static class ExpiredException extends TokenException {
        public ExpiredException() {
            super(AuthErrorCode.TOKEN_EXPIRED);
        }
        
        public ExpiredException(String message) {
            super(AuthErrorCode.TOKEN_EXPIRED);
        }
    }

    /**
     * Token无效异常
     */
    public static class InvalidException extends TokenException {
        public InvalidException() {
            super(AuthErrorCode.TOKEN_INVALID);
        }
        
        public InvalidException(String message) {
            super(message);
        }
    }

    /**
     * Token在黑名单异常
     */
    public static class BlacklistedException extends TokenException {
        public BlacklistedException() {
            super(AuthErrorCode.TOKEN_BLACKLISTED);
        }
    }
}
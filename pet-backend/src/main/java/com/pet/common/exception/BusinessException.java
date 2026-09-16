package com.pet.common.exception;

import com.pet.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常:业务规则不满足时抛出,由全局异常处理器统一转成 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String msg) {
        super(msg);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }
}

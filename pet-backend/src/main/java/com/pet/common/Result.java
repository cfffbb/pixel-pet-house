package com.pet.common;

import lombok.Data;

/**
 * 统一返回格式 {code, msg, data}(M1 拍板:统一包装)
 * 97 个 Controller 方法全部使用此类,无裸返回
 */
@Data
public class Result<T> {

    private int code = ResultCode.SUCCESS.getCode();
    private String msg = ResultCode.SUCCESS.getMsg();
    private T data;

    private Result() {}

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.msg = ResultCode.SUCCESS.getMsg();
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        return fail(ResultCode.ERROR.getCode(), msg);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMsg());
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}

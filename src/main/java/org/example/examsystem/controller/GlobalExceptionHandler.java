package org.example.examsystem.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.examsystem.vo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器（主要用于把 JSON 类型错误提示得更清楚）
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public Result handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
//        Throwable cause = e.getCause();
//        if (cause instanceof InvalidFormatException) {
//            InvalidFormatException ife = (InvalidFormatException) cause;
//            String fieldName = ife.getPath().isEmpty() ? "未知字段" : ife.getPath().get(ife.getPath().size() - 1).getFieldName();
//            return Result.fail("字段 '" + fieldName + "' 类型不正确，请检查请求参数类型");
//        }
//        return Result.fail("请求参数格式错误，请检查 JSON 数据格式");
//    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public Result handleUnauthenticatedException(UnauthenticatedException e) {
        return Result.info(401, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return Result.fail(e.getMessage());
    }
}









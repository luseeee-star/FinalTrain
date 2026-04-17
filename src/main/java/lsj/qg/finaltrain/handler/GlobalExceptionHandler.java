package lsj.qg.finaltrain.handler;

import lsj.qg.finaltrain.utils.ResultJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 标记这个类是一个[全局控制器增强器】，它会监听【整个应用中所有 @RestController 抛出的异常】
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResultJson handleException(Exception e) {
        log.error("全局异常捕获 - 异常类型: {}, 错误信息: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        return ResultJson.systemError();
    }
}
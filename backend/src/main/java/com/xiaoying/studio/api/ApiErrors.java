package com.xiaoying.studio.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.ErrorResponse;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(
                ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason() == null ? "请求失败" : error.getReason()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> tooLarge() { return problem(HttpStatus.PAYLOAD_TOO_LARGE, "每张图片不能超过 5 MB"); }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ProblemDetail> invalidInput() { return problem(HttpStatus.BAD_REQUEST, "请求参数不符合要求，请检查必填项、数量和取值范围"); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception error) {
        if (error instanceof ErrorResponse response && response.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(response.getStatusCode()).body(
                    ProblemDetail.forStatusAndDetail(response.getStatusCode(), "请求路径、方法或内容类型不符合要求"));
        }
        LOG.error("Unhandled API error", error);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时无法完成请求，请稍后重试");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}

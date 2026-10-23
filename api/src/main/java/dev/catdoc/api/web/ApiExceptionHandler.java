package dev.catdoc.api.web;

import dev.catdoc.api.application.ArchiveTooLargeException;
import dev.catdoc.api.application.ImportRejectedException;
import dev.catdoc.api.application.InvalidRequestException;
import dev.catdoc.api.application.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 例外を RFC 9457 の Problem Details にする。
 * 足りないパラメータや UUID の形式違反、送信の大きさの超過は、親クラスが 400 や 413 にする。
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler
    ProblemDetail handle(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidRequestException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handle(ArchiveTooLargeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, e.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handle(ImportRejectedException e) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                "The archive has %d error(s)".formatted(e.errors().size()));
        problem.setTitle("Import rejected");
        problem.setProperty("errors", e.errors());
        return problem;
    }

    /** 内部の情報は返さず、ログにだけ残す。 */
    @ExceptionHandler
    ProblemDetail handle(Exception e) {
        log.error("Unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}

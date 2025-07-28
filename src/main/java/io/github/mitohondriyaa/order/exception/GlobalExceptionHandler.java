package io.github.mitohondriyaa.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<Info> handleNotFoundException(
        NotFoundException exception
    ) {
        Info info = new Info(exception.getMessage());

        return new ResponseEntity<>(info, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<Info> handleOutOfStockExceptions(
        OutOfStockException exception
    ) {
        Info info = new Info(exception.getMessage());

        return new ResponseEntity<>(info, HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    public ResponseEntity<Info> handleServiceUnavailableException(
        ServiceUnavailableException exception
    ) {
        Info info = new Info(exception.getMessage());

        return new ResponseEntity<>(info, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler
    public ResponseEntity<Info> handleOtherExceptions(
        Exception exception
    ) {
        Info info = new Info(exception.getMessage());

        return new ResponseEntity<>(info, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
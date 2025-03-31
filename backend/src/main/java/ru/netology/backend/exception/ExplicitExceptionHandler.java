package ru.netology.backend.exception;

import ru.netology.backend.model.Error;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;

@ControllerAdvice
@Slf4j
public class ExplicitExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Error> handleUnauthorizedException(UnauthorizedException ex) {
        log.error("Unauthorized error: {}", ex.getMessage());
        Error error = Error.of(ex.getMessage(), 401);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Error> handleBadRequestException(BadRequestException ex) {
        log.error("Bad request error: {}", ex.getMessage());
        Error error = Error.of(ex.getMessage(), 400);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Error> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.error("File size exceeded: {}", ex.getMessage());
        Error error = Error.of("File size exceeds the maximum allowed size", 400);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Error> handleIOException(IOException ex) {
        log.error("IO error: {}", ex.getMessage());
        Error error = Error.of("File operation failed: " + ex.getMessage(), 500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> handleGlobalException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        Error error = Error.of("Internal server error", 500);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
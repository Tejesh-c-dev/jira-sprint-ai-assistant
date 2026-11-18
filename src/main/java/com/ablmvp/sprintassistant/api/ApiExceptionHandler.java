package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(SprintNotFoundException.class)
    public ResponseEntity<ErrorResponse> sprintNotFound(SprintNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("SPRINT_NOT_FOUND", "Sprint not found: " + exception.sprintId()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", "The request parameters are invalid"));
    }

    @ExceptionHandler(InvalidSprintIdException.class)
    public ResponseEntity<ErrorResponse> invalidSprintId(InvalidSprintIdException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_SPRINT_ID", "The sprint identifier must not be blank"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", "The requested resource was not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected server error occurred"));
    }
}

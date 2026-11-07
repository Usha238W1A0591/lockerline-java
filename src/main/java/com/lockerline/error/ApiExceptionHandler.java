
package com.lockerline.error;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonMappingException;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ProblemDetail refusal(DomainException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(e.status()), e.getMessage());
        problem.setType(URI.create(e.type()));
        problem.setTitle(e.title());
        e.properties().forEach(problem::setProperty);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String field = ex instanceof MethodArgumentTypeMismatchException m
                ? m.getName() : ex.getPropertyName();

        return handleExceptionInternal(
                ex,
                validation(List.of(error(field,
                        "'" + ex.getValue() + "' is not an allowed value"))),
                headers,
                HttpStatus.BAD_REQUEST,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> error(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()))
                .toList();

        return handleExceptionInternal(
                ex, validation(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String field = "body";

        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof JsonMappingException mappingException) {
                List<JsonMappingException.Reference> path = mappingException.getPath();

                if (!path.isEmpty()) {
                    String fieldName = path.get(path.size() - 1).getFieldName();
                    if (fieldName != null) {
                        field = fieldName;
                    }
                }
                break;
            }
            cause = cause.getCause();
        }

        return handleExceptionInternal(
                ex,
                validation(List.of(error(field, "Invalid or unreadable value"))),
                headers,
                HttpStatus.BAD_REQUEST,
                request);
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of(
                "field", String.valueOf(field),
                "message", String.valueOf(message));
    }

    private static ProblemDetail validation(List<Map<String, String>> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request is not valid");

        problem.setType(URI.create("/errors/validation"));
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);

        return problem;
    }
}

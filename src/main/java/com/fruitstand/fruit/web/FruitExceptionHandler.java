package com.fruitstand.fruit.web;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import com.fruitstand.fruit.domain.exception.DuplicateNameException;
import com.fruitstand.fruit.domain.exception.FruitNotFoundException;
import com.fruitstand.fruit.domain.exception.InsufficientStockException;

/**
 * Traduce las excepciones a respuestas HTTP con formato ProblemDetail (RFC 9457).
 * La clase base ya cubre el resto de errores de Spring (método no permitido, tipo de contenido, etc.).
 */
@RestControllerAdvice
public class FruitExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(FruitNotFoundException.class)
	public ProblemDetail notFound(FruitNotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(DuplicateNameException.class)
	public ProblemDetail duplicateName(DuplicateNameException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	/** Respaldo si dos pedidos crean el mismo nombre a la vez y salta uk_fruit_name. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail databaseConflict(DataIntegrityViolationException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The data conflicts with an existing fruit");
	}

	/** Bloqueo optimista: otra petición cambió la fruta entre la lectura y el guardado. */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail concurrentModification(OptimisticLockingFailureException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The fruit was modified by another operation, please try again");
	}

	/** Reglas de la entidad: precio negativo, demasiados decimales, stock insuficiente... */
	@ExceptionHandler({ IllegalArgumentException.class, InsufficientStockException.class })
	public ProblemDetail businessRule(RuntimeException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(),
						(first, second) -> first));
		return invalidData(status, errors);
	}

	/** JSON que no se puede leer: si el problema es un campo (p. ej. "unit": "LB"), se dice cuál. */
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (!(e.getCause() instanceof MismatchedInputException cause) || cause.getPath().isEmpty()) {
			return ResponseEntity.status(status)
					.body(ProblemDetail.forStatusAndDetail(status, "Request body is missing or malformed"));
		}
		String field = cause.getPath().stream()
				.map(JacksonException.Reference::getPropertyName)
				.filter(Objects::nonNull)
				.collect(Collectors.joining("."));
		return invalidData(status, Map.of(field, invalidValueMessage(cause)));
	}

	private static ResponseEntity<Object> invalidData(HttpStatusCode status, Map<String, String> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Invalid data");
		problem.setProperty("errors", errors);
		return ResponseEntity.status(status).body(problem);
	}

	private static String invalidValueMessage(MismatchedInputException e) {
		Class<?> type = e.getTargetType();
		if (type != null && type.isEnum()) {
			String accepted = Arrays.stream(type.getEnumConstants()).map(Object::toString)
					.collect(Collectors.joining(", "));
			Object value = e instanceof InvalidFormatException invalid ? invalid.getValue() : null;
			return "invalid value '" + value + "', accepted values: " + accepted;
		}
		if (type != null && Number.class.isAssignableFrom(type)) {
			return "must be a number";
		}
		return "invalid value";
	}

}

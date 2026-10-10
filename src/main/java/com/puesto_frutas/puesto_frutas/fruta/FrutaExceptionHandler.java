package com.puesto_frutas.puesto_frutas.fruta;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a respuestas HTTP con formato ProblemDetail (RFC 9457).
 * La clase base ya cubre los errores de Spring (JSON mal formado, unidad inválida, etc.).
 */
@RestControllerAdvice
public class FrutaExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(FrutaNoEncontradaException.class)
	public ProblemDetail noEncontrada(FrutaNoEncontradaException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(NombreDuplicadoException.class)
	public ProblemDetail nombreDuplicado(NombreDuplicadoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	/** Respaldo si dos pedidos crean el mismo nombre a la vez y salta uk_fruta_nombre. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail conflictoEnBaseDeDatos(DataIntegrityViolationException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Los datos chocan con otra fruta existente");
	}

	/** Bloqueo optimista: otra petición cambió la fruta entre la lectura y el guardado. */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ProblemDetail modificadaALaVez(OptimisticLockingFailureException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"La fruta fue modificada por otra operación, intenta de nuevo");
	}

	/** Reglas de la entidad: precio negativo, demasiados decimales, stock insuficiente... */
	@ExceptionHandler({ IllegalArgumentException.class, StockInsuficienteException.class })
	public ProblemDetail reglaDeNegocio(RuntimeException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "Datos inválidos");
		Map<String, String> errores = e.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(),
						(primero, segundo) -> primero));
		problema.setProperty("errores", errores);
		return ResponseEntity.status(status).body(problema);
	}

}

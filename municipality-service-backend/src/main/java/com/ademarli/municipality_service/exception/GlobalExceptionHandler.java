package com.ademarli.municipality_service.exception;

import com.ademarli.municipality_service.model.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;



@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException e, HttpServletRequest req) {
        String code = e.getCode();
        String msg = resolveMessage(code, e.getMessage());
        return build(HttpStatus.NOT_FOUND, code, msg, req.getRequestURI(), null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException e, HttpServletRequest req) {
        String code = e.getCode();
        String msg = resolveMessage(code, e.getMessage());
        return build(HttpStatus.CONFLICT, code, msg, req.getRequestURI(), null);
    }

    // eski yerler kırılmasın diye kalsın (sonra yavaş yavaş kaldırırsın)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", req.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Invalid JSON body", req.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY", "Data integrity violation", req.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException e, HttpServletRequest req) {
        e.printStackTrace();
        String code = e.getMessage();
        String msg = resolveMessage(code, "Forbidden");
        return build(HttpStatus.FORBIDDEN, code, msg, req.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception e, HttpServletRequest req) {
        log.error("[UNHANDLED] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.toString(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR", "Unexpected server error", req.getRequestURI(), null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, String path, Map<String, String> fieldErrors) {
        ApiError err = new ApiError(status.value(), status.getReasonPhrase(), code, message, path);
        err.setFieldErrors(fieldErrors);
        return ResponseEntity.status(status).body(err);
    }

    private String resolveMessage(String code, String fallback) {
        if (code == null) return fallback;
        String mapped = ERROR_MESSAGES.get(code);
        if (mapped != null) return mapped;

        if (fallback == null || fallback.isBlank() || fallback.equals(code)) return code;
        return fallback;
    }

    private static final Map<String, String> ERROR_MESSAGES = Map.ofEntries(
            Map.entry("USER_NOT_FOUND", "Bu e-posta/telefon ile kayıtlı kullanıcı bulunamadı."),
            Map.entry("INVALID_PASSWORD", "Şifre hatalı."),
            Map.entry("INVALID_CREDENTIALS", "Giriş bilgileri hatalı."),
            Map.entry("EMAIL_ALREADY_IN_USE", "Bu e-posta zaten kullanılıyor."),
            Map.entry("PHONE_ALREADY_IN_USE", "Bu telefon numarası zaten kullanılıyor."),
            Map.entry("USER_DISABLED", "Kullanıcı pasif durumda."),

            Map.entry("CATEGORY_NOT_FOUND", "Kategori bulunamadı."),
            Map.entry("COMPLAINT_NOT_FOUND", "Şikayet bulunamadı."),
            Map.entry("CATEGORY_NOT_ACTIVE", "Kategori aktif değil."),
            Map.entry("CATEGORY_HAS_NO_DEFAULT_DEPARTMENT", "Kategori için varsayılan departman tanımlı değil."),
            Map.entry("DEFAULT_DEPARTMENT_NOT_ACTIVE", "Varsayılan departman aktif değil."),
            Map.entry("COMPLAINT_ALREADY_CLOSED", "Bu şikayet zaten kapatılmış."),
            Map.entry("PUBLIC_ANSWER_REQUIRED_ON_RESOLVED", "Çözüldü durumuna geçmek için cevap zorunludur."),
            Map.entry("PUBLIC_ANSWER_ONLY_ALLOWED_ON_RESOLVED", "Cevap sadece 'RESOLVED' durumunda verilebilir."),
            Map.entry("COMPLAINT_HAS_NO_DEPARTMENT", "Şikayetin departmanı yok."),
            Map.entry("TRACKING_CODE_GENERATION_FAILED", "Takip kodu üretilemedi, tekrar deneyin."),

            Map.entry("ONLY_STAFF_CAN_CHANGE_STATUS", "Sadece personel şikayet durumunu değiştirebilir."),
            Map.entry("NOT_A_MEMBER_OF_THIS_DEPARTMENT", "Bu departmanda yetkiniz yok."),
            Map.entry("NOT_OWNER", "Bu kaynağa erişim yetkiniz yok."),

            Map.entry("DEPARTMENT_NOT_FOUND", "Departman bulunamadı."),
            Map.entry("DEPARTMENT_NOT_ACTIVE", "Departman aktif değil."),
            Map.entry("DEPARTMENT_NAME_ALREADY_EXISTS", "Departman adı zaten mevcut."),
            Map.entry("CATEGORY_NAME_ALREADY_EXISTS", "Kategori adı zaten mevcut."),
            Map.entry("DEFAULT_DEPARTMENT_NOT_FOUND", "Varsayılan departman bulunamadı."),

            Map.entry("INVALID_STATUS_TRANSITION", "Geçersiz durum geçişi.")
    );
}

package br.com.medclinic.presentation;

import br.com.medclinic.application.AccessDenied;
import br.com.medclinic.domain.RuleViolation;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class Errors {
    @ExceptionHandler({
        IllegalArgumentException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String badRequest(Exception error, Model model) {
        model.addAttribute("message", "Dados de formulário inválidos. Reabra a página e confira os campos.");
        return "error";
    }

    @ExceptionHandler(AccessDenied.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String denied(AccessDenied error, Model model) {
        model.addAttribute("message", error.getMessage());
        return "error";
    }

    @ExceptionHandler(RuleViolation.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String invalid(RuleViolation error, Model model) {
        model.addAttribute("message", error.getMessage());
        return "error";
    }

    @ExceptionHandler({DataAccessException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    String unavailable(Exception error, Model model) {
        model.addAttribute(
                "message", "Operação indisponível ou em conflito. Reabra a ficha antes de tentar novamente.");
        return "error";
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String tooLarge(Model model) {
        model.addAttribute("message", "O arquivo deve ter no máximo 10 MiB.");
        return "error";
    }
}

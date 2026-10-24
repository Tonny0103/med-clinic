package br.com.medclinic.domain;

public class RuleViolation extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public final String field;

    public RuleViolation(String field, String message) {
        super(message);
        this.field = field;
    }
}

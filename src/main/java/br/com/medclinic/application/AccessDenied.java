package br.com.medclinic.application;

public class AccessDenied extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AccessDenied() {
        super("Seu perfil não permite esta operação.");
    }
}

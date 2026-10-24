package br.com.medclinic.application;

@FunctionalInterface
public interface FileScanner {
    void scan(byte[] content) throws java.io.IOException;
}

package br.com.medclinic.application;

public interface PrivateFiles {
    Prepared prepare(byte[] content, String declaredType);

    byte[] load(String key);

    record Prepared(String storageKey, String mediaType, long size, String digest) {}
}

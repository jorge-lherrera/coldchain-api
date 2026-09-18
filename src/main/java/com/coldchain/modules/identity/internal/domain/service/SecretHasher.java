package com.coldchain.modules.identity.internal.domain.service;

public interface SecretHasher {

    String hash(String plainSecret);

    boolean matches(String plainSecret, String hash);
}

/*
 * Copyright (c) 2008-2013 Haulmont. All rights reserved.
 * Use is subject to license terms, see http://www.cuba-platform.com/license for details.
 */

package com.haulmont.cuba.core.sys.encryption;

import com.haulmont.cuba.core.global.HashDescriptor;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import javax.annotation.ManagedBean;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.UUID;

/**
 * @author artamonov
 * @version $Id$
 */
@ManagedBean("cuba_Sha1EncryptionModule")
public class Sha1EncryptionModule implements EncryptionModule {

    protected static final String ALGORITHM = "PBKDF2WithHmacSHA1";

    protected static final String RANDOMIZE_ALGORITHM = "SHA1PRNG";
    protected static final int DERIVED_KEY_LENGTH_BITS = 160;

    protected static final int SALT_LENGTH_BYTES = 8;

    protected static final int ITERATIONS = 20000;

    protected static final String STATIC_SALT = "bae5b072f23b2417";

    @Override
    public String getHashMethod() {
        return "sha1";
    }

    @Override
    public HashDescriptor getHash(String content) {
        String salt;
        String result;
        try {
            salt = generateSalt();
            result = apply(content, salt.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new HashDescriptor(result, salt);
    }

    @Override
    public String getPasswordHash(UUID userId, String password) {
        String plainHash = getPlainHash(password);
        return getHash(plainHash, userId.toString());
    }

    @Override
    public String getHash(String content, String salt) {
        if (StringUtils.isEmpty(salt))
            salt = STATIC_SALT;
        String result;
        try {
            result = apply(content, salt.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public String getPlainHash(String content) {
        return getHash(content, STATIC_SALT);
    }

    @Override
    public boolean checkPassword(User user, String password) {
        String hashedPassword = getHash(password, user.getId().toString());
        return StringUtils.equals(hashedPassword, user.getPassword());
    }

    protected String generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(RANDOMIZE_ALGORITHM);
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return new String(Hex.encodeHex(salt));
    }

    protected KeySpec getKeySpec(String content, byte[] salt) {
        return new PBEKeySpec(content.toCharArray(), salt, ITERATIONS, DERIVED_KEY_LENGTH_BITS);
    }

    protected String apply(String content, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec keySpec = getKeySpec(content, salt);

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);

        byte[] encoded = keyFactory.generateSecret(keySpec).getEncoded();
        return new String(Hex.encodeHex(encoded));
    }
}
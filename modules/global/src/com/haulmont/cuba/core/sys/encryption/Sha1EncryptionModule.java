/*
 * Copyright (c) 2012 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.core.sys.encryption;

import com.haulmont.cuba.core.global.HashDescriptor;
import com.haulmont.cuba.core.global.PasswordHashDescriptor;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

/**
 * @author artamonov
 * @version $Id$
 */
public class Sha1EncryptionModule implements EncryptionModule {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";

    private static final String RANDOMIZE_ALGORITHM = "SHA1PRNG";

    private static final int DERIVED_KEY_LENGTH_BITS = 160;

    private static final int SALT_LENGTH_BYTES = 8;

    private static final int ITERATIONS = 20000;

    private static final String STATIC_SALT = "bae5b072f23b2417";

    @Override
    public HashDescriptor getHash(String content) {
        byte[] salt;
        String result;
        try {
            salt = generateSalt();
            result = apply(content, salt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new HashDescriptor(result, new String(Hex.encodeHex(salt)));
    }

    @Override
    public HashDescriptor getPasswordHash(String password) {
        String plainHash = getPlainHash(password);
        return new PasswordHashDescriptor(getHash(plainHash), plainHash);
    }

    @Override
    public String getHash(String content, String salt) {
        if (salt == null)
            salt = "";
        String result;
        try {
            result = apply(content, Hex.decodeHex(salt.toCharArray()));
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
    public boolean checkUserAccess(User user, String givenPassword) {
        String hashedPassword = getHash(givenPassword, user.getSalt());
        return StringUtils.equals(hashedPassword, user.getPassword());
    }

    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance(RANDOMIZE_ALGORITHM);
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private KeySpec getKeySpec(String content, byte[] salt) {
        return new PBEKeySpec(content.toCharArray(), salt, ITERATIONS, DERIVED_KEY_LENGTH_BITS);
    }

    private String apply(String content, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec keySpec = getKeySpec(content, salt);

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ALGORITHM);

        byte[] encoded = keyFactory.generateSecret(keySpec).getEncoded();
        return new String(Hex.encodeHex(encoded));
    }
}
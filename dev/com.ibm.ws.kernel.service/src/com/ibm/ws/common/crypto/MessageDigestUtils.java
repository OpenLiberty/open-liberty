/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.common.crypto;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.ConcurrentObjectPool;

public class MessageDigestUtils {

    private static final TraceComponent tc = Tr.register(MessageDigestUtils.class);

    private static final Base64.Encoder JAVA_BASIC_ENCODER = Base64.getEncoder();

    public enum Base64Option {
        // Uses the com.ibm.ws.common.encoder.Base64Coder implement to base 64 encode the bytes
        LIBERTY {
            @Override
            String encode(byte[] bytes) {
                return Base64Coder.base64EncodeToString(bytes);
            }
        },
        // Uses the java.util.Base64.getEncoder() implement to base 64 encode the bytes
        JAVA_BASIC {
            @Override
            String encode(byte[] bytes) {
                return JAVA_BASIC_ENCODER.encodeToString(bytes);
            }
        };
        // If we ever want to support additional get*Encoder methods on Base64, we would have additional enum options

        abstract String encode(byte[] bytes);
    };

    private static class MessageDigestProcessor {
        private final MessageDigest CLONEABLE_MESSAGE_DIGEST;
        private final String algorithm;
        private final ConcurrentObjectPool<MessageDigest> messageDigestPool = new ConcurrentObjectPool<>(50);

        MessageDigestProcessor(String algorithm) throws NoSuchAlgorithmException {
            this.algorithm = algorithm;
            CLONEABLE_MESSAGE_DIGEST = MessageDigest.getInstance(algorithm);
        }

        /**
         * Converts a String to its hashed value using a MessageDigest.
         *
         * @param originalValue String value which may be a password so is marked Sensitive
         * @param charset       Charset to use to get the bytes
         * @param base64option  Base 64 encoding option to use
         * @return
         * @throws NoSuchAlgorithmException
         */
        String getHashedValue(@Sensitive String originalValue, Charset charset, Base64Option base64option) throws NoSuchAlgorithmException {
            byte[] valueBytes = originalValue.getBytes(charset);

            MessageDigest md = getMessageDigest();

            byte[] hashedBytes = md.digest(valueBytes);

            // If an exception happens we are not putting it back into the pool in case the
            // MessageDigest is not in a healthy state
            messageDigestPool.put(md);

            return base64option.encode(hashedBytes);
        }

        /**
         * Use clone() to get a new instance as its approximately 50% faster (as
         * seen in empirical testing), if we can. Worst case scenario is we will
         * create a new one each time.
         *
         * @return A MessageDigest instance, possibly a pooled entry
         * @throws NoSuchAlgorithmException
         */
        @Trivial
        @FFDCIgnore({ CloneNotSupportedException.class })
        private MessageDigest getMessageDigest() throws NoSuchAlgorithmException {

            MessageDigest pooledDigest = messageDigestPool.get();

            if (pooledDigest != null) {
                return pooledDigest;
            }

            /*
             * Try to clone the parent. If we can't, then we'll ignore the FFDC and create a
             * new instance. If the clone fails, which is REALLY unlikely, as we
             * know the SHA MessageDigest is cloneable on IBM and Sun JDKs
             */
            try {
                return (MessageDigest) CLONEABLE_MESSAGE_DIGEST.clone();
            } catch (CloneNotSupportedException cnse) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "CloneNotSupportedException caught while trying to clone MessageDigest with algorithm " + algorithm
                                 + ". This is pretty unlikely, and we need to get details about the JDK which is in use.",
                             cnse);
                }
                return MessageDigest.getInstance(algorithm);
            }
        }
    }

    private static Map<String, MessageDigestProcessor> messageDigestProcessors = new ConcurrentHashMap<>();

    /**
     * Converts a String to its hashed value based off of a MessageDigest for the provided
     * algorithm.
     *
     * This method uses UTF-8 character set to get the bytes from the String that is to be
     * encoded and uses com.ibm.ws.common.encoder.Base64Coder to convert the encoded bytes
     * to get the resulting string.
     *
     * @param originalValue String value which may be a password so is marked Sensitive
     * @param algorithm     MessageDigest algorithm to use
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String getHashedValue(@Sensitive String originalValue, String algorithm) throws NoSuchAlgorithmException {
        return getHashedValue(originalValue, algorithm, StandardCharsets.UTF_8, Base64Option.LIBERTY);
    }

    /**
     * Converts a String to its hashed value based off of a MessageDigest for the provided
     * algorithm.
     *
     * @param originalValue String value which may be a password so is marked Sensitive
     * @param algorithm     MessageDigest algorithm to use
     * @param charset       Charset to use to get the bytes
     * @param base64option  Base 64 encoding option to use
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String getHashedValue(@Sensitive String originalValue, String algorithm, Charset charset, Base64Option base64option) throws NoSuchAlgorithmException {
        MessageDigestProcessor mdProcessor = messageDigestProcessors.get(algorithm);

        if (mdProcessor == null) {
            AtomicReference<NoSuchAlgorithmException> caughtExc = new AtomicReference<>();
            mdProcessor = messageDigestProcessors.computeIfAbsent(algorithm, (k) -> {
                try {
                    return new MessageDigestProcessor(k);
                } catch (NoSuchAlgorithmException e) {
                    caughtExc.set(e);
                    return null;
                }
            });
            if (mdProcessor == null) {
                throw caughtExc.get();
            }
        }

        return mdProcessor.getHashedValue(originalValue, charset, base64option);
    }
}

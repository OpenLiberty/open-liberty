package com.ibm.ws.crypto.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class MessageDigestUtils {

    // Message Digest algorithms
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA256 = "SHA-256";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA384 = "SHA-384";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA512 = "SHA-512";

    /**
     * List of supported Message Digest Algorithms.
     */
    private static final List<String> supportedMessageDigestAlgorithms = Arrays.asList(
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA256,
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA384,
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA512);

    public static String getMessageDigestAlgorithm() {
        return MESSAGE_DIGEST_ALGORITHM_SHA256;
    }

    public static MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        return getMessageDigest(getMessageDigestAlgorithm());
    }

    public static MessageDigest getMessageDigest(String algorithm) throws NoSuchAlgorithmException {
        if (!supportedMessageDigestAlgorithms.contains(algorithm))
            throw new NoSuchAlgorithmException(String.format("Algorithm %s is not supported", algorithm));
        return MessageDigest.getInstance(algorithm);
    }
}

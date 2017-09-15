package com.ibm.ws.crypto.certificateutil;

import java.io.File;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *
 */
public interface DefaultSSLCertificateCreator {

    /*
     * Key algorithm types
     */
    static final String KEYALG_RSA_TYPE = "RSA";
    static final String KEYALG_EC_TYPE = "EC";

    /**
     * keytool's minimum password length is 6, so it was decided to use
     * that as the restriction for all keystores, regardless of how they
     * are created. This minimum value could be higher for different
     * implementations.
     */
    static final int MINIMUM_PASSWORD_LENGTH = 6;
    static final int MINIMUM_VALIDITY = 365;
    static final int DEFAULT_VALIDITY = 365;
    static final int DEFAULT_SIZE = 2048;
    static final String KEYALG = KEYALG_RSA_TYPE;
    static final String SIGALG = "SHA256withRSA";
    static final String DEFAULT_KEYSTORE_TYPE = "JKS";

    /**
     * These constants are not intended to be specified by the caller,
     * but are defined so all implementations use the same settings.
     */
    static final String ALIAS = "default";

    /*
     * Valid keys sizes for a RSA key
     */
    public static final List<Integer> VALID_RSA_KEYSIZE = Arrays.asList(new Integer[] {
                                                                                        512,
                                                                                        1024,
                                                                                        2048,
                                                                                        4096,
                                                                                        8192,
    });

    /*
     * Valid keys sizes for a EC key
     */
    public static final List<Integer> VALID_EC_KEYSIZE = Arrays.asList(new Integer[] {
                                                                                       192,
                                                                                       224,
                                                                                       256,
                                                                                       384,
                                                                                       521,
    });

    /*
     * Valid signature algorithms
     */
    public static final List<String> VALID_SIG_ALG = Arrays.asList(new String[] {
                                                                                  "SHA1withRSA",
                                                                                  "SHA256withRSA",
                                                                                  "SHA1withECDSA",
                                                                                  "SHA256withECDSA",
                                                                                  "SHA384withRSA",
                                                                                  "SHA512withRSA",
                                                                                  "SHA384withECDSA",
                                                                                  "SHA512withECDSA"
    });

    /**
     * Creates a default SSL certificate.
     *
     * @param filePath The valid, complete path on the file system of the keystore to create. e.g. /tmp/key.jks
     * @param password Minimum 6 characters
     * @param validity Minimum 365 days (?)
     * @param subjectDN The subjectDN. Use {@link DefaultSubjectDN} to construct the default value.
     * @param keySize The size of the certificate key. Default is 2048.
     * @return File representing the created keystore
     * @throws CertificateException if the certificate could not be created
     * @throws IllegalArgumentException if an argument violates the minimum required value or if the value is otherwise considered invalid
     */
    File createDefaultSSLCertificate(String filePath, String password, int validity, String subjectDN, int keySize, String sigAlg) throws CertificateException;

}

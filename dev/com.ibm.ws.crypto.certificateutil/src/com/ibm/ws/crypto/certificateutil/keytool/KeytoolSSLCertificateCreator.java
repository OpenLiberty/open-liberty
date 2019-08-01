/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.certificateutil.keytool;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;

/**
 *
 */
public class KeytoolSSLCertificateCreator implements DefaultSSLCertificateCreator {

    /** {@inheritDoc} */
    @Override
    public File createDefaultSSLCertificate(String filePath, String password, int validity, String subjectDN, int keySize, String sigAlg,
                                            String extInfo) throws CertificateException {

        String setKeyStoreType = null;
        KeytoolCommand keytoolCmd = null;

        validateParameters(filePath, password, validity, subjectDN, keySize, sigAlg);

        String keyType = getKeyFromSigAlg(sigAlg);

        if (filePath.lastIndexOf(".") != -1) {
            setKeyStoreType = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());
        }

        if (extInfo == null) {
            extInfo = defaultExtInfo();
        }

        if (!setKeyStoreType.equals("p12") && (!setKeyStoreType.equals(DEFAULT_KEYSTORE_TYPE))) {
            keytoolCmd = new KeytoolCommand(filePath, password, validity, subjectDN, keySize, keyType, sigAlg, setKeyStoreType, extInfo);
        } else {
            keytoolCmd = new KeytoolCommand(filePath, password, validity, subjectDN, keySize, keyType, sigAlg, DEFAULT_KEYSTORE_TYPE, extInfo);
        }

        keytoolCmd.executeCommand();
        File f = new File(filePath);
        if (f.exists()) {
            return f;
        } else {
            throw new CertificateException("KeytoolCommand executed successfully but file does not exist.");
        }
    }

    /**
     * Validate the parameters.
     *
     * @param filePath
     * @param password
     * @param validity
     * @param subjectDN
     */
    private void validateParameters(String filePath, String password, int validity, String subjectDN, int keySize, String sigAlg) {
        if (!validateFilePath(filePath)) {
            throw new IllegalArgumentException("filePath must be a valid filePath within the file system.");
        }
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("The password must be at least " +
                                               MINIMUM_PASSWORD_LENGTH +
                                               " characters in length.");
        }
        if (validity < MINIMUM_VALIDITY) {
            throw new IllegalArgumentException("The validity period must be at least " +
                                               MINIMUM_VALIDITY + " days.");
        }

        List<String> validSigAlg = VALID_SIG_ALG;
        if (!validSigAlg.contains(sigAlg)) {
            throw new IllegalArgumentException("The signagure algorithm values include " + VALID_SIG_ALG);
        }

        String type = getKeyFromSigAlg(sigAlg);
        if (type.equals(KEYALG_RSA_TYPE)) {
            List<Integer> validKeySizes = VALID_RSA_KEYSIZE;
            if (!validKeySizes.contains(keySize)) {
                throw new IllegalArgumentException("The key sizes for an RSA key include " + VALID_RSA_KEYSIZE);
            }
        } else {
            List<Integer> validKeySizes = VALID_EC_KEYSIZE;
            if (!validKeySizes.contains(keySize)) {
                throw new IllegalArgumentException("The key sizes for an EC key include " + VALID_EC_KEYSIZE);
            }
        }

        validateSubjectDN(subjectDN);
    }

    /**
     * The specified filePath must either exist, or in the case the file
     * should be created, its parent directory.
     *
     * @param loc
     * @return
     */
    private boolean validateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("filePath must be a valid filePath within the file system.");
        }

        // Check if the filename exists as a File -- use an absolute file to ensure we have
        // a parent: even if that parent is ${user.dir} ...
        File loc = new File(filePath).getAbsoluteFile();

        return (loc.exists() || loc.getParentFile().exists());
    }

    /**
     * @param subjectDN
     */
    private void validateSubjectDN(String subjectDN) {
        if (subjectDN == null || subjectDN.isEmpty()) {
            throw new IllegalArgumentException("The subject DN must be a valid DN");
        }

        // Validate the subjectDN
        try {
            new LdapName(subjectDN);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("The subject DN must be a valid DN");
        }
    }

    /**
     * @param sigAlg
     * @return
     */
    private String getKeyFromSigAlg(String sigAlg) {

        if (sigAlg.endsWith("ECDSA"))
            return KEYALG_EC_TYPE;
        else
            return KEYALG_RSA_TYPE;
    }

    /**
     * Create the default SAN extension value
     *
     * @param hostName May be {@code null}. If {@code null} an attempt is made to determine it.
     */
    public String defaultExtInfo() {
        String hostname = getHostName();
        String ext = null;

        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
            if (addr != null && addr.toString().startsWith("/"))
                ext = "SAN=ip:" + hostname;
            else {
                // If the hostname start with a digit keytool will not create a SAN with the value
                if (!Character.isDigit(hostname.charAt(0)))
                    ext = "SAN=dns:" + hostname;
            }
        } catch (UnknownHostException e) {
            // use return null and not set SAN if there is an exception here
        }
        return ext;
    }

    /**
     * Get the host name.
     *
     * @return String value of the host name or "localhost" if not able to resolve
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException e) {
            return "localhost";
        }
    }

}

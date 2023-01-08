/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.utils;

import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.service.util.JavaInfo;

/**
 *
 */
public class SAFEncryptionKey {

    private String keyring;
    private String type;
    private String label;

    private static final String racfPass = "password";
    private KeyStore keystore = null;

    // keyring prefixes
    private static final String PREFIX_SAFKEYRING = "safkeyring:";
    private static final String PREFIX_SAFKEYRINGHYBRID = "safkeyringhybrid:";
    private static final String PREFIX_SAFKEYRINGHW = "safkeyringhw:";
    private static final String PREFIX_SAFKEYRINGJCE = "safkeyringjce:";
    private static final String PREFIX_SAFKEYRINGJCEHYBRID = "safkeyringjcehybrid:";
    private static final String PREFIX_SAFKEYRINGJCECCA = "safkeyringjcecca:";

    /**
     * SAF keyring URL pattern with two slashes and no username: "safkeyring://My.Keyring"
     */
    Pattern safKeyringPatternTwoSlashes = Pattern.compile("(safkeyring|(safkeyringhw|safkeyringhybrid|safkeyringjce|safkeyringjcehybrid|safkeyringjcecca))://\\w.*");

    /**
     * SAF keyring URL pattern with three slashes: "safkeyring:///My.Keyring" OR "safkeyring://MyUser1/My.Keyring"
     */
    Pattern safKeyringPatternThreeSlashes = Pattern.compile("(safkeyring|(safkeyringhw|safkeyringhybrid|safkeyringjce|safkeyringjcehybrid|safkeyringjcecca))://.*/\\w.*");

    /**
     * SAF crypto handlers
     */
    private final Map<String, String> handlers = new HashMap<String, String>();

    public SAFEncryptionKey(String saf_keyring, String saf_type, String saf_label) throws Exception {

        if (saf_keyring != null)
            this.keyring = saf_keyring;

        if (saf_type != null)
            this.type = saf_type;

        if (saf_label != null)
            this.label = saf_label;

        if (JavaInfo.majorVersion() < 11) {
            //Starting with Java 11 the JDK does not require handlers to open saf keyrings,  they are built in
            setSAFHandlers();
        }
        validateConfig();

    }

    /*
     * Get the translated message from the UtilityMessages file
     */
    protected String getMessage(String key, Object... args) {
        return CommandUtils.getMessage(key, args);
    }

    /**
     * Populates a map with supported JCE handlers.
     */
    public void setSAFHandlers() {
        handlers.put("safkeyring", "com.ibm.crypto.provider.safkeyring.Handler");
        handlers.put("safkeyringhw", "com.ibm.crypto.hdwrCCA.provider.safkeyring.Handler");
        handlers.put("safkeyringhybrid", "com.ibm.crypto.ibmjcehybrid.provider.safkeyring.Handler");
    }

    private void validateConfig() throws Exception {

        try {

            keystore = loadKeyStore();

            X509Certificate certEntry = null;
            if (keystore != null) {
                if (!keystore.containsAlias(label)) {
                    String msg = getMessage("saf.label.does.not.exist", new Object[] { label, keyring });
                    throw new Exception(msg);
                }

                if (!keystore.isKeyEntry(label)) {
                    String msg = "The " + label + " certificate is not a key entry.  The certificate needs to be a key entry for use as an AES password encryption key.";
                    throw new Exception(msg);
                }

                // Get the certificate to check if it's expired
                certEntry = (X509Certificate) keystore.getCertificate(label);

                if (!checkIfCertDateIsGood(certEntry)) {
                    String msg = getMessage("saf.cert.expired", label);
                    throw new Exception(msg);
                }
            } else {
                String msg = getMessage("saf.keyring.does.not.exist", keyring);
                throw new Exception(msg);
            }

        } catch (KeyStoreException kse) {
            throw kse;
        }
    }

    /**
     * Verify that the certificate is not expired
     *
     * @param cert
     * @return
     */
    private boolean checkIfCertDateIsGood(X509Certificate cert) {
        if (cert != null) {
            long currentTime = System.currentTimeMillis();
            long notAfter = cert.getNotAfter().getTime();

            if (notAfter < currentTime) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Open URL to SAF keyring location
     *
     * @param location
     * @return
     * @throws Exception
     */
    private InputStream openKeyStoreURL() throws Exception {

        // Get the appropriate SAF handler
        URLStreamHandler handler = null;
        URL url = null;

        String effectiveLocation = processKeyringURL(keyring);
        if (JavaInfo.majorVersion() < 11) {

            if (effectiveLocation != null) {
                try {
                    // Get handler
                    String keyringPrefix = effectiveLocation.substring(0, effectiveLocation.indexOf(":"));
                    String jceHandler = handlers.get(keyringPrefix.toLowerCase());

                    handler = (URLStreamHandler) Class.forName(jceHandler).newInstance();

                } catch (Exception e) {
                    String msg = "Failed to set SAF handler associated with the keyring: " + effectiveLocation + ".  Extended error: " + e.getMessage();
                    throw new Exception(msg);
                }
            }

            // Create the URL
            url = new URL(null, effectiveLocation, handler);
        } else {
            url = new URL(effectiveLocation);
        }

        // Open the file.
        InputStream fis = url.openStream();

        return fis;
    }

    /**
     * Load keystore Object
     *
     * @param location
     * @param type
     * @return
     * @throws Exception
     */
    private KeyStore loadKeyStore() throws Exception {
        try {
            KeyStore ks = KeyStore.getInstance(type);
            InputStream is = openKeyStoreURL();

            ks.load(is, racfPass.toCharArray());

            return ks;

        } catch (Exception e) {
            String msg = "An exception occured when loading the " + keyring
                         + " SAF key ring.  No key can be retrieved to use as the AES password encryption key.  Extended error is: " + e.getMessage();
            throw new Exception(msg);
        }
    }

    /**
     * Process SAF keyring URL for correct format
     *
     * @param safKeyringURL
     * @return
     */
    private String processKeyringURL(String safKeyringURL) {
        String processedUrl = null;

        if (safKeyringURL != null) {
            if (safKeyringPatternThreeSlashes.matcher(safKeyringURL).matches()) {
                // The keyring url is already in the correct format
                processedUrl = safKeyringURL;
            } else if (safKeyringPatternTwoSlashes.matcher(safKeyringURL).matches()) {
                // The user specified a url in this format: "safkeyring://My.Keyring". This is supported,
                // but we will want to reformat this to have three slashes: "safkeyring:///My.Keyring".
                int index = safKeyringURL.indexOf("//");
                StringBuffer sb = new StringBuffer(safKeyringURL);
                sb.insert(index, "/");
                processedUrl = sb.toString();
            }

            // Todo: Needs to be made common. This code is repeated in other files,  BaseCommandTask.java, SAFEncryptionKey.java
            //        CollectiveHostAuthInfoImpl.java and KeyStringResolverImpl.java
            //Check the prefix first,  it may need to be converted first
            String replacePrefix = null;
            if (processedUrl != null) {
                if (JavaInfo.majorVersion() >= 11) {
                    if (processedUrl.startsWith(PREFIX_SAFKEYRING))
                        replacePrefix = PREFIX_SAFKEYRINGJCE;
                    else if (processedUrl.startsWith(PREFIX_SAFKEYRINGHYBRID))
                        replacePrefix = PREFIX_SAFKEYRINGJCEHYBRID;
                    else if (processedUrl.startsWith(PREFIX_SAFKEYRINGHW))
                        replacePrefix = PREFIX_SAFKEYRINGJCECCA;
                } else {
                    if (processedUrl.startsWith(PREFIX_SAFKEYRINGJCE))
                        replacePrefix = PREFIX_SAFKEYRING;
                    else if (processedUrl.startsWith(PREFIX_SAFKEYRINGJCEHYBRID))
                        replacePrefix = PREFIX_SAFKEYRINGHYBRID;
                    else if (processedUrl.startsWith(PREFIX_SAFKEYRINGJCECCA))
                        replacePrefix = PREFIX_SAFKEYRINGHW;
                }

                if (replacePrefix != null) {
                    int index = processedUrl.indexOf(":");
                    String removedPrefix = processedUrl.substring(index + 1);
                    processedUrl = replacePrefix + removedPrefix;
                }
            }
        }

        return processedUrl;
    }

    public String getKey() throws Exception {
        Key key = null;
        PrivateKey privKey = null;

        try {
            if (keystore != null && label != null) {
                key = keystore.getKey(label, racfPass.toCharArray());
                if (key instanceof PrivateKey) {
                    privKey = (PrivateKey) key;
                }
            }
        } catch (Exception e) {
            String msg = "An exception occurred during the " + label + " private key access.  No key can be retrieved for use as the AES password encryption key.  Extended error: "
                         + e.getMessage();
            throw new Exception(msg);
        }

        //Pull the encoded bytes out of the private key object
        byte[] keyBytes = privKey.getEncoded();

        // Make it a string
        String keyString = new String(keyBytes);

        return keyString;

    }

}

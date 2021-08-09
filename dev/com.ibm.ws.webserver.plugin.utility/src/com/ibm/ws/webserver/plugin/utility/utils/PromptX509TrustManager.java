/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webserver.plugin.utility.utils;

import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Custom trust manager which will check the provided managers, and if no
 * trust is established, will prompt for acceptance.
 */
public class PromptX509TrustManager implements X509TrustManager {
    private final Logger logger = Logger.getLogger(PromptX509TrustManager.class.getCanonicalName());

    /** HEX character list */
    private final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private final ConsoleWrapper stdin;
    private final PrintStream stdout;
    private final TrustManager[] trustManagers;
    private final boolean autoAccept;
    private static Map<String, Boolean> answeredCertificates = new HashMap<String, Boolean>();

    /**
     * Clears the cache of certificates which were either accepted or rejected by the user.
     */
    static void clearAnsweredCertificates() {
        answeredCertificates.clear();
    }

    public PromptX509TrustManager(ConsoleWrapper stdin, PrintStream stdout, TrustManager[] trustManagers, boolean autoAccept) {
        this.stdin = stdin;
        this.stdout = stdout;
        this.trustManagers = trustManagers;
        this.autoAccept = autoAccept;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}

    private String getMessage(String key, Object... args) {
        return CommandUtils.getCMessage(key, args);
    }

    /**
     * This method is used to create a "SHA-1" or "MD5" digest on an
     * X509Certificate as the "fingerprint".
     * 
     * @param algorithmName
     * 
     * @param cert
     * 
     * @return String
     */
    private String generateDigest(String algorithmName, X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithmName);
            md.update(cert.getEncoded());
            byte data[] = md.digest();

            StringBuilder buffer = new StringBuilder(3 * data.length);
            int i = 0;
            buffer.append(HEX_CHARS[(data[i] >> 4) & 0xF]);
            buffer.append(HEX_CHARS[(data[i] % 16) & 0xF]);
            for (++i; i < data.length; i++) {
                buffer.append(':');
                buffer.append(HEX_CHARS[(data[i] >> 4) & 0xF]);
                buffer.append(HEX_CHARS[(data[i] % 16) & 0xF]);
            }

            return buffer.toString();
        } catch (NoClassDefFoundError e) {
            return getMessage("sslTrust.genDigestError", algorithmName, e.getMessage());
        } catch (Exception e) {
            return getMessage("sslTrust.genDigestError", algorithmName, e.getMessage());
        }
    }

    boolean isYes(String read) {
        String expectedYesShortResponse = getMessage("yes.response.short");
        String expectedYesFullResponse = getMessage("yes.response.full");
        return ("y".equalsIgnoreCase(read) || "yes".equalsIgnoreCase(read) || // always accept english y and yes.
                (expectedYesShortResponse != null && expectedYesShortResponse.length() > 0 && expectedYesShortResponse.equalsIgnoreCase(read)) || // return true if translated y matches
        (expectedYesFullResponse != null && expectedYesFullResponse.length() > 0 && expectedYesFullResponse.equalsIgnoreCase(read))); // return true if translated yes matches
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Attempting to estalbish trust with target certificate chain:");
            for (int i = 0; i < chain.length; i++) {
                logger.fine("Certificate information [" + i + "]:");
                logger.fine("  Subject DN: " + chain[i].getSubjectDN());
                logger.fine("  Issuer DN: " + chain[i].getIssuerDN());
                logger.fine("  Serial number: " + chain[i].getSerialNumber());
                logger.fine("");
            }
        }

        // If we have trust managers, check them first... maybe we are already trusted!
        if (trustManagers != null && trustManagers.length > 0) {
            for (int i = 0; i < trustManagers.length; i++) {
                TrustManager tm = trustManagers[i];
                if (tm instanceof X509TrustManager) {
                    X509TrustManager x509tm = (X509TrustManager) tm;
                    try {
                        x509tm.checkServerTrusted(chain, authType);
                        // If we're trusted, just stop processing
                        logger.fine("One of the default trust managers trusts the certificate, accepting...");
                        return;
                    } catch (CertificateException e) {
                        // Not trusted, keep trying
                    }
                }
            }
        }
        logger.fine("None of the default trust managers trusts the certificate...");

        // Compute the full MD5 digest. This does not have to be perfectly unique,
        // it just has to be unique enough to know if we've seen this certificate
        // before. If we have, we do not have to prompt again. This entire class
        // will eventually be replaced when we have a client-side SSL solution.
        StringBuilder fullMD5Buf = new StringBuilder();
        for (int i = 0; i < chain.length; i++) {
            fullMD5Buf.append(generateDigest("MD5", chain[i]));
            if (i < (chain.length - 1)) {
                // Still more to go, so append a comma
                fullMD5Buf.append(',');
            }
        }
        final String fullMD5 = fullMD5Buf.toString();

        // If we have already provided an accept answer, do not prompt again
        final Boolean previousAnswer = answeredCertificates.get(fullMD5);
        if (previousAnswer != null) {
            if (previousAnswer) {
                return;
            } else {
                throw new CertificateException(getMessage("sslTrust.rejectTrust"));
            }
        }

        // Check the property to auto-accept certificates
        if (autoAccept) {
            stdout.println();
            stdout.println(getMessage("sslTrust.autoAccept", chain[0].getSubjectDN()));
            stdout.println();
            answeredCertificates.put(fullMD5, Boolean.TRUE);
            return;
        }

        // Lastly, prompt
        stdout.println();
        stdout.println(getMessage("sslTrust.noDefaultTrust"));
        stdout.println();
        stdout.println(getMessage("sslTrust.certInfo"));
        for (int i = 0; i < chain.length; i++) {
            stdout.println(getMessage("sslTrust.cert", "[" + i + "]"));
            stdout.println(getMessage("sslTrust.certSubjectDN", chain[i].getSubjectDN()));
            stdout.println(getMessage("sslTrust.certIssueDN", chain[i].getIssuerDN()));
            stdout.println(getMessage("sslTrust.certSerial", chain[i].getSerialNumber()));
            stdout.println(getMessage("sslTrust.certExpires", chain[i].getNotAfter()));
            stdout.println(getMessage("sslTrust.certSHADigest", generateDigest("SHA-1", chain[i])));
            stdout.println(getMessage("sslTrust.certMD5Digest", generateDigest("MD5", chain[i])));
            stdout.println();
        }

        String read = stdin.readText(getMessage("sslTrust.promptToAcceptTrust"));
        if (isYes(read)) {
            // We are trusted
            answeredCertificates.put(fullMD5, Boolean.TRUE);
        } else {
            answeredCertificates.put(fullMD5, Boolean.FALSE);
            throw new CertificateException(getMessage("sslTrust.rejectTrust"));
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
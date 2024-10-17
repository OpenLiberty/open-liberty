/*******************************************************************************
* Copyright (c) 2024 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package com.ibm.ws.common.crypto;

import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.service.util.JavaInfo;

public class CryptoUtils {
    private static final TraceComponent tc = Tr.register(CryptoUtils.class);

    private static boolean issuedBetaMessage = false;

    static String FIPSLevel = getFipsLevel();
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA256 = "SHA-256";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA384 = "SHA-384";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA512 = "SHA-512";
    public final static String MESSAGE_DIGEST_ALGORITHM_SHA = "SHA";

    public static boolean ibmJCEAvailable = false;
    public static boolean ibmJCEPlusFIPSAvailable = false;
    public static boolean openJCEPlusAvailable = false;
    public static boolean openJCEPlusFIPSAvailable = false;
    public static boolean ibmJCEProviderChecked = false;
    public static boolean ibmJCEPlusFIPSProviderChecked = false;
    public static boolean openJCEPlusProviderChecked = false;
    public static boolean openJCEPlusFIPSProviderChecked = false;

    public static boolean unitTest = false;
    public static boolean fipsChecked = false;

    public static boolean javaVersionChecked = false;
    public static boolean isJava11orHigher = false;

    public static boolean zOSAndJAVA11orHigherChecked = false;
    public static boolean iszOSAndJava11orHigher = false;

    public static String osName = System.getProperty("os.name");
    public static boolean isZOS = false;
    public static boolean osVersionChecked = false;

    public static String IBMJCE_PROVIDER = "com.ibm.crypto.provider.IBMJCE";
    public static String IBMJCE_PLUS_FIPS_PROVIDER = "com.ibm.crypto.plus.provider.IBMJCEPlusFIPS";
    public static String OPENJCE_PLUS_PROVIDER = "com.ibm.crypto.plus.provider.OpenJCEPlus";
    public static String OPENJCE_PLUS_FIPS_PROVIDER = "com.ibm.crypto.plus.provider.OpenJCEPlusFIPS";

    public static final String IBMJCE_NAME = "IBMJCE";
    public static final String IBMJCE_PLUS_FIPS_NAME = "IBMJCEPlusFIPS";
    public static final String OPENJCE_PLUS_NAME = "OpenJCEPlus";
    public static final String OPENJCE_PLUS_FIPS_NAME = "OpenJCEPlusFIPS";

    public static final String SIGNATURE_ALGORITHM_SHA1WITHRSA = "SHA1withRSA";
    public static final String SIGNATURE_ALGORITHM_SHA256WITHRSA = "SHA256withRSA";

    public static final String CRYPTO_ALGORITHM_RSA = "RSA";

    public static final String ENCRYPT_ALGORITHM_DESEDE = "DESede";
    public static final String ENCRYPT_ALGORITHM_RSA = "RSA";

    public static final String AES_GCM_CIPHER = "AES/GCM/NoPadding";
    public static final String DES_ECB_CIPHER = "DESede/ECB/PKCS5Padding";
    public static final String AES_CBC_CIPHER = "AES/CBC/PKCS5Padding";

    private static boolean fipsEnabled = isFIPSEnabled();

    private static Map<String, String> secureAlternative = new HashMap<>();
    static {
        secureAlternative.put("SHA", "SHA256");
        secureAlternative.put("SHA1", "SHA256");
        secureAlternative.put("SHA-1", "SHA256");
        secureAlternative.put("SHA128", "SHA256");
        secureAlternative.put("MD5", "SHA256");
    }

    /**
     * Answers whether a crypto algorithm is considered insecure.
     *
     * @param algorithm The algorithm to check.
     * @return True if the algorithm is considered insecure, false otherwise.
     */
    public static boolean isAlgorithmInsecure(String algorithm) {
        return secureAlternative.containsKey(algorithm);
    }

    /**
     * Returns a secure crypto algorithm to use in place of the given one.
     *
     * @param algorithm The insecure algorithm to be replaced.
     * @return A secure replacement algorithm. If there is none a null is returned.
     */
    public static String getSecureAlternative(String algorithm) {
        return secureAlternative.get(algorithm);
    }

    public static String getSignatureAlgorithm() {
        if (fipsEnabled && (isOpenJCEPlusFIPSAvailable() || isIBMJCEPlusFIPSAvailable()))
            return SIGNATURE_ALGORITHM_SHA256WITHRSA;
        else
            return SIGNATURE_ALGORITHM_SHA1WITHRSA;
    }

    public static String getEncryptionAlgorithm() {
        if (fipsEnabled && (isOpenJCEPlusFIPSAvailable() || isIBMJCEPlusFIPSAvailable()))
            return ENCRYPT_ALGORITHM_RSA;
        else
            return ENCRYPT_ALGORITHM_DESEDE;
    }

    public static String getCipher() {
        String cipher = fipsEnabled ? AES_GCM_CIPHER : DES_ECB_CIPHER;
        return cipher;
    }

    public static void logInsecureAlgorithm(String configProperty, String insecureAlgorithm) {
        // TODO remove beta check
        if (isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE", configProperty, insecureAlgorithm);
        }
    }

    public static void logInsecureAlgorithmReplaced(String configProperty, String insecureAlgorithm, String secureAlgorithm) {
        // TODO remove beta check
        if (isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE_REPLACED", configProperty, insecureAlgorithm, secureAlgorithm);
        }
    }

    public static void logInsecureProvider(String provider, String insecureAlgorithm) {
        // TODO remove beta check
        if (isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE_PROVIDER", provider, insecureAlgorithm);
        }
    }

    public static boolean isIBMJCEAvailable() {
        if (ibmJCEProviderChecked) {
            return ibmJCEAvailable;
        } else {
            ibmJCEAvailable = JavaInfo.isSystemClassAvailable(IBMJCE_PROVIDER);
            ibmJCEProviderChecked = true;
            return ibmJCEAvailable;
        }
    }

    public static boolean isIBMJCEPlusFIPSAvailable() {
        if (ibmJCEPlusFIPSProviderChecked) {
            return ibmJCEPlusFIPSAvailable;
        } else {
            ibmJCEPlusFIPSAvailable = JavaInfo.isSystemClassAvailable(IBMJCE_PLUS_FIPS_PROVIDER);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ibmJCEPlusFIPSProvider: " + IBMJCE_PLUS_FIPS_PROVIDER);
            }

            ibmJCEPlusFIPSProviderChecked = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ibmJCEPlusFIPSAvailable: " + ibmJCEPlusFIPSAvailable);
            }

            if (isRunningBetaMode() && ibmJCEPlusFIPSAvailable) {
                ibmJCEPlusFIPSAvailable = true;
            } else {
                if (fipsEnabled && !isSemeruFips()) {
                    Tr.error(tc, "FIPS is enabled but the " + IBMJCE_PLUS_FIPS_PROVIDER + " provider is not available.");
                }
                ibmJCEPlusFIPSAvailable = false;
            }
            return ibmJCEPlusFIPSAvailable;
        }
    }

    public static boolean isOpenJCEPlusAvailable() {
        if (openJCEPlusProviderChecked) {
            return openJCEPlusAvailable;
        } else {
            openJCEPlusAvailable = JavaInfo.isSystemClassAvailable(OPENJCE_PLUS_PROVIDER);
            openJCEPlusProviderChecked = true;
            return openJCEPlusAvailable;
        }
    }

    public static boolean isOpenJCEPlusFIPSAvailable() {
        if (openJCEPlusFIPSProviderChecked) {
            return openJCEPlusFIPSAvailable;
        } else {
            openJCEPlusFIPSAvailable = JavaInfo.isSystemClassAvailable(OPENJCE_PLUS_FIPS_PROVIDER);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "openJCEPlusFIPSProvider: " + OPENJCE_PLUS_FIPS_PROVIDER);
            }

            openJCEPlusFIPSProviderChecked = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "openJCEPlusFIPSAvailable: " + openJCEPlusFIPSAvailable);
            }

            if (isRunningBetaMode() && openJCEPlusFIPSAvailable) {
                openJCEPlusFIPSAvailable = true;
            } else {
                if (fipsEnabled && isSemeruFips()) {
                    Tr.error(tc, "Semeru FIPS is enabled but the " + OPENJCE_PLUS_FIPS_PROVIDER + " provider is not available.");
                }
                openJCEPlusFIPSAvailable = false;
            }
            return openJCEPlusFIPSAvailable;
        }
    }

    private static boolean isJava11orHigher() {
        if (javaVersionChecked) {
            return isJava11orHigher;
        } else {
            isJava11orHigher = JavaInfo.majorVersion() >= 11;
            javaVersionChecked = true;
            return isJava11orHigher;
        }
    }

    private static boolean isZOS() {
        if (osVersionChecked) {
            return isZOS;
        } else {
            isZOS = (osName.equalsIgnoreCase("z/OS") || osName.equalsIgnoreCase("OS/390"));
            osVersionChecked = true;
            return isZOS;
        }
    }

    public static boolean isZOSandRunningJava11orHigher() {
        if (zOSAndJAVA11orHigherChecked) {
            return iszOSAndJava11orHigher;
        } else {
            iszOSAndJava11orHigher = isJava11orHigher() && isZOS();
            zOSAndJAVA11orHigherChecked = true;
            return iszOSAndJava11orHigher;
        }
    }

    public static String getProvider() {
        String provider = null;
        if (fipsEnabled && isOpenJCEPlusFIPSAvailable()) {
            provider = OPENJCE_PLUS_FIPS_NAME;
        } else if (fipsEnabled && isIBMJCEPlusFIPSAvailable()) {
            provider = IBMJCE_PLUS_FIPS_NAME;
        } else if (isZOSandRunningJava11orHigher() && isOpenJCEPlusAvailable()) {
            provider = OPENJCE_PLUS_NAME;
        } else if (isIBMJCEAvailable()) {
            provider = IBMJCE_NAME;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (provider == null) {
                Tr.debug(tc, "Provider configured by JDK is " + Security.getProviders()[0].getName());
            } else {
                Tr.debug(tc, "Provider configured is " + provider);
            }
        }
        return provider;
    }

    public static final String MESSAGE_DIGEST_ALGORITHM = (fipsEnabled ? MESSAGE_DIGEST_ALGORITHM_SHA256 : MESSAGE_DIGEST_ALGORITHM_SHA);
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

    public static MessageDigest getMessageDigestForLTPA() {
        MessageDigest md1 = null;
        try {
            if (fipsEnabled && isOpenJCEPlusFIPSAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA256,
                                                OPENJCE_PLUS_FIPS_NAME);
            } else if (fipsEnabled && isIBMJCEPlusFIPSAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA256,
                                                IBMJCE_PLUS_FIPS_NAME);
            } else if (isOpenJCEPlusAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA,
                                                OPENJCE_PLUS_NAME);
            } else if (isIBMJCEAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA,
                                                IBMJCE_NAME);
            } else {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA);
            }

        } catch (NoSuchAlgorithmException e) {
            // instrumented ffdc
        } catch (NoSuchProviderException e) {
            // instrumented ffdc;
        }

        return md1;
    }

    public static String getProperty(final String prop, final String defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(prop, defaultValue).toLowerCase();
            }
        });
    }

    static String getFipsLevel() {
        return getProperty("com.ibm.fips.mode", "disabled");
    }

    public static boolean isSemeruFips() {
        return "true".equals(getProperty("semeru.fips", "false"));
    }

    public static boolean isFips140_3Enabled() {

        return isRunningBetaMode() &&
               "140-3".equals(FIPSLevel) || "true".equalsIgnoreCase(getProperty("global.fips_140-3", "false")) || isSemeruFips();
    }

    public static boolean isFips140_2Enabled() {
        return isRunningBetaMode() && "140-2".equals(FIPSLevel);
    }

    public static boolean isFIPSEnabled() {
        if (fipsChecked) {
            return fipsEnabled;
        } else {
            fipsEnabled = isFips140_2Enabled() || isFips140_3Enabled();
            fipsChecked = true;
            return fipsEnabled;
        }
    }

    public static boolean isRunningBetaMode() {

        if (!ProductInfo.getBetaEdition()) {
            return false;
        } else {
            // Running beta exception, issue message if we haven't already issued one for
            // this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class CryptoUtils for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
            return true;
        }
    }
}

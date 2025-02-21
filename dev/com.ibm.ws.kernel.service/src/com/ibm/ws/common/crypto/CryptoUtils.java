/*******************************************************************************
* Copyright (c) 2024, 2025 IBM Corporation and others.
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
import java.security.SecureRandom;
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
    public static boolean fips140_3Checked = false;

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

    public static final String USE_FIPS_PROVIDER = "com.ibm.jsse2.usefipsprovider";
    public static final String USE_FIPS_PROVIDER_NAME = "com.ibm.jsse2.usefipsProviderName";

    public static final String SIGNATURE_ALGORITHM_SHA1WITHRSA = "SHA1withRSA";
    public static final String SIGNATURE_ALGORITHM_SHA256WITHRSA = "SHA256withRSA";
    public static final String SIGNATURE_ALGORITHM_SHA512WITHRSA = "SHA512withRSA";

    public static final String CRYPTO_ALGORITHM_RSA = "RSA";

    public static final String ENCRYPT_ALGORITHM_DESEDE = "DESede";
    public static final String ENCRYPT_ALGORITHM_AES = "AES";

    public static final String ENCRYPT_MODE_ECB = "ECB";

    public static final String AES_GCM_CIPHER = "AES/GCM/NoPadding";
    /** Cipher used for LTPA Password */
    public static final String DES_ECB_CIPHER = "DESede/ECB/PKCS5Padding";
    /** Cipher used for LTPA tokens and audit. */
    public static final String AES_CBC_CIPHER = "AES/CBC/PKCS5Padding";

    public static final int AES_128_KEY_LENGTH_BYTES = 16;
    public static final int AES_256_KEY_LENGTH_BYTES = 32;

    public static final int DESEDE_KEY_LENGTH_BYTES = 24;

    private static boolean fips140_3Enabled = isFips140_3Enabled();
    private static boolean fipsEnabled = fips140_3Enabled;

    /** Algorithm used for encryption in LTPA and audit. */
    public static final String ENCRYPT_ALGORITHM = ENCRYPT_ALGORITHM_AES;

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
            return SIGNATURE_ALGORITHM_SHA512WITHRSA;
        else
            return SIGNATURE_ALGORITHM_SHA1WITHRSA;
    }

    public static String getCipher() {
        return fipsEnabled ? AES_CBC_CIPHER : DES_ECB_CIPHER;
    }

    public static void logInsecureAlgorithm(String configProperty, String insecureAlgorithm) {
        // TODO remove beta check
        // TODO disabling CRYPTO_INSECURE warnings until full FIPS 140-3 support on Semeru is complete
        if (false && isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE", configProperty, insecureAlgorithm, getSecureAlternative(insecureAlgorithm));
        }
    }

    public static void logInsecureAlgorithmReplaced(String configProperty, String insecureAlgorithm, String secureAlgorithm) {
        // TODO remove beta check
        // TODO disabling CRYPTO_INSECURE warnings until full FIPS 140-3 support on Semeru is complete
        if (false && isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE_REPLACED", configProperty, insecureAlgorithm, secureAlgorithm);
        }
    }

    public static void logInsecureProvider(String provider, String insecureAlgorithm) {
        // TODO remove beta check
        // TODO disabling CRYPTO_INSECURE warnings until full FIPS 140-3 support on Semeru is complete
        if (false && isRunningBetaMode()) {
            Tr.warning(tc, "CRYPTO_INSECURE_PROVIDER", provider, insecureAlgorithm);
        }
    }

    public static boolean isIBMJCEAvailable() {
        if (ibmJCEProviderChecked) {
            return ibmJCEAvailable;
        } else {
            ibmJCEAvailable = JavaInfo.isSystemClassAvailable(IBMJCE_PROVIDER);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ibmJCEAvailable: " + ibmJCEAvailable);
            }
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
                Tr.debug(tc, "ibmJCEPlusFIPSAvailable: " + ibmJCEPlusFIPSAvailable);
            }
            ibmJCEPlusFIPSProviderChecked = true;
            return ibmJCEPlusFIPSAvailable;
        }
    }

    public static boolean isOpenJCEPlusAvailable() {
        if (openJCEPlusProviderChecked) {
            return openJCEPlusAvailable;
        } else {
            openJCEPlusAvailable = JavaInfo.isSystemClassAvailable(OPENJCE_PLUS_PROVIDER);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "openJCEPlusAvailable: " + openJCEPlusAvailable);
            }
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
                Tr.debug(tc, "openJCEPlusFIPSAvailable: " + openJCEPlusFIPSAvailable);
            }
            openJCEPlusFIPSProviderChecked = true;
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

        if (fipsEnabled) {
            //Do not check the provider available or not. Later on when we use the provider, the JDK will handle it.
            if (isSemeruFips()) {
                provider = OPENJCE_PLUS_FIPS_NAME;
            } else {
                provider = IBMJCE_PLUS_FIPS_NAME;
            }
        } else if (isZOSandRunningJava11orHigher() && isOpenJCEPlusAvailable()) {
            provider = OPENJCE_PLUS_NAME;
        } else if (isIBMJCEAvailable()) {
            provider = IBMJCE_NAME;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (provider != null) {
                Tr.debug(tc, "Provider configured is " + provider);
            } else {
                Tr.debug(tc, "Provider configured by JDK is " + (Security.getProviders() != null ? Security.getProviders()[0].getName() : "NULL"));
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
            if (fipsEnabled) {
                if (isSemeruFips()) {
                    md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA512,
                                                    OPENJCE_PLUS_FIPS_NAME);
                } else {
                    md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA512,
                                                    IBMJCE_PLUS_FIPS_NAME);
                }
            } else if (CryptoUtils.isIBMJCEAvailable()) {
                md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA, IBMJCE_NAME);
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

    public static String getPropertyLowerCase(final String prop, final String defaultValue) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(prop, defaultValue).toLowerCase();
            }
        });
    }

    static String getFipsLevel() {
        String result = getPropertyLowerCase("com.ibm.fips.mode", "disabled");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getFipsLevel: " + result);
        }
        return result;
    }

    public static boolean isSemeruFips() {
        return "true".equals(getPropertyLowerCase("semeru.fips", "false"));
    }

    public static boolean isFips140_3Enabled() {
        if (fips140_3Checked)
            return fips140_3Enabled;
        else {
            boolean enabled = ("140-3".equals(FIPSLevel) || "true".equals(getPropertyLowerCase("global.fips_140-3", "false")) || isSemeruFips())
                              && isRunningBetaMode();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isFips140_3Enabled: " + enabled);
            }

            if (enabled) { // Check for FIPS 140-3 available
                if (isIBMJCEPlusFIPSAvailable() || isOpenJCEPlusFIPSAvailable() || isFIPSProviderAvailable()) {
                    fips140_3Enabled = true;
                    Tr.info(tc, "FIPS_140_3ENABLED", (ibmJCEPlusFIPSAvailable ? IBMJCE_PLUS_FIPS_NAME : OPENJCE_PLUS_FIPS_NAME));
                } else {
                    Tr.error(tc, "FIPS_140_3ENABLED_ERROR");
                }
            }
            fips140_3Checked = true;
            return fips140_3Enabled;
        }
    }

    /**
     * Check the provider names exist instead of the provider class for securityUtility command.
     *
     */
    private static boolean isFIPSProviderAvailable() {
        return (Security.getProvider(IBMJCE_PLUS_FIPS_NAME) != null || Security.getProvider(OPENJCE_PLUS_FIPS_NAME) != null);
    }

    public static boolean isFips140_2Enabled() {
        //JDK set the fip mode default to 140-2
        //boolean result = "140-2".equals(FIPSLevel) && isRunningBetaMode();
        boolean result = !isFips140_3Enabled() && "true".equals(getPropertyLowerCase(USE_FIPS_PROVIDER, "false")) &&
                         IBMJCE_PLUS_FIPS_NAME.equalsIgnoreCase(getPropertyLowerCase(USE_FIPS_PROVIDER_NAME, "NO_PROVIDER_NAME"));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isFips140_2Enabled: " + result);
        }
        return result;
    }

    public static boolean isFIPSEnabled() {
        if (fipsChecked) {
            return fipsEnabled;
        } else {
            //fipsEnabled = isFips140_2Enabled() || isFips140_3Enabled();
            fipsEnabled = isFips140_3Enabled();
            fipsChecked = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isFIPSEnabled: " + fipsEnabled);
            }
            return fipsEnabled;
        }
    }

    public static boolean isRunningBetaMode() {

        if (!ProductInfo.getBetaEdition()) {
            return false;
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class CryptoUtils for the first time.");
                issuedBetaMessage = true;
            }
            return true;
        }
    }

    /** generate random bytes using SecureRandom */
    public static byte[] generateRandomBytes(int length) {
        byte[] seed = null;
        SecureRandom rand = new SecureRandom();

        // TODO: Investigate hardware Crypto
        //String hardwareCryptoProvider = "IBMJCECCA";
        //Provider provider = rand.getProvider();
        //if (hardwareCryptoProvider.equals(provider.getName())) {
        //    seed = new byte[length];
        //    rand.nextBytes(seed);
        //} else {
        seed = rand.generateSeed(length);
        return seed;
    }
}

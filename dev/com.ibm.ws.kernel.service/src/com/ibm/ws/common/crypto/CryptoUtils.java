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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedAction;
import java.security.Provider;
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
    /**
     * When set true, property 'use.enhanced.security.algorithms' will enable the FIPS algorithms
     * even when FIPS isn't enabled at the JVM level.
     */
    private static final String PROPERTY_USE_ENHANCED_SECURITY_ALG = "use.enhanced.security.algorithms";

    private static final TraceComponent tc = Tr.register(CryptoUtils.class);

    private static boolean issuedBetaMessage = false;

    public static final String MESSAGE_DIGEST_ALGORITHM_SHA_128 = "SHA-128";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA128 = "SHA128";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA_256 = "SHA-256";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA256 = "SHA256";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA_384 = "SHA-384";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA384 = "SHA384";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA_512 = "SHA-512";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA512 = "SHA512";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA = "SHA";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA1 = "SHA1";
    public static final String MESSAGE_DIGEST_ALGORITHM_SHA_1 = "SHA-1";
    public static final String MESSAGE_DIGEST_ALGORITHM_MD5 = "MD5";

    public static boolean ibmJCEAvailable = false;
    public static boolean openJCEPlusAvailable = false;
    public static boolean ibmJCEProviderChecked = false;
    public static boolean openJCEPlusProviderChecked = false;

    public static boolean unitTest = false;
    public static boolean fipsChecked = false;
    public static boolean fips140_3Checked = false;
    public static boolean semeruFips140_3Checked = false;
    public static boolean ibmJdk8Fips140_3Checked = false;

    public static boolean isEnhancedSecurity = false;
    public static boolean isEnhancedSecurityChecked = false;

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
    public static final String IBMJCECCA_NAME = "IBMJCECCA";
    public static final String IBMJCE_PLUS_FIPS_NAME = "IBMJCEPlusFIPS";
    public static final String OPENJCE_PLUS_NAME = "OpenJCEPlus";
    public static final String OPENJCE_PLUS_FIPS_NAME = "OpenJCEPlusFIPS";

    public static final String USE_FIPS_PROVIDER = "com.ibm.jsse2.usefipsprovider";
    public static final String USE_FIPS_PROVIDER_NAME = "com.ibm.jsse2.usefipsProviderName";

    public static final String SIGNATURE_ALGORITHM_SHA1WITHRSA = "SHA1withRSA";
    public static final String SIGNATURE_ALGORITHM_SHA256WITHRSA = "SHA256withRSA";
    public static final String SIGNATURE_ALGORITHM_SHA512WITHRSA = "SHA512withRSA";
    public static final String RSA_SHA_512 = "RSA/SHA-512";
    public static final String RSA_SHA_1 = "RSA/SHA-1";
    public static final String CRYPTO_ALGORITHM_RSA = "RSA";
    public static final String SHA1PRNG = "SHA1PRNG";
    public static final String SHA256DRBG = "SHA256DRBG";

    public static final String HMACSHA1 = "HmacSHA1";

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

    public static final int PBKDF2HMACSHA1_ITERATIONS = 84756;
    // recommended PBKDF2WithHmacSHA512 OWASP recommended iterations
    public static final int PBKDF2HMACSHA512_ITERATIONS = 210000;

    public static String SHA2DRBG = "SHA2DRBG";

    /**
     * For tracking all uses of PBKDF2WithHmacSHA1
     * <p>Example Usages:
     * -AESKeyManager.java for AES_V0(AES-128) password encryption, default prior to 25.0.0.2
     * -PasswordHashGenerator.java for password hashing, default prior to 25.0.0.3
     */
    public static final String PBKDF2_WITH_HMAC_SHA1 = "PBKDF2WithHmacSHA1";

    /**
     * For tracking all uses of PBKDF2WithHmacSHA512
     * <p>Example Usages:
     * -AESKeyManager.java for AES_V1(AES-256) password encryption, default 25.0.0.2+
     * -PasswordHashGenerator.java for password hashing, default 25.0.0.3+
     */
    public static final String PBKDF2_WITH_HMAC_SHA512 = "PBKDF2WithHmacSHA512";

    // FIPS minimum allowable salt length in bytes
    public static final int FIPS1403_PBKDF2_MINIMUM_SALT_LENGTH_BYTES = 16;
    // FIPS recommended salt length in bytes
    public static final int FIPS1403_PBKDF2_SALT_LENGTH_BYTES = 128;
    // FIPS minimum allowable key length in bits
    public static final int FIPS1403_PBKDF2_MINIMUM_KEY_LENGTH_BITS = 112;
    // FIPS recommended key length in bits
    public static final int FIPS1403_PBKDF2_KEY_LENGTH_BITS = 256;
    // FIPS minimum allowable iteration count
    public static final int FIPS1403_PBKDF2_MINIMUM_ITERATIONS = 1000;
    // FIPS recommended iteration count
    public static final int FIPS1403_PBKDF2_ITERATIONS = PBKDF2HMACSHA512_ITERATIONS;

    private static boolean ibmJdk8Fips140_3Enabled = isIbmJdk8Fips140_3Enabled();
    private static boolean semeruFips140_3Enabled = isSemeruFips140_3Enabled();

    private static boolean fips140_3Enabled = isFips140_3Enabled();
    private static boolean fipsEnabled = fips140_3Enabled;

    /** Algorithm used for encryption in LTPA and audit. */
    public static final String ENCRYPT_ALGORITHM = ENCRYPT_ALGORITHM_AES;

    /**
     * AES Password Encryption Constants, used in AESKeyManager.java
     * Uses:
     * PBKDF2WithHmacSHA1
     * PBKDF2WithHmacSHA512
     * AES_128_KEY_LENGTH_BITS
     * AES_256_KEY_LENGTH_BITS
     **/
    /**
     * For tracking all 128-bit AES key usages
     * <p>Example Usages:
     * -AESKeyManager.java for AES_V0(AES-128) password encryption</li>
     */
    public static final int AES_128_KEY_LENGTH_BITS = AES_128_KEY_LENGTH_BYTES * 8;

    /**
     * For tracking all 256-bit AES key usages
     * <p>Example Usages:
     * -AESKeyManager.java for AES_V1(AES-256) password encryption
     */
    public static final int AES_256_KEY_LENGTH_BITS = AES_256_KEY_LENGTH_BYTES * 8;

    private static Map<String, String> secureAlternative = new HashMap<>();
    static {
        secureAlternative.put(MESSAGE_DIGEST_ALGORITHM_SHA, MESSAGE_DIGEST_ALGORITHM_SHA256);
        secureAlternative.put(MESSAGE_DIGEST_ALGORITHM_SHA1, MESSAGE_DIGEST_ALGORITHM_SHA256);
        secureAlternative.put(MESSAGE_DIGEST_ALGORITHM_SHA_1, MESSAGE_DIGEST_ALGORITHM_SHA256);
        secureAlternative.put(MESSAGE_DIGEST_ALGORITHM_SHA128, MESSAGE_DIGEST_ALGORITHM_SHA256);
        secureAlternative.put(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_MD5, MESSAGE_DIGEST_ALGORITHM_SHA256);
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
        if (fipsEnabled)
            return SIGNATURE_ALGORITHM_SHA512WITHRSA;
        else
            return SIGNATURE_ALGORITHM_SHA1WITHRSA;
    }

    public static String getCipher() {
        return fipsEnabled ? AES_CBC_CIPHER : DES_ECB_CIPHER;
    }

    public static void logInsecureAlgorithm(String configProperty, String insecureAlgorithm) {
        // TODO disabling CRYPTO_INSECURE warnings until full FIPS 140-3 support on Semeru is complete
        if (false) {
            Tr.warning(tc, "CRYPTO_INSECURE", configProperty, insecureAlgorithm, getSecureAlternative(insecureAlgorithm));
        }
    }

    public static void logInsecureAlgorithmReplaced(String configProperty, String insecureAlgorithm, String secureAlgorithm) {
        // TODO disabling CRYPTO_INSECURE warnings until full FIPS 140-3 support on Semeru is complete
        if (false) {
            Tr.warning(tc, "CRYPTO_INSECURE_REPLACED", configProperty, insecureAlgorithm, secureAlgorithm);
        }
    }

    public static void logInsecureProvider(String provider, String insecureAlgorithm) {
        // TODO disabling CRYPTO_INSECURE warnings until full FIPS 140-3 support on Semeru is complete
        if (false) {
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
        // if useEnhancedSecurityAlgorithms() returns true we will assume FIPS is not enabled at the JVM level.
        // Do not return a FIPS provider in this case because it likely isn't available.
        if (fipsEnabled && !useEnhancedSecurityAlgorithms()) {
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
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA_384,
                                                                                       MESSAGE_DIGEST_ALGORITHM_SHA_512);

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
                if (useEnhancedSecurityAlgorithms()) {
                    md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA_512);
                } else if (isSemeruFips()) {
                    md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA_512,
                                                    OPENJCE_PLUS_FIPS_NAME);
                } else {
                    md1 = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM_SHA_512,
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

    public static boolean isIbmJdk8Fips140_3() {
        List<String> args = AccessController.doPrivileged(new PrivilegedAction<List<String>>() {
            @Override
            public List<String> run() {
                return ManagementFactory.getRuntimeMXBean().getInputArguments();
            }
        });
        return args.contains("-Xenablefips140-3");
    }

    public static boolean isSemeruFips() {
        return "true".equals(getPropertyLowerCase("semeru.fips", "false"));
    }

    protected static boolean useEnhancedSecurityAlgorithms() {
        if (isEnhancedSecurityChecked) {
            return isEnhancedSecurity;
        } else {
            isEnhancedSecurity = isRunningBetaMode() && Boolean.valueOf(getPropertyLowerCase(PROPERTY_USE_ENHANCED_SECURITY_ALG, "false"));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isEnhancedSecurity: " + (isEnhancedSecurity ? "enabled" : "disabled"));
            }
        }
        isEnhancedSecurityChecked = true;
        return isEnhancedSecurity;
    }

    /**
     * Checks if Beta is enabled and FIPS 140-3 is enabled for either Semeru or IBM JDK.
     *
     * @return true if Beta is enabled and FIPS 140-3 is enabled for either Semeru or IBM JDK. Otherwise, false.
     */
    public static boolean isFips140_3EnabledWithBetaGuard() {
        return isRunningBetaMode() && isFips140_3Enabled();
    }

    /**
     * Checks if Beta is enabled and FIPS 140-3 is enabled for Semeru.
     * Private for now unless there is a use-case to add functionality specific to IBM Semeru FIPS 140-3.
     *
     * @return true if Beta is enabled and FIPS 140-3 is enabled for Semeru. Otherwise, false.
     */
    private static boolean isSemeruFips140_3EnabledWithBetaGuard() {
        return isRunningBetaMode() && isSemeruFips140_3Enabled();
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

    /**
     * Checks if FIPS 140-3 is enabled for either Semeru or IBM JDK.
     *
     * @return true if FIPS 140-3 is enabled for either Semeru or IBM JDK. Otherwise false.
     */
    public static boolean isFips140_3Enabled() {
        if (fips140_3Checked)
            return fips140_3Enabled;
        else {
            fips140_3Enabled = isIbmJdk8Fips140_3Enabled() || isSemeruFips140_3Enabled();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isFips140_3Enabled: " + fips140_3Enabled);
            }

            fips140_3Checked = true;
            return fips140_3Enabled;
        }
    }

    /**
     * Checks if FIPS 140-3 is enabled for Semeru.
     * Not public for now unless there is a use-case to add functionality specific to IBM Semeru FIPS 140-3.
     *
     * @return true if FIPS 140-3 is enabled for Semeru. Otherwise, false.
     */
    static boolean isSemeruFips140_3Enabled() {
        if (semeruFips140_3Checked)
            return semeruFips140_3Enabled;
        else {
            semeruFips140_3Enabled = false;
            if (isSemeruFips() && "140-3".equals(getFipsLevel())) {
                if (isOpenJCEPlusFIPSProviderAvailable()) {
                    semeruFips140_3Enabled = true;
                    Tr.info(tc, "FIPS_140_3ENABLED", OPENJCE_PLUS_FIPS_NAME);
                } else {
                    Tr.error(tc, "FIPS_140_3ENABLED_ERROR");
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isSemeruFips140_3Enabled: " + semeruFips140_3Enabled);
            }

            if (!semeruFips140_3Enabled) {
                semeruFips140_3Enabled = useEnhancedSecurityAlgorithms();
                if (semeruFips140_3Enabled) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "isSemeruFips140_3Enabled set to true by useEnhancedSecurityAlgorithms()");
                    }
                }
            }

            semeruFips140_3Checked = true;
            return semeruFips140_3Enabled;
        }
    }

    /**
     * Checks if FIPS 140-3 is enabled for IBM JDK 8.
     * Not public for now unless there is a use-case to add functionality specific to IBM JDK 8 FIPS 140-3.
     *
     * @return true if FIPS 140-3 is enabled for IBM JDK 8. Otherwise, false.
     */
    static boolean isIbmJdk8Fips140_3Enabled() {
        if (ibmJdk8Fips140_3Checked)
            return ibmJdk8Fips140_3Enabled;
        else {
            ibmJdk8Fips140_3Enabled = false;
            if (isIbmJdk8Fips140_3() && "140-3".equals(getFipsLevel())) {
                if (isIBMJCEPlusFIPSProviderAvailable()) {
                    ibmJdk8Fips140_3Enabled = true;
                    Tr.info(tc, "FIPS_140_3ENABLED", IBMJCE_PLUS_FIPS_NAME);
                } else {
                    Tr.error(tc, "FIPS_140_3ENABLED_ERROR");
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isIbmJdk8Fips140_3Enabled: " + ibmJdk8Fips140_3Enabled);
            }

            if (!ibmJdk8Fips140_3Enabled) {
                ibmJdk8Fips140_3Enabled = useEnhancedSecurityAlgorithms();
                if (ibmJdk8Fips140_3Enabled) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ibmJdk8Fips140_3Enabled set to true by useEnhancedSecurityAlgorithms()");
                    }
                }
            }

            ibmJdk8Fips140_3Checked = true;
            return ibmJdk8Fips140_3Enabled;
        }
    }

    /**
     *
     * @param saltString                  a salt value that is intended to be used to generate a hash via the property PasswordUtil.PROPERTY_HASH_SALT.
     *                                        null or empty strings are valid here because PasswordUtil will generate salt if that is the case.
     * @param throwExceptionIfSaltInvalid if true, an exception is thrown if saltString is not compatible with FIPS140-3.
     * @return true if compatible, false otherwise, exception if false and throwExceptionIfSaltInvalid is true.
     */
    public static boolean checkFipsCompatibleSalt(String saltString, boolean logIfIncompatible) {
        boolean isCompatible = true;
        if (CryptoUtils.isFips140_3EnabledWithBetaGuard() && saltString != null && !saltString.isEmpty() && saltString.length() < FIPS1403_PBKDF2_MINIMUM_SALT_LENGTH_BYTES) {
            isCompatible = false;
        }
        // TODO delete this logging
        if (!isCompatible && logIfIncompatible) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                try {
                    throw new Exception("checkFipsCompatibleSalt failed!");
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    Tr.debug(tc, "isCompatible: false, saltString: " + saltString + "\n" + sw.toString());
                }

            }
        }
        // TODO delete this logging

        return isCompatible;
    }

    /**
     * Check the provider name exist instead of the provider class for securityUtility command.
     *
     */
    static boolean isIBMJCEPlusFIPSProviderAvailable() {
        return (Security.getProvider(IBMJCE_PLUS_FIPS_NAME) != null);
    }

    /**
     * Check the provider name exist instead of the provider class for securityUtility command.
     *
     */
    static boolean isOpenJCEPlusFIPSProviderAvailable() {
        return (Security.getProvider(OPENJCE_PLUS_FIPS_NAME) != null);
    }

    public static boolean isFips140_2Enabled() {
        //JDK set the fip mode default to 140-2
        boolean result = !isFips140_3Enabled() && "true".equals(getUseFipsProvider()) && IBMJCE_PLUS_FIPS_NAME.equalsIgnoreCase(getFipsProviderName());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isFips140_2Enabled: " + result);
        }
        return result;
    }

    static String getUseFipsProvider() {
        return getPropertyLowerCase(USE_FIPS_PROVIDER, "false");
    }

    static String getFipsProviderName() {
        return getPropertyLowerCase(USE_FIPS_PROVIDER_NAME, "NO_PROVIDER_NAME");
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

    /** generate random bytes using SecureRandom */
    public static byte[] generateRandomBytes(int length) {
        byte[] seed = null;
        SecureRandom rand = new SecureRandom();
        Provider provider = rand.getProvider();
        String providerName = provider.getName();

        if (providerName.equals(IBMJCECCA_NAME)) {
            seed = new byte[length];
            rand.nextBytes(seed);
        } else {
            seed = rand.generateSeed(length);
        }

        return seed;
    }

    public static int getPbkdf2Salt(int dflt) {
        return isFips140_3EnabledWithBetaGuard() ? FIPS1403_PBKDF2_SALT_LENGTH_BYTES : dflt;
    }

    public static int getPbkdf2Iterations(int dflt) {
        return isFips140_3EnabledWithBetaGuard() ? FIPS1403_PBKDF2_ITERATIONS : dflt;
    }

    public static int getPbkdf2KeyLength(int dflt) {
        return isFips140_3EnabledWithBetaGuard() ? FIPS1403_PBKDF2_KEY_LENGTH_BITS : dflt;
    }

    public static final byte[] AES_V0_SALT = new byte[] { -89, -94, -125, 57, 76, 90, -77, 79, 50, 21, 10, -98, 47, 23, 17, 56, -61, 46, 125, -128 };

    public static final byte[] AES_V1_SALT = new byte[] { -89, -63, 22, 15, -121, 11, 102, 75, -91, 68, -94, -89, 96, 83, -21, -69, -45, 29, 26, 106, -18, 69, 60, -6, 108, 73,
       111, 122, 41, -19, -78, -79, -28, 102, 57, -10, 66, 48, 54, 111, 35, 92, 59, -121, 36, 15, 14, -63, -43, 107, 63, -18,
       87, 43, -57, 74, 0, 107, -119, -2, -7, -7, -46, -95, -44, 36, -10, 86, -119, -80, -114, 10, 85, 24, 24, -121, -30, 63,
       59, 49, 52, -76, -122, 108, -84, 16, 4, -39, 58, 75, 9, -25, 126, 127, -96, 122, -62, -94, 71, -8, -101, -33, 57, -44,
       -93, 86, 76, -115, 113, -124, 104, -40, -121, -9, 86, 121, -48, -57, -77, -58, 73, 7, 12, 4, 24, -81, -64, 107 };

    public static final int GCM_TAG_LENGTH = 128;

}
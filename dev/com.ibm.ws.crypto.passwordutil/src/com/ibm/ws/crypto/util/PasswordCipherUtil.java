/*******************************************************************************
 * Copyright (c) 2007, 2026 IBM Corporation and others.
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

package com.ibm.ws.crypto.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.crypto.util.custom.CustomManifest;
import com.ibm.ws.crypto.util.custom.CustomUtils;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.crypto.CustomPasswordEncryption;
import com.ibm.wsspi.security.crypto.EncryptedInfo;

/**
 * Utility class for password enciphering and deciphering.
 */
@Component(service = PasswordCipherUtil.class,
           name = "com.ibm.ws.crypto.util.PasswordCipherUtil",
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = "service.vendor=IBM")
public class PasswordCipherUtil {

    private static final Logger logger = Logger.getLogger(PasswordCipherUtil.class.getCanonicalName(), MessageUtils.RB);

    private static final String CUSTOM = "custom";
    private static final String CUSTOM_COLON = "custom:";
    private static final String XOR = "xor";
    private static final String AES = "aes";
    private static final String AES_128 = "aes-128";
    private static final String AES_256 = "aes-256";
    private static final String HASH = "hash";

    private static final byte XOR_MASK = 0x5F;

    private static final String[] SUPPORTED_CRYPTO_ALGORITHMS_DEFAULT = new String[] { XOR, AES, AES_128, AES_256, HASH };
    private static final String[] SUPPORTED_CRYPTO_ALGORITHMS_CUSTOM = new String[] { XOR, AES, HASH, CUSTOM };
    private static volatile String[] SUPPORTED_CRYPTO_ALGORITHMS = SUPPORTED_CRYPTO_ALGORITHMS_DEFAULT;
    private static String[] SUPPORTED_HASH_ALGORITHMS = new String[] { HASH };

    static final String KEY_ENCRYPTION_SERVICE = "customPasswordEncryption";
    private static AtomicServiceReference<CustomPasswordEncryption> customPasswordEncryption = new AtomicServiceReference<CustomPasswordEncryption>(KEY_ENCRYPTION_SERVICE);

    private static volatile CustomPasswordEncryption cpeImpl = null;
    private static volatile List<CustomManifest> cms = null;

    private static final java.util.concurrent.atomic.AtomicBoolean alreadyLoggedHASHWeakPasswordAlgoWarning = new java.util.concurrent.atomic.AtomicBoolean(false);

    // in order to support the custom encryption for the command line parameter, implement a static initialier to check whether
    // the custom encryption is enabled.
    static {
        try {
            initialize();
        } catch (InvocationTargetException e) {
            throw new ExceptionInInitializerError(e.getTargetException());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static protected void initialize() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        //        if (CustomUtils.isCommandLine() && CustomUtils.isCustomEnabled()) {
        if (CustomUtils.isCommandLine()) {
            cms = CustomUtils.findCustomEncryption(CustomUtils.CUSTOM_ENCRYPTION_DIR);
            if (cms != null) {
                // only support one custom encryption
                if (cms.size() == 1) {
                    Class<?> c = Class.forName(cms.get(0).getImplClass());
                    cpeImpl = (CustomPasswordEncryption) c.getDeclaredConstructor().newInstance();
                    SUPPORTED_CRYPTO_ALGORITHMS = SUPPORTED_CRYPTO_ALGORITHMS_CUSTOM;
                }
            }
        }
    }

    /**
     * Returns the list of custom password encryption if exists.
     * This method only works under the command line utility environment.
     *
     * @return list of the custom password encryption in JSON format
     * @throws UnsupportedConfigurationException If there are multiple custom password encryption exists.
     */
    public static String listCustom() throws UnsupportedConfigurationException {
        String output = null;
        if (cms != null && !cms.isEmpty()) {
            if (cms.size() != 1) {
                // the number of the custom encryption is more than one, an exception is thrown.
                String message = composeMultipleCustomErrorMessage(cms);
                throw new UnsupportedConfigurationException(message);
            }
            output = CustomUtils.toJSON(cms);
        }
        return output;
    }

    protected void initializeCustomEncryption() {
        if (customPasswordEncryption.getService() != null) {
            logger.log(Level.INFO, "PASSWORDUTIL_CUSTOM_SERVICE_STARTED", customPasswordEncryption.getService().getClass().getName());
            SUPPORTED_CRYPTO_ALGORITHMS = SUPPORTED_CRYPTO_ALGORITHMS_CUSTOM;
        } else {
            // The stopped message is logged in unsetCustomPasswordEncryption method in order to log the class name.
            SUPPORTED_CRYPTO_ALGORITHMS = SUPPORTED_CRYPTO_ALGORITHMS_DEFAULT;
        }
    }

    @Activate
    protected void activate(ComponentContext cc) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("activate : customPasswordEncryption : " + customPasswordEncryption);
        }
        customPasswordEncryption.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("deactivate : customPasswordEncryption : " + customPasswordEncryption);
        }
        customPasswordEncryption.deactivate(cc);
    }

    @Reference(service = CustomPasswordEncryption.class,
               policy = ReferencePolicy.DYNAMIC,
               cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY,
               name = KEY_ENCRYPTION_SERVICE)
    protected void setCustomPasswordEncryption(ServiceReference<CustomPasswordEncryption> reference) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("setCustomPasswordEncryption : customPasswordEncryption : " + customPasswordEncryption);
        }
        customPasswordEncryption.setReference(reference);
        initializeCustomEncryption();
    }

    protected void unsetCustomPasswordEncryption(ServiceReference<CustomPasswordEncryption> reference) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("unsetCustomPasswordEncryption : customPasswordEncryption : " + customPasswordEncryption);
        }
        if (customPasswordEncryption.getService() != null) {
            logger.log(Level.INFO, "PASSWORDUTIL_CUSTOM_SERVICE_STOPPED", customPasswordEncryption.getService().getClass().getName());
        }
        customPasswordEncryption.unsetReference(reference);
        initializeCustomEncryption();
    }

    /**
     * Decipher the input password using the provided algorithm.
     *
     * @param encrypted_bytes
     * @param crypto_algorithm
     * @return byte[] - decrypted password
     * @throws InvalidKeySpecException
     * @throws InvalidPasswordCipherException
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedCryptoAlgorithmException
     */
    public static byte[] decipher(byte[] encrypted_bytes,
                                  String crypto_algorithm) throws InvalidKeySpecException, InvalidPasswordCipherException, NoSuchAlgorithmException, UnsupportedCryptoAlgorithmException {

        if (crypto_algorithm == null) {
            logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "decipher", "PASSWORDUTIL_UNKNOWN_ALGORITHM",
                        new Object[] { "null", formatSupportedCryptoAlgorithms() });
            throw new UnsupportedCryptoAlgorithmException();
        }

        byte[] decrypted_bytes = null;

        if (AES.equalsIgnoreCase(crypto_algorithm) || AES_128.equalsIgnoreCase(crypto_algorithm) || AES_256.equalsIgnoreCase(crypto_algorithm)) {
            decrypted_bytes = AesCipher.forDecrypt(encrypted_bytes).decrypt(encrypted_bytes);
        } else if (XOR.equalsIgnoreCase(crypto_algorithm)) {
            decrypted_bytes = xor(encrypted_bytes);
        } else if (HASH.equalsIgnoreCase(crypto_algorithm)) {
            throw new InvalidPasswordCipherException(MessageUtils.getMessage("PASSWORDUTIL_ERROR_UNSUPPORTED_OPERATION", crypto_algorithm));
        } else if (CUSTOM.equalsIgnoreCase(crypto_algorithm) || crypto_algorithm.startsWith(CUSTOM_COLON)) {

            CustomPasswordEncryption cpe = getCustomImpl();
            if (cpe != null) {
                int index = crypto_algorithm.indexOf(':');
                String keyAlias = null;

                if (index != -1) {
                    keyAlias = crypto_algorithm.substring(index + 1);
                }

                try {
                    decrypted_bytes = cpe.decrypt(new EncryptedInfo(encrypted_bytes, keyAlias));
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Successfully decrypted password using custom encryption plug point.");
                } catch (Exception e) {
                    logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "decipher", "PASSWORDUTIL_CUSTOM_DECRYPTION_ERROR", e);
                    throw (InvalidPasswordCipherException) new InvalidPasswordCipherException(e.getMessage()).initCause(e);
                }
            } else {
                logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "decipher", "PASSWORDUTIL_CUSTOM_SERVICE_DOES_NOT_EXIST");
                throw new UnsupportedCryptoAlgorithmException();
            }
        } else {
            logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "decipher", "PASSWORDUTIL_UNKNOWN_ALGORITHM", new Object[] { crypto_algorithm,
                                                                                                                                       formatSupportedCryptoAlgorithms() });
            throw new UnsupportedCryptoAlgorithmException();
        }

        if (decrypted_bytes == null) {
            throw new InvalidPasswordCipherException("The output is null.");
        }
        return decrypted_bytes;
    }

    /**
     * Encipher the raw password using the provided algorithm.
     *
     * @param decrypted_bytes
     * @param crypto_algorithm
     * @return byte[] - enciphered value
     * @throws InvalidPasswordCipherException
     * @throws UnsupportedCryptoAlgorithmException
     */
    public static byte[] encipher(byte[] decrypted_bytes,
                                  String crypto_algorithm) throws InvalidKeySpecException, InvalidPasswordCipherException, NoSuchAlgorithmException, UnsupportedCryptoAlgorithmException {
        EncryptedInfo info = encipher_internal(decrypted_bytes, crypto_algorithm, (String) null);
        return info.getEncryptedBytes();
    }

    // ---------------------------------------------------------------------------
    // Method: encipher( decrypted password byte[], crypto algorithm string )
    // Return: encrypted password byte[]
    // ---------------------------------------------------------------------------
    public static EncryptedInfo encipher_internal(byte[] decrypted_bytes, String crypto_algorithm,
                                                  String cryptoKey) throws InvalidKeySpecException, InvalidPasswordCipherException, NoSuchAlgorithmException, UnsupportedCryptoAlgorithmException {
        HashMap<String, String> props = new HashMap<String, String>();
        if (cryptoKey != null) {
            props.put(PasswordUtil.PROPERTY_CRYPTO_KEY, cryptoKey);
        }
        return encipher_internal(decrypted_bytes, crypto_algorithm, props);
    }

    public static EncryptedInfo encipher_internal(byte[] decrypted_bytes, String crypto_algorithm,
                                                  Map<String, String> properties) throws InvalidKeySpecException, InvalidPasswordCipherException, NoSuchAlgorithmException, UnsupportedCryptoAlgorithmException {

        EncryptedInfo info = null;
        byte[] encrypted_bytes = null;

        if (AES.equalsIgnoreCase(crypto_algorithm) || AES_256.equalsIgnoreCase(crypto_algorithm)) {
            String cryptoKey = null;
            String base64Key = null;
            if (properties != null) {
                cryptoKey = properties.get(PasswordUtil.PROPERTY_CRYPTO_KEY);
                base64Key = properties.get(PasswordUtil.PROPERTY_AES_KEY);
            }
            AESKeyManager.EncipherContext ctx = AESKeyManager.resolverForEncipher(base64Key, cryptoKey);
            if (logger.isLoggable(Level.FINE)) {
                if (base64Key != null)
                    logger.fine("Encrypting password using " + PasswordUtil.PROPERTY_AES_KEY);
                else if (AESKeyManager.hasCustomSecretKeyResolver())
                    logger.fine("Encrypting password using hardware SecretKeyResolver (AES_V2)");
                else
                    logger.fine("Encrypting password using " + PasswordUtil.PROPERTY_CRYPTO_KEY);
            }
            info = AesCipher.forEncrypt(ctx.version, ctx.resolver).encrypt(decrypted_bytes);

        } else if (AES_128.equalsIgnoreCase(crypto_algorithm)) {
            String cryptoKey = null;
            if (properties != null) {
                cryptoKey = properties.get(PasswordUtil.PROPERTY_CRYPTO_KEY);
            }
            AESKeyManager.EncipherContext ctx = new AESKeyManager.EncipherContext(
                    AESKeyManager.KeyVersion.AES_V0, AESKeyManager.resolverForKey(AESKeyManager.KeyVersion.AES_V0, cryptoKey));
            info = AesCipher.forEncrypt(ctx.version, ctx.resolver).encrypt(decrypted_bytes);
        } else if (XOR.equalsIgnoreCase(crypto_algorithm)) {
            encrypted_bytes = xor(decrypted_bytes);
            if (encrypted_bytes != null)
                info = new EncryptedInfo(encrypted_bytes, "");
        } else if (HASH.equalsIgnoreCase(crypto_algorithm)) {
            char[] decrypted_chars = null;
            try {
                String originalString = new String(decrypted_bytes, StandardCharsets.UTF_8);
                decrypted_chars = originalString.toCharArray();
            } catch (Exception e) {
                throw new InvalidPasswordCipherException();
            }
            info = generateHash(decrypted_chars, properties);
        } else if (crypto_algorithm != null && crypto_algorithm.equalsIgnoreCase(CUSTOM)) {
            CustomPasswordEncryption cpe = getCustomImpl();
            if (cpe != null) {
                try {
                    info = cpe.encrypt(decrypted_bytes);
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Successfully encrypted password using custom encryption plug point.");
                } catch (Exception e) {
                    logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "encipher", "PASSWORDUTIL_CUSTOM_ENCRYPTION_ERROR", e);
                    throw (InvalidPasswordCipherException) new InvalidPasswordCipherException(e.getMessage()).initCause(e);
                }
            } else {
                logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "encipher", "PASSWORDUTIL_CUSTOM_SERVICE_DOES_NOT_EXIST");
                throw new UnsupportedCryptoAlgorithmException();
            }
        } else {
            logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "encipher", "PASSWORDUTIL_UNKNOWN_ALGORITHM", new Object[] { crypto_algorithm,
                                                                                                                                       formatSupportedCryptoAlgorithms() });
            throw new UnsupportedCryptoAlgorithmException();
        }

        if (info == null) {
            throw new InvalidPasswordCipherException("The output is null.");
        }
        return info;
    }

    /**
     * Generates a hashed representation of the given plaintext password bytes.
     *
     * @param plainBytes the plaintext password as a char array
     * @param properties optional hash parameters (algorithm, salt, iteration count, output length)
     * @return an {@link EncryptedInfo} wrapping the serialised {@link HashedData} bytes,
     *         or {@code null} if FIPS 140-3 is enabled and an incompatible algorithm is selected
     * @throws InvalidPasswordCipherException if hashing fails or the algorithm is unavailable
     */
    private static EncryptedInfo generateHash(char[] plainBytes, Map<String, String> properties) throws InvalidPasswordCipherException {
        EncryptedInfo info = null;
        String algorithm = null;
        String saltString = null;
        String encodedString = null;
        int iteration = -1;
        int length = -1;
        byte[] salt = null;
        byte[] output = null;
        boolean saltSet = false;
        if (properties != null) {
            encodedString = properties.get(PasswordUtil.PROPERTY_HASH_ENCODED);
            if (encodedString != null && PasswordUtil.isHashed(encodedString)) {
                try {
                    String value = PasswordUtil.removeCryptoAlgorithmTag(encodedString);
                    HashedData dd = new HashedData(Base64Coder.base64Decode(value.getBytes(StandardCharsets.UTF_8)));
                    algorithm = dd.getAlgorithm();
                    iteration = dd.getIteration();
                    length = dd.getOutputLength();
                    salt = dd.getSalt();
                    saltSet = true;
                } catch (Exception e) {
                    throw (InvalidPasswordCipherException) new InvalidPasswordCipherException(e.getMessage()).initCause(e);
                }
            }
            if (algorithm == null) {
                algorithm = properties.get(PasswordUtil.PROPERTY_HASH_ALGORITHM);
            }

            if (!saltSet) {
                saltString = properties.get(PasswordUtil.PROPERTY_HASH_SALT);
                salt = PasswordHashGenerator.generateSalt(saltString);
                saltSet = true;
            }

            if (iteration < 0) {
                String value = properties.get(PasswordUtil.PROPERTY_HASH_ITERATION);
                if (value != null) {
                    iteration = Integer.parseInt(value);
                }
            }

            if (length < 0) {
                String value = properties.get(PasswordUtil.PROPERTY_HASH_LENGTH);
                if (value != null) {
                    length = Integer.parseInt(value);
                }
            }
        }

        // If there were no properties or only a partial set of properties provided to fill in information need to hash
        // the data then fill in the missing information with the defaults.

        // LATEST_DEFAULT_ALGORITHM will be used for generating new hashed passwords
        if (algorithm == null) {
            algorithm = PasswordHashGenerator.LATEST_DEFAULT_ALGORITHM;
        }

        if (!saltSet) {
            salt = PasswordHashGenerator.generateSalt(saltString);
        }

        if (iteration < 0) {
            iteration = PasswordHashGenerator.getDefaultIteration();
        }

        if (length < 0) {
            length = PasswordHashGenerator.getDefaultOutputLength();
        }

        boolean usingSHA1 = PasswordHashGenerator.getDefaultAlgorithm().equals(algorithm);
        //Throw error if older algorithm is used when FIPS is enabled
        if (CryptoUtils.isFips140_3Enabled() && usingSHA1) {
            logger.logp(Level.SEVERE, PasswordUtil.class.getName(), "decode_password",
                        MessageUtils.getMessage("PASSWORDUTIL_EXCEPTION_FIPS140_3_HASH_SHA1_UNAVAILABLE_ALGORITHM"));
            return null;
        }
        //Print warning if older algorithm is being used.
        else if (usingSHA1 && alreadyLoggedHASHWeakPasswordAlgoWarning.compareAndSet(false, true)) {
            logger.logp(Level.WARNING, PasswordUtil.class.getName(), "generateHash", "PASSWORDUTIL_WEAK_ALGORITHM_WARNING",
                        new Object[] { "{hash}", ": " + algorithm, ": " + PasswordHashGenerator.LATEST_DEFAULT_ALGORITHM });
        }

        try {
            HashedData dd = new HashedData(plainBytes, algorithm, salt, iteration, length, (byte[]) null);
            output = dd.toBytes();
        } catch (InvalidPasswordCipherException ipce) {
            throw ipce;
        } catch (Exception e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException(e.getMessage()).initCause(e);
        }

        if (output != null) {
            info = new EncryptedInfo(output, "");
        }
        return info;
    }

    /**
     * Query the list of supported crypto algorithms.
     *
     * @return String[]
     */
    public static String[] getSupportedCryptoAlgorithms() {
        return SUPPORTED_CRYPTO_ALGORITHMS.clone();
    }

    /**
     * Query the fail-safe crypto algorithm.
     *
     * @return String
     */
    public static String getFailSafeCryptoAlgorithm() {
        return XOR;
    }

    public static String[] getSupportedHashAlgorithms() {
        return SUPPORTED_HASH_ALGORITHMS.clone();
    }

    // ---------------------------------------------------------------------------
    // Method: xor( byte[] )
    // Return: XOR_MASK ^ byte[]
    // ---------------------------------------------------------------------------
    private static byte[] xor(byte[] bytes) {
        byte[] xor_bytes = null;

        if (bytes != null) {
            xor_bytes = new byte[bytes.length];

            for (int i = 0; i < bytes.length; i++) {
                xor_bytes[i] = (byte) (XOR_MASK ^ bytes[i]);
            }
        }

        return xor_bytes;
    }

    private static String formatSupportedCryptoAlgorithms() {
        return String.join(", ", SUPPORTED_CRYPTO_ALGORITHMS);
    }

    private static CustomPasswordEncryption getCustomImpl() {
        CustomPasswordEncryption cpe = customPasswordEncryption.getService();
        if (cpe == null) {
            cpe = cpeImpl;
        }
        return cpe;
    }

    private static String composeMultipleCustomErrorMessage(List<CustomManifest> list) {
        StringBuilder sb = new StringBuilder(MessageUtils.getMessage("PASSWORDUTIL_DUPLICATE_CUSTOM_ENCRYPTION"));
        for (CustomManifest cm : list) {
            sb.append("\n").append(cm.getLocation());
        }
        return sb.toString();
    }

}

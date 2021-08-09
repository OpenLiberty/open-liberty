/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.crypto.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
import com.ibm.ws.common.internal.encoder.Base64Coder;
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

    private static final Class<?> CLASS_NAME = PasswordCipherUtil.class;
    private final static Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName(), MessageUtils.RB);

    private static final String CUSTOM = "custom";
    private static final String CUSTOM_COLON = "custom:";
    private static final String XOR = "xor";
    private static final String AES = "aes";
    private static final String HASH = "hash";

    private static final byte XOR_MASK = 0x5F;

    private static final String[] SUPPORTED_CRYPTO_ALGORITHMS_DEFAULT = new String[] { XOR, AES, HASH };
    private static final String[] SUPPORTED_CRYPTO_ALGORITHMS_CUSTOM = new String[] { XOR, AES, HASH, CUSTOM };
    private static String[] SUPPORTED_CRYPTO_ALGORITHMS = SUPPORTED_CRYPTO_ALGORITHMS_DEFAULT;
    private static String[] SUPPORTED_HASH_ALGORITHMS = new String[] { HASH };

    static final String KEY_ENCRYPTION_SERVICE = "customPasswordEncryption";
    private static AtomicServiceReference<CustomPasswordEncryption> customPasswordEncryption = new AtomicServiceReference<CustomPasswordEncryption>(KEY_ENCRYPTION_SERVICE);

    private static CustomPasswordEncryption cpeImpl = null;
    private static List<CustomManifest> cms = null;
    // in order to support the custom encryption for the command line parameter, implement a static initialier to check whether
    // the custom encryption is enabled.
    static {
        try {
            initialize();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static protected void initialize() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
//        if (CustomUtils.isCommandLine() && CustomUtils.isCustomEnabled()) {
        if (CustomUtils.isCommandLine()) {
            cms = CustomUtils.findCustomEncryption(CustomUtils.CUSTOM_ENCRYPTION_DIR);
            if (cms != null) {
                // only support one custom encryption
                if (cms.size() == 1) {
                    Class<?> c = Class.forName(cms.get(0).getImplClass());
                    cpeImpl = (CustomPasswordEncryption) c.newInstance();
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
     * @throws InvalidPasswordCipherException
     * @throws UnsupportedCryptoAlgorithmException
     */
    public static byte[] decipher(byte[] encrypted_bytes, String crypto_algorithm) throws InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException {

        if (crypto_algorithm == null) {
            logger.logp(Level.SEVERE, PasswordCipherUtil.class.getName(), "decipher", "PASSWORDUTIL_UNKNOWN_ALGORITHM",
                        new Object[] { "null", formatSupportedCryptoAlgorithms() });
            throw new UnsupportedCryptoAlgorithmException();
        }

        byte[] decrypted_bytes = null;

        if (AES.equalsIgnoreCase(crypto_algorithm)) {
            decrypted_bytes = aesDecipher(encrypted_bytes, decrypted_bytes);
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
     * @param encrypted_bytes
     * @param decrypted_bytes
     * @return
     * @throws UnsupportedCryptoAlgorithmException
     * @throws InvalidPasswordCipherException
     */
    private static byte[] aesDecipher(byte[] encrypted_bytes, byte[] decrypted_bytes) throws UnsupportedCryptoAlgorithmException, InvalidPasswordCipherException {
        if (encrypted_bytes[0] == 0) { // we only process if we understand the encoding scheme.
            Cipher c;
            try {
                c = Cipher.getInstance("AES/CBC/PKCS5Padding");
                c.init(Cipher.DECRYPT_MODE, AESKeyManager.getKey(null), AESKeyManager.getIV(null));
                byte[] decrypted = c.doFinal(encrypted_bytes, 1, encrypted_bytes.length - 1);
                if (decrypted != null) {
                    decrypted_bytes = new byte[decrypted.length - decrypted[0] - 1];
                    System.arraycopy(decrypted, decrypted[0] + 1, decrypted_bytes, 0, decrypted_bytes.length);
                }
            } catch (NoSuchAlgorithmException e) {
                throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
            } catch (NoSuchPaddingException e) {
                throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
            } catch (InvalidKeyException e) {
                throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
            } catch (IllegalBlockSizeException e) {
                throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
            } catch (BadPaddingException e) {
                throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
            } catch (InvalidAlgorithmParameterException e) {
                throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
            }
        } else {
            throw new InvalidPasswordCipherException();
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
    public static byte[] encipher(byte[] decrypted_bytes, String crypto_algorithm) throws InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException {
        EncryptedInfo info = encipher_internal(decrypted_bytes, crypto_algorithm, (String) null); // TODO check null
        return info.getEncryptedBytes();
    }

    // ---------------------------------------------------------------------------
    // Method: encipher( decrypted password byte[], crypto algorithm string )
    // Return: encrypted password byte[]
    // ---------------------------------------------------------------------------
    public static EncryptedInfo encipher_internal(byte[] decrypted_bytes, String crypto_algorithm,
                                                  String cryptoKey) throws InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException {
        HashMap<String, String> props = new HashMap<String, String>();
        if (cryptoKey != null) {
            props.put(PasswordUtil.PROPERTY_CRYPTO_KEY, cryptoKey);
        }
        return encipher_internal(decrypted_bytes, crypto_algorithm, props);
    }

    public static EncryptedInfo encipher_internal(byte[] decrypted_bytes, String crypto_algorithm,
                                                  Map<String, String> properties) throws InvalidPasswordCipherException, UnsupportedCryptoAlgorithmException {

        EncryptedInfo info = null;
        byte[] encrypted_bytes = null;

        if (AES.equalsIgnoreCase(crypto_algorithm)) {
            String cryptoKey = null;
            if (properties != null) {
                cryptoKey = properties.get(PasswordUtil.PROPERTY_CRYPTO_KEY);
            }
            info = aesEncipher(decrypted_bytes, cryptoKey, info, encrypted_bytes);
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
     * @param decrypted_bytes
     * @param properties
     * @return
     * @throws InvalidPasswordCipherException
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
            if (algorithm == null) {
                algorithm = PasswordHashGenerator.getDefaultAlgorithm();
            }

            if (!saltSet) {
                saltString = properties.get(PasswordUtil.PROPERTY_HASH_SALT);
                salt = PasswordHashGenerator.generateSalt(saltString);
            }

            if (iteration < 0) {
                String value = properties.get(PasswordUtil.PROPERTY_HASH_ITERATION);
                if (value != null) {
                    iteration = Integer.parseInt(value);
                }
            }
            if (iteration < 0) {
                iteration = PasswordHashGenerator.getDefaultIteration();
            }

            if (length < 0) {
                String value = properties.get(PasswordUtil.PROPERTY_HASH_LENGTH);
                if (value != null) {
                    length = Integer.parseInt(value);
                }
            }
            if (length < 0) {
                length = PasswordHashGenerator.getDefaultOutputLength();
            }
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
     * @param decrypted_bytes
     * @param cryptoKey
     * @param info
     * @param encrypted_bytes
     * @return
     * @throws UnsupportedCryptoAlgorithmException
     * @throws InvalidPasswordCipherException
     */
    private static EncryptedInfo aesEncipher(byte[] decrypted_bytes, String cryptoKey, EncryptedInfo info,
                                             byte[] encrypted_bytes) throws UnsupportedCryptoAlgorithmException, InvalidPasswordCipherException {
        SecureRandom rand = new SecureRandom();
        byte[] seed = rand.generateSeed(20);
        byte[] preEncrypted = new byte[decrypted_bytes.length + 21];
        preEncrypted[0] = 20; // how many seed bytes there are.
        System.arraycopy(seed, 0, preEncrypted, 1, 20);
        System.arraycopy(decrypted_bytes, 0, preEncrypted, 21, decrypted_bytes.length);
        try {
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, AESKeyManager.getKey(cryptoKey), AESKeyManager.getIV(cryptoKey));
            encrypted_bytes = c.doFinal(preEncrypted);
            if (encrypted_bytes != null) {
                byte[] updatedBytes = new byte[encrypted_bytes.length + 1];
                updatedBytes[0] = 0; // indicates how we encoded so later on we can decode
                System.arraycopy(encrypted_bytes, 0, updatedBytes, 1, encrypted_bytes.length);
                info = new EncryptedInfo(updatedBytes, "");
            }
        } catch (NoSuchAlgorithmException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (NoSuchPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (InvalidKeyException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
        } catch (IllegalBlockSizeException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (BadPaddingException e) {
            throw (UnsupportedCryptoAlgorithmException) new UnsupportedCryptoAlgorithmException().initCause(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw (InvalidPasswordCipherException) new InvalidPasswordCipherException().initCause(e);
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
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < SUPPORTED_CRYPTO_ALGORITHMS.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(SUPPORTED_CRYPTO_ALGORITHMS[i]);
        }
        return sb.toString();
    }

    private static CustomPasswordEncryption getCustomImpl() {
        CustomPasswordEncryption cpe = customPasswordEncryption.getService();
        if (cpe == null) {
            cpe = cpeImpl;
        }
        return cpe;
    }

    private static String composeMultipleCustomErrorMessage(List<CustomManifest> list) {
        StringBuffer sb = new StringBuffer(MessageUtils.getMessage("PASSWORDUTIL_DUPLICATE_CUSTOM_ENCRYPTION"));
        for (CustomManifest cm : list) {
            sb.append("\n").append(cm.getLocation());
        }
        return sb.toString();
    }
}

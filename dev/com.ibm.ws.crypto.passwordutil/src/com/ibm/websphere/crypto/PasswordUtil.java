/*******************************************************************************
 * Copyright (c) 1997, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.crypto;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.crypto.util.InvalidPasswordCipherException;
import com.ibm.ws.crypto.util.PasswordCipherUtil;
import com.ibm.ws.crypto.util.PasswordHashGenerator;
import com.ibm.wsspi.security.crypto.EncryptedInfo;

/**
 * Password related utilities.
 */
public class PasswordUtil {
    /**
     * <p>
     * Constant that holds the name of the property for specifying the encryption algorithm for the encode and encode_password method.
     * </p>
     **/
    public final static String PROPERTY_CRYPTO_KEY = "crypto.key";
    /**
     * <p>
     * Constant that holds the name of the property for specifying whether the leading and trailing whitespace omitted
     * from the string for the encode and encode_password method. When the value is true, the whitespace will not be omitted.
     * Otherwise, the whitespace will be omitted.
     * The default value is false.
     * </p>
     **/
    public final static String PROPERTY_NO_TRIM = "option.notrim";
    /**
     * <p>
     * Reserved for future use.
     * </p>
     **/
    public final static String PROPERTY_HASH_ALGORITHM = "hash.algorithm";
    /**
     * <p>
     * Reserved for future use.
     * </p>
     **/
    public final static String PROPERTY_HASH_ITERATION = "hash.iteration";
    /**
     * <p>
     * Reserved for future use.
     * </p>
     **/
    public final static String PROPERTY_HASH_SALT = "hash.salt";
    /**
     * <p>
     * Reserved for future use.
     * </p>
     **/
    public final static String PROPERTY_HASH_ENCODED = "hash.encoded";
    /**
     * <p>
     * Reserved for future use.
     * </p>
     **/
    public final static String PROPERTY_HASH_LENGTH = "hash.length";

    private static final Class<?> CLASS_NAME = PasswordUtil.class;
    private static final String RB = "com.ibm.ws.crypto.util.internal.resources.Messages";
    private final static Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName(), RB);

    private static final String CRYPTO_ALGORITHM_STARTED = "{";
    private static final String CRYPTO_ALGORITHM_STOPPED = "}";

    private static final String EMPTY_STRING = "";
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Return the default algorithm for the encoding or decoding.
     *
     * @return The default algorithm.
     */
    public static final String getDefaultEncoding() {
        return PasswordCipherUtil.getSupportedCryptoAlgorithms()[0];
    }

    /**
     * Decode the provided string. The string should consist of the algorithm to be used for decoding and encoded string.
     * For example, {xor}CDo9Hgw=.
     * Use this method unless the encryption key needs to be specified for the AES encryption.
     * <p>
     * An empty algorithm "{}" is treated as not encoded.
     * However, a missing algorithm will trigger the InvalidPasswordDecodingException.
     *
     * @param encoded_string the string to be decoded.
     * @return The decoded string
     * @throws InvalidPasswordDecodingException If the encoded_string is null or invalid. Or the decoded_string is null.
     * @throws UnsupportedCryptoAlgorithmException If the specified algorithm is not supported for decoding.
     */
    public static String decode(String encoded_string) throws InvalidPasswordDecodingException, UnsupportedCryptoAlgorithmException {
        /*
         * check input:
         *
         * -- encoded_string: any string, any length, cannot be null,
         * must start with valid (supported) crypto algorithm tag
         */

        if (encoded_string == null) {
            // don't accept null password
            throw new InvalidPasswordDecodingException();
        }

        String crypto_algorithm = getCryptoAlgorithm(encoded_string);

        if (crypto_algorithm == null) {
            // don't accept decoded password
            throw new InvalidPasswordDecodingException();
        }

        // valid input ... decode password
        logger.logp(Level.FINEST, PasswordUtil.class.getName(), "decode", "before invoking decode_password : crypto_algorithm : " + crypto_algorithm + "\nencoded_string : "
                                                                          + encoded_string);

        String decoded_string = decode_password(removeCryptoAlgorithmTag(encoded_string), crypto_algorithm);
        if (decoded_string == null) {
            // In order to log the error information when "custom" encryption is invoked prior to the service is activated,
            // this error check was moved to here.
            if (!isValidCryptoAlgorithm(crypto_algorithm)) {
                // don't accept unsupported crypto algorithm
                throw new UnsupportedCryptoAlgorithmException();
            } else {
                throw new InvalidPasswordDecodingException();
            }
        }

        return decoded_string;
    }

    /**
     * Encode the provided password by using the default encoding algorithm. The encoded string consists of the algorithm of the encoding and the encoded value.
     * For example, {xor}CDo9Hgw=.
     * If the decoded_string is already encoded, the string will be decoded and then encoded by using the default encoding algorithm.
     * Use this method for encoding the string by using the default encoding algorithm.
     * 
     * @param decoded_string the string to be encoded.
     * @return The encoded string.
     * @throws InvalidPasswordEncodingException If the decoded_string is null or invalid. Or the encoded_string is null.
     * @throws UnsupportedCryptoAlgorithmException If the algorithm is not supported.
     */
    public static String encode(String decoded_string) throws InvalidPasswordEncodingException, UnsupportedCryptoAlgorithmException {
        return encode(decoded_string, PasswordCipherUtil.getSupportedCryptoAlgorithms()[0], (String) null);
    }

    /**
     * Encode the provided password by using the specified encoding algorithm. The encoded string consistes of the
     * algorithm of the encoding and the encoded value.
     * If the decoded_string is already encoded, the string will be decoded and then encoded by using the specified crypto algorithm.
     * Use this method for encoding the string by using specific encoding algorithm.
     * Use securityUtility encode --listCustom command line utility to see if any additional custom encryptions are supported.
     *
     * @param decoded_string the string to be encoded.
     * @param crypto_algorithm the algorithm to be used for encoding. The supported values are xor, aes, or hash.
     * @return The encoded string.
     * @throws InvalidPasswordEncodingException If the decoded_string is null or invalid. Or the encoded_string is null.
     * @throws UnsupportedCryptoAlgorithmException If the algorithm is not supported.
     */
    public static String encode(String decoded_string, String crypto_algorithm) throws InvalidPasswordEncodingException, UnsupportedCryptoAlgorithmException {
        return encode(decoded_string, crypto_algorithm, (String) null);
    }

    /**
     * Encode the provided string with the specified algorithm and the crypto key
     * If the decoded_string is already encoded, the string will be decoded and then encoded by using the specified crypto algorithm.
     * Use this method for encoding the string by using the AES encryption with the specific crypto key.
     * Note that this method is only avaiable for the Liberty profile.
     * 
     * @param decoded_string the string to be encoded.
     * @param crypto_algorithm the algorithm to be used for encoding.
     * @param crypto_key the key for the encryption. This value is only valid for aes algorithm.
     * @return The encoded string.
     * @throws InvalidPasswordEncodingException If the decoded_string is null or invalid. Or the encoded_string is null.
     * @throws UnsupportedCryptoAlgorithmException If the algorithm is not supported.
     */
    public static String encode(String decoded_string, String crypto_algorithm, String crypto_key) throws InvalidPasswordEncodingException, UnsupportedCryptoAlgorithmException {
        HashMap<String, String> props = new HashMap<String, String>();
        if (crypto_key != null) {
            props.put(PROPERTY_CRYPTO_KEY, crypto_key);
        }
        return encode(decoded_string, crypto_algorithm, props);
    }

    /**
     * Encode the provided string with the specified algorithm and the properties
     * If the decoded_string is already encoded, the string will be decoded and then encoded by using the specified crypto algorithm.
     * Note that this method is only avaiable for the Liberty profile.
     *
     * @param decoded_string the string to be encoded.
     * @param crypto_algorithm the algorithm to be used for encoding. The supported values are xor, aes, or hash.
     * @param properties the properties for the encryption.
     * @return The encoded string.
     * @throws InvalidPasswordEncodingException If the decoded_string is null or invalid. Or the encoded_string is null.
     * @throws UnsupportedCryptoAlgorithmException If the algorithm is not supported.
     */
    public static String encode(String decoded_string, String crypto_algorithm,
                                Map<String, String> properties) throws InvalidPasswordEncodingException, UnsupportedCryptoAlgorithmException {
        /*
         * check input:
         *
         * -- decoded_string: any string, any length, cannot be null,
         * cannot start with crypto algorithm tag
         *
         * -- crypto_algorithm: any string, any length, cannot be null,
         * must be valid (supported) crypto algorithm
         */

        if (!isValidCryptoAlgorithm(crypto_algorithm)) {
            // don't accept unsupported crypto algorithm
            throw new UnsupportedCryptoAlgorithmException();
        }

        if (decoded_string == null) {
            // don't accept null password
            throw new InvalidPasswordEncodingException();
        }

        String current_crypto_algorithm = getCryptoAlgorithm(decoded_string);

        if ((current_crypto_algorithm != null && current_crypto_algorithm.startsWith(crypto_algorithm)) || isHashed(decoded_string)) {
            // don't accept encoded password
            throw new InvalidPasswordEncodingException();
        } else if (current_crypto_algorithm != null) {
            decoded_string = passwordDecode(decoded_string);
        }
        if (properties == null || !properties.containsKey(PROPERTY_NO_TRIM) || !"true".equalsIgnoreCase(properties.get(PROPERTY_NO_TRIM))) {
            decoded_string = decoded_string.trim();
        }
        String encoded_string = encode_password(decoded_string, crypto_algorithm.trim(), properties);

        if (encoded_string == null) {
            throw new InvalidPasswordEncodingException();
        }

        return encoded_string;
    }

    /**
     * Return the crypto algorithm of the provided password.
     * For example, if the password is {xor}CDo9Hgw=, "xor" will be returned.
     *
     * @param password the encoded string with encoding algorithm.
     * @return The encoding algorithm. Null if not present.
     */
    public static String getCryptoAlgorithm(String password) {
        if (null == password) {
            return null;
        }
        String algorithm = null;
        String data = password.trim();

        if (data.length() >= 2) {
            if ('{' == data.charAt(0)) {
                int end = data.indexOf('}', 1);
                if (end > 0) {
                    algorithm = data.substring(1, end).trim();
                }
            }
        }

        return algorithm;
    }

    /**
     * Return the algorithm tag of the provided string.
     * For example, if the password is {xor}CDo9Hgw=, "{xor}" will be returned.
     *
     * @param password the encoded string with encoding algorithm.
     * @return The encoding algorithm with algorithm tags. Null if not present.
     */
    public static String getCryptoAlgorithmTag(String password) {
        if (null == password) {
            return null;
        }
        String tag = null;
        String data = password.trim();

        if (data.length() >= 2) {
            if ('{' == data.charAt(0)) {
                int end = data.indexOf('}', 1);
                if (end > 0) {
                    end++; // we want to include the end marker
                    if (end == data.length()) {
                        tag = data;
                    } else {
                        tag = data.substring(0, end).trim();
                    }
                }
            }
        }

        return tag;
    }

    /**
     * Check whether the encoded string contains a valid crypto algorithm.
     * For example, "{xor}CDo9Hgw=" returns true, while "{unknown}CDo9Hgw=" or "CDo9Hgw=" returns false.
     *
     * @param encoded_string the encoded string.
     * @return true if the encoding algorithm is supported.
     */
    public static boolean isEncrypted(String encoded_string) {
        String algorithm = getCryptoAlgorithm(encoded_string);
        return isValidCryptoAlgorithm(algorithm);
    }

    /**
     * Determine if the provided algorithm string is valid.
     * The valid values are xor, aes, or hash.
     * Use securityUtility encode --listCustom command line utility to see if any additional custom encryptions are supported.
     *
     * @param crypto_algorithm the string of algorithm.
     * @return true if the algorithm is supported. false otherwise.
     */
    public static boolean isValidCryptoAlgorithm(String crypto_algorithm) {
        if (crypto_algorithm != null) {
            String algorithm = crypto_algorithm.trim();

            if (algorithm.length() == 0) {
                return true;
            }
            String[] SUPPORTED_CRYPTO_ALGORITHMS = PasswordCipherUtil.getSupportedCryptoAlgorithms();
            for (int i = 0; i < SUPPORTED_CRYPTO_ALGORITHMS.length; i++) {
                if (algorithm.startsWith(SUPPORTED_CRYPTO_ALGORITHMS[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determine if the provided algorithm tag is valid. the algorithm tag consists of "{<algorithm>}" such as "{xor}".
     * The valid values are {xor}, {aes}, or {hash}.
     * Use securityUtility encode --listCustom command line utility to see if any additional custom encryptions are supported.
     *
     * @param tag the string of algorithm tag to be examined.
     * @return true if the algorithm is supported. false otherwise.
     */
    public static boolean isValidCryptoAlgorithmTag(String tag) {
        return isValidCryptoAlgorithm(getCryptoAlgorithm(tag));
    }

    /**
     * Determine if the provided string is hashed by examining the algorithm tag.
     * Note that this method is only avaiable for the Liberty profile.
     *
     * @param encoded_string the string with the encoded algorithm tag.
     * @return true if the encoded algorithm is hash. false otherwise.
     */
    public static boolean isHashed(String encoded_string) {
        String algorithm = getCryptoAlgorithm(encoded_string);
        return isValidAlgorithm(algorithm, PasswordCipherUtil.getSupportedHashAlgorithms());
    }

    private static boolean isValidAlgorithm(String cryptoAlgorithm, String[] supportList) {
        boolean value = false;
        if (cryptoAlgorithm != null && supportList != null) {
            String algorithm = cryptoAlgorithm.trim();
            if (algorithm != null && algorithm.length() > 0) {
                for (int i = 0; i < supportList.length; i++) {
                    if (algorithm.startsWith(supportList[i])) {
                        value = true;
                        break;
                    }
                }
            }
        }
        return value;
    }

    /**
     * Decode the provided string. The string should consist of the algorithm to be used for decoding and encoded string.
     * For example, {xor}CDo9Hgw=.
     *
     * @param encoded_string the string to be decoded.
     * @return The decoded string, null if there is any failure during decoding, or invalid or null encoded_string.
     */
    public static String passwordDecode(String encoded_string) {
        /*
         * check input:
         *
         * -- encoded_string: any string, any length, cannot be null,
         * may start with valid (supported) crypto algorithm tag
         */

        if (encoded_string == null) {
            // don't accept null password
            return null;
        }

        String crypto_algorithm = getCryptoAlgorithm(encoded_string);

        if (crypto_algorithm == null) {
            // password not encoded
            return encoded_string;
        }

        // valid input ... decode password
        return decode_password(removeCryptoAlgorithmTag(encoded_string), crypto_algorithm);
    }

    /**
     * Encode the provided password by using the default encoding algorithm. The encoded string consists of the algorithm of the encoding and the encoded value.
     * For example, {xor}CDo9Hgw=.
     *
     * @param decoded_string the string to be encoded.
     * @return The encoded string. null if there is any failure during encoding, or invalid or null decoded_string
     */
    public static String passwordEncode(String decoded_string) {
        return passwordEncode(decoded_string, PasswordCipherUtil.getSupportedCryptoAlgorithms()[0]);
    }

    /**
     * Encode the provided password with the algorithm. If another algorithm
     * is already applied, it will be removed and replaced with the new algorithm.
     *
     * @param decoded_string the string to be encoded, or the encoded string.
     * @param crypto_algorithm the algorithm to be used for encoding. The supported values are xor, aes, or hash.
     * @return The encoded string. Null if there is any failure during encoding, or invalid or null decoded_string
     */
    public static String passwordEncode(String decoded_string, String crypto_algorithm) {
        /*
         * check input:
         *
         * -- decoded_string: any string, any length, cannot be null,
         * may start with valid (supported) crypto algorithm tag
         *
         * -- crypto_algorithm: any string, any length, cannot be null,
         * must be valid (supported) crypto algorithm
         */

        if (decoded_string == null) {
            // don't accept null password
            return null;
        }

        String current_crypto_algorithm = getCryptoAlgorithm(decoded_string);

        if (current_crypto_algorithm != null && current_crypto_algorithm.equals(crypto_algorithm)) {
            // Return the decoded_string if it is tagged with a valid crypto algorithm.
            if (isValidCryptoAlgorithm(current_crypto_algorithm))
                return decoded_string.trim();
            return null;
        } else if (current_crypto_algorithm != null) {
            decoded_string = passwordDecode(decoded_string);
        }

        // valid input ... encode password
        return encode_password(decoded_string.trim(), crypto_algorithm.trim(), null); // TODO check this
    }

    /**
     * Remove the algorithm tag from the input encoded password.
     *
     * @param password the string which contains the crypto algorithm tag.
     * @return The string which the crypto algorithm tag is removed.
     */
    public static String removeCryptoAlgorithmTag(String password) {
        if (null == password) {
            return null;
        }

        String rc = null;
        String data = password.trim();
        if (data.length() >= 2) {
            if ('{' == data.charAt(0)) {
                int end = data.indexOf('}', 1);
                if (end > 0) {
                    end++; // we want to jump past the end marker
                    if (end == data.length()) {
                        rc = EMPTY_STRING;
                    } else {
                        rc = data.substring(end).trim();
                    }
                }
            }
        }

        return rc;
    }

    /**
     * Convert the provided string to a byte[] using the UTF-8 encoding.
     *
     * @param string
     * @return byte[] - null if input is null or if UTF-8 is not supported
     */
    private static byte[] convert_to_bytes(String string) {
        if (null == string) {
            return null;
        }
        if (0 == string.length()) {
            return EMPTY_BYTE_ARRAY;
        }
        return string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convert the byte[] data to a String using the UTF-8 encoding.
     *
     * @param bytes
     * @return String - null if input is null or if UTF-8 is not supported
     */
    private static String convert_to_string(byte[] bytes) {
        if (null == bytes) {
            return null;
        }
        if (0 == bytes.length) {
            return EMPTY_STRING;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Convert the string to bytes using UTF-8 encoding and then run it through
     * the base64 decoding.
     *
     * @param string
     * @return byte[] - null if null input or an error in the conversion happens
     */
    private static byte[] convert_viewable_to_bytes(String string) {
        if (null == string) {
            return null;
        }
        if (0 == string.length()) {
            return EMPTY_BYTE_ARRAY;
        }
        return Base64Coder.base64Decode(convert_to_bytes(string));
    }

    /**
     * Use base64 encoding on the bytes and then convert them to a string using
     * UTF-8 encoding.
     *
     * @param bytes
     * @return String - null if input is null or an error happens during the conversion
     */
    private static String convert_viewable_to_string(byte[] bytes) {
        String string = null;

        if (bytes != null) {
            if (bytes.length == 0) {
                string = EMPTY_STRING;
            } else {
                string = convert_to_string(Base64Coder.base64Encode(bytes));
            }
        }
        return string;
    }

    /**
     * Decode the provided string with the specified algorithm.
     *
     * @param encoded_string
     * @param crypto_algorithm
     * @return String
     */
    private static String decode_password(String encoded_string, String crypto_algorithm) {
        /*
         * decoding process:
         *
         * -- check for empty algorithm tag
         * -- convert input String to byte[] using base64 decoding
         * -- decipher byte[]
         * -- convert byte[] to String using UTF8 conversion code
         */

        StringBuilder buffer = new StringBuilder();

        if (crypto_algorithm.length() == 0) {
            // crypto algorithm is empty ... don't decode password
            buffer.append(encoded_string);
        } else {
            // decode password with specified crypto algorithm
            String decoded_string = null;
            if (encoded_string.length() > 0) {
                // convert viewable string to encrypted password byte[]
                byte[] encrypted_bytes = convert_viewable_to_bytes(encoded_string);
                logger.logp(Level.FINEST, PasswordUtil.class.getName(), "decode_password", "byte array before decoding\n" + PasswordHashGenerator.hexDump(encrypted_bytes));

                if (encrypted_bytes == null) {
                    // base64 decoding failed
                    logger.logp(Level.SEVERE, PasswordUtil.class.getName(), "decode_password", "PASSWORDUTIL_INVALID_BASE64_STRING");
                    return null;
                }

                if (encrypted_bytes.length > 0) {
                    // decrypt encrypted password byte[] with specified crypto algorithm

                    byte[] decrypted_bytes = null;

                    try {
                        decrypted_bytes = PasswordCipherUtil.decipher(encrypted_bytes, crypto_algorithm);
                    } catch (InvalidPasswordCipherException e) {
                        logger.logp(Level.SEVERE, PasswordUtil.class.getName(), "decode_password", "PASSWORDUTIL_CYPHER_EXCEPTION", e);
                        return null;
                    } catch (UnsupportedCryptoAlgorithmException e) {
                        logger.logp(Level.SEVERE, PasswordUtil.class.getName(), "decode_password", "PASSWORDUTIL_UNKNOWN_ALGORITHM_EXCEPTION", e);
                        return null;
                    }

                    if ((decrypted_bytes != null) && (decrypted_bytes.length > 0)) {
                        // convert decrypted password byte[] to string
                        decoded_string = convert_to_string(decrypted_bytes);
                    }
                }
            }

            if ((decoded_string != null) && (decoded_string.length() > 0)) {
                // append decoded string
                buffer.append(decoded_string);
            }
        }

        return buffer.toString();
    }

    /**
     * Encode the provided string by using the specified encoding algorithm and properties
     *
     * @param decoded_string the string to be encoded.
     * @param crypto_algorithm the algorithm to be used for encoding. The supported values are xor, aes, or hash.
     * @param properties the properties for the encryption.
     * @return The encoded string. null if there is any failure during encoding, or invalid or null decoded_string
     */
    public static String encode_password(String decoded_string, String crypto_algorithm, Map<String, String> properties) {
        /*
         * encoding process:
         *
         * -- check for empty algorithm tag
         * -- convert input String to byte[] UTF8 conversion code
         * -- encipher byte[]
         * -- convert byte[] to String using using base64 encoding
         */

        StringBuilder buffer = new StringBuilder();
        buffer.append(CRYPTO_ALGORITHM_STARTED);

        if (crypto_algorithm.length() == 0) {
            // crypto algorithm is empty ... don't encode password
            buffer.append(CRYPTO_ALGORITHM_STOPPED).append(decoded_string);
        } else {
            // encode password with specified crypto algorithm

            String encoded_string = null;
            EncryptedInfo info = null;

            if (decoded_string.length() > 0) {
                // convert decoded password string to byte[]

                byte[] decrypted_bytes = convert_to_bytes(decoded_string);

                if (decrypted_bytes.length > 0) {
                    // encrypt decrypted password byte[] with specified crypto algorithm

                    byte[] encrypted_bytes = null;
                    boolean done = false;
                    while (!done) {
                        try {
                            info = PasswordCipherUtil.encipher_internal(decrypted_bytes, crypto_algorithm, properties);
                            if (info != null) {
                                encrypted_bytes = info.getEncryptedBytes();
                            }

                            done = true;
                        } catch (InvalidPasswordCipherException e) {
                            logger.logp(Level.SEVERE, PasswordUtil.class.getName(), "encode_password", "PASSWORDUTIL_CYPHER_EXCEPTION", e);
                            return null;
                        } catch (UnsupportedCryptoAlgorithmException e) {
                            logger.logp(Level.SEVERE, PasswordUtil.class.getName(), "encode_password", "PASSWORDUTIL_UNKNOWN_ALGORITHM_EXCEPTION", e);
                            return null;
                        }
                    }
                    if ((encrypted_bytes != null) && (encrypted_bytes.length > 0)) {
                        // convert encrypted password byte[] to viewable string

                        encoded_string = convert_viewable_to_string(encrypted_bytes);

                        if (encoded_string == null) {
                            // base64 encoding failed
                            return null;
                        }
                    }
                }
            }

            buffer.append(crypto_algorithm);
            String alias = (null == info) ? null : info.getKeyAlias();
            if (alias != null && 0 < alias.length()) {
                buffer.append(':').append(alias);
            }
            buffer.append(CRYPTO_ALGORITHM_STOPPED);

            if ((encoded_string != null) && (encoded_string.length() > 0)) {
                // append encoded string
                buffer.append(encoded_string);
            }
        }

        return buffer.toString();
    }

}

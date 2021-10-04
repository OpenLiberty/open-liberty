/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.crypto.InvalidPasswordEncodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.crypto.util.PasswordCipherUtil;
import com.ibm.ws.crypto.util.UnsupportedConfigurationException;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;
import com.ibm.ws.security.utility.utils.SAFEncryptionKey;

/**
 * Main class for password encryption utility.
 * Not bundled with the core runtime jars by design.
 */
public class EncodeTask extends BaseCommandTask {
    private static final String ARG_ENCODING = "--encoding";
    private static final String ARG_KEY = "--key";
    private static final String ARG_PASSWORD = "--password";
    private static final String ARG_NO_TRIM = "--notrim";
    private static final String ARG_LIST_CUSTOM = "--listCustom";
    private static final String ARG_HASH_SALT = "--salt";
    private static final String ARG_HASH_ITERATION = "--iteration";
    private static final String ARG_HASH_ALGORITHM = "--algorithm";
    private static final String ARG_HASH_ENCODED = "--encoded"; // this is for debug
    private static final String ARG_KEYRING = "--keyring";
    private static final String ARG_KEYRING_TYPE = "--keyringType";
    private static final String ARG_KEY_LABEL = "--keyLabel";
    private static final List<String> ARG_TABLE = Arrays.asList(ARG_ENCODING, ARG_KEY, ARG_LIST_CUSTOM, ARG_PASSWORD, ARG_HASH_SALT, ARG_HASH_ITERATION, ARG_HASH_ALGORITHM,
                                                                ARG_HASH_ENCODED, ARG_KEYRING, ARG_KEYRING_TYPE, ARG_KEY_LABEL);

    public EncodeTask(String scriptName) {
        super(scriptName);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "encode";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        String customJSON = null;
        String customAlgorithm = "";
        String customDescription = "";
        try {
            customJSON = PasswordCipherUtil.listCustom();
            if (customJSON != null) {
                JSONArray customInfoArray = (JSONArray) JSON.parse(customJSON);
                customAlgorithm = getAlgorithm(customInfoArray);
                customDescription = getDescription(customInfoArray);
            }
        } catch (UnsupportedConfigurationException uce) {
            // do nothing. The error message will be logged when listCustom option is used.
        } catch (IOException ioe) {
            // if there is a json error. forget about custom.
        }

        return getTaskHelp("encode.desc", "encode.usage.options",
                           null, null,
                           "encode.option-key.", "encode.option-desc.",
                           null, null,
                           scriptName, customAlgorithm, customDescription);
    }

    @Override
    public String getTaskDescription() {
        return getOption("encode.desc", true);
    }

    /**
     * Handle encoding of the plaintext provided. Capture any
     * Exceptions and print the stack trace.
     *
     * @param plaintext
     * @param encodingType
     * @param encodingKey
     * @return ciphertext
     * @throws InvalidPasswordEncodingException
     * @throws UnsupportedCryptoAlgorithmException
     */
    private String encode(PrintStream stderr, String plaintext, String encodingType,
                          Map<String, String> properties) throws InvalidPasswordEncodingException, UnsupportedCryptoAlgorithmException {
        String ret = null;
        try {
            ret = PasswordUtil.encode(plaintext, encodingType == null ? PasswordUtil.getDefaultEncoding() : encodingType, properties);
        } catch (InvalidPasswordEncodingException e) {
            e.printStackTrace(stderr);
            throw e;
        } catch (UnsupportedCryptoAlgorithmException e) {
            e.printStackTrace(stderr);
            throw e;
        }
        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {
        Map<String, String> argMap = parseArgumentList(args);
        if (argMap.containsKey(ARG_LIST_CUSTOM)) {
            String output = PasswordCipherUtil.listCustom();
            if (output == null) {
                output = getMessage("no.custom.encyption");
            }
            stdout.println(output);
        } else {
            String encoding = argMap.get(ARG_ENCODING);
            Map<String, String> props = convertToProperties(argMap);
            // need to add the key if this is AES/SAF and keyring parameters are provided
            if (isZOS()) {
                props = getKeyIfSAF(encoding, props);
            } else {
                //Not z/OS just make sure Z specific parameters are not used
                checkForZArgs(props);
            }
            if (!!!argMap.containsKey(ARG_PASSWORD)) {
                stdout.println(encode(stderr, promptForText(stdin, stdout), encoding, props));
            } else {
                stdout.println(encode(stderr, argMap.get(ARG_PASSWORD), encoding, props));
            }
        }

        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * @param props
     */
    private void checkForZArgs(Map<String, String> props) throws IllegalArgumentException {
        // Lets make sure the Z args are not being used
        String keyring = props.get(PasswordUtil.PROPERTY_KEYRING);
        String type = props.get(PasswordUtil.PROPERTY_KEYRING_TYPE);
        String label = props.get(PasswordUtil.PROPERTY_KEY_LABEL);

        if (keyring != null || type != null || label != null) {
            throw new IllegalArgumentException(getMessage("saf.arg.not.onZ"));
        }
    }

    /**
     * @return boolean true if the system is Z/OS false otherwise
     */
    private boolean isZOS() {

        boolean isZSeries = false;
        String _osName = System.getProperty("os.name");
        isZSeries = ((_osName.indexOf("OS/390") != -1) || (_osName.indexOf("z/OS") != -1));
        return isZSeries;
    }

    /**
     * @param encoding
     * @param props
     * @return
     */
    private Map<String, String> getKeyIfSAF(String encoding, Map<String, String> props) throws Exception {

        Map<String, String> p = props;
        String cryptoKey = null;

        String keyring = props.get(PasswordUtil.PROPERTY_KEYRING);
        String type = props.get(PasswordUtil.PROPERTY_KEYRING_TYPE);
        String label = props.get(PasswordUtil.PROPERTY_KEY_LABEL);

        if (encoding != null && encoding.trim().equalsIgnoreCase("aes")) {
            if ((keyring != null && !keyring.isEmpty()) && (type != null && !type.isEmpty()) && (label != null && !label.isEmpty())) {
                SAFEncryptionKey ek = new SAFEncryptionKey(keyring, type, label);
                cryptoKey = ek.getKey();
                p.put(PasswordUtil.PROPERTY_CRYPTO_KEY, cryptoKey);
            }
        } else {
            //This is not aes, lets error if the keyring args are used
            if (keyring != null || type != null || label != null) {
                throw new IllegalArgumentException(getMessage("saf.arg.not.aes"));
            }
        }

        return p;
    }

    /**
     *
     * @param args
     */
    private Map<String, String> parseArgumentList(String[] args) {
        Map<String, String> result = new HashMap<String, String>();
        // Skip the first argument as it is the task name
        String arg = null;
        for (int i = 1; i < args.length; i++) {
            arg = args[i];
            if (arg.startsWith("--")) {
                if (arg.equals(ARG_NO_TRIM) || arg.equals(ARG_LIST_CUSTOM)) {
                    result.put(arg, "true");
                } else {
                    int index = arg.indexOf('=');
                    if (index == -1) {
                        // Any options specified must have values
                        throw new IllegalArgumentException(getMessage("invalidArg", arg));
                    }
                    String value = null;
                    if (index + 1 < arg.length()) {
                        value = arg.substring(index + 1);
                    }
                    arg = arg.substring(0, index);
                    if (!isKnownArgument(arg)) {
                        throw new IllegalArgumentException(getMessage("invalidArg", arg));
                    } else if (value == null) {
                        throw new IllegalArgumentException(getMessage("missingValue", arg));
                    }
                    result.put(arg, value);
                }
            } else if (result.containsKey(ARG_PASSWORD)) {
                // A non-option argument to be encoded has already been recorded
                throw new IllegalArgumentException(getMessage("invalidArg", arg));
            } else {
                // The first non-option argument is assumed to be the value to be encoded
                result.put(ARG_PASSWORD, arg);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        boolean value = false;
        if (arg != null) {
            value = ARG_TABLE.contains(arg);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        // validateArgumentList is not used by this implementation
    }

    /**
     * Returns the message string of custom encryption name(s).
     *
     * @param customInfoArray JSONArray which contains the list of custom encryption information. Null is not expected.
     */
    protected String getAlgorithm(JSONArray customInfoArray) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < customInfoArray.size(); i++) {
            JSONObject customInfo = (JSONObject) customInfoArray.get(i);
            sb.append("|").append(customInfo.get("name"));
        }
        return sb.toString();
    }

    /**
     * Returns the message string of the custom encryption information.
     *
     * @param customInfoArray JSONArray which contains the list of custom encryption information. Null is not expected.
     */
    protected String getDescription(JSONArray customInfoArray) {
        StringBuffer sb = new StringBuffer();
        sb.append(getMessage("encode.option-custom.encryption"));
        for (int i = 0; i < customInfoArray.size(); i++) {
            JSONObject customInfo = (JSONObject) customInfoArray.get(i);
            String name = (String) customInfo.get("name");
            sb.append(getMessage("encode.option-desc.custom.feature", name));
            sb.append((String) customInfo.get("featurename"));
            sb.append(getMessage("encode.option-desc.custom.description", name));
            sb.append((String) customInfo.get("description"));
        }
        return sb.toString();
    }

    /**
     * Convert the properties for encoding from the command line parameters.
     */
    protected Map<String, String> convertToProperties(Map<String, String> argMap) {
        HashMap<String, String> props = new HashMap<String, String>();

        String value = argMap.get(ARG_KEY);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_CRYPTO_KEY, value);
        }
        if (argMap.containsKey(ARG_NO_TRIM)) {
            props.put(PasswordUtil.PROPERTY_NO_TRIM, "true");
        }
        value = argMap.get(ARG_HASH_SALT);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_HASH_SALT, value);
        }
        value = argMap.get(ARG_HASH_ITERATION);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_HASH_ITERATION, value);
        }
        value = argMap.get(ARG_HASH_ALGORITHM);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_HASH_ALGORITHM, value);
        }
        // following value are for debug
        value = argMap.get(ARG_HASH_ENCODED);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_HASH_ENCODED, value);
        }
        value = argMap.get(ARG_KEYRING);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_KEYRING, value);
        }
        value = argMap.get(ARG_KEYRING_TYPE);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_KEYRING_TYPE, value);
        }
        value = argMap.get(ARG_KEY_LABEL);
        if (value != null) {
            props.put(PasswordUtil.PROPERTY_KEY_LABEL, value);
        }

        return props;
    }

}

/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
package com.ibm.ws.security.utility.tasks;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.crypto.InvalidPasswordEncodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.crypto.util.PasswordCipherUtil;
import com.ibm.ws.crypto.util.UnsupportedConfigurationException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;
import com.ibm.ws.security.utility.utils.SAFEncryptionKey;

/**
 * Main class for password encryption utility.
 * Not bundled with the core runtime jars by design.
 */
public class EncodeTask extends BaseCommandTask {
    private static final String ATTR_NAME = "name";
    private static final List<Set<String>> EXCLUSIVE_ARGUMENTS = Arrays.asList(
                                                                               new HashSet<String>(Arrays.asList(BaseCommandTask.ARG_KEY, BaseCommandTask.ARG_BASE64_KEY,
                                                                                                                 BaseCommandTask.ARG_AES_CONFIG_FILE)));

    private static final List<String> ARG_TABLE = Arrays.asList(BaseCommandTask.ARG_ENCODING, BaseCommandTask.ARG_KEY, BaseCommandTask.ARG_LIST_CUSTOM,
                                                                BaseCommandTask.ARG_PASSWORD, BaseCommandTask.ARG_HASH_SALT, BaseCommandTask.ARG_HASH_ITERATION,
                                                                BaseCommandTask.ARG_HASH_ALGORITHM,
                                                                BaseCommandTask.ARG_HASH_ENCODED, BaseCommandTask.ARG_KEYRING, BaseCommandTask.ARG_KEYRING_TYPE,
                                                                BaseCommandTask.ARG_KEY_LABEL);
    private static final List<String> BETA_ARG_TABLE = Arrays.asList(BaseCommandTask.ARG_BASE64_KEY, BaseCommandTask.ARG_AES_CONFIG_FILE);
    private static final List<String> BETA_OPTS = BETA_ARG_TABLE.stream().map(s -> s.startsWith("--") ? s.substring(2) : s).collect(Collectors.toList());

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
        if (argMap.containsKey(BaseCommandTask.ARG_LIST_CUSTOM)) {
            String output = PasswordCipherUtil.listCustom();
            if (output == null) {
                output = getMessage("no.custom.encyption");
            }
            stdout.println(output);
        } else {
            String encoding = argMap.get(BaseCommandTask.ARG_ENCODING);
            Map<String, String> props = BaseCommandTask.convertToProperties(argMap, stdout);
            // need to add the key if this is AES/SAF and keyring parameters are provided
            if (isZOS()) {
                props = getKeyIfSAF(encoding, props);
            } else {
                //Not z/OS just make sure Z specific parameters are not used
                checkForZArgs(props);
            }
            if (!!!argMap.containsKey(BaseCommandTask.ARG_PASSWORD)) {
                stdout.println(encode(stderr, promptForText(stdin, stdout), encoding, props));
            } else {
                stdout.println(encode(stderr, argMap.get(BaseCommandTask.ARG_PASSWORD), encoding, props));
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
                if (arg.equals(BaseCommandTask.ARG_NO_TRIM) || arg.equals(BaseCommandTask.ARG_LIST_CUSTOM)) {
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
            } else if (result.containsKey(BaseCommandTask.ARG_PASSWORD)) {
                // A non-option argument to be encoded has already been recorded
                throw new IllegalArgumentException(getMessage("invalidArg", arg));
            } else {
                // The first non-option argument is assumed to be the value to be encoded
                result.put(BaseCommandTask.ARG_PASSWORD, arg);
            }
            this.validateMutuallyExclusiveArgs(arg, result);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        boolean value = false;
        if (arg != null) {
            value = ARG_TABLE.contains(arg);
            if (!value && ProductInfo.getBetaEdition()) {
                value = BETA_ARG_TABLE.contains(arg);
            }
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        // checkRequiredArguments is not used by this implementation
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
            sb.append("|").append(customInfo.get(ATTR_NAME));
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
            String name = (String) customInfo.get(ATTR_NAME);
            sb.append(getMessage("encode.option-desc.custom.feature", name));
            sb.append((String) customInfo.get("featurename"));
            sb.append(getMessage("encode.option-desc.custom.description", name));
            sb.append((String) customInfo.get("description"));
        }
        return sb.toString();
    }

    @Override
    protected List<String> getBetaOptions() {
        return BETA_OPTS;
    }

    @Override
    protected List<Set<String>> getExclusiveArguments() {
        return EXCLUSIVE_ARGUMENTS;
    }

}
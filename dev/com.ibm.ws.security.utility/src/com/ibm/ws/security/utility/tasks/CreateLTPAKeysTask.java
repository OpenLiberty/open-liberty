/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

import java.io.File;
import java.io.PrintStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.crypto.ltpakeyutil.AesLTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.crypto.util.AESKeyManager;
import com.ibm.ws.crypto.util.AesConfigFileParser;
import com.ibm.ws.crypto.util.ICSFSecretKeyResolver;
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;
import com.ibm.ws.security.utility.utils.SAFEncryptionKey;

/**
 * Usage options:
 * createLTPAKeys --password WebAS -> creates a local ltpa.keys file
 * createLTPAKeys --server serverName --password WebAS -> creates a ltpa.keys file in the server
 * createLTPAKeys --file fileName --password WebAS -> creates a fileName file
 * createLTPAKeys --useEncryptionKey=true --passwordKey=myKey --file fileName -> creates a fileName file protected by AES key
 */
public class CreateLTPAKeysTask extends BaseCommandTask {
    static final String SLASH = String.valueOf(File.separatorChar);

    static final String DEFAULT_LTPA_KEY_FILE = "ltpa.keys";

    static final String ARG_PASSWORD = "--password";
    static final String ARG_SERVER = "--server";
    static final String ARG_FILE = "--file";
    static final String ARG_USE_ENCRYPTION_KEY = "--useEncryptionKey";
    private static final List<String> BETA_ARG_TABLE = new ArrayList<>();
    private static final List<String> BETA_OPTS = BETA_ARG_TABLE.stream().map(s -> s.startsWith("--") ? s.substring(2) : s).collect(Collectors.toList());
    private final LTPAKeyFileUtility ltpaKeyFileUtil;
    private final IFileUtility fileUtility;
    protected ConsoleWrapper stdin;
    protected PrintStream stdout;
    private static final List<Set<String>> EXCLUSIVE_ARGUMENTS = Arrays.asList(
                                                                               new HashSet<String>(Arrays.asList(BaseCommandTask.ARG_PASSWORD_KEY,
                                                                                                                 BaseCommandTask.ARG_PASSWORD_BASE64_KEY,
                                                                                                                 BaseCommandTask.ARG_AES_CONFIG_FILE)),
                                                                               new HashSet<String>(Arrays.asList(ARG_SERVER,
                                                                                                                 ARG_FILE)));

    /** Constant for the ICSF keyring type value (matches zosPasswordEncryptionKey type="ICSF"). */
    private static final String KEYRING_TYPE_ICSF = "ICSF";

    /**
     * @param scriptName The name of the script to which this task belongs
     */
    public CreateLTPAKeysTask(LTPAKeyFileUtility ltpaKeyFileUtil, IFileUtility fileUtility, String scriptName) {
        super(scriptName);
        this.ltpaKeyFileUtil = ltpaKeyFileUtil;
        this.fileUtility = fileUtility;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "createLTPAKeys";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("createLTPAKeys.desc", true);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("createLTPAKeys.desc", "createLTPAKeys.usage.options",
                           "createLTPAKeys.required-key.", "createLTPAKeys.required-desc.",
                           "createLTPAKeys.option-key", "createLTPAKeys.option-desc",
                           null, null, scriptName);
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        return arg.equals(ARG_SERVER) || arg.equals(ARG_PASSWORD) ||
               arg.equals(ARG_PASSWORD_ENCODING) || arg.equals(ARG_PASSWORD_KEY) ||
               arg.equals(ARG_FILE) || arg.equals(ARG_PASSWORD_BASE64_KEY) || arg.equals(ARG_AES_CONFIG_FILE) ||
               arg.equals(BaseCommandTask.ARG_KEYRING) || arg.equals(BaseCommandTask.ARG_KEYRING_TYPE) ||
               arg.equals(BaseCommandTask.ARG_KEY_LABEL) || arg.equals(ARG_USE_ENCRYPTION_KEY);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        String message = "";
        // We expect at least the task name plus at least one argument
        if (args.length < 2) {
            message = getMessage("insufficientArgs");
        }

        boolean useEncryptionKey = false;
        boolean passwordFound = false;
        boolean icsfKeyringType = false;
        boolean keyLabelFound = false;
        boolean keyringFound = false;
        boolean passwordKeyFound = false;
        boolean passwordBase64KeyFound = false;
        boolean aesConfigFileFound = false;

        for (String arg : args) {
            String key = arg.split("=")[0];
            String val = arg.contains("=") ? arg.substring(arg.indexOf('=') + 1) : null;
            if (key.equals(ARG_PASSWORD)) {
                passwordFound = true;
            }
            if (key.equals(ARG_USE_ENCRYPTION_KEY) && "true".equalsIgnoreCase(val)) {
                useEncryptionKey = true;
            }
            if (key.equals(BaseCommandTask.ARG_KEYRING_TYPE) && KEYRING_TYPE_ICSF.equalsIgnoreCase(val)) {
                icsfKeyringType = true;
            }
            if (key.equals(BaseCommandTask.ARG_KEY_LABEL) && val != null && !val.isEmpty()) {
                keyLabelFound = true;
            }
            if (key.equals(BaseCommandTask.ARG_KEYRING) && val != null && !val.isEmpty()) {
                keyringFound = true;
            }
            if (key.equals(BaseCommandTask.ARG_PASSWORD_KEY)) {
                passwordKeyFound = true;
            }
            if (key.equals(BaseCommandTask.ARG_PASSWORD_BASE64_KEY)) {
                passwordBase64KeyFound = true;
            }
            if (key.equals(BaseCommandTask.ARG_AES_CONFIG_FILE)) {
                aesConfigFileFound = true;
            }
        }

        boolean icsfArgs = icsfKeyringType && keyLabelFound;
        boolean safArgs = keyringFound && !icsfKeyringType && keyLabelFound; // all three SAF args (keyring + non-ICSF type + label)
        boolean hasAesConfig = icsfArgs || safArgs || keyringFound || passwordKeyFound || passwordBase64KeyFound || aesConfigFileFound;

        if (useEncryptionKey && passwordFound) {
            message += " " + getMessage("createLTPAKeys.useEncryptionKey.passwordConflict");
        }
        if (!passwordFound && !useEncryptionKey) {
            message += " " + getMessage("missingArg", ARG_PASSWORD);
        }
        if (useEncryptionKey && !hasAesConfig) {
            message += " " + getMessage("createLTPAKeys.useEncryptionKey.missingAesConfig");
        }

        if (!message.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * @see BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)
     */
    private String getArgumentValue(String arg, String[] args, String defalt) {
        return getArgumentValue(arg, args, defalt, ARG_PASSWORD, stdin, stdout);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {
        this.stdin = stdin;
        this.stdout = stdout;

        validateArgumentList(args, Arrays.asList(new String[] { ARG_PASSWORD }));

        String path = getArgumentValue(ARG_FILE, args, DEFAULT_LTPA_KEY_FILE);
        String serverName = getArgumentValue(ARG_SERVER, args, null);

        // Resolve z/OS SAF/ICSF arguments (non-z/OS systems error if these are supplied)
        String keyring = getArgumentValue(BaseCommandTask.ARG_KEYRING, args, null);
        String keyringType = getArgumentValue(BaseCommandTask.ARG_KEYRING_TYPE, args, null);
        String keyLabel = getArgumentValue(BaseCommandTask.ARG_KEY_LABEL, args, null);

        if (!isZOS()) {
            // On non-z/OS, reject the SAF/ICSF arguments early with a clear message
            if (keyring != null || keyringType != null || keyLabel != null) {
                throw new IllegalArgumentException(getMessage("saf.arg.not.onZ"));
            }
        }

        boolean useEncryptionKey = "true".equalsIgnoreCase(getArgumentValue(ARG_USE_ENCRYPTION_KEY, args, "false"));

        // Verify the server exists before prompting for anything
        if (serverName != null) {
            String usrServers = fileUtility.getServersDirectory();
            String serverDir = usrServers + serverName + SLASH;

            if (!fileUtility.exists(serverDir)) {
                usrServers = fileUtility.resolvePath(usrServers);
                stdout.println(getMessage("createLTPAKeys.abort"));
                stdout.println(getMessage("serverNotFound", serverName, usrServers));
                return SecurityUtilityReturnCodes.ERR_SERVER_NOT_FOUND;
            }

            String location = serverDir + "resources" + SLASH + "security" + SLASH + "ltpa.keys";
            location = fileUtility.resolvePath(location);
            File fLocation = new File(location);
            if (!fileUtility.createParentDirectory(stdout, fLocation)) {
                stdout.println(getMessage("createLTPAKeys.abort"));
                stdout.println(getMessage("file.requiredDirNotCreated", location));
                return SecurityUtilityReturnCodes.ERR_PATH_CANNOT_BE_CREATED;
            }
            path = location;
        }

        if (fileUtility.exists(path)) {
            stdout.println(getMessage("createLTPAKeys.abort"));
            stdout.println(getMessage("createLTPAKeys.fileExists", path));
            return SecurityUtilityReturnCodes.ERR_FILE_EXISTS;
        }

        boolean isICSF = KEYRING_TYPE_ICSF.equalsIgnoreCase(keyringType) && keyLabel != null && !keyLabel.isEmpty();

        // --useEncryptionKey=true: encrypt the LTPA file directly with an AES key (no password).
        if (useEncryptionKey) {
            if (isICSF) {
                // Hardware ICSF key path: install resolver, get key via resolver, create file.
                return handleICSFPath(path, serverName, keyLabel);
            }
            // Software or SAF key path: derive AES key from the supplied config, create file.
            return handleEncryptionKeyPath(path, serverName, keyring, keyringType, keyLabel, args);
        }

        // --useEncryptionKey absent/false: password-based paths.
        if (isICSF) {
            // ICSF + password: file encrypted with password, snippet password encoded via ICSF key.
            return handleICSFWithPasswordPath(path, serverName, keyLabel, args);
        }

        // SAF keyring path: all three z/OS args supplied.
        // The SAF private key bytes are used as the AES password-encoding key.
        if (keyring != null && keyringType != null && keyLabel != null) {
            return handleSAFPath(path, serverName, keyring, keyringType, keyLabel, args);
        }

        // Standard password path
        return handlePasswordPath(path, serverName, args);
    }

    /**
     * Creates the LTPA keys file protected by an ICSF hardware AES key.
     * No keysPassword is required; the server.xml snippet uses
     * {@code useEncryptionKey="true"} and a companion {@code <zosPasswordEncryptionKey>}.
     * Reached when {@code --useEncryptionKey=true} and ICSF args are present.
     */
    private SecurityUtilityReturnCodes handleICSFPath(String path, String serverName,
                                                      String keyLabel) throws Exception {
        try {
            AESKeyManager.setSecretKeyResolver(new ICSFSecretKeyResolver(keyLabel));
            java.security.Key aesKey = AESKeyManager.getKeyViaResolver(AESKeyManager.KeyVersion.AES_V2);
            LTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(aesKey);
            ltpaKeyFileUtil.createLTPAKeysFile(path, encryptor);
        } finally {
            AESKeyManager.setSecretKeyResolver(null);
        }

        // The server needs both a zosPasswordEncryptionKey element (to load the ICSF key
        // at runtime) and an ltpa element with useEncryptionKey="true".
        String zosSnippet = "    <zosPasswordEncryptionKey type=\"ICSF\" label=\"" + keyLabel + "\" />";
        String ltpaSnippet = buildLtpaSnippet(serverName, path, "useEncryptionKey=\"true\"");
        stdout.println(getMessage("createLTPAKeys.createdFile", path, zosSnippet + "\n" + ltpaSnippet));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Creates the LTPA keys file protected by an AES key derived from a non-ICSF source
     * (SAF keyring, {@code --passwordKey}, {@code --passwordBase64Key}, or {@code --aesConfigFile}).
     * Reached when {@code --useEncryptionKey=true} and no ICSF args are present.
     */
    private SecurityUtilityReturnCodes handleEncryptionKeyPath(String path, String serverName,
                                                               String keyring, String keyringType, String keyLabel,
                                                               String[] args) throws Exception {
        Key aesKey;
        String hintLine;

        if (keyring != null && keyringType != null && keyLabel != null) {
            // SAF keyring: extract private key bytes, derive AES key via PBKDF2 (same as --passwordKey).
            SAFEncryptionKey ek = new SAFEncryptionKey(keyring, keyringType, keyLabel);
            aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V1, ek.getKey());
            hintLine = "    <zosPasswordEncryptionKey type=\"" + keyringType + "\" keyring=\"" + keyring + "\" label=\"" + keyLabel + "\" />";
        } else {
            String base64Key = getArgumentValue(BaseCommandTask.ARG_PASSWORD_BASE64_KEY, args, null);
            String aesConfigFile = getArgumentValue(BaseCommandTask.ARG_AES_CONFIG_FILE, args, null);

            if (base64Key != null) {
                // --passwordBase64Key: Base64-decode directly to AES_V2 key.
                aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V2, base64Key);
                hintLine = "    <!-- Set variable: wlp.aes.encryption.key=" + base64Key + " -->";
            } else if (aesConfigFile != null) {
                // --aesConfigFile: parse the file; it contains either a base64 key (PROPERTY_AES_KEY)
                // or a password key (PROPERTY_CRYPTO_KEY).
                Map<String, String> fileProps = AesConfigFileParser.parseAesEncryptionFile(aesConfigFile);
                String fileBase64Key = fileProps.get(PasswordUtil.PROPERTY_AES_KEY);
                if (fileBase64Key != null) {
                    aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V2, fileBase64Key);
                    hintLine = "    <!-- Set variable: wlp.aes.encryption.key=<your base64 key from " + aesConfigFile + "> -->";
                } else {
                    // PROPERTY_CRYPTO_KEY — password-derived key (AES_V1 PBKDF2).
                    String cryptoKey = fileProps.get(PasswordUtil.PROPERTY_CRYPTO_KEY);
                    aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V1, cryptoKey);
                    hintLine = "    <!-- Set variable: wlp.password.encryption.key=<your key from " + aesConfigFile + "> -->";
                }
            } else {
                // --passwordKey: PBKDF2 hash of the supplied string.
                String keyStr = getArgumentValue(BaseCommandTask.ARG_PASSWORD_KEY, args, null);
                aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V1, keyStr);
                hintLine = "    <!-- Set variable: wlp.password.encryption.key=" + keyStr + " -->";
            }
        }

        LTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(aesKey);
        ltpaKeyFileUtil.createLTPAKeysFile(path, encryptor);

        String ltpaSnippet = buildLtpaSnippet(serverName, path, "useEncryptionKey=\"true\"");
        stdout.println(getMessage("createLTPAKeys.createdFile", path, hintLine + "\n" + ltpaSnippet));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Creates the LTPA keys file protected by a password, encoding that password for
     * server.xml using the ICSF hardware AES key.
     * Reached when {@code --useEncryptionKey} is absent/false and ICSF args are present.
     * Modelled on {@code EncodeTask.getKeyIfSAF}'s CKDS branch: install resolver,
     * call {@code PasswordUtil.encode}, clear resolver in finally.
     */
    private SecurityUtilityReturnCodes handleICSFWithPasswordPath(String path, String serverName,
                                                                  String keyLabel, String[] args) throws Exception {
        String password = getArgumentValue(ARG_PASSWORD, args, null);
        String encoding = getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, "aes");

        String encodedPassword;
        try {
            AESKeyManager.setSecretKeyResolver(new ICSFSecretKeyResolver(keyLabel));
            encodedPassword = PasswordUtil.encode(password, encoding, new HashMap<>());
        } finally {
            AESKeyManager.setSecretKeyResolver(null);
        }

        ltpaKeyFileUtil.createLTPAKeysFile(path, password.getBytes());

        stdout.println(getMessage("createLTPAKeys.createdFile", path,
                                  buildLtpaSnippet(serverName, path, "keysPassword=\"" + encodedPassword + "\"")));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Creates the LTPA keys file protected by a key extracted from a SAF keyring.
     * The SAF private key bytes drive AES encoding of the keysPassword for server.xml.
     */
    private SecurityUtilityReturnCodes handleSAFPath(String path, String serverName,
                                                     String keyring, String keyringType, String keyLabel,
                                                     String[] args) throws Exception {
        SAFEncryptionKey ek = new SAFEncryptionKey(keyring, keyringType, keyLabel);
        String cryptoKey = ek.getKey();

        Map<String, String> argMap = new HashMap<>();
        argMap.put(BaseCommandTask.ARG_PASSWORD_KEY, cryptoKey);
        Map<String, String> props = BaseCommandTask.convertToProperties(argMap, stdout);

        String password = getArgumentValue(ARG_PASSWORD, args, null);
        String encoding = getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, "aes");
        String encodedPassword = PasswordUtil.encode(password, encoding, props);

        ltpaKeyFileUtil.createLTPAKeysFile(path, password.getBytes());

        stdout.println(getMessage("createLTPAKeys.createdFile", path,
                                  buildLtpaSnippet(serverName, path, "keysPassword=\"" + encodedPassword + "\"")));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Standard path: LTPA keys encrypted with a plaintext password.
     */
    private SecurityUtilityReturnCodes handlePasswordPath(String path, String serverName,
                                                          String[] args) throws Exception {
        Map<String, String> argMap = new HashMap<>();
        String password = getArgumentValue(ARG_PASSWORD, args, null);
        String encoding = getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, PasswordUtil.getDefaultEncoding());
        String key = getArgumentValue(BaseCommandTask.ARG_PASSWORD_KEY, args, null);
        argMap.put(BaseCommandTask.ARG_PASSWORD_KEY, key);
        String base64Key = getArgumentValue(BaseCommandTask.ARG_PASSWORD_BASE64_KEY, args, null);
        argMap.put(BaseCommandTask.ARG_PASSWORD_BASE64_KEY, base64Key);
        String aesConfigFile = getArgumentValue(BaseCommandTask.ARG_AES_CONFIG_FILE, args, null);
        argMap.put(BaseCommandTask.ARG_AES_CONFIG_FILE, aesConfigFile);
        Map<String, String> props = BaseCommandTask.convertToProperties(argMap, stdout);
        String encodedPassword = PasswordUtil.encode(password, encoding, props);

        ltpaKeyFileUtil.createLTPAKeysFile(path, password.getBytes());
        stdout.println(getMessage("createLTPAKeys.createdFile", path,
                                  buildLtpaSnippet(serverName, path, "keysPassword=\"" + encodedPassword + "\"")));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Builds a {@code <ltpa .../>} server.xml snippet.
     * When {@code serverName} is non-null the server's default key file location is
     * implied, so {@code keysFileName} is omitted.  Otherwise the explicit {@code path}
     * is included.
     *
     * @param serverName non-null when the file is inside a named server directory
     * @param path       absolute path to the LTPA key file
     * @param attributes one or more pre-formatted XML attributes, e.g. {@code keysPassword="..."}
     * @return a four-space-indented {@code <ltpa .../> } element string
     */
    private String buildLtpaSnippet(String serverName, String path, String attributes) {
        if (serverName != null) {
            return "    <ltpa " + attributes + " />";
        }
        return "    <ltpa " + attributes + " keysFileName=\"" + path + "\" />";
    }

    /**
     * Returns {@code true} when running on z/OS.
     */
    boolean isZOS() {
        String osName = System.getProperty("os.name");
        return osName != null && (osName.contains("OS/390") || osName.contains("z/OS"));
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

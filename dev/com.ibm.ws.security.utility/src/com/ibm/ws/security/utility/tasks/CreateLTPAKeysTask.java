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
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

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

    /** Pre-formatted XML attribute used in every AES-encrypted LTPA key snippet. */
    private static final String LTPA_USE_ENCRYPTION_KEY_ATTR = "useEncryptionKey=\"true\"";

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
               arg.equals(ARG_USE_ENCRYPTION_KEY);
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

        boolean hasAesConfig = passwordKeyFound || passwordBase64KeyFound || aesConfigFileFound;

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
        boolean useEncryptionKey = "true".equalsIgnoreCase(getArgumentValue(ARG_USE_ENCRYPTION_KEY, args, "false"));

        // Verify the server or client exists, if it does not then exit and do not create the certificate
        // Do this first so we don't prompt for a password we'll not use
        if (serverName != null) {
            String usrServers = fileUtility.getServersDirectory();
            String serverDir = usrServers + serverName + SLASH;

            if (!fileUtility.exists(serverDir)) {
                usrServers = fileUtility.resolvePath(usrServers);
                stdout.println(getMessage("createLTPAKeys.abort"));
                stdout.println(getMessage("serverNotFound", serverName, usrServers));
                return SecurityUtilityReturnCodes.ERR_SERVER_NOT_FOUND;
            }

            // Create the directories we need before we prompt for a password
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

        if (useEncryptionKey) {
            return handleEncryptionKeyPath(path, serverName, args);
        }

        return handlePasswordPath(path, serverName, args);
    }

    /**
     * Creates the LTPA keys file protected directly by an AES key derived from
     * --passwordKey, --passwordBase64Key, or --aesConfigFile.
     * The server.xml snippet uses useEncryptionKey="true" and a hint comment.
     */
    private SecurityUtilityReturnCodes handleEncryptionKeyPath(String path, String serverName,
                                                               String[] args) throws Exception {
        String base64Key    = getArgumentValue(BaseCommandTask.ARG_PASSWORD_BASE64_KEY, args, null);
        String aesConfigFile = getArgumentValue(BaseCommandTask.ARG_AES_CONFIG_FILE,    args, null);
        String keyStr       = getArgumentValue(BaseCommandTask.ARG_PASSWORD_KEY,        args, null);

        Key aesKey;
        String hint;

        if (base64Key != null) {
            aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V2, base64Key);
            hint = "    <!-- Ensure the variable " + AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY
                   + " is set to the value supplied via " + BaseCommandTask.ARG_PASSWORD_BASE64_KEY + " -->";
        } else if (aesConfigFile != null) {
            Map<String, String> fileProps = AesConfigFileParser.parseAesEncryptionFile(aesConfigFile);
            String fileBase64Key = fileProps.get(PasswordUtil.PROPERTY_AES_KEY);
            if (fileBase64Key != null) {
                aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V2, fileBase64Key);
                hint = "    <!-- Set variable: " + AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY
                       + "=<your base64 key from " + aesConfigFile + "> -->";
            } else {
                String cryptoKey = fileProps.get(PasswordUtil.PROPERTY_CRYPTO_KEY);
                if (cryptoKey == null) {
                    throw new IllegalArgumentException(getMessage("encode.aesConfigFileMissingEncryptionVariables",
                                                                  AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY,
                                                                  AESKeyManager.NAME_WLP_PASSWORD_ENCRYPTION_KEY));
                }
                aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V1, cryptoKey);
                hint = "    <!-- Set variable: " + AESKeyManager.NAME_WLP_PASSWORD_ENCRYPTION_KEY
                       + "=<your key from " + aesConfigFile + "> -->";
            }
        } else {
            // --passwordKey
            aesKey = AESKeyManager.getKey(AESKeyManager.KeyVersion.AES_V1, keyStr);
            hint = "    <!-- Ensure the variable " + AESKeyManager.NAME_WLP_PASSWORD_ENCRYPTION_KEY
                   + " is set to the value supplied via " + BaseCommandTask.ARG_PASSWORD_KEY + " -->";
        }

        LTPAKeyEncryptor encryptor = new AesLTPAKeyEncryptor(aesKey);
        ltpaKeyFileUtil.createLTPAKeysFile(path, encryptor);

        String ltpaSnippet = buildLtpaSnippet(serverName, path, LTPA_USE_ENCRYPTION_KEY_ATTR);
        stdout.println(getMessage("createLTPAKeys.createdFile", path, hint + "\n" + ltpaSnippet));
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
     */
    private String buildLtpaSnippet(String serverName, String path, String attributes) {
        if (serverName != null) {
            return String.format("    <ltpa %s />", attributes);
        }
        return String.format("    <ltpa %s keysFileName=\"%s\" />", attributes, path);
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

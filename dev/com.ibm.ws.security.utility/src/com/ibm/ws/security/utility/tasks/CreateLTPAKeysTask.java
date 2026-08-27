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
 */
public class CreateLTPAKeysTask extends BaseCommandTask {
    static final String SLASH = String.valueOf(File.separatorChar);

    static final String DEFAULT_LTPA_KEY_FILE = "ltpa.keys";

    static final String ARG_PASSWORD = "--password";
    static final String ARG_SERVER = "--server";
    static final String ARG_FILE = "--file";
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
               arg.equals(BaseCommandTask.ARG_KEY_LABEL);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        String message = "";
        // We expect at least the task name plus at least one argument
        if (args.length < 2) {
            message = getMessage("insufficientArgs");
        }

        // --password is required unless the ICSF path is taken, in which case
        // the LTPA key material is protected by the hardware AES key and no password
        // is needed. Detect the ICSF path by the presence of --keyringType=ICSF.
        boolean icsf = false;
        boolean passwordFound = false;
        for (String arg : args) {
            String key = arg.split("=")[0];
            String val = arg.contains("=") ? arg.substring(arg.indexOf('=') + 1) : null;
            if (key.equals(ARG_PASSWORD)) {
                passwordFound = true;
            }
            if (key.equals(BaseCommandTask.ARG_KEYRING_TYPE) && KEYRING_TYPE_ICSF.equalsIgnoreCase(val)) {
                icsf = true;
            }
        }

        if (!passwordFound && !icsf) {
            message += " " + getMessage("missingArg", ARG_PASSWORD);
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

        // ICSF path: --keyringType=ICSF + --keyLabel, no password needed.
        // The LTPA key material will be encrypted with the hardware AES key.
        if (KEYRING_TYPE_ICSF.equalsIgnoreCase(keyringType) && keyLabel != null && !keyLabel.isEmpty()) {
            return handleICSFPath(path, serverName, keyLabel);
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
        String ltpaSnippet;
        if (serverName != null) {
            ltpaSnippet = "    <ltpa useEncryptionKey=\"true\" />";
        } else {
            ltpaSnippet = "    <ltpa useEncryptionKey=\"true\" keysFileName=\"" + path + "\" />";
        }
        stdout.println(getMessage("createLTPAKeys.createdFile", path, zosSnippet + "\n" + ltpaSnippet));
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

        String xmlSnippet;
        if (serverName != null) {
            xmlSnippet = "    <ltpa keysPassword=\"" + encodedPassword + "\" />";
        } else {
            xmlSnippet = "    <ltpa keysPassword=\"" + encodedPassword + "\" keysFileName=\"" + path + "\" />";
        }
        stdout.println(getMessage("createLTPAKeys.createdFile", path, xmlSnippet));
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

        String xmlSnippet;
        if (serverName != null) {
            xmlSnippet = "    <ltpa keysPassword=\"" + encodedPassword + "\" />";
        } else {
            xmlSnippet = "    <ltpa keysPassword=\"" + encodedPassword + "\" keysFileName=\"" + path + "\" />";
        }
        ltpaKeyFileUtil.createLTPAKeysFile(path, password.getBytes());
        stdout.println(getMessage("createLTPAKeys.createdFile", path, xmlSnippet));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Returns {@code true} when running on z/OS.
     */
    private boolean isZOS() {
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

/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.ws.crypto.ltpakeyutil.AesLTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.PasswordLTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.crypto.util.AESKeyManager;
import com.ibm.ws.crypto.util.AESKeyManager.KeyVersion;
import com.ibm.ws.crypto.util.ICSFSecretKeyResolver;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 * Task: reEncryptLTPAKeys
 *
 * Reads an existing LTPA keys file and re-encrypts the same key material
 * with a new password or AES key (passphrase key, Base64 key, or CKDS label),
 * writing the result to a new file.
 * <p>
 * Passwords serve as the common intermediary. Supported combinations are:
 * <ul>
 *   <li>Password → Password: {@code --currentPassword} + {@code --newPassword}</li>
 *   <li>Password → AES Key:  {@code --currentPassword} + ({@code --key} | {@code --base64Key} | {@code --ckdsLabel})</li>
 *   <li>AES Key → Password:  ({@code --key} | {@code --base64Key} | {@code --ckdsLabel}) + {@code --newPassword}</li>
 * </ul>
 */
public class ReEncryptLTPAKeysTask extends BaseCommandTask {

    static final String ARG_CURRENT_FILE     = "--currentFile";
    static final String ARG_NEW_FILE         = "--newFile";
    static final String ARG_CURRENT_PASSWORD = "--currentPassword";
    static final String ARG_NEW_PASSWORD     = "--newPassword";
    static final String ARG_KEY              = "--key";
    static final String ARG_BASE64_KEY       = "--base64Key";
    static final String ARG_CKDS_LABEL       = "--ckdsLabel";

    private final LTPAKeyFileUtility ltpaKeyFileUtil;

    protected ConsoleWrapper stdin;
    protected PrintStream stdout;

    /**
     * @param ltpaKeyFileUtil the LTPA key file utility
     * @param scriptName      the name of the script to which this task belongs
     */
    public ReEncryptLTPAKeysTask(LTPAKeyFileUtility ltpaKeyFileUtil, String scriptName) {
        super(scriptName);
        this.ltpaKeyFileUtil = ltpaKeyFileUtil;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "reEncryptLTPAKeys";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("reEncryptLTPAKeys.desc", true);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("reEncryptLTPAKeys.desc", "reEncryptLTPAKeys.usage.options",
                           "reEncryptLTPAKeys.required-key.", "reEncryptLTPAKeys.required-desc.",
                           "reEncryptLTPAKeys.option-key", "reEncryptLTPAKeys.option-desc",
                           null, null, scriptName);
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        return arg.equals(ARG_CURRENT_FILE)     ||
               arg.equals(ARG_NEW_FILE)          ||
               arg.equals(ARG_CURRENT_PASSWORD)  ||
               arg.equals(ARG_NEW_PASSWORD)       ||
               arg.equals(ARG_KEY)               ||
               arg.equals(ARG_BASE64_KEY)        ||
               arg.equals(ARG_CKDS_LABEL);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        StringBuilder message = new StringBuilder();
        checkFileArguments(args, message);
        checkEncryptionArguments(args, message);

        String msg = message.toString().trim();
        if (!msg.isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
    }

    private void checkFileArguments(String[] args, StringBuilder message) {
        boolean currentFileFound = hasArgument(args, ARG_CURRENT_FILE);
        boolean newFileFound     = hasArgument(args, ARG_NEW_FILE);

        if (!currentFileFound) {
            message.append(" ").append(getMessage("missingArg", ARG_CURRENT_FILE));
        }
        if (!newFileFound) {
            message.append(" ").append(getMessage("missingArg", ARG_NEW_FILE));
        }
    }

    private void checkEncryptionArguments(String[] args, StringBuilder message) {
        boolean currentPasswordFound = hasArgument(args, ARG_CURRENT_PASSWORD);
        boolean newPasswordFound     = hasArgument(args, ARG_NEW_PASSWORD);
        boolean keyFound             = hasArgument(args, ARG_KEY);
        boolean base64KeyFound       = hasArgument(args, ARG_BASE64_KEY);
        boolean ckdsLabelFound       = hasArgument(args, ARG_CKDS_LABEL);

        int passwordCount = (currentPasswordFound ? 1 : 0) + (newPasswordFound ? 1 : 0);
        int aesKeyCount   = (keyFound ? 1 : 0) + (base64KeyFound ? 1 : 0) + (ckdsLabelFound ? 1 : 0);

        if (aesKeyCount > 1) {
            message.append(" ").append(getMessage("reEncryptLTPAKeys.multipleAesKeysNotSupported",
                                                   ARG_KEY, ARG_BASE64_KEY, ARG_CKDS_LABEL));
        } else if (passwordCount == 2 && aesKeyCount == 1) {
            String specifiedAesKey = getSpecifiedAesKey(keyFound, base64KeyFound);
            message.append(" ").append(getMessage("reEncryptLTPAKeys.ckdsWithBothPasswords",
                                                   specifiedAesKey, ARG_CURRENT_PASSWORD, ARG_NEW_PASSWORD));
        } else if (passwordCount == 0 && aesKeyCount >= 1) {
            message.append(" ").append(getMessage("reEncryptLTPAKeys.passwordIntermediaryRequired",
                                                   ARG_CURRENT_PASSWORD, ARG_NEW_PASSWORD));
        } else if (passwordCount + aesKeyCount < 2) {
            message.append(" ").append(getMessage("reEncryptLTPAKeys.twoKeyArgsRequired",
                                                   ARG_CURRENT_PASSWORD, ARG_NEW_PASSWORD,
                                                   ARG_KEY, ARG_BASE64_KEY, ARG_CKDS_LABEL));
        }
    }

    private static boolean hasArgument(String[] args, String targetKey) {
        for (String arg : args) {
            if (arg.split("=")[0].equals(targetKey)) {
                return true;
            }
        }
        return false;
    }

    private static String getSpecifiedAesKey(boolean keyFound, boolean base64KeyFound) {
        if (keyFound) {
            return ARG_KEY;
        }
        if (base64KeyFound) {
            return ARG_BASE64_KEY;
        }
        return ARG_CKDS_LABEL;
    }

    /**
     * Convenience wrapper that delegates to
     * {@link BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)}
     * with ARG_CURRENT_PASSWORD as the password trigger argument.
     */
    private String getArgumentValue(String arg, String[] args, String defalt) {
        return getArgumentValue(arg, args, defalt, ARG_CURRENT_PASSWORD, stdin, stdout);
    }

    /** {@inheritDoc} */
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout,
                                                  PrintStream stderr, String[] args) throws Exception {
        this.stdin  = stdin;
        this.stdout = stdout;

        validateArgumentList(args, java.util.Collections.emptyList());

        String currentFile     = getArgumentValue(ARG_CURRENT_FILE,     args, null);
        String newFile         = getArgumentValue(ARG_NEW_FILE,          args, null);
        String currentPassword = getArgumentValue(ARG_CURRENT_PASSWORD,  args, null);
        String newPassword     = getArgumentValue(ARG_NEW_PASSWORD,      args, null);
        String key             = getArgumentValue(ARG_KEY,               args, null);
        String base64Key       = getArgumentValue(ARG_BASE64_KEY,        args, null);
        String ckdsLabel       = getArgumentValue(ARG_CKDS_LABEL,        args, null);

        byte[] currentBytes = (currentPassword != null) ? currentPassword.getBytes(StandardCharsets.UTF_8) : null;
        byte[] newBytes     = (newPassword     != null) ? newPassword.getBytes(StandardCharsets.UTF_8)     : null;
        try {
            LTPAKeyEncryptor currentEncryptor;
            LTPAKeyEncryptor newEncryptor;

            if (currentBytes != null && newBytes != null) {
                // Password -> Password
                currentEncryptor = new PasswordLTPAKeyEncryptor(currentBytes);
                newEncryptor     = new PasswordLTPAKeyEncryptor(newBytes);
            } else if (currentBytes != null) {
                // Password -> AES Key
                currentEncryptor = new PasswordLTPAKeyEncryptor(currentBytes);
                newEncryptor     = buildAesEncryptor(key, base64Key, ckdsLabel);
            } else {
                // AES Key -> Password
                currentEncryptor = buildAesEncryptor(key, base64Key, ckdsLabel);
                newEncryptor     = new PasswordLTPAKeyEncryptor(newBytes);
            }

            ltpaKeyFileUtil.reEncryptLTPAKeysFile(currentFile, currentEncryptor, newFile, newEncryptor);
        } finally {
            if (currentBytes != null) Arrays.fill(currentBytes, (byte) 0);
            if (newBytes     != null) Arrays.fill(newBytes,     (byte) 0);
        }

        stdout.println(getMessage("reEncryptLTPAKeys.success", newFile));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Builds an {@link LTPAKeyEncryptor} for whichever AES key argument is provided.
     *
     * @param key        the AES_V1 passphrase key (or null)
     * @param base64Key  the AES_V2 base64-encoded key (or null)
     * @param ckdsLabel  the ICSF CKDS hardware key label (or null)
     * @return an {@link AesLTPAKeyEncryptor} backed by the resolved AES key
     * @throws Exception if key resolution or decryption fails
     */
    private LTPAKeyEncryptor buildAesEncryptor(String key, String base64Key, String ckdsLabel) throws Exception {
        if (key != null) {
            Key aesKey = AESKeyManager.getKey(KeyVersion.AES_V1, key);
            return new AesLTPAKeyEncryptor(aesKey);
        } else if (base64Key != null) {
            Key aesKey = AESKeyManager.getKey(KeyVersion.AES_V2, base64Key);
            return new AesLTPAKeyEncryptor(aesKey);
        } else if (ckdsLabel != null) {
            Key aesKey = new ICSFSecretKeyResolver(ckdsLabel).getKey();
            return new AesLTPAKeyEncryptor(aesKey);
        }
        throw new IllegalStateException("No AES key option provided");
    }

    /** {@inheritDoc} */
    @Override
    protected List<java.util.Set<String>> getExclusiveArguments() {
        return new ArrayList<>();
    }
}

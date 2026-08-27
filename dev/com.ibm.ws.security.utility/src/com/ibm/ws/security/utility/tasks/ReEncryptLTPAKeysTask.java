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
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.ws.crypto.ltpakeyutil.AesLTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.KeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.crypto.util.ICSFSecretKeyResolver;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 * Task: reEncryptLTPAKeys
 *
 * Reads an existing LTPA keys file and re-encrypts the same key material
 * with a new password or CKDS label, writing the result to a new file.
 * <p>
 * Exactly two of the three key-material arguments must be supplied:
 * <ul>
 *   <li>Password → password: {@code --currentPassword} + {@code --newPassword}</li>
 *   <li>Password → CKDS:     {@code --currentPassword} + {@code --ckdsLabel}</li>
 *   <li>CKDS → password:     {@code --ckdsLabel}       + {@code --newPassword}</li>
 * </ul>
 * Specifying all three is not supported.
 */
public class ReEncryptLTPAKeysTask extends BaseCommandTask {

    static final String ARG_CURRENT_FILE     = "--currentFile";
    static final String ARG_NEW_FILE         = "--newFile";
    static final String ARG_CURRENT_PASSWORD = "--currentPassword";
    static final String ARG_NEW_PASSWORD     = "--newPassword";
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
               arg.equals(ARG_CKDS_LABEL);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        boolean currentFileFound     = false;
        boolean newFileFound         = false;
        boolean currentPasswordFound = false;
        boolean newPasswordFound     = false;
        boolean ckdsLabelFound       = false;

        for (String arg : args) {
            String key = arg.split("=")[0];
            if (key.equals(ARG_CURRENT_FILE))     currentFileFound     = true;
            if (key.equals(ARG_NEW_FILE))          newFileFound         = true;
            if (key.equals(ARG_CURRENT_PASSWORD))  currentPasswordFound = true;
            if (key.equals(ARG_NEW_PASSWORD))      newPasswordFound     = true;
            if (key.equals(ARG_CKDS_LABEL))        ckdsLabelFound       = true;
        }

        StringBuilder message = new StringBuilder();

        if (!currentFileFound) {
            message.append(" ").append(getMessage("missingArg", ARG_CURRENT_FILE));
        }
        if (!newFileFound) {
            message.append(" ").append(getMessage("missingArg", ARG_NEW_FILE));
        }

        // Exactly 2 of the 3 key-material args must be present.
        int keyArgCount = (currentPasswordFound ? 1 : 0) + (newPasswordFound ? 1 : 0) + (ckdsLabelFound ? 1 : 0);
        if (keyArgCount == 3) {
            // All three supplied — ambiguous; ckdsLabel can only serve one side.
            message.append(" ").append(getMessage("reEncryptLTPAKeys.ckdsWithBothPasswords",
                                                   ARG_CKDS_LABEL, ARG_CURRENT_PASSWORD, ARG_NEW_PASSWORD));
        } else if (keyArgCount < 2) {
            // Fewer than 2 supplied — can't determine both source and target encryptors.
            message.append(" ").append(getMessage("reEncryptLTPAKeys.twoKeyArgsRequired",
                                                   ARG_CURRENT_PASSWORD, ARG_NEW_PASSWORD, ARG_CKDS_LABEL));
        }

        String msg = message.toString().trim();
        if (!msg.isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
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

        validateArgumentList(args, Arrays.asList(new String[0]));

        String currentFile     = getArgumentValue(ARG_CURRENT_FILE,     args, null);
        String newFile         = getArgumentValue(ARG_NEW_FILE,          args, null);
        String currentPassword = getArgumentValue(ARG_CURRENT_PASSWORD,  args, null);
        String newPassword     = getArgumentValue(ARG_NEW_PASSWORD,      args, null);
        String ckdsLabel       = getArgumentValue(ARG_CKDS_LABEL,        args, null);

        // Source encryptor: prefer currentPassword; fall back to ckdsLabel.
        LTPAKeyEncryptor currentEncryptor = (currentPassword != null)
                ? new KeyEncryptor(currentPassword.getBytes())
                : buildCkdsEncryptor(ckdsLabel);

        // Target encryptor: prefer newPassword; fall back to ckdsLabel.
        LTPAKeyEncryptor newEncryptor = (newPassword != null)
                ? new KeyEncryptor(newPassword.getBytes())
                : buildCkdsEncryptor(ckdsLabel);

        ltpaKeyFileUtil.reEncryptLTPAKeysFile(currentFile, currentEncryptor, newFile, newEncryptor);

        stdout.println(getMessage("reEncryptLTPAKeys.success", newFile));
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * Builds an {@link LTPAKeyEncryptor} backed by a CKDS hardware key.
     *
     * @param label the CKDS key label
     * @return an {@link AesLTPAKeyEncryptor} backed by the CKDS key
     * @throws Exception if the IBMJCECCA provider or key label is not available
     */
    private LTPAKeyEncryptor buildCkdsEncryptor(String label) throws Exception {
        Key key = new ICSFSecretKeyResolver(label).getKey();
        return new AesLTPAKeyEncryptor(key);
    }

    /** {@inheritDoc} */
    @Override
    protected List<java.util.Set<String>> getExclusiveArguments() {
        return new ArrayList<>();
    }
}

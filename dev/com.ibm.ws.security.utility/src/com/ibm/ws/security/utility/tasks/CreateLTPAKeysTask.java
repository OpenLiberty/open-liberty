/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

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
    static final String ARG_ENCODING = "--passwordEncoding";
    static final String ARG_KEY = "--passwordKey";

    private final LTPAKeyFileUtility ltpaKeyFileUtil;
    private final IFileUtility fileUtility;
    protected ConsoleWrapper stdin;
    protected PrintStream stdout;

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
               arg.equals(ARG_ENCODING) || arg.equals(ARG_KEY) ||
               arg.equals(ARG_FILE);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        String message = "";
        // We expect at least the password arguments and the task name
        if (args.length < 2) {
            message = getMessage("insufficientArgs");
        }

        boolean passwordFound = false;
        boolean serverFound = false;
        boolean fileFound = false;
        for (String arg : args) {
            String key = arg.split("=")[0];
            if (key.equals(ARG_SERVER)) {
                serverFound = true;
            }
            if (key.equals(ARG_FILE)) {
                fileFound = true;
            }
            if (key.equals(ARG_PASSWORD)) {
                passwordFound = true;
            }
        }
        if (serverFound && fileFound) {
            //both --server and --file can not be specified
            message += " " + getMessage("exclusiveArg", ARG_SERVER, ARG_FILE);
        }
        if (!passwordFound) {
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
        } else {
            String password = getArgumentValue(ARG_PASSWORD, args, null);
            String encoding = getArgumentValue(ARG_ENCODING, args, PasswordUtil.getDefaultEncoding());
            String key = getArgumentValue(ARG_KEY, args, null);
            String encodedPassword = PasswordUtil.encode(password, encoding, key);

            String xmlSnippet;
            // If the keys are generated for a server, omit the keysFileName as it would end up
            // being the default value
            if (serverName != null) {
                xmlSnippet = "    <ltpa keysPassword=\"" + encodedPassword + "\" />";
            } else {
                xmlSnippet = "    <ltpa keysPassword=\"" + encodedPassword + "\" keysFileName=\"" + path + "\" />";
            }

            ltpaKeyFileUtil.createLTPAKeysFile(path, password.getBytes());
            stdout.println(getMessage("createLTPAKeys.createdFile", path, xmlSnippet));
            return SecurityUtilityReturnCodes.OK;
        }
    }

}

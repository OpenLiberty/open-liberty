/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.reader.tasks;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.security.audit.reader.AuditUtilityReturnCodes;
import com.ibm.ws.security.audit.reader.utils.ConsoleWrapper;

/**
 * Main class for audit log reader utility.
 * Not bundled with the core runtime jars by design.
 */
public class AuditReaderTask extends BaseCommandTask {
    private static Logger theLogger = Logger.getLogger(AuditReaderTask.class.getName());

    private static final String ARG_AUDIT_FILE_LOCATION = "--auditFileLocation";
    private static final String ARG_OUTPUT_FILE_LOCATION = "--outputFileLocation";
    private static final String ARG_ENCRYPTED = "--encrypted";
    private static final String ARG_SIGNED = "--signed";
    private static final String ARG_ENCRYPTION_KEYSTORE_PASSWORD = "--encKeyStorePassword";
    private static final String ARG_SIGNING_KEYSTORE_PASSWORD = "--signingKeyStorePassword";
    private static final String ARG_ENCRYPTION_KEYSTORE_LOCATION = "--encKeyStoreLocation";
    private static final String ARG_SIGNING_KEYSTORE_LOCATION = "--signingKeyStoreLocation";
    private static final String ARG_ENCRYPTION_KEYSTORE_TYPE = "--encKeyStoreType";
    private static final String ARG_SIGNING_KEYSTORE_TYPE = "--signingKeyStoreType";
    private static final String DEBUG = "--debug";
    private static final List<String> ARG_TABLE = Arrays.asList(ARG_AUDIT_FILE_LOCATION,
                                                                ARG_OUTPUT_FILE_LOCATION,
                                                                ARG_ENCRYPTED,
                                                                ARG_ENCRYPTION_KEYSTORE_LOCATION,
                                                                ARG_ENCRYPTION_KEYSTORE_PASSWORD,
                                                                ARG_ENCRYPTION_KEYSTORE_TYPE,
                                                                ARG_SIGNED,
                                                                ARG_SIGNING_KEYSTORE_LOCATION,
                                                                ARG_SIGNING_KEYSTORE_PASSWORD,
                                                                ARG_SIGNING_KEYSTORE_TYPE,
                                                                DEBUG);

    private boolean isDebug = false;

    public AuditReaderTask(String scriptName) {
        super(scriptName);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "auditReader";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {

        String footer = buildScriptOptions("auditreader.option-key.", "auditreader.option-desc.") +
                        NL;

        String auditFileLocation = "";
        String outputLocation = "";
        String encPassword = "";
        String signingPassword = "";
        String encrypted = "";
        String signed = "";
        String encKeyStoreType = "";
        String signingKeyStoreType = "";
        String encKeyStoreLocation = "";
        String signingKeyStoreLocation = "";

        return getTaskHelp("auditreader.desc", "auditreader.usage.options",
                           null, null,
                           null, null,
                           null, footer,
                           scriptName, auditFileLocation, outputLocation, encrypted, encKeyStoreLocation, encPassword, encKeyStoreType,
                           signed, signingKeyStoreLocation, signingPassword, signingKeyStoreType);
    }

    @Override
    public String getTaskDescription() {
        return getOption("auditreader.desc", true);
    }

    /**
     * Decrypt and/or unsign the audit log. Capture any
     * Exceptions and print the stack trace.
     *
     * @param auditLogLocation
     * @param outputLocation
     * @param password
     * @return String
     * @throws Exception
     */
    private String auditReader(PrintStream stderr, Map<String, String> props) throws Exception {

        try {
            return AuditLogReader.getReport(props.get("auditFileLocation"), props.get("outputFileLocation"),
                                            props.get("encrypted"),
                                            props.get("encKeyStoreLocation"),
                                            props.get("encKeyStorePassword"),
                                            props.get("encKeyStoreType"),
                                            props.get("signed"),
                                            props.get("signingKeyStoreLocation"),
                                            props.get("signingKeyStorePassword"),
                                            props.get("signingKeyStoreType"),
                                            isDebug);
        } catch (Exception e) {
            throw e;
        }
    }

    /** {@inheritDoc} */

    @Override
    public AuditUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {

        Map<String, String> argMap = parseArgumentList(args);
        Map<String, String> props = convertToProperties(argMap);

        if (props.isEmpty()) {
            throw new IllegalArgumentException(getMessage("insufficientArgs"));
        }

        if (isDebug) {
            Handler[] handlers = Logger.getLogger("").getHandlers();
            for (int index = 0; index < handlers.length; index++) {
                handlers[index].setLevel(Level.FINE);
            }
            theLogger.setLevel(Level.FINE);
        }

        if (isDebug) {
            theLogger.fine("Arguments passed in: " + argMap.toString());
        }

        // validate that the --encrypted argument value, if specified, is either true or false
        if (argMap.containsKey(ARG_ENCRYPTED) && (!argMap.get(ARG_ENCRYPTED).equalsIgnoreCase("true") && (!argMap.get(ARG_ENCRYPTED).equalsIgnoreCase("false")))) {
            if (isDebug)
                theLogger.fine("Invalid value, " + argMap.get(ARG_ENCRYPTED) + ", specified for the --encrypted argument.   Value must be either true or false.");
            throw new IllegalArgumentException(getMessage("invalidValue", argMap.get(ARG_ENCRYPTED), ARG_ENCRYPTED));
        }

        // validate that the --signed argument value, if specified,  is either true or false
        if (argMap.containsKey(ARG_SIGNED) && (!argMap.get(ARG_SIGNED).equalsIgnoreCase("true") && (!argMap.get(ARG_SIGNED).equalsIgnoreCase("false")))) {
            if (isDebug)
                theLogger.fine("Invalid value, " + argMap.get(ARG_SIGNED) + ", specified for the --signed argument.   Value must be either true or false.");
            throw new IllegalArgumentException(getMessage("invalidValue", argMap.get(ARG_SIGNED), ARG_SIGNED));
        }

        // if --encrypted=true, ensure the keystore type specified for the encryption keystore is not CMS or PKCS11
        if (argMap.containsKey(ARG_ENCRYPTED) && argMap.get(ARG_ENCRYPTED).equalsIgnoreCase("true") && (argMap.containsKey(ARG_ENCRYPTION_KEYSTORE_TYPE))) {
            if (argMap.get(ARG_ENCRYPTION_KEYSTORE_TYPE).equalsIgnoreCase("PKCS11") || argMap.get(ARG_ENCRYPTION_KEYSTORE_TYPE).equals("CMS")) {
                if (isDebug)
                    theLogger.fine("The keystore type, " + argMap.get(ARG_ENCRYPTION_KEYSTORE_TYPE)
                                   + ", specified for --encKeyStoreType is not supported. Only JKS, JCEKS and PKCS12 are supported.");
                throw new IllegalArgumentException(getMessage("security.audit.UnsupportedKeyStoreType", argMap.get(ARG_ENCRYPTION_KEYSTORE_TYPE), ARG_ENCRYPTION_KEYSTORE_TYPE));
            }
        }

        // if --signed=true, ensure the keystore type specified for the signing keystore is not CMS or PKCS11
        if (argMap.containsKey(ARG_SIGNED) && argMap.get(ARG_SIGNED).equalsIgnoreCase("true") && (argMap.containsKey(ARG_SIGNING_KEYSTORE_TYPE))) {
            if (argMap.get(ARG_SIGNING_KEYSTORE_TYPE).equalsIgnoreCase("PKCS11") || argMap.get(ARG_SIGNING_KEYSTORE_TYPE).equals("CMS")) {
                if (isDebug)
                    theLogger.fine("The keystore type, " + argMap.get(ARG_SIGNING_KEYSTORE_TYPE)
                                   + ", specified for --signingKeyStoreType is not supported. Only JKS, JCEKS and PKCS12 are supported.");
                throw new IllegalArgumentException(getMessage("security.audit.UnsupportedKeyStoreType", argMap.get(ARG_SIGNING_KEYSTORE_TYPE), ARG_SIGNING_KEYSTORE_TYPE));
            }
        }

        // ensure the --auditFileLocation is specified (required argument)
        if (!argMap.containsKey(ARG_AUDIT_FILE_LOCATION)) {
            if (isDebug)
                theLogger.fine("Missing --auditFileLocation argument");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_AUDIT_FILE_LOCATION));
        }

        // ensure the audit log specified in the --auditFileLocation argument exists
        if (argMap.containsKey(ARG_AUDIT_FILE_LOCATION)) {
            String afl = argMap.get(ARG_AUDIT_FILE_LOCATION);
            File f = new File(afl);
            if (!f.exists()) {
                if (isDebug)
                    theLogger.fine("Specified location for the audit log does not exist");
                throw new IllegalArgumentException(getMessage("security.audit.FileNotFound", afl));
            }
        }

        // ensure the --outputFileLocation is specified (required argument)
        if (!argMap.containsKey(ARG_OUTPUT_FILE_LOCATION)) {
            if (isDebug)
                theLogger.fine("Missing --outputFileLocation argument");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_OUTPUT_FILE_LOCATION));
        }

        // check that the location of the audit log is not the same as the location of the output log
        if (argMap.get(ARG_AUDIT_FILE_LOCATION).equals(argMap.get(ARG_OUTPUT_FILE_LOCATION))) {
            if (isDebug)
                theLogger.fine("The input audit file location, " + argMap.get(ARG_AUDIT_FILE_LOCATION) +
                               ", cannot be the same as the output audit file location, " + argMap.get(ARG_OUTPUT_FILE_LOCATION) + ".");
            throw new IllegalArgumentException(getMessage("invalidFileLocations", argMap.get(ARG_AUDIT_FILE_LOCATION), argMap.get(ARG_OUTPUT_FILE_LOCATION)));
        }

        // check that the output file location specified is not a directory
        if (argMap.containsKey(ARG_OUTPUT_FILE_LOCATION)) {
            String afl = argMap.get(ARG_OUTPUT_FILE_LOCATION);
            File f = new File(afl);
            if (f.isDirectory()) {
                if (isDebug)
                    theLogger.fine("Specified location for the output audit log does not exist.  It is specified but is a directory.");
                throw new IllegalArgumentException(getMessage("security.audit.FileNotFound", afl));
            } else {
                if (isDebug)
                    theLogger.fine("Specified location for the output audit log exists and is not a directory.");

            }

            if (afl.endsWith("/") || afl.endsWith("\\")) {
                if (isDebug)
                    theLogger.fine("Specified location for the output audit log does not exist.  It is specified but is a non existant directory.");
                throw new IllegalArgumentException(getMessage("security.audit.FileNotFound", afl));
            }

            if (f.createNewFile()) {
                if (!f.canWrite()) {
                    if (isDebug)
                        theLogger.fine("Specified location for the output audit log is a non-writable file.  Ensure that the location for the output file is writable");
                    throw new IllegalArgumentException(getMessage("audit.NonWriteableOuputFile", afl));
                }
            }
            boolean fd = false;
            if (f.exists() && f.canWrite())
                fd = f.delete();
        }

        // if --encrypted=true, ensure that the encryption keystore password is specified.
        if (argMap.containsKey(ARG_ENCRYPTED) && argMap.get(ARG_ENCRYPTED).equalsIgnoreCase("true") &&
            !argMap.containsKey(ARG_ENCRYPTION_KEYSTORE_PASSWORD)) {
            if (isDebug)
                theLogger.fine("The --encrypted argument is true, but nothing was specified for --encKeyStorePassword.");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_ENCRYPTION_KEYSTORE_PASSWORD));
        }

        // if --encrypted=true, ensure that the encryption keystore type is specified.
        if (argMap.containsKey(ARG_ENCRYPTED) && argMap.get(ARG_ENCRYPTED).equalsIgnoreCase("true") &&
            !argMap.containsKey(ARG_ENCRYPTION_KEYSTORE_TYPE)) {
            if (isDebug)
                theLogger.fine("The --encrypted argument is true, but nothing was specified for --encKeyStoreType.");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_ENCRYPTION_KEYSTORE_TYPE));
        }

        // if --encrypted=true, and an encryption keystore location is specified, ensure it 1) has a URL prefix, 2) exists,  and 3) is not CMS or PKCS11
        if (argMap.containsKey(ARG_ENCRYPTED) && argMap.get(ARG_ENCRYPTED).equalsIgnoreCase("true") &&
            argMap.containsKey(ARG_ENCRYPTION_KEYSTORE_LOCATION)) {
            String loc = argMap.get(ARG_ENCRYPTION_KEYSTORE_LOCATION);
            File f = new File(loc);
            if (!f.exists()) {
                if (isDebug)
                    theLogger.fine("Specified location for the encryption keystore does not exist");
                throw new IllegalArgumentException(getMessage("security.audit.FileNotFound", loc));

            }
            if (loc.endsWith(".CMS") || loc.endsWith(".cms") || loc.endsWith(".pkce11") || loc.endsWith(".PKCE11")) {
                if (isDebug)
                    theLogger.fine("The keystore type, " + argMap.get(ARG_ENCRYPTION_KEYSTORE_TYPE)
                                   + ", specified for --encKeyStoreType is not supported. Only JKS, JCEKS and PKCS12 are supported.");
                throw new IllegalArgumentException(getMessage("security.audit.UnsupportedKeyStoreType", argMap.get(ARG_ENCRYPTION_KEYSTORE_TYPE), ARG_ENCRYPTION_KEYSTORE_TYPE));
            }
        }

        // if --signed=true, ensure that the signing keystore password is specified.
        if (argMap.containsKey(ARG_SIGNED) && argMap.get(ARG_SIGNED).equalsIgnoreCase("true") &&
            !argMap.containsKey(ARG_SIGNING_KEYSTORE_PASSWORD)) {
            if (isDebug)
                theLogger.fine("The --signed argument is true, but nothing was specified for --signingKeyStorePassword.");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_SIGNING_KEYSTORE_PASSWORD));
        }

        // if --signed=true, ensure that the signing keystore type is specified.
        if (argMap.containsKey(ARG_SIGNED) && argMap.get(ARG_SIGNED).equalsIgnoreCase("true") &&
            !argMap.containsKey(ARG_SIGNING_KEYSTORE_TYPE)) {
            if (isDebug)
                theLogger.fine("The --signed argument is true, but nothing was specified for --signingKeyStoreType.");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_SIGNING_KEYSTORE_TYPE));
        }

        // if --signed=true, and a signed keystore location is specified, ensure it 1) has a URL prefix, 2) exists,  and 3) is not CMS or PKCS11
        if (argMap.containsKey(ARG_SIGNED) && argMap.get(ARG_SIGNED).equalsIgnoreCase("true") &&
            argMap.containsKey(ARG_SIGNING_KEYSTORE_LOCATION)) {
            String loc = argMap.get(ARG_SIGNING_KEYSTORE_LOCATION);
            File f = new File(loc);
            if (!f.exists()) {
                if (isDebug)
                    theLogger.fine("Specified location for the signing keystore does not exist");
                throw new IllegalArgumentException(getMessage("security.audit.FileNotFound", loc));

            }
            if (loc.endsWith(".CMS") || loc.endsWith(".cms") || loc.endsWith(".pkce11") || loc.endsWith(".PKCE11")) {
                if (isDebug)
                    theLogger.fine("The keystore type, " + argMap.get(ARG_SIGNING_KEYSTORE_TYPE)
                                   + ", specified for --signingKeyStoreType is not supported. Only JKS, JCEKS and PKCS12 are supported.");
                throw new IllegalArgumentException(getMessage("security.audit.UnsupportedKeyStoreType", argMap.get(ARG_SIGNING_KEYSTORE_TYPE), ARG_SIGNING_KEYSTORE_TYPE));
            }
        }

        if ((argMap.containsKey(ARG_ENCRYPTION_KEYSTORE_PASSWORD) || argMap.containsKey(ARG_ENCRYPTION_KEYSTORE_TYPE) ||
             argMap.containsKey(ARG_ENCRYPTION_KEYSTORE_LOCATION))
            && (!argMap.containsKey(ARG_ENCRYPTED))) {
            if (isDebug)
                theLogger.fine("The --encKeyStoreLocation, --encKeyStoreType, and --encKeyStorePassword were specified, but nothing was specified for the --encrypted argument.");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_ENCRYPTED));
        }

        if ((argMap.containsKey(ARG_SIGNING_KEYSTORE_PASSWORD) || argMap.containsKey(ARG_SIGNING_KEYSTORE_TYPE) ||
             argMap.containsKey(ARG_SIGNING_KEYSTORE_LOCATION))
            && (!argMap.containsKey(ARG_SIGNED))) {
            if (isDebug)
                theLogger.fine("The --signingKeyStoreLocation, --signingKeyStoreType, and --signingKeyStorePassword were specified, but nothing was specified for the --signed argument.");
            throw new IllegalArgumentException(getMessage("missingArg", ARG_SIGNED));
        }

        try {
            auditReader(stderr, props);
        } catch (Exception e) {
            throw e;
        }

        return AuditUtilityReturnCodes.OK;
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

                int index = arg.indexOf('=');
                if (index == -1) {
                    // Any options specified must have values
                    if (!isKnownArgument(arg)) {
                        throw new IllegalArgumentException(getMessage("invalidArg", arg));
                    } else {
                        throw new IllegalArgumentException(getMessage("missingValue", arg));
                    }
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

            } else {
                // A non-option argument to be encoded has already been recorded
                throw new IllegalArgumentException(getMessage("invalidArg", arg));
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
     * Convert the properties for encoding from the command line parameters.
     */
    protected Map<String, String> convertToProperties(Map<String, String> argMap) {
        HashMap<String, String> props = new HashMap<String, String>();

        String value = argMap.get(ARG_AUDIT_FILE_LOCATION);
        if (value != null) {
            props.put("auditFileLocation", value);
        }

        value = argMap.get(ARG_OUTPUT_FILE_LOCATION);
        if (value != null) {
            props.put("outputFileLocation", value);
        }

        value = argMap.get(ARG_ENCRYPTED);
        if (value != null) {
            props.put("encrypted", value);
        }

        value = argMap.get(ARG_SIGNED);
        if (value != null) {
            props.put("signed", value);
        }

        value = argMap.get(ARG_ENCRYPTION_KEYSTORE_LOCATION);
        if (value != null) {
            props.put("encKeyStoreLocation", value);
        }

        value = argMap.get(ARG_ENCRYPTION_KEYSTORE_PASSWORD);
        if (value != null) {
            props.put("encKeyStorePassword", value);
        }

        value = argMap.get(ARG_SIGNING_KEYSTORE_LOCATION);
        if (value != null) {
            props.put("signingKeyStoreLocation", value);
        }

        value = argMap.get(ARG_SIGNING_KEYSTORE_PASSWORD);
        if (value != null) {
            props.put("signingKeyStorePassword", value);
        }

        value = argMap.get(ARG_ENCRYPTION_KEYSTORE_TYPE);
        if (value != null) {
            props.put("encKeyStoreType", value);
        }

        value = argMap.get(ARG_SIGNING_KEYSTORE_TYPE);
        if (value != null) {
            props.put("signingKeyStoreType", value);
        }

        value = argMap.get(DEBUG);
        if (value != null) {
            props.put("debug", value);
            if (value.equals("true"))
                isDebug = true;
            else
                isDebug = false;
        } else {
            props.put("debug", "false");
            isDebug = false;
        }

        return props;
    }

}

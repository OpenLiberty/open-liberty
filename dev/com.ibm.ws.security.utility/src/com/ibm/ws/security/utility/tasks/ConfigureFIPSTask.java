/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.StringJoiner;

import org.apache.commons.io.FilenameUtils;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 *
 */
public class ConfigureFIPSTask extends BaseCommandTask {

    static final String SLASH = String.valueOf(File.separatorChar);
    static final String PATH_SEPARATOR = File.pathSeparator;

    static final String ARG_SERVER = "--server";
    static final String ARG_CLIENT = "--client";
    static final String ARG_DISABLE = "--disable";
    static final String ARG_CUSTOMPROFILE_FILE = "--customProfileFile";

    static final String DEFAULT_ENV = "default.env";
    static final String SERVER_ENV = "server.env";
    static final String CLIENT_ENV = "client.env";
    static final String ENABLE_FIPS140_3_ENV_VAR = "ENABLE_FIPS140_3";

    static final String LIBERTY_PROFILE_FILE_NAME = "FIPS140-3-Liberty.properties";
    static final String APP_PROFILE_FILE_NAME = "FIPS140-3-Liberty-Application.properties";

    static final String PROFILE_NAME_HOLDER = "PROFILE_NAME_HOLDER";
    static final String BASE_PROFILE_NAME_HOLDER = "BASE_NAME_HOLDER";
    final String APP_PROFILE = String.join(NL,
                                           "# Visit the link for more information about configuring this file:",
                                           "# https://ibm.biz/BdeU7c",
                                           "#",
                                           "# To find the FIPS140-3-Liberty profile configuration, see wlp/lib/security/FIPS140-3-Liberty.properties",
                                           "#",
                                           "# Example configuration for allowing SHA-1 for the io.openliberty.myClass class in the existing OpenJCEPlusFIPS provider",
                                           "# and adding a new io.openliberty.myProvider provider:",
                                           "#",
                                           "# RestrictedSecurity.OpenJCEPlusFIPS.FIPS140-3-Liberty-Application.desc.name = OpenJCEPlusFIPS Cryptographic Module FIPS 140-3 for Liberty Application",
                                           "# RestrictedSecurity.OpenJCEPlusFIPS.FIPS140-3-Liberty-Application.extends = RestrictedSecurity.OpenJCEPlusFIPS.FIPS140-3-Liberty",
                                           "# RestrictedSecurity.OpenJCEPlusFIPS.FIPS140-3-Liberty-Application.jce.provider.1 = com.ibm.crypto.plus.provider.OpenJCEPlusFIPS [+ \\",
                                           "#     {MessageDigest, SHA-1, *, FullClassName:io.openliberty.myClass}]",
                                           "# RestrictedSecurity.OpenJCEPlusFIPS.FIPS140-3-Liberty-Application.jce.provider.16 = io.openliberty.myProvider",
                                           "#",
                                           "RestrictedSecurity.OpenJCEPlusFIPS." + PROFILE_NAME_HOLDER
                                                + ".desc.name = OpenJCEPlusFIPS Cryptographic Module FIPS 140-3 for Liberty Application",
                                           "RestrictedSecurity.OpenJCEPlusFIPS." + PROFILE_NAME_HOLDER + ".extends = RestrictedSecurity.OpenJCEPlusFIPS."
                                                                                                                                          + BASE_PROFILE_NAME_HOLDER,
                                           "");

    protected ConsoleWrapper stdin;
    protected PrintStream stdout;
    protected PrintStream stderr;

    private final IFileUtility fileUtility;

    private final String osName = System.getProperty("os.name");
    private final boolean isZOS = (osName.equalsIgnoreCase("z/OS") || osName.equalsIgnoreCase("OS/390"));
    private final Charset CHARSET = isZOS ? Charset.forName("IBM1047") : StandardCharsets.UTF_8;

    private boolean isIbmSdkChecked = false;
    private boolean isIbmSdk = false;
    private boolean isSemeruChecked = false;
    private boolean isSemeru = false;

    public ConfigureFIPSTask(IFileUtility fileUtility, String scriptName) {
        super(scriptName);
        this.fileUtility = fileUtility;
    }

    @Override
    public String getTaskName() {
        return "configureFIPS";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("configureFIPS.desc", "configureFIPS.usage.options",
                           "configureFIPS.required-key.", "configureFIPS.required-desc.",
                           "configureFIPS.option-key", "configureFIPS.option-desc",
                           null, null, scriptName);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("configureFIPS.desc", true);
    }

    /** {@inheritDoc} */
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;

        if (ProductInfo.getBetaEdition()) {
            stdout.println("BETA: The SecurityUtility configureFIPS task is only available in beta." + NL);
        }

        String serverName = getArgumentValue(ARG_SERVER, args, null);
        String clientName = getArgumentValue(ARG_CLIENT, args, null);
        String customProfileFile = getArgumentValue(ARG_CUSTOMPROFILE_FILE, args, null);
        boolean disable = Arrays.asList(args).contains(ARG_DISABLE);

        if (isZOS) {
            stdout.println(getMessage("configureFIPS.zosNotAvailable"));
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }

        try {
            if (!disable && (!isIbmSdk() && !isSemeru())) {
                stdout.println(getMessage("configureFIPS.notIbmSdkNorSemeru"));
                return SecurityUtilityReturnCodes.ERR_GENERIC;
            }
        } catch (IOException e) {
            stdout.println(getMessage("configureFIPS.abort"));
            e.printStackTrace(stdout);
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }

        // no server, nor client specified; enable for all via wlp/etc/default.env
        if (serverName == null && clientName == null) {
            String etcDir = fileUtility.getInstallDirectory() + "etc" + SLASH;
            String envFileLocation = fileUtility.resolvePath(etcDir + DEFAULT_ENV);

            if (disable) {
                SecurityUtilityReturnCodes rc = handleFileToDisableFips(envFileLocation, null, null);
                if (rc != SecurityUtilityReturnCodes.OK) {
                    return rc;
                }
            } else {
                String customProfileFilePaths = "";
                if (isSemeru()) {
                    customProfileFilePaths = customProfileFile;
                    if (customProfileFilePaths == null) {
                        customProfileFilePaths = etcDir + APP_PROFILE_FILE_NAME;
                    }
                }

                SecurityUtilityReturnCodes rc = handleFilesToEnableFips(envFileLocation, customProfileFilePaths, null, null);
                if (rc != SecurityUtilityReturnCodes.OK) {
                    return rc;
                }
            }
        }

        if (serverName != null) {
            String usrServers = fileUtility.getServersDirectory();
            String serverDir = usrServers + serverName + SLASH;

            if (!fileUtility.exists(serverDir)) {
                usrServers = fileUtility.resolvePath(usrServers);
                stdout.println(getMessage("configureFIPS.abort"));
                stdout.println(getMessage("serverNotFound", serverName, usrServers));
                return SecurityUtilityReturnCodes.ERR_SERVER_NOT_FOUND;
            }

            String envFileLocation = fileUtility.resolvePath(serverDir + SLASH + SERVER_ENV);

            if (disable) {
                SecurityUtilityReturnCodes rc = handleFileToDisableFips(envFileLocation, serverName, null);
                if (rc != SecurityUtilityReturnCodes.OK) {
                    return rc;
                }
            } else {
                String customProfileFilePaths = "";
                if (isSemeru()) {
                    customProfileFilePaths = customProfileFile;
                    if (customProfileFilePaths == null) {
                        customProfileFilePaths = serverDir + "resources" + SLASH + "security" + SLASH + APP_PROFILE_FILE_NAME;
                    }
                }

                SecurityUtilityReturnCodes rc = handleFilesToEnableFips(envFileLocation, customProfileFilePaths, serverName, null);
                if (rc != SecurityUtilityReturnCodes.OK) {
                    return rc;
                }
            }
        }

        if (clientName != null) {
            if (serverName != null) {
                stdout.println(""); // add a new line if we previously setup for server
            }

            String usrClients = fileUtility.getClientsDirectory();
            String clientDir = usrClients + clientName + SLASH;

            if (!fileUtility.exists(clientDir)) {
                usrClients = fileUtility.resolvePath(usrClients);
                stdout.println(getMessage("configureFIPS.abort"));
                stdout.println(getMessage("clientNotFound", clientName, usrClients));
                return SecurityUtilityReturnCodes.ERR_CLIENT_NOT_FOUND;
            }

            String envFileLocation = fileUtility.resolvePath(clientDir + SLASH + CLIENT_ENV);

            if (disable) {
                SecurityUtilityReturnCodes rc = handleFileToDisableFips(envFileLocation, null, clientName);
                if (rc != SecurityUtilityReturnCodes.OK) {
                    return rc;
                }
            } else {
                String customProfileFilePaths = "";
                if (isSemeru()) {
                    customProfileFilePaths = customProfileFile;
                    if (customProfileFilePaths == null) {
                        customProfileFilePaths = clientDir + "resources" + SLASH + "security" + SLASH + APP_PROFILE_FILE_NAME;
                    }
                }

                SecurityUtilityReturnCodes rc = handleFilesToEnableFips(envFileLocation, customProfileFilePaths, null, clientName);
                if (rc != SecurityUtilityReturnCodes.OK) {
                    return rc;
                }
            }
        }

        return SecurityUtilityReturnCodes.OK;
    }

    private boolean isSemeru() throws IOException {
        if (isSemeruChecked) {
            return isSemeru;
        }

        isIbmSdk = isIbmSdk();
        if (isIbmSdk) {
            isSemeru = false;
            isSemeruChecked = true;

            return isSemeru;
        }

        isSemeru = false;

        String javaHome = getJavaHome();
        String javaSecurity = javaHome + (javaHome.endsWith(SLASH) ? "" : SLASH) + "conf" + SLASH + "security" + SLASH + "java.security";
        if (fileUtility.exists(javaSecurity)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(javaSecurity), CHARSET))) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("RestrictedSecurity.OpenJCEPlusFIPS.FIPS140-3-Strongly-Enforced.")) {
                        isSemeru = true;
                        break;
                    }
                }
            }
        }

        isSemeruChecked = true;
        return isSemeru;
    }

    private boolean isIbmSdk() {
        if (isIbmSdkChecked) {
            return isIbmSdk;
        }

        isIbmSdk = false;

        String javaHome = getJavaHome();
        if (javaHome.endsWith("jre" + SLASH)) {
            isIbmSdk = fileUtility.exists(javaHome + "fips140-3" + SLASH);
        } else if (javaHome.endsWith("jre")) {
            isIbmSdk = fileUtility.exists(javaHome + SLASH + "fips140-3" + SLASH);
        } else if (javaHome.endsWith(SLASH)) {
            isIbmSdk = fileUtility.exists(javaHome + "jre" + SLASH + "fips140-3" + SLASH);
        } else {
            isIbmSdk = fileUtility.exists(javaHome + SLASH + "jre" + SLASH + "fips140-3" + SLASH);
        }

        isIbmSdkChecked = true;
        return isIbmSdk;
    }

    private String getJavaHome() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            javaHome = System.getenv("JRE_HOME");
        }
        if (javaHome == null) {
            javaHome = System.getenv("WLP_DEFAULT_JAVA_HOME");
        }
        if (javaHome == null) {
            javaHome = System.getProperty("java.home");
        }
        return javaHome;
    }

    private SecurityUtilityReturnCodes handleFilesToEnableFips(String envFileLocation, String customProfileFilePaths, String serverName, String clientName) {
        if (customProfileFilePaths.isEmpty()) {
            stdout.println(getMessage("configureFIPS.configureIbmSdk"));

            customProfileFilePaths = "true";
        } else {
            stdout.println(getMessage("configureFIPS.configureSemeru"));

            String[] customProfileFileLocations = customProfileFilePaths.split(PATH_SEPARATOR);
            File previousCustomProfileFile = null;
            for (int i = 0; i < customProfileFileLocations.length; i++) {
                customProfileFileLocations[i] = fileUtility.resolvePath(customProfileFileLocations[i]);
                String customProfileFileLocation = customProfileFileLocations[i];

                File customProfileFile = new File(customProfileFileLocation);

                String profileName = FilenameUtils.removeExtension(customProfileFile.getName());
                if (profileName.equals("FIPS140-3-Liberty")) {
                    stdout.println(getMessage("configureFIPS.fileNameNotAllowed", customProfileFileLocation));
                    return SecurityUtilityReturnCodes.ERR_GENERIC;
                }

                if (!fileUtility.createParentDirectory(stdout, customProfileFile)) {
                    stdout.println(getMessage("configureFIPS.abortSemeruFile"));
                    stdout.println(getMessage("file.requiredDirNotCreated", customProfileFileLocation));
                    return SecurityUtilityReturnCodes.ERR_PATH_CANNOT_BE_CREATED;
                }

                if (fileUtility.exists(customProfileFile)) {
                    stdout.println(getMessage("configureFIPS.fileExists", customProfileFileLocation));
                    previousCustomProfileFile = customProfileFile;
                    continue;
                }

                String baseProfileName = FilenameUtils.removeExtension(previousCustomProfileFile == null ? LIBERTY_PROFILE_FILE_NAME : previousCustomProfileFile.getName());
                String customProfile = APP_PROFILE.replaceAll(PROFILE_NAME_HOLDER, profileName).replaceAll(BASE_PROFILE_NAME_HOLDER, baseProfileName);
                fileUtility.writeToFile(stderr, customProfile, customProfileFile);
                stdout.println(getMessage("configureFIPS.createdSemeruFile", customProfileFileLocation));

                previousCustomProfileFile = customProfileFile;
            }
            customProfileFilePaths = "\"" + String.join(PATH_SEPARATOR, customProfileFileLocations) + "\"";
        }

        File envFile = new File(envFileLocation);

        if (!fileUtility.createParentDirectory(stdout, envFile)) {
            stdout.println(getMessage("configureFIPS.abortEnvFile"));
            stdout.println(getMessage("file.requiredDirNotCreated", envFileLocation));
            return SecurityUtilityReturnCodes.ERR_PATH_CANNOT_BE_CREATED;
        }

        return setFipsEnvironmentVariable(envFile, customProfileFilePaths, serverName, clientName);
    }

    private SecurityUtilityReturnCodes handleFileToDisableFips(String envFileLocation, String serverName, String clientName) {
        File envFile = new File(envFileLocation);
        return disableFipsEnvironmentVariable(envFile, serverName, clientName);
    }

    /**
     * Set the ENABLE_FIPS140_3 environment variable in the specified .env file.
     * For the case of IBM SDK, it should be empty string.
     * For the case of IBM Semeru Runtimes, it should be the path to the user's custom profile file.
     *
     * @param file
     * @param value
     */
    private SecurityUtilityReturnCodes setFipsEnvironmentVariable(File file, String value, String serverName, String clientName) {
        if (!fileUtility.exists(file)) {
            fileUtility.writeToFile(stderr, ENABLE_FIPS140_3_ENV_VAR + "=" + value + NL, file);
            stdout.println(getMessage("configureFIPS.createdEnvFileToEnableFips", fileUtility.resolvePath(file)));
            printRestartServerMessage(serverName, clientName);
            return SecurityUtilityReturnCodes.OK;
        }

        boolean enabled = false;
        StringJoiner joiner = new StringJoiner(NL);
        boolean fileEndsWithNewLineChar = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileUtility.resolvePath(file)), CHARSET))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(ENABLE_FIPS140_3_ENV_VAR + "=")) {
                    if (line.equals(ENABLE_FIPS140_3_ENV_VAR + "=false")) {
                        joiner.add(ENABLE_FIPS140_3_ENV_VAR + "=" + value);
                        enabled = true;
                    } else {
                        stdout.println(getMessage("configureFIPS.abortEnvFile"));
                        stdout.println(getMessage("configureFIPS.fipsAlreadyEnabled", fileUtility.resolvePath(file)));
                        return SecurityUtilityReturnCodes.ERR_GENERIC;
                    }
                } else {
                    joiner.add(line);
                }
            }

            fileEndsWithNewLineChar = fileEndsOnNewLine(file);
        } catch (IOException e) {
            stdout.println(getMessage("configureFIPS.abortEnvFile"));
            e.printStackTrace(stdout);
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }

        if (!enabled) {
            joiner.add(ENABLE_FIPS140_3_ENV_VAR + "=" + value);
        }

        try {
            backupFile(file);
            fileUtility.writeToFile(stderr, joiner.toString() + (fileEndsWithNewLineChar ? NL : ""), file, CHARSET);
            stdout.println(getMessage("configureFIPS.updatedEnvFileToEnableFips", fileUtility.resolvePath(file)));
            printRestartServerMessage(serverName, clientName);
            return SecurityUtilityReturnCodes.OK;
        } catch (IOException e) {
            stdout.println(getMessage("configureFIPS.abortEnvFile"));
            e.printStackTrace(stdout);
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }
    }

    /**
     * Comments out the ENABLE_FIPS140_3 environment variable in the specified .env file.
     *
     * @param file
     */
    private SecurityUtilityReturnCodes disableFipsEnvironmentVariable(File file, String serverName, String clientName) {
        if (!fileUtility.exists(file)) {
            stdout.println(getMessage("configureFIPS.abortEnvFile"));
            stdout.println(getMessage("configureFIPS.fileDoesNotExist", fileUtility.resolvePath(file)));
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }

        boolean disabled = false;
        StringJoiner joiner = new StringJoiner(NL);
        boolean fileEndsWithNewLineChar = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileUtility.resolvePath(file)), CHARSET))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(ENABLE_FIPS140_3_ENV_VAR + "=") && !line.equals(ENABLE_FIPS140_3_ENV_VAR + "=false")) {
                    joiner.add(ENABLE_FIPS140_3_ENV_VAR + "=false");
                    disabled = true;
                } else {
                    joiner.add(line);
                }
            }

            fileEndsWithNewLineChar = fileEndsOnNewLine(file);
        } catch (IOException e) {
            stdout.println(getMessage("configureFIPS.abortEnvFile"));
            e.printStackTrace(stdout);
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }

        if (!disabled) {
            stdout.println(getMessage("configureFIPS.abortEnvFile"));
            stdout.println(getMessage("configureFIPS.fipsNotEnabled", fileUtility.resolvePath(file)));
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }

        try {
            backupFile(file);
            fileUtility.writeToFile(stderr, joiner.toString() + (fileEndsWithNewLineChar ? NL : ""), file, CHARSET);
            stdout.println(getMessage("configureFIPS.updatedEnvFileToDisableFips", fileUtility.resolvePath(file)));
            printRestartServerMessage(serverName, clientName);
            return SecurityUtilityReturnCodes.OK;
        } catch (IOException e) {
            stdout.println(getMessage("configureFIPS.abortEnvFile"));
            e.printStackTrace(stdout);
            return SecurityUtilityReturnCodes.ERR_GENERIC;
        }
    }

    private boolean fileEndsOnNewLine(File file) throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(fileUtility.resolvePath(file), "r")) {
            long lastByteIdx = randomAccessFile.length() - 1;
            if (lastByteIdx == -1) { // file is empty
                return true;
            }
            randomAccessFile.seek(lastByteIdx);
            if (isZOS) {
                return randomAccessFile.read() == 0x15;
            }
            return randomAccessFile.read() == '\n';
        }
    }

    private void printRestartServerMessage(String serverName, String clientName) {
        if (serverName != null) {
            stdout.println(getMessage("configureFIPS.restartServer", serverName));
        } else if (clientName != null) {
            stdout.println(getMessage("configureFIPS.restartClient", clientName));
        } else {
            stdout.println(getMessage("configureFIPS.restart"));
        }
    }

    private void backupFile(File file) throws IOException {
        if (file.length() == 0) {
            // don't need to backup empty file
            return;
        }
        File backupFile = new File(fileUtility.resolvePath(file) + ".backup");
        Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        return arg.equals(ARG_SERVER)
               || arg.equals(ARG_CLIENT)
               || arg.equals(ARG_DISABLE)
               || arg.equals(ARG_CUSTOMPROFILE_FILE);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) throws IllegalArgumentException {
        String message = "";
        // We expect at least the task name
        if (args.length < 1) {
            message = getMessage("insufficientArgs");
        }

        if (!message.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * @see BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)
     */
    private String getArgumentValue(String arg, String[] args, String defalt) {
        return getArgumentValue(arg, args, defalt, null, stdin, stdout);
    }

}

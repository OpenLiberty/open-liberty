/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import java.io.IOException;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.ibm.websphere.crypto.InvalidPasswordEncodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.DefaultSubjectDN;
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 *
 */
public class CreateSSLCertificateTask extends BaseCommandTask {
    static final String SLASH = String.valueOf(File.separatorChar);

    static final String ARG_SERVER = "--server";
    static final String ARG_CLIENT = "--client";
    static final String ARG_PASSWORD = "--password";
    static final String ARG_VALIDITY = "--validity";
    static final String ARG_SUBJECT = "--subject";
    static final String ARG_ENCODING = "--passwordEncoding";
    static final String ARG_KEY = "--passwordKey";
    static final String ARG_CREATE_CONFIG_FILE = "--createConfigFile";
    static final String ARG_KEYSIZE = "--keySize";
    static final String ARG_SIGALG = "--sigAlg";
    static final String ARG_KEY_TYPE = "--keyType";
    static final String ARG_EXT = "--extInfo";

    static final String JKS_KEYFILE = "key.jks";
    static final String PKCS12_KEYFILE = "key.p12";

    static final String JKS = "jks";
    static final String PKCS12 = "pkcs12";

    private final DefaultSSLCertificateCreator creator;
    private final IFileUtility fileUtility;
    protected ConsoleWrapper stdin;
    protected PrintStream stdout;
    protected PrintStream stderr;

    public CreateSSLCertificateTask(DefaultSSLCertificateCreator creator,
                                    IFileUtility fileUtility, String scriptName) {
        super(scriptName);
        this.creator = creator;
        this.fileUtility = fileUtility;

    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "createSSLCertificate";
    }

    @Override
    public String getTaskDescription() {
        return getOption("sslCert.desc", true);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("sslCert.desc", "sslCert.usage.options",
                           "sslCert.required-key.", "sslCert.required-desc.",
                           "sslCert.option-key.", "sslCert.option-desc.",
                           "sslCert.option.addon", null,
                           scriptName,
                           DefaultSSLCertificateCreator.MINIMUM_PASSWORD_LENGTH,
                           DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                           DefaultSSLCertificateCreator.MINIMUM_VALIDITY,
                           DefaultSSLCertificateCreator.ALIAS,
                           DefaultSSLCertificateCreator.KEYALG_RSA_TYPE,
                           DefaultSSLCertificateCreator.SIGALG,
                           DefaultSSLCertificateCreator.DEFAULT_SIZE,
                           DefaultSSLCertificateCreator.SIGALG);
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
        this.stderr = stderr;

        validateArgumentList(args, Arrays.asList(new String[] { ARG_PASSWORD }));
        String serverName = getArgumentValue(ARG_SERVER, args, null);
        String clientName = getArgumentValue(ARG_CLIENT, args, null);
        String ou_name = null;
        String dir = null;

        // if a server and client both were specified we would not get this far

        // Verify the server or client exists, if it does not then exit and do not create the certificate
        // Do this first so we don't prompt for a password we'll not use
        if (serverName != null) {
            String usrServers = fileUtility.getServersDirectory();
            String serverDir = usrServers + serverName + SLASH;

            if (!fileUtility.exists(serverDir)) {
                usrServers = fileUtility.resolvePath(usrServers);
                stdout.println(getMessage("sslCert.abort"));
                stdout.println(getMessage("serverNotFound", serverName, usrServers));
                return SecurityUtilityReturnCodes.ERR_SERVER_NOT_FOUND;
            }
            dir = serverDir;
            ou_name = serverName;
        }

        if (clientName != null) {
            String usrClients = fileUtility.getClientsDirectory();
            String clientDir = usrClients + clientName + SLASH;

            if (!fileUtility.exists(clientDir)) {
                usrClients = fileUtility.resolvePath(usrClients);
                stdout.println(getMessage("sslCert.abort"));
                stdout.println(getMessage("sslCert.clientNotFound", clientName, usrClients));
                return SecurityUtilityReturnCodes.ERR_CLIENT_NOT_FOUND;
            }
            dir = clientDir;
            ou_name = clientName;
        }

        // Create the directories we need before we prompt for a password
        String location = null;

        String keyType = getArgumentValue(ARG_KEY_TYPE, args, null);
        if (keyType != null) {
            if (keyType.equalsIgnoreCase(JKS))
                location = dir + "resources" + SLASH + "security" + SLASH + JKS_KEYFILE;
            else if (keyType.equalsIgnoreCase(PKCS12))
                location = dir + "resources" + SLASH + "security" + SLASH + PKCS12_KEYFILE;
        } else {
            location = dir + "resources" + SLASH + "security" + SLASH + PKCS12_KEYFILE;
        }

        File fLocation = new File(location);
        location = fileUtility.resolvePath(fLocation);
        if (!fileUtility.createParentDirectory(stdout, fLocation)) {
            stdout.println(getMessage("sslCert.abort"));
            stdout.println(getMessage("file.requiredDirNotCreated", location));
            return SecurityUtilityReturnCodes.ERR_PATH_CANNOT_BE_CREATED;
        }

        if (fLocation.exists()) {
            stdout.println(getMessage("sslCert.abort"));
            stdout.println(getMessage("file.exists", location));
            return SecurityUtilityReturnCodes.ERR_FILE_EXISTS;
        }

        String password = getArgumentValue(ARG_PASSWORD, args, null);
        int validity = Integer.valueOf(getArgumentValue(ARG_VALIDITY, args, String.valueOf(DefaultSSLCertificateCreator.DEFAULT_VALIDITY)));
        String subjectDN = getArgumentValue(ARG_SUBJECT, args, new DefaultSubjectDN(null, ou_name).getSubjectDN());
        int keySize = Integer.valueOf(getArgumentValue(ARG_KEYSIZE, args, String.valueOf(DefaultSSLCertificateCreator.DEFAULT_SIZE)));
        String sigAlg = getArgumentValue(ARG_SIGALG, args, DefaultSSLCertificateCreator.SIGALG);
        List<String> extInfo = getExtInfoArgumentValues(ARG_EXT, args);

        try {
            String encoding = getArgumentValue(ARG_ENCODING, args, PasswordUtil.getDefaultEncoding());
            String key = getArgumentValue(ARG_KEY, args, null);
            stdout.println(getMessage("sslCert.createKeyStore", location));
            String encodedPassword = PasswordUtil.encode(password, encoding, key);
            creator.createDefaultSSLCertificate(location, password, keyType, null, validity, subjectDN, keySize, sigAlg, extInfo);
            String xmlSnippet = null;
            if (serverName != null) {
                stdout.println(getMessage("sslCert.serverXML", serverName, subjectDN));
                xmlSnippet = "    <featureManager>" + NL +
                             "        <feature>transportSecurity-1.0</feature>" + NL +
                             "    </featureManager>" + NL +
                             "    <keyStore id=\"defaultKeyStore\" password=\"" + encodedPassword + "\" />" + NL;

            } else {
                stdout.println(getMessage("sslCert.clientXML", clientName, subjectDN));
                xmlSnippet = "    <featureManager>" + NL +
                             "        <feature>appSecurityClient-1.0</feature>" + NL +
                             "    </featureManager>" + NL +
                             "    <keyStore id=\"defaultKeyStore\" password=\"" + encodedPassword + "\" />" + NL;

            }
            stdout.println(NL + createConfigFileIfNeeded(dir, args, xmlSnippet) + NL);
        } catch (CertificateException e) {
            stdout.println(getMessage("sslCert.createFailed", e.getMessage()));
            throw (e);
        } catch (InvalidPasswordEncodingException e) {
            stdout.println(getMessage("sslCert.errorEncodePassword", e.getMessage()));
            throw (e);
        } catch (UnsupportedCryptoAlgorithmException e) {
            stdout.println(getMessage("sslCert.errorEncodePassword", e.getMessage()));
            throw (e);
        }

        return SecurityUtilityReturnCodes.OK;
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        return arg.equals(ARG_SERVER) || arg.equals(ARG_PASSWORD) ||
               arg.equals(ARG_VALIDITY) || arg.equals(ARG_SUBJECT) ||
               arg.equals(ARG_ENCODING) || arg.equals(ARG_KEY) ||
               arg.equals(ARG_CREATE_CONFIG_FILE) || arg.equals(ARG_KEYSIZE) ||
               arg.equals(ARG_CLIENT) || arg.equals(ARG_SIGALG) ||
               arg.equals(ARG_KEY_TYPE) || arg.equals(ARG_EXT);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        String message = "";
        // We expect at least two arguments and the task name
        if (args.length < 3) {
            message = getMessage("insufficientArgs");
        }

        boolean serverFound = false;
        boolean clientFound = false;
        boolean passwordFound = false;
        for (String arg : args) {
            if (arg.startsWith(ARG_SERVER)) {
                serverFound = true;
            }
            if (arg.startsWith(ARG_CLIENT)) {
                clientFound = true;
            }
            if (arg.startsWith(ARG_PASSWORD)) {
                passwordFound = true;
            }
        }
        if (!serverFound && !clientFound) {
            //missingArg need either --server or --client
            message += " " + getMessage("missingArg2", ARG_SERVER, ARG_CLIENT);
        }
        if (serverFound && clientFound) {
            //both --server and --client can not be specified
            message += " " + getMessage("exclusiveArg", ARG_SERVER, ARG_CLIENT);
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
     * getExtInfoArgumentValues(String, String[])
     */
    private List<String> getExtInfoArgumentValues(String arg, String[] args) {
        boolean addSAN = true;
        ArrayList<String> extArgs = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            String key = args[i].split("=")[0];
            if (key.equals(arg)) {
                String value = getValue(args[i]);
                if (value.substring(0, 3).equalsIgnoreCase("SAN"))
                    addSAN = false;
                extArgs.add(value);
            }
        }
        if (addSAN) {
            String defaultSAN = defaultExtInfo();
            if (defaultSAN != null)
                extArgs.add(defaultSAN);
        }
        return extArgs;
    }

    /**
     * Create the default SAN extension value
     *
     * @param hostName May be {@code null}. If {@code null} an attempt is made to determine it.
     */
    public String defaultExtInfo() {
        String hostname = getHostName();
        String ext = null;

        InetAddress addr;
        try {
            addr = InetAddress.getByName(hostname);
            ext = "SAN=";
            if (addr != null && addr.toString().startsWith("/"))
                ext += "ip:" + hostname;
            else {
                // If the hostname start with a digit keytool will not create a SAN with the value
                if (!Character.isDigit(hostname.charAt(0)))
                    ext += "dns:" + hostname;
            }
            String ipAddresses = buildSanIpStringFromNetworkInterface();
            if (ipAddresses != null)
                ext = ext + ipAddresses;
        } catch (UnknownHostException e) {
            // use return null and not set SAN if there is an exception here
        }
        return ext;
    }

    /**
     * Get the host name.
     *
     * @return String value of the host name or "localhost" if not able to resolve
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException e) {
            return "localhost";
        }
    }

    /**
     *  This method builds subjectAltNames ip addresses from System's configured network interface
     **/
    private static String buildSanIpStringFromNetworkInterface() {
        List<String> sanIpAddress = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements()) {
                NetworkInterface n = e.nextElement();
                Enumeration<java.net.InetAddress> ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress ip = ee.nextElement();
                    if(ip instanceof java.net.Inet4Address){
                        sanIpAddress.add(ip.getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            // no network interfaces found for the system
            sanIpAddress.add("127.0.0.1");
        }

        // on Liberty consider using String.join(",", sanIpAddress); instead of below. (twas doesn't compile with String.join())
        if (!sanIpAddress.isEmpty()) {
            StringBuilder sb = new StringBuilder("ip:" + sanIpAddress.get(0).toString());
            for (int i = 1; i < sanIpAddress.size(); i++) {
                sb.append(",ip:");
                sb.append(sanIpAddress.get(i).toString());
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * This method acts like a filter for xml snippets. If the user provides the {@link #ARG_OPT_CREATE_CONFIG_FILE} option, then we will write it to a file and
     * provide an include snippet. Otherwise, we just return the provided xml snippet.
     *
     * @param serverDir Path to the root of the server. e.g. /path/to/wlp/usr/servers/myServer/
     * @param commandLine The command-line arguments.
     * @param xmlSnippet The xml configuration the task produced.
     * @return An include snippet or the given xmlSnippet.
     */
    protected String createConfigFileIfNeeded(String serverDir, String[] commandLine, String xmlSnippet) {
        String utilityName = this.scriptName;
        String taskName = this.getTaskName();

        final String MAGICAL_SENTINEL = "@!$#%$#%32543265k425k4/3nj5k43n?m2|5k4\\n5k2345";

        /*
         * getArgumentValue() can return 3 possible values.
         * null - The ARG_OPT_CREATE_CONFIG_FILE option was provided, but no value was associated with it.
         * MAGICAL_SENTINEL - The ARG_OPT_CREATE_CONFIG_FILE option was not in the command-line at all.
         * ??? - The value associated with ARG_OPT_CREATE_CONFIG_FILE is ???.
         */
        String targetFilepath = getArgumentValue(ARG_CREATE_CONFIG_FILE, commandLine, MAGICAL_SENTINEL);

        if (targetFilepath == MAGICAL_SENTINEL) {
            // the config file is not needed
            return xmlSnippet;
        }

        // note that generateConfigFileName() will handle the case where targetFilepath == null
        File outputFile = generateConfigFileName(utilityName, taskName, serverDir, targetFilepath);

        String xmlTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + NL +
                             "<server description=\"This file was generated by the ''{0} {1}'' command on {2,date,yyyy-MM-dd HH:mm:ss z}.\">" + NL +
                             "{3}" + NL +
                             "</server>" + NL;

        String xmlData = MessageFormat.format(xmlTemplate, utilityName, taskName, Calendar.getInstance().getTime(), xmlSnippet);

        fileUtility.createParentDirectory(stdout, outputFile);
        fileUtility.writeToFile(stderr, xmlData, outputFile);

        String includeSnippet = "    <include location=\"" + outputFile.getAbsolutePath() + "\" />" + NL;

        return includeSnippet;
    }

    protected File generateConfigFileName(String utilityName, String taskName, String serverDir, String targetFilepath) {
        // if no file path was provided, create the config file in the server directory
        if (targetFilepath == null || targetFilepath.equals("")) {
            targetFilepath = serverDir + SLASH;
        }

        // if the file path is a directory, generate a file name
        File outputFile = new File(targetFilepath);
        if (fileUtility.isDirectory(outputFile)) {
            outputFile = new File(outputFile, utilityName + "-" + taskName + "-include.xml");
        }

        // generate a new file name until we have no conflicts
        if (fileUtility.exists(outputFile)) {
            String filePath = FilenameUtils.removeExtension(outputFile.getPath());
            String fileExt = FilenameUtils.getExtension(outputFile.getPath());
            int counter = 1;
            do {
                outputFile = new File(filePath + counter + "." + fileExt);
                counter++;
            } while (fileUtility.exists(outputFile));
        }

        return outputFile;
    }

}

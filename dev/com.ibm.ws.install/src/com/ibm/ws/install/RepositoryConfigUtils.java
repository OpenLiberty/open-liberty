/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.install.RepositoryConfigValidationResult.ValidationFailedReason;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.repository.internal.RepositoryUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryException;

/**
 * RepositoryConfigUtils class to get Proxy information from property file
 */
public class RepositoryConfigUtils {

    public final static String WLP_REPO = "default";
    public final static String USE_WLP_REPO = "useDefaultRepository";
    public final static String ORDER = "order";
    public final static String URL_SUFFIX = ".url";
    public final static String APIKEY_SUFFIX = ".apiKey";
    public final static String USER_SUFFIX = ".user";
    public final static String USERPWD_SUFFIX = ".userPassword";
    public final static String PWD_SUFFIX = ".password";
    public final static String COMMENT_PREFIX = "#";
    public final static String PROXY_HOST = "proxyHost";
    public final static String PROXY_PORT = "proxyPort";
    public final static String PROXY_USER = "proxyUser";
    public final static String PROXY_PASSWORD = "proxyPassword";
    public final static String EQUALS = "=";
    private final static String[] SUPPORTED_KEYS = { USE_WLP_REPO, PROXY_HOST, PROXY_PORT, PROXY_USER, PROXY_PASSWORD };
    private static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);

    /**
     * Loads the repository properties into a Properties object
     *
     * @return A properties object with the repo properties
     * @throws InstallException
     */
    public static Properties loadRepoProperties() throws InstallException {
        Properties repoProperties = null;

        // Retrieves the Repository Properties file
        File repoPropertiesFile = new File(getRepoPropertiesFileLocation());

        //Check if default repository properties file location is overridden
        boolean isPropsLocationOverridden = System.getProperty(InstallConstants.OVERRIDE_PROPS_LOCATION_ENV_VAR) != null ? true : false;

        if (repoPropertiesFile.exists() && repoPropertiesFile.isFile()) {
            //Load repository properties
            repoProperties = new Properties();
            FileInputStream repoPropertiesInput = null;
            try {
                repoPropertiesInput = new FileInputStream(repoPropertiesFile);
                repoProperties.load(repoPropertiesInput);
            } catch (Exception e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_REPOSITORY_PROPS_NOT_LOADED",
                                                                                          getRepoPropertiesFileLocation()), InstallException.IO_FAILURE);
            } finally {
                InstallUtils.close(repoPropertiesInput);
            }
        } else if (isPropsLocationOverridden) {
            //Checks if the override location is a directory
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage(repoPropertiesFile.isDirectory() ? "ERROR_TOOL_REPOSITORY_PROPS_NOT_FILE" : "ERROR_TOOL_REPOSITORY_PROPS_NOT_EXISTS",
                                                                                      getRepoPropertiesFileLocation()), InstallException.IO_FAILURE);
        }
        return repoProperties;
    }

    /**
     * Finds the repository properties file location
     *
     * @return the location of the repo properties
     */
    public static String getRepoPropertiesFileLocation() {
        String installDirPath = Utils.getInstallDir().getAbsolutePath();
        String overrideLocation = System.getProperty(InstallConstants.OVERRIDE_PROPS_LOCATION_ENV_VAR);
        //Gets the repository properties file path from the default location
        if (overrideLocation == null) {
            return installDirPath + InstallConstants.DEFAULT_REPO_PROPERTIES_LOCATION;
        } else {
            //Gets the repository properties file from user specified location
            return overrideLocation;
        }
    }

    /**
     * Set Proxy Authenticator Default
     *
     * @param proxyHost The proxy host
     * @param proxyPort The proxy port
     * @param proxyUser the proxy username
     * @param decodedPwd the proxy password decoded
     */
    public static void setProxyAuthenticator(final String proxyHost, final String proxyPort, final String proxyUser, final String decodedPwd) {
        if (proxyUser == null || proxyUser.isEmpty() || decodedPwd == null || decodedPwd.isEmpty()) {
            return;
        }
        //Authenticate proxy credentials
        Authenticator.setDefault(new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() == RequestorType.PROXY) {
                    if (getRequestingHost().equals(proxyHost) && getRequestingPort() == Integer.valueOf(proxyPort)) {
                        return new PasswordAuthentication(proxyUser, decodedPwd.toCharArray());
                    }
                }
                return null;
            }
        });
    }

    /**
     * Gets a proxy for the inputted repository properties
     *
     * @param repoProperties The repository properties
     * @return A new RestRepositoryCOnnectionProxy object
     * @throws InstallException
     */
    public static RestRepositoryConnectionProxy getProxyInfo(Properties repoProperties) throws InstallException {
        RestRepositoryConnectionProxy proxyInfo = null;
        URL proxyURL = null;
        if (repoProperties != null) {
            //Retrieve proxy settings properties
            String proxyHost = repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_HOST);
            String proxyPort = repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_PORT);
            String proxyUser = getProxyUser(repoProperties);
            String proxyPwd = getProxyPwd(repoProperties);
            try {
                //Construct proxy URL object from proxy host and port
                if ((proxyHost != null && !proxyHost.isEmpty()) && (proxyPort != null && !proxyPort.isEmpty())) {
                    //Trim any trailing whitespaces
                    proxyHost = proxyHost.trim();
                    proxyPort = proxyPort.trim();
                    logger.log(Level.FINEST, "proxyHost: " + proxyHost);
                    logger.log(Level.FINEST, "proxyPort: " + proxyPort);
                    logger.log(Level.FINEST, "proxyUser: " + proxyUser);
                    logger.log(Level.FINEST, "proxyPassword: ********");
                    if (proxyHost.toLowerCase().startsWith("http://"))
                        proxyURL = new URL(proxyHost + ":" + proxyPort);
                    else
                        proxyURL = new URL("http://" + proxyHost + ":" + proxyPort);
                    //Check proxy port number in range
                    if (proxyURL.getPort() < 0 || proxyURL.getPort() > 0xFFFF)
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_INVALID_PROXY_PORT", String.valueOf(proxyURL.getPort())));
                } else {
                    if ((proxyHost == null || proxyHost.isEmpty()) && proxyPort != null) {
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PROXY_HOST_MISSING"), InstallException.MISSING_CONTENT);
                    } else if (proxyHost != null && (proxyPort == null || proxyPort.isEmpty())) {
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PROXY_PORT_MISSING"), InstallException.MISSING_CONTENT);
                    }
                }
            } catch (MalformedURLException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_INVALID_PROXY_PORT", proxyPort), e, InstallException.RUNTIME_EXCEPTION);
            }
            if (proxyURL != null) {
                if ((proxyUser != null && !proxyUser.isEmpty()) && (proxyPwd != null && !proxyPwd.isEmpty())) {
                    //Trim any trailing whitespaces
                    proxyUser = proxyUser.trim();
                    proxyPwd = proxyPwd.trim();
                    String decodedPwd = proxyPwd;
                    try {
                        //Decode encrypted proxy server password
                        decodedPwd = PasswordUtil.decode(proxyPwd);
                        //Check proxy server credentials for Authentication
                        setProxyAuthenticator(proxyHost, proxyPort, proxyUser, decodedPwd);
                    } catch (InvalidPasswordDecodingException ipde) {
                        decodedPwd = proxyPwd;
                        logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_PASSWORD_NOT_ENCODED_PROXY", proxyURL) + InstallUtils.NEWLINE);
                        setProxyAuthenticator(proxyHost, proxyPort, proxyUser, decodedPwd);
                    } catch (UnsupportedCryptoAlgorithmException ucae) {
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PROXY_PWD_CRYPTO_UNSUPPORTED"), ucae, InstallException.RUNTIME_EXCEPTION);
                    }
                }

                try {
                    proxyInfo = new RestRepositoryConnectionProxy(proxyURL);
                } catch (RepositoryException e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    if (sw.toString().contains("com.ibm.websphere.ssl.protocol.SSLSocketFactory"))
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT_JDK_WRONG"), e, InstallException.RUNTIME_EXCEPTION);
                    else
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT"), e, InstallException.RUNTIME_EXCEPTION);
                }

            }
        }
        return proxyInfo;
    }

    /**
     * Gets the Proxy user name.
     *
     * @param repoProperties the Repository Properties
     * @return the user name
     */
    public static String getProxyUser(Properties repoProperties) {
        return repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_USER);
    }

    /**
     * Gets the Proxy user password.
     *
     * @param repoProperties the Repository Properties
     * @return the user password
     */
    public static String getProxyPwd(Properties repoProperties) {
        if (repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_USERPWD) != null)
            return repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_USERPWD);
        else if (repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_PWD) != null)
            return repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_PWD);
        else
            return null;
    }

    /**
     * Compiles a list of repository names from a repository properties file, removing duplicates and .url suffixes
     *
     * @param repoProperties the Repository Properties
     * @return A list of Strings of repository names
     * @throws InstallException
     */
    public static List<String> getOrderList(Properties repoProperties) throws InstallException {
        List<String> orderList = new ArrayList<String>();
        List<String> repoList = new ArrayList<String>();
        // Retrieves the Repository Properties file
        File repoPropertiesFile = new File(getRepoPropertiesFileLocation());

        if (repoPropertiesFile.exists() && repoPropertiesFile.isFile()) {
            //Load repository properties
            FileInputStream repoPropertiesInput = null;
            BufferedReader repoPropertiesReader = null;
            try {
                repoPropertiesInput = new FileInputStream(repoPropertiesFile);
                repoPropertiesReader = new BufferedReader(new InputStreamReader(repoPropertiesInput));
                String line;
                String repoName;
                while ((line = repoPropertiesReader.readLine()) != null) {
                    String keyValue[] = line.split(EQUALS);
                    if (!line.startsWith(COMMENT_PREFIX) && keyValue.length > 1 && keyValue[0].endsWith(URL_SUFFIX)) {
                        repoName = keyValue[0].substring(0, keyValue[0].length() - URL_SUFFIX.length());
                        //Ignore empty repository names
                        if (repoName.isEmpty()) {
                            continue;
                        }
                        //Remove duplicate entries
                        if (orderList.contains(repoName)) {
                            repoList.remove(orderList.indexOf(repoName));
                            orderList.remove(repoName);
                        }
                        orderList.add(repoName);
                        repoList.add(line);

                        // if Windows, replace \ to \\
                        if (InstallUtils.isWindows && keyValue.length > 1 && keyValue[1].contains("\\")) {
                            String url = repoProperties.getProperty(keyValue[0]);
                            if (url != null && !InstallUtils.isURL(url)) {
                                repoProperties.put(keyValue[0], keyValue[1].trim());
                                logger.log(Level.FINEST, "The value of " + keyValue[0] + " was replaced to " + repoProperties.getProperty(keyValue[0]));
                            }
                        }
                    }
                }

            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_REPOSITORY_PROPS_NOT_LOADED",
                                                                                          getRepoPropertiesFileLocation()), InstallException.IO_FAILURE);
            } finally {
                InstallUtils.close(repoPropertiesInput);
                InstallUtils.close(repoPropertiesReader);
            }
        }

        if (isWlpRepoEnabled(repoProperties)) {
            orderList.add(WLP_REPO);
        }

        return orderList;
    }

    /**
     * Checks that the repository file exists
     *
     * @return True if repository exists
     */
    private static boolean repoPropertiesFileExists() {
        return new File(getRepoPropertiesFileLocation()).exists();
    }

    /**
     * Checks if the repository uses the WLP repository
     *
     * @param repoProperties The repository's properties
     * @return True if the repository is using WLP
     */
    public static boolean isWlpRepoEnabled(Properties repoProperties) {
        if (!repoPropertiesFileExists() || repoProperties == null)
            return true;
        String wlpEnabled = repoProperties.getProperty(USE_WLP_REPO);
        if (wlpEnabled == null)
            return true;
        return !wlpEnabled.trim().equalsIgnoreCase("false");
    }

    /**
     * Compiles a list of repository names from a repository properties file.
     *
     * @param repoProperties the Repository Properties
     * @return A list of Strings of repository names
     * @throws InstallException
     */
    public static List<RepositoryConfig> getRepositoryConfigs(Properties repoProperties) throws InstallException {
        List<String> orderList = getOrderList(repoProperties);
        List<RepositoryConfig> connections = new ArrayList<RepositoryConfig>(orderList.size());
        if (repoProperties == null || repoProperties.isEmpty()) {
            connections.add(new RepositoryConfig(WLP_REPO, null, null, null, null));
            return connections;
        }

        for (String r : orderList) {
            if (r.equalsIgnoreCase(WLP_REPO)) {
                connections.add(new RepositoryConfig(WLP_REPO, null, null, null, null));
                continue;
            }
            String url = repoProperties.getProperty(r + URL_SUFFIX);
            String user = null;
            String userPwd = null;
            if (url != null && !url.isEmpty()) {
                user = repoProperties.getProperty(r + USER_SUFFIX);
                if (user != null) {
                    if (user.isEmpty())
                        user = null;
                    else {
                        userPwd = repoProperties.getProperty(r + PWD_SUFFIX);
                        if (userPwd == null || userPwd.isEmpty()) {
                            userPwd = repoProperties.getProperty(r + USERPWD_SUFFIX);
                        }
                        if (userPwd != null && userPwd.isEmpty())
                            userPwd = null;
                    }
                }
                String apiKey = repoProperties.getProperty(r + APIKEY_SUFFIX);
                url = url.trim();
                if (!InstallUtils.isURL(url)) {
                    File f = new File(url);
                    try {
                        url = f.toURI().toURL().toString();
                    } catch (MalformedURLException e1) {
                        logger.log(Level.FINEST, "Failed to convert " + f.getAbsolutePath() + " to url format", e1);
                    }
                }
                connections.add(new RepositoryConfig(r, url, apiKey, user, userPwd));
            }
        }
        if (connections.isEmpty()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_NO_REPO_WAS_ENABLED"));
        }
        return connections;
    }

    /**
     * Gets the name of the repository by connection and properties
     *
     * @param repoProperties The repository properties
     * @param repoConn The repository connection
     * @return The name of the Repository
     * @throws InstallException
     */
    public static String getRepoName(Properties repoProperties, RepositoryConnection repoConn) throws InstallException {
        String repoName = null;
        List<RepositoryConfig> configRepos = RepositoryConfigUtils.getRepositoryConfigs(repoProperties);

        if (!(repoConn instanceof DirectoryRepositoryConnection || repoConn instanceof ZipRepositoryConnection)) {
            if (RepositoryConfigUtils.isLibertyRepository((RestRepositoryConnection) repoConn, repoProperties))
                return WLP_REPO;
        }
        for (RepositoryConfig rc : configRepos) {
            if (rc.getUrl() != null) {
                if (rc.getUrl().toLowerCase().startsWith("file:") && (repoConn instanceof DirectoryRepositoryConnection || repoConn instanceof ZipRepositoryConnection)) {
                    String repoDir = rc.getUrl();
                    try {
                        URL fileURL = new URL(repoDir);
                        File repoFile = new File(fileURL.getPath());
                        if (repoFile.getAbsolutePath().equalsIgnoreCase(repoConn.getRepositoryLocation())) {
                            repoName = rc.getId();
                            break;
                        }
                    } catch (Exception e) {
                        throw new InstallException(RepositoryUtils.getMessage("ERROR_DIRECTORY_NOT_EXISTS", repoDir));
                    }
                } else {
                    if (rc.getUrl().equalsIgnoreCase(repoConn.getRepositoryLocation())) {
                        repoName = rc.getId();
                        break;
                    }
                }
            }
        }
        return repoName;
    }

    /**
     * Checks if the inputed Repository Connection is a liberty repository
     *
     * @param lie The RestRepositoryConnection connection
     * @param repoProperties The repository properties
     * @return True of the repository location is a liberty location
     * @throws InstallException
     */
    public static boolean isLibertyRepository(RestRepositoryConnection lie, Properties repoProperties) throws InstallException {
        if (isWlpRepoEnabled(repoProperties)) {
            return lie.getRepositoryLocation().startsWith(InstallConstants.REPOSITORY_LIBERTY_URL);
        }
        return false;
    }

    /**
     * Checks if the inputed key is a supported property key
     *
     * @param key The Property key
     * @return True if the key is supported
     */
    private static boolean isKeySupported(String key) {
        if (Arrays.asList(SUPPORTED_KEYS).contains(key))
            return true;
        if (key.endsWith(URL_SUFFIX) || key.endsWith(APIKEY_SUFFIX) ||
            key.endsWith(USER_SUFFIX) || key.endsWith(PWD_SUFFIX) || key.endsWith(USERPWD_SUFFIX))
            return true;
        return false;
    }

    private static void validateRepositoryPropertiesLine(Properties repoProperties, String line, int lineNum, Map<String, String> configMap, Map<String, Integer> lineMap,
                                                         List<RepositoryConfigValidationResult> validationResults) {

        line = line.trim();
        //skip comment
        if (!line.isEmpty() && !line.startsWith(COMMENT_PREFIX)) {
            String keyValue[] = line.split(EQUALS);
            String key = null;
            String value = null;

            if (keyValue.length >= 2) {
                key = keyValue[0];
                value = keyValue[1];
            } else if (keyValue.length == 1) {
                key = keyValue[0];
            }

            //empty key check
            if (key == null || key.isEmpty()) {
                validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.EMPTY_KEY, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_EMPTY_KEY")));
                return;
            }

            //key is not supported
            if (!isKeySupported(key)) {
                validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.INVALID_KEY, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_INVALID_KEY",
                                                                                                                                                                       key)));
                return;
            }

            /**
             * emptyRepoName
             */
            if (key.endsWith(URL_SUFFIX) || key.endsWith(PWD_SUFFIX) || key.endsWith(USER_SUFFIX) || key.endsWith(USERPWD_SUFFIX) || key.endsWith(APIKEY_SUFFIX)) {
                int suffixIndex = key.lastIndexOf(".");
                String suffix = key.substring(suffixIndex);
                String repoName = keyValue[0].substring(0, keyValue[0].length() - suffix.length()).trim();
                //Check for empty repository names
                if (repoName.isEmpty()) {
                    logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_NO_REPO_NAME", line));
                    validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.MISSING_REPONAME, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_EMPTY_REPONAME")));
                    return;
                }
            }

            /**
             * duplicate keys
             */
            if (configMap.containsKey(key)) {
                validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.DUPLICATE_KEY, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_DUPLICATE_KEY",
                                                                                                                                                                         key,
                                                                                                                                                                         lineMap.get(key))));
                return;
            } else {
                configMap.put(key, value);
                lineMap.put(key, lineNum);
            }

            //empty value check
            if (value == null || value.isEmpty()) {
                validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.EMPTY_VALUE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_EMPTY_VALUE",
                                                                                                                                                                       key)));
                return;
            }

            /**
             * value is invalid useDefaultRepository
             */
            if (key.equals(USE_WLP_REPO)) {
                if (!(value.toLowerCase().equals("true") || value.toLowerCase().equals("false"))) {
                    validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.INVALID_VALUE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_INVALID_DEFAULTREPO_VALUE",
                                                                                                                                                                             value)));
                    return;
                }
            }

            if (key.endsWith(URL_SUFFIX)) {
                /**
                 * invalid url
                 *
                 */
                String url = value;
                url = repoProperties.getProperty(key);
                try {
                    url = url.trim();
                    if (InstallUtils.isURL(url)) {

                        URL repoUrl = new URL(url);
                        /**
                         * unsupported protocol
                         */
                        String protocol = repoUrl.getProtocol();
                        if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https") && !protocol.equalsIgnoreCase("file")) {
                            validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.UNSUPPORTED_PROTOCOL, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_UNSUPPORT_PROTOCOL",
                                                                                                                                                                                            value)));
                            return;
                        }
                    }

                } catch (MalformedURLException e) {
                    validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.INVALID_URL, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_INVALID_URL",
                                                                                                                                                                           value)));
                    return;
                }

            }

            /**
             * port number is between 1-65535
             */

            if (key.equals(PROXY_PORT)) {
                try {
                    int port = Integer.parseInt(value);
                    //Check proxy port number in range
                    if (port <= 0 || port > 0xFFFF) {
                        validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.INVALID_PORT, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_INVALID_PORT_VALUE",
                                                                                                                                                                                value)));
                        return;
                    }
                } catch (Exception e) {
                    validationResults.add(new RepositoryConfigValidationResult(lineNum, ValidationFailedReason.INVALID_PORT, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_INVALID_PORT_VALUE",
                                                                                                                                                                            value)));
                    return;
                }

            }
        }

    }

    /**
     * Checks that properties in the repository preoperty file are valid.
     *
     * @param repoProperties the Repository Properties
     * @return A list of RepositoryConfigValidationResults
     * @throws InstallException
     */
    public static List<RepositoryConfigValidationResult> validateRepositoryPropertiesFile(Properties repoProperties) throws InstallException {

        List<RepositoryConfigValidationResult> validationResults = new ArrayList<RepositoryConfigValidationResult>();
        // Retrieves the Repository Properties file
        File repoPropertiesFile = new File(getRepoPropertiesFileLocation());
        Map<String, String> configMap = new HashMap<String, String>();
        Map<String, Integer> lineMap = new HashMap<String, Integer>();

        if (repoPropertiesFile.exists() && repoPropertiesFile.isFile()) {
            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_VALIDATING", getRepoPropertiesFileLocation()));
            //Load repository properties
            FileInputStream repoPropertiesInput = null;
            BufferedReader repoPropertiesReader = null;
            try {
                repoPropertiesInput = new FileInputStream(repoPropertiesFile);
                repoPropertiesReader = new BufferedReader(new InputStreamReader(repoPropertiesInput));
                String line;
                int lineNum = 0;
                //Validate configurations in the repositories.properties file
                while ((line = repoPropertiesReader.readLine()) != null) {
                    lineNum++;
                    validateRepositoryPropertiesLine(repoProperties, line, lineNum, configMap, lineMap, validationResults);
                }
                //Missing Host
                if (configMap.containsKey(PROXY_HOST)) {
                    int ln = lineMap.get(PROXY_HOST);
                    if (!configMap.containsKey(PROXY_PORT)) {
                        validationResults.add(new RepositoryConfigValidationResult(ln, ValidationFailedReason.MISSING_PORT, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_MISSING_PORT_VALUE",
                                                                                                                                                                           configMap.get(PROXY_HOST))));
                    } else {
                        /**
                         * verify proxy host url along with the port number
                         */
                        String proxyHost = repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_HOST);
                        String proxyPort = repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_PORT);
                        try {
                            //Trim any trailing whitespaces
                            proxyHost = proxyHost.trim();
                            proxyPort = proxyPort.trim();
                            URL proxyUrl = null;
                            if (proxyHost.toLowerCase().contains("://")) {
                                proxyUrl = new URL(proxyHost + ":" + proxyPort);
                                if (!proxyUrl.getProtocol().toLowerCase().equals("http"))
                                    validationResults.add(new RepositoryConfigValidationResult(ln, ValidationFailedReason.INVALID_HOST, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_INVALID_HOST",
                                                                                                                                                                                       configMap.get(PROXY_HOST))));
                            } else {
                                new URL("http://" + proxyHost + ":" + proxyPort);
                            }

                        } catch (MalformedURLException e) {
                            validationResults.add(new RepositoryConfigValidationResult(ln, ValidationFailedReason.INVALID_HOST, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_INVALID_HOST",
                                                                                                                                                                               configMap.get(PROXY_HOST))));
                        }
                    }
                } else {
                    if (configMap.containsKey(PROXY_PORT) || configMap.containsKey(PROXY_USER) || configMap.containsKey(PROXY_PASSWORD)) {
                        int ln = 0;
                        if (lineMap.containsKey(PROXY_PORT))
                            ln = lineMap.get(PROXY_PORT);
                        else if (lineMap.containsKey(PROXY_USER))
                            ln = lineMap.get(PROXY_USER);
                        else
                            ln = lineMap.get(PROXY_PASSWORD);
                        validationResults.add(new RepositoryConfigValidationResult(ln, ValidationFailedReason.MISSING_HOST, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_VALIDATION_MISSING_HOST")));
                    }
                }
            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_REPOSITORY_PROPS_NOT_LOADED",
                                                                                          getRepoPropertiesFileLocation()), InstallException.IO_FAILURE);
            } finally {
                InstallUtils.close(repoPropertiesInput);
                InstallUtils.close(repoPropertiesReader);
            }
            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_VALIDATION_DONE"));
        }

        return validationResults;

    }

    /**
     * Validates properties files (Deprecated)
     *
     * @param repoProperties the repository Properties
     * @throws InstallException
     */
    @Deprecated
    public static void validatePropertiesFile(Properties repoProperties) throws InstallException {
        List<String> repoList = new ArrayList<String>();
        List<String> duplicateRepoList = new ArrayList<String>();

        // Retrieves the Repository Properties file
        File repoPropertiesFile = new File(getRepoPropertiesFileLocation());

        if (repoPropertiesFile.exists() && repoPropertiesFile.isFile()) {
            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_VALIDATING", getRepoPropertiesFileLocation()));
            //Load repository properties
            FileInputStream repoPropertiesInput = null;
            BufferedReader repoPropertiesReader = null;
            try {
                repoPropertiesInput = new FileInputStream(repoPropertiesFile);
                repoPropertiesReader = new BufferedReader(new InputStreamReader(repoPropertiesInput));
                String line;
                String repoName;
                //Validate configurations in the repositories.properties file
                while ((line = repoPropertiesReader.readLine()) != null) {
                    String keyValue[] = line.split(EQUALS);
                    if (!line.startsWith(COMMENT_PREFIX) && keyValue.length > 1 && keyValue[0].endsWith(URL_SUFFIX)) {
                        repoName = keyValue[0].substring(0, keyValue[0].length() - URL_SUFFIX.length());
                        //Check for empty repository names
                        if (repoName.isEmpty()) {
                            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_NO_REPO_NAME", line));
                            continue;
                        }
                        //Check for duplicate entries
                        if (repoList.contains(repoName)) {
                            logger.log(Level.FINE,
                                       Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_DUPLICATE_REPO_NAMES", repoName, duplicateRepoList.get(repoList.indexOf(repoName))));
                            duplicateRepoList.remove(repoList.indexOf(repoName));
                            repoList.remove(repoName);
                        }
                        repoList.add(repoName);
                        duplicateRepoList.add(line);
                    }
                }
                //Check for invalid value set for default repository
                String wlpEnabled = repoProperties.getProperty(USE_WLP_REPO);
                wlpEnabled = wlpEnabled == null ? null : wlpEnabled.trim();
                if (wlpEnabled != null && !wlpEnabled.equalsIgnoreCase("false") && !wlpEnabled.equalsIgnoreCase("true"))
                    logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_DEFAULT_INVALID_VALUE", wlpEnabled));
            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_REPOSITORY_PROPS_NOT_LOADED",
                                                                                          getRepoPropertiesFileLocation()), InstallException.IO_FAILURE);
            } finally {
                InstallUtils.close(repoPropertiesInput);
                InstallUtils.close(repoPropertiesReader);
            }
            logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_VALIDATION_DONE"));
        }
    }
}

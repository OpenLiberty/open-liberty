/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.utility.internal.cmdline;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernelInteractive;
import com.ibm.ws.install.RepositoryConfig;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.RepositoryConfigValidationResult;
import com.ibm.ws.install.internal.ExceptionUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.platform.InstallPlatformUtils;
import com.ibm.ws.install.utility.cmdline.CmdlineConstants;
import com.ibm.ws.install.utility.cmdline.ReturnCode;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.connections.liberty.MainRepository;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryHttpException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

import wlp.lib.extract.SelfExtract;
import wlp.lib.extract.platform.Platform;

/**
 * This class contains utilities for command functions.
 */
public class CmdUtils {

    private static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
    private static Locale locale;
    private static ResourceBundle installUtilityMessages;
    private static ResourceBundle installUtilitySampleConfigurations;
    private static List<RepositoryConnection> invalidRepoList;
    private static List<RepositoryConnection> repoAuthenticationList;
    private static int defaultRepoStatus;

    /**
     *
     * @param key string value associated with different messages
     * @return message associated with the key returned
     */
    public static synchronized String getMessage(String key, Object... args) {
        if (installUtilityMessages == null) {
            if (locale == null)
                locale = Locale.getDefault();
            installUtilityMessages = ResourceBundle.getBundle("com.ibm.ws.install.utility.internal.resources.InstallUtilityMessages", locale);
        }
        String message = installUtilityMessages.getString(key);
        if (args.length == 0)
            return message;
        MessageFormat messageFormat = new MessageFormat(message, locale);
        return messageFormat.format(args);
    }

    /**
     *
     * @return formated message for SAMPLE_CONFIGURATION key
     */
    public static synchronized String getSampleConfig() {
        if (installUtilitySampleConfigurations == null) {
            if (locale == null)
                locale = Locale.getDefault();
            installUtilitySampleConfigurations = ResourceBundle.getBundle("com.ibm.ws.install.utility.internal.resources.InstallUtilitySampleConfiguration", locale);
        }
        String message = installUtilitySampleConfigurations.getString("SAMPLE_CONFIG");
        File path = new File(InstallUtils.isWindows ? "C:\\IBM\\LibertyRepository" : "/usr/LibertyRepository");
        String url = null;
        try {
            url = path.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            url = InstallUtils.isWindows ? "file:/C:/IBM/LibertyRepository" : "file:///usr/LibertyRepository";
        }
        String featureRepo = null;
        try {
            ProductInfo product = ProductInfo.getAllProductInfo().get("com.ibm.websphere.appserver");
            featureRepo = product == null ? "wlp-featureRepo.zip" : "wlp-featureRepo-" + product.getVersion() + ".zip";
        } catch (Exception e) {
            featureRepo = "wlp-featureRepo.zip";
        }
        File zipRepoPath = new File(InstallUtils.isWindows ? "C:\\IBM" : "/usr", featureRepo);
        MessageFormat messageFormat = new MessageFormat(message, locale);
        return messageFormat.format(new Object[] { path.getAbsolutePath() + "1", zipRepoPath, url + "3" });
    }

    /**
     *
     * @param input any string value
     * @return strict PadRight of 35
     */
    public static String padRight(String input) {
        return padRight(input, 35);
    }

    /**
     *
     * @param input string value
     * @param length padding amount
     * @return variable PadRight of param:length
     */

    public static String padRight(String input, int length) {
        return String.format("%1$-" + length + "s", input);
    }

    /**
     *
     * @param urlStr
     * @return true if urlStri is a file Protocol
     */
    public static boolean isFileProtocol(String urlStr) {
        try {
            URL url = new URL(urlStr);
            if (url.getProtocol().equalsIgnoreCase("file")) {
                return true;
            }
            return false;
        } catch (MalformedURLException e) {
            return false;
        }

    }

    /**
     *
     * @param urlStr string input
     * @return if input is http/https/file it is a valid url
     */
    public static boolean isValidURL(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https") || protocol.equalsIgnoreCase("file")) {
                return true;
            }
            return false;
        } catch (MalformedURLException e) {
            return false;
        }

    }

    /**
     *
     * @param fromDir - pathname string
     * @return login info for the abstract pathname in a RepositoryConnectionList
     * @throws InstallException if Repo is invalid or file path does not exist
     */
    public static RepositoryConnectionList getDirectoryRepoLoginInfo(String fromDir) throws InstallException {
        try {
            File repoDir = new File(fromDir);
            List<RepositoryConnection> loginEntries = new ArrayList<RepositoryConnection>();
            if (repoDir.exists()) {
                if (repoDir.isDirectory()) {
                    if (!isValidDirectoryRepo(repoDir))
                        throw new InstallException(getMessage("ERROR_REPO_IS_INVALID", fromDir));
                    DirectoryRepositoryConnection lie = new DirectoryRepositoryConnection(repoDir);
                    loginEntries.add(lie);
                    RepositoryConnectionList loginInfo = new RepositoryConnectionList(loginEntries);
                    return loginInfo;
                } else {
                    if (!isValidZipBasedRepo(repoDir))
                        throw new InstallException(getMessage("ERROR_REPO_IS_INVALID", fromDir));
                    ZipRepositoryConnection lie = new ZipRepositoryConnection(repoDir);
                    loginEntries.add(lie);
                    RepositoryConnectionList loginInfo = new RepositoryConnectionList(loginEntries);
                    return loginInfo;
                }

            } else {
                throw new InstallException(com.ibm.ws.install.repository.internal.RepositoryUtils.getMessage("ERROR_FILEPATH_NOT_EXISTS", fromDir));
            }
        } catch (InstallException e) {
            throw e;
        } catch (Exception e) {
            throw new InstallException(com.ibm.ws.install.repository.internal.RepositoryUtils.getMessage("ERROR_FILEPATH_NOT_EXISTS", fromDir));
        }
    }

    /**
     *
     * @param dir
     * @return true if dir exists as a repo File
     */
    public static boolean isValidDirectoryRepo(File dir) {

        if (dir != null & dir.exists()) {
            File repoFile = new File(dir, "repository.config");
            if (repoFile.exists())
                return true;
        }
        return false;

    }

    /**
     *
     * @param zip - file name
     * @return true if zip is a valid Zip based Repo
     */
    @SuppressWarnings("resource")
    public static boolean isValidZipBasedRepo(File zip) {

        if (zip != null && zip.exists() && zip.isFile()) {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(zip);
                final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    if (entry.getName().equalsIgnoreCase("repository.config")) {
                        return true;
                    }
                }
            } catch (ZipException e) {
                return false;
            } catch (IOException e) {
                return false;
            } finally {
                InstallUtils.close(zipFile);
            }
        }
        return false;
    }

    private static int testConnectionToDefaultRepo(RestRepositoryConnectionProxy proxy) {
        try {
            RestRepositoryConnection lie = MainRepository.createConnection(proxy);
            return checkUserCredentials(lie);
        } catch (RepositoryBackendIOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            if (sw.toString().contains("com.ibm.websphere.ssl.protocol.SSLSocketFactory")) {
                logger.log(Level.FINEST, getStackTrace(e), e);
                return CmdlineConstants.WRONG_JDK;
            }

            if (e instanceof RepositoryHttpException) {
                if (((RepositoryHttpException) e).get_httpRespCode() == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                    return ((RepositoryHttpException) e).get_httpRespCode();
                }
                synchronized (CheckRepositoryStatusRunnable.class) {
                    logger.log(Level.FINE,
                               getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_FAILED").replace(".", ": ")
                                                 + InstallUtils.NEWLINE
                                                 + (getMessage("MSG_DEFAULT_REPO_NAME"))).trim());
                    logger.log(Level.FINE,
                               getWordWrappedMsg(getMessage("FIELD_REPO_REASON",
                                                            getMessage("LOG_REPO_CONNECTION_EXCEPTION",
                                                                       RepositoryConfigUtils.WLP_REPO,
                                                                       e.getClass().getSimpleName()
                                                                                                       + ": "
                                                                                                       + getMessage("LOG_HTTP_SERVER_RESPONSE_CODE",
                                                                                                                    ((RepositoryHttpException) e).get_httpRespCode(),
                                                                                                                    getMessage("MSG_DEFAULT_REPO_NAME")))),
                                                 "    "));
                    logger.log(Level.FINEST, getStackTrace(e), e);
                }
                return ((RepositoryHttpException) e).get_httpRespCode();
            } else {
                synchronized (CheckRepositoryStatusRunnable.class) {
                    logger.log(Level.FINE,
                               getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_FAILED").replace(".", ": ")
                                                 + InstallUtils.NEWLINE
                                                 + (getMessage("MSG_DEFAULT_REPO_NAME"))).trim());
                    logger.log(Level.FINE,
                               getWordWrappedMsg(getMessage("FIELD_REPO_REASON",
                                                            getMessage("LOG_REPO_CONNECTION_EXCEPTION", RepositoryConfigUtils.WLP_REPO,
                                                                       e.getClass().getSimpleName() + ": " + e.getMessage())),
                                                 "    "));
                    logger.log(Level.FINEST, getStackTrace(e), e);
                }
                return CmdlineConstants.CONNECTION_REFUSED;
            }
        }
    }

    /**
     *
     * @param installKernel - api to perform Liberty installation interactively
     * @param repoProperties - corresponds to property list for repo
     * @param actionName - string value
     * @param fromDir - string value
     * @return - return code based on Repository Status check success
     * @throws InstallException
     */
    public static ReturnCode checkRepositoryStatus(InstallKernelInteractive installKernel, Properties repoProperties, String actionName, String fromDir) throws InstallException {

        //if --from is specified, we only connect to this RepositoryConnection only
        if (fromDir != null) {

            RepositoryConnectionList dirLoginInfo = CmdUtils.getDirectoryRepoLoginInfo(fromDir);
            installKernel.setLoginInfo(dirLoginInfo);

            logger.log(Level.INFO, getMessage("MSG_CONNECT_REPO_SUCCESS") + InstallUtils.NEWLINE);
            return ReturnCode.OK;
        }

        //Validate repositories.properties file
        List<RepositoryConfigValidationResult> validationResults = RepositoryConfigUtils.validateRepositoryPropertiesFile(repoProperties);
        if (validationResults.size() == 0)
            logger.log(Level.FINE, getMessage("MSG_VALIDATION_SUCCESSFUL"));
        else
            logger.log(Level.FINE, getMessage("LOG_VALIDATION_FAILED") + InstallUtils.NEWLINE);
        RestRepositoryConnectionProxy proxy = RepositoryConfigUtils.getProxyInfo(repoProperties);

        RepositoryConnectionList loginInfo = getLoginInfo(installKernel, repoProperties, proxy);
        boolean isWlpRepoEnabled = RepositoryConfigUtils.isWlpRepoEnabled(repoProperties);
        int numOfRepos;
        if (isWlpRepoEnabled) {
            //Add Default repository
            numOfRepos = loginInfo != null ? loginInfo.size() + 1 : 1;
        } else {
            numOfRepos = loginInfo.size();
        }

        //Initialize repository lists and variables
        invalidRepoList = Collections.synchronizedList(new ArrayList<RepositoryConnection>(numOfRepos));
        repoAuthenticationList = Collections.synchronizedList(new ArrayList<RepositoryConnection>(numOfRepos));

        //Test connection to all configured repositories
        logger.log(Level.INFO, getMessage("MSG_CONNECTING"));

        if (loginInfo != null) {
            ExecutorService executor = Executors.newFixedThreadPool(numOfRepos);
            //Test user-defined configured repositories
            for (RepositoryConnection rc : loginInfo) {
                if (rc instanceof DirectoryRepositoryConnection || rc instanceof ZipRepositoryConnection) {
                    logger.log(Level.FINE,
                               getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_SUCCESS").replace(".", ": ")
                                                 + InstallUtils.NEWLINE
                                                 + RepositoryConfigUtils.getRepoName(repoProperties, rc) + " (" + rc.getRepositoryLocation() + ")"));
                    continue;
                }
                if (!isValidURL(((RestRepositoryConnection) rc).getRepositoryUrl())) {
                    logger.log(Level.FINE,
                               getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_FAILED").replace(".", ": ") + InstallUtils.NEWLINE
                                                 + RepositoryConfigUtils.getRepoName(repoProperties, rc)
                                                 + " (" + ((RestRepositoryConnection) rc).getRepositoryUrl() + ")").trim());
                    logger.log(Level.FINE,
                               getWordWrappedMsg(getMessage("FIELD_REPO_REASON",
                                                            getMessage("ERROR_REPO_UNSUPPORT_PROTOCOL", ((RestRepositoryConnection) rc).getRepositoryUrl())).replace("CWWKF1416E: ",
                                                                                                                                                                     ""),
                                                 "    "));
                    invalidRepoList.add(rc);
                    continue;
                }
                Runnable worker = new CheckRepositoryStatusRunnable(rc, repoProperties, proxy, false);
                executor.execute(worker);
            }
            //Test default repository
            if (isWlpRepoEnabled) {
                Runnable worker = new CheckRepositoryStatusRunnable(null, repoProperties, proxy, true);
                executor.execute(worker);
            }
            executor.shutdown();

            // Wait until all threads are finished
            while (!executor.isTerminated());
        } else if (isWlpRepoEnabled) {
            //Only default repository is configured
            defaultRepoStatus = testConnectionToDefaultRepo(proxy);
            ReturnCode rc = checkDefaultRepoStatus(proxy, repoProperties, defaultRepoStatus, actionName);
            if (rc == ReturnCode.OK) {
                RestRepositoryConnection lie = null;
                try {
                    lie = MainRepository.createConnection(proxy);
                } catch (RepositoryBackendIOException e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    if (sw.toString().contains("com.ibm.websphere.ssl.protocol.SSLSocketFactory"))
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT_JDK_WRONG"), e, InstallException.CONNECTION_FAILED);
                    else
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT"), e, InstallException.CONNECTION_FAILED);
                }
                if (lie != null) {
                    lie.setUserAgent(InstallConstants.ASSET_MANAGER);
                    loginInfo = new RepositoryConnectionList(lie);
                    logger.log(Level.INFO, getMessage("MSG_CONNECT_ALL_REPO_SUCCESS"));
                    installKernel.setLoginInfo(loginInfo);
                }
            }
            return rc;
        }

        //Check authentication to prompt user
        int connectionStatus = CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE;
        if (repoAuthenticationList != null && !repoAuthenticationList.isEmpty()) {
            for (RepositoryConnection rc : loginInfo) {
                if (repoAuthenticationList.contains(rc)) {
                    connectionStatus = credentialsPrompt((RestRepositoryConnection) rc, repoProperties, actionName);
                    if (connectionStatus == CmdlineConstants.USER_ABORT_ACTION) {
                        return ReturnCode.USER_ABORT;
                    } else if (connectionStatus == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                        //Add all repositories that require authentication to invalid list
                        invalidRepoList.addAll(repoAuthenticationList);
                        break;
                    } else if (connectionStatus != CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE) {
                        //Add unauthenticated repository to invalid list
                        invalidRepoList.add(rc);
                        continue;
                    }
                }
            }
        }

        //Check connection status to default repository
        boolean isDefaultRepoConnected = false;
        if (isWlpRepoEnabled) {
            if (connectionStatus != InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                try {
                    ReturnCode rc = checkDefaultRepoStatus(proxy, repoProperties, defaultRepoStatus, actionName);
                    if (rc != ReturnCode.OK) {
                        return rc;
                    } else {
                        isDefaultRepoConnected = true;
                        RestRepositoryConnection lie = null;
                        try {
                            lie = MainRepository.createConnection(proxy);
                        } catch (RepositoryBackendIOException e) {
                            isDefaultRepoConnected = false;
                        }
                        if (lie != null) {
                            lie.setUserAgent(InstallConstants.ASSET_MANAGER);
                            loginInfo.add(lie);
                        }
                    }
                } catch (InstallException ie) {
                    isDefaultRepoConnected = false;
                }
            }
        }

        //Skip invalid configured repositories
        if ((invalidRepoList != null && !invalidRepoList.isEmpty()) || (isWlpRepoEnabled && !isDefaultRepoConnected)) {
            //Proxy not authenticated, and no directory-based repositories
            if (connectionStatus == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE && invalidRepoList.size() == loginInfo.size() && invalidRepoList.containsAll(loginInfo)) {
                throw new InstallException(getMessage("ERROR_TOOL_INCORRECT_PROXY_CREDENTIALS",
                                                      RepositoryConfigUtils.getRepoPropertiesFileLocation()), InstallException.CONNECTION_FAILED);
            }
            //All configured repositories are bad
            if (invalidRepoList.containsAll(loginInfo) && invalidRepoList.size() == loginInfo.size()) {
                throw new InstallException(getMessage("ERROR_ALL_CONFIG_REPOS_FAIL", RepositoryConfigUtils.getRepoPropertiesFileLocation()), InstallException.CONNECTION_FAILED);
            }
            //Set new loginInfo object, if there are some good configured repositories
            else {
                StringBuffer sb = new StringBuffer();
                String delimitChar = "";
                for (RepositoryConnection rc : loginInfo) {
                    if (invalidRepoList.contains(rc)) {
                        sb.append(delimitChar).append(RepositoryConfigUtils.getRepoName(repoProperties, rc));
                        delimitChar = ", ";
                    }
                }
                //Skip invalid default repository
                if (isWlpRepoEnabled && !isDefaultRepoConnected)
                    sb.append(delimitChar).append(getMessage("MSG_DEFAULT_REPO_NAME"));

                logger.log(Level.WARNING, getWordWrappedMsg(getMessage("MSG_WARNING_SKIPPED_REPOS", sb.toString())));
                logger.log(Level.WARNING, getWordWrappedMsg(getMessage("MSG_VERIFY_REPO_CONNECTION", actionName)));
                loginInfo.removeAll(invalidRepoList);
                installKernel.setLoginInfo(loginInfo);
                return ReturnCode.BAD_CONNECTION_FOUND;
            }
        }
        logger.log(Level.INFO, getMessage("MSG_CONNECT_ALL_REPO_SUCCESS"));
        installKernel.setLoginInfo(loginInfo);
        return ReturnCode.OK;
    }

    private static ReturnCode checkDefaultRepoStatus(RestRepositoryConnectionProxy proxy, Properties repoProperties, int status, String actionName) throws InstallException {
        if (status == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
            status = promptProxyDefaultRepo(proxy, repoProperties, actionName);
            if (status == CmdlineConstants.USER_ABORT_ACTION) {
                return ReturnCode.USER_ABORT;
            }
            if (status == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                throw new InstallException(getMessage("ERROR_TOOL_INCORRECT_PROXY_CREDENTIALS",
                                                      RepositoryConfigUtils.getRepoPropertiesFileLocation()), InstallException.CONNECTION_FAILED);
            }
        }
        if (status == CmdlineConstants.UNTRUSTED_CERTIFICATE) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT_CAUSED_BY_CERT"), InstallException.CONNECTION_FAILED);
        }

        if (status == CmdlineConstants.WRONG_JDK) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT_JDK_WRONG"), InstallException.CONNECTION_FAILED);
        }

        if (status != CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT"), InstallException.CONNECTION_FAILED);
        } else {
            logger.log(Level.FINE,
                       getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_SUCCESS").replace(".", ": ")
                                         + InstallUtils.NEWLINE
                                         + (getMessage("MSG_DEFAULT_REPO_NAME"))));
            return ReturnCode.OK;
        }
    }

    /**
     * Gets the LoginInfo object for all the configured repositories. This object does not include the IBM WebSphere Liberty Repository
     *
     * @param installKernel
     * @param repoProperties
     * @param proxy
     *
     * @return The object list of loginInfo, which contains all the information of the configured repositories except the IBM WebSphere Liberty Repository
     * @throws InstallException if there is an error
     */
    private static RepositoryConnectionList getLoginInfo(InstallKernelInteractive installKernel, Properties repoProperties,
                                                         RestRepositoryConnectionProxy proxy) throws InstallException {
        RepositoryConnectionList loginInfo = null;
        List<RepositoryConfig> repositoryConfigs = RepositoryConfigUtils.getRepositoryConfigs(repoProperties);
        List<RepositoryConnection> loginEntries = new ArrayList<RepositoryConnection>(repositoryConfigs.size());
        List<String> notEncodedRepos = new ArrayList<String>();
        for (RepositoryConfig rc : repositoryConfigs) {
            RepositoryConnection lie = null;
            String url = rc.getUrl();
            if (url != null && url.toLowerCase().startsWith("file:")) {
                try {
                    URL urlProcessed = new URL(url);
                    File repoDir = new File(urlProcessed.getPath());
                    if (repoDir.exists()) {
                        if (repoDir.isDirectory()) {
                            if (!isValidDirectoryRepo(repoDir))
                                throw new InstallException(getMessage("ERROR_REPO_IS_INVALID", url));
                            lie = new DirectoryRepositoryConnection(repoDir);
                            loginEntries.add(lie);
                            continue;
                        } else {
                            if (!isValidZipBasedRepo(repoDir))
                                throw new InstallException(getMessage("ERROR_REPO_IS_INVALID", url));
                            lie = new ZipRepositoryConnection(repoDir);
                            loginEntries.add(lie);
                            continue;
                        }

                    } else {
                        throw new InstallException(com.ibm.ws.install.repository.internal.RepositoryUtils.getMessage("ERROR_FILEPATH_NOT_EXISTS", url));
                    }
                } catch (InstallException e) {
                    throw e;
                } catch (Exception e) {
                    throw new InstallException(com.ibm.ws.install.repository.internal.RepositoryUtils.getMessage("ERROR_FILEPATH_NOT_EXISTS", url));
                }
            }
            if (!rc.isLibertyRepository()) {
                String decodedPwd = rc.getUserPwd();
                if (decodedPwd != null && !decodedPwd.isEmpty()) {
                    try {
                        decodedPwd = PasswordUtil.decode(rc.getUserPwd());
                    } catch (InvalidPasswordDecodingException ipde) {
                        decodedPwd = rc.getUserPwd();
                        notEncodedRepos.add(rc.getId());
                    } catch (UnsupportedCryptoAlgorithmException ucae) {
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PWD_CRYPTO_UNSUPPORTED"), ucae, InstallException.CONNECTION_FAILED);
                    }
                }
                lie = new RestRepositoryConnection(rc.getUser(), decodedPwd, rc.getApiKey(), rc.getUrl().toString());
                ((RestRepositoryConnection) lie).setProxy(proxy);
            }
            if (lie != null) {
                ((RestRepositoryConnection) lie).setUserAgent(InstallConstants.ASSET_MANAGER);
                loginEntries.add(lie);
            }
        }
        if (!notEncodedRepos.isEmpty()) {
            String notEncodedReposString = InstallUtils.getFeatureListOutput(notEncodedRepos);
            logger.log(Level.FINE, CmdUtils.getMessage("LOG_PASSWORD_NOT_ENCODED", notEncodedReposString) + InstallUtils.NEWLINE);
        }

        //Set proxy object in kernel
        installKernel.setProxy(proxy);

        if (loginEntries != null && !loginEntries.isEmpty())
            loginInfo = new RepositoryConnectionList(loginEntries);

        return loginInfo;
    }

    /**
     *
     * @param proxy - defines proxy server locations & credentials (optional)
     * @param repoProperties - properties corresponding to the repo
     * @param actionName
     * @return - response code on prompt proxy success
     * @throws InstallException
     */

    public static int promptProxyDefaultRepo(RestRepositoryConnectionProxy proxy, Properties repoProperties, String actionName) throws InstallException {
        int proxyRetries = 3;
        int responseCode;
        String inputUser = null;
        String inputPwd = null;
        String proxyUser = RepositoryConfigUtils.getProxyUser(repoProperties);
        String proxyPwd = RepositoryConfigUtils.getProxyPwd(repoProperties);
        while ((responseCode = testConnectionToDefaultRepo(proxy)) != CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE) {
            if (responseCode == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                if (proxyRetries == 0) {
                    logger.log(Level.INFO, getWordWrappedMsg(getMessage("MSG_PROXY_AUTHENTICATION_FAIL", proxy.getProxyURL().toString())));
                    logger.log(Level.INFO, getWordWrappedMsg(getMessage("MSG_REACHED_MAX_PROXY_RETRIES", actionName, RepositoryConfigUtils.getRepoPropertiesFileLocation())));
                    boolean actionContinue = SelfExtract.getResponse(getMessage("TOOL_PROMPT_CONTINUE_OR_QUIT", actionName), "", "Xx");
                    logger.log(Level.INFO, "");
                    return !actionContinue ? CmdlineConstants.USER_ABORT_ACTION : responseCode;
                }
                //Credentials already defined in configuration, no prompt
                if (((proxyUser != null && !proxyUser.isEmpty()) && (proxyPwd != null && !proxyPwd.isEmpty()))) {
                    return responseCode;
                } else {
                    logger.log(Level.INFO, proxyRetries == 3 ? getMessage("MSG_AUTHENTICATION_PROMPT") : getMessage("MSG_AUTHENTICATION_RETRY", proxyRetries));
                    logger.log(Level.INFO, getMessage("FIELD_PROXY", proxy.getProxyURL().toString()));
                    if (proxyUser != null && !proxyUser.isEmpty() && proxyRetries == 3) {
                        inputUser = proxyUser;
                        logger.log(Level.INFO, getMessage("TOOL_PROMPT_USERNAME") + " " + inputUser);
                        inputPwd = getPromptPassword();
                    } else {
                        inputUser = getPromptUsername();
                        inputPwd = getPromptPassword();
                    }
                    proxyRetries--;

                    //Set proxy credentials
                    if (inputUser != null && inputPwd != null) {
                        RepositoryConfigUtils.setProxyAuthenticator(proxy.getProxyURL().getHost(), String.valueOf(proxy.getProxyURL().getPort()), inputUser,
                                                                    inputPwd);
                    }
                    logger.log(Level.FINE, getMessage("LOG_PROMPT_PROXY_AUTHENTICATION"));
                    continue;
                }
            }
            if (proxyRetries < 3) {
                //Reset proxy authentication counter
                proxyRetries = 3;
                logger.log(Level.FINE, getMessage("LOG_PROMPT_PROXY_SUCCESS", proxy.getProxyURL().toString()));
            }
            return responseCode;
        } ;
        if (proxyRetries < 3)
            logger.log(Level.FINE, getMessage("LOG_PROMPT_PROXY_SUCCESS", proxy.getProxyURL().toString()));
        return responseCode;
    }

    /**
     * Get proxy credentials such as username and password
     *
     * @param lie
     * @param repoProperties
     * @param actionName
     * @return - HTTP SUCCESS RESPONSE CODE on successful credentials prompt
     * @throws InstallException - when Proxy Authentication and Message Authentication fails
     */
    public static int credentialsPrompt(RestRepositoryConnection lie, Properties repoProperties, String actionName) throws InstallException {
        int proxyRetries = 3;
        int repoRetries = 3;
        int responseCode;
        String inputUser = null;
        String inputPwd = null;
        String repoUser = lie.getUserId();
        String repoPwd = lie.getPassword();
        String proxyUser = RepositoryConfigUtils.getProxyUser(repoProperties);
        String proxyPwd = RepositoryConfigUtils.getProxyPwd(repoProperties);
        while ((responseCode = checkUserCredentials(lie)) != CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE) {
            if (responseCode == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                if (proxyRetries == 0) {
                    logger.log(Level.INFO, getWordWrappedMsg(getMessage("MSG_PROXY_AUTHENTICATION_FAIL", lie.getProxy().getProxyURL().toString())));
                    logger.log(Level.INFO, getWordWrappedMsg(getMessage("MSG_REACHED_MAX_PROXY_RETRIES", actionName, RepositoryConfigUtils.getRepoPropertiesFileLocation())));
                    boolean actionContinue = SelfExtract.getResponse(getMessage("TOOL_PROMPT_CONTINUE_OR_QUIT", actionName), "", "Xx");
                    logger.log(Level.INFO, "");
                    return !actionContinue ? CmdlineConstants.USER_ABORT_ACTION : responseCode;
                }
                //Credentials already defined in configuration, no prompt
                if (((proxyUser != null && !proxyUser.isEmpty()) && (proxyPwd != null && !proxyPwd.isEmpty()))) {
                    return responseCode;
                } else {
                    logger.log(Level.INFO, proxyRetries == 3 ? getMessage("MSG_AUTHENTICATION_PROMPT") : getMessage("MSG_AUTHENTICATION_RETRY", proxyRetries));
                    logger.log(Level.INFO, getMessage("FIELD_PROXY", lie.getProxy().getProxyURL().toString()));
                    if (proxyUser != null && !proxyUser.isEmpty() && proxyRetries == 3) {
                        inputUser = proxyUser;
                        logger.log(Level.INFO, getMessage("TOOL_PROMPT_USERNAME") + " " + inputUser);
                        inputPwd = getPromptPassword();
                    } else {
                        inputUser = getPromptUsername();
                        inputPwd = getPromptPassword();
                    }
                    proxyRetries--;

                    //Set proxy credentials
                    if (inputUser != null && inputPwd != null) {
                        RepositoryConfigUtils.setProxyAuthenticator(lie.getProxy().getProxyURL().getHost(), String.valueOf(lie.getProxy().getProxyURL().getPort()), inputUser,
                                                                    inputPwd);
                    }
                    logger.log(Level.FINE, getMessage("LOG_PROMPT_PROXY_AUTHENTICATION"));
                    continue;
                }
            }
            if (proxyRetries < 3) {
                //Reset proxy authentication counter
                proxyRetries = 3;
                logger.log(Level.FINE, getMessage("LOG_PROMPT_PROXY_SUCCESS", lie.getProxy().getProxyURL().toString()));
            }
            if (responseCode == CmdlineConstants.HTTP_AUTH_RESPONSE_CODE) {
                if (repoRetries == 0) {
                    logger.log(Level.INFO, getWordWrappedMsg(getMessage("MSG_AUTHENTICATION_FAIL", RepositoryConfigUtils.getRepoName(repoProperties, lie))));
                    logger.log(Level.INFO, getWordWrappedMsg(getMessage("MSG_REACHED_MAX_RETRIES", actionName)));
                    boolean actionContinue = SelfExtract.getResponse(getMessage("TOOL_PROMPT_CONTINUE_OR_QUIT", actionName), "", "Xx");
                    logger.log(Level.INFO, "");
                    return !actionContinue ? CmdlineConstants.USER_ABORT_ACTION : responseCode;
                }
                //Credentials already defined in configuration, no prompt
                if ((repoUser != null && !repoUser.isEmpty()) && (repoPwd != null && !repoPwd.isEmpty())) {
                    return responseCode;
                } else {
                    logger.log(Level.INFO, repoRetries == 3 ? getMessage("MSG_AUTHENTICATION_PROMPT") : getMessage("MSG_AUTHENTICATION_RETRY", repoRetries));
                    logger.log(Level.INFO, getMessage("FIELD_REPO", RepositoryConfigUtils.getRepoName(repoProperties, lie), lie.getRepositoryUrl()));
                    if (repoUser != null && !repoUser.isEmpty() && repoRetries == 3) {
                        inputUser = repoUser;
                        logger.log(Level.INFO, getMessage("TOOL_PROMPT_USERNAME") + " " + inputUser);
                        inputPwd = getPromptPassword();
                    } else {
                        inputUser = getPromptUsername();
                        inputPwd = getPromptPassword();
                    }
                    repoRetries--;

                    //Set Repository credentials
                    if (inputUser != null && inputPwd != null) {
                        lie.setUserId(inputUser);
                        lie.setPassword(inputPwd);
                    }
                    logger.log(Level.FINE, getMessage("LOG_PROMPT_REPO_AUTHENTICATION", RepositoryConfigUtils.getRepoName(repoProperties, lie)));
                    continue;
                }
            }
            logger.log(Level.FINE,
                       getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_FAILED").replace(".", ": ") + InstallUtils.NEWLINE + RepositoryConfigUtils.getRepoName(repoProperties, lie)
                                         + " (" + (lie.getRepositoryUrl() + ")")));
            return responseCode;
        } ;
        if (repoRetries < 3) {
            logger.log(Level.FINE, getMessage("LOG_PROMPT_REPO_SUCCESS", RepositoryConfigUtils.getRepoName(repoProperties, lie)));
        }
        logger.log(Level.FINE,
                   getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_SUCCESS").replace(".", ": ")
                                     + InstallUtils.NEWLINE
                                     + (RepositoryConfigUtils.isLibertyRepository(lie,
                                                                                  repoProperties) ? getMessage("MSG_DEFAULT_REPO_NAME") : RepositoryConfigUtils.getRepoName(repoProperties,
                                                                                                                                                                            lie)
                                                                                                                                          + " (" + lie.getRepositoryUrl()
                                                                                                                                          + ")")));
        return responseCode;
    }

    private static String getPromptUsername() {
        return SelfExtract.getResponse(getMessage("TOOL_PROMPT_USERNAME") + " ");
    }

    private static String getPromptPassword() {
        String pwdStr = "";
        System.out.print(getMessage("TOOL_PROMPT_PASSWORD") + " ");
        Console console = System.console();
        if (console != null) {
            char[] promptPwd = console.readPassword();
            if (promptPwd != null) {
                pwdStr = new String(promptPwd);
                if (Platform.isZOS() && Charset.isSupported(InstallPlatformUtils.getEBCIDICSystemCharSet()) && !isStringASCII(pwdStr)) {
                    //Get EBCIDIC encoded string
                    pwdStr = new String(pwdStr.getBytes(Charset.defaultCharset()), Charset.forName(InstallPlatformUtils.getEBCIDICSystemCharSet())).trim();
                }
            }
        }
        logger.log(Level.INFO, "");
        return pwdStr;
    }

    private static boolean isStringASCII(String str) {
        try {
            //Encode original string using System ASCII code page
            String asciiEncoded = new String(str.getBytes(Charset.defaultCharset()), Charset.forName(InstallPlatformUtils.getASCIISystemCharSet())).trim();

            //Check if ASCII encoded string matches the original string.
            if (str.equals(asciiEncoded)) {
                logger.log(Level.FINEST, "com.ibm.ws.install.utility.internal.cmdline.CmdUtils - isStringASCII(): String is ASCII encoded.");
                return true;
            }
        } catch (UnsupportedCharsetException uce) {
            logger.log(Level.FINEST, "com.ibm.ws.install.utility.internal.cmdline.CmdUtils - isStringASCII(): System does not support ASCII encoding.");
            return false;
        }
        logger.log(Level.FINEST, "com.ibm.ws.install.utility.internal.cmdline.CmdUtils - isStringASCII(): String is not ASCII encoded.");
        return false;
    }

    private static int checkUserCredentials(RestRepositoryConnection lie) {
        try {
            lie.checkRepositoryStatus();
        } catch (IOException ioe) {
            logger.log(Level.FINEST, "Exception Thrown for: " + lie.getRepositoryLocation() + InstallUtils.NEWLINE + getStackTrace(ioe), ioe);
            if (ExceptionUtils.isCertPathBuilderException(ioe.getCause())) {
                return CmdlineConstants.UNTRUSTED_CERTIFICATE;
            }
            return CmdlineConstants.CONNECTION_REFUSED;
        } catch (RequestFailureException rfe) {
            if (rfe.getResponseCode() != CmdlineConstants.HTTP_AUTH_RESPONSE_CODE && rfe.getResponseCode() != InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                logger.log(Level.FINEST, getStackTrace(rfe), rfe);
            }
            return rfe.getResponseCode();
        }
        return CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE;
    }

    private static void checkInvalidRepository(RepositoryConnection rc, Properties repoProperties) throws InstallException {
        boolean isLibertyRepo = RepositoryConfigUtils.isLibertyRepository((RestRepositoryConnection) rc, repoProperties);
        try {
            rc.checkRepositoryStatus();
            logger.log(Level.FINE,
                       getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_SUCCESS").replace(".", ": ")
                                         + InstallUtils.NEWLINE
                                         + (isLibertyRepo ? getMessage("MSG_DEFAULT_REPO_NAME") : RepositoryConfigUtils.getRepoName(repoProperties, rc) + " ("
                                                                                                  + ((RestRepositoryConnection) rc).getRepositoryUrl() + ")")));
        } catch (IOException ioe) {
            invalidRepoList.add(rc);
            synchronized (CheckRepositoryStatusRunnable.class) {
                logger.log(Level.FINE,
                           getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_FAILED").replace(".", ": ")
                                             + InstallUtils.NEWLINE
                                             + (isLibertyRepo ? getMessage("MSG_DEFAULT_REPO_NAME") : RepositoryConfigUtils.getRepoName(repoProperties, rc) + " ("
                                                                                                      + ((RestRepositoryConnection) rc).getRepositoryUrl() + ")")).trim());
                logger.log(Level.FINE,
                           getWordWrappedMsg(getMessage("FIELD_REPO_REASON",
                                                        getMessage("LOG_REPO_CONNECTION_EXCEPTION", RepositoryConfigUtils.getRepoName(repoProperties, rc),
                                                                   ioe.getClass().getSimpleName() + ": " + ioe.getMessage())),
                                             "    "));
                logger.log(Level.FINEST, getStackTrace(ioe), ioe);
            }
        } catch (RequestFailureException rfe) {
            if (rfe.getResponseCode() == CmdlineConstants.HTTP_AUTH_RESPONSE_CODE || rfe.getResponseCode() == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                repoAuthenticationList.add(rc);
                return;
            }
            invalidRepoList.add(rc);
            synchronized (CheckRepositoryStatusRunnable.class) {
                logger.log(Level.FINE,
                           getWordWrappedMsg(getMessage("MSG_CONNECT_REPO_FAILED").replace(".", ": ")
                                             + InstallUtils.NEWLINE
                                             + (isLibertyRepo ? getMessage("MSG_DEFAULT_REPO_NAME") : RepositoryConfigUtils.getRepoName(repoProperties, rc) + " ("
                                                                                                      + ((RestRepositoryConnection) rc).getRepositoryUrl() + ")")).trim());
                logger.log(Level.FINE,
                           getWordWrappedMsg(getMessage("FIELD_REPO_REASON",
                                                        getMessage("LOG_REPO_CONNECTION_EXCEPTION",
                                                                   RepositoryConfigUtils.getRepoName(repoProperties, rc),
                                                                   rfe.getClass().getSimpleName()
                                                                                                                          + ": "
                                                                                                                          + getMessage("LOG_HTTP_SERVER_RESPONSE_CODE",
                                                                                                                                       rfe.getResponseCode(),
                                                                                                                                       ((RestRepositoryConnection) rc).getRepositoryUrl()))),
                                             "    "));
                logger.log(Level.FINEST, getStackTrace(rfe), rfe);
            }
        }
    }

    /**
     *
     * @param message - string input
     * @return Wrapped message terminated with a space
     */
    public static String getWordWrappedMsg(String message) {
        return getWordWrappedMsg(message, "");
    }

    /**
     *
     * @param message - string input
     * @param indentStr - indent amount string input
     * @return - append indentStr to message
     */
    public static String getWordWrappedMsg(String message, String indentStr) {
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = new BufferedReader(new StringReader(message));
        try {
            for (String line; (line = reader.readLine()) != null;) {
                InstallUtils.wordWrap(sb, line, indentStr);
            }
            return sb.toString();
        } catch (IOException e) {
            sb = new StringBuffer();
            InstallUtils.wordWrap(sb, message, indentStr);
            return sb.toString();
        }
    }

    /**
     *
     * @param e - exception value
     * @return - e's throwable and backtrace as string
     */
    public static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Runnable class to spawn new threads to test connection for each configured repository
     */
    private static class CheckRepositoryStatusRunnable implements Runnable {
        private final RepositoryConnection repositoryConnection;
        private final Properties repoProperties;
        private final RestRepositoryConnectionProxy proxy;
        private final boolean testDefaultRepo;

        CheckRepositoryStatusRunnable(RepositoryConnection rc, Properties repoProperties, RestRepositoryConnectionProxy proxy, boolean testDefaultRepo) {
            this.repositoryConnection = rc;
            this.repoProperties = repoProperties;
            this.proxy = proxy;
            this.testDefaultRepo = testDefaultRepo;
        }

        @Override
        public void run() {
            try {
                if (repositoryConnection != null && !testDefaultRepo) {
                    checkInvalidRepository(repositoryConnection, repoProperties);
                } else {
                    defaultRepoStatus = testConnectionToDefaultRepo(proxy);
                }
            } catch (InstallException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }

        }
    }

    /**
     *
     * @param e - installation exception
     * @param isBadConnectionFound - boolean
     * @return - if bad connection found convert installation exception to bad connection error otherwise keep install exception
     */
    public static InstallException convertToBadConnectionError(InstallException e, boolean isBadConnectionFound) {
        if (isBadConnectionFound) {
            List<Object> data = e.getData();

            if (e.getMessage().contains("CWWKF1259E")) {
                return new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_RESOLVE_ASSETS_BAD_CONNECTION",
                                                                                           data.get(0)), e.getCause(), e.getRc());

            } else if (e.getMessage().contains("CWWKF1258E")) {
                return new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_ASSET_MISSING_DEPENDENT_BAD_CONNECTION",
                                                                                           data.get(0), data.get(1)), e.getCause(), e.getRc());

            }
        }
        return e;
    }
}

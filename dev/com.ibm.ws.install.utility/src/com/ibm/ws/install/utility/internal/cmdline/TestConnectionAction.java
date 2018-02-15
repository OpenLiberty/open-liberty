/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.utility.internal.cmdline;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.RepositoryConfig;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.internal.ExceptionUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.cmdline.InstallExecutor;
import com.ibm.ws.install.utility.cmdline.CmdlineConstants;
import com.ibm.ws.install.utility.cmdline.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;
import com.ibm.ws.repository.connections.liberty.MainRepository;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryHttpException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

/**
 * This API is used to execute the Test Connection Action.
 * This action enures that the connection is valid.
 */
public class TestConnectionAction implements ActionHandler {

    private Properties repoProperties;
    private File repoPropertiesFile;
    private String repoId;
    private static ReturnCode returnCode = ReturnCode.OK;
    private static Map<String, TestConnectionResults> repoConnResults = new ConcurrentHashMap<String, TestConnectionResults>();
    private static Map<String, RepositoryConfig> repoMap = new ConcurrentHashMap<String, RepositoryConfig>();
    private static List<RepositoryConfig> repoAuthenticationList;

    ReturnCode initialize(Arguments args) {
        if (args.getPositionalArguments().size() > 1) {
            System.out.println(CmdUtils.getMessage("ERROR_MORE_THAN_0_OR_1_ARGUMENTS", Action.testConnection, args.getPositionalArguments().size()));
            return ReturnCode.BAD_ARGUMENT;
        }
        repoId = args.getPositionalArguments().isEmpty() ? null : args.getPositionalArguments().get(0);
        return ReturnCode.OK;
    }

    /**
     * Initialize and Load Repo properties
     * Run testRepositories() to complete action
     */
    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        ReturnCode rc = initialize(args);
        if (!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        // Initialize the repository properties
        repoPropertiesFile = new File(RepositoryConfigUtils.getRepoPropertiesFileLocation());
        if (repoPropertiesFile.exists()) {
            // Load the repository properties instance from properties file
            try {
                repoProperties = RepositoryConfigUtils.loadRepoProperties();
            } catch (InstallException e) {
                System.out.println(e.getMessage());
                return ReturnCode.IO_FAILURE;
            }
        }

        try {
            return testRepositories();
        } catch (InstallException e) {
            System.out.println(e.getMessage());
            return InstallExecutor.returnCode(e.getRc());
        }
    }

    private static ReturnCode testRepository(RepositoryConfig rc, RestRepositoryConnectionProxy proxy, String proxyStatus) {
        RestRepositoryConnection lie = null;
        TestConnectionResults results = new TestConnectionResults(null, null, null);
        if (rc.isLibertyRepository()) {
            if (proxyStatus != null) {
                //If the specified proxy info is invalid, display error message
                results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                results.setTestReason(proxyStatus);
                repoConnResults.put(rc.getId(), results);
                return ReturnCode.CONNECTION_FAILED;
            }
            try {
                lie = MainRepository.createConnection(proxy);
            } catch (RepositoryBackendIOException e) {
                Throwable cause = e;
                Throwable rootCause = e;
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);

                if (e instanceof RepositoryHttpException) {
                    if (((RepositoryHttpException) e).get_httpRespCode() == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                        repoAuthenticationList.add(rc);
                        return ReturnCode.OK;
                    }
                }

                // Check the list of causes of the exception for connection issues
                while ((rootCause = cause.getCause()) != null && cause != rootCause) {
                    if (rootCause instanceof UnknownHostException && proxy != null) {
                        results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                        results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_UNKNOWN_PROXY_HOST", proxy.getProxyURL().getHost()));
                        repoConnResults.put(rc.getId(), results);
                        return ReturnCode.CONNECTION_FAILED;
                    }
                    if (rootCause instanceof ConnectException && rootCause.getMessage() != null && rootCause.getMessage().contains("Connection refused") && proxy != null) {
                        results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                        results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_INCORRECT_PROXY_PORT", String.valueOf(proxy.getProxyURL().getPort())));
                        repoConnResults.put(rc.getId(), results);
                        return ReturnCode.CONNECTION_FAILED;
                    }
                    cause = rootCause;
                }
                results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                if (sw.toString().contains("com.ibm.websphere.ssl.protocol.SSLSocketFactory"))
                    results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT_JDK_WRONG"));
                else
                    results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT"));
                repoConnResults.put(rc.getId(), results);
                return ReturnCode.CONNECTION_FAILED;
            }
        } else {
            URL url;
            try {
                url = new URL(rc.getUrl());
                String protocol = url.getProtocol();
                if (!protocol.equalsIgnoreCase("http") && !protocol.equalsIgnoreCase("https") && !protocol.equalsIgnoreCase("file")) {
                    results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                    results.setTestReason(CmdUtils.getMessage("ERROR_REPO_UNSUPPORT_PROTOCOL", rc.getUrl()));
                    repoConnResults.put(rc.getId(), results);
                    return ReturnCode.CONNECTION_FAILED;
                }
            } catch (MalformedURLException e) {
                results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                results.setTestReason(CmdUtils.getMessage("ERROR_REPO_INVALID_URL", rc.getUrl()));
                repoConnResults.put(rc.getId(), results);
                return ReturnCode.CONNECTION_FAILED;
            }
            if (url.getProtocol().equalsIgnoreCase("file")) {
                try {
                    File f = new File(url.toURI());
                    if (!f.exists()) {
                        results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                        results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_NOT_EXISTS", f.getAbsolutePath()));
                        repoConnResults.put(rc.getId(), results);
                        return ReturnCode.CONNECTION_FAILED;
                    }
                    if (!f.isDirectory()) {
                        if (!CmdUtils.isValidZipBasedRepo(f)) {
                            results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                            results.setTestReason(CmdUtils.getMessage("ERROR_REPO_IS_INVALID", f.getAbsolutePath()));
                            repoConnResults.put(rc.getId(), results);
                            return ReturnCode.CONNECTION_FAILED;
                        }
                    } else {
                        if (!CmdUtils.isValidDirectoryRepo(f)) {
                            results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                            results.setTestReason(CmdUtils.getMessage("ERROR_REPO_IS_INVALID", f.getAbsolutePath()));
                            repoConnResults.put(rc.getId(), results);
                            return ReturnCode.CONNECTION_FAILED;
                        }
                    }

                    results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_SUCCESS"));
                    repoConnResults.put(rc.getId(), results);
                } catch (Exception e) {
                    results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                    results.setTestReason(CmdUtils.getMessage("ERROR_REPO_INVALID_URL", rc.getUrl()));
                    repoConnResults.put(rc.getId(), results);
                    return ReturnCode.CONNECTION_FAILED;
                }
                return ReturnCode.OK;
            }
            String decodedPwd = rc.getUserPwd();
            if (decodedPwd != null && !decodedPwd.isEmpty()) {
                try {
                    decodedPwd = PasswordUtil.decode(rc.getUserPwd());
                } catch (InvalidPasswordDecodingException ipde) {
                    decodedPwd = rc.getUserPwd();
                    results.setWarning(CmdUtils.getMessage("MSG_PASSWORD_NOT_ENCODED", rc.getId()));
                    repoConnResults.put(rc.getId(), results);
                } catch (UnsupportedCryptoAlgorithmException ucae) {
                    results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                    results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PWD_CRYPTO_UNSUPPORTED"));
                    repoConnResults.put(rc.getId(), results);
                    return ReturnCode.CONNECTION_FAILED;
                }
            }
            lie = new RestRepositoryConnection(rc.getUser(), decodedPwd, rc.getApiKey(), rc.getUrl().toString());
        }
        if (lie != null) {
            if (proxyStatus != null) {
                //If the specified proxy info is invalid, display error message
                results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                results.setTestReason(proxyStatus);
                repoConnResults.put(rc.getId(), results);
                return ReturnCode.CONNECTION_FAILED;
            }
            if (proxy != null) {
                lie.setProxy(proxy);
            }
            //Check repository connection
            try {
                lie.checkRepositoryStatus();
                results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_SUCCESS"));
                repoConnResults.put(rc.getId(), results);
            } catch (IOException ioe) {
                results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                if (ioe.getClass().getName().contains("UnknownHostException") && proxy != null)
                    results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_UNKNOWN_PROXY_HOST", proxy.getProxyURL().getHost()));
                else if ((ioe.getClass().getName().contains("ConnectException") && ioe.getMessage() != null && ioe.getMessage().contains("Connection refused")) && proxy != null)
                    results.setTestReason(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_INCORRECT_PROXY_PORT", String.valueOf(proxy.getProxyURL().getPort())));
                else if (ExceptionUtils.isCertPathBuilderException(ioe.getCause()))
                    results.setTestReason(CmdUtils.getMessage("ERROR_FAILED_TO_CONNECT_REPO_CAUSED_BY_CERT"));
                else
                    results.setTestReason(CmdUtils.getMessage("ERROR_FAILED_TO_CONNECT_REPO"));
                repoConnResults.put(rc.getId(), results);
                return ReturnCode.CONNECTION_FAILED;
            } catch (RequestFailureException rfe) {
                if ((rfe.getResponseCode() == CmdlineConstants.HTTP_AUTH_RESPONSE_CODE) || (rfe.getResponseCode() == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE)) {
                    repoAuthenticationList.add(rc);
                } else {
                    results.setTestStatus(CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"));
                    results.setTestReason(CmdUtils.getMessage("ERROR_FAILED_TO_CONNECT_REPO", RepositoryConfigUtils.getRepoPropertiesFileLocation()));
                    repoConnResults.put(rc.getId(), results);
                    return ReturnCode.CONNECTION_FAILED;
                }
            }
        }
        return ReturnCode.OK;
    }

    private ReturnCode testRepositories() throws InstallException {
        List<RepositoryConfig> repositoryConfigs = null;
        String proxyStatus = null;
        try {
            repositoryConfigs = RepositoryConfigUtils.getRepositoryConfigs(repoProperties);
        } catch (InstallException e) {
            System.out.println(e.getMessage());
            return ReturnCode.RUNTIME_EXCEPTION;
        }
        RestRepositoryConnectionProxy proxy = null;
        try {
            proxy = RepositoryConfigUtils.getProxyInfo(repoProperties);
        } catch (InstallException e) {
            proxyStatus = e.getMessage();
        }
        repoAuthenticationList = Collections.synchronizedList(new ArrayList<RepositoryConfig>(repositoryConfigs.size()));
        if (repoId == null) {
            ExecutorService executor = Executors.newFixedThreadPool(repositoryConfigs.size());
            System.out.println(CmdUtils.getMessage("MSG_TESTING_ALL"));
            try {
                for (RepositoryConfig rc : repositoryConfigs) {
                    repoMap.put(rc.getId(), rc);
                    if (rc.isLibertyRepository() && !RepositoryConfigUtils.isWlpRepoEnabled(repoProperties)) {
                        insertRepoConnResultsEntry(rc, CmdUtils.getMessage("MSG_CONNECT_NOT_TESTED"),
                                                   CmdUtils.getMessage("MSG_DEFAULT_NOT_ENABLED", RepositoryConfigUtils.getRepoPropertiesFileLocation()), null);
                        continue;
                    }
                    Runnable worker = new TestConnectionActionRunnable(rc, proxy, proxyStatus);
                    executor.execute(worker);
                }
                executor.shutdown();
                // Wait until all threads are finished
                while (!executor.isTerminated());
            } catch (Exception e) {
                throw new InstallException(CmdUtils.getMessage("ERROR_NO_CONNECTION"), e, InstallException.RUNTIME_EXCEPTION);
            }
        } else {
            for (RepositoryConfig rc : repositoryConfigs) {
                repoMap.put(rc.getId(), rc);
                if (rc.getId().equalsIgnoreCase(repoId)) {
                    System.out.println(CmdUtils.getMessage("MSG_TESTING", rc.getId()));
                    if (rc.isLibertyRepository() && !RepositoryConfigUtils.isWlpRepoEnabled(repoProperties)) {
                        insertRepoConnResultsEntry(rc, CmdUtils.getMessage("MSG_CONNECT_NOT_TESTED"),
                                                   CmdUtils.getMessage("MSG_DEFAULT_NOT_ENABLED", RepositoryConfigUtils.getRepoPropertiesFileLocation()), null);
                        break;
                    }
                    returnCode = testRepository(rc, proxy, proxyStatus);
                }
            }
        }
        //Check for authentication prompt and display results
        if ((repoConnResults != null && !repoConnResults.isEmpty()) || (repoAuthenticationList != null && !repoAuthenticationList.isEmpty())) {
            if (repoAuthenticationList != null && !repoAuthenticationList.isEmpty()) {
                int promptCode = CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE;
                List<RepositoryConfig> orderedList = RepositoryConfigUtils.getRepositoryConfigs(repoProperties);
                for (RepositoryConfig rc : orderedList) {
                    for (RepositoryConfig authList : repoAuthenticationList) {
                        if (authList.getId().equalsIgnoreCase(rc.getId())) {
                            if (promptCode == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
                                insertRepoConnResultsEntry(rc, CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"),
                                                           CmdUtils.getMessage("ERROR_TOOL_INCORRECT_PROXY_CREDENTIALS", RepositoryConfigUtils.getRepoPropertiesFileLocation()),
                                                           null);
                                break;
                            }
                            //prompt user for credentials
                            promptCode = promptUser(rc, proxy);
                            if (promptCode == CmdlineConstants.USER_ABORT_ACTION) {
                                return ReturnCode.USER_ABORT;
                            }
                            if (promptCode != CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE) {
                                returnCode = ReturnCode.CONNECTION_FAILED;
                            }
                            break;
                        }
                    }
                }
            }
            showConnectionResults();
            return returnCode;
        }
        System.out.println(CmdUtils.getMessage("ERROR_REPO_NOT_IN_LIST", repoId, RepositoryConfigUtils.getRepoPropertiesFileLocation()));
        return ReturnCode.REPOSITORY_NOT_FOUND;
    }

    private void showConnectionResults() throws InstallException {
        if (repoId == null) {
            System.out.println(CmdUtils.getMessage("MSG_CONFIG_REPO_LABEL"));
            System.out.println(CmdlineConstants.DASHES);
        }
        List<String> orderedList = RepositoryConfigUtils.getOrderList(repoProperties);
        if (!orderedList.contains(RepositoryConfigUtils.WLP_REPO))
            orderedList.add(RepositoryConfigUtils.WLP_REPO);
        for (String repoList : orderedList) {
            for (Entry<String, TestConnectionResults> connectionResults : repoConnResults.entrySet()) {
                if (connectionResults.getKey().equalsIgnoreCase(repoList)) {
                    showRepoHeader(repoMap.get(connectionResults.getKey()), repoConnResults.get(connectionResults.getKey()));
                    String testReason = connectionResults.getValue().getTestReason();
                    String testWarning = connectionResults.getValue().getWarning();
                    if (testReason != null && !testReason.isEmpty()) {
                        String failReason = CmdUtils.getMessage("FIELD_REPO_REASON", testReason);
                        StringBuffer sb = new StringBuffer();
                        BufferedReader reader = new BufferedReader(new StringReader(failReason));
                        try {
                            for (String line; (line = reader.readLine()) != null;) {
                                InstallUtils.wordWrap(sb, line, "    ");
                            }
                            System.out.print(sb.toString());
                        } catch (IOException e) {
                            System.out.println(failReason);
                        }
                    }
                    if (testWarning != null && !testWarning.isEmpty()) {
                        String warning = CmdUtils.getMessage("FIELD_REPO_WARNING", testWarning);
                        StringBuffer sb = new StringBuffer();
                        BufferedReader reader = new BufferedReader(new StringReader(warning));
                        try {
                            for (String line; (line = reader.readLine()) != null;) {
                                InstallUtils.wordWrap(sb, line, "    ");
                            }
                            System.out.print(sb.toString() + (InstallUtils.NEWLINE));
                        } catch (IOException e) {
                            System.out.println(warning);
                        }
                    } else {
                        System.out.print(InstallUtils.NEWLINE);
                    }
                    break;
                }
            }
        }
    }

    private void showRepoHeader(RepositoryConfig repo, TestConnectionResults connResults) {
        StringBuffer sb = new StringBuffer();
        sb.append(CmdUtils.getMessage("FIELD_REPO_NAME", repo.isLibertyRepository() ? CmdUtils.getMessage("MSG_DEFAULT_REPO_LABEL") : repo.getId())).append(InstallUtils.NEWLINE);
        if (repo.isLibertyRepository()) {
            sb.append(CmdUtils.getMessage("FIELD_REPO_STATUS", connResults.getTestStatus())).append(InstallUtils.NEWLINE);
            System.out.print(sb.toString());
            return;
        }
        sb.append(CmdUtils.getMessage("FIELD_REPO_LOCATION", repo.getUrl())).append(InstallUtils.NEWLINE);
        sb.append(CmdUtils.getMessage("FIELD_REPO_STATUS", connResults.getTestStatus())).append(InstallUtils.NEWLINE);
        System.out.print(sb.toString());
    }

    private int promptUser(RepositoryConfig rc, RestRepositoryConnectionProxy proxy) throws InstallException {
        RestRepositoryConnection lie = null;
        int connectionStatus;
        if (rc.isLibertyRepository()) {
            connectionStatus = CmdUtils.promptProxyDefaultRepo(proxy, repoProperties, "testConnection");
        } else {
            lie = new RestRepositoryConnection(rc.getUser(), rc.getUserPwd(), rc.getApiKey(), rc.getUrl().toString());
            lie.setProxy(proxy);
            connectionStatus = CmdUtils.credentialsPrompt(lie, repoProperties, "testConnection");
        }
        if (connectionStatus == CmdlineConstants.USER_ABORT_ACTION) {
            return connectionStatus;
        }
        if (connectionStatus == CmdlineConstants.HTTP_SUCCESS_RESPONSE_CODE) {
            insertRepoConnResultsEntry(rc, CmdUtils.getMessage("MSG_CONNECT_REPO_SUCCESS"), null, null);
        } else if (connectionStatus == InstallConstants.PROXY_AUTH_HTTP_RESPONSE_CODE) {
            insertRepoConnResultsEntry(rc, CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"), CmdUtils.getMessage("ERROR_TOOL_INCORRECT_PROXY_CREDENTIALS",
                                                                                                               RepositoryConfigUtils.getRepoPropertiesFileLocation()),
                                       null);
        } else if (connectionStatus == CmdlineConstants.HTTP_AUTH_RESPONSE_CODE) {
            insertRepoConnResultsEntry(rc, CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"), CmdUtils.getMessage("ERROR_REPO_REQUIRES_AUTH",
                                                                                                               RepositoryConfigUtils.getRepoPropertiesFileLocation()),
                                       null);
        }

        else if (connectionStatus == CmdlineConstants.WRONG_JDK) {
            insertRepoConnResultsEntry(rc,
                                       CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"),
                                       rc.isLibertyRepository() ? Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT_WRONG_JDK") : CmdUtils.getMessage("ERROR_FAILED_TO_CONNECT_REPO",
                                                                                                                                                                            RepositoryConfigUtils.getRepoPropertiesFileLocation()),
                                       null);
        }

        else {
            insertRepoConnResultsEntry(rc,
                                       CmdUtils.getMessage("MSG_CONNECT_REPO_FAILED"),
                                       rc.isLibertyRepository() ? Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_FAILED_TO_CONNECT") : CmdUtils.getMessage("ERROR_FAILED_TO_CONNECT_REPO",
                                                                                                                                                                  RepositoryConfigUtils.getRepoPropertiesFileLocation()),
                                       null);
        }
        return connectionStatus;
    }

    private void insertRepoConnResultsEntry(RepositoryConfig rc, String testStatus, String failReason, String warning) {

        if (repoConnResults.get(rc.getId()) == null) {
            repoConnResults.put(rc.getId(), new TestConnectionResults(testStatus, failReason, warning));
        } else {
            TestConnectionResults result = repoConnResults.get(rc.getId());
            if (testStatus != null)
                result.setTestStatus(testStatus);
            if (failReason != null)
                result.setTestReason(failReason);
            if (warning != null)
                result.setWarning(warning);
            repoConnResults.put(rc.getId(), result);
        }
    }

    /**
     * Stores the connection results for each repository tested
     */
    private static class TestConnectionResults {
        private String testStatus;
        private String failReason;
        private String warning;

        private TestConnectionResults(String status, String reason, String warning) {
            this.testStatus = status;
            this.failReason = reason;
            this.warning = warning;
        }

        private void setTestStatus(String status) {
            this.testStatus = status;
        }

        private void setTestReason(String reason) {
            this.failReason = reason;
        }

        private void setWarning(String warning) {
            this.warning = warning;
        }

        private String getTestStatus() {
            return this.testStatus;
        }

        private String getTestReason() {
            return this.failReason;
        }

        private String getWarning() {
            return this.warning;
        }
    }

    /**
     * Runnable class to spawn new threads for each repository test connection
     */
    private static class TestConnectionActionRunnable implements Runnable {
        private final RepositoryConfig repositoryConfig;
        private final RestRepositoryConnectionProxy proxy;
        private final String proxyStatus;

        TestConnectionActionRunnable(RepositoryConfig rc, RestRepositoryConnectionProxy proxy, String proxyStatus) {
            this.repositoryConfig = rc;
            this.proxy = proxy;
            this.proxyStatus = proxyStatus;
        }

        /**
         * Test connection to repo
         */
        @Override
        public void run() {
            //Test the connection to the repository for each spawned thread for repoID
            ReturnCode rc = TestConnectionAction.testRepository(repositoryConfig, proxy, proxyStatus);

            //Check and store the return code for each test thread, if connection failed
            if (!rc.equals(ReturnCode.OK)) {
                TestConnectionAction.returnCode = rc;
            }
        }
    }
}

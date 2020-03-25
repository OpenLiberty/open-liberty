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
package com.ibm.ws.install.featureUtility.cli;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ibm.websphere.crypto.InvalidPasswordDecodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.RepositoryConfigUtils;
import com.ibm.ws.install.RepositoryConfigValidationResult;
import com.ibm.ws.install.featureUtility.props.FeatureUtilityPropertiesUtils;
import com.ibm.ws.install.featureUtility.props.FeatureUtilityRepoProperties;
import com.ibm.ws.install.featureUtility.props.MavenRepository;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.cmdline.InstallExecutor;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;

/**
 * This API is used to execute the View Settings Action.
 */
public class ViewSettingsAction implements ActionHandler {

    private Properties repoProperties;
    private File repoPropertiesFile;
    private List<String> orderList;
    private final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
    private List<RepositoryConfigValidationResult> validationResults;
    private boolean isViewValidationMessages;

    ReturnCode initialize(Arguments args) {
        isViewValidationMessages = args.getOption("viewvalidationmessages") != null;
        return ReturnCode.OK;
    }

    /**
     * {@inheritDoc}
     * This executs the View Settings action
     *
     * @return - return code depending on success of task
     */
    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        ReturnCode rc = initialize(args);
        if (!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        // Initialize the repository properties
//        repoPropertiesFile = new File(RepositoryConfigUtils.getRepoPropertiesFileLocation());
//        if (repoPropertiesFile.exists()) {
        if(FeatureUtilityRepoProperties.didLoadProperties()){
            // Load the repository properties instance from properties file
            try {
//                repoProperties = RepositoryConfigUtils.loadRepoProperties();
//                orderList = RepositoryConfigUtils.getOrderList(repoProperties);
                showSettings();
                if (validationResults.size() > 0)
//                    return ReturnCode.REPO_PROPS_VALIDATION_FAILED;
                    return ReturnCode.BAD_ARGUMENT;
            } catch (InstallException e) {
                logger.log(Level.SEVERE, e.getMessage());
                return InstallExecutor.returnCode(e.getRc());
            }
        } else {
            showSettingsTemplate();
        }
        return ReturnCode.OK;
    }

    private void showHeader() throws InstallException {
        StringBuffer sb = new StringBuffer();
        String settingsPath = FeatureUtilityRepoProperties.getRepoPropertiesFile().getPath();
        sb.append(InstallUtils.NEWLINE);
        sb.append(FeatureUtilityPropertiesUtils.getMessage("MSG_FEATUREUTILITY_SETTTINGS")).append(InstallUtils.NEWLINE);
        sb.append(FeatureUtilityPropertiesUtils.CmdlineConstants.DASHES).append(InstallUtils.NEWLINE);

        sb.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_PROPS_FILE", settingsPath));
        sb.append(InstallUtils.NEWLINE);

        String defaultRepoUseage = FeatureUtilityRepoProperties.isUsingDefaultRepo() ? FeatureUtilityPropertiesUtils.getMessage("MSG_TRUE") : FeatureUtilityPropertiesUtils.getMessage("MSG_FALSE");
        sb.append(FeatureUtilityPropertiesUtils.getMessage("MSG_DEFAULT_REPO_NAME_LABEL") + " " + FeatureUtilityPropertiesUtils.getMessage("MSG_DEFAULT_REPO_NAME")).append(InstallUtils.NEWLINE);
        sb.append(FeatureUtilityPropertiesUtils.getMessage("MSG_DEFAULT_REPO_USEAGE_LABEL") + " " + defaultRepoUseage).append(InstallUtils.NEWLINE);

        System.out.println(sb.toString());

    }

    private void showRepositories() throws InstallException {
        StringBuffer sb = new StringBuffer();
        for (MavenRepository mvnRepository : FeatureUtilityRepoProperties.getMirrorRepositories()) {
            sb.append(mvnRepository.getRepositoryUrl());
//            if (!r.equalsIgnoreCase(RepositoryConfigUtils.WLP_REPO)) {
//                String urlKey = r + RepositoryConfigUtils.URL_SUFFIX;
//                String userKey = r + RepositoryConfigUtils.USER_SUFFIX;
//                String pwdKey = r + RepositoryConfigUtils.PWD_SUFFIX;
//                String userPwdKey = r + RepositoryConfigUtils.USERPWD_SUFFIX;
//                String url = repoProperties.getProperty(urlKey);
//                String user = repoProperties.getProperty(userKey);
//                String pass = repoProperties.getProperty(pwdKey);
//                if (pass == null || pass.isEmpty()) {
//                    pass = repoProperties.getProperty(userPwdKey);
//                }
//                url = (url == null || url.isEmpty()) ? CmdUtils.getMessage("MSG_UNSPECIFIED") : url;
//                user = (user == null || user.isEmpty()) ? CmdUtils.getMessage("MSG_UNSPECIFIED") : user;
//                String warningDetail = null;
//                if (pass == null || pass.isEmpty()) {
//                    pass = CmdUtils.getMessage("MSG_UNSPECIFIED");
//                } else {
//                    try {
//                        PasswordUtil.decode(pass);
//                        pass = CmdUtils.CmdlineConstants.HIDDEN_PASSWORD;
//                    } catch (InvalidPasswordDecodingException ipde) {
//                        warningDetail = CmdUtils.getMessage("MSG_PASSWORD_NOT_ENCODED");
//                        pass = CmdUtils.CmdlineConstants.HIDDEN_PASSWORD;
//                    } catch (UnsupportedCryptoAlgorithmException ucae) {
//                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PWD_CRYPTO_UNSUPPORTED"), ucae, InstallException.CONNECTION_FAILED);
//                    }
//                }
//
//                sb.append(CmdUtils.getMessage("FIELD_NAME") + " " + r).append(InstallUtils.NEWLINE);
//                sb.append(CmdUtils.getMessage("FIELD_LOCATION") + " " + url).append(InstallUtils.NEWLINE);
//                if (CmdUtils.isFileProtocol(url)) {
//                    sb.append(CmdUtils.getMessage("FIELD_USER") + " " + CmdUtils.getMessage("MSG_INAPPLICABLE")).append(InstallUtils.NEWLINE);
//                    sb.append(CmdUtils.getMessage("FIELD_PASS") + " " + CmdUtils.getMessage("MSG_INAPPLICABLE")).append(InstallUtils.NEWLINE);
//                } else {
//                    sb.append(CmdUtils.getMessage("FIELD_USER") + " " + user).append(InstallUtils.NEWLINE);
//                    sb.append(CmdUtils.getMessage("FIELD_PASS") + " " + pass).append(InstallUtils.NEWLINE);
//                    if (warningDetail != null) {
//                        sb.append(CmdUtils.getMessage("FIELD_REPO_WARNING", warningDetail)).append(InstallUtils.NEWLINE);
//                    }
//                }
            sb.append(InstallUtils.NEWLINE);
        }

        System.out.println(FeatureUtilityPropertiesUtils.getMessage("MSG_CONFIG_REPO_LABEL"));
        System.out.println(FeatureUtilityPropertiesUtils.CmdlineConstants.DASHES);
        if (sb.length() > 0) {
            System.out.print(sb.toString());
        } else {
            System.out.println(FeatureUtilityPropertiesUtils.getMessage("MSG_NO_CONFIG_REPO"));
            System.out.println();
        }
    }

    private void showProxyInfo() throws InstallException {
        StringBuffer proxInfo = new StringBuffer();
        String pHost = null;
        if (FeatureUtilityRepoProperties.didLoadProperties()) {
            pHost = FeatureUtilityRepoProperties.getProxyHost();
            if (pHost != null && !pHost.isEmpty()) {
                proxInfo.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_PROXY_SERVER") + " " + pHost).append(InstallUtils.NEWLINE);
            }

            String pPort = FeatureUtilityRepoProperties.getProxyPort();
            pPort = (pPort == null || pPort.isEmpty()) ? FeatureUtilityPropertiesUtils.getMessage("MSG_UNSPECIFIED") : pPort;
            proxInfo.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_PORT") + " " + pPort).append(InstallUtils.NEWLINE);

            String pUser = FeatureUtilityRepoProperties.getProxyUser();
            pUser = (pUser == null || pUser.isEmpty()) ? FeatureUtilityPropertiesUtils.getMessage("MSG_UNSPECIFIED") : pUser;
            proxInfo.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_USER") + " " + pUser).append(InstallUtils.NEWLINE);

//            String pUserPwd = repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_USERPWD) != null ? repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_USERPWD) : repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_PWD) != null ? repoProperties.getProperty(InstallConstants.REPO_PROPERTIES_PROXY_PWD) : null;
            String pUserPwd = FeatureUtilityRepoProperties.getProxyPassword();
            String warning = null;
            if (pUserPwd == null || pUserPwd.isEmpty())
                pUserPwd = FeatureUtilityPropertiesUtils.getMessage("MSG_UNSPECIFIED");
            else {
                pUserPwd = FeatureUtilityPropertiesUtils.CmdlineConstants.HIDDEN_PASSWORD;
                try {
                    //Decode encrypted proxy server password
                    PasswordUtil.decode(pUserPwd);
                    proxInfo.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_PASS") + " " + pUserPwd).append(InstallUtils.NEWLINE);
                    //Check proxy server credentials for Authentication
                } catch (InvalidPasswordDecodingException ipde) {
                    String warningMessage = FeatureUtilityPropertiesUtils.getMessage("MSG_PASSWORD_NOT_ENCODED_PROXY");
                    warning = FeatureUtilityPropertiesUtils.getMessage("FIELD_REPO_WARNING", warningMessage);
                } catch (UnsupportedCryptoAlgorithmException ucae) {
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_PROXY_PWD_CRYPTO_UNSUPPORTED"), ucae, InstallException.RUNTIME_EXCEPTION);
                }
            }
            proxInfo.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_PASS") + " " + pUserPwd).append(InstallUtils.NEWLINE);
            if (warning != null)
                proxInfo.append(warning).append(InstallUtils.NEWLINE);
        }

        System.out.println(FeatureUtilityPropertiesUtils.getMessage("MSG_PROXY_LABEL"));
        System.out.println(FeatureUtilityPropertiesUtils.CmdlineConstants.DASHES);

        // Only show proxy info if host is specified
        if (pHost != null) {
            System.out.print(proxInfo.toString());
            System.out.println();
        } else {
            System.out.print(FeatureUtilityPropertiesUtils.getMessage("MSG_NO_CONFIG_PROXY"));
            System.out.println();
        }
    }

    private void showSettingsTemplate() {
        StringBuffer sb = new StringBuffer();
        InstallUtils.wordWrap(sb, FeatureUtilityPropertiesUtils.getMessage("MSG_PROPERTIES_FILE_NOT_FOUND"), "");
        sb.append(InstallUtils.NEWLINE);
        InstallUtils.wordWrap(sb, FeatureUtilityPropertiesUtils.getMessage("MSG_PROPERTIES_FILE_NOT_FOUND_EXPLANATION", FeatureUtilityRepoProperties.getRepoPropertiesFileLocation()), "");
        sb.append(InstallUtils.NEWLINE);
        InstallUtils.wordWrap(sb, FeatureUtilityPropertiesUtils.getMessage("MSG_PROPERTIES_FILE_NOT_FOUND_ACTION"), "");
        System.out.print(sb.toString());

        System.out.println(FeatureUtilityPropertiesUtils.CmdlineConstants.DASHES);
        System.out.println(FeatureUtilityPropertiesUtils.getSampleConfig());
    }

    private void showValidationResults() throws InstallException {
        StringBuffer sb = new StringBuffer();
        sb.append(FeatureUtilityPropertiesUtils.getMessage("MSG_VALIDATION_MESSAGES")).append(InstallUtils.NEWLINE);;
        sb.append(FeatureUtilityPropertiesUtils.CmdlineConstants.DASHES).append(InstallUtils.NEWLINE);
        validationResults = FeatureUtilityPropertiesUtils.validateRepositoryPropertiesFile(repoProperties);
        if (validationResults.size() == 0)
            sb.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_VALIDATION_RESULTS", FeatureUtilityPropertiesUtils.getMessage("MSG_VALIDATION_SUCCESSFUL"))).append(InstallUtils.NEWLINE);
        else {
            if (!isViewValidationMessages)
                sb.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_VALIDATION_RESULTS", FeatureUtilityPropertiesUtils.getMessage("MSG_VALIDATION_FAILED_NO_MESSAGES"))).append(InstallUtils.NEWLINE);
            else {
                sb.append(FeatureUtilityPropertiesUtils.getMessage("MSG_VALIDATION_NUM_OF_ERRORS", validationResults.size())).append(InstallUtils.NEWLINE);
                sb.append(InstallUtils.NEWLINE);
                for (RepositoryConfigValidationResult vr : validationResults) {
                    sb.append(FeatureUtilityPropertiesUtils.getMessage("FIELD_VALIDATION_LINE", vr.getLineNum())).append(InstallUtils.NEWLINE);
                    sb.append(FeatureUtilityPropertiesUtils.getMessage("MSG_VALIDATION_MESSAGE_FORMAT", vr.getFailedReason(), vr.getValidationMessage())).append(InstallUtils.NEWLINE);
                    sb.append(InstallUtils.NEWLINE);
                }
            }
        }
        System.out.println(sb.toString());

    }

    private void showSettings() throws InstallException {
        showHeader();
        showValidationResults();
        showRepositories();
        showProxyInfo();

    }
}

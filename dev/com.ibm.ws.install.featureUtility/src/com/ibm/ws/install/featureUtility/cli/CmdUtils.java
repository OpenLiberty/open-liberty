package com.ibm.ws.install.featureUtility.cli;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class CmdUtils {
//    private static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
    private static Locale locale;
    private static ResourceBundle featureUtilityMessages;
    private static ResourceBundle featureUtilitySampleConfigurations;

    public class CmdlineConstants {
        public static final String DASHES = "----------------------------------------------------------------------";
        public static final String HIDDEN_PASSWORD = "********";
        public static final int HTTP_AUTH_RESPONSE_CODE = 401;
        public static final int HTTP_SUCCESS_RESPONSE_CODE = 200;
        public static final int CONNECTION_REFUSED = 500;
        public static final int USER_ABORT_ACTION = -1;
        public static final int UNTRUSTED_CERTIFICATE = -2;
        public static final int WRONG_JDK = -3;
    }

    public static synchronized String getMessage(String key, Object... args) {
        if (featureUtilityMessages == null) {
            if (locale == null)
                locale = Locale.getDefault();
            featureUtilityMessages = ResourceBundle.getBundle("com.ibm.ws.install.featureUtility.internal.resources.FeatureUtilityMessages", locale);
        }
        String message = featureUtilityMessages.getString(key);
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
        if (featureUtilitySampleConfigurations == null) {
            if (locale == null)
                locale = Locale.getDefault();
            featureUtilitySampleConfigurations = ResourceBundle.getBundle("com.ibm.ws.install.featureUtility.internal.resources.FeatureUtilitySampleConfiguration", locale);
        }
        String message = featureUtilitySampleConfigurations.getString("SAMPLE_CONFIG");
        // ??? below stuff
//        File path = new File(InstallUtils.isWindows ? "C:\\IBM\\LibertyRepository" : "/usr/LibertyRepository");
//        String url = null;
//        try {
//            url = path.toURI().toURL().toString();
//        } catch (MalformedURLException e) {
//            url = InstallUtils.isWindows ? "file:/C:/IBM/LibertyRepository" : "file:///usr/LibertyRepository";
//        }
//        String featureRepo = null;
//        try {
//            ProductInfo product = ProductInfo.getAllProductInfo().get("com.ibm.websphere.appserver");
//            featureRepo = product == null ? "wlp-featureRepo.zip" : "wlp-featureRepo-" + product.getVersion() + ".zip";
//        } catch (Exception e) {
//            featureRepo = "wlp-featureRepo.zip";
//        }
//        File zipRepoPath = new File(InstallUtils.isWindows ? "C:\\IBM" : "/usr", featureRepo);
//        MessageFormat messageFormat = new MessageFormat(message, locale);
        return message;
    }

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


}



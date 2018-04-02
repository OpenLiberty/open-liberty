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
package com.ibm.ws.install.internal;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.repository.connections.DirectoryRepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.ZipRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;

public class InstallLogUtils {
    static class InstallKernelConsoleHandler extends Handler {
        private final boolean verbose;

        public InstallKernelConsoleHandler() {
            this(false);
        }

        public InstallKernelConsoleHandler(boolean verbose) {
            super();
            this.verbose = verbose;
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (this.isLoggable(record)) {
                logToOutStream(record);
            }
        }

        private void logToOutStream(LogRecord record) {
            String strDate = verbose ? DataFormatHelper.formatCurrentTime() : "";

            if (null != record.getMessage())
                System.out.println(strDate + getFormatter().formatMessage(record));

            System.out.flush();
        }

        @Override
        public boolean isLoggable(LogRecord record) {
            return super.isLoggable(record) && !record.getLevel().equals(Level.SEVERE);
        }

        @Override
        public void close() {
            flush();
        }

        @Override
        public void flush() {
            System.out.flush();
        }
    } // InstallKernelConsoleHandler ===========================================================

    static class InstallKernelErrorConsoleHandler extends Handler {
        private final boolean verbose;

        public InstallKernelErrorConsoleHandler() {
            this(false);
        }

        public InstallKernelErrorConsoleHandler(boolean verbose) {
            super();
            this.verbose = verbose;
            super.setLevel(Level.SEVERE);
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (this.isLoggable(record)) {
                logToErrStream(record);
            }
        }

        /**
         * Prints the error message to the std err along with any exception message.
         * If the handler's level is set to FINEST then will also print out the stack trace
         *
         * @param record The log record to print
         */
        private void logToErrStream(LogRecord record) {
            String strDate = verbose ? DataFormatHelper.formatCurrentTime() : "";

            if (null != record.getMessage())
                System.err.println(strDate + getFormatter().formatMessage(record));

            Throwable t = record.getThrown();

            if (null != t) {
                if (verbose) {
                    String failingConnection = getFailingConnection(t);
                    if (failingConnection != null)
                        System.err.println(strDate + "The following exception was related to the repository " + failingConnection);
                    t.printStackTrace(System.err);
                }
            }

            System.err.flush();
        }

        private String getFailingConnection(Throwable t) {
            RepositoryBackendException rbe = null;
            if (t instanceof RepositoryBackendException) {
                rbe = (RepositoryBackendException) t;
            } else {
                Throwable c = t.getCause();
                if (c != null && c instanceof RepositoryBackendException) {
                    rbe = (RepositoryBackendException) c;
                }
            }
            if (rbe != null) {
                RepositoryConnection fc = rbe.getFailingConnection();
                if (fc != null)
                    return fc.getRepositoryLocation();
            }
            return null;
        }

        @Override
        public void close() {
            flush();
        }

        @Override
        public void flush() {
            System.err.flush();
        }

        /**
         * The log level for this handler will always be set to SEVERE and
         * cannot be overridden.
         */
        @Override
        public synchronized void setLevel(Level newLevel) throws SecurityException {
            // Do not allow the level to be overridden
            return;
        }

    } // InstallKernelErrorConsoleHandler =======================================================

    public static enum Messages {
        INSTALL_KERNEL_MESSAGES("com.ibm.ws.install.internal.resources.InstallKernel"),
        UTILITY_MESSAGES("com.ibm.ws.product.utility.resources.UtilityMessages"),
        PROVISIONER_MESSAGES("com.ibm.ws.kernel.feature.internal.resources.ProvisionerMessages"),
        SELF_EXTRACTOR_MESSAGES("wlp.lib.extract.SelfExtractMessages");

        private static final String explanationTag = ".explanation";
        private static final String useractionTag = ".useraction";

        private static Locale locale = Locale.getDefault();
        private final String resourceBunbleName;
        private ResourceBundle messages;

        Messages(String name) {
            resourceBunbleName = name;
            messages = null;
        }

        public String getMessage(String key, Object... args) {
            initResourceBundle();
            String message = messages.getString(key);

            if (args.length > 0) {
                message = new MessageFormat(message, locale).format(args);
            }

            return message;
        }

        public String getLogMessage(String key, Object... args) {
            StringBuilder logMessage = new StringBuilder(getMessage(key, args));

            if (messages.containsKey(key.concat(explanationTag))) {
                logMessage.append(InstallUtils.NEWLINE);
                logMessage.append(messages.getString(key.concat(explanationTag)));
            }

            if (messages.containsKey(key.concat(useractionTag))) {
                logMessage.append(InstallUtils.NEWLINE);
                logMessage.append(messages.getString(key.concat(useractionTag)));
            }

            return logMessage.toString();
        }

        private void initResourceBundle() {
            if (messages == null) {
                messages = ResourceBundle.getBundle(resourceBunbleName, locale);
            } else if (!locale.equals(messages.getLocale())) {
                messages = ResourceBundle.getBundle(resourceBunbleName, locale);
            }
        }

        public static void setLocale(Locale locale) {
            if (locale != null && !Messages.locale.equals(locale)) {
                Messages.locale = locale;
            }
        }
    } // Messages =======================================================

    public static Logger getInstallLogger() {
        return Logger.getLogger(InstallConstants.LOGGER_NAME);
    }

    public static void enableConsoleErrorLogging(boolean verbose) {
        Logger logger = getInstallLogger();
        Handler[] handlers = logger.getHandlers();

        for (Handler handler : handlers) {
            if (handler instanceof InstallKernelErrorConsoleHandler) {
                return;
            }
        }

        Handler ch = new InstallKernelErrorConsoleHandler(verbose);
        ch.setFormatter(new SimpleFormatter());
        getInstallLogger().addHandler(ch);
    }

    public static void enableConsoleLogging(Level logLevel, boolean verbose) {
        Logger logger = getInstallLogger();
        Handler[] handlers = logger.getHandlers();

        for (Handler handler : handlers) {
            if (handler instanceof InstallKernelConsoleHandler
                && !handler.getLevel().equals(logLevel)) {
                logger.removeHandler(handler);
                break;
            }
        }

        Handler ch = new InstallKernelConsoleHandler(verbose);
        ch.setLevel(logLevel);
        ch.setFormatter(new SimpleFormatter());
        getInstallLogger().addHandler(ch);
    }

    public static void fixLogger(PrintWriter log, String fixID, String message) {
        Date d = new Date();
        String dateString = InstallUtils.getDateFormat().format(d);
        log.println("[" + dateString + "] " + fixID + " " + message);
        log.flush();
    }

    public static void logLoginInfo(RepositoryConnectionList logininfo, String logLable) {
        Logger logger = getInstallLogger();

        if (null == logLable) {
            logLable = "";
        } else if (!logLable.isEmpty()) {
            logLable += "   ";
        }

        for (RepositoryConnection c : logininfo) {
            if (c instanceof RestRepositoryConnection) {
                RestRepositoryConnection l = (RestRepositoryConnection) c;
                String repURL = l.getRepositoryUrl();
                if (null != repURL && !repURL.isEmpty()) {
                    logger.log(Level.FINEST, logLable + "Repository URL: " + repURL);
                }

                String apiKey = l.getApiKey();
                if (null != apiKey && !apiKey.isEmpty()) {
                    logger.log(Level.FINEST, logLable + "API Key: " + apiKey);
                }

                String userAgent = l.getUserAgent();
                if (null != userAgent && !userAgent.isEmpty()) {
                    logger.log(Level.FINEST, logLable + "User Agent: " + userAgent);
                }
            } else if (c instanceof DirectoryRepositoryConnection || c instanceof ZipRepositoryConnection) {
                logger.log(Level.FINEST, logLable + "Directory Repository: " + c.getRepositoryLocation());
            }
        }
    }
}

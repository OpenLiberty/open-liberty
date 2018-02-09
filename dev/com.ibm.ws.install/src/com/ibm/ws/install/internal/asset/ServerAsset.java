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
package com.ibm.ws.install.internal.asset;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.EmbeddedServerImpl;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.wsspi.kernel.embeddable.Server;
import com.ibm.wsspi.kernel.embeddable.ServerBuilder;

public class ServerAsset implements Comparable<ServerAsset> {
    private static final String TEMP_SERVER_NAME = "tempServer";

    private final Collection<String> requiredFeatures;
    private final File serverInfo; // server.xml
    private final String serverName;

    public ServerAsset(String serverName) throws InstallException, IOException {
        this(new File(InstallUtils.getServersDir(), ((null == serverName) ? "" : serverName + File.separator) + InstallUtils.SERVER_XML));

    }

    public ServerAsset(File serverXMLFile) throws InstallException, IOException {
        this(determineServerName(serverXMLFile), serverXMLFile);
    }

    public ServerAsset(String serverName, File serverXMLFile) {
        this.serverInfo = serverXMLFile;
        this.serverName = serverName;
        requiredFeatures = new HashSet<String>();
    }

    public File getServerDirectory() {
        return serverInfo.getParentFile();
    }

    private static String determineServerName(File serverXMLFile) throws InstallException, IOException {
        if (isExistingServer(serverXMLFile)) {
            return serverXMLFile.getParentFile().getCanonicalFile().getName();
        } else {
            return generateTempServerName();
        }
    }

    private static boolean isExistingServer(File serverXMLFile) throws IOException {
        if (null != serverXMLFile) {
            File path = serverXMLFile.getParentFile().getCanonicalFile();
            String name = path.getName();

            if (null != name && !InstallUtils.getServersDir().getCanonicalFile().equals(path)) {
                return InstallUtils.serverExists(name);
            }
        }

        return false;
    }

    // Random number for temp server name generation
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a server name that does not already exist in the Liberty's servers
     * directory.
     *
     * @return the name for the temporary server
     */
    private static String generateTempServerName() {
        String serverName = TEMP_SERVER_NAME;

        while (InstallUtils.serverExists(serverName)) {

            int randomNumber = random.nextInt();

            while (randomNumber == Integer.MIN_VALUE) {
                randomNumber = random.nextInt();
            }

            serverName = TEMP_SERVER_NAME + "_" + Integer.toString(Math.abs(randomNumber));
        }

        return serverName;
    }

    public Collection<String> getRequiredFeatures() throws InstallException, IOException {
        if (requiredFeatures.isEmpty()) {
            requiredFeatures.addAll(EmbeddedServerLauncher.getRequiredFeatures(serverInfo, serverName));
        }

        return Collections.unmodifiableCollection(requiredFeatures);
    }

    public String getServerName() {
        return serverName;
    }

    private static String getServerLogsDirectoryPath(File serverDir) {
        File dir = new File(serverDir, "logs");

        return dir.getAbsolutePath();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getServerName();
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(ServerAsset sa) {
        if (null == sa) {
            return 1;
        } else if (this.getServerName().equalsIgnoreCase(sa.getServerName())) {
            return 0;
        } else {
            return this.getServerName().compareTo(sa.getServerName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (null != o && o instanceof ServerAsset) {
            return this.getServerName().equalsIgnoreCase(((ServerAsset) o).getServerName());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.getServerName().hashCode();
    }

    private static class EmbeddedServerLauncher {

        public static Collection<String> getRequiredFeatures(File serverInfo, String serverName) throws InstallException, IOException {
            boolean isTempServer = !isExistingServer(serverInfo);
            InstallLogUtils.getInstallLogger().log(Level.FINEST,
                                                   serverName + (isTempServer ? " is " : " is not ") + " a temp server which serverInfo is " + serverInfo.getAbsolutePath());

            File serverXML = (isTempServer) ? createTempServer(serverInfo, serverName) : serverInfo;

            InstallLogUtils.getInstallLogger().log(Level.FINEST, "serverXML of " + serverName + " is " + serverXML.getAbsolutePath());

            try {
                Collection<String> features = launchServerInFeatureGatherMode(serverXML, serverName, isTempServer);

                // Results can be null if the stars didn't quite align to allow the feature gather mode to run.
                if (null == features) {
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("UNABLE_TO_DETERMINE_FEATURES",
                                                                                              serverName,
                                                                                              getServerLogsDirectoryPath(isTempServer ? serverXML.getParentFile() : Utils.getServerOutputDir(serverName))), InstallException.RUNTIME_EXCEPTION);
                }

                // If temp server was created then delete the root temp directory
                if (isTempServer) {
                    InstallLogUtils.getInstallLogger().log(Level.FINEST,
                                                           Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_DELETING_TEMP_SERVER_DIR",
                                                                                                          serverXML.getParent()));
                    InstallUtils.deleteDirectory(serverXML.getParentFile().getParentFile().getParentFile());
                }

                return features;
            } catch (InstallException inste) {
                // Do not delete the temp server if an error occurs
                if (isTempServer) {
                    InstallLogUtils.getInstallLogger().log(Level.INFO,
                                                           Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_NOT_DELETING_TEMP_SERVER_DIR",
                                                                                                          serverXML.getParent(),
                                                                                                          serverXML.getParentFile().getParent()));
                }
                throw inste;
            }
        }

        private static File createTempServer(File serverXML, String serverName) throws InstallException {
            File tmpdir = new File(AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty("java.io.tmpdir");
                }
            }));

            File tempWLPDir = new File(tmpdir, "tmpwlp");

            for (int i = 0; tempWLPDir.exists(); i++) {
                tempWLPDir = new File(tmpdir, "tmpwlp_" + i);
            }

            File tempServDir = new File(tempWLPDir, InstallUtils.SERVER_DIR_NAME + File.separator + serverName);

            try {
                if (!tempServDir.mkdirs()) {
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_CREATING_TEMP_SERVER_DIR",
                                                                                              tempServDir.getAbsolutePath(),
                                                                                              tmpdir.getAbsolutePath()), InstallException.IO_FAILURE);
                } else {
                    InstallLogUtils.getInstallLogger().log(Level.FINEST, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_CREATED_TEMP_SERVER_DIR",
                                                                                                                        tempServDir.getAbsolutePath()));
                }
            } catch (SecurityException se) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_CREATING_DIR", tempServDir.getAbsolutePath(),
                                                                                          se.getMessage()), se, InstallException.IO_FAILURE);
            }

            File destServerXML = new File(tempServDir, InstallUtils.SERVER_XML);

            try {
                InstallUtils.copyFile(serverXML, destServerXML);
            } catch (IOException ioe) {
                InstallUtils.deleteDirectory(tempWLPDir);

                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_COPYING_FILE",
                                                                                          serverXML.getAbsolutePath(),
                                                                                          tempServDir.getAbsolutePath()), ioe, InstallException.IO_FAILURE);
            }

            return destServerXML;
        }

        private static Collection<String> launchServerInFeatureGatherMode(File serverXML, String serverName, boolean isTempServer) throws InstallException {
            // Temporarily redirect the std out and err
            PrintStream stdOut = System.out;
            PrintStream stdErr = System.err;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos, true);
            System.setOut(ps);
            System.setErr(ps);

            try {
                Map<String, String> serverProps = new HashMap<String, String>();
                serverProps.put(BootstrapConstants.REQUEST_SERVER_FEATURES_PROPERTY, "1.0.0");

                // Start the server in feature gather mode
                EmbeddedServerImpl server = startServer(serverXML, serverName, serverProps, isTempServer);

                try {
                    // Invoke the getServerFeatures method & remember the result..
                    return server.getServerFeatures();
                } finally {
                    // Stop server in finally block to ensure that we always stop the server
                    stopServer(server, serverXML, serverName, isTempServer);
                }

            } finally {
                // Reset the standard out back to the original stream
                System.out.flush();
                System.err.flush();
                System.setOut(stdOut);
                System.setErr(stdErr);

                if (baos.size() > 0) {
                    InstallLogUtils.getInstallLogger().log(Level.FINEST, baos.toString());
                }

                InstallUtils.close(ps);
            }
        }

        private static EmbeddedServerImpl startServer(File serverXML, String serverName, Map<String, String> serverProps, boolean isTempServer) throws InstallException {
            validateFile(serverXML);

            ServerBuilder sb = new ServerBuilder().setName(serverName);
            if (isTempServer) {
                sb.setOutputDir(serverXML.getParentFile().getParentFile());
            } else {
                sb.setOutputDir(Utils.getOutputDir());
                InstallLogUtils.getInstallLogger().log(Level.FINEST, "Set output dir to " + Utils.getOutputDir());
                sb.setLogDir(Utils.getLogDir());
                InstallLogUtils.getInstallLogger().log(Level.FINEST, "Set log dir to " + (Utils.getLogDir() != null ? Utils.getLogDir() : Utils.getOutputDir()));
            }
            sb.setUserDir(serverXML.getParentFile().getParentFile().getParentFile());
            EmbeddedServerImpl server = (EmbeddedServerImpl) sb.build();

            if (server.isRunning()) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_GET_FEATURES_FROM_RUNNING_SERVER",
                                                                                          serverName), InstallException.RUNTIME_EXCEPTION);
            }

            //add in the 'do not pass go' property.
            //this will prevent the server raising the start level, preventing features starting
            //but allowing the kernel to be alive enough for us to query the configured features.
            Future<Server.Result> launchFuture = server.start(serverProps);

            try {
                // We're only partially starting the server, so this should complete
                // very quickly, but use a timeout in case something hangs.  Use a
                // timeout that should be excessive even on very slow machines.
                Server.Result rc = launchFuture.get(10, TimeUnit.MINUTES);
                if (rc == null || !rc.successful()) {
                    if (rc != null && rc.getException() != null) {
                        Exception e = rc.getException();
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STARTING_SERVER", serverName, e.getMessage()), e, rc.getReturnCode());
                    } else {
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("UNABLE_TO_STOP_START_SERVER",
                                                                                                  serverName,
                                                                                                  getServerLogsDirectoryPath(isTempServer ? serverXML.getParentFile() : Utils.getServerOutputDir(serverName))), InstallException.RUNTIME_EXCEPTION);
                    }
                }
            } catch (TimeoutException to) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STARTING_SERVER", serverName,
                                                                                          to.getMessage()), to, InstallException.RUNTIME_EXCEPTION);
            } catch (InterruptedException inte) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STARTING_SERVER", serverName,
                                                                                          inte.getMessage()), inte, InstallException.RUNTIME_EXCEPTION);
            } catch (ExecutionException ee) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STARTING_SERVER", serverName,
                                                                                          ee.getMessage()), ee, InstallException.RUNTIME_EXCEPTION);
            }

            return server;
        }

        private static void stopServer(EmbeddedServerImpl server, File serverXML, String serverName, boolean isTempServer) throws InstallException {

            // Stopping the server
            Future<Server.Result> stopFuture = server.stop();
            try {
                Server.Result rc = stopFuture.get(60, TimeUnit.SECONDS);
                if (rc == null || (!rc.successful() && rc.getReturnCode() != com.ibm.ws.kernel.boot.ReturnCode.REDUNDANT_ACTION_STATUS.getValue())) {
                    if (rc != null && rc.getException() != null) {
                        Exception e = rc.getException();
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STOPPING_SERVER", serverName, e.getMessage()), e, rc.getReturnCode());
                    } else {
                        throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("UNABLE_TO_STOP_START_SERVER",
                                                                                                  serverName,
                                                                                                  getServerLogsDirectoryPath(isTempServer ? serverXML.getParentFile() : Utils.getServerOutputDir(serverName))), InstallException.RUNTIME_EXCEPTION);
                    }
                }
            } catch (TimeoutException to) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STOPPING_SERVER", serverName,
                                                                                          to.getMessage()), to, InstallException.RUNTIME_EXCEPTION);
            } catch (InterruptedException inte) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STOPPING_SERVER", serverName,
                                                                                          inte.getMessage()), inte, InstallException.RUNTIME_EXCEPTION);
            } catch (ExecutionException ee) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("EXCEPTION_STOPPING_SERVER", serverName,
                                                                                          ee.getMessage()), ee, InstallException.RUNTIME_EXCEPTION);
            }
        }

        private static void validateFile(File serverXMLFile) throws InstallException {
            if (null == serverXMLFile) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_FIND_SERVER_XML", "null"));
            } else if (!serverXMLFile.isFile()) {
                String serverDir = (InstallUtils.SERVER_XML.equalsIgnoreCase(serverXMLFile.getName())) ? serverXMLFile.getParent() : serverXMLFile.getAbsolutePath();
                if (null == serverDir) {
                    serverDir = "";
                }
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_FIND_SERVER_XML", serverDir));
            }
        }
    }
}

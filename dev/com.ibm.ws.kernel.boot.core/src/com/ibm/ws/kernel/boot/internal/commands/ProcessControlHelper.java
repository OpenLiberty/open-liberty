/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.LaunchArguments;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileShareLockProcessStatusImpl;
import com.ibm.ws.kernel.boot.internal.PSProcessStatusImpl;
import com.ibm.ws.kernel.boot.internal.ProcessStatus;
import com.ibm.ws.kernel.boot.internal.ProcessStatus.State;
import com.ibm.ws.kernel.boot.internal.ServerLock;
import com.ibm.ws.kernel.productinfo.ProductInfo;

/**
 * The ProcessControlHelper is the central location for implementing commands
 * that need to interact with a distinct/separate server process (either started or stopped).
 * <p>
 * This class works with {@link ServerLock} to determine if the server is running,
 * and will work with the {@link ServerCommandClient} to invoke operations on a running
 * server.
 */
public class ProcessControlHelper {

    public static final String INTERNAL_PID = "pid";
    public static final String INTERNAL_PID_FILE = "pid-file";

    final String serverName;
    final String serverConfigDir;
    final String serverOutputDir;
    final File consoleLogFile;
    final BootstrapConfig bootProps;
    final LaunchArguments launchArgs;

    public ProcessControlHelper(BootstrapConfig bootProps, LaunchArguments launchArgs) {
        this.serverName = bootProps.getProcessName();
        this.bootProps = bootProps;
        this.launchArgs = launchArgs;

        // Use the system property bootstrap config set: all conversion/endings will be the same
        serverConfigDir = bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVCFG_DIR);
        serverOutputDir = bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVOUT_DIR);
        consoleLogFile = bootProps.getConsoleLogFile();
    }

    private String getPID() {
        String pid = launchArgs.getOption(INTERNAL_PID);
        return "".equals(pid) ? null : pid;
    }

    /**
     * Stop the server
     *
     * @return
     */
    public ReturnCode stop() {
        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStopping"), serverName));

        // Use initialized bootstrap configuration to find the server lock file.
        ServerLock serverLock = ServerLock.createTestLock(bootProps);

        // we can not pre-test the server: on some platforms, the server lock file
        // can be deleted, which means waiting to see if it is there leads to
        // an abandoned server.
        ReturnCode stopRc = ReturnCode.STOP_ACTION;

        // The lock file may have been (erroneously) deleted: this can happen on linux
        boolean lockExists = serverLock.lockFileExists();

        if (lockExists) {
            // If the lock exists, check quickly to see if the process is holding it.
            if (serverLock.testServerRunning()) {
                // we need to tell the server to stop...
                ServerCommandClient scc = new ServerCommandClient(bootProps);

                if (scc.isValid()) {
                    stopRc = scc.stopServer(launchArgs.getOption("force") != null);
                } else {
                    // we can't communicate to the server...
                    stopRc = ReturnCode.ERROR_SERVER_STOP;
                }
            } else {
                // nope: lock not held, we're already stopped
                stopRc = ReturnCode.REDUNDANT_ACTION_STATUS;
            }
        } else {
            // no lock file: we assume the server is not running, we have nothing to do.
            stopRc = ReturnCode.REDUNDANT_ACTION_STATUS;
        }

        // If the lock file existed before we attempted to stop the server,
        // wait until we can obtain the server lock file (because the server process has stopped)
        if (stopRc == ReturnCode.OK && lockExists) {
            stopRc = serverLock.waitForStop();
        }

        if (stopRc == ReturnCode.OK) {
            String pid = getPID();
            if (pid != null) {
                stopRc = waitForProcessStop(pid);
            }
        }

        if (stopRc == ReturnCode.OK) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStopped"), serverName));
        } else if (stopRc == ReturnCode.REDUNDANT_ACTION_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverNotRunning"), serverName));
        } else if (stopRc == ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.serverStopCommandPortDisabled"), serverName));
        } else {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStopException"), serverName));
        }

        return stopRc;
    }

    private ReturnCode waitForProcessStop(String pid) {
        ProcessStatus ps = new PSProcessStatusImpl(pid);

        for (int i = 0; i < BootstrapConstants.MAX_POLL_ATTEMPTS; i++) {
            try {
                State processRunning = ps.isPossiblyRunning();
                if ((processRunning == State.NO) || (processRunning == State.UNDETERMINED)) {
                    return ReturnCode.OK;
                }

                Thread.sleep(BootstrapConstants.POLL_INTERVAL_MS);
            } catch (Exception e) {
                Debug.printStackTrace(e);
                break;
            }
        }

        return ReturnCode.ERROR_SERVER_STOP;
    }

    /**
     * Check the server status
     *
     * @return
     */
    public ReturnCode status(boolean starting) {
        // Use initialized bootstrap configuration to find the server lock file.
        ServerLock serverLock = ServerLock.createTestLock(bootProps);
        ReturnCode rc = ReturnCode.OK;

        // The lock file may have been (erroneously) deleted: this can happen on linux
        boolean lockExists = serverLock.lockFileExists();

        if (lockExists) {
            // If the lock exists, check quickly to see if the process is holding it.
            if (serverLock.testServerRunning()) {
                rc = ReturnCode.OK;
            } else {
                // nope: lock not held, we're not running
                rc = ReturnCode.REDUNDANT_ACTION_STATUS;
            }
        } else {
            // no lock file: we assume the server is not running.
            rc = ReturnCode.REDUNDANT_ACTION_STATUS;
        }

        if (rc == ReturnCode.OK) {
            String pid = getPID();
            if (pid == null) {
                if (starting) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverIsAlreadyRunning"), serverName));
                } else {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverIsRunning"), serverName));
                }
            } else {
                if (starting) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverIsAlreadyRunningWithPID"), serverName, pid));
                } else {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverIsRunningWithPID"), serverName, pid));
                }
            }
        } else if (rc == ReturnCode.REDUNDANT_ACTION_STATUS) {
            if (!starting) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverNotRunning"), serverName));
            }
        } else if (rc == ReturnCode.SERVER_NOT_EXIST_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverNotExist"), serverName));
        } else {
            if (!starting) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStatusException"), serverName));
            } else {
                if (rc == ReturnCode.SERVER_UNKNOWN_STATUS) {
                    String pid = getPID();
                    String pidFile = launchArgs.getOption(INTERNAL_PID_FILE);
                    if (pid == null || pidFile == null) {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStartException"), serverName));
                    } else {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStartUnreachable"), serverName, pidFile, pid));
                    }
                } else {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStartException"), serverName));
                }
            }
        }

        return rc;
    }

    /**
     * Check the server status
     *
     * @return
     */
    public ReturnCode startStatus() {
        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStarting"), serverName));

        // Use initialized bootstrap configuration to find the server lock file.
        ServerLock serverLock = ServerLock.createTestLock(bootProps);
        ReturnCode rc = ReturnCode.OK;
        String pid = getPID();

        if (serverLock.lockFileExists()) {
            ProcessStatus ps = pid == null ? new FileShareLockProcessStatusImpl(consoleLogFile) : new PSProcessStatusImpl(pid);
            rc = serverLock.waitForStart(ps);
            if (rc == ReturnCode.OK) {
                ServerCommandClient scc = new ServerCommandClient(bootProps);
                rc = scc.startStatus(serverLock);
            }
        } else {
            // we have no server lock file, despite the fact that we're supposed to be looking
            // for a server in the process of starting...
            rc = ReturnCode.ERROR_SERVER_START;
        }

        displayWarningIfBeta();

        if (rc == ReturnCode.OK) {
            if (pid == null) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStarted"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStartedWithPID"), serverName, pid));
            }
        } else if (rc == ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS) {
            if (pid == null) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.serverStartedCommandPortDisabled"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.serverStartedWithPIDCommandPortDisabled"), serverName, pid));
            }
            rc = ReturnCode.SERVER_UNKNOWN_STATUS;
        } else {
            rc = ReturnCode.ERROR_SERVER_START;
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStartException"), serverName));
        }

        return rc;
    }

    /**
     * If this is an early access release of Liberty ( determined by openLiberty.properties,
     * property com.ibm.websphere.productEdition=EARLY_ACCESS ) display a warning.
     */
    private void displayWarningIfBeta() {
        ProductInfo.setBetaEditionJVMProperty();

        if (ProductInfo.getBetaEditionDuringBootstrap()) {
            System.out.println(BootstrapConstants.messages.getString("warning.earlyRelease"));
        }
    }

    private void parseJavaDumpInclude(Set<JavaDumpAction> javaDumpActions) {
        String includeValue = launchArgs.getOption(BootstrapConstants.CLI_PACKAGE_INCLUDE_VALUE);
        if (includeValue != null) {
            for (String include : includeValue.split("\\s*,\\s*")) {
                JavaDumpAction action = JavaDumpAction.forDisplayName(include);
                if (action != null) {
                    javaDumpActions.add(action);
                }
            }
        }
    }

    /**
     * Dump the server
     *
     * @return
     */
    public ReturnCode dump() {
        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumping"), serverName));

        Set<JavaDumpAction> javaDumpActions = new LinkedHashSet<JavaDumpAction>();
        parseJavaDumpInclude(javaDumpActions);

        ServerDumpPackager sdp = new ServerDumpPackager(bootProps, launchArgs.getOption(BootstrapConstants.CLI_ARG_ARCHIVE_TARGET));
        sdp.initializeDumpDirectory();

        ReturnCode dumpRc = createDumps(javaDumpActions, true, sdp.getDumpTimestamp());

        boolean serverInactiveStatusFlag = dumpRc == ReturnCode.SERVER_INACTIVE_STATUS;
        boolean serverUnknownStatusFlag = dumpRc == ReturnCode.SERVER_UNKNOWN_STATUS;
        boolean serverCommandPortDisabledFlag = dumpRc == ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS;

        if (dumpRc == ReturnCode.OK) {
            File dumpedFlag = new File(sdp.getDumpDir(), BootstrapConstants.SERVER_DUMPED_FLAG_FILE_NAME);
            if (!dumpedFlag.exists()) {
                dumpRc = ReturnCode.ERROR_SERVER_DUMP;
            }
        }

        // already dumped, then zip up. Dump zip must be created in the output directory
        if (dumpRc == ReturnCode.OK || dumpRc == ReturnCode.SERVER_INACTIVE_STATUS || dumpRc == ReturnCode.SERVER_UNKNOWN_STATUS
            || dumpRc == ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS) {

            if (javaDumpActions != null && (!!!javaDumpActions.isEmpty())) {
                dumpRc = sdp.packageDump(true);
            } else {
                dumpRc = sdp.packageDump(false);
            }
        }

        sdp.cleanupDumpDirectory();

        if (dumpRc == ReturnCode.OK) {
            if (serverInactiveStatusFlag) {
                // We are dumping a stopped server. Since it is not a problem, just print a message.
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverNotRunning"), serverName));
            }
            if (serverUnknownStatusFlag) {
                // We tried to attach to the server process, but that failed.
                // Since it might indicate an error, print a message and change the return code though the dump archive has been generated successfully.
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverStatusException"), serverName));
                dumpRc = ReturnCode.SERVER_UNKNOWN_STATUS;
            }
            if (serverCommandPortDisabledFlag) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.serverDumpCompleteCommandPortDisabled"), serverName,
                                                        sdp.getDumpFile().getAbsolutePath()));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumpComplete"), serverName, sdp.getDumpFile().getAbsolutePath()));
            }
        } else {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumpException"), serverName));
        }

        return dumpRc;
    }

    public ReturnCode dumpJava() {
        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumping"), serverName));

        Set<JavaDumpAction> javaDumpActions = new LinkedHashSet<JavaDumpAction>();
        javaDumpActions.add(JavaDumpAction.THREAD);
        parseJavaDumpInclude(javaDumpActions);

        File outputDir = bootProps.getOutputFile(null);
        File dumpedFlag = new File(outputDir, BootstrapConstants.SERVER_DUMPED_FLAG_FILE_NAME);

        ReturnCode dumpRc = ReturnCode.JAVADUMP_ACTION;
        Map<JavaDumpAction, String> files = new EnumMap<JavaDumpAction, String>(JavaDumpAction.class);

        if (!dumpedFlag.delete() && dumpedFlag.exists()) {
            dumpRc = ReturnCode.ERROR_SERVER_DUMP;
        } else {
            dumpRc = createDumps(javaDumpActions, false, null);
            if (dumpRc == ReturnCode.OK) {
                files = readJavaDumpLocations(dumpedFlag);
                if (files == null) {
                    dumpRc = ReturnCode.ERROR_SERVER_DUMP;
                }
            }
        }

        boolean isZos = ServerDumpUtil.isZos();
        if (dumpRc == ReturnCode.OK) {
            for (JavaDumpAction javaDumpAction : javaDumpActions) {
                String fileName = files.get(javaDumpAction);
                boolean zosJavaDumpSystem = (isZos && (JavaDumpAction.SYSTEM == javaDumpAction));
                if (fileName.isEmpty() && !!!zosJavaDumpSystem) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumpOptionUnsupported"),
                                                            serverName, javaDumpAction.displayName()));
                    dumpRc = ReturnCode.maxRc(dumpRc, ReturnCode.REDUNDANT_ACTION_STATUS);
                } else if (fileName.startsWith("ERROR")) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumpException"), serverName));
                    dumpRc = ReturnCode.maxRc(dumpRc, ReturnCode.ERROR_SERVER_DUMP);
                } else if (fileName.isEmpty() && zosJavaDumpSystem) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumpCompleteZos"), serverName));
                } else {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumpComplete"),
                                                            serverName, fileName));
                }
            }
        } else if (dumpRc == ReturnCode.SERVER_INACTIVE_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverNotRunning"), serverName));
        } else if (dumpRc == ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.serverJavaDumpCommandPortDisabled"), serverName));
        } else {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverDumpException"), serverName));
        }

        return dumpRc;
    }

    /**
     * Read in the file containing the location of the java dumps and populate into a map of dump type
     * to file location
     *
     * @param javaDumpLocationFile the file containing the dump locations
     * @return a map of java dump action and the corresponding file location or null if we could not successfully read the .dumpedjava file
     */
    public static Map<JavaDumpAction, String> readJavaDumpLocations(File javaDumpLocationFile) {

        Map<JavaDumpAction, String> fileLocations = new EnumMap<JavaDumpAction, String>(JavaDumpAction.class);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(javaDumpLocationFile), "UTF-8"));
            for (String line; (line = reader.readLine()) != null;) {
                int index = line.indexOf('=');
                String actionName = line.substring(0, index);
                String fileName = line.substring(index + 1).trim();
                fileLocations.put(JavaDumpAction.valueOf(actionName), fileName);
            }
        } catch (IOException ex) {
            return null;
        } finally {
            Utils.tryToClose(reader);
        }

        return fileLocations;
    }

    /**
     * Run the relevant command for dumping the system
     *
     * @param javaDumpActions the java dump actions to take place
     * @param systemDump whether this is a full dump (true) or just javadump (false)
     * @param dumpTimestamp the timestamp on the server dump packager of the full dump
     * @return the return code from attempting to run the dump
     */
    private ReturnCode createDumps(Set<JavaDumpAction> javaDumpActions, boolean introspect, String dumpTimestamp) {

        ServerLock serverLock = ServerLock.createTestLock(bootProps);
        ReturnCode dumpRc = ReturnCode.OK;

        // The lock file may have been (erroneously) deleted: this can happen on linux
        boolean lockExists = serverLock.lockFileExists();

        if (lockExists) {
            if (serverLock.testServerRunning()) {
                // server is running
                ServerCommandClient scc = new ServerCommandClient(bootProps);

                if (scc.isValid()) { //check .sCommand exist
                    if (introspect) {

                        dumpRc = scc.introspectServer(dumpTimestamp, javaDumpActions);
                    } else {
                        dumpRc = scc.javaDump(javaDumpActions);
                    }
                } else {
                    // we have a server holding a lock that we can't talk to...
                    // the dump is likely fine.. but we don't know / can't tell
                    dumpRc = ReturnCode.SERVER_UNKNOWN_STATUS;
                }
            } else {
                // nope: lock not held, we're not running
                dumpRc = ReturnCode.SERVER_INACTIVE_STATUS;
            }
        } else {
            // no lock file: we assume the server is not running.
            dumpRc = ReturnCode.SERVER_INACTIVE_STATUS;
        }

        return dumpRc;
    }

    /**
     * Pause inbound work to the Server.
     *
     * @return
     */
    public ReturnCode pause() {
        // Use initialized bootstrap configuration to find the server lock file.
        ServerLock serverLock = ServerLock.createTestLock(bootProps);

        // we can not pre-test the server: on some platforms, the server lock file
        // can be deleted, which means waiting to see if it is there leads to
        // an abandoned server.
        ReturnCode pauseRc = ReturnCode.PAUSE_ACTION;

        // The lock file may have been (erroneously) deleted: this can happen on linux
        boolean lockExists = serverLock.lockFileExists();

        String targetParm = launchArgs.getOption("target");
        if (lockExists) {
            // If the lock exists, check quickly to see if the process is holding it.
            if (serverLock.testServerRunning()) {
                // We need to tell the server to pause...
                ServerCommandClient scc = new ServerCommandClient(bootProps);

                if (scc.isValid()) {

                    if (targetParm != null) {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.pausingListeners.target"), serverName));
                        targetParm = "target=" + targetParm;
                    } else {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.pausingListeners"), serverName));
                    }

                    pauseRc = scc.pause(targetParm);
                } else {
                    // We can't communicate to the server...
                    pauseRc = ReturnCode.SERVER_UNKNOWN_STATUS;
                }
            } else {
                // nope: lock not held, server not up
                pauseRc = ReturnCode.SERVER_UNKNOWN_STATUS;
            }
        } else {
            // no lock file: we assume the server is not running, we have nothing to do.
            pauseRc = ReturnCode.SERVER_UNKNOWN_STATUS;
        }

        if (pauseRc == ReturnCode.OK) {
            if (targetParm != null) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.pausedListeners.target"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.pausedListeners"), serverName));
            }
        } else if (pauseRc == ReturnCode.SERVER_UNKNOWN_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverNotRunning"), serverName));
        } else if (pauseRc == ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.server.pause.command.port.disabled"), serverName));
        } else {
            if (targetParm != null) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.pauseFailedException.target"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.pauseFailedException"), serverName));
            }
        }

        return pauseRc;
    }

    /**
     * Resume inbound work to the server.
     *
     * @return
     */
    public ReturnCode resume() {
        // Use initialized bootstrap configuration to find the server lock file.
        ServerLock serverLock = ServerLock.createTestLock(bootProps);

        // we can not pre-test the server: on some platforms, the server lock file
        // can be deleted, which means waiting to see if it is there leads to
        // an abandoned server.
        ReturnCode resumeRc = ReturnCode.RESUME_ACTION;

        // The lock file may have been (erroneously) deleted: this can happen on linux
        boolean lockExists = serverLock.lockFileExists();

        String targetParm = launchArgs.getOption("target");
        if (lockExists) {
            // If the lock exists, check quickly to see if the process is holding it.
            if (serverLock.testServerRunning()) {
                // We need to tell the server to resume...
                ServerCommandClient scc = new ServerCommandClient(bootProps);

                if (scc.isValid()) {
                    if (targetParm != null) {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.resumingListeners.target"), serverName));
                        targetParm = "target=" + targetParm;
                    } else {
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.resumingListeners"), serverName));
                    }

                    resumeRc = scc.resume(targetParm);
                } else {
                    // We can't communicate to the server...
                    resumeRc = ReturnCode.SERVER_UNKNOWN_STATUS;
                }
            } else {
                // nope: lock not held, server not up
                resumeRc = ReturnCode.SERVER_UNKNOWN_STATUS;
            }
        } else {
            // no lock file: we assume the server is not running, we have nothing to do.
            resumeRc = ReturnCode.SERVER_UNKNOWN_STATUS;
        }

        if (resumeRc == ReturnCode.OK) {
            if (targetParm != null) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.resumedListeners.target"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.resumedListeners"), serverName));
            }
        } else if (resumeRc == ReturnCode.SERVER_UNKNOWN_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverNotRunning"), serverName));
        } else if (resumeRc == ReturnCode.SERVER_COMMAND_PORT_DISABLED_STATUS) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.server.resume.command.port.disabled"), serverName));
        } else {
            if (targetParm != null) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.resumeFailedException.target"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.resumeFailedException"), serverName));
            }
        }

        return resumeRc;
    }
}

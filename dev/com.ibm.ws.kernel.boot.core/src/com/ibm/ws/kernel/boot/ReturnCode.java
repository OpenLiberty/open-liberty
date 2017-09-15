/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants.VerifyServer;

/**
 * Pick and use a consistent set of return codes across all
 * platforms. Most common range is 0 to 256.
 */
public enum ReturnCode implements ExitCode {
    OK(ExitCode.OK), // 0
    // started/stopped is set based on operation.
    // process will return this code if start is called when server is already running
    // or will return this code for stop/status when the server is not running
    REDUNDANT_ACTION_STATUS(1),
    SERVER_NOT_EXIST_STATUS(2),
    SERVER_ACTIVE_STATUS(3),
    SERVER_INACTIVE_STATUS(4),
    // The server's workarea was removed and we failed to attach.
    SERVER_UNKNOWN_STATUS(5),
    // The server's command port is disabled, which is the default when starting as a started
    // task on z/OS.  The command port can also be explicitly disabled with a bootstrap property.
    SERVER_COMMAND_PORT_DISABLED_STATUS(6),
    // Jump a few numbers for error return codes-- see readInitialConfig
    BAD_ARGUMENT(ExitCode.BAD_ARGUMENT), // 20
    ERROR_SERVER_STOP(21),
    ERROR_SERVER_START(22),
    LOCATION_EXCEPTION(23),
    LAUNCH_EXCEPTION(24),
    RUNTIME_EXCEPTION(25),
    UNKNOWN_EXCEPTION(26),
    PROCESS_CLIENT_EXCEPTION(27),
    ERROR_SERVER_PACKAGE(28),
    ERROR_SERVER_DUMP(29),
    // Used by EnvCheck classes when a bad Java version is used.
    ERROR_BAD_JAVA_VERSION(ExitCode.ERROR_BAD_JAVA_BITMODE), // 30
    // Used by EnvCheck classes when a unsupported Java bitmode is used.
    ERROR_BAD_JAVA_BITMODE(ExitCode.ERROR_BAD_JAVA_BITMODE), // 31
    // Used by the server scripts when jvm.options has an invalid line.
    ERROR_BAD_JVM_OPTION(32),
    ERROR_COMMUNICATE_SERVER(34),
    // Used by ClientRunner when an exception occurs during executing client application's main().
    CLIENT_RUNNER_EXCEPTION(35),
    ERROR_SERVER_PAUSE(36),
    ERROR_SERVER_RESUME(37),
    // All "actions" should be < 0, these are not returned externally
    MESSAGE_ACTION(-1),
    HELP_ACTION(-2),
    STOP_ACTION(-3, "stop.log"),
    STATUS_ACTION(-4, "status.log"),
    STARTING_STATUS_ACTION(-5, "start.log"),
    START_STATUS_ACTION(-6, "start.log"),
    VERSION_ACTION(-7),
    PACKAGE_ACTION(-8, "package.log"),
    DUMP_ACTION(-9, "dump.log"),
    JAVADUMP_ACTION(-10, "javadump.log"),
    CREATE_ACTION(-11, "create.log"),
    LIST_ACTION(-12),
    INVALID(13),
    PACKAGE_WLP_ACTION(-15, "package.log"),
    PAUSE_ACTION(-16),
    RESUME_ACTION(-17);

    final int val;
    final String logName;

    ReturnCode(int val, String logName) {
        this.val = val;
        this.logName = logName;
    }

    ReturnCode(int val) {
        this(val, null);
    }

    /**
     * Some return codes (specifically a subset of the _ACTION return codes)
     * do not need to look at initial config. Do not look at initial config
     * if we already have a bad return code (like a bad argument)
     *
     * @return false if reading bootstrap properties to establish bootstrap
     *         server config is not necessary.
     */
    boolean readInitialConfig() {
        return val < 20 &&
               this != HELP_ACTION &&
               this != CREATE_ACTION &&
               this != VERSION_ACTION &&
               this != STATUS_ACTION &&
               this != MESSAGE_ACTION;
    }

    @Override
    public int getValue() {
        return val;
    }

    /**
     * @return true if a message should be issued regarding use of the default
     *         server name
     */
    public boolean defaultServerNameMessage() {
        return this == OK ||
               this == CREATE_ACTION;
    }

    /**
     * @return
     */
    public VerifyServer getVerifyServer() {
        switch (this) {
            case OK:
                // We're running the server, so create the default if necessary.
                return BootstrapConstants.VerifyServer.CREATE_DEFAULT;
            case CREATE_ACTION:
                // We're creating a server, obviously
                return BootstrapConstants.VerifyServer.CREATE;
            case HELP_ACTION:
            case VERSION_ACTION:
            case LIST_ACTION:
            case MESSAGE_ACTION:
            case PACKAGE_WLP_ACTION:
                // These actions don't need to verify that the server exists
                // they can handle lack of existence just fine on their own.
                return BootstrapConstants.VerifyServer.SKIP;
            case START_STATUS_ACTION:
                // This action is only used by the script after it has launched
                // the server process in the background.  Since the script has
                // already verified the server name is valid, we don't need to
                // check it again.  Verifying won't work anyway if defaultServer
                // doesn't already exist since we are racing the server process,
                // which is trying to create it.
                return BootstrapConstants.VerifyServer.SKIP;
            default:
                if (val < 20) {
                    // Otherwise, we're executing some other action against the
                    // server, so require it to exist.
                    return BootstrapConstants.VerifyServer.EXISTS;
                } else {
                    // Error return code-- skip
                    return BootstrapConstants.VerifyServer.SKIP;
                }
        }
    }

    /**
     * Return the Return code with the higher value. Used to
     * set return codes where the highest (error) is preserved
     * over an intermediate ok/redundant.
     *
     * @param oldRc
     * @param newRc
     * @return
     */
    public static ReturnCode maxRc(ReturnCode oldRc, ReturnCode newRc) {
        return (oldRc.val > newRc.val) ? oldRc : newRc;
    }

    public static ReturnCode getEnum(int i) {
        ReturnCode[] codes = ReturnCode.values();
        for (ReturnCode code : codes) {
            if (code.getValue() == i) {
                return code;
            }
        }
        return ReturnCode.INVALID;
    }
}
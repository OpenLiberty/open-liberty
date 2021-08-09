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
package com.ibm.ws.install.internal.platform;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils;

import wlp.lib.extract.ReturnCode;
import wlp.lib.extract.platform.PlatformUtils;

/**
 *
 */
public class InstallPlatformUtils {
    static final Logger LOG = Logger.getLogger(InstallConstants.LOGGER_NAME);
    public static final int UMASK_NOT_APPLICABLE = PlatformUtils.UMASK_NOT_APPLICABLE;

    public static int executeCommand(String[] cmd, String[] env, File workingDir, Writer out, Writer err) throws IOException {
        return PlatformUtils.executeCommand(cmd, env, workingDir, out, err);
    }

    public static void setExecutePermissionAccordingToUmask(String[] files) throws InstallException, IOException {
        ReturnCode rc = PlatformUtils.setExecutePermissionAccordingToUmask(files);

        if (null != rc && ReturnCode.OK.getCode() != rc.getCode()) {
            throw createInstallException(rc);
        }
    }

    public static void setExtendedAttributes(Map<String, Set<String>> extattrFilesMap) throws InstallException, IOException {
        ReturnCode rc = PlatformUtils.setExtendedAttributes(extattrFilesMap);

        if (null != rc && ReturnCode.OK.getCode() != rc.getCode()) {
            throw createInstallException(rc);
        }
    }

    public static int getUmask() throws IOException {
        return PlatformUtils.getUmask();
    }

    public static String getASCIISystemCharSet() {
        return PlatformUtils.getASCIISystemCharSet();
    }

    public static String getEBCIDICSystemCharSet() {
        return PlatformUtils.getEBCIDICSystemCharSet();
    }

    private static InstallException createInstallException(ReturnCode rc) {
        String msgKey = rc.getMessageKey();

        if ("ERROR_UNABLE_TO_SET_EXT_ATTR".equals(msgKey)) {
            String msg = InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_SET_EXT_ATTR",
                                                                                        rc.getParameters());
            return new InstallException(msg, InstallException.RUNTIME_EXCEPTION);
        } else if ("ERROR_UNABLE_TO_SET_EXECUTE_PERMISSIONS".equals(msgKey)) {
            String msg = InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_SET_EXECUTE_PERMISSIONS",
                                                                                        rc.getParameters());
            return new InstallException(msg, InstallException.RUNTIME_EXCEPTION);
        } else if ("ERROR_UNABLE_TO_GET_UMASK".equals(msgKey)) {
            String msg = InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_GET_UMASK",
                                                                                        rc.getParameters());
            return new InstallException(msg, InstallException.RUNTIME_EXCEPTION);
        } else if ("ERROR_INVALID_EXTATTR_PARMS".equals(msgKey)) {
            String msg = InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_EXTATTR_PARMS",
                                                                                        rc.getParameters());
            return new InstallException(msg, InstallException.BAD_ARGUMENT);
        } else if ("ERROR_EXECUTING_COMMAND".equals(msgKey)) {
            String msg = InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_EXECUTING_COMMAND",
                                                                                        rc.getParameters());
            return new InstallException(msg, InstallException.IO_FAILURE);
        } else if ("ERROR_UNABLE_TO_LOCATE_COMMAND_EXE".equals(msgKey)) {
            String msg = InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_TO_LOCATE_COMMAND_EXE",
                                                                                        rc.getParameters());
            return new InstallException(msg, InstallException.RUNTIME_EXCEPTION);
        }

        return new InstallException(rc.getErrorMessage(), InstallException.RUNTIME_EXCEPTION);
    }
}

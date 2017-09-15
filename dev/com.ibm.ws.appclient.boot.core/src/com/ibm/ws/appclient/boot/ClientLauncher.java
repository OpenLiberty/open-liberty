/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.appclient.boot;

import java.util.ResourceBundle;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchArguments;
import com.ibm.ws.kernel.boot.Launcher;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.KernelBootstrap;
import com.ibm.ws.kernel.boot.internal.ServerLock;

/**
 * The platform client launcher; processes command line options and
 * bootstrap.properties file(s) and launches the OSGi framework.
 * 
 * Use the --help option (passed to main) for details on available command line
 * options.
 */
public class ClientLauncher extends Launcher {

    /**
     * The runtime client launcher/main. String arguments are treated and parsed as
     * command line parameters. This method will call <code>System.exit</code> when the framework shuts down.
     * 
     * @param args
     *            Command line arguments.
     * @throws UnsupportedOperationException
     * 
     * @see Launcher#createPlatform(String[])
     */
    public static void main(String[] args) throws UnsupportedOperationException {
        ClientLauncher launcher = new ClientLauncher();
        System.exit(launcher.createPlatform(args));
    }

    @Override
    protected BootstrapConfig createBootstrapConfig() {
        return new ClientBootstrapConfig();
    }

    @Override
    protected boolean isClient() {
        return true;
    }

    @Override
    protected ReturnCode handleActions(BootstrapConfig bootProps, LaunchArguments launchArgs) {
        ReturnCode rc = launchArgs.getRc();

        switch (rc) {
            case OK:
                rc = new KernelBootstrap(bootProps).go();
                break;
            case CREATE_ACTION:
                // Use initialized bootstrap configuration to create the server lock.
                // This ensures the server and nested workarea directory exist and are writable
                ServerLock.createServerLock(bootProps);
                rc = ReturnCode.OK;
                break;
            case MESSAGE_ACTION:
                rc = showMessage(launchArgs);
                break;
            case HELP_ACTION:
                rc = showHelp(launchArgs);
                break;
            case PACKAGE_ACTION:
                rc = new com.ibm.ws.kernel.boot.internal.commands.PackageCommand(bootProps, launchArgs).doPackage();
                break;
            default:
                showHelp(launchArgs);
                rc = ReturnCode.BAD_ARGUMENT;
        }
        return rc;
    }

    @Override
    protected ResourceBundle getOptionsResourceBundle() {
        /** Since this launches the framework, we have to do translation ourselves.. */
        return ResourceBundle.getBundle("com.ibm.ws.appclient.boot.resources.ClientLauncherOptions");
    }

    @Override
    protected String getDefaultProcessName() {
        return BootstrapConstants.DEFAULT_CLIENT_NAME;
    }

    @Override
    protected String getDefaultProcessNameMessageKey() {
        return "info.defaultClient";
    }
}

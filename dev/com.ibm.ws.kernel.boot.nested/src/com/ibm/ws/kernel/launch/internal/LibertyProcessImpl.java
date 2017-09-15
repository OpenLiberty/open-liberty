/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.launch.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.commands.JavaDumpAction;
import com.ibm.ws.kernel.boot.internal.commands.ServerDumpPackager;
import com.ibm.ws.kernel.provisioning.VersionUtility;

/**
 * Implementation for simple command line service. This service is published
 * into the framework by the FrameworkManager, and gives requestors access to
 * the original command line parameters (those arguments following -- on the
 * command line,
 * 
 * @see {@link com.ibm.ws.kernel.boot.Launcher} <p>
 *      This also provides a hook for driving framework shutdown from outside of
 *      the framework.
 */
public class LibertyProcessImpl implements LibertyProcess {
    private List<String> argList;
    private final FrameworkManager frameworkMgr;

    /**
     * Construct a new command line implementation with the provided argument
     * list
     * 
     * @param args
     *            Array of String command line arguments
     * @param frameworkMgr
     *            Calling FrameworkManager: used for shutdown
     */
    public LibertyProcessImpl(List<String> args, FrameworkManager frameworkMgr) {
        if (args == null)
            argList = new ArrayList<String>();
        else
            argList = args;

        this.frameworkMgr = frameworkMgr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getArgs() {
        return argList.toArray(new String[argList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        frameworkMgr.shutdownFramework();
    }

    /**
     * Used
     * 
     * @returns a dictionary containing the original command line arguments
     */
    public Dictionary<String, ?> getServiceProps() {
        Hashtable<String, Object> d = new Hashtable<String, Object>();
        d.put(BootstrapConstants.INTERNAL_COMMAND_LINE_ARG_LIST, getArgs());
        String version = System.getProperty(BootstrapConstants.JAVA_SPEC_VERSION);
        if (version != null) {
            try {
                d.put(BootstrapConstants.JAVA_SPEC_VERSION, VersionUtility.stringToVersion(version));
            } catch (IllegalArgumentException iae) {

            }
        }

        // wlp.process.type
        d.put(BootstrapConstants.LOC_PROPERTY_PROCESS_TYPE, frameworkMgr.config.getProcessType());

        return d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createJavaDump(Set<String> requestedDumps) {
        frameworkMgr.dumpJava(parseJavaDumpActions(requestedDumps));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createServerDump(Set<String> includedDumps) {
        ServerDumpPackager sdp = new ServerDumpPackager(frameworkMgr.config, null);
        sdp.initializeDumpDirectory();

        frameworkMgr.introspectFramework(sdp.getDumpTimestamp(), parseJavaDumpActions(includedDumps));

        ReturnCode rc = ReturnCode.OK;
        if (includedDumps == null || includedDumps.isEmpty()) {
            rc = sdp.packageDump(false);
        } else {
            rc = sdp.packageDump(true);
        }
        sdp.cleanupDumpDirectory();
        if (rc == ReturnCode.OK) {
            return sdp.getDumpFile().getAbsolutePath();
        }
        else {
            return null;
        }
    }

    private Set<JavaDumpAction> parseJavaDumpActions(Set<String> namedDumpActions) {
        Set<JavaDumpAction> parsedActions = new LinkedHashSet<JavaDumpAction>();
        for (String namedDumpAction : namedDumpActions) {
            JavaDumpAction parsedAction = JavaDumpAction.forDisplayName(namedDumpAction);
            if (parsedAction != null) {
                parsedActions.add(parsedAction);
            }
        }
        return parsedActions;
    }
}

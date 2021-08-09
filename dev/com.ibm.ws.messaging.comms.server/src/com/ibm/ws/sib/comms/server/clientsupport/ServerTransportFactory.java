/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server.clientsupport;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.comms.common.CommsDiagnosticModule;
import com.ibm.ws.sib.comms.server.AcceptListenerFactoryImpl;
import com.ibm.ws.sib.jfapchannel.server.ServerConnectionManager;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author schmittm
 * 
 */
public class ServerTransportFactory {
    /** Class name for FFDC's */
    private static String CLASS_NAME = ServerTransportFactory.class.getName();

    /** Register our trace component */
    private static TraceComponent tc = SibTr.register(ServerTransportFactory.class,
                                                      CommsConstants.MSG_GROUP,
                                                      CommsConstants.MSG_BUNDLE);

    /** Trace the class information */
    static {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "Source info: @(#)SIB/ws/code/sib.comms.server.impl/src/com/ibm/ws/sib/comms/server/clientsupport/ServerTransportFactory.java, SIB.comms, WASX.SIB, aa1225.01 1.31");

        // Initialise the FFDC diagnositic module
        CommsDiagnosticModule.initialise();
    }

    /**
     * Constructor
     * 
     * @param port
     */
    public ServerTransportFactory(int port) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "<init>", "" + port);

        try {
            ServerConnectionManager.initialise(new AcceptListenerFactoryImpl());
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".<init>",
                                        CommsConstants.SERVERTRANSPORTFACTORY_INIT_02,
                                        this);

            SibTr.error(tc, "SERVER_FAILED_TO_START_SICO2004", t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * Starts the comms server communications.
     * 
     */
    public static void startServerComms() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "startServerComms");

        try {
            ServerConnectionManager.initialise(new AcceptListenerFactoryImpl());
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".startServerComms",
                                        CommsConstants.SERVERTRANSPORTFACTORY_INIT_02,
                                        null);

            SibTr.error(tc, "SERVER_FAILED_TO_START_SICO2004", t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "startServerComms");
    }

}

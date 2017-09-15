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
package com.ibm.rls.jdbc;

import org.osgi.service.component.ComponentContext;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.custom.jdbc.impl.SQLMultiScopeRecoveryLog;
import com.ibm.ws.recoverylog.custom.jdbc.impl.SQLSharedServerLeaseLog;
import com.ibm.ws.recoverylog.spi.CustomLogProperties;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.InvalidLogPropertiesException;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.RecoveryLog;
import com.ibm.ws.recoverylog.spi.RecoveryLogComponent;
import com.ibm.ws.recoverylog.spi.RecoveryLogFactory;
import com.ibm.ws.recoverylog.spi.SharedServerLeaseLog;
import com.ibm.ws.recoverylog.spi.TraceConstants;

/**
 * This class is defined as a Declarative Service in the com.ibm.rls.jdbc component's
 * bnd.bnd file. It provides a service to the com.ibm.ws.recoverylog component which uses a
 * RecoveryLogFactory to create a RecoveryLog where transactions maybe logged.
 */
public class SQLRecoveryLogFactory implements RecoveryLogFactory {

    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(SQLRecoveryLogFactory.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    public SQLRecoveryLogFactory() {}

    /*
     * Called by DS to activate service
     */
    public void activate(ComponentContext cc) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "activate  ComponentContext " + cc);
    }

    /*
     * createRecoveryLog
     * 
     * @param props properties to be associated with the new recovery log (eg DBase config)
     * 
     * @param agent RecoveryAgent which provides client service data eg clientId
     * 
     * @param logcomp RecoveryLogComponent which can be used by the recovery log to notify failures
     * 
     * @param failureScope the failurescope (server) for which this log is to be created
     * 
     * @return RecoveryLog or MultiScopeLog to be used for logging
     * 
     * @exception InvalidLogPropertiesException thrown if the properties are not consistent with the logFactory
     */
    @Override
    public RecoveryLog createRecoveryLog(CustomLogProperties props, RecoveryAgent agent, RecoveryLogComponent logComp, FailureScope fs) throws InvalidLogPropertiesException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoveryLog", new Object[] { props, agent, logComp, fs });

        SQLMultiScopeRecoveryLog theLog = new SQLMultiScopeRecoveryLog(props, agent, fs);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoveryLog", theLog);
        return theLog;
    }

    public SharedServerLeaseLog createLeaseLog(CustomLogProperties props) throws InvalidLogPropertiesException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createLeaseLog", new Object[] { props });

        SharedServerLeaseLog leaseLog = new SQLSharedServerLeaseLog(props);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createLeaseLog", leaseLog);
        return leaseLog;
    }
}

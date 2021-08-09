/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jmsra.impl;

//Sanjay Liberty Changes
//javax.transaction.Synchronization and com.ibm.ws.Transaction.SynchronizationProvider could not resolved in 
//JmsJcaManagedConnectionSynchronizationProvider, need to check with transaction team for resolution.  For time being 
//commenting all the contents in this class. So now it behaves like super class JmsJcaManagedConnection


import javax.security.auth.Subject;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SIUncoordinatedTransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;

/**
 * Sub-class of <code>JmsJcaManagedConnection</code> supporting the
 * <code>SynchronizationProvider</code> interface to allow sharing of
 * connections with container-managed persistence.
 */
final class JmsJcaManagedConnectionSynchronizationProvider extends
        JmsJcaManagedConnection { //Sanjay Liberty Changes - implements SynchronizationProvider {

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    /**
     * The current <code>Synchronization</code> registered with a transaction.
     * Set in <code>getSynchronization</code> and unset in
     * <code>afterComplation</code>.
     */
    private SIUncoordinatedTransaction synchronization;

    private static TraceComponent TRACE = SibTr.register(
            JmsJcaManagedConnectionSynchronizationProvider.class,
            JmsraConstants.MSG_GROUP, JmsraConstants.MSG_BUNDLE);

    private static TraceNLS NLS = TraceNLS
            .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    /**
     * Constructs a managed connection that supports connection sharing with
     * CMP.
     * 
     * @param managedConnectionFactory
     *            the parent managed connection factory
     * @param coreConnection
     *            the initial connection
     * @param userDetails
     *            the user details specified when the core connection was
     *            created
     * @param subject
     *            the subject
     * @throws SIConnectionUnavailableException
     *             if the core connection is no longer available
     * @throws SIConnectionDroppedException
     *             if the core connection has been dropped
     */
    JmsJcaManagedConnectionSynchronizationProvider(
            final JmsJcaManagedConnectionFactoryImpl managedConnectionFactory,
            final SICoreConnection coreConnection,
            final JmsJcaUserDetails userDetails, final Subject subject)
            throws SIConnectionDroppedException,
            SIConnectionUnavailableException {

        super(managedConnectionFactory, coreConnection, userDetails, subject);

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "JmsJcaManagedConnectionSynchronization",
                    new Object[] { managedConnectionFactory, coreConnection,
                            userDetails, subjectToString(subject) });
            SibTr.exit(this, TRACE, "JmsJcaManagedConnectionSynchronization");
        }

    }
}

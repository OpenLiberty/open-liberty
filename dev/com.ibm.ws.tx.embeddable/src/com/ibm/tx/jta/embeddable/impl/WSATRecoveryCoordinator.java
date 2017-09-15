package com.ibm.tx.jta.embeddable.impl;

/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.transaction.SystemException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.util.TxBundleTools;
import com.ibm.tx.remote.RecoveryCoordinator;
import com.ibm.tx.remote.RecoveryCoordinatorFactory;
import com.ibm.tx.remote.RecoveryCoordinatorNotAvailableException;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public final class WSATRecoveryCoordinator implements RecoveryCoordinator, Serializable
{
    // 601 SUID based on private final data and non-public ctor
    private static final long serialVersionUID = 5500037426315245114L; /* @274187C */

    private static transient final TraceComponent tc = Tr.register(WSATRecoveryCoordinator.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private final String _recoveryCoordinatorFactoryFilter;
    private final Serializable _recoveryCoordinatorInfo;
    private final String _globalId;

    private transient RecoveryCoordinator rc;

    /**
     * @param recoveryCoordinatorFactoryFilter
     * @param recoveryCoordinatorInfo
     * @param globalId
     */
    public WSATRecoveryCoordinator(String recoveryCoordinatorFactoryFilter, Serializable recoveryCoordinatorInfo, String globalId) {
        _recoveryCoordinatorFactoryFilter = recoveryCoordinatorFactoryFilter;
        _recoveryCoordinatorInfo = recoveryCoordinatorInfo;
        _globalId = globalId;
    }

    // As called after recovery on distributed platform
    public static WSATRecoveryCoordinator fromLogData(byte[] bytes) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fromLogData", bytes);

        WSATRecoveryCoordinator wsatRC = null;
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            final ObjectInputStream ois = new ObjectInputStream(bais);
            final Object obj = ois.readObject();
            wsatRC = (WSATRecoveryCoordinator) obj;
        } catch (Throwable e) {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.wstx.WSATRecoveryCoordinator.fromLogData", "67");

            final Throwable se = new SystemException().initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "fromLogData", se);
            throw (SystemException) se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fromLogData", wsatRC);
        return wsatRC;
    }

    public String getGlobalId()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getGlobalId", _globalId);
        return _globalId;
    }

    // Only called on distributed - z/OS logging is done by WSATCRRecoveryCoordinator
    public byte[] toLogData() throws javax.transaction.SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toLogData", this);

        byte[] logData = null;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            logData = baos.toByteArray();
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.wstx.WSATRecoveryCoordinator.toLogData", "118", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "Exception caught in toLogData " + e);

            final Throwable se = new SystemException().initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "toLogData", se);
            throw (SystemException) se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toLogData", logData);
        return logData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.tx.remote.RecoveryCoordinator#replayCompletion(java.lang.String)
     */
    @Override
    public void replayCompletion(String globalId) {
        if (rc == null) {
            // Find a factory to create the RecoveryCoordinator
            try {
                rc = getRecoveryCoordinator();
            } catch (RecoveryCoordinatorNotAvailableException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "replayCompletion", e);
            }
        }

        if (rc != null) {
            rc.replayCompletion(globalId);
        }
    }

    private RecoveryCoordinator getRecoveryCoordinator() throws RecoveryCoordinatorNotAvailableException {
        try {
            final RecoveryCoordinatorFactory factory = lookupRecoveryCoordinatorFactory(_recoveryCoordinatorFactoryFilter);

            if (factory == null) {
                throw new RecoveryCoordinatorNotAvailableException();
            }

            return factory.getRecoveryCoordinator(_recoveryCoordinatorInfo);
        } catch (RecoveryCoordinatorNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            throw new RecoveryCoordinatorNotAvailableException(e);
        }
    }

    public static RecoveryCoordinatorFactory lookupRecoveryCoordinatorFactory(String filter)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lookupRecoveryCoordinatorFactory", filter);

        final BundleContext bundleContext = TxBundleTools.getBundleContext();

        if (bundleContext == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "lookupRecoveryCoordinatorFactory", null);
            return null;
        }

        ServiceReference[] results = null;

        try
        {
            results = bundleContext.getServiceReferences(RecoveryCoordinatorFactory.class.getCanonicalName(), filter);
        } catch (InvalidSyntaxException e) {
            // Wasn't a filter
            if (tc.isEntryEnabled())
                Tr.exit(tc, "lookupRecoveryCoordinatorFactory", "not a filter");
            return null;
        }

        if (results == null || results.length <= 0) {
            if (results == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Results returned from registry are null");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Results of length " + results.length + " returned from registry");
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "lookupRecoveryCoordinatorFactory", null);
            return null;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Found " + results.length + " service references in the registry");

        final RecoveryCoordinatorFactory recoveryCoordinatorFactory = (RecoveryCoordinatorFactory) bundleContext.getService(results[0]);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "lookupRecoveryCoordinatorFactory", recoveryCoordinatorFactory);
        return recoveryCoordinatorFactory;
    }
}
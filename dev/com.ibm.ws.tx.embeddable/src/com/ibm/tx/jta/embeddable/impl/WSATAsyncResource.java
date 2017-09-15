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
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.impl.XARecoveryDataHelper;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class defines a 2PC participant and allows the TransactionManager to communicate with it via web services
 * 
 * This is the thing we're gonna construct from what we're given and is what we're gonna log
 */

public final class WSATAsyncResource implements Serializable
{
    /**  */
    private static final long serialVersionUID = 4244509484588694781L;

    private static transient final TraceComponent tc = Tr.register(WSATAsyncResource.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private final Xid _xid;
    protected final String _XAResourceFactoryFilter;
    protected final Serializable _XAResourceFactoryKey;

    public WSATAsyncResource(String factoryFilter, Serializable key, Xid xid)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "WSATAsyncResource", new Object[] { factoryFilter, key, xid });

        _xid = xid;
        _XAResourceFactoryFilter = factoryFilter;
        _XAResourceFactoryKey = key;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "WSATAsyncResource", this);
    }

    String getTxIdentifier()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getTxIdentifier", _xid);
        return _xid.toString();
    }

    // This is what we call to get the answer back. All the wsat happens here
    // It's gonna be called asynchronously
    public int prepareOperation() throws XAException, XAResourceNotAvailableException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepareOperation", new Object[] { this });

        final int retVal = getXAResource().prepare(_xid);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepareOperation", retVal);
        return retVal;
    }

    public void commitOperation() throws XAException, XAResourceNotAvailableException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commitOperation", new Object[] { this });

        getXAResource().commit(_xid, false);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commitOperation");
    }

    public void rollbackOperation() throws XAException, XAResourceNotAvailableException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollbackOperation", new Object[] { this });

        getXAResource().rollback(_xid);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollbackOperation");
    }

    public void forgetOperation() throws XAException, XAResourceNotAvailableException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forgetOperation", new Object[] { this });

        getXAResource().forget(_xid);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forgetOperation");
    }

    // Distributed logData call only - z/os encodes log data in WSATCRAsyncResource
    byte[] toLogData() throws javax.transaction.SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toLogData", this);

        byte[] logData = null;

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try
        {
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            logData = baos.toByteArray();
        } catch (Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.wstx.WSATAsyncResource.toLogData", "279", this);

            final SystemException se = new SystemException();
            se.initCause(e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "toLogData", se);
            throw se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toLogData", logData);
        return logData;
    }

    // Distributed logData call only - z/os encodes log data in WSATCRAsyncResource
    static WSATAsyncResource fromLogData(byte[] bytes) throws javax.transaction.SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fromLogData", bytes);

        WSATAsyncResource resource = null;
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        try
        {
            final ObjectInputStream ois = new ObjectInputStream(bais);
            final Object obj = ois.readObject();
            resource = (WSATAsyncResource) obj;
        } catch (Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.wstx.WSATAsyncResource.fromLogData", "307");

            final SystemException se = new SystemException();
            se.initCause(e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "fromLogData", se);
            throw se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fromLogData", resource);
        return resource;
    }

    String describe()
    {
        return _XAResourceFactoryKey.toString();
    }

    Serializable getKey() {
        return _XAResourceFactoryKey;
    }

    private XAResource getXAResource() throws XAResourceNotAvailableException {
        try {
            final XAResourceFactory factory = XARecoveryDataHelper.lookupXAResourceFactory(_XAResourceFactoryFilter);

            if (factory == null) {
                throw new XAResourceNotAvailableException();
            }

            return factory.getXAResource(_XAResourceFactoryKey);
        } catch (XAResourceNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            throw new XAResourceNotAvailableException(e);
        }
    }
}
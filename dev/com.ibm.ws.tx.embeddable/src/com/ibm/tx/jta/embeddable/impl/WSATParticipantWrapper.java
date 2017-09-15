package com.ibm.tx.jta.embeddable.impl;

/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;

public final class WSATParticipantWrapper extends JTAAsyncResourceBase
{
    private static final TraceComponent tc = Tr.register(WSATParticipantWrapper.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected WSATAsyncResource _wsatAsyncResource;

    private FutureTask<Integer> _prepareResult;
    private FutureTask<Void> _commitResult;
    private FutureTask<Void> _rollbackResult;
    private FutureTask<Void> _forgetResult;

    private ExecutorService _commitExecutor;
    private ExecutorService _prepareExecutor;
    private ExecutorService _rollbackExecutor;

    // TODO timeouts

    public WSATParticipantWrapper(WSATAsyncResource wsatAsyncResource)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "WSATParticipantWrapper", wsatAsyncResource);

        _wsatAsyncResource = wsatAsyncResource;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "WSATParticipantWrapper", this);
    }

    public WSATParticipantWrapper(byte[] logData) throws SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "WSATParticipantWrapper", logData);

        _wsatAsyncResource = WSATAsyncResource.fromLogData(logData);
        _asyncState = ASYNC_STATE_PREPARED; // d229947

        if (tc.isEntryEnabled())
            Tr.exit(tc, "WSATParticipantWrapper", this);
    }

    @Override
    public void commit_one_phase() throws XAException
    {
        // This is an error.  We are not ResourceSupportsOnePhaseCommit so this
        // method should never be called.
    }

    @Override
    public void commit() throws XAException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit", this);

        int retVal = XAResource.XA_OK;

        _stateProcessed = true;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_asyncState in commit = " + _asyncState);

        XAException xae = null;

        try
        {
            try {
                _commitResult.get();
                _commitExecutor.shutdown();
                _commitExecutor = null;
            } catch (Exception e) {
                xae = new XAException(XAException.XAER_RMFAIL);
                xae.initCause(e);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", xae);
                throw xae;
            }
        } finally
        {
            int rc = xae == null ? retVal : xae.errorCode;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit");
    }

    @Override
    public int prepare() throws XAException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepare", this);

        int retVal = XAResource.XA_OK;

        XAException xae = null;

        _stateProcessed = true;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_asyncState in prepare = " + _asyncState);

        try {
            retVal = _prepareResult.get();
            _prepareExecutor.shutdown();
            _prepareExecutor = null;
        } catch (ExecutionException e) {

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Async operation threw ExecutionException", e);

            if (e.getCause() instanceof XAException) {
                xae = (XAException) e.getCause();
            } else {
                xae = new XAException(XAException.XAER_RMFAIL);
                xae.initCause(e);
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepare", xae);
            throw xae;
        } catch (InterruptedException e) {
            xae = new XAException(XAException.XAER_RMFAIL);
            xae.initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepare", xae);
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepare", retVal);
        return retVal;
    }

    @Override
    public void rollback() throws XAException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback", this);

        _stateProcessed = true;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "_asyncState in rollback = " + _asyncState);

        XAException xae = null;

        try {
            _rollbackResult.get();
        } catch (ExecutionException e) {

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Async operation threw ExecutionException", e);

            if (e.getCause() instanceof XAException) {
                xae = (XAException) e.getCause();
            } else {
                xae = new XAException(XAException.XAER_RMFAIL);
                xae.initCause(e);
            }
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", xae);
            throw xae;
        } catch (InterruptedException e) {
            xae = new XAException(XAException.XAER_RMFAIL);
            xae.initCause(e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", xae);
            throw xae;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollback");
    }

    @Override
    public void sendAsyncCommit() throws XAException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sendAsyncCommit", this);

        if (_asyncState != ASYNC_STATE_ABORTED && _asyncState != ASYNC_STATE_COMMITTED)
        {
            _commitResult = new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() throws RemoteException, XAException, XAResourceNotAvailableException {
                    _wsatAsyncResource.commitOperation();
                    return null;
                }
            });

            if (_commitExecutor == null) {
                _commitExecutor = Executors.newScheduledThreadPool(0);
            }
            _commitExecutor.execute(_commitResult);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sendAsyncCommit");
    }

    @Override
    public void sendAsyncPrepare() throws XAException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sendAsyncPrepare", this);

        if (_asyncState == ASYNC_STATE_ACTIVE) {
            _prepareResult = new FutureTask<Integer>(new Callable<Integer>() {
                @Override
                public Integer call() throws XAException, XAResourceNotAvailableException {
                    return _wsatAsyncResource.prepareOperation();
                }
            });

            if (_prepareExecutor == null) {
                _prepareExecutor = Executors.newScheduledThreadPool(0);
            }
            _prepareExecutor.execute(_prepareResult);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sendAsyncPrepare", XAResource.XA_RDONLY);
    }

    @Override
    public void sendAsyncRollback() throws XAException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sendAsyncRollback", this);

        if (_asyncState != ASYNC_STATE_ABORTED && _asyncState != ASYNC_STATE_COMMITTED)
        {
            _rollbackResult = new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() throws XAException, XAResourceNotAvailableException {
                    _wsatAsyncResource.rollbackOperation();
                    return null;
                }
            });

            if (_rollbackExecutor == null) {
                _rollbackExecutor = Executors.newScheduledThreadPool(0);
            }

            _rollbackExecutor.execute(_rollbackResult);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sendAsyncRollback");
    }

    @Override
    public void start() throws XAException
    {}

    @Override
    public void end(int flag) throws XAException
    {}

    @Override
    public void forget() throws XAException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forget", this);

        _forgetResult = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws XAException, XAResourceNotAvailableException {
                _wsatAsyncResource.forgetOperation();
                return null;
            }
        });

        ExecutorService forgetExecutor = Executors.newScheduledThreadPool(0);
        forgetExecutor.execute(_forgetResult);
        forgetExecutor.shutdown();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forget");
    }

    @Override
    public Xid getXID()
    {
        return null;
    }

    @Override
    public int getState()
    {
        return -1;
    }

    // Return default priority
    @Override
    public int getPriority()
    {
        return DEFAULT_COMMIT_PRIORITY;
    }

    @Override
    public XAResource XAResource()
    {
        return null;
    }

    @Override
    public void destroy()
    {}

    @Override
    public void setState(int in)
    {}

    @Override
    public void log(RecoverableUnitSection rus) throws javax.transaction.SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "log", new Object[] { rus, this });

        // Log the WSATAsyncResource by serializing it

        try
        {
            rus.addData(_wsatAsyncResource.toLogData());
        } catch (Exception exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.Transaction.wstx.WSATParticipantWrapper.log", "409", this);

            if (tc.isEventEnabled())
                Tr.event(tc, "Exception raised adding data to the transaction log", exc);

            final SystemException se = new SystemException();
            se.initCause(exc);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "log", se);
            throw se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "log");
    }

    public byte[] toLogData() throws javax.transaction.SystemException /* @LIDB1922-5AA */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toLogData", this);

        byte[] data = null;
        try
        {
            data = _wsatAsyncResource.toLogData();
        } catch (Exception exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.Transaction.wstx.WSATParticipantWrapper.toLogData", "448", this);

            if (tc.isEventEnabled())
                Tr.event(tc, "Exception raised serializing participant data", exc);

            final SystemException se = new SystemException();
            se.initCause(exc);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "toLogData", se);
            throw se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toLogData");
        return data;
    } /* @LIDB1922-5A */

    @Override
    public Serializable getKey()
    {
        return _wsatAsyncResource.getKey(); // this is not right yet
    }

    @Override
    public String describe()
    {
        return _wsatAsyncResource.describe();
    }

    @Override
    public String toString()
    {
        return describe();
    }
}

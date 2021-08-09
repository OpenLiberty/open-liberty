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

import java.io.Serializable;

import javax.transaction.xa.XAException;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.JTA.JTAResource;
import com.ibm.ws.Transaction.JTA.ResourceWrapper;

public abstract class JTAAsyncResourceBase extends ResourceWrapper implements JTAResource
{
    private static final TraceComponent tc = Tr.register(JTAAsyncResourceBase.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public final static int ASYNC_STATE_ACTIVE = 0;
    public final static int ASYNC_STATE_READONLY = 1;
    public final static int ASYNC_STATE_PREPARED = 2;
    public final static int ASYNC_STATE_ABORTED = 3;
    public final static int ASYNC_STATE_COMMITTED = 4;
    public final static int ASYNC_STATE_HEURROLLBACK = 5;
    public final static int ASYNC_STATE_HEURCOMMIT = 6;
    public final static int ASYNC_STATE_HEURMIXED = 7;
    public final static int ASYNC_STATE_HEURHAZARD = 8;
    public final static int ASYNC_STATE_LAST = 8;
//   
//    protected Semaphore _semaphore;
//
    protected int _asyncState = ASYNC_STATE_ACTIVE;
    protected boolean _stateProcessed;

    final static boolean[][] stateSuperceded = {
                                                /* from to actve rdonly pd rd cd hr hc hm hh */
/* active */                                    { false, true, true, true, false, false, false, true, true },
                                                /* readonly */{ false, false, false, false, false, false, false, false, false },
                                                /* prepared */{ false, false, false, true, true, true, true, true, true },
                                                /* aborted */{ false, false, false, false, false, false, false, false, false },
                                                /* committed */{ false, false, false, false, false, false, false, false, false },
                                                /* heurrollback */{ false, false, false, false, false, false, false, false, false },
                                                /* heurcommit */{ false, false, false, false, false, false, false, false, false },
                                                /* heurmixed */{ false, false, false, false, false, false, false, false, false },
                                                /* heurhazard */{ false, false, false, false, false, false, false, false, false } };

//    public void setResponse(int newState)
//    {
//        if (tc.isEntryEnabled()) Tr.entry(tc, "setResponse", new Object[]{new Integer(newState), this});
//
//        if((newState< ASYNC_STATE_ACTIVE) || (newState > ASYNC_STATE_LAST))
//        {
//            if (tc.isEventEnabled()) Tr.event(tc, "invalid state:"+newState);
//        }
//        else
//            {
//            // Synchronize on Semaphore to avoid possibility of sending messages to this resource for
//            // a different 2PC phase whilst processing this response.
//            synchronized(_semaphore)
//            {
//            	if (tc.isDebugEnabled()) Tr.debug(tc, "_asyncState = " + _asyncState);
//            	
//                if(stateSuperceded[_asyncState][newState])
//                {
//                    _asyncState = newState;
//                    _stateProcessed = false;
//                    // need to change so that decrement only occurs if response is for correct phase.
//                    _semaphore.decrement();
//                }
//            }
//        }
//
//        if ( tc.isEntryEnabled() ) Tr.exit(tc, "setResponse");
//    }
//
//    public void setImmediateResponse(int newState)
//    {
//        if (tc.isEntryEnabled()) Tr.entry(tc, "setImmediateResponse", new Object[]{new Integer(newState), this});
//
//        if((newState< ASYNC_STATE_ACTIVE) || (newState > ASYNC_STATE_LAST))
//        {
//            if ( tc.isEventEnabled() ) Tr.event(tc, "invalid state:"+newState);
//        }
//        else
//        {
//            // Synchronize on Semaphore to avoid possibility of sending messages to this resource for
//            // a different 2PC phase whilst processing this response.
//            synchronized(_semaphore)
//            {
//            	if (tc.isDebugEnabled()) Tr.debug(tc, "_asyncState = " + _asyncState);
//            	
//                if( stateSuperceded[_asyncState][newState] )
//                {
//                    _asyncState = newState;
//                    _stateProcessed = false;
//                    _semaphore.decrement();
//                    _semaphore.force();
//                }
//            }
//        }
//
//        if ( tc.isEntryEnabled() ) Tr.exit(tc, "setImmediateResponse");
//    }
//
//    public void setSemaphore(Semaphore semaphore)
//    {
//        if (tc.isEntryEnabled()) Tr.entry(tc, "setSemaphore", new Object[]{semaphore, this});
//
//        _semaphore = semaphore;
//
//        if (tc.isEntryEnabled()) Tr.exit(tc, "setSemaphore");
//    }
//
//    public Semaphore getSemaphore()
//    {
//        if (tc.isEntryEnabled()) Tr.entry(tc, "getSemaphore", this);
//        if (tc.isEntryEnabled()) Tr.exit(tc, "getSemaphore", _semaphore);
//        return _semaphore;
//    }

    abstract public void sendAsyncPrepare() throws XAException;

    abstract public void sendAsyncCommit() throws XAException;

    abstract public void sendAsyncRollback() throws XAException;

    abstract public Serializable getKey();

}

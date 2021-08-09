/*******************************************************************************
 * Copyright (c) 2002, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.ltc.embeddable.impl;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeCallbackManager;
import com.ibm.wsspi.tx.UOWEventListener;

/**
 * This class provides a way for Resource Manager Local Transactions (RMLTs)
 * accessed from an EJB or web component to be coordinated or contained within a
 * local transaction containment (LTC) scope. The LTC is what WebSphere provides
 * in the place of the <i>unspecified transaction context</i> described by the
 * EJB specification.
 * RMLTs are enlisted either to be coordinated by the LTC according to an external
 * signal or to be cleaned up at LTC end in the case that the application fails
 * in its duties.
 * The LocalTransactionCoordinator encapsulates details of local transaction
 * boundary and scopes itself either to the method invocation or ActivitySession.
 */
public class EmbeddableLocalTranCurrentImpl extends com.ibm.tx.ltc.impl.LocalTranCurrentImpl 
{
    private static final TraceComponent tc = Tr.register(EmbeddableLocalTranCurrentImpl.class, TranConstants.TRACE_GROUP, TranConstants.LTC_NLS_FILE);

    protected UOWScopeCallbackManager _callbackManager;

    protected EmbeddableLocalTranCurrentImpl(UOWScopeCallbackManager callbackManager)
    {
        _callbackManager = callbackManager;
    }

    @Override
    public void begin(boolean boundaryIsAS) throws IllegalStateException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "begin", new Object[]{boundaryIsAS, this});

        // This check was previously commented out as this is a system
        // interface and we should never fall down this path.  If that's
        // still a concern (note that the thread local game is gone), feel
        // free to comment it back out.  The z/OS implementation will die
        // a horrible death if there really is a context on the thread.
        if (false && globalTranExists())
        {
            IllegalStateException ise = new IllegalStateException("Cannot begin a LocalTransactionCoordinator. A Global transaction is active.");
            Tr.error(tc, "ERR_BEGIN_TX_GLB_ACT");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "begin", ise);
            throw ise;
        }
 
        if (_coord != null)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot begin a LocalTransactionContainment. A LocalTransactionContainment is already active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.impl.EmbeddableLocalTranCurrentImpl.begin", "76", this);
            Tr.error(tc, "ERR_BEGIN_LTC_ACT");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "begin", ise);
            throw ise;
        }

        try
        {
            _coord = new EmbeddableLocalTranCoordImpl(boundaryIsAS, this);
            
            try
            {
                _callbackManager.notifyCallbacks(UOWScopeCallback.POST_BEGIN, _coord);
            }
            catch (IllegalStateException ise)
            {
                FFDCFilter.processException(ise, "com.ibm.tx.ltc.impl.EmbeddableLocalTranCurrentImpl.begin", "93", this);
                _coord.setRollbackOnly();               
            }
        }
        finally
        {
            invokeEventListener(_coord, UOWEventListener.POST_BEGIN, null);
            // Exception logging/reporting performed by LocalTranCoordImpl constructor
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "begin");
        }
    }

    /**
       * Starts a new LTC  scope and associates it with the current thread.
       * The configuration of the LTC is determined by the caller rather than via J2EE component
       * metadata.
       *
       * @param boundaryIsAs true if the boundary is ActivitySession; false if the boundary is BeanMethod
       *
       * @param unresActionIsCommit true if the unresolved action is commit; false if it is rollback
       *
       * @param resolvedIsCAB true if the resolver is ContainerAtBoundary; false if it is Application
       *
       * @exception IllegalStateException
       *                   Thrown if the LocalTransactionCoordinator is not in a
       *                   valid state to execute the operation, for example if
       *                   a global transaction is active.
       *
       */
    @Override
    public void begin(boolean boundaryIsAS, boolean unresActionIsCommit,
                      boolean resolverIsCAB) throws IllegalStateException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(tc, "begin", new Object[]{boundaryIsAS, unresActionIsCommit, resolverIsCAB, this});

        // This check was previously commented out as this is a system
        // interface and we should never fall down this path.  If that's
        // still a concern (note that the thread local game is gone), feel
        // free to comment it back out.  The z/OS implementation will die
        // a horrible death if there really is a context on the thread.
        if (false && globalTranExists())
        {
            IllegalStateException ise = new IllegalStateException("Cannot begin a LocalTransactionCoordinator. A Global transaction is active.");
            Tr.error(tc, "ERR_BEGIN_TX_GLB_ACT");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "begin", ise);
            throw ise;
        }
 
        if (_coord != null)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot begin a LocalTransactionContainment. A LocalTransactionContainment is already active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.impl.EmbeddableLocalTranCurrentImpl.begin", "142", this);
            Tr.error(tc, "ERR_BEGIN_LTC_ACT");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "begin", ise);
            throw ise;
        }

        try
        {
            _coord = new EmbeddableLocalTranCoordImpl(boundaryIsAS, unresActionIsCommit, resolverIsCAB, this);
            
            try
            {
                _callbackManager.notifyCallbacks(UOWScopeCallback.POST_BEGIN, _coord);
            }
            catch (IllegalStateException ise)
            {
                FFDCFilter.processException(ise, "com.ibm.tx.ltc.impl.EmbeddableLocalTranCurrentImpl.begin", "158", this);
                _coord.setRollbackOnly();               
            }
        }
        finally
        {
            invokeEventListener(_coord, UOWEventListener.POST_BEGIN, null);

            // Exception logging/reporting performed by LocalTranCoordImpl constructor
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "begin");
        }
    }
}
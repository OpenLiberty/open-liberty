/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.ltc.impl;

import java.util.HashSet;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.ws.LocalTransaction.*;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.wsspi.tx.UOWEventListener;
import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

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
public class LocalTranCurrentImpl 
{
    static protected TransactionManager _currentTM = TransactionManagerFactory.getTransactionManager();

	private static HashSet<UOWEventListener> _UOWEventListeners;

    // static void setCurrentTM(TransactionManager tm) {_currentTM = tm; }

    /**
     * Local transaction coordinator that is associated with this instance
     * of <code>LocalTranCurrentImpl</code>.
     */
    protected LocalTranCoordImpl _coord;

    private static final TraceComponent tc = Tr.register(LocalTranCurrentImpl.class, TranConstants.TRACE_GROUP, TranConstants.LTC_NLS_FILE);

    protected LocalTranCurrentImpl()
    {
    }

    /**
     * Gets the LocalTransactionCoordinator instance, if any, associated
     * with the current thread.
     *
     * @return the current LocalTransactionCoordinator instance. If there is a
     *          global transaction associated with the thread, returns null.
     */
    public LocalTransactionCoordinator getLocalTranCoord()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "getLocalTranCoord", _coord);
        return _coord;
    }


    /**
       * Starts a new LTC  scope and associates it with the current thread.
       * Connection-related LTC properties are determined from J2EE metadata once a connection is enlisted.
       *
       * @param boundaryIsAs true if the boundary is ActivitySession; false if the boundary is BeanMethod
       *
       * @exception IllegalStateException
       *                   Thrown if the LocalTransactionCoordinator is not in a
       *                   valid state to execute the operation, for example if
       *                   a global transaction is active.
       *
       */
    public void begin() throws IllegalStateException
    {
        begin(false);
    }

    public void begin(boolean boundaryIsAS) throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "begin", new Object[]{boundaryIsAS, this});
 
        if (_coord != null)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot begin a LocalTransactionContainment. A LocalTransactionContainment is already active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCurrentImpl.begin", "141", this);
            Tr.error(tc, "ERR_BEGIN_LTC_ACT");
            if (tc.isEntryEnabled()) Tr.exit(tc, "begin", ise);
            throw ise;
        }

        try
        {
            _coord = new LocalTranCoordImpl(boundaryIsAS, this);
        }
        finally
        {
            invokeEventListener(_coord, UOWEventListener.POST_BEGIN, null);

            // Exception logging/reporting performed by LocalTranCoordImpl constructor
            if (tc.isEntryEnabled()) Tr.exit(tc, "begin");
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
    public void begin(boolean boundaryIsAS, boolean unresActionIsCommit,
                      boolean resolverIsCAB) throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "begin", new Object[]{boundaryIsAS, unresActionIsCommit, resolverIsCAB, this});
 
        if (_coord != null)
        {
            final IllegalStateException ise = new IllegalStateException("Cannot begin a LocalTransactionContainment. A LocalTransactionContainment is already active.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCurrentImpl.begin", "205", this);
            Tr.error(tc, "ERR_BEGIN_LTC_ACT");
            if (tc.isEntryEnabled()) Tr.exit(tc, "begin", ise);
            throw ise;
        }

        try
        {
            _coord = new LocalTranCoordImpl(boundaryIsAS, unresActionIsCommit, resolverIsCAB, this);
            
        }
        finally
        {
        	invokeEventListener(_coord, UOWEventListener.POST_BEGIN, null);

        	// Exception logging/reporting performed by LocalTranCoordImpl constructor
            if (tc.isEntryEnabled()) Tr.exit(tc, "begin");
        }
    }

    public void beginShareable(boolean boundaryIsAS, boolean unresActionIsCommit,
                      boolean resolverIsCAB) throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "beginShareable");
        begin(boundaryIsAS, unresActionIsCommit, resolverIsCAB);
        _coord.setShareable(true);
        if (tc.isEntryEnabled()) Tr.exit(tc, "beginShareable");
    }

    /**
     * Disassociates the LTC scope from the thread.
     *
     * @return the LocalTransactionCoordinator instance representing the LTC scope
     *    disassociated from the thread. If there was no LTC scope, returns null.
     *
     */
    public LocalTransactionCoordinator suspend()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "suspend", this);

        final LocalTranCoordImpl ltc = _coord;
        if (ltc != null)
        {
            _coord = null;
            ltc.suspend();           // suspend any native context
            
            invokeEventListener(ltc, UOWEventListener.SUSPEND, null);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "suspend", ltc);
        return ltc;
    }

    /**
     * Associates an LTC scope with the thread. Any existing LTC is suspended first.
     *
     * @param ltc    The LocalTransactionCoordinator instance that represents the
     *               LTC scope to be resumed. If a null is specified, then no LTC is associated
     *               with the thread.
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active.
     */
    public void resume(LocalTransactionCoordinator ltc) // throws IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "resume", new Object[]{ltc, this});
        
        // If there's already an LTC on this thread and the LTC that we're
        // resuming is non-null then fail the resume attempt. This mirrors
        // the resume behaviour of the JTA code and also prevents problems
        // on Z when we attempt a resume when there's already context on
        // the thread; it dies a horrible death.
        if (ltc != null && _coord != null)
        {
        	// We don't output a message here as manipulation of LTCs is only ever
        	// done by system level code - there's nothing a user can do to 
        	// prevent this problem from occurring
        	final IllegalStateException ise = new IllegalStateException();
        	if (tc.isEntryEnabled()) Tr.exit(tc, "resume", ise);
        	throw ise;
        }
 
        _coord = (LocalTranCoordImpl) ltc;
        if (_coord != null)
        {
        	_coord.resume(this);           // resume any native context

        	invokeEventListener(_coord, UOWEventListener.RESUME, null);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
    }

	/**
     * Returns a boolean to indicate whether there are incomplete RMLTs in the
     * current LTC.
     *
     * @return true if there is an LTC associated with the thread that has resources
     *    enlisted either for cleanup or for coordination. Under these circumstances,
     *    it would be illegal for the bean with which the LTC was associated
     *    to start a global transaction.
     *
     */
    public boolean hasOutstandingWork()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "hasOutstandingWork", this);

        final boolean retval = (_coord != null) ? _coord.hasWork() : false;

        if (tc.isEntryEnabled()) Tr.exit(tc, "hasOutstandingWork", retval);
        return retval;
    }

    /*
     * Checks whether a Global Transaction exists on the
     * current thread.
     *
     * @return <UL>
     *         <LI>true - If a global transaction exists</LI>
     *         <LI>false - If a global transaction doesn't exist</LI>
     *         </UL>
     * @since 1.0
     */
    protected static boolean globalTranExists()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "globalTranExists");

        boolean retval = false;

        try
        {
            if (_currentTM != null)
                retval = _currentTM.getTransaction() != null;
        }
        catch (SystemException se)
        {
            // No FFDC needed as our implementation will never throw exception
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "globalTranExists", retval);
        return retval;
    }


    /**
     * Completes all <CODE>OnePhaseXAResource</CODE> objects that have
     * been enlisted, via the enlist() method, with the LocalTransactionCoordinator
     * associated with the current thread.
     * Ends the association of the LTC scope with the thread.
     *
     * @param endMode The action to be taken when completing the OnePhaseXAResources enlisted
     *                with the coordinator. Possible values are:
     *
     *                <UL>
     *                <LI>EndModeCommit</LI>
     *                <LI>EndModeRollBack</LI>
     *                </UL>
     * @exception InconsistentLocalTranException
     *                   Thrown when completion of a resource fails leaving the local
     *                   transaction containment in an inconsistent state.
     * @exception RolledbackException
     *                   Thrown if EndModeCommit is specified but the LTC has been marked
     *                   RollbackOnly. Any enlisted resources are rolled back.
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   the LocalTransactionCoordinator has already completed
     *                   or if there is no LocalTransactionCoordinator associated
     *                   with the current thread.
     */
    public void complete(int endMode) throws InconsistentLocalTranException, RolledbackException, IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "complete", new Object[]{endMode, this});

        if (_coord == null)
        {
            final IllegalStateException ise = new IllegalStateException("No LocalTransactionCoordinator to complete.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCurrentImpl.complete", "382", this);
            Tr.error(tc, "ERR_NO_LTC_COMPLETE");
            if (tc.isEntryEnabled()) Tr.exit(tc, "complete", ise);
            throw ise;
        }
        try
        {
            _coord.complete(endMode);
        }
        finally
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "complete");
        }
    }

    /**
     * Cleans up all <CODE>OnePhaseXAResource</CODE> objects that have
     * been enlisted, via the enlistForCleanup() method, with the
     * LocalTransactionCoordinator associated with the current thread.
     * The direction in which resources are completed during <code>cleanup</code>
     * is determined from the unresolved-action DD.
     *
     * @exception InconsistentLocalTranException
     *                   Thrown when completion of a resource fails leaving the local
     *                   transaction containment in an inconsistent state.
     * @exception RolledbackException
     *                   Thrown if unresolved-action is COMMIT but the LTC has been marked
     *                   RollbackOnly. Any enlisted resources are rolled back.
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   the LocalTransactionCoordinator has already completed
     *                   or if there is no LocalTransactionCoordinator associated
     *                   with the current thread.
     */
    public void cleanup() throws InconsistentLocalTranException, IllegalStateException,RolledbackException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "cleanup", this);

        if (_coord == null)
        {
            final IllegalStateException ise = new IllegalStateException("No LocalTransactionCoordinator to cleanup.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCurrentImpl.cleanup", "421", this);
            Tr.error(tc, "ERR_NO_LTC_CLEANUP");
            if (tc.isEntryEnabled()) Tr.exit(tc, "cleanup", ise);
            throw ise;
        }

        try
        {
            _coord.cleanup();
        }
        finally
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "cleanup");
        }
    }


    /**
     * Ends the LTC in a manner consistent with the resolver type - if the LTC is configured as
     * being resolved by ContainerAtBoundary then the LTC is ended as described by the
     * {@link #complete(int) complete}, if the LTC is configured as
     * being resolved by Application then the LTC is ended as described by the
     * {@link #cleanup cleanup}.
     * Ends the association of the LTC scope with the thread.
     *
     * @param endMode The action to be taken when completing the OnePhaseXAResources enlisted
     *                with the coordinator. Possible values are:
     *
     *                <UL>
     *                <LI>EndModeCommit</LI>
     *                <LI>EndModeRollBack</LI>
     *                </UL>
     * @exception InconsistentLocalTranException
     *                   Thrown when completion of a resource fails leaving the local
     *                   transaction containment in an inconsistent state.
     * @exception RolledbackException
     *                   Thrown if EndModeCommit is specified but the LTC has been marked
     *                   RollbackOnly. Any enlisted resources are rolled back.
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   the LocalTransactionCoordinator has already completed
     *                   or if there is no LocalTransactionCoordinator associated
     *                   with the current thread.
     */
    public void end(int endMode) throws InconsistentLocalTranException, RolledbackException, IllegalStateException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "end", new Object[]{endMode, this});

        if (_coord == null)
        {
            final IllegalStateException ise = new IllegalStateException("No LocalTransactionCoordinator to end.");
            FFDCFilter.processException(ise, "com.ibm.tx.ltc.LocalTranCurrentImpl.end", "469", this);
            Tr.error(tc, "ERR_NO_LTC_CLEANUP");
            if (tc.isEntryEnabled()) Tr.exit(tc, "end", ise);
            throw ise;
        }

        // LTC is gonna get taken off the thread as part of end processing so save a reference here
        final UOWCoordinator endingLTC = _coord;

        try
        {
            _coord.end(endMode);
        }
        finally
        {
        	if (tc.isEntryEnabled()) Tr.exit(tc, "end");
        }
    }

    /**
     * Setter for the _coord member var.
     */
    void setCoordinator(LocalTranCoordImpl ltc)
    {
        _coord = ltc;
        if (tc.isDebugEnabled()) Tr.debug(tc, "setCoordinator: LTC="+ltc);
    }


   /**
    * Return the current UOW coordinator (either local or global)
    */
    public UOWCoordinator getUOWCoord()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "getUOWCoord", this);
 
        UOWCoordinator  uow = (UOWCoordinator)_coord;
        if (uow == null)
        {
            // see if there is a global tran ... 
            if (_currentTM != null)
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "getting possible global transaction", _currentTM);
                try 
                {
                    uow = (UOWCoordinator) _currentTM.getTransaction();
                }
                catch (SystemException se)
                {
                   // No FFDC needed as our implementation will never throw exception
                }
            }
        }
 
        if (tc.isEntryEnabled()) Tr.exit(tc, "getUOWCoord", uow);
        return uow;
    }

    public static synchronized void setUOWEventListener(UOWEventListener el)
    {
    	if (tc.isDebugEnabled()) Tr.debug(tc, "setUOWEventListener", el);

    	if (_UOWEventListeners == null)
    	{
    		_UOWEventListeners = new HashSet<UOWEventListener>();
    	}
    	
    	_UOWEventListeners.add(el);
    }

    public static synchronized void unsetUOWEventListener(UOWEventListener el)
    {
    	if (tc.isDebugEnabled()) Tr.debug(tc, "unsetUOWEventListener", el);
    	
    	if (_UOWEventListeners != null)
    	{
    		_UOWEventListeners.remove(el);
    	}
    }

    protected synchronized void invokeEventListener(UOWCoordinator uowc, int event, Object data)
    {
    	if (_UOWEventListeners != null)
    	{
    		if (tc.isDebugEnabled()) Tr.debug(tc, "invokeEventListener", new Object[]{uowc, event, data});
    		
    		for (UOWEventListener el : _UOWEventListeners)
    		{
    			el.UOWEvent(uowc, event, data);
    		}
    	}
	}
}
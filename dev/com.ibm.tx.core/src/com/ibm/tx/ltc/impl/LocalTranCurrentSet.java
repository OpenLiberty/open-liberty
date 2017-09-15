/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.ltc.impl;

import com.ibm.tx.util.TMService;
import com.ibm.ws.LocalTransaction.InconsistentLocalTranException;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.LocalTransaction.RolledbackException;
import com.ibm.ws.Transaction.UOWCallback;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeCallbackAgent;
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

public class LocalTranCurrentSet implements LocalTransactionCurrent, UOWCurrent
{
    protected static ThreadLocal<LocalTranCurrentImpl> _context;


    /**
     * Single instance
     */
    protected static LocalTranCurrentSet _instance;

    static
    {
        _context = new ThreadLocal<LocalTranCurrentImpl>()
        {
            protected LocalTranCurrentImpl initialValue() { return new LocalTranCurrentImpl(); }
        };

        _instance = new LocalTranCurrentSet();
    }

    /**
     * Private constructor used to create single instance.
     * Need to allow subclasses, so change to protected ...
     * ... for osgi DS, need to be public (no need to use 'instance')
     */
    public LocalTranCurrentSet() {}

    protected void setTMService (TMService tm)
    {    
        // dependency injection ... forces tran service to initialize
        // extract the TM and set in LocalTranCurrentImpl
        // LocalTranCurrentImpl.setCurrentTM(TransactionManagerFactory.getTransactionManager());  
    }

    protected void unsetTMService (TMService tm)
    {  
        // clear the TMService ref in LocalTranCurrentImpl  
        // LocalTranCurrentImpl.setCurrentTM(null);  
    }

    protected void setUOWScopeCallbackAgent(UOWScopeCallbackAgent cb)
    {
        cb.registerCallback(LTCUOWCallback.getUserTransactionCallback());
    }

    protected void unsetUOWScopeCallbackAgent(UOWScopeCallbackAgent cb)
    {
        cb.unregisterCallback(LTCUOWCallback.getUserTransactionCallback());
    }

   /**
     * Static instance method to return single instance
     */
    public static LocalTranCurrentSet instance()
    {
        return _instance;
    }

    public LocalTranCurrentImpl self()
    {
        return _context.get();
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
        return self().getLocalTranCoord();
    }


    /**
     * Starts a new LTC  scope and associates it with
     * the current thread.
     *
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active.
     *
     */
    public void begin() throws IllegalStateException
    {
        self().begin(false);
    }

    public void begin(boolean boundaryIsAS) throws IllegalStateException
    {
        self().begin(boundaryIsAS);
    }

    public void begin(boolean boundaryIsAS, boolean unresActionIsCommit,
                      boolean resolverIsCAB) throws IllegalStateException
    {
        self().begin(boundaryIsAS,unresActionIsCommit,resolverIsCAB);
    }

    public void beginShareable(boolean boundaryIsAS, boolean unresActionIsCommit,
                      boolean resolverIsCAB) throws IllegalStateException
    {
        self().beginShareable(boundaryIsAS,unresActionIsCommit,resolverIsCAB);
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
        return self().suspend();
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
    public void resume(LocalTransactionCoordinator ltc) throws IllegalStateException
    {
        self().resume(ltc);
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
        return self().hasOutstandingWork();
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
        self().complete(endMode);
    }

    /**
     * Cleans up all <CODE>OnePhaseXAResource</CODE> objects that have
     * been enlisted, via the enlistForCleanup() method, with the
     * LocalTransactionCoordinator associated with the current thread.
     . The direction in which resources are completed during <code>cleanup</code>
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
    public void cleanup() throws InconsistentLocalTranException, IllegalStateException,
                                 RolledbackException
    {
        self().cleanup();
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
        self().end(endMode);
    }

    /**
     * Sets the coordinator value of the corresponding LocalTranCurrentImpl.
     */
    void setCoordinator(LocalTranCoordImpl ltc)
    {
        self().setCoordinator(ltc);
    }

    public void registerCallback(UOWScopeCallback callback)
    {
        // no support for callbacks yet
    }

    //
    // UOWCurrent interface
    //
    public UOWCoordinator getUOWCoord()
    {
        return self().getUOWCoord();
    }
 
    public int getUOWType()
    { 
        //if (tc.isEntryEnabled()) Tr.entry(tc, "getUOWType");
 
        final UOWCoordinator uowCoord = getUOWCoord();
 
        int result = UOWCurrent.UOW_NONE;
 
        if (uowCoord != null)
        {
            result = uowCoord.isGlobal() ? UOWCurrent.UOW_GLOBAL : UOWCurrent.UOW_LOCAL;
        }
 
        // if (tc.isEntryEnabled()) Tr.exit(tc, "getUOWType", result);
        return result;
    }    
 
    public void registerLTCCallback(UOWCallback callback)
    { 
        // if (tc.isEntryEnabled()) Tr.entry(tc, "registerLTCCallback", callback);
 
        // No support for LTCCallbacks ... provided by derived classes
 
        // if (tc.isEntryEnabled()) Tr.exit(tc, "registerLTCCallback");
    }

	@Override
	public void setUOWEventListener(UOWEventListener el)
	{
		LocalTranCurrentImpl.setUOWEventListener(el);
	}

	@Override
	public void unsetUOWEventListener(UOWEventListener el)
	{
		LocalTranCurrentImpl.unsetUOWEventListener(el);
	}
}
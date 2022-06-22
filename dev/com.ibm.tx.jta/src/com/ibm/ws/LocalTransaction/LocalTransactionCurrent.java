/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.LocalTransaction;

import com.ibm.ws.uow.UOWScopeCallback;

/**
 * 
 * <p> This class is private to WAS.
 * Any use of this class outside the WAS Express/ND codebase 
 * is not supported.
 *
 */
public interface LocalTransactionCurrent 
{
    public static final int EndModeCommit   = 0;
    public static final int EndModeRollBack = 1;

    /**
     * Gets the LocalTransactionCoordinator instance, if any, associated
     * with the current thread.
     *
     * @return the current LocalTransactionCoordinator instance. If there is a
     *          global transaction associated with the thread, returns null.
     */
    public LocalTransactionCoordinator getLocalTranCoord();

    /**
     * Starts a new bean-method-scoped LTC and associates it with
     * the current thread. Connection-related LTC properties are determined from the J2EE metadata
     * once a connection is enlisted. 
     *
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active.
     *
     *
     */
    public void begin() throws IllegalStateException;

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
     *
     */
    public void begin(boolean boundaryIsAS) throws IllegalStateException;

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
     *
     */
    public void begin(boolean boundaryIsAS, boolean unresActionIsCommit,
                      boolean resolverIsCAB) throws IllegalStateException;

    /**
     * Starts a new SHAREABLE LTC  scope and associates it with the current thread.
     * The configuration of the LTC is determined by the caller rather than via
     * J2EE component  metadata.
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

    public void beginShareable(boolean boundaryIsAS, boolean unresActionIsCommit,
                      boolean resolverIsCAB) throws IllegalStateException;

    /**
     * Disassociates the LTC scope from the thread.
     *
     * @return the LocalTransactionCoordinator instance representing the LTC scope
     *    disassociated from the thread. If there was no LTC scope, returns null.
     *
     */
    public LocalTransactionCoordinator suspend();

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
     *
     *
     */
    public void resume(LocalTransactionCoordinator ltc) throws IllegalStateException;

    /**
     * Returns a boolean to indicate whether there are incomplete RMLTs in the
     * current LTC.
     *
     * @return true if there is an LTC associated with the thread that has resources
     *    enlisted either for cleanup or for coordination. Under these circumstances,
     *    it would be illegal for the bean with which the LTC was associated
     *    to start a global transaction.
     *
     *
     */
    public boolean hasOutstandingWork();

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
     *
     *
     */
    public void complete(int endMode) throws InconsistentLocalTranException, RolledbackException, IllegalStateException;

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
     *
     *
     */
    public void cleanup() throws InconsistentLocalTranException, IllegalStateException,
                               RolledbackException;

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
     *
     *
     */
    public void end(int endMode) throws InconsistentLocalTranException, RolledbackException, IllegalStateException;
    
    /**
     * Registers the given callback for notification of local
     * transaction POST_BEGIN and PRE_END.
     * 
     * @param callback The callback to register for notification
     */
    public void registerCallback(UOWScopeCallback callback);
}

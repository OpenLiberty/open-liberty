package com.ibm.ws.LocalTransaction;
/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.tx.jta.OnePhaseXAResource;

/**
 * 
 * <p> This interface is private to WAS.
 * Any use of this interface outside the WAS Express/ND codebase 
 * is not supported.
 * 
 */
public interface LocalTransactionCoordinator 
{

   static final public int EndModeCommit   = 0;
   static final public int EndModeRollBack = 1;


  /**
   * Enlists the provided <CODE>resource</CODE>
   * object with the target <CODE>LocalTransactionCoordinator</CODE> in order
   * that the resource be coordinated by the LTC.
   * The <code>resource</code> is called to <code>start</code> as part of the enlist
   * processing and will be called to <code>commit</code> or <code>rollback</code>
   * when the LTC completes.
   * The boundary at which the local transaction containment will
   * be completed is set at deployment time using the
   * <CODE>boundary</CODE> descriptor.
   *
   * <PRE>
   * &lt;local-transaction>
   *   &lt;boundary>ActivitySession|BeanMethod&lt;/boundary>
   * &lt;/local-transaction>
   * </PRE>
   *
   * @param resource The <CODE>OnePhaseXAResource</CODE> to coordinate
   * @exception IllegalStateException
   *                   Thrown if the LocalTransactionCoordinator is not in a
   *                   valid state to execute the operation, for example if
   *                   a global transaction is active or if a resource has been
   *                   elisted for <b>cleanup</b> in this LTC scope.
   */
  public void enlist(OnePhaseXAResource resource) throws IllegalStateException,LTCSystemException;

  /**
     * <P>Enlists the provided <CODE>resource</CODE> object with the target
     * <code>LocalTransactionCoordinator</code>
     * for cleanup. If the resource has not been completed by the application
     * component that started it before the local transaction containment boundary ends,
     * and {@link #delistFromCleanup delistFromCleanup} has not been called, then the
     * <CODE>LocalTransactionCoordinator</CODE>
     * will complete the <code>resource</code> using the direction configured in the
     * <CODE>unresolved-action</CODE> descriptor:
     *
     * <PRE>
     * &lt;local-transaction>
     *   &lt;unresolved-action>Commit|Rollback&lt;/unresolved-action>
     * &lt;/local-transaction>
     * </PRE>
     *
     * @param resource The <CODE>OnePhaseXAResource</CODE> to track
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active or if a resource has been
     *                   enlisted for coordination in this LTC scope.
     */
  public void enlistForCleanup(OnePhaseXAResource resource) throws IllegalStateException,LTCSystemException;


  /**
   * Removes the provided <CODE>resource</CODE> from the list of resources
   * that need to be cleaned-up when the LTC completes.
   * This method should be called when the application completes the RMLT.
   *
   * @param resource The <CODE>OnePhaseXAResource</CODE> to stop tracking
   * @exception IllegalStateException
   *                   Thrown if the LocalTransactionCoordinator is not in a
   *                   valid state to execute the operation, for example if
   *                   a global transaction is active.
   */
  public void delistFromCleanup(OnePhaseXAResource resource) throws IllegalStateException;

  /**
     * Enlist a Synchronization object that will be informed upon
     * completion of the local transaction containment boundary.
     *
     * @param sync   The Synchronization object to inform of local transaction containment
     *               completion
     * @exception IllegalStateException
     *                   Thrown if the LocalTransactionCoordinator is not in a
     *                   valid state to execute the operation, for example if
     *                   a global transaction is active.
     */
  public void enlistSynchronization(javax.transaction.Synchronization sync) throws IllegalStateException;

  /**
   * Completes all <CODE>OnePhaseXAResource</CODE> objects that have
   * been enlisted with the coordinator via the enlist() method,
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
   */
  public void complete(int endMode) throws InconsistentLocalTranException, RolledbackException, IllegalStateException;


  /**
   * Cleans up all <CODE>OnePhaseXAResource</CODE> objects that have
   * been enlisted with the coordinator via the enlistForCleanup()
   * method. The direction in which resources are completed during <code>cleanup</code>
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
   */
  public void end(int endMode) throws InconsistentLocalTranException, RolledbackException, IllegalStateException;

  /**
   * Returns a boolean to indicate whether the target LTC has been marked RollbackOnly.
   *
   * @return true if the target LTC has been marked RollbackOnly.
   *
   */
  public boolean getRollbackOnly();

  /**
   * Marks the target LocalTransactionCoordinator such that the LTC
   * will direct all enlisted resources to rollback regardless of the
   * completion endMode.
   * <br>If the LTC boundary is ActivitySession, then the ActivitySession
   * is marked ResetOnly.
   *
   */
  public void setRollbackOnly();

  /**
   * Indicates whether the LTC is scoped to an ActivitySession.  If true, the LTC
   * will be completed by the ActivitySession.
   */
  public boolean isASScoped();

  /**
   * Indicates whether the LTC is resolved by the container at boundary.  If true, the LTC
   * is resolved by the container, otherwise false if resolved by the application.
   */
  public boolean isContainerResolved();

  /**
   * Indicates whether the LTC is shareable between components.
   *
   */
  public boolean isShareable();
}

/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.uow;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;

/**
 * <p>
 * This interface provides functionality to execute application
 * logic under a specific unit of work (UOW). Application logic
 * is encapsulted in a UOWAction instance and is executed under
 * the specified type of UOW, either a new UOW or an existing
 * UOW. If the work is executed under a new UOW, then the
 * existing UOW is suspended for the duration of the new UOW and
 * resumed once the new UOW is completed. The UOW on the thread
 * before a <i>runUnderUOW</i> is always the same as the UOW on
 * the thread after completion of <i>runUnderUOW</i>.
 * </p>
 * 
 * <p>
 * This interface is intended for use by system level
 * application server components such as persistence managers,
 * resource adapters, as well as EJB and Web application
 * components.
 * </p>
 * 
 * <p>
 * This interface is implemented by the application server using
 * a stateless object. The same object can be used by any number
 * of components with thread safety.
 * </p>
 * 
 * <p>
 * An instance implementing this interface can be looked up via JNDI by using the
 * name <code>java:comp/websphere/UOWManager</code> when executed within a container
 * environment. If the UOWManager is required outside of a container managed
 * environment, then an instance can be obtained using UOWManagerFactory. Note that
 * UOWManager is only available in a server environment.
 * </p>
 * 
 * @ibm-spi
 */
public interface UOWManager extends UOWSynchronizationRegistry
{
    /**
     * <p>
     * Causes the work encapsulated by the uowAction <i>run</i>
     * method to be executed under the requested UOW. The UOW that
     * is requested is controlled by both the <i>uowType</i> and
     * <i>join</i> parameters.
     * </p>
     * 
     * <p>
     * The uowType parameter determines under what type of UOW the work will
     * be run, i.e. under a local transaction containment, a global
     * transaction, or an ActivitySession. If the current unit of
     * work is not of the requested type then the current unit of
     * work is suspended and a new UOW, of the requested type, is
     * begun. When a new UOW of type UOW_TYPE_ACTIVITYSESSION is
     * started, an associated container-resolved local transaction
     * containment is also started with a boundary that is scoped to
     * the ActivitySession. When a new UOW of type
     * UOW_TYPE_LOCAL_TRANSACTION is started, an
     * application-resolved local transaction containment is
     * started.
     * </p>
     * 
     * <p>
     * In the event of the current UOW being of the same type as the requested
     * UOW, the <i>join</i> parameter determines whether or not the
     * current UOW is joined. With a join parameter of
     * <code>true</code> the existing UOW will be used. With a join
     * parameter of <code>false</code> the existing UOW will be
     * suspended and a new UOW begun. Note that when requesting to
     * run under a local transaction the join parameter has no
     * effect and a new application-resolved local transaction
     * containment will always be begun.
     * </p>
     * 
     * <p>
     * Exceptions thrown by the given action's run method are handled as follows:
     * <ul>
     * <li>
     * Any checked exception is wrapped in a UOWActionException instance
     * which is then thrown to this method's caller and the UOW is unaffected, i.e.
     * it is not marked rollback only and it may be committed.
     * </li>
     * <li>
     * Any unchecked exception is passed to this method's caller as-is and the UOW
     * is marked rollback only.
     * </li>
     * </ul>
     * </p>
     * 
     * <p>
     * In the event of a new UOW being begun it will run for the duration of the
     * call to this method. Upon completion of this method's execution the new
     * UOW will be committed unless the action's run method marked the transaction
     * rollback-only or threw an unchecked exception, in which case the new UOW is
     * rolled back. Should the action's run method complete successfuly but the commit
     * processing fail unexpectedly, a UOWException will be thrown which will contain,
     * as its cause, the exception that caused the failure. In all cases the
     * previously suspended UOW is resumed.
     * </p>
     * 
     * <p>
     * In the event of the caller's UOW being joined then the transaction is never ended
     * as a result of processing the action's run method, although the UOW is marked
     * rollback-only if the action's run method throws an unchecked exception.
     * </p>
     * 
     * <p>
     * The action may be defined as an in-line anonymous class as follows. This example
     * illustrates performing some logic in the scope of a new global transaction:
     * <pre>
     * try {
     * uowManager.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction()
     * {
     * public void run() throws Exception {
     * // Perform transactional work here.
     * }
     * });
     * } catch (UOWActionException uowae) {
     * // Transactional work resulted in a checked exception being thrown.
     * // The UOW was not affected.
     * } catch (RuntimeException re) {
     * // Transactional work resulted in an unchecked exception being thrown.
     * // The UOW was rolled back
     * } catch (UOWException uowe) {
     * // The completion of the UOW failed unexpectedly.
     * }
     * </pre>
     * </p>
     * 
     * @param uowType The type of UOW to run the work under
     * @param join Whether the current UOW, if it is of the required type,
     *            should be joined or a new UOW must be begun.
     * @param uowAction The work to be executed under the requested UOW
     * 
     * @throws UOWActionException Thrown if the given action's run method threw a
     *             checked exception
     * @throws UOWException Thrown if completion of a new UOW
     *             fails unexpectedly.
     * 
     * @see UOWAction#run()
     * @see UOWSynchronizationRegistry#UOW_TYPE_ACTIVITYSESSION
     * @see UOWSynchronizationRegistry#UOW_TYPE_GLOBAL_TRANSACTION
     * @see UOWSynchronizationRegistry#UOW_TYPE_LOCAL_TRANSACTION
     */
    public void runUnderUOW(int uowType, boolean join, UOWAction uowAction) throws UOWActionException, UOWException;

    /**
     * Returns the total timeout, in seconds, as set when the current unit
     * of work was begun.
     * 
     * @throws IllegalStateException Thrown if no UOW is bound to the thread or if
     *             the type of UOW bound to the thread does not support timeout, e.g.
     *             UOW_TYPE_LOCAL_TRANSACTION
     * 
     * @return The current unit of work's initial timeout value in seconds.
     */
    public int getUOWTimeout() throws IllegalStateException;

    /**
     * Returns the time in milliseconds since the epoch
     * at which the current unit of work is set to timeout.
     * 
     * @throws IllegalStateException Thrown if no UOW is bound to the thread or if
     *             the type of UOW bound to the thread does not support timeout, e.g.
     *             UOW_TYPE_LOCAL_TRANSACTION
     * 
     * @return The time at which the current unit of work is set to timeout
     */
    public long getUOWExpiration() throws IllegalStateException;

    /**
     * Sets the timeout, in seconds, for the given UOW type to be used by
     * the current thread when programmatically beginning a new UOW. A value
     * of zero will restore the default timeout. For global transactions, this
     * default will be the total transaction timeout value configured on the
     * transaction service.
     * 
     * @param uowType The type of UOW for which the timeout is to be set
     * @param timeout The timeout, in seconds, for the UOW type
     * 
     * @throws IllegalArgumentException Thrown if the specified type of UOW
     *             does not support timeout, e.g. UOW_TYPE_LOCAL_TRANSACTION.
     * 
     * @see UOWSynchronizationRegistry#UOW_TYPE_ACTIVITYSESSION
     * @see UOWSynchronizationRegistry#UOW_TYPE_GLOBAL_TRANSACTION
     * @see UOWSynchronizationRegistry#UOW_TYPE_LOCAL_TRANSACTION
     */
    public void setUOWTimeout(int uowType, int timeout);

    /**
     * <p>
     * Causes the work encapsulated by the uowAction <i>run</i>
     * method to be executed under the requested UOW. The UOW that
     * is requested is controlled by both the <i>uowType</i> and
     * <i>join</i> parameters. This method returns the value
     * returned by the <i>run</i> method.
     * </p>
     * 
     * <p>
     * The uowType parameter determines under what type of UOW the work will
     * be run, i.e. under a local transaction containment or a global
     * transaction. If the current unit of work is not of the requested type
     * then the current unit of work is suspended and a new UOW, of the
     * requested type, is begun. When a new UOW of type
     * UOW_TYPE_LOCAL_TRANSACTION is started, an application-resolved local
     * transaction containment is started.
     * </p>
     * 
     * <p>
     * In the event of the current UOW being of the same type as the requested
     * UOW, the <i>join</i> parameter determines whether or not the
     * current UOW is joined. With a join parameter of
     * <code>true</code> the existing UOW will be used. With a join
     * parameter of <code>false</code> the existing UOW will be
     * suspended and a new UOW begun. Note that when requesting to
     * run under a local transaction the join parameter has no
     * effect and a new application-resolved local transaction
     * containment will always be begun.
     * </p>
     * 
     * <p>
     * Exceptions thrown by the given action's run method are handled as follows:
     * <ul>
     * <li>
     * Any checked exception is thrown to this method's caller. Unless the
     * exception is in the rollbackOn list the UOW is unaffected, i.e.
     * it is not marked rollback-only and it may be committed.
     * </li>
     * <li>
     * Any RuntimeException or Error is thrown to this method's caller and the
     * UOW is marked rollback-only unless it is in the dontRollbackOn list.
     * </li>
     * </ul>
     * </p>
     * 
     * <p>
     * In the event of a new UOW being begun it will run for the duration of the
     * call to this method. Upon completion of this method's execution the new
     * UOW will be committed unless the action's run method is committed unless
     * the run method threw something that caused the UOW to be marked rollback-only.
     * Any exception thrown by the commit processing is thrown to the caller. In all
     * cases the previously suspended UOW is resumed.
     * </p>
     * 
     * <p>
     * In the event of the caller's UOW being joined then the transaction is never
     * ended as a result of processing the action's run method, although the UOW
     * could be marked rollback-only if the action's run method throws an exception.
     * </p>
     */
    public Object runUnderUOW(int uowType, boolean join, ExtendedUOWAction uowAction, Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) throws Exception;
}

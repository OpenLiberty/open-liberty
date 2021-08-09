/*******************************************************************************
 * Copyright (c) 2002, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: DistributedRecoveryLog
//------------------------------------------------------------------------------
/**
 * DistributedRecoveryLog extends the RecoveryLog interface and provides
 * additional support for "service data". Service data is a byte array whose
 * contents and size is not defined by the RLS. It is supplied by the client
 * serivce and associated with the recovery log.
 */
public interface DistributedRecoveryLog extends RecoveryLog
{
    //------------------------------------------------------------------------------
    // Method: RecoveryLog.serviceData
    //------------------------------------------------------------------------------
    /**
     * Returns a copy of the service data or null if there is none defined. Changes to
     * the copy will have no affect on the service data stored by the RLS.
     * 
     * @return The service data.
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    public byte[] serviceData() throws LogClosedException, InternalLogException;

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.recoveryComplete
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the RLS that any outstanding recovery process for the recovery log is
     * complete. Client services may issue this call to give the RLS an opportunity
     * to optomize log access (ie by clearing old information from the recovery log)
     * Client services do not have to issue this call.
     * </p>
     * 
     * <p>
     * This call is separate from the <code>RecoveryDirector.recoveryComplete</code>
     * method which must be invoked by a client service in response to a recovery
     * request. The RecoveryDirector callback indicates that sufficient recovery
     * processing has been performed to allow the request to be passed to the next
     * client service. The recovery process may however still execute on a separate
     * thread and call <code>RecoveryLog.recoveryComplete</code> when it has
     * finished.
     * </p>
     * 
     * <p>
     * This extended version of the <code>RecoveryLog.recoveryCompelte()</code> method
     * allows the service data to be updated.
     * </p>
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    public void recoveryComplete(byte[] serviceData) throws LogClosedException, InternalLogException, LogIncompatibleException;

    //------------------------------------------------------------------------------
    // Method: MultiScopeRecoveryLog.keypoint
    //------------------------------------------------------------------------------
    /**
     * <p>
     * A directive from the client service to keypoint the recovery log. Any redundant
     * information will be removed and all cached information will be forced to disk.
     * </p>
     * 
     * @exception LogClosedException Thrown if the recovery log is closed and must
     *                be opened before this call can be issued.
     * @exception InternalLogException Thrown if an unexpected error has occured.
     * @exception LogIncompatibleException An attempt has been made access a recovery
     *                log that is not compatible with this version
     *                of the service.
     */
    public void keypoint() throws LogClosedException, InternalLogException, LogIncompatibleException;

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Close the recovery log. The RLS will ensure that all active RecoverableUnits
     * and RecoverableUnitSections are stored persistently and, if possible that out
     * of date information is purged from the recovery log. The recovery log should
     * be opened again before further access.
     * </p>
     * 
     * <p>
     * Since the client service may issue any number of openLog calls, each must be
     * paired with a corrisponding closeLog call. This allows common logic to be
     * executed on independent threads without any need to determine if a
     * recovery log is already open. This model would typically be used when
     * different threads obtain the same RecoveryLog object through independant
     * calls to <p>RecoveryLogDirector.getRecoveryLog</p>. For example, a recovery
     * process may be in progress on one thread whilst forward processing is being
     * performed on another. Both pieces of logic may issue their own openLog and
     * closeLog calls independently.
     * </p>
     * 
     * <p>
     * Alternativly, the reference to a RecoveryLog may be shared directly around
     * the program logic or between threads. Using this model, a single openLog and
     * closeLog pair are required at well defined initialziation and shutdown points
     * in the client service.
     * </p>
     * 
     * <p>
     * This extended version of the <code>RecoveryLog.closeLog()</code> method
     * allows the service data to be updated prior to the close operation being
     * performed.
     * </p>
     * 
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    public void closeLog(byte[] serviceData) throws InternalLogException;

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.closeLogImmediate
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Close the recovery log. Unlike the closeLog call, the RLS will not perform a
     * of date information. The recovery log should be opened again before further
     * access.
     * </p>
     * 
     * <p>
     * Although the client service may issue any number of openLog calls, no
     * pairing is required for this version of the closeLog call. The close operation
     * will be executed immediatly, regardless of the number of openLog calls.
     * </p>
     * 
     * This method is intended to be used for error cases where a client service has
     * detected some failure condition and must close the log and bail out immediatly.
     * It is the job of the client service to ensure that no further access to the
     * log takes place after this call has been issued.
     * </p>
     * 
     * @exception InternalLogException Thrown if an unexpected error has occured.
     */
    public void closeLogImmediate() throws InternalLogException;

    //------------------------------------------------------------------------------
    // Method: RecoveryLog.associateLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Associates another log with this one. Only one log can be associated.
     * FFDC is generated for the associated log if this log fails.
     * If failAssociatedLog is true then failure of this log will mark the associated log as failed.
     * </p>
     */
    public void associateLog(DistributedRecoveryLog otherLog, boolean failAssociatedLog);
}

/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
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
// Interface: RecoveryLog
//------------------------------------------------------------------------------
/**
* <p>
* The RecoveryLog interface provides support for controling a specific recovery 
* log on behalf of a client service. Instances of RecoveryLog are obtained via
* the <code>RecoveryLogManager.getRecoveryLog</code> method and cannot be
* created directly.
* </p>
*
* <p>
* This interface provides facilities for opening and closing the recovery log, as
* well as access to the underlying RecoverableUnits from which it is comprised.
* </p>
*/                                                                          
public interface RecoveryLog
{
  //------------------------------------------------------------------------------
  // Method: RecoveryLog.openLog
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Open the recovery log. Before a recovery log may be used, it must be opened by
  * the client service. The first time a recovery log is opened, any data stored 
  * on disk will be used to reconstruct the RecoverableUnits and
  * RecoverableUnitSections that were active when the log was closed or the server
  * failed. Only information that was 'forced' to disk will be recovered in this
  * way.
  * </p>
  *
  * <p>
  * The client service may issue any number of openLog calls, but each must be
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
  * @exception LogCorruptedException The recovery log has become corrupted and
  *                                  cannot be opened.
  * @exception LogAllocationException The recovery log could not be created.
  * @exception InternalLogException An unexpected failure has occured.
  * @exception LogIncompatibleException An attempt has been made to open a recovery log
  *                                     that is not compatible with this service
  */                           
  public void openLog() throws LogCorruptedException,LogAllocationException,
                               InternalLogException, LogIncompatibleException;

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
  * @exception InternalLogException Thrown if an unexpected error has occured.
  */
  public void closeLog() throws InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoveryLog.recoveryCompelte
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
  * @exception LogClosedException Thrown if the recovery log is closed and must
  *                               be opened before this call can be issued.
  * @exception InternalLogException Thrown if an unexpected error has occured.
  * @exception LogIncompatibleException An attempt has been made access a recovery
  *                                     log that is not compatible with this version
  *                                     of the service.
  */
  public void recoveryComplete() throws LogClosedException,InternalLogException,LogIncompatibleException;

  //------------------------------------------------------------------------------
  // Method: RecoveryLog.createRecoverableUnit
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Create a new RecoverableUnit under which to write information to the recovery
  * log. 
  * </p>
  *
  * <p>
  * Information written to the recovery log is grouped by the service into a number
  * of RecoverableUnit objects, each of which is then subdivided into a number of
  * RecoverableUnitSection objects. Information to be logged is passed to a
  * RecoverableUnitSection in the form of a byte array.
  * </p>
  * 
  * <p>The identity of the recoverable unit will be allocated by the recovery log.
  * Use of this method <b>must not</b> be mixed with createRecoverableUnit(long)</p>
  *
  * @return The new RecoverableUnit.
  *
  * @exception LogClosedException Thrown if the recovery log is closed and must be 
  *                               opened before this call can be issued.
  * @exception InternalLogException Thrown if an unexpected error has occured.
  * @exception LogIncompatibleException An attempt has been made access a recovery
  *                                     log that is not compatible with this version
  *                                     of the service.
  */
  public RecoverableUnit createRecoverableUnit() throws LogClosedException, InternalLogException,LogIncompatibleException;
  
  //------------------------------------------------------------------------------
  // Method: RecoveryLog.removeRecoverableUnit
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Remove a RecoverableUnit from the recovery logs set of active RecoverableUnits.
  * </p>
  *
  * <p>
  * The RecoverableUnit and its associated RecoverableUnitSections are no longer
  * considered valid after this call. The client service must not invoke any further
  * methods on them.
  * </p>
  *
  * <p>
  * Whilst the RLS will remove these objects from its "in memory" record of the
  * recovery log, it does not guarentee to remove them from the underlying
  * persistent storage when this call is invoked. This latter process will occur
  * at an implementation defined point. Because of this, removed objects may be
  * reconstructed during recovery processing and client services must be able
  * to cope with this.
  * </p>
  *
  * <p>
  * This method must not be invoked whilst an unclosed LogCursor is held (for either 
  * all RecoverableUnits or this RecoverableUnits RecoverableUnitSections.) The
  * <code>LogCursor.remove</code> method should be used instead.
  * </p>
  *
  * @param identity Identity of the RecoverableUnit to be removed.
  *
  * @exception LogClosedException Thrown if the recovery log is closed and must be 
  *                               opened before this call can be issued.
  * @exception InvalidRecoverableUnitException Thrown if the RecoverableUnit does not exist.
  * @exception InternalLogException Thrown if an unexpected error has occured.
  * @exception LogIncompatibleException An attempt has been made access a recovery
  *                                     log that is not compatible with this version
  *                                     of the service.
  */
  public void removeRecoverableUnit(long identity) throws LogClosedException,InvalidRecoverableUnitException,InternalLogException,LogIncompatibleException;

  //------------------------------------------------------------------------------
  // Method: RecoveryLog.lookupRecoverableUnit
  //------------------------------------------------------------------------------
  /**
  * Returns the RecoverableUnit previously created with the supplied identifier or
  * null if no such RecoverableUnit exists (or has been removed)
  *
  * @param identity The identity of the required RecoverableUnit.
  *
  * @return The RecoverableUnit previously created with the supplied identifier or
  *         null if no such RecoverableUnit exists.
  *
  * @exception LogClosedException hrown if the recovery log is closed and must be 
  *                               opened before this call can be issued.
  */
  public RecoverableUnit lookupRecoverableUnit(long identity) throws LogClosedException;

  //------------------------------------------------------------------------------
  // Method: RecoveryLog.recoverableUnits
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Returns a LogCursor that can be used to itterate through all active 
  * RecoverableUnits. The order in which they are returned is not defined.
  * </p>
  *
  * </p>
  * The LogCursor must be closed when it is no longer needed or its itteration
  * is complete. (See the LogCursor class for more information)
  * </p>
  *
  * <p>
  * Objects returned by <code>LogCursor.next</code> or <code>LogCursor.last</code>
  * must be cast to type RecoverableUnit.
  * </p>
  *
  * <p>
  * Care must be taken not remove or add recoverable units whilst the resulting 
  * LogCursor is open. Doing so will result in a ConcurrentModificationException 
  * being thrown.
  * </p>
  *
  * @return A LogCursor that can be used to itterate through all active
  *         RecoverableUnits.
  * 
  * @exception LogClosedException Thrown if the recovery log is closed and must be 
  *                               opened before this call can be issued.
  */
  public LogCursor recoverableUnits() throws LogClosedException;

  //------------------------------------------------------------------------------
  // Method: RecoveryLog.logProperties
  //------------------------------------------------------------------------------
  /**
  * Returns the LogProperties object that defines the physical nature and identity 
  * of the associated recovery log.
  *
  * @return The LogProperties object.
  */
  public LogProperties logProperties();
}

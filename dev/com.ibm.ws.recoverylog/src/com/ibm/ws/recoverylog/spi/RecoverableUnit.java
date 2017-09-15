/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
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
// Interface: RecoverableUnit
//------------------------------------------------------------------------------
/**
* <p>
* Information written to a recovery log is grouped into arbitrary number of
* discrete blocks called "recoverable units". The RecoverableUnit class 
* represents a single recoverable unit within a recovery log. A RecoverableUnit 
* is identified by a key that must be supplied by the client service and 
* guaranteed to be unique within the recovery log. Client services use recoverable
* units to group information according to their requirements. Typically, the
* client service will group information related to a specific unit of work
* in a single recoverable unit.
* </p>
*
* <p>
* Each recoverable unit is further subdivided into an arbitrary number of 
* discrete blocks called "recoverable unit sections". The RecoverableUnitSection
* class represents a single recoverable unit section within a recoverable unit.
* A RecoverableUnitSection is identified by a key that must be supplied by
* the client service and guaranteed to be unique within the recoverable unit. 
* Typically, the client service will group information of a given type into
* a single recoverable unit section.
* </p>
*
* <p>
* Information in the form of byte arrays is written to a recoverable unit section
* rather than directly to recoverable unit.
* </p>
*
* <p>
* This interface defines the operations that can be performed on a RecoverableUnit
* </p>
*/                                                                          
public interface RecoverableUnit
{
  //------------------------------------------------------------------------------
  // Method: RecoverableUnit.createSection
  //------------------------------------------------------------------------------
  /**
  * Creates a new recoverable unit section.
  *
  * @param identity Identity of the new recoverable unit section (must be unique
  *                 within the recoverable unit)
  * @param singleData Flag indicating if the new recoverable unit section should
  *                   retain only a single item of data at any one time. The most
  *                   recent item of data written is retained whilst preceeding
  *                   items are thrown away.
  *
  * @return The new RecoverableUnitSection instance.
  *
  * @exception RecoverableUnitSectionExistsException Thrown if a recoverable unit
  *                                                  section already exists with
  *                                                  the supplied identity.
  * @exception InternalLogException An unexpected error has occured.
  */
  public RecoverableUnitSection createSection(int identity,boolean singleData) throws RecoverableUnitSectionExistsException, InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnit.removeSection
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Removes a recoverable unit section from the recoverable unit.
  * </p>
  *
  * <p>
  * The recoverable unit section is no longer considered valid after this
  * call. The client service must not invoke any further methods on it.
  * </p>
  *
  * <p>
  * Whilst the RLS will remove this object from its "in memory" record of the
  * recovery log, it does not guarentee to remove it from the underlying
  * persistent storage when this call is invoked. This latter process will occur
  * at an implementation defined point. Because of this, removed objects may be
  * reconstructed during recovery processing and client services must be able
  * to cope with this.
  * </p>
  *
  * <p>
  * This method must not be invoked whilst an unclosed LogCursor is held for the
  * recoverable unit sections in this recoverable unit. The 
  * <code>LogCursor.remove</code> method should be used instead.
  * </p>
  * 
  * @param identity The identity of the target recoverable unit section.
  *
  * @exception InvalidRecoverableUnitSectionException The recoverable unit section
  *                                                   does not exist.
  * @exception InternalLogException An unexpected error has occured.
  */
  public void removeSection(int identity) throws InvalidRecoverableUnitSectionException,InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnit.lookupSection
  //------------------------------------------------------------------------------
  /**
  * Returns the recoverable unit section previously created with the supplied 
  * identity. If no such recoverable unit section exists, this method returns null.
  *
  * @param identity The required identity.
  *
  * @return The recoverable unit section previously created with the supplied
  *         identity.
  */
  public RecoverableUnitSection lookupSection(int identity);

  //------------------------------------------------------------------------------
  // Method: RecoverableUnit.writeSections
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Writes to the recovery log any information in the recoverable unit sections
  * that has not already been written by a previous call. This ensures that the
  * recovery log contains an up to date copy of the information retained in the
  * target recoverable unit.
  * </p>
  *
  * <p>
  * The information is written but not forced. This means that it may be buffered 
  * in memory and not actually stored persistently. There is no guarentee that
  * it will be retrieved in the event of a system failure.
  * </p>
  *
  * <p>
  * At some point after this call has completed the information may be transfered
  * from memory to persistent storage. There are two events that will cause this
  * to happen:
  * </p>
  *
  * <p>
  * <ul>
  * <li>1. The RLS chooses to persist the information for implementation
  *        specifc reasons</li>
  * <li>2. The client service issues a further call to direct additional
  *        information to be forced onto persistent storage (for any
  *        recoverable unit in the recovery log)</li>
  * </ul>
  * </p>
  *
  * <p>
  * The InternalLogException may be an instance of LogFullException if the
  * operation has failed because the maximum size of the recovery log has
  * been exceeded.
  * </p>
  *
  * <p>
  * The InternalLogException may be an instance of WriteOperationFailedException
  * if the operation has failed because a file I/O problem has occured.
  * </p>
  *
  * @exception InternalLogException An unexpected error has occured.
  */
  public void writeSections() throws InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnit.forceSections
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Forces to the recovery log any information in the recoverable unit sections
  * that has not already been written or forced by a previous call.This ensures
  * that the recovery log contains an up to date copy of the information retained
  * in the target recoverable unit.
  * </p>
  *
  * <p>
  * This method is similar to writeSections except that the information is guarenteed
  * to be stored persistently and retrieved in the event of a system failure.
  * </p>
  *
  * <p>
  * Any oustanding information buffered in memory from previous 'writeSections'
  * calls is also forced to the recovery log.
  * </p>
  *
  * <p>
  * The InternalLogException may be an instance of LogFullException if the
  * operation has failed because the maximum size of the recovery log has
  * been exceeded.
  * </p>
  *
  * <p>
  * The InternalLogException may be an instance of WriteOperationFailedException
  * if the operation has failed because a file I/O problem has occured.
  * </p>
  *
  * @exception InternalLogException An unexpected error has occured.
  */
  public void forceSections() throws InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnit.sections
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Returns a LogCursor that can be used to itterate through all active 
  * RecoverableUnitSections. The order in which they are returned is not defined.
  * </p>
  *
  * <p>
  * The LogCursor must be closed when it is no longer needed or its itteration
  * is complete. (See the LogCursor class for more information)
  * </p>
  *
  * <p>
  * Objects returned by <code>LogCursor.next</code> or <code>LogCursor.last</code>
  * must be cast to type RecoverableUnitSection.
  * </p>
  *
  * <p>
  * Care must be taken not remove or add recoverable unit sections whilst the
  * resulting LogCursor is open. Doing so will result in a 
  * ConcurrentModificationException being thrown.
  * </p>
  *
  * @return A LogCursor that can be used to cycle through all active RecoverableUnitSections
  */
  public LogCursor sections();

  //------------------------------------------------------------------------------
  // Method: RecoverableUnit.identity
  //------------------------------------------------------------------------------
  /**
  * Returns the identity of this recoverable unit.
  *
  * @return long The identity of this recoverable unit.
  */
  public long identity();
}

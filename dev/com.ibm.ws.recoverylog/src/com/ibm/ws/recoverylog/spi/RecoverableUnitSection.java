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
// Interface: RecoverableUnitSection
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
* This interface defines the operations that can be performed on a 
* RecoverableUnitSection
* </p>
*/                                                                          
public interface RecoverableUnitSection
{
  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSection.addData
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Store the data supplied in the recoverable unit section. The data is cached 
  * ready to be written to the underlying recovery log.
  * </p>
  *
  * <p>
  * Data items will be accumulated on successive addData calls, unless the recoverable 
  * unit section can hold only a single data item. If this is the case, the old data
  * will be disgarded and replaced with new data.
  * </p>
  *
  * <p>
  * Only non-null and non-empty byte arrays may be passed to this method.
  * </p>
  *
  * @param data The new data item to be stored.
  *
  * @exception InternalLogException An unxpected error has occured
  */
  public void addData(byte[] data) throws InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSection.write
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Writes to the recovery log any data that has not already been written by a
  * previous call. This ensures that the recovery log contains an up to date copy
  * of the data retained in the target recoverable unit section.
  * </p>
  *
  * <p>
  * The data is written but not forced. This means that it may be buffered in memory
  * and not actually stored persistently. There is no gurantee that it will be
  * retrieved in the event of a system failue.
  * </p>
  *
  * <p>
  * At some point after this call has completed the data may be transfered
  * from memory to persistent storage. There are two events that will cause this
  * to happen:
  * </p>
  *
  * <p>
  * <ul>
  * <li>1. The RLS chooses to persist the data for implementation specifc
  *        reasons</li>
  * <li>2. The client service issues a further call to direct additional
  *        data to be forced onto persistent storage (for any recoverable
  *        unit in the recovery log)</li>
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
  * @exception InternalLogException An unxpected error has occured
  */
  public void write() throws InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSection.force
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Forces to the recovery log any data that has not already been written or 
  * forced by a previous call.This ensures that the recovery log contains an
  * up to date copy of the information retained in the target recoverable unit.
  * </p>
  *
  * <p>
  * This method is similar to write except that the data is guarenteed to be
  * stored persistently and retrieved in the event of a system failure.
  * </p>
  *
  * <p>
  * Any oustanding information buffered in memory from previous 'write' calls
  * is also forced to the recovery log.
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
  * @exception InternalLogException An unxpected error has occured
  */
  public void force() throws InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSection.data
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Returns a LogCursor that can be used to itterate through all contained data
  * buffers. The data buffers are returned in the same order in which the they 
  * were added (ie FIFO)
  * </p>
  *
  * <p>
  * The LogCursor must be closed when it is no longer needed or its itteration
  * is complete. (See the LogCursor class for more information)
  * </p>
  *
  * <p>
  * Objects returned by <code>LogCursor.next</code> or <code>LogCursor.last</code>
  * must be cast to type byte[].
  * </p>
  *
  * <p>
  * Care must be taken not remove or add data whilst the resulting LogCursor is 
  * open. Doing so will result in a ConcurrentModificationException being thrown.
  * </p>
  *
  * @return A LogCursor that can be used to itterate through all contained data.
  */
  public LogCursor data() throws InternalLogException;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSection.identity
  //------------------------------------------------------------------------------
  /**
  * Returns the identity of the recoverable unit section.
  *
  * @return The identity of the recoverable unit section.
  */
  public int identity();

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSection.lastData
  //------------------------------------------------------------------------------
  /**
  * Retrieves the data item most recently stored inside the recoverable unit 
  * section. Since the data items inside the recoverable unit section are stored 
  * in the same order in which the they were added (ie FIFO), this method is
  * the logical equivalent of obtaining a LogCursor on the data and repeatedly 
  * invoking next() until the last data item is obtained.
  *
  * @return The last data item stored inside the recoverable unit section.
  */
  public byte[] lastData();
}


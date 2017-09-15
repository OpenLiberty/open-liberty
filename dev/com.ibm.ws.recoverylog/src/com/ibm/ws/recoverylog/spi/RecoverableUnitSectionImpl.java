/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi; 

import java.io.IOException;
import java.util.ArrayList;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: RecoverableUnitSectionImpl
//------------------------------------------------------------------------------
/**
* <p>
* Information written to a recovery log is grouped into arbitrary number of
* discrete blocks called "recoverable units". The RecoverableUnitImpl class 
* represents a single recoverable unit within a recovery log. A recoverable
* unit is identified by a key that must be supplied by the client service and 
* guaranteed to be unique within the recovery log. Client services use recoverable
* units to group information according to their requirements. Typically, the
* client service will group information related to a specific unit of work
* in a single recoverable unit.
* </p>
*
* <p>
* Each recoverable unit is further subdivided into an arbitrary number of 
* discrete blocks called "recoverable unit sections". The RecoverableUnitSectionImpl
* class represents a single recoverable unit section within a recoverable unit.
* A recoverable unit section is identified by a key that must be supplied by
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
* The recoverable unit section categorizes information into two groups - 'written'
* information and 'unwritten' information. Written information has already been
* passed to the underlying recovery log (and potentially stored persistently)
* whereas unwritten information has not. This distinction is made to support two
* types of log write.
* </p>
* 
* <p>
* <ul>
* <li>Standard log writes where only unwritten (new) information is written to
*     the underlying recovery log</li>
* <li>Keypoint log writes where both written and unwritten (old and new) 
*     information is written to the underlying recovery log</li>
* </ul>
* </p>
*
* This class provides the implementation of the RecoverableUnitSection interface.
*/                                                                          
public class RecoverableUnitSectionImpl implements RecoverableUnitSection
{
  /**
  * WebSphere RAS TraceComponent registration.
  */
  private static final TraceComponent tc = Tr.register(RecoverableUnitSectionImpl.class,
                                           TraceConstants.TRACE_GROUP, null);

  /**
  * Initial size of the ArrayLists holding both written and unwritten data. The
  * ArrayList will grow if the number of elements exceeds this initial value.
  */
  private static final int INITIAL_DATA_CAPACITY = 10;

  /**
  * A record type code inserted into the header information of a recoverable
  * unit section when it is written to the recovery log. This type code is a marker
  * to indicate that the record contains information about the recoverable
  * unit section that requires recovery.
  */
  protected static short RECORDTYPENORMAL = 1;

  /**
  * A record type code inserted into the header information of a recoverable
  * unit section when its written to the recovery log. This type code is a marker
  * to indicate that the recoverable unit section has been deleted.
  *
  * Currently, this record type code is not used as delection of a recoverable
  * unit section is not implemented (methods throw "NotImplemented")
  */
  protected static short RECORDTYPEDELETED = 2;

  /**
  * The size, in bytes, of the "head" and "tail" information needed to encapsulate
  * a recoverable unit section written to a record on a persistent storage device.
  */
  private static final int HEADER_SIZE = RLSUtils.INT_SIZE +     // section ID
                                         RLSUtils.SHORT_SIZE +   // record type (unused)
                                         RLSUtils.BOOLEAN_SIZE + // single data flag
                                         RLSUtils.INT_SIZE;      // number of data items

  /**
  * Constants to identify specific lock get/set pairs. These values must be
  * unique within the RLS code. Rather than use any form of allocation method
  * for these values components use statically defined constants to save
  * CPU cycles. All instances throughout the RLS source should be prefixed
  * with LOCK_REQUEST_ID to assist location and help ensure that no repeats
  * are introduced by mistake. This comment should also be included whereever
  * these values are defined.
  */
  private static final int LOCK_REQUEST_ID_RUSI_ADDDATA = 3;
  private static final int LOCK_REQUEST_ID_RUSI_WRITE = 4;
  private static final int LOCK_REQUEST_ID_RUSI_FORCE = 5;
  private static final int LOCK_REQUEST_ID_RUSI_FORMAT = 6;
  private static final int LOCK_REQUEST_ID_RUSI_DATA = 7;

  /**
  * The identity of the recoverable unit section
  */
  private int _identity = 0;

  /**
  * An array of data items written to the recoverable unit section but not yet
  * written to the underlying recovery log. These data items are stored in the
  * order that they were added. 
  */
  private ArrayList _unwrittenData = null;

  /**
  * An array of data items written to the recoverable unit section and subsequently
  * written to the underlying recovery log. These data items are stored in the
  * order that they were added.
  */
  private ArrayList _writtenData = null;

  /**
  * Flag to indicate if a recoverable unit section should hold only a single
  * data item. If this is true then the recoverable unit section will replace 
  * any current data item with a new item on an addData call. If this is false,
  * the recoverable unit section will accumulate data items on successive 
  * addData calls.
  */
  private boolean _singleData = false;

  /**
  * A "quick" reference to the last data item that was added to the recoverable
  * unit. This is maintained to allow the lastData() method direct access to it
  * rather than having to itterate search through the arraylist for the last
  * item.
  */
  private DataItem _lastDataItem = null;

  /**
  * Lock used to coordinate access to the data structures that make up the 
  * in memory recovery log. Operations that wish to access these data
  * structures need to obtain a 'shared' lock. See Lock.java for more 
  * information on the locking model.
  */
  private Lock _controlLock = null;

  /**
  * Acccess to the underlying memory mapped recovery log files to allow the
  * files to be forced to persistent storage.
  */
  private LogHandle _logHandle = null;

  /**
  * Identity of the recoverable unit that contains this recoverable unit section
  */
  private long _recoverableUnitIdentity = 0;
                                        
  /**
  * The size, in bytes, required to write all of this recoverable unit sections
  * unwritten data items to persistent storage.
  */
  private int _unwrittenDataSize = 0;
  
  /**
  * The size, in bytes, required to write, or rewrite, all of this
  * recoverable unit sections data items to persistent storage.
  * This includes both the unwritten data items and those that have
  * already been output to persistent storage.
  */
  private int _totalDataSize = 0;  

  /**
  * The recovery log to which this recoverable unit section belongs.
  */
  private MultiScopeRecoveryLog _recLog = null;   
  
  /**
  * A reference to the recoverable unit that contains this recoverable unit section.
  */
  private RecoverableUnitImpl _recUnit = null;

  /**
  * The storage mode in use by this recovery log. Default is memory backed.
  */
  private int _storageMode = MultiScopeRecoveryLog.MEMORY_BACKED;

  /**
  * The name of the application server to which this recoverable unit belongs.
  */
  private String _serverName = null;

  /**
  * The name of the client service that owns the recoverable unit.
  */
  private String _clientName = null;

  /**
  * The version number of the client service that owns the recoverable unit.
  */
  private int _clientVersion = 0;

  /**
  * The name of the associated recovery log within which this recoverable unit 
  * resides.
  */
  private String _logName = null;

  /**
  * The identity of the associated recovery log within which this recoverable unit
  * resides.
  */
  private int _logIdentifier = 0;

  /**
  * The 'traceId' string is output at key trace points to allow easy mapping
  * of recovery log operations to clients logs.
  */
  private String _traceId;

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.RecoverableUnitSectionImpl
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Package access constructor for the creation of recoverable unit sections.
  * </p>
  * 
  * <p>
  * This method should only be called by the RLS itself during either initial
  * creation of a recoverable unit section or recreation during server startup.
  * </p>
  * 
  * @param recLog The recovery log that contains this recoverable unit section.
  * @param recUnit The recoverable unit that contains this recoverable unit section.
  * @param recoverableUnitIdentity The identity of the recoverable unit that contains
  *                                this recoverable unit section)
  * @param identity The identity of the new recoverable unit section (unique within
  *                 the recoverable unit)
  * @param controlLock The lock object, owned by the associated recovery log and used
  *                    to coordinate access to it.
  * @param logHandle The LogHandle reference that provides access to the
  *                  underlying physical recovery log.
  * @param storageMode The required storage mode (defined in the RecoveryLogImpl.java)
  * @param singleData Boolean flag to indicate if this recoverable unit section can 
  *                   hold just a single item of data at a time. If this is true
  *                   then the recoverable unit section will replace any current 
  *                   data item with a new item on an addData call. If this is 
  *                   false, the recoverable unit section will accumulate data 
  *                   items on successive addData calls.
  */
  RecoverableUnitSectionImpl(MultiScopeRecoveryLog recLog,RecoverableUnitImpl recUnit, long recoverableUnitIdentity,int identity,Lock controlLock,LogHandle logHandle,int storageMode,boolean singleData)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "RecoverableUnitSectionImpl",new java.lang.Object[] {recLog,recUnit, new Long(recoverableUnitIdentity),new Integer(identity),controlLock,logHandle,new Integer(storageMode),new Boolean (singleData)});

    // Cache the supplied information
    _recLog = recLog;
    _recoverableUnitIdentity = recoverableUnitIdentity;
    _identity = identity;
    _controlLock = controlLock;
    _logHandle = logHandle;
    _singleData = singleData;
    _recUnit = recUnit;
    _storageMode = storageMode;

    // Prepare the two array lists used to hold DataItems.
    _unwrittenData = new ArrayList(INITIAL_DATA_CAPACITY);
    _writtenData = new ArrayList(INITIAL_DATA_CAPACITY);

    // Cache details about the identity of the associated client / recovery log
    _serverName = recLog.serverName();
    _clientName = recLog.clientName();
    _clientVersion = recLog.clientVersion();
    _logName = recLog.logName();
    _logIdentifier = recLog.logIdentifier();

  
    if (tc.isEntryEnabled()) Tr.exit(tc, "RecoverableUnitSectionImpl", this);
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.addData
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Adds new data to the recoverable unit section. Initially, the data is 
  * 'unwritten' and cachjed inside the '_unwrittenData' array. Once the data has 
  * been passed to the underlying recovery log, its considered 'written' and cached
  * inside the '_writtenData' array. 
  * </p>
  *
  * <p>
  * Data added to a recoverable unit section is wrapped inside a DataItem object. This
  * allows the access to the data to be independant of the physical location of the
  * data (which can be cached in memory but may only be held on disk)
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
  public void addData(byte[] data) throws InternalLogException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "addData",new java.lang.Object[] {RLSUtils.toHexString(data,RLSUtils.MAX_DISPLAY_BYTES), this});

    // If the parent recovery log instance has experienced a serious internal error then prevent
    // this operation from executing.
    if (_recLog.failed())
    {
      if (tc.isEntryEnabled()) Tr.exit(tc, "addData",this);
      throw new InternalLogException(null);
    }

    // Get the shared lock since we are going to modify the internal data structures.
    _controlLock.getSharedLock(LOCK_REQUEST_ID_RUSI_ADDDATA);

    if (_singleData)
    {
        // This recoverable unit section can hold only a single data item.
        if (tc.isDebugEnabled()) Tr.debug(tc, "Section can hold only a single data item.");

        if (_writtenData.size() > 0)
        { 
            // It already contains written data. This must be replaced.
            if (tc.isDebugEnabled()) Tr.debug(tc, "There is existing WRITTEN data. Updating data wrapper.");

            // Retrieve the first, and only written data item.
            SingleDataItem dataItem = (SingleDataItem)_writtenData.get(0);

            // Replace the data content with the new data.
            dataItem.setData(data);

            // Clear out the written data array and move this into the unwritten data array
            _writtenData.clear();
            _unwrittenData.add(0, dataItem);

            // Ensure that the last data reference is accurate.
            _lastDataItem = dataItem;
        }
        else if (_unwrittenData.size() > 0)
        { 
            // It already contains unwritten data. This must be replaced.
            if (tc.isDebugEnabled()) Tr.debug(tc, "There is existing UNWRITTEN data. Updating data wrapper.");

            // Retrieve the first, and only unwritten data item.
            SingleDataItem dataItem = (SingleDataItem)_unwrittenData.get(0);

            // Replace the data content with the new data.
            dataItem.setData(data);

            // Ensure that the last data reference is accurate.
            _lastDataItem = dataItem;
        }
        else
        {
            // It contains no existing data. Create a new DataItem wrapper
            if (tc.isDebugEnabled()) Tr.debug(tc, "There is no existing data. Creating data wrapper");

            // There is no existing data item, so create one and add it to the
            // list of unwritten data items. In practice, this will be the only
            // data item added to this list. Subsequent addData calls will replace
            // the data stored within it (see above).
            SingleDataItem dataItem = new SingleDataItem(_storageMode, data, this);
            _unwrittenData.add(0, dataItem);
            _lastDataItem = dataItem; 
        }
    }
    else
    {
        // This recoverable unit section can hold multiple data item.
        if (tc.isDebugEnabled()) Tr.debug(tc, "Section holds multiple data items");

        // This recoverable unit section can hold an arbitrary number of data items.
        // Create a new data item to hold 'data' and add it to the list of unwritten
        // data items.
        DataItem dataItem = new DataItem(_storageMode, data, this);
        _unwrittenData.add(dataItem);
        _lastDataItem = dataItem;
    } 
  
    try
    {
      _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUSI_ADDDATA);
    }
    catch(NoSharedLockException exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.addData", "382", this);
      if (tc.isEntryEnabled()) Tr.exit(tc, "addData", "InternalLogException");
      throw new InternalLogException(exc);
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "addData");
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.write
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Writes to the underlying recovery log information from the target
  * recoverable unit section that has not already been written by a previous call. 
  * This ensures that the recovery log contains an up to date copy of the 
  * information retained in the target recoverable unit section.
  * </p>
  *
  * <p>
  * The information is written to the underlying recovery log, but not forced
  * through to persisent storage. After this call, the information is not
  * guaranteed to be retrieved during any post-failure recovery processing.
  * To ensure that this information will be recovered, a force operation
  * should be used instead (eg RecoverableUnitSectionImpl.forceSections)
  * </p>
  *
  * @exception InternalLogException An unexpected error has occured.
  */
  public void write() throws InternalLogException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "write", this);

    // If the parent recovery log instance has experienced a serious internal error then prevent
    // this operation from executing.
    if (_recLog.failed())
    {
      if (tc.isEntryEnabled()) Tr.exit(tc, "write",this);
      throw new InternalLogException(null);
    }

    _controlLock.getSharedLock(LOCK_REQUEST_ID_RUSI_WRITE);

    // If there is unwritten data stored within this recoverable unit section then direct the 
    // parent recoverable unit to perform a write for this recoverable unit section. We have
    // to go via the parent because this class has no knowledge of the parents recovery log
    // record structure etc..
    if (_unwrittenDataSize > 0)
    {
      try
      {
        _recUnit.writeSection(this,_unwrittenDataSize + HEADER_SIZE);
      }
      catch(InternalLogException exc)
      {
        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.write", "437", this);
        if (tc.isEntryEnabled()) Tr.exit(tc, "write", exc);
        throw exc;
      }
    }

    try
    {
      _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUSI_WRITE);
    }
    catch(NoSharedLockException exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.write", "449", this);
      if (tc.isEntryEnabled()) Tr.exit(tc, "write", "InternalLogException");
      throw new InternalLogException(exc);
    }
    
    if (tc.isEntryEnabled()) Tr.exit(tc, "write");
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.force
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Forces to the underlying recovery log any information in the target recoverable
  * unit section that has not already been written by a previous call. This ensures 
  * that the recovery log contains an up to date copy of the information retained
  * in the target recoverable unit section.
  * </p>
  *
  * <p>
  * The information is written to the underlying recovery log and forced
  * through to persisent storage. After this call, the information is
  * guaranteed to be retrieved during any post-failure recovery processing.
  * </p>
  *
  * <p>
  * This call my be used as part of an optomization when several recoverable unit
  * sections need to be pushed to disk. For example, the following sequence will
  * ensure that recoverable unit sections 1 through 4 are all persisted to
  * physical storage:-
  * </p>
  * <p>
  * <ul>
  * <li>RecoverableUnitSection1.writeSection()</li>
  * <li>RecoverableUnitSection2.writeSection()</li>
  * <li>RecoverableUnitSection3.writeSection()</li>
  * <li>RecoverableUnitSection4.forceSection()</li>
  * </ul>
  * </p>
  *
  * @exception InternalLogException An unexpected error has occured.
  */
  public void force() throws InternalLogException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "force", this);

    // If the parent recovery log instance has experienced a serious internal error then prevent
    // this operation from executing.
    if (_recLog.failed())
    {
      if (tc.isEntryEnabled()) Tr.exit(tc, "force",this);
      throw new InternalLogException(null);
    }

    try
    {
      this.write();
    }
    catch(InternalLogException exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.force", "509", this);
      if (tc.isEntryEnabled()) Tr.exit(tc, "force", exc);
      throw exc;
    }

    _controlLock.getSharedLock(LOCK_REQUEST_ID_RUSI_FORCE);
    
    try
    {
      _logHandle.force();
    }
    catch(InternalLogException exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.force", "522", this);
      _recLog.markFailed(exc); /* @MD19484C*/
      if (tc.isEntryEnabled()) Tr.exit(tc, "force", exc);
      throw exc;
    }
    finally
    {
      try
      {
        _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUSI_FORCE);
      }
      catch(Throwable exc)
      {
        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.force", "535", this);
        throw new InternalLogException(exc);
      }
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "force");
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.format
  //------------------------------------------------------------------------------
  /**
  * <p>
  * This method is called by the parent recoverable unit to direct this recoverable
  * unit section to output its content to the supplied log record.
  * </p>
  *
  * <p>
  * The amount of data output depends on the input argument 'rewriteRequired'.
  * If this flag is false then only data that has not not already been written 
  * or forced by a previous call should be output. If this flag is true then 
  * all data should be output. Either way, the the underlying recovery log
  * will contain an up to date copy of the information retained in the target.
  * </p>
  *
  * @param rewriteRequired Flag indicating if a rewrite operation is required.
  * @param logRecord A WritableLogRecord into which the data should be output.
  *
  * @exception java.io.IOException Thrown if a failure occurs writing the data
  * @exception InternalLogException An unexpected error has occured.
  */
  void format(boolean rewriteRequired, WriteableLogRecord logRecord) throws IOException,InternalLogException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "format",new java.lang.Object[] {new Boolean(rewriteRequired), logRecord, this});

    // If the parent recovery log instance has experienced a serious internal error then prevent
    // this operation from executing.
    if (_recLog.failed())
    {
      if (tc.isEntryEnabled()) Tr.exit(tc, "format", "InternalLogException");
      throw new InternalLogException(null);
    }

    _controlLock.getSharedLock(LOCK_REQUEST_ID_RUSI_FORMAT);

    int totalDataSize = 0;
    int writtenDataSize = 0; 
    int unwrittenDataSize = 0;
    
    // Calculate the number of written data items
    if (_writtenData != null)
    {
        writtenDataSize = _writtenData.size();
    }
       
    // Calculate the number of unwritten data items
    if (_unwrittenData != null)
    {
        unwrittenDataSize = _unwrittenData.size();    
    }        
    
    // Calculate the total number of data items that need to be written.
    if (rewriteRequired)
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "Section has '" + unwrittenDataSize + "' unwritten data items and '" + writtenDataSize + "' written data items to write");
        
        totalDataSize = unwrittenDataSize + writtenDataSize;
    }
    else
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "Section has '" + unwrittenDataSize + "' unwritten data items and '0' written data items to write");
        
        totalDataSize = unwrittenDataSize;
    }

    // As long as there are some to write then do so. Otherwise, this method is a no-op.
    if (totalDataSize > 0)
    {
      if (tc.isDebugEnabled()) Tr.debug(tc, "Writing section identity '" + _identity + "'");

      // Write the identity of this recoverable unit section
      logRecord.putInt(_identity);

      // Write the 'record type' field. Currently this is not used but is provided for future compatibility.
      // It would be used to distinguish between a 'normal' write and a 'delete' write. The latter would be used
      // to remove the recoverable unit section from the recovery log. Ignore this field for now.
      logRecord.putShort(RECORDTYPENORMAL);

      // Write the singleData flag
      logRecord.putBoolean(_singleData);

      // Write out the amount of data to be written
      logRecord.putInt(totalDataSize);

      if (rewriteRequired)
      {
        // Write the written data items to the buffer if a rewrite operation is being performed. 
        for( int i = 0; i < writtenDataSize; i++ )
        {            
          if (tc.isDebugEnabled()) Tr.debug(tc, "Writing written data item '" + i + "'");
          ((DataItem)_writtenData.get(i)).write(logRecord);
        }
      }

      // Write the unwritten data items to the buffer. Additionally, these must then be transferred 
      // to the written data items list.
      for( int i = 0; i < unwrittenDataSize; i++ )
      {
        if (tc.isDebugEnabled()) Tr.debug(tc, "Writing unwritten data item '" + i + "'");

        DataItem dataItem = (DataItem)_unwrittenData.get(i);

        dataItem.write(logRecord);

        _writtenData.add(dataItem);
      }

      // There can be no unwritten data items left after this method has run. Ensure that
      // we have a clear unwritten data item list.
      if (unwrittenDataSize > 0)
      {
        _unwrittenData.clear();
      }
    }
    else
    {
      if (tc.isDebugEnabled()) Tr.debug(tc, "RecoverableUnitSectionImpl '" + _identity + "' has no data to format");
    }

    try
    {
      _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUSI_FORMAT);
    }
    catch(NoSharedLockException exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.format", "670", this);
      if (tc.isEntryEnabled()) Tr.exit(tc, "format", "InternalLogException");
      throw new InternalLogException(exc);
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "format");
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.data
  //------------------------------------------------------------------------------
  /**
  * <p>
  * Returns a LogCursor that can be used to itterate through all data contained
  * by this recoverable unit section (both unwritten and written). The byte arrays
  * will be returned in that they were added (ie FIFO)
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
  *
  *
  */
  public LogCursor data() throws InternalLogException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "data", this);

    // If the parent recovery log instance has experienced a serious internal error then prevent
    // this operation from executing.
    if (_recLog.failed())
    {
      if (tc.isEntryEnabled()) Tr.exit(tc, "data",this);
      throw new InternalLogException(null);
    }

    _controlLock.getSharedLock(LOCK_REQUEST_ID_RUSI_DATA);

    // Trace out the number of written and unwritten data blocks held by this RecoverableUnitSectionImpl
    if (tc.isEventEnabled())
    {
        int writtenDataBlocks = 0;
        int unwrittenDataBlocks = 0;

        if (_writtenData!=null)
        {
          writtenDataBlocks = _writtenData.size();
        }
        if (_unwrittenData!=null)
        {
          unwrittenDataBlocks = _unwrittenData.size();
        }

        Tr.event(tc,"#writtenDataBlocks = " + writtenDataBlocks + " #unwrittenDataBlocks = " + unwrittenDataBlocks);
    }

    LogCursor cursor = null;
    
    if (_singleData)
    {
        // There is only a single data item. The LogCursor provides an optomized constructor
        // for this case.
        if (_writtenData.size() > 0)
        {
            cursor = new LogCursorImpl(null,((DataItem)_writtenData.get(0)).getData());
        }
        else
        {
            cursor = new LogCursorImpl(null,((DataItem)_unwrittenData.get(0)).getData());    
        }
    }
    else
    {
      // There are potentially multiple data items. Use the standard LogCursor constructor.
      cursor = new LogCursorImpl(null,_writtenData,_unwrittenData,false,null);
    }

    try
    {
      _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUSI_DATA);
    }
    catch(NoSharedLockException exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.data", "766", this);
      if (tc.isEntryEnabled()) Tr.exit(tc, "data", "InternalLogException");
      throw new InternalLogException(exc);
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "data", new Integer(cursor.initialSize()));

    return cursor;
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.identity
  //------------------------------------------------------------------------------
  /**
  * Returns the identity of the recoverable unit section.
  *
  * @return The identity of the recoverable unit section.
  */
  public int identity()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "identity",this);
    if (tc.isEntryEnabled()) Tr.exit(tc, "identity",new Integer(_identity));
    return _identity;
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.recover
  //------------------------------------------------------------------------------
  /**
  * <p>
  * This internal method is called by the RLS to direct the recoverable unit section
  * to retrieve recovery data from the underlying recovery log. The ReadableLogRecord
  * supplied by the caller provides direct access to the underlying recovery log.
  * From it, this method can retrieve details of data items that must be ADDED to any
  * already stored in memory.
  * </p>
  *
  * <p>
  * This method may be called any number of times to complete recovery processing
  * for the target recoverable unit section.
  * </p>
  *
  * <p>
  * This method throws LogCorruptedException to indicate that a failure has occured 
  * whilst parsing the data. We assume that this failure has been caused as a result
  * of a previous system crash during a disk write. The calling code will react to
  * this by assmuning that all valid information has now been retireved from the 
  * underlying recovery log.
  * </p>
  *
  * @exception LogCorruptedException Corrupt log data was detected (see above)
  * @exception InternalLogException An unexpected exceptions has occured
  */
  void recover(ReadableLogRecord logRecord) throws LogCorruptedException,InternalLogException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "recover", new Object[] {logRecord, this});

    // If the parent recovery log instance has experienced a serious internal error then prevent
    // this operation from executing.
    if (_recLog.failed())
    {
      if (tc.isEntryEnabled()) Tr.exit(tc, "recover", "InternalLogException");
      throw new InternalLogException(null);
    }

    try
    {
      // Determine the number of data items to be recovered this time.
      int numDataItems = logRecord.getInt();

      if (tc.isDebugEnabled()) Tr.debug(tc, "Recovering '" + numDataItems + "' data items");

      // Reconstruct each data item in memory.
      for (int d = 0; d < numDataItems; d++)
      {
          if (tc.isDebugEnabled()) Tr.debug(tc, "Recovering data item '" + d + "'");
        
          DataItem dataItem = null;
          
          if (_singleData)
          {
              // Check to see if we are replacing something, or if there is no
              // data in this section.
              if (_writtenData.size() > 0)                  /* @MD19840A*/
              {                                             /* @MD19840A*/
                  dataItem = (DataItem)_writtenData.get(0); /* @MD19840A*/
              }                                             /* @MD19840A*/

              if (dataItem == null)                         /* @MD19840A*/
              {                                             /* @MD19840A*/
                  
                  // This recoverable unit section may hold only piece of data. This
                  // must be held by the special DataItem subclass, SingleDataItem. 
                  // Create the wrapper to hold this information.
                  dataItem = new SingleDataItem(_storageMode, logRecord, this);
                  _writtenData.add(dataItem);               /* @MD19994M*/
              }                                             /* @MD19840A*/
              else                                          /* @MD19840A*/
              {                                             /* @MD19840A*/
                  // Replace the existing data in the SingleDataItem /* @MD19840A*/
                  ((SingleDataItem)dataItem).setData(logRecord);     /* @MD19840A*/
                  _writtenData.set(0, dataItem);            /* @MD19994A*/
              }                                             /* @MD19840A*/
          }
          else
          {
              // This recoverable unit section may hold an arbitrary number of pieces
              // of data. These are all cached within the standard DataItem class.
              // Create the wrapper to hold this information.
              dataItem = new DataItem(_storageMode, logRecord, this);

              // As this recoverable unit seciton may hold an arbirary number of
              // data items, add the wrapper to the end of the list.
              _writtenData.add(dataItem);        
          }

         // However this information is stored inside the recoverable unit, 'dataItem' is the
         // last data block to be added to the recoverable unit section. Preserve this reference
         // in '_lastData' in order to have quick access to it from the lastData() method.
         _lastDataItem = dataItem;
      }
    }
    catch (InternalLogException exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.recover", "876", this);
      if (tc.isDebugEnabled()) Tr.debug(tc, "An InternalLogException occured reconstructng a RecoverableUnitSectionImpl");
      _recLog.markFailed(exc); /* @MD19484C*/
      if (tc.isEntryEnabled()) Tr.exit(tc, "recover", "LogCorruptedException");
      throw new LogCorruptedException(exc);
    }
    catch (Throwable exc)
    {
      FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitSectionImpl.recover", "884", this);
      if (tc.isDebugEnabled()) Tr.debug(tc, "An exception occured reconstructng a RecoverableUnitSectionImpl");
      _recLog.markFailed(exc); /* @MD19484C*/
      if (tc.isEntryEnabled()) Tr.exit(tc, "recover", "InternalLogException");
      throw new InternalLogException(exc);
    }
    if (tc.isEntryEnabled()) Tr.exit(tc, "recover");
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSection.lastData
  //------------------------------------------------------------------------------
  /**
  * Returns the data item most recently stored inside the recoverable unit 
  * section. Since the data items inside the recoverable unit section are retrieved
  * in the same order in which the they were added (ie FIFO), this method is
  * the logical equivalent of obtaining a LogCursor on the data and repeatedly 
  * invoking next() until the last data item is obtained.
  *
  * This class etains ownership of the data buffer. The caller must not modify
  * it as doing so may result in this updated information being written to the
  * recovery log at an undefined time. It is not possible to update the buffer
  * and then ensure its written to disk by calling force.
  *
  * @return The last data item stored inside the recoverable unit section or
  *         null if the recoverable unit section is empty.
  */
  public byte[] lastData()
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "lastData",this);

    byte[] lastData = null;

    // Assuming that we have actually added data to this section, '_lastData' always
    // references the last data item that was stored inside the recoverable unit
    // section. Retireve any data content and return it.
    if (_lastDataItem != null)
    {
      lastData = _lastDataItem.getData();
    }
    
    if (tc.isEntryEnabled()) Tr.exit(tc, "lastData",RLSUtils.toHexString(lastData,RLSUtils.MAX_DISPLAY_BYTES));
    return lastData;
  }
  
  //------------------------------------------------------------------------------
  // Method: RecoveryUnitSectionImpl.payloadAdded
  //------------------------------------------------------------------------------
  /**
  * Informs the recoverable unit section that data has been added to it. It may seem
  * strange that there is a method on this class to do this, since data is actually
  * added directly to it anyway (via the addData method), but the payload size is
  * dependent on both the size of the data and the size of a DataItem header. Since
  * only the DataItem can determine this, it pushes the payload information back to
  * the recoverable unit when it is created as a result of the addData call.
  *
  * The recoverable unit section must use the supplied information to track the 
  * amount of active data that it holds. This information must be passed to its parent
  * recoverable unit in order that it may track the amount of active data it holds.
  *
  * This call is driven by data item which has been created or updated and accounts
  * for the additional data and header fields necessary to form a persistent record
  * of the new data item.
  * 
  * This data has not yet been written to persistent storage and must therefour be
  * tracked in both total and unwritten data size fields.
  *
  * @param payloadSize The additional number of bytes that would be needed to form
  *                    a persistent record of the new data item when a writeSections 
  *                    or forceSections operation is driven by the client service.
  */
  protected void payloadAdded(int payloadSize)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "payloadAdded", new Object[] {this, new Integer(payloadSize)});
          
      int unwrittenRecUnitAdjustment = payloadSize;
      int totalRecUnitAdjustment = payloadSize;            
      
      // When adding new payload, if there was currently no unwritten data then we will need to
      // an additional header in order to be able to contain this information. When we pass on
      // this payload adjustment we must account for the header size.
      if (_unwrittenDataSize == 0)
      {
          unwrittenRecUnitAdjustment += HEADER_SIZE;                                
      }
        
      // When adding new payload, if there was currently no written data then we will need to
      // an additional header in order to be able to contain this information. When we pass on
      // this payload adjustment we must account for the header size.
      if (_totalDataSize == 0)
      {
          totalRecUnitAdjustment += HEADER_SIZE;
      } 
      
      // Track the payload increases directly. We take no account for this classes header values
      // in these figures.
      _unwrittenDataSize += payloadSize;
      _totalDataSize += payloadSize;       
        
      // Pass on the payload adjustment to the parent class.
      _recUnit.payloadAdded(unwrittenRecUnitAdjustment, totalRecUnitAdjustment);
            
      if (tc.isDebugEnabled()) Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize + " totalDataSize = " + _totalDataSize);
      
      if (tc.isEntryEnabled()) Tr.exit(tc, "payloadAdded");
  }
  
  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.payloadWritten
  //------------------------------------------------------------------------------
  /**
  * Informs the recoverable unit section that previously unwritten data has been
  * written to disk by one of its data items and no longer needs to be tracked
  * in the unwritten data field. The recoverable unit must use the supplied 
  * information to track the amount of unwritten active data it holds. This 
  * information must be passed to its parent recovery log in order that it
  * may track the amount of unwritten data in the entire recovery log. 
  *
  * This call is driven by the data item which has been written and accounts for
  * both the data and header fields necessary to write the data completly.
  * 
  * Writing data in this manner will not change the total amount of active data 
  * contained by the recoverable unit so only the unwritten data size will be effected.
  *
  * @param payloadSize The number of bytes that no longer need to be written in order
  *                    to form a persistent record of the data item when a writeSections
  *                    or forceSections operation is driven by the client service.
  */
  protected void payloadWritten(int payloadSize)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "payloadWritten", new Object[] {this, new Integer(payloadSize)});
      
      // Track the unwritten payload decrease directly. We take no account for this classes header
      // values in this figure. The total payload remains unchanged since we are not removing the
      // corrisponding payload, just writing it to the underlying recovery log.
      _unwrittenDataSize -= payloadSize;
      
      // When writing existing payload, if the resulting unwritten data size has gone back down to 
      // zero then there will be no further need to account for the unwritten data header.
      // When we pass on this payload adjustment we must account for the header size.
      if (_unwrittenDataSize == 0)
      {
          _recUnit.payloadWritten(payloadSize + HEADER_SIZE);    
      }
      else
      {
          _recUnit.payloadWritten(payloadSize);   
      }
      
      if (tc.isDebugEnabled()) Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize + " totalDataSize = " + _totalDataSize);
      if (tc.isEntryEnabled()) Tr.exit(tc, "payloadWritten");
  }
  
  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.payloadDeleted
  //------------------------------------------------------------------------------
  /**
  * Informs the recoverable unit section that data has been removed. At present this
  * method is only used by the SingleDataItem class to remove the payload required
  * for the its data before adding back additional payload back again for the
  * replacement data data.
  *
  * The recoverable unit section must use the supplied information to track the 
  * amount of active data that it holds. This information must be passed to its parent
  * recoverable unit in order that it may track the amount of active data it holds.
  *
  * This call is driven by the data item from which data has been removed and 
  * accounts for both the data and header fields that would have been necessary
  * to form a persistent record of this data content.
  * 
  * This data may or may not have been written to persistent storage and must 
  * therefour be tracked in both total and unwritten data size fields. It is important 
  * to understand why two parameters are required on this call rather than a single
  * value that could be reflected in both fields. The following example can be used
  * to illustrate the reason for this. Consider that a data item of size D1 has
  * been added to this recoverable unit section and it may hold only a single data 
  * item. The unwritten and total data size fields will be made up as follows:
  *
  * unwritten    total
  * D1           D1
  *
  * If this information is then written to disk, D1 will be deducted from the
  * unwritten total (see payloadWritten) whilst the total data size remains 
  * unchanged.
  * 
  * unwritten    total
  *  -           D1
  *
  * If D1 is subsequently deleted, the total will need to be reduced but the
  * unwritten field will remian unchanged. Since it is the callers 
  * responsibility to determine the amount that needs to be removed from 
  * each, two arguments are required.
  *
  * @param unwrittenPayloadSize The number of bytes that will no longer be required
  *                             to form a persistent record of the data item
  *                             when either the writeSections or forceSections
  *                             operation is driven by the client service.
  * @param totalPayloadSize     The number of bytes that will no longer be required
  *                             to form a persistent record of the data item the
  *                             next time a keypoint operation occurs.
  */
  protected void payloadDeleted(int totalPayloadSize, int unwrittenPayloadSize)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "payloadDeleted", new Object[] {this, new Integer(totalPayloadSize), new Integer(unwrittenPayloadSize)});
      
      // Track the payload decreases directly. We take no account for this classes header values
      // in these figures.
      _totalDataSize -= totalPayloadSize;
      _unwrittenDataSize -= unwrittenPayloadSize;
      
      // When removing existing payload, if the resulting unwritten data size has gone back down to 
      // zero then there will be no further need to account for the unwritten data header.
      // When we pass on this payload adjustment we must account for the header size.
      if (_unwrittenDataSize == 0 && (unwrittenPayloadSize != 0))
      {
          unwrittenPayloadSize += HEADER_SIZE;
      }
      
      // When removing existing payload, if the resulting written data size has gone back down to 
      // zero then there will be no further need to account for the written data header.
      // When we pass on this payload adjustment we must account for the header size.
      if (_totalDataSize == 0)
      {
          totalPayloadSize += HEADER_SIZE;    
      }
      
      _recUnit.payloadDeleted(totalPayloadSize, unwrittenPayloadSize);
      
      if (tc.isDebugEnabled()) Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize + " totalDataSize = " + _totalDataSize);
      if (tc.isEntryEnabled()) Tr.exit(tc, "payloadDeleted");    
  }

  //------------------------------------------------------------------------------
  // Method: RecoverableUnitSectionImpl.toString
  //------------------------------------------------------------------------------
  /**
  * Returns the string representation of this object instance.
  * 
  * @return String The string representation of this object instance.
  */
  public String toString()
  {
      if (_traceId == null)
          // Now establish a 'traceId' string. This is output at key trace points to allow
          // easy mapping of recovery log operations to clients logs.
          _traceId = "RecoverableUnitSectionImpl:" + "serverName=" +_serverName + ":"
                                             + "clientName=" + _clientName + ":"
                                             + "clientVersion=" + _clientVersion + ":"
                                             + "logName=" +_logName + ":"
                                             + "logIdentifier=" + _logIdentifier + " @"
                                             + System.identityHashCode(this);

  	return _traceId;
  }
}


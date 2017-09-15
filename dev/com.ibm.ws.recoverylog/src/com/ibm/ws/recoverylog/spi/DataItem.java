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

import java.nio.BufferUnderflowException;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
//Class: DataItem
//------------------------------------------------------------------------------
/** A DataItem encapsulates a single piece of data that has been added
 *  to the log. A DataItem can be either file backed or memory backed. A
 *  memory backed data item will maintain an in-memory copy of its data
 *  at all times. A file backed data item will maintain an in-memory copy
 *  of its data until it is written to persistent storage. Once the data
 *  has been persisted the in-memory copy of the data will be forgotten
 *  and a pointer to the data's position in the underlying log file will
 *  be maintained instead.
 */
public class DataItem
{
    private static final TraceComponent tc = Tr.register(DataItem.class, TraceConstants.TRACE_GROUP, null);
    
    // The size, in bytes, of the header information
    // included in the data output when this DataItem
    // is written to persistent storage.
    protected static final int HEADER_SIZE = RLSUtils.INT_SIZE; // data length
    
    /** A constant to indicate that this DataItem's data
     *  has not be written to persistent storage.
     */
    protected static final int UNWRITTEN = -1;
    
    /**
    * Flag indicating the selected storage mode. When using memory backed storage, all active
    * information written to the data item is retained in memory in addition to being written
    * to disk during force operations. When using file backed storage the in memory copy is
    * discarded once the data has been written to persistant storage. Subsequent retrieval
    * of the information (eg during keypoint) will require a disk read operation.
    */
    protected int _storageMode = MultiScopeRecoveryLog.MEMORY_BACKED;
    
    // The memory-based view of this DataItem's data.
    // When using a file backed storage mode this
    // reference will be null once the data has been
    // written to persistent storage.
    protected byte[] _data = null;
    
    // The position in the underlying log file at
    // which this data item's data has been written.
    // When using a memory backed storage mode this
    // position will be unused (i.e. it will remain -1)
    // When using a file-based storage mode this
    // position will be -1 until the data is written 
    // to disk at which time it will be updated to
    // reflect the position of the data on disk.
    protected int _filePosition = UNWRITTEN;

    // The LogRecord that contains this data items
    // data at position _filePosition. This reference
    // will remain null when using memory backed
    // storage.
    protected LogRecord _logRecord = null;
   
    // The size, in bytes, of this DataItem's data
    // This values represents the active size of the
    // data that this item represents, be it currently
    // held in memory (_data) or written out to 
    // persistent storage (pointed to by _filePosition).
    protected int _dataSize = 0;
    
    // The RecoverableUnitSection that contains this
    // DataItem.
    protected RecoverableUnitSectionImpl _rus = null;
    
    // A flag to track whether or not this DataItem's
    // data has been written to persistent storage
    protected boolean _written = false;
    
    //------------------------------------------------------------------------------
    // Method: DataItem.DataItem
    //------------------------------------------------------------------------------
    /** Create a new DataItem for use during mainline running, i.e. not for recovery.
      * 
      * @param storageMode The storage mode for this DataItem, either FILE_BACKED or MEMORY_BACKED
      * @param data The data to be encapsulated by this DataItem
      * @param rus The recoverable unit section that contains the data being encapsulated.
      * 
      */
    public DataItem(int storageMode, byte[] data, RecoverableUnitSectionImpl rus)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "DataItem", new Object[] {new Integer(storageMode), RLSUtils.toHexString(data,RLSUtils.MAX_DISPLAY_BYTES), rus});
         
        _rus = rus;
        _data = data;
        _storageMode = storageMode;
        
        _dataSize = data.length; 
               
        _rus.payloadAdded(_dataSize + HEADER_SIZE);
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "DataItem", this);       
    }
    
    //------------------------------------------------------------------------------
    // Method: DataItem.DataItem
    //------------------------------------------------------------------------------
    /** Create a new DataItem with the given storage mode from the data held in the
      * given ReadableLogRecord. The amount of data to be encapsulated by this DataItem
      * is read for the log record as an int followed by that many bytes of data.
      * @param storageMode The storage mode for this DataItem, either FILE_BACKED or MEMORY_BACKED
      * @param logRecord The log record to read this DataItem's data from
      * @param rus The recoverable unit section that contains this item of data
      * @throws InternalLogException Thrown if a failure occurs recovering this DataItem's data
      *                              from the log.
      */
    public DataItem(int storageMode, ReadableLogRecord logRecord, RecoverableUnitSectionImpl rus) throws InternalLogException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "DataItem", new Object[] {new Integer(storageMode), logRecord, rus});
        
        _rus = rus;
        _storageMode = storageMode;
        _logRecord = logRecord;
        
        try
        {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Reading data size field @ position " + logRecord.position());

            _dataSize = logRecord.getInt();       
        
            if (tc.isDebugEnabled()) Tr.debug(tc, "This data item contains " + _dataSize + " bytes of data");
            
            _rus.payloadAdded(_dataSize + HEADER_SIZE);
            _rus.payloadWritten(_dataSize + HEADER_SIZE);
            _written = true;            
            
            if (storageMode == MultiScopeRecoveryLog.MEMORY_BACKED)
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "Reading " + _dataSize + "bytes of data @ position " + logRecord.position());
                _data = new byte[_dataSize];
                _logRecord.get(_data);
            }
            else
            {
                if (tc.isDebugEnabled()) Tr.debug(tc, "Tracking " + _dataSize + "bytes of data @ position " + logRecord.position());
                _filePosition = logRecord.position();
                _data = null;
                _logRecord.advancePosition(_dataSize);          
            }
        }
        catch (BufferUnderflowException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.DataItem.DataItem", "176", this);
            if (tc.isEntryEnabled()) Tr.exit(tc, "DataItem", "InternalLogException");
            throw new InternalLogException(exc);
        }
        catch (Exception exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.DataItem.DataItem", "182", this);
            if (tc.isEntryEnabled()) Tr.exit(tc, "DataItem", "InternalLogException");
            throw new InternalLogException(exc);
        }
        
        if (tc.isEntryEnabled()) Tr.exit(tc, "DataItem", this);
    }
    
    //------------------------------------------------------------------------------
    // Method: DataItem.write
    //------------------------------------------------------------------------------
    /** Instructs this DataItem to write its data to the given WriteableLogRecord.
      * The write involves writing the length of the data as an int followed by
      * the data itself. The data is written at the log record's current position. 
      * 
      * @param logRecord The WriteableLogRecord to write the enapsulated data to.
      */
    protected void write(WriteableLogRecord logRecord)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "write", new Object[]{logRecord, this});

        // Retrieve the data stored within this data item. This will either come from
        // the cached in memory copy or retrieved from disk.
        byte[] data = this.getData();

        if (tc.isDebugEnabled()) Tr.debug(tc, "Writing '" + data.length + "' bytes " + RLSUtils.toHexString(data,RLSUtils.MAX_DISPLAY_BYTES));

        if (tc.isDebugEnabled()) Tr.debug(tc, "Writing length field");
        logRecord.putInt(data.length);
                
        // If this data item is using the file backed storage method then we need
        // record details of the mapped storage buffer and offset within that buffer
        // where the data has been placed. If this is the first write call then the
        // information would currently be cached in memory. After this call has
        // executed further access will require the getData method to go to disk.
        // If this is a subsequent write call then the getData method will retrieve
        // the information from the current mapped byte buffer and position and
        // write it to the new mapped byte buffer and position. It will then cache
        // the new location details.
        if (_storageMode == MultiScopeRecoveryLog.FILE_BACKED)
        {
          if (tc.isDebugEnabled()) Tr.debug(tc, "Updaing data location references");
          _filePosition = logRecord.position();
          _logRecord = logRecord;
          _data = null;
        }

        if (tc.isDebugEnabled()) Tr.debug(tc, "Writing data field");
        logRecord.put(data);

        if (!_written)
        {
          // This is the first time since creation of this object or reset of its internal
          // data (SingleData class only, setData method) that the write method has been
          // called. We know that the parent recoverable unit section is accounting for
          // this data items payload in its unwritten data size. Accordingly we need to
          // direct it to update this value.
          _rus.payloadWritten(_dataSize + HEADER_SIZE);
        }

        _written = true;

        if (tc.isEntryEnabled()) Tr.exit(tc, "write");
    }

    //------------------------------------------------------------------------------
    // Method: DataItem.getData
    //------------------------------------------------------------------------------    
    /** Returns the data encapsulated by this DataItem instance. If this DataItem
     *  is memory back the in memory copy of the data is always returned. If the
     *  DataItem is file backed and it has been written to disk the data is
     *  retrieved from disk and then returned.
     *  @return This DataItem instance's data
     */
    protected byte[] getData()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "getData", this);

        byte[] data = _data;

        if (data != null)
        {
          // There is data cached in memory. Simply use this directly.
          if (tc.isDebugEnabled()) Tr.debug(tc, "Cached data located");
        }
        else
        {
          if (_storageMode == MultiScopeRecoveryLog.FILE_BACKED)
          {
            // There is no data cached in memory - it must be retrieved from disk.
            if (tc.isDebugEnabled()) Tr.debug(tc, "No cached data located. Attempting data retreival");

            // If there is no data cached in memory and we are operating in file
            // backed mode then retireve the data from disk.
            if (_filePosition != UNWRITTEN)
            {
              if (tc.isDebugEnabled()) Tr.debug(tc, "Retrieving " + _dataSize + " bytes of data @ position " + _filePosition);
              _logRecord.position(_filePosition);
              data = new byte[_dataSize];
              _logRecord.get(data);
            }
            else
            {
              // The data should have been stored on disk but the file position was not set. Return null
              // to the caller and allow them to handle this failure.
              if (tc.isDebugEnabled()) Tr.debug(tc, "Unable to retrieve data as file position is not set");
            }
          }
          else
          {
            // The data should have been cached in memory but was not found Return null to the caller and 
            // allow them to handle this failure.
            if (tc.isDebugEnabled()) Tr.debug(tc, "No cached data located");
          }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "getData", new Object[]{new Integer(data.length),RLSUtils.toHexString(data,RLSUtils.MAX_DISPLAY_BYTES)});
        return data;
    }
}

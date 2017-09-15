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

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
//Class: DataItem
//------------------------------------------------------------------------------
/** A SingleDataItem is a specialized form of DataItem. When a user indicates
 *  that a RecoverableUnitSection can only hold a single item of data that
 *  item of data will be encapsulated by a SingleDataItem instance. In addition
 *  to those provided by a DataItem a SingleDataItem provides that ability to
 *  update the data that it is encapsulating via the setData method.
 */
public class SingleDataItem extends DataItem
{
    private static final TraceComponent tc = Tr.register(SingleDataItem.class, TraceConstants.TRACE_GROUP, null);

    //------------------------------------------------------------------------------
    // Method: SingleDataItem.SingleDataItem
    //------------------------------------------------------------------------------
    /** Create a new singleDataItem for use during mainline running, i.e. not for recovery.
      * 
      * @param storageMode The storage mode for this SingleDataItem, either FILE_BACKED or MEMORY_BACKED
      * @param data The data to be encapsulated by this SingleDataItem
      * @param rus The recoverable unit section that contains the data being encapsulated. 
      */
    public SingleDataItem(int storageMode, byte[] data, RecoverableUnitSectionImpl rus)
    {
        super(storageMode, data, rus);    
        if (tc.isEntryEnabled()) Tr.entry(tc, "SingleDataItem", new Object[] {new Integer(storageMode), RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES), rus});
        if (tc.isEntryEnabled()) Tr.exit(tc, "SingleDataItem", this); 
    }
    
    // ------------------------------------------------------------------------------
    // Method: singleDataItem.SingleDataItem
    //------------------------------------------------------------------------------
    /** Create a new SingleDataItem with the given storage mode from the data held in the
      * given ReadableLogRecord. The amount of data to be encapsulated by this DataItem
      * is read from the log record as an int followed by that many bytes of data.
      * 
      * @param storageMode The storage mode for this SingleDataItem, either FILE_BACKED or
      *                    MEMORY_BACKED
      * @param logRecord The log record to read this SingleDataItem's data from
      * @param rus The recoverable unit section that contains this item of data
      * 
      * @throws InternalLogException Thrown if a failure occurs recovering this
      *                              SingleDataItem's data from the log.
      */
    public SingleDataItem(int storageMode, ReadableLogRecord logRecord, RecoverableUnitSectionImpl rus) throws InternalLogException
    {
        super(storageMode, logRecord, rus);    
        if (tc.isEntryEnabled()) Tr.entry(tc, "SingleDataItem", new Object[] {new Integer(storageMode),logRecord,rus});
        if (tc.isEntryEnabled()) Tr.exit(tc, "SingleDataItem", this);
    }
    
    //------------------------------------------------------------------------------
    // Method: SingleDataItem.SingleDataItem
    //------------------------------------------------------------------------------
    /** Update the data being encapsulated by this SingleDataItem. Following a setData
     *  call this data item's payload will reflect the size of new data and if the
     *  new data was a different size to the old data the containing recoverable unit
     *  section's payload will have been adjusted accordingly.
     *  @param data The data to be encapsulated by this SingleDataItem instance
     */
    protected void setData(byte[] data)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "setData", new Object[] {RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES), this});
    
        int totalDataSize = _dataSize + HEADER_SIZE;
        
        int unwrittenDataSize = 0;
        
        if (!_written)
        {
            unwrittenDataSize = totalDataSize;
        }
        
        // Remove the old payload from the containing recoverable
        // unit section.
        _rus.payloadDeleted(totalDataSize, unwrittenDataSize);        
        
        // Cache the new data and flag that the encapsulated data
        // has not been written to disk.
        _data = data;
        _dataSize = data.length;
        _filePosition = UNWRITTEN;
        _written = false;      
    
        // Add the new payload to the containing recoverable
        // unit section.
        _rus.payloadAdded(_data.length + HEADER_SIZE);
           
        if (tc.isEntryEnabled()) Tr.exit(tc, "setData");    
    }


    //------------------------------------------------------------------------------
    // Method: SingleDataItem.setData
    //------------------------------------------------------------------------------
    /** Update the data being encapsulated by this SingleDataItem. Following a setData
     *  call this data item's payload will reflect the size of new data and if the
     *  new data was a different size to the old data the containing recoverable unit
     *  section's payload will have been adjusted accordingly.
     *  @param logRecord The data to be encapsulated by this SingleDataItem instance
     */
    protected void setData(ReadableLogRecord logRecord) /* @MD19840AA*/
        throws InternalLogException
    {
        if (tc.isEntryEnabled()) 
            Tr.entry(tc, "setData", new Object[] {this, logRecord});

        int totalDataSize = _dataSize + HEADER_SIZE;
        
        int unwrittenDataSize = 0;

        if (!_written)
        {
            unwrittenDataSize = totalDataSize;
        }

        // Remove the old payload from the containing recoverable 
        // unit section
        _rus.payloadDeleted(totalDataSize, unwrittenDataSize);

        // The new data is encapsulated in the logRecord.  We need to
        // swap the logRecord in the DataItem class.
        _logRecord = logRecord;

        try
        {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Reading data size field @ position " + logRecord.position());

            _dataSize = logRecord.getInt();       
        
            if (tc.isDebugEnabled()) Tr.debug(tc, "This data item contains " + _dataSize + " bytes of data");
            
            _rus.payloadAdded(_dataSize + HEADER_SIZE);
            _rus.payloadWritten(_dataSize + HEADER_SIZE);            
            _written = true;
            
            if (_storageMode == MultiScopeRecoveryLog.MEMORY_BACKED)
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
        catch (java.nio.BufferUnderflowException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SingleDataItem.setData", "180", this);
            throw new InternalLogException(exc);
        }
        catch (Exception exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SingleDataItem.setData", "185", this);
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "setData");
    }
}

/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: ReadableLogRecord 
//------------------------------------------------------------------------------
/**
* <p>
* The ReadableLogRecord class extends LogRecord with additional function to read
* and validate the record header and tail structures. The parent LogRecord class
* provides the interfaces needed to allow other recovery log components to read
* the record content.
* </p>
*/                                                                          
public class ReadableLogRecord extends LogRecord
{
  /**
  * WebSphere RAS TraceComponent registration
  */
  private static final TraceComponent tc = Tr.register(ReadableLogRecord.class, TraceConstants.TRACE_GROUP, null);
  
  private final long _sequenceNumber;
    
  //------------------------------------------------------------------------------
  // Method: ReadableLogRecord.ReadableLogRecord
  //------------------------------------------------------------------------------
  /**
  * Constructor for a new ReadableLogRecord. As part of construction, this class 
  * validates that the current position in the source buffer contains a valid
  * log record. On exit from construction, the source buffer position will
  * have been moved forwards to the start of the next log record. This class
  * retains a reference to a duplicate of the source buffer with its own independant
  * position cursor. On  exit it will have moved the duplicates cursor forward to 
  * the start of the data encapsulated within the log record.
  *
  * @param buffer The buffer from which a record should be obtained.
  */
  private ReadableLogRecord(ByteBuffer buffer, int absolutePosition, long sequenceNumber)
  {
    super(buffer, absolutePosition);
    _sequenceNumber = sequenceNumber; 
    
    if (tc.isEntryEnabled()) Tr.entry(tc, "ReadableLogRecord", new Object[]{new Integer(absolutePosition), new Long(sequenceNumber)});                                                     
    if (tc.isEntryEnabled()) Tr.exit(tc, "ReadableLogRecord", this);       
  } 
  
  protected static ReadableLogRecord read(ByteBuffer sourceBuffer, long expectedSequenceNumber, boolean doByteByByteScanning)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "read", new Object[] {sourceBuffer, new Long(expectedSequenceNumber), doByteByByteScanning});
      ReadableLogRecord logRecord = null;
      try
      {
          logRecord = read(sourceBuffer.slice(), expectedSequenceNumber, sourceBuffer);        
      }
      catch (RuntimeException e)
      {
          // No FFDC code needed
          if (tc.isDebugEnabled()) Tr.debug(tc, "EXPECTED: RuntimeException reading log: ", e);
      }
      
      if (logRecord == null && doByteByByteScanning)
      {
          // see if we can find a valid log record later in the log
          logRecord = doByteByByteScanning(sourceBuffer, expectedSequenceNumber);
      }
      
      if (tc.isEntryEnabled()) Tr.exit(tc, "read", logRecord);
      return logRecord;
  }
  
  // careful with trace in this method as it is called many times from doByteByByteScanning
  private static ReadableLogRecord read(ByteBuffer viewBuffer, long expectedSequenceNumber, ByteBuffer sourceBuffer)
  {
    ReadableLogRecord logRecord = null;
    int absolutePosition = sourceBuffer.position() + viewBuffer.position();
    
    // Read the record magic number field.
    final byte[] magicNumberBuffer = new byte[RECORD_MAGIC_NUMBER.length];
    
    viewBuffer.get(magicNumberBuffer);        
    
    int recordLength = 0;
            
    if (Arrays.equals(magicNumberBuffer, RECORD_MAGIC_NUMBER))
    {
      long recordSequenceNumber = viewBuffer.getLong(); 
      
      if (recordSequenceNumber >= expectedSequenceNumber)
      {
        // The record sequence is consistent with the expected sequence number supplied by the
        // caller. So skip over the actual log record data in this record so that
        // we can check the tail sequence number.
        recordLength = viewBuffer.getInt();
      
        // Preserve the current byte cursor position so that we can reset back to it later. 
        // Move the byte cursor to the first byte after the record data.
        final int recordDataPosition = viewBuffer.position();                                                
        viewBuffer.position(recordDataPosition + recordLength);

        // Read the repeated record sequence number
        final long tailSequenceNumber = viewBuffer.getLong();
    
        // Because are are only looking for sequence numbers larger than expected the only assurance that we
        // have not read garbage following the magic number is that the first and tail sequence numbers are equal.
        
        // Note its still possible garbage is in the data but that can't be helped without changing the log format.
        // It will be discovered later no doubt!
        if (tailSequenceNumber == recordSequenceNumber)
        {
          // Return the buffer's pointer to the start of
          // the record's data prior to creating a new
          // ReadableLogRecord to return to the caller.
          viewBuffer.position(recordDataPosition);
          
          logRecord = new ReadableLogRecord(viewBuffer, absolutePosition, tailSequenceNumber); 
          
          // Advance the original buffer's position to the end of this record. This ensures that
          // the next ReadableLogRecord or WritableLogRecord constructed will read or write the
          // next record in the file.
          sourceBuffer.position(absolutePosition + HEADER_SIZE + recordLength);    
        }
      }
    }
    
    return logRecord;       
  }
  
  // careful with trace in this methods loop
  private static ReadableLogRecord doByteByByteScanning(ByteBuffer sourceBuffer, long expectedSequenceNumber)
  {
      if (tc.isEntryEnabled()) Tr.entry(tc, "doByteByByteScanning", new Object[] {sourceBuffer, new Long(expectedSequenceNumber)});
      ReadableLogRecord logRecord = null;
      ByteBuffer viewBuffer = sourceBuffer.slice();
      
      // If there is a partial write, or a missing record, the next valid record will be at least LogRecord.HEADER_SIZE
      // forward from the current position so skip to that in the viewBuffer and start from there.
      for (int position = LogRecord.HEADER_SIZE; 
           position + LogRecord.HEADER_SIZE < viewBuffer.limit(); //  its possible we will be able to find a record in the remainder 
           position++)
      {
          viewBuffer.position(position);
          
          // Peak 2 bytes to get RC in RCRD before bothering to try parse a Record.
          // This saves stack frames and avoids some Exceptions which should help with startup times
          byte peak = viewBuffer.get(position);
          if (peak == 82) // 'R'
          {
              peak = viewBuffer.get(position + 1);
              if (peak == 67) // C - its a kinda magic, worth having a go.
              {
                  try
                  {
                      logRecord = read(viewBuffer, expectedSequenceNumber, sourceBuffer);
                      if (logRecord != null)
                          break; // we found a valid record, sourceBuffer position has been updated
                  }
                  catch (RuntimeException e)
                  {
                      // No FFDC code needed
                      // continue till the end of the loop
                  }
              }
          }
      }
      
      if (tc.isEntryEnabled()) Tr.exit(tc, "doByteByByteScanning", logRecord);
      return logRecord;
  }
  
  protected long getSequenceNumber()
  {
      return _sequenceNumber;
  }
}

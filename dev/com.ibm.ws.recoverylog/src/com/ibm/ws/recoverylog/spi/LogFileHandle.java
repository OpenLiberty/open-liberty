/*******************************************************************************
 * Copyright (c) 2002, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: LogHandle
//------------------------------------------------------------------------------
/**
 * <p>
 * INTERNAL CLASS FOR USE BY THE RECOVERY LOG SERVICE ONLY
 * </p>
 * 
 * <p>
 * This class provides the low level disk access support for the LogHandle class. Each
 * instance of this class allows reading and writing to a single log file on disk.
 * </p>
 */
class LogFileHandle
{
    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(LogFileHandle.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * Offset within the log file of the status field to allow direct access. The format for this
     * is HEADER LENGTH FIELD (an int) + MAGIC NUMBER LENGTH BYTES + RLS VERSION NUMBER LENGTH FIELD (an int).
     */
    final static int STATUS_FIELD_FILE_OFFSET = RLSUtils.INT_SIZE + LogFileHeader.MAGIC_NUMBER.length + RLSUtils.INT_SIZE;

    /**
     * A reference to this LogFileHandle's log file viewed as a mapped byte buffer.
     * Changes made to the buffer's contents are automatically mirrored by the
     * underlying OS into the log file that is residing on disk. No guarantees are
     * made about when this mirroring will occur. To be 100% sure that a change to
     * the buffer is reflected in the data stored persistently on disk the buffer
     * must be forced.
     */
    private ByteBuffer _fileBuffer = null;

    /**
     * Indicates whether or not the <code>_fileBuffer</code> is actually a mapped
     * byte buffer of the file. If true, changes made to the buffer's contents
     * are automatically mirrored by the underlying OS into the log file that is
     * residing on disk. No guarantees are made about when this mirroring will occur.
     * To be 100% sure that a change to the buffer is reflected in the data stored
     * persistently on disk the buffer must be forced.
     */
    private boolean _isMapped = false;

    /**
     * List of Records that still need to be forced to disk.
     */
    private final List _pendingWriteList = new ArrayList();

    /**
     * Tally of log records created but not written
     */
    private final AtomicInteger _outstandingWritableLogRecords = new AtomicInteger();

    /**
     * Comparator class to compare the "order" of LogRecords
     * with respect to the LogRecord's absolute position in the log ByteBuffer.
     * Internal class used by LogFileHandle only, so
     */
    private final Comparator _recordComparator = new Comparator()
    {
        @Override
        public int compare(Object obj1, Object obj2)
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "compare", new Object[] { obj1, obj2, this });

            final int comparison = ((LogRecord) obj1).absolutePosition() - ((LogRecord) obj2).absolutePosition();

            if (tc.isEntryEnabled())
                Tr.exit(tc, "compare", new Integer(comparison));
            return comparison;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "equals", new Object[] { obj, this });
            if (tc.isEntryEnabled())
                Tr.exit(tc, "equals", Boolean.FALSE);
            return false;
        }
    };

    /**
     * A reference to the log file managed by this LogFileHandle instance.
     */
    private RandomAccessFile _file = null;

    /**
     * A FileChannel for the _file reference. The file channel is used to create
     * the mapped view of the log file when it is first opened and to re-map
     * the file when the log file is being extended.
     */
    private FileChannel _fileChannel = null;

    /**
     * A reference to the LogFileHeader which provides an in memory copy of the file header stored
     * in the file managed by this instance of LogFileHandle.
     */
    private LogFileHeader _logFileHeader = null;

    /**
     * The directory in which the file managed by this instance of LogFileHandle resides.
     */
    private String _logDirectory = null;

    /**
     * The name of file managed by this instance of LogFileHandle.
     */
    private String _fileName = null;

    /**
     * The name of the current application server
     */
    private String _serverName = null;

    /**
     * The name of the service that owns the file managed by this instance of LogFileHandle.
     */
    private String _serviceName = null;

    /**
     * The version number of the service which created the log file
     */
    private final int _serviceVersion;

    /**
     * The name of the log that owns the file managed by this instance of LogFileHandle.
     */
    private String _logName = null;

    /**
     * The filesize (in kilobytes) of the file managed by this instance of LogFileHandle.
     */
    private int _fileSize = 0;

    /**
  */
    FailureScope _failureScope = null;

    /**
     * Whether of not an exception was thrown during force
     */
    private volatile boolean _exceptionInForce = false;

    /**
     * Whether or not the header was rewritten following a restart and before writing new log records.
     * Visibility is assured by locks in LogHandle.getWriteableLogRecord
     */
    private boolean _headerFlushedFollowingRestart = false;

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.LogFileHandle
    //------------------------------------------------------------------------------
    /**
     * Package access constructor to create a new instance of the LogFilefile handle
     * to manage the given recovery log file
     * 
     * @param logDirectory The directory in which the file managed by this instance of LogFileHandle resides.
     * @param fileName The name of file managed by this instance of LogFileHandle.
     * @param serverName The name of the current application server
     * @param serviceName The name of the service that owns the file managed by this instance of LogFileHandle.
     * @param serviceVersion The version number of the client service.
     * @param logName The name of the log that owns the file managed by this instance of LogFileHandle.
     * @param fileSize The filesize (in kilobytes) of the file managed by this instance of LogFileHandle.
     */
    protected LogFileHandle(String logDirectory, String fileName, String serverName, String serviceName, int serviceVersion, String logName, int fileSize, FailureScope fs)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "LogFileHandle", new Object[] { logDirectory, fileName, serverName, serviceName, new Integer(serviceVersion), logName, new Integer(fileSize), fs });

        _logDirectory = logDirectory;
        _fileName = fileName;
        _serverName = serverName;
        _serviceName = serviceName;
        _serviceVersion = serviceVersion;
        _logName = logName;
        _fileSize = fileSize;
        _failureScope = fs;

        // Construct an empty LogFileHeader object ready to hold the header information
        _logFileHeader = new LogFileHeader(_serverName, _serviceName, _serviceVersion, _logName);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "LogFileHandle", this);
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.getReadableLogRecord     
    //------------------------------------------------------------------------------
    /**
     * Read a composite record from the disk. The caller supplies the expected sequence
     * number of the record and this method confirms that this matches the next record
     * recovered.
     * 
     * @param sequenceNumber The expected sequence number
     * 
     * @return ReadableLogRecord The composite record
     */
    protected ReadableLogRecord getReadableLogRecord(long expectedSequenceNumber)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getReadableLogRecord", new java.lang.Object[] { this, new Long(expectedSequenceNumber) });

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Creating readable log record to read from file " + _fileName);

        final ReadableLogRecord readableLogRecord = ReadableLogRecord.read(_fileBuffer, expectedSequenceNumber, !_logFileHeader.wasShutdownClean());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getReadableLogRecord", readableLogRecord);

        return readableLogRecord;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.fileOpen               
    //------------------------------------------------------------------------------
    /**
     * Open the file on the disk. The name of the file was supplied to the constructor.
     * 
     * @exception InternalLogException An unexpected error has occured.
     * @exception LogAllocationException The new log file on disk could not be created
     *                correctly.
     */
    protected void fileOpen() throws InternalLogException, LogAllocationException,
                    LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fileOpen", this);

        final boolean fileColdStarting;

        // Open the file, creating it if it does not already exist.
        try
        {
            try
            {
                final File pFile = new File(_logDirectory, _fileName);

                // Determine if the log file exists or is zero bytes long. In either of
                // these cases, we consider it to be a cold start of the file.
                fileColdStarting = ((!pFile.exists()) || (pFile.length() == 0));

                if (fileColdStarting)
                {
                    Tr.info(tc, "CWRLS0006_RECOVERY_LOG_CREATE", _logDirectory + File.separator + _fileName);
                }

                // Open/Create the file.
                // _fileBuffer = (MappedByteBuffer) AccessController.doPrivileged(
                _fileBuffer = (MappedByteBuffer) Configuration.getAccessController()
                                .doPrivileged(
                                              new java.security.PrivilegedExceptionAction()
                                              {
                                                  @Override
                                                  public java.lang.Object run() throws Exception
                                                  {
                                                      if (tc.isEntryEnabled())
                                                          Tr.entry(tc, "run", this);

//              _file = new RandomAccessFile(pFile, "rw");
                                                      _file = RLSAccessFile.getRLSAccessFile(pFile); // @255605C
                                                      _fileChannel = _file.getChannel();

                                                      // NB - this limits us to a maximum file size
                                                      // of 2GB. This is the max value of an int and
                                                      // is also the maximum amount of data that
                                                      // can be held in a MappedByteBuffer.
                                                      final int fileLength = (int) _file.length();

                                                      final int fileSizeBytes = _fileSize * 1024;

                                                      // In the event that the log file already exists and it's bigger
                                                      // than the required file size ensure that we map the entire
                                                      // contents of the file.             
                                                      final int sizeToMap = fileLength > fileSizeBytes ? fileLength : fileSizeBytes;

                                                      // Feature 731093 default for noMemoryMappedFIles flag is operating system dependent
                                                      String osName = System.getProperty("os.name");
                                                      if (osName != null)
                                                          osName = osName.toLowerCase();
                                                      if (tc.isDebugEnabled())
                                                          Tr.debug(tc, "Working on operating system " + osName);
                                                      // If not specified - default is false
                                                      boolean noMemoryMappedFiles = Boolean.getBoolean("com.ibm.ws.recoverylog.spi.NoMemoryMappedFiles");
                                                      // If Windows and HA enabled, or z/OS (feature 731093) then we want the default to be non-memory mapped
                                                      if (osName != null
                                                          && ((Configuration.HAEnabled() && osName.startsWith("windows")) || osName.startsWith("z/os") || osName.startsWith("os/390")))
                                                      {
                                                          final String propertyValue = System.getProperty("com.ibm.ws.recoverylog.spi.NoMemoryMappedFiles");
                                                          if (propertyValue == null || !(propertyValue.equalsIgnoreCase("false")))
                                                              noMemoryMappedFiles = true;
                                                      }
                                                      if (tc.isDebugEnabled())
                                                          Tr.debug(tc, "NoMemoryMappedFiles flag is " + noMemoryMappedFiles);

                                                      Object fileBuffer = null;

                                                      if (!noMemoryMappedFiles)
                                                      {
                                                          try
                                                          {
                                                              fileBuffer = _fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, sizeToMap);
                                                              _isMapped = true;
                                                          }
                                                          catch (Throwable t)
                                                          {
                                                              if (tc.isEventEnabled())
                                                                  Tr.event(tc, "Mapping of recovery log file failed. Using non-mapped file.", t);
                                                              if (tc.isEventEnabled())
                                                                  Tr.event(tc, "Resetting file Channel position to '0' from :", _fileChannel.position());
                                                              _fileChannel.position(0); //An Exception in the map method can leave an incorrect position (PM14310)
                                                          }
                                                      }
                                                      else
                                                      {
                                                          if (tc.isEventEnabled())
                                                              Tr.event(tc, "Recovery log has been instructed not to use a mapped-file model.");
                                                      }

                                                      if (fileBuffer == null)
                                                      {
                                                          // Either we were instructed not to use a mapped buffer or the
                                                          // attempt to use one failed. Allocate a direct byte buffer
                                                          // and read the FileChannel into the buffer.  write()s to the
                                                          // direct byte buffer will not be reflected by the FileChannel
                                                          // until a force() is made on the LogFileHandle.

                                                          final ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(sizeToMap);

                                                          if (fileColdStarting)
                                                          {
                                                              _fileChannel.write(directByteBuffer, 0);
                                                              _fileChannel.force(true);
                                                          }

                                                          _fileChannel.read(directByteBuffer);
                                                          directByteBuffer.rewind();
                                                          _isMapped = false;

                                                          if (tc.isDebugEnabled())
                                                              Tr.debug(tc, "A direct byte buffer has been allocated successfully.");

                                                          fileBuffer = directByteBuffer;
                                                      }

                                                      if (tc.isEntryEnabled())
                                                          Tr.exit(tc, "run", fileBuffer);
                                                      return fileBuffer;
                                                  }
                                              });
            } catch (java.security.PrivilegedActionException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "338", this);
                throw new LogAllocationException(exc);
            } catch (Throwable exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "343", this);
                throw new InternalLogException(exc);
            }
        } catch (LogAllocationException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "351", this);
            _fileBuffer = null;
            if (_file != null)
                fileClose(); // @255605A
            if (tc.isEntryEnabled())
                Tr.exit(tc, "fileOpen", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "359", this);
            _fileBuffer = null;
            if (_file != null)
                fileClose(); // @255605A
            if (tc.isEntryEnabled())
                Tr.exit(tc, "fileOpen", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (fileColdStarting)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Log File " + this._fileName + " is cold starting");

            // The file did not exist when this call was issued and has been created above.
            // Write an empty log header into the file to ensure that it can't be confused
            // with a corrupt log file in the event of a crash before we fully initialize 
            // it.
            try
            {
                writeFileHeader(false);
            } catch (InternalLogException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "388", this);
                fileClose(); // @255605A
                _file = null;
                _fileChannel = null;
                _fileBuffer = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "fileOpen", exc);
                throw exc;
            } catch (Throwable exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "396", this);
                fileClose(); // @255605A
                _file = null;
                _fileChannel = null;
                _fileBuffer = null;
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "fileOpen", "InternalLogException");
                throw new InternalLogException(exc);
            }
        }
        else
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Log File " + this._fileName + " is warm starting");

            // The file already existed on disk before this method was called. We must load
            // the header information from this file.
            readFileHeader();
        }

        // First check whether invalid. invalid trumps incompatible. 
        if (!_logFileHeader.valid())
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Log File " + this._fileName + " is not valid");
            try
            {
//        Dont close channel as it will free a lock - wait until file close
//        _fileChannel.close(); // @255605D
                _file.close();
            } catch (java.io.IOException exc)
            {
                // The file could not even be closed! There is nothing that can be done in this situation.
                // Simply allow the LogIncompatibleException exception to be passed back up the stack.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "423", this);
            } catch (Throwable exc)
            {
                // The file could not even be closed! There is nothing that can be done in this situation.
                // Simply allow the LogIncompatibleException exception to be passed back up the stack.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "429", this);
            }

            _fileBuffer = null;
            _file = null;
            _fileChannel = null;
            _logFileHeader = null;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "fileOpen", "InternalLogException");
            throw new InternalLogException();
        }

        // If the header incompatible (created by an unsupported version of the RLS) then throw an exception.
        if (!_logFileHeader.compatible())
        {
            try
            {
//        Dont close channel as it will free a lock - wait until file close
//        _fileChannel.close(); // @255605D
                _file.close();
            } catch (java.io.IOException exc)
            {
                // The file could not even be closed! There is nothing that can be done in this situation.
                // Simply allow the LogIncompatibleException exception to be passed back up the stack.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "423", this);
            } catch (Throwable exc)
            {
                // The file could not even be closed! There is nothing that can be done in this situation.
                // Simply allow the LogIncompatibleException exception to be passed back up the stack.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "429", this);
            }

            _fileBuffer = null;
            _file = null;
            _fileChannel = null;
            _logFileHeader = null;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "fileOpen", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        if (!serviceCompatible())
        {
            try
            {
//        Dont close channel as it will free a lock - wait until file close
//        _fileChannel.close(); // @255605D
                _file.close();
            } catch (java.io.IOException exc)
            {
                // The file could not even be closed! There is nothing that can be done in this situation.
                // Simply allow the LogIncompatibleException exception to be passed back up the stack.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "423", this);
            } catch (Throwable exc)
            {
                // The file could not even be closed! There is nothing that can be done in this situation.
                // Simply allow the LogIncompatibleException exception to be passed back up the stack.
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileOpen", "429", this);
            }

            _fileBuffer = null;
            _file = null;
            _fileChannel = null;
            _logFileHeader = null;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "fileOpen", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fileOpen");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.fileExist               
    //------------------------------------------------------------------------------
    /*
     * Determine if the file managed by this LogFileHandle instance currently exists
     * on disk.
     * 
     * @return boolean true if the file currently exists.
     */
    protected boolean fileExists()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fileExists", this);

        boolean fileAlreadyExists = true;

        File file = new File(_logDirectory, _fileName);
        fileAlreadyExists = (file.exists() && (file.length() > 0));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fileExists", new Boolean(fileAlreadyExists));
        return fileAlreadyExists;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.fileClose               
    //------------------------------------------------------------------------------
    /**
     * Close the file managed by this LogFileHandle instance.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void fileClose() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fileClose", this);

        if (_fileChannel != null)
        {
            try
            {
                // Don't close channel as it will free a lock - wait until file close

                if (_outstandingWritableLogRecords.get() == 0 && !_exceptionInForce)
                {
                    force(); // make sure all records really are on disk

                    // Now we know that there are no gaps in the records and none have failed
                    // to be written, byte by byte scanning at the next start can be avoided.
                    _logFileHeader.setCleanShutdown(); // set the fact that shutdown was clean
                    writeFileHeader(false); // write clean shutdown into header
                    force();
                }

                _file.close();
            } catch (Throwable e)
            {
                FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileClose", "541", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "fileClose", "InternalLogException");
                throw new InternalLogException(e);
            }

            _fileBuffer = null;
            _file = null;
            _fileChannel = null;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fileClose");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.getWriteableLogRecord
    //------------------------------------------------------------------------------
    /**
     * Get a new WritableLogRecord for the log file managed by this LogFileHandleInstance.
     * The size of the record must be specified along with the record's sequence number.
     * It is the caller's responsbility to ensure that both the sequence number is correct
     * and that the record written using the returned WritableLogRecord is of the given
     * length.
     * 
     * @param recordLength The length of the record to be created
     * @param sequenceNumber The newly created record's sequence number
     * @return WriteableLogRecord A new writeable log record of the specified size
     */
    public WriteableLogRecord getWriteableLogRecord(int recordLength, long sequenceNumber) throws InternalLogException
    {

        if (!_headerFlushedFollowingRestart)
        {
            // ensure header is updated now we start to write records for the first time
            // synchronization is assured through locks in LogHandle.getWriteableLogRecord
            writeFileHeader(true);
            _headerFlushedFollowingRestart = true;
        }

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getWriteableLogRecord", new Object[] { new Integer(recordLength), new Long(sequenceNumber), this });

        // Create a slice of the file buffer and reset its limit to the size of the WriteableLogRecord.
        // The view buffer's content is a shared subsequence of the file buffer.
        // No other log records have access to this log record's view buffer.
        ByteBuffer viewBuffer = (ByteBuffer) _fileBuffer.slice().limit(recordLength + WriteableLogRecord.HEADER_SIZE);
        WriteableLogRecord writeableLogRecord = new WriteableLogRecord(viewBuffer, sequenceNumber, recordLength, _fileBuffer.position());

        // Advance the file buffer's position to the end of the newly created WriteableLogRecord
        _fileBuffer.position(_fileBuffer.position() + recordLength + WriteableLogRecord.HEADER_SIZE);

        _outstandingWritableLogRecords.incrementAndGet();
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getWriteableLogRecord", writeableLogRecord);
        return writeableLogRecord;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.writeFileHeader
    //------------------------------------------------------------------------------
    /**
     * Writes the file header stored in '_logFileHeader' to the file managed by this
     * LogFileHandle instance.
     * 
     * @param maintainPosition Flag to indicate if file pointer before the header write
     *            needs to be restored after the header write (ie retain
     *            the file pointer position)
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    private void writeFileHeader(boolean maintainPosition) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeFileHeader", new java.lang.Object[] { this, new Boolean(maintainPosition) });

        // Build the buffer that forms the major part of the file header and
        // then convert this into a byte array.
        try
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Writing header for log file " + _fileName);
            _logFileHeader.write(_fileBuffer, maintainPosition);
            force();
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.writeFileHeader", "706", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeFileHeader", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.writeFileHeader", "712", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeFileHeader", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeFileHeader");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.writeFileStatus
    //------------------------------------------------------------------------------
    /**
     * Updates the status field of the file managed by this LogFileHandle instance.
     * The status field is stored in '_logFileHeader'.
     * 
     * @param maintainPosition Flag to indicate if file pointer before the header write
     *            needs to be restored after the header write (ie retain
     *            the file pointer position)
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    private void writeFileStatus(boolean maintainPosition) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeFileStatus", new java.lang.Object[] { this, new Boolean(maintainPosition) });

        if (_logFileHeader.status() == LogFileHeader.STATUS_INVALID)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeFileStatus", "InternalLogException");
            throw new InternalLogException(null);
        }

        try
        {
            int currentFilePointer = 0;

            if (maintainPosition)
            {
                // If the caller wishes the file pointer's current
                // position to be maintained cache it's position
                // here so that it can be reset once the header
                // has been written.
                currentFilePointer = _fileBuffer.position();
            }

            // Move the buffer's pointer to the header status
            // field and perform a forced write of the new status
            _fileBuffer.position(STATUS_FIELD_FILE_OFFSET);
            _fileBuffer.putInt(_logFileHeader.status());
            force();

            if (maintainPosition)
            {
                // Reinstate the fileBuffer's pointer to its original position.
                _fileBuffer.position(currentFilePointer);
            }
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.writeFileStatus", "797", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeFileStatus", "WriteOperationFailedException");
            throw new WriteOperationFailedException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeFileStatus");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.readFileHeader
    //------------------------------------------------------------------------------
    /**
     * Read the header data from the file managed by this LogFileHandle instance. Any
     * failure that occurs will result in the log file header ('_logFileHeader')
     * being marked as invalid. This can then be tested later by calling the
     * _logFileHeader.status() method and comparing for STATUS_INVALID.
     */
    private void readFileHeader()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "readFileHeader", this);

        // Reset the current header information. Once reset has executed, the header
        // is marked as invalid and will only become valid again once a correctly 
        // formatted header has been read from the disk.
        _logFileHeader.reset();

        // Read the header, ignoring any failures. Failures will be detected later
        // when _logFileHeader.status() method returns STATUS_INVALID.
        try
        {
            // Ensure that there is sufficient capacity to read the integer header
            // size field before proceeding. If there is not then exit directly. This
            // header will remain marked as inactive.
            if (_fileBuffer.capacity() >= RLSUtils.INT_SIZE)
            {
                _logFileHeader.read(_fileBuffer);
            }
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.readFileHeader", "863", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "The log header could not be read from the disk due to an unexpected exception");
            // There in incomplete or invalid header. No action is required here as the log header is still
            // marked as invalid.
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "readFileHeader");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.serviceCompatible
    //------------------------------------------------------------------------------
    /**
  */
    private boolean serviceCompatible()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serviceCompatible");

        boolean serviceCompatible = false;

        // Extract the service name and version and the log name from the
        // header. These should have been read by a call to readFileHeader
        // prior to invoking this method.
        String serviceNameFromFile = _logFileHeader.serviceName();
        int serviceVersionFromFile = _logFileHeader.serviceVersion();
        String logNameFromFile = _logFileHeader.logName();

        // Now compare these values with the associated client service. Only
        // if we get a compatible match from each field will we deem this
        // header to be compatible with the client service.
        if ((serviceVersionFromFile > _serviceVersion) ||
            (serviceNameFromFile == null) ||
            (logNameFromFile == null) ||
            !(serviceNameFromFile.equals(_serviceName)) ||
            !(logNameFromFile.equals(_logName)))
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "Client service and recovery log are not compatible");
            if (tc.isEventEnabled())
                Tr.event(tc, "Current service name is " + _serviceName);
            if (tc.isEventEnabled())
                Tr.event(tc, "Service name from file is " + serviceNameFromFile);
            if (tc.isEventEnabled())
                Tr.event(tc, "Current log name is " + _logName);
            if (tc.isEventEnabled())
                Tr.event(tc, "Log name from file is " + logNameFromFile);
            if (tc.isEventEnabled())
                Tr.event(tc, "Client version number is " + _serviceVersion);
            if (tc.isEventEnabled())
                Tr.event(tc, "Version number from file is " + serviceVersionFromFile);
        }
        else
        {
            serviceCompatible = true;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serviceCompatible", new Boolean(serviceCompatible));
        return serviceCompatible;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.logFileHeader
    //------------------------------------------------------------------------------
    /**
     * Accessor for the log file header object assoicated with this LogFileHandle
     * instance.
     * 
     * @return LogFileHeader The log file header object associated with this LogFileHandle
     *         instance.
     */
    protected LogFileHeader logFileHeader()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logFileHeader", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "logFileHeader", _logFileHeader);
        return _logFileHeader;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.getServiceData         
    //------------------------------------------------------------------------------
    /**
     * Accessor for the service data assoicated with this LogFileHandle instance.
     * 
     * @return byte[] The service data associated with this LogFileHandle instance or
     *         null if none exists.
     */
    public byte[] getServiceData()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getServiceData", this);

        byte[] serviceData = null;

        if (_logFileHeader != null)
        {
            serviceData = _logFileHeader.getServiceData();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getServiceData", RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES));
        return serviceData;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.freeBytes
    //------------------------------------------------------------------------------
    /**
     * Returns the number of free bytes remaining in the file associated with this
     * LogFileHandle instance.
     * 
     * @return long The number of free bytes remaining.
     */
    public int freeBytes()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "freeBytes", this);

        int freeBytes = 0;

        try
        {
            int currentCursorPosition = _fileBuffer.position();
            int fileLength = _fileBuffer.capacity();

            freeBytes = fileLength - currentCursorPosition;

            if (freeBytes < 0)
            {
                freeBytes = 0;
            }
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.freeBytes", "956", this);
            freeBytes = 0;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "freeBytes", new Integer(freeBytes));

        return freeBytes;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.setServiceData
    //------------------------------------------------------------------------------
    /**
     * Updates the service data stored in memory. This operation does not cause the
     * updated service data to be written to disk - this is is achieved by keypointing
     * the log. The reason that the data can't just be written to disk is that its
     * stored in the log file header at the top of the file and its length can't be
     * changed whilst the file is active.
     * 
     * @param serviceData The new sercvice data
     */
    public void setServiceData(byte[] serviceData)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setServiceData", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        _logFileHeader.setServiceData(serviceData);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setServiceData");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.fileName
    //------------------------------------------------------------------------------
    /**
     * Returns the name of the file associated with this LogFileHandle instance
     * 
     * @return String The name of the file associated with this LogFileHandle instance
     */
    public String fileName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fileName", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "fileName", _fileName);
        return _fileName;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.keypointStarting
    //------------------------------------------------------------------------------
    /**
     * Informs the LogFileHandle instance that a keypoint operation is about
     * begin into the file associated with this LogFileHandle instance. The status of the
     * log file is updated to KEYPOINTING and written to disk.
     * 
     * @param nextRecordSequenceNumber The sequence number to be used for the first
     *            record in the file.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void keypointStarting(long nextRecordSequenceNumber) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypointStarting", new Object[] { new Long(nextRecordSequenceNumber), this });

        // Set the header to indicate a keypoint operation. This also marks the header
        // as valid.
        _logFileHeader.keypointStarting(nextRecordSequenceNumber);
        try
        {
            writeFileHeader(false);
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.keypointStarting", "1073", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointStarting", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.keypointStarting", "1079", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointStarting", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypointStarting");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.keypointComplete
    //------------------------------------------------------------------------------
    /**
     * Informs the LogFileHandle instance that a keypoint operation into the file
     * has completed. The status of the log file is updated to ACTIVE and written
     * to disk.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void keypointComplete() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypointComplete", this);

        _logFileHeader.keypointComplete();
        try
        {
            writeFileStatus(true);
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.keypointComplete", "1117", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointComplete", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.keypointComplete", "1123", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointComplete", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypointComplete");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.becomeInactive
    //------------------------------------------------------------------------------
    /**
     * This method is invoked to inform the LogFileHandle instance that the file
     * it manages is no longer required by the recovery log serivce. The status
     * field stored in the header is updated to INACTIVE.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void becomeInactive() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "becomeInactive", this);

        _logFileHeader.changeStatus(LogFileHeader.STATUS_INACTIVE);
        try
        {
            writeFileStatus(false);
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.becomeInactive", "1161", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "becomeInactive", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.becomeInactive", "1167", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "becomeInactive", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "becomeInactive", this);
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.becomeActive
    //------------------------------------------------------------------------------
    /**
     * Called by the controlling logic to inform this file handle that it represents
     * a brand new file that has been selected to recieve the first data written to
     * the log. This method will only be called during a cold start when there are
     * no existing log files available for recovery. Both file handles that make up
     * the log have been pre-set to INACTIVE state and this method is used to set
     * one (and only one) to ACTIVE state.
     */
    void becomeActive()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "becomeActive", this);

        _logFileHeader.changeStatus(LogFileHeader.STATUS_ACTIVE);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "becomeActive");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.fileExtend
    //------------------------------------------------------------------------------
    /**
     * Extend the physical log file. If 'newFileSize' is larger than the current file
     * size, the log file will extended so that it is 'newFileSize' kilobytes long.
     * If 'newFileSize' is equal to or less than the current file size, the current
     * log file will be unchanged.
     * 
     * @param newFileSize The new file size for the physical log file (in kbytes).
     * @exception LogAllocationException The system was unable to expand the log file.
     */
    public void fileExtend(int newFileSize) throws LogAllocationException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fileExtend", new Object[] { new Integer(newFileSize), this });

        final int fileLength = _fileBuffer.capacity();

        if (fileLength < newFileSize)
        {
            try
            {
                // Expand the file to the new size ensuring that its pointer
                // remains in its current position.
                int originalPosition = _fileBuffer.position();

                // TODO
                //        Tr.uncondEvent(tc, "Expanding log file to size of " + newFileSize + " bytes.");
                Tr.event(tc, "Expanding log file to size of " + newFileSize + " bytes.");

                if (_isMapped)
                {
                    _fileBuffer = _fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, newFileSize);
                }
                else
                {
                    // The file is not mapped.
                    // Allocate a new DirectByteBuffer, copy the old ByteBuffer into the new one,
                    // then write the new ByteBuffer to the FileChannel (automatically expanding it).
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "File is NOT mapped.  Allocating new DirectByteBuffer");

                    _fileBuffer.position(0);

                    final ByteBuffer newByteBuffer = ByteBuffer.allocateDirect(newFileSize);
                    newByteBuffer.put(_fileBuffer);
                    newByteBuffer.position(0);

                    _fileChannel.write(newByteBuffer, 0);
                    _fileBuffer = newByteBuffer;
                }

                _fileBuffer.position(originalPosition);
                _fileSize = newFileSize;
            } catch (Exception exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.fileExtend", "1266", this);
                if (tc.isEntryEnabled())
                    Tr.event(tc, "Unable to extend file " + _fileName + " to " + newFileSize + " bytes");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "fileExtend", "LogAllocationException");
                throw new LogAllocationException(exc);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fileExtend");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.force
    //------------------------------------------------------------------------------ 
    /**
     * Forces the contents of the memory-mapped view of the log file to disk.
     * Having invoked this method the caller can be certain that any data added
     * to the log as part of a prior log write is now stored persistently on disk.
     */
    protected void force() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "force", this);

        try
        {
            if (_isMapped)
            {
                // Note: on Win2K we can get an IOException from this even though it is not declared
                ((MappedByteBuffer) _fileBuffer).force();
            }
            else
            {
                // Write the "pending" WritableLogRecords.
                writePendingToFile();
                _fileChannel.force(false);
            }
        } catch (java.io.IOException ioe)
        {
            FFDCFilter.processException(ioe, "com.ibm.ws.recoverylog.spi.LogFileHandle.force", "1049", this);
            _exceptionInForce = true;
            if (tc.isEventEnabled())
                Tr.event(tc, "Unable to force file " + _fileName);

            // d453958: moved terminateserver code to MultiScopeRecoveryLog.markFailed method.

            if (tc.isEntryEnabled())
                Tr.exit(tc, "force", "InternalLogException");
            throw new InternalLogException(ioe);
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.force", "1056", this);
            _exceptionInForce = true;
            if (tc.isEventEnabled())
                Tr.event(tc, "Unable to force file " + _fileName);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "force", "InternalLogException");
            throw exc;
        } catch (LogIncompatibleException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHandle.force", "1096", this);
            _exceptionInForce = true;
            if (tc.isEventEnabled())
                Tr.event(tc, "Unable to force file " + _fileName);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "force", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "force");
    }

//------------------------------------------------------------------------------
    // Method: LogFileHandle.writeLogRecord
    //------------------------------------------------------------------------------
    /**
     * Do any necessary under the covers work to write the LogRecord.
     * If the FileChannel is mapped by a MappedByteBuffer, then there is nothing
     * to do here, as the data has already been written to the FileChannel.
     * If the FileChannel is represented by a DirectByteBuffer, then write the
     * LogRecord into an ordered list of "pending writes" that will eventually be
     * written to the FileChannel and back to disk on the next force().
     * Synchronize the inserts into the pendingWrites list.
     */
    protected void writeLogRecord(LogRecord logRecord) /* @MD18931A */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeLogRecord", logRecord);

        if (!_isMapped)
        {
            synchronized (_pendingWriteList)
            {
                // Add the logrecord to the ordered list of "pending writes".
                // Find where it belongs, then add it.  
                _pendingWriteList.add(logRecord);
            }
        }

        // if we've got here then forces prior to shutdown will ensure log records added
        // are really in the file.
        _outstandingWritableLogRecords.decrementAndGet();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeLogRecord");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.writePendingToFile
    //------------------------------------------------------------------------------
    /**
     * Write the pendingWrites list to the FileChannel.
     * This operation is likely immediate followed by a FileChannel.force().
     */
    private void writePendingToFile() throws java.io.IOException, InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writePendingToFile");

        synchronized (_pendingWriteList)
        {
            LogRecord[] records = (LogRecord[]) _pendingWriteList.toArray(new LogRecord[0]);

            // Don't use vectored IO unless we're told to.
            if (!Boolean.getBoolean("com.ibm.ws.recoverylog.spi.UseVectoredIO"))
            {
                // Put the file header at the front of the list
                ByteBuffer header = (ByteBuffer) _fileBuffer.duplicate().position(0).limit(_logFileHeader.length());
                _fileChannel.write(header, 0);

                // Write the remainder of the list
                for (int i = 0; i < records.length; i++)
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Performing write at position " + records[i].absolutePosition());
                    _fileChannel.write((ByteBuffer) records[i]._buffer.flip(), records[i].absolutePosition());
                }
            }
            else
            {
                // Sort the records into ascending order based on their absolute
                // position in the file.  We're going to try and keep the disk
                // moving forward.
                Arrays.sort(records, _recordComparator);

                // Now separate the records into two arrays:
                //   o ByteBuffers of data
                //   o Absolute File positions
                ByteBuffer[] buffers = new ByteBuffer[records.length + 1];
                int[] positions = new int[records.length + 1];

                for (int i = 0; i < records.length; i++)
                {
                    buffers[i + 1] = (ByteBuffer) records[i]._buffer.flip();
                    positions[i + 1] = records[i].absolutePosition();
                }

                // Put the file header at the front of the list
                buffers[0] = (ByteBuffer) _fileBuffer.duplicate().position(0).limit(_logFileHeader.length());
                positions[0] = 0;

                // Here's where we go through the list and attempt to write
                // batches of adjacent records using vectored I/O.
                int idx = 0;
                while (idx < buffers.length)
                {
                    int min = idx;
                    int max = idx;

                    // Somewhere in the middle
                    while (max <= (buffers.length - 2))
                    {
                        if (positions[max + 1] == (positions[max] + buffers[max].remaining()))
                            max++;
                    }

                    // Do the actual writes                    
                    _fileChannel.position(positions[min]);
                    _fileChannel.write(buffers, min, (max - min + 1));

                    // Go back and find the next batch
                    idx = max + 1;
                }
            }

            // Cleanup the pending write list for the next force
            _pendingWriteList.clear();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writePendingToFile");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHandle.resetHeader
    //------------------------------------------------------------------------------
    /**
     * Ensure that the internal state of a header if valid, by copying a known
     * good header to an invalid header. This method is called during the initialization
     * and validation of a pair of recovery log files.
     */
    public void resetHeader(LogFileHandle validFile)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resetHeader");
        _logFileHeader.resetHeader(validFile.logFileHeader());
        if (tc.isEntryEnabled())
            Tr.exit(tc, "resetHeader");
    }

    @Override
    public String toString()
    {
        return "LogFileHandle: " + _serviceName + " " + _logName;
    }
}

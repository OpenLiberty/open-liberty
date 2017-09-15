/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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
import java.util.ArrayList;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
//Class: LogHandle
//------------------------------------------------------------------------------
/**
 * <p>
 * This class provides access to the underlying recovery log through the creation of
 * LogRecords. A LogRecord provides access to the physical file that contains the
 * recovery information through getter and setter methods such as getInt() and
 * setInt(). There are two types of LogRecord, ReadableLogRecord and WritableLogRecord.
 * As its name suggests, ReadableLogRecord provides supports for read operations whilst
 * WritableLogRecord provides support for both read and write operations.
 * </p>
 * 
 * <p>
 * This class acts as a middle-man, managing the pair of files that form the recovery
 * log and allocating LogRecords to allow other parts of the recovery log service to
 * read and write their data.
 * </p>
 * 
 * <p>
 * Each of the two files is represented by an instance of the LogFileHandle class. At
 * any one time, only one of these is being written to and this is said to be the
 * "active" file. This class automatically creates LogRecords that refer to the active
 * file.
 * </p>
 */
class LogHandle
{
    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(LogHandle.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * The name of the first recovery log file.
     */
    private static String RECOVERY_FILE_1_NAME = "log1";

    /**
     * The name of the second recovery log file.
     */
    private static String RECOVERY_FILE_2_NAME = "log2";

    /**
     * Reference to the recovery log that owns this instance of the LogHandle class
     */
    private final MultiScopeRecoveryLog _recoveryLog;

    /**
     * The name of the client service that owns the recovery log.
     */
    private final String _serviceName;

    /**
     * The version number of the client service that owns the recovery log.
     */
    private final int _serviceVersion;

    /**
     * The name of the application server for which the LogHandle has been created.
     */
    private final String _serverName;

    /**
     * The unique (within service) name of the recovery log managed by this LogHandle.
     */
    private final String _logName;

    /**
     * The directory path under which the files that make up this recovery log will
     * be stored.
     */
    private final String _logDirectory;

    /**
     * The size of this recovery log in kilobytes.
     */
    private final int _logFileSize;

    /**
     * The maximum log file size in kilobytes
     */
    private final int _maxLogFileSize;

    /**
     * Reference to one of the two recovery log files.
     */
    private LogFileHandle _file1;

    /**
     * Reference to one of the two recovery log files.
     */
    private LogFileHandle _file2;

    /**
     * Reference to the currently active recovery log file.
     */
    private LogFileHandle _activeFile;

    /**
     * Reference to the currently in-active recovery log file.
     */
    private LogFileHandle _inactiveFile;

    /**
     * The current record sequence number that will be allocated to the next log record. Increases
     * by 1 each time a log record is created.
     */
    private long _recordSequenceNumber;

    /**
     * An array of ReadableLogRecords that have been extracted from the recovery log
     * by the LogHandle.openLog() method.
     */
    private ArrayList<ReadableLogRecord> _recoveredRecords;

    /**
     * The physical free bytes are the number of bytes that are currently available on disk
     * in the target extent file for storage of further information
     */
    public int _physicalFreeBytes;

    /**
     * The service data is an arbitrary block of bytes the size and content of which
     * is not defined by the recovery log service. The service data is initially
     * configured when the recovery log is opened. It can be changed a single time
     * when the recoveryCompelte method is called. After that point, its fixed and
     * can only be changed when the log is closed and then re-opened.
     */
    private byte[] _serviceData;

    /**
     */
    FailureScope _failureScope;

    //------------------------------------------------------------------------------
    // Method: LogHandle.LogHandle          
    //------------------------------------------------------------------------------
    /*
     * Package access constructor to create a LogHandle object to manage disk access
     * for the supplied RecoveryLog instance.
     * 
     * The caller must ensure that the maximum size is greater than or equal to the
     * initial size.
     * 
     * @param recoveryLog The RecoveryLog instance that is creating the LogHandle
     * 
     * @param serviceName The name of the client service.
     * 
     * @param serviceVersion The version number of the client service.
     * 
     * @param serverName The name of the current application server
     * 
     * @param logDirectory The target path to the required recovery log
     * 
     * @param logFileSize The initial log file size (in kbytes)
     * 
     * @param maxLogFileSize The maximum allowable log file size (in kbytes)
     */
    LogHandle(MultiScopeRecoveryLog recoveryLog, String serviceName, int serviceVersion, String serverName, String logName, String logDirectory, int logFileSize,
              int maxLogFileSize, FailureScope fs)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "LogHandle", new java.lang.Object[] { recoveryLog,
                                                              serviceName,
                                                              new Integer(serviceVersion),
                                                              serverName,
                                                              logName,
                                                              logDirectory,
                                                              logFileSize,
                                                              maxLogFileSize,
                                                              fs });

        _recoveryLog = recoveryLog;
        _serviceName = serviceName;
        _serviceVersion = serviceVersion;
        _serverName = serverName;
        _logName = logName;
        _logDirectory = logDirectory;
        _maxLogFileSize = maxLogFileSize;
        _logFileSize = logFileSize;
        _failureScope = fs;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "LogHandle", this);
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.openLog          
    //------------------------------------------------------------------------------
    /*
     * Package access method to open the LogHandle for use. The required directory hierarchy
     * and recovery log files will be created if they do not already exist. If the recovery
     * log files do exist, this method will select the 'latest' of the two files based on
     * their header content and then re-load the individual records into memory. These
     * records can then be retrieved and processed via the LogHandle.recoveredRecords() method.
     * 
     * @exception LogCorruptedException The recovery log files have become corrupted
     * such that the log cannot be opened.
     * 
     * @exception LogAllocationException The required log files could not be created (eg
     * insufficnent disk space)
     * 
     * @exception InternalLogException An unexpected failure has occured.
     */
    void openLog() throws LogOpenException, LogCorruptedException, LogAllocationException, InternalLogException,
                    LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "openLog", this);

        // Check that the file is not already open.
        if (_activeFile != null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "LogOpenException");
            throw new LogOpenException(null);
        }

        // Create (ensure) the required directory tree
        if (!(RLSUtils.createDirectoryTree(_logDirectory)))
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "LogAllocationException");
            throw new LogAllocationException(null);
        }

        // Create DO NOT DELETE LOG FILES file in logs directory
        createWarningFile();

        // Now construct the two LogFileHandle objects that manage the individual log
        // files. 
        //
        // There is a bug in the HP JVM's NIO implementation that causes it to fail when
        // an attempt to expand the mapped files is made. To overcome this when running
        // on HP the log files must be opened at their configured maximum size so that
        // they will an attempt to expand them will never be made.
        final String osName = System.getProperty("os.name");

        if (osName.equalsIgnoreCase("HPUX") || osName.equalsIgnoreCase("HP-UX"))
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Running on HP-UX, open logs with initial size of maximum log size");

            _file1 = new LogFileHandle(_logDirectory, RECOVERY_FILE_1_NAME, _serverName, _serviceName, _serviceVersion, _logName, _maxLogFileSize, _failureScope);
            _file2 = new LogFileHandle(_logDirectory, RECOVERY_FILE_2_NAME, _serverName, _serviceName, _serviceVersion, _logName, _maxLogFileSize, _failureScope);
        }
        else
        {
            _file1 = new LogFileHandle(_logDirectory, RECOVERY_FILE_1_NAME, _serverName, _serviceName, _serviceVersion, _logName, _logFileSize, _failureScope);
            _file2 = new LogFileHandle(_logDirectory, RECOVERY_FILE_2_NAME, _serverName, _serviceName, _serviceVersion, _logName, _logFileSize, _failureScope);
        }

        _activeFile = null;
        _inactiveFile = null;

        // If one of the recovery log files has been deleted the recovery log has potentially been
        // damaged. In some situations it might be possible to recover, but the integrity has
        // been comprimized and we should be somewhat draconian in this case. Throw an exception. 
        if (_file1.fileExists() != _file2.fileExists())
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "One of the recovery log files has been deleted. Recovery can't complete");
            _file1 = null;
            _file2 = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "LogCorruptedException");
            throw new LogCorruptedException(null);
        }

        // Determine if this will be a 'cold' start (no existing log files) or a 'warm' start  
        // (existing log files to recover) 
        boolean coldStart = !_file1.fileExists();

        // If this is a cold start then we need to choose which of the two recovery log files
        // should be written to first. This choice is arbitrary - by convention always pick the
        // the first file. Calling the activate method on the file changes its state to ACTIVE
        // and by doing so ensures that it will be selected as the target file by the logic
        // below (file two will be INACTIVE in this state)
        if (coldStart)
        {
            _file1.becomeActive();

            Tr.info(tc, "CWRLS0007_RECOVERY_LOG_NOT_EXIST", _logDirectory);
        }

        // Open the files. If this is a cold start then they will be created and the headers written
        // to disk. If this is a warm start, the existing headers will be loaded from disk.
        try
        {
            _file1.fileOpen();
        } catch (LogIncompatibleException exc)
        {
            // The _file1 log file has been opened and examined, but was found to be incompatible with the required
            // log for one of the following reasons:
            // 
            // 1. The file was created by a different service
            // 2. The file was created by this service but is a different log file
            // 3. The file was created by a later version of this service and has a different content format.
            // 
            // The reason will be preserved in the trace file. The log file can't be opened and an exception will
            // be generated.

            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "391", this);

            _file1 = null;
            _file2 = null;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", exc);
            throw exc;
        } catch (InternalLogException ile)
        {
            FFDCFilter.processException(ile, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "331", this);
            _file1 = null;
            _file2 = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", ile);
            throw ile;
        } catch (LogAllocationException lae)
        {
            FFDCFilter.processException(lae, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "339", this);
            _file1 = null;
            _file2 = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", lae);
            throw lae;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "347", this);
            _file1 = null;
            _file2 = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "InternalLogException");
            throw new InternalLogException(exc);
        }

        try
        {
            _file2.fileOpen();
        } catch (LogIncompatibleException exc)
        {
            // The _file2 log file has been opened and examined, but was found to be incompatible with the required
            // log for one of the following reasons:
            // 
            // 1. The file was created by a different service
            // 2. The file was created by this service but is a different log file
            // 3. The file was created by a later version of this service and has a different content format.
            // 
            // The reason will be preserved in the trace file. The log file can't be opened and an exception will
            // be generated.

            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "449", this);

            _file1 = null;
            _file2 = null;

            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", exc);
            throw exc;
        } catch (InternalLogException ile)
        {
            FFDCFilter.processException(ile, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "369", this);
            _file1 = null;
            _file2 = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", ile);
            throw ile;
        } catch (LogAllocationException lae)
        {
            FFDCFilter.processException(lae, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "377", this);
            _file1 = null;
            _file2 = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", lae);
            throw lae;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "385", this);
            _file1 = null;
            _file2 = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "InternalLogException");
            throw new InternalLogException(exc);
        }

        // If neither file had a valid header then the recovery log is totally corrupted.
        if ((_file1 == null) || (_file2 == null))
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "Neither of the recovery log files are valid. Recovery can't complete");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "LogCorruptedException");
            throw new LogCorruptedException(null);
        }

        if (_file2.logFileHeader().status() == LogFileHeader.STATUS_INVALID)
        {
            if (_file1.logFileHeader().status() == LogFileHeader.STATUS_ACTIVE)
            {
                // The only valid file is file1. This file is ACTIVE - ie not INACTIVE or KEYPOINTING
                if (tc.isEventEnabled())
                    Tr.event(tc, "Selecting recovery log file '" + _file1.fileName() + "' as its ACTIVE and '" + _file2.fileName() + "' is not valid");
                _activeFile = _file1;
                _inactiveFile = _file2;
                _inactiveFile.resetHeader(_activeFile);
            }
            else
            {
                // The only valid file is file1. Unfortunatly, this file is not ACTIVE so it can't be used to recover.
                if (tc.isEventEnabled())
                    Tr.event(tc, "The only valid recovery log file '" + _file1.fileName() + "' is not in ACTIVE state");

                _file1 = null;
                _file2 = null;

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "LogCorruptedException");
                throw new LogCorruptedException(null);
            }
        }
        else if (_file1.logFileHeader().status() == LogFileHeader.STATUS_INVALID)
        {
            if (_file2.logFileHeader().status() == LogFileHeader.STATUS_ACTIVE)
            {
                // The only valid file is file2. This file is ACTIVE - ie not INVALID or KEYPOINTING
                if (tc.isEventEnabled())
                    Tr.event(tc, "Selecting recovery log file '" + _file2.fileName() + "' as its ACTIVE and '" + _file1.fileName() + "' is not valid");
                _activeFile = _file2;
                _inactiveFile = _file1;
                _inactiveFile.resetHeader(_activeFile);
            }
            else
            {
                // The only valid file is file2. Unfortunatly, this file is not ACTIVE so it can't be used to recover.
                if (tc.isEventEnabled())
                    Tr.event(tc, "The only valid recovery log file '" + _file2.fileName() + "' is not in ACTIVE state");

                _file1 = null;
                _file2 = null;

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "LogCorruptedException");
                throw new LogCorruptedException(null);
            }
        }
        else
        {
            // Both files have valid headers.

            final int file1Status = _file1.logFileHeader().status();
            final int file2Status = _file2.logFileHeader().status();
            final long file1FRSN = _file1.logFileHeader().firstRecordSequenceNumber();
            final long file2FRSN = _file2.logFileHeader().firstRecordSequenceNumber();
            final long file1Date = _file1.logFileHeader().date();
            final long file2Date = _file2.logFileHeader().date();

            if ((file1Status == LogFileHeader.STATUS_ACTIVE) && (file2Status != LogFileHeader.STATUS_ACTIVE))
            {
                // File 1 is ACTIVE whilst file2 is not ACTIVE. Use file 1
                if (tc.isEventEnabled())
                    Tr.event(tc, "Selecting recovery log file '" + _file1.fileName() + "' as its ACTIVE and '" + _file2.fileName() + "' is not ACTIVE");
                _activeFile = _file1;
                _inactiveFile = _file2;
            }
            else if ((file1Status != LogFileHeader.STATUS_ACTIVE) && (file2Status == LogFileHeader.STATUS_ACTIVE))
            {
                // File 2 is ACTIVE whilst file1 is not ACTIVE. Use file 2
                if (tc.isEventEnabled())
                    Tr.event(tc, "Selecting recovery log file '" + _file2.fileName() + "' as its ACTIVE and '" + _file1.fileName() + "' is not ACTIVE");
                _activeFile = _file2;
                _inactiveFile = _file1;
            }
            else if ((file1Status == LogFileHeader.STATUS_ACTIVE) && (file2Status == LogFileHeader.STATUS_ACTIVE))
            {
                // Both files are in active state. This is an unusual case as the window for having two files in ACTIVE
                // state is very small. The keypointing logic switches the 'old' file from ACTIVE to INACTIVE state
                // immediatly after changeing the 'new' file from KEYPOINTING to ACTIVE state. To get this situation to
                // occur, the server has to fail between these two state changes.
                if (tc.isEventEnabled())
                    Tr.event(tc, "Both recovery files are in ACTIVE state. Determine target file by examining timestamp");

                if (file1Date > file2Date)
                {
                    // File 1 was created after file 2 so select file 1.
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Selecting recovery log file '" + _file1.fileName() + "' as it was created after '" + _file2.fileName() + "'");
                    _activeFile = _file1;
                    _inactiveFile = _file2;
                }
                else if (file1Date < file2Date)
                {
                    // File 2 was created after file 1 so select file 2.
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Selecting recovery log file '" + _file2.fileName() + "' as it was created after '" + _file1.fileName() + "'");
                    _activeFile = _file2;
                    _inactiveFile = _file1;
                }
                else
                {
                    // Both files were created at the same time. This seems unlikely as the times are in milliseconds but
                    // its possible, so choose between them by examining the first record sequence numbers and picking
                    // the file that contains the newer records (larger number).
                    if (tc.isEventEnabled())
                        Tr.event(tc, "Both recovery files have the same timestamp. Determine target file by examining first record sequence numbers");

                    if ((file1FRSN == 0) && (file2FRSN == 0))
                    {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Selecting recovery log file '" + _file1.fileName() + "' as both files are empty");
                        // Totally empty log
                        _activeFile = _file1;
                        _inactiveFile = _file2;
                    }
                    else if (file1FRSN > file2FRSN)
                    {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Selecting recovery log file '" + _file1.fileName() + "' as it has as the highest record sequence number");
                        _activeFile = _file1;
                        _inactiveFile = _file2;
                    }
                    else if (file1FRSN < file2FRSN)
                    {
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Selecting recovery log file '" + _file2.fileName() + "' as it has as the highest record sequence number");
                        _activeFile = _file2;
                        _inactiveFile = _file1;
                    }
                    else
                    {
                        _file1 = null;
                        _file2 = null;
                        if (tc.isEventEnabled())
                            Tr.event(tc, "Neither of the recovery log files are valid as they have the same record sequence number. Recovery can't complete");
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "openLog", "LogCorruptedException");
                        throw new LogCorruptedException(null);
                    }
                }
            }
            else
            {
                _file1 = null;
                _file2 = null;
                if (tc.isEventEnabled())
                    Tr.event(tc, "The recovery lof files are in an undefined state");
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "openLog", "LogCorruptedException");
                throw new LogCorruptedException(null);
            }
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Selected log file '" + _activeFile.fileName() + "' for recovery");

        // Lookup the service data from the current target file. This will either be null (if this
        // was a cold start) or whatever service data was read from the selected target file.
        _serviceData = _activeFile.getServiceData();

        // Pass the service data to the file not selected for recovery. This ensures that when
        // this file is used it will have the service data originally recovered from the target
        // file.
        if (_activeFile == _file1)
        {
            _file2.setServiceData(_serviceData);
        }
        else
        {
            _file1.setServiceData(_serviceData);
        }

        // Now recover the record data that can be used to re-construct the recoverable units and sections
        try
        {
            _recoveredRecords = readRecords();
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "554", this);
            _file1 = null;
            _file2 = null;
            _activeFile = null;
            _inactiveFile = null;
            _recoveredRecords = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.openLog", "565", this);
            _file1 = null;
            _file2 = null;
            _activeFile = null;
            _inactiveFile = null;
            _recoveredRecords = null;
            if (tc.isEntryEnabled())
                Tr.exit(tc, "openLog", "InternalLogException");
            throw new InternalLogException(exc);
        }

        // At this point one of the following will apply:
        //
        // 1. The file has been created from scratch and has a header containing a first record sequence number
        // of zero. The file has been expanded to the required file size and the file cursor is positioned on
        // the first byte after the header. No records are stored in memory.
        // 2. An existing file has been opened and the valid header loaded into memory. The first record sequence
        // number in the header was zero, so no records have been loaded into memory. The file has been expanded
        // to the required file size and the file cursor is positioned on the first byte after the header.
        // 3. An existing file has been opened and the valid header loaded into memory. The first record sequence
        // number in the header non-zero, so the records have been loaded into memory. The file has been expanded
        // to the required file size and the file cursor is positioned on the first byte after the last valid record.

        // Determine the remaining free bytes in the file
        _physicalFreeBytes = _activeFile.freeBytes();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "openLog");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.readRecords
    //------------------------------------------------------------------------------
    /*
     * Private method to read the records stored in the active log file back into memory. This
     * method should only be called from the LogHandle.openLog method. Once loaded, these
     * records can be accessed through the LogHandle.recoveredRecords method.
     * 
     * @return ArrayList An array of the recovered records.
     * 
     * @exception InternalLogException An unexpected failure has occured.
     */
    private ArrayList<ReadableLogRecord> readRecords() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "readRecords", this);

        // Check that the file is actually open
        if (_activeFile == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "readRecords", "InternalLogException");
            throw new InternalLogException(null);
        }

        ArrayList<ReadableLogRecord> records = null;

        // Retrieve the first record sequence number from the header. If this file has
        // been freshly created (ie cold start) then this will be zero, the sequence
        // number for the first ever record. Otherwise, the sequence number will be
        // been retrieved from the active file.
        _recordSequenceNumber = _activeFile.logFileHeader().firstRecordSequenceNumber();

        // Keep extracting log records until no more are found.
        ReadableLogRecord readableLogRecord = _activeFile.getReadableLogRecord(_recordSequenceNumber);

        // If there is a "first" record then create the array that will be used to store these
        // records as they are retrieved.
        if (readableLogRecord != null)
        {
            records = new ArrayList<ReadableLogRecord>();
        }

        while (readableLogRecord != null)
        {
            records.add(readableLogRecord);

            // 776666/PI69183 - may need to adjust sequence number if byte-by-byte scanning finds more records.
            long realSequenceNumber = readableLogRecord.getSequenceNumber();
            if (_recordSequenceNumber != realSequenceNumber)
            {
                if (realSequenceNumber > _recordSequenceNumber)
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "byte-by-byte scanning found records. Adjusting record sequence number:" + _recordSequenceNumber + " to:" + realSequenceNumber);
                    _recordSequenceNumber = realSequenceNumber;
                }
                else
                {
                    // This SHOULD and I think CAN'T happen
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "ERROR byte-by-byte scanning found records. Expected to find record sequence number:" + _recordSequenceNumber + " but found :"
                                     + realSequenceNumber);
                    FFDCFilter.processException(new Exception(), "com.ibm.ws.recoverylog.spi.LogHandle.readRecords", "685", this,
                                                new Object[] { String.valueOf(_recordSequenceNumber), String.valueOf(realSequenceNumber) });
                }
            }

            _recordSequenceNumber++;
            readableLogRecord = _activeFile.getReadableLogRecord(_recordSequenceNumber);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Next record sequence number is " + _recordSequenceNumber);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "readRecords", records);
        return records;
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.keypointStarting
    //------------------------------------------------------------------------------
    /*
     * A package access utility method to prepare the underlying recovery log file
     * for a keypoint operation. The active file is switched and a keypoint header
     * is then written to it.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void keypointStarting() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypointStarting", this);

        // Check that the file is actually open
        if (_activeFile == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointStarting", "InternalLogException");
            throw new InternalLogException(null);
        }

        // Choose the 'other' file to be the target for the keypoint operation.
        if (_activeFile == _file1)
        {
            _activeFile = _file2;
            _inactiveFile = _file1;
        }
        else
        {
            _activeFile = _file1;
            _inactiveFile = _file2;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Keypoint processing is switching to log file " + _activeFile.fileName());

        // _activeFile is in INACTIVE state, other file is in ACTIVE state
        try
        {
            _activeFile.keypointStarting(_recordSequenceNumber);
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.keypointStarting", "690", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointStarting", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.keypointStarting", "696", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointStarting", "InternalLogException");
            throw new InternalLogException(exc);
        }

        // Now determine how many bytes are currently available in the new target log file.
        _physicalFreeBytes = _activeFile.freeBytes();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypointStarting");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.closeLog
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Package access method to close the recovery log. This method directs closes the
     * two log files and then clears its internal state.
     * </p>
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void closeLog() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "closeLog", this);

        // Check that the file is actually open
        if (_activeFile == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "closeLog", "InternalLogException");
            throw new InternalLogException(null);
        }

        try
        {
            _file1.fileClose();
            _file2.fileClose();
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.closeLog", "736", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "closeLog", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.closeLog", "742", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "closeLog", "InternalLogException");
            throw new InternalLogException(exc);
        }

        _file1 = null;
        _file2 = null;
        _activeFile = null;
        _recoveredRecords = null;
        _physicalFreeBytes = 0;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "closeLog");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.getServiceData
    //------------------------------------------------------------------------------
    /**
     * Package access method to return the current service data.
     * 
     * @return The current service data.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    byte[] getServiceData() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getServiceData", this);

        // Check that the file is actually open
        if (_activeFile == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getServiceData", "InternalLogException");
            throw new InternalLogException(null);
        }

        final byte[] serviceData = _activeFile.getServiceData();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getServiceData", RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES));
        return serviceData;
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.force
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Package access method to ensure that all information written to the active file
     * is forced out to persistent storeage.
     * </p>
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void force() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "force", this);

        // Check that the file is actually open
        if (_activeFile == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "force", "InternalLogException");
            throw new InternalLogException(null);
        }

        // Attempt to get exclusive lock on the lock object provided by RecoveryLogService
        // to protect access to the isSuspended flag, which is toggled during calls
        // to RecoveryLogService suspend/resume
        synchronized (RLSControllerImpl.SUSPEND_LOCK)
        {
            while (RLSControllerImpl.isSuspended())
            {
                try
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Waiting for RecoveryLogService to resume");
                    RLSControllerImpl.SUSPEND_LOCK.wait();
                } catch (InterruptedException exc)
                {
                    // This exception is received if another thread interrupts this thread by calling this threads 
                    // Thread.interrupt method. The RecoveryLogService class does not use this mechanism for 
                    // breaking out of the wait call - it uses notifyAll to wake up all waiting threads. This
                    // exception should never be generated. If for some reason it is called then ignore it and 
                    // start to wait again.
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.force", "834", this);
                }
            }

            // Check if we've been configured to run in snapshot safe mode
            // With this in place we synchronize all force method calls and
            // take the synchronization performance hit.  The benefit is that we
            // can be sure that the log files will be in a consistent state
            // for snapshotting
            if (Configuration._isSnapshotSafe)
            {
                _activeFile.force();
            }
        }

        //  If we're not configured to be snapshot safe then we
        //  run a slight risk here that a force operation could be called
        //  just after the RecoveryLogService has been called to suspend,
        //  as we're outwith the sync block.
        //  The benefit is that we don't have the synchronization performance 
        //  hit when calling the force method 
        if (!Configuration._isSnapshotSafe)
        {
            _activeFile.force();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "force");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.logFileHeader
    //------------------------------------------------------------------------------
    /**
     * Returns the LogFileHeader for the currently active log file.
     * 
     * @return The LogFileHeader for the currently active log file.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    LogFileHeader logFileHeader() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logFileHeader", this);

        // Check that the file is actually open
        if (_activeFile == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "logFileHeader", "InternalLogException");
            throw new InternalLogException(null);
        }

        final LogFileHeader logFileHeader = _activeFile.logFileHeader();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "logFileHeader", logFileHeader);
        return logFileHeader;
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.recoveredRecords
    //------------------------------------------------------------------------------
    /**
     * Returns an array of ReadableLogRecords retrieved when the recovery log was
     * opened.
     * 
     * @return ArrayList An array of ReadableLogRecords.
     */
    ArrayList<ReadableLogRecord> recoveredRecords()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "recoveredRecords", _recoveredRecords);
        return _recoveredRecords;
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.setServiceData
    //------------------------------------------------------------------------------
    /**
     * Package access method to replace the existing service data with the new service
     * data.
     * 
     * @param serviceData The new service data or null for no service data.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void setServiceData(byte[] serviceData) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setServiceData", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        // Check that the files are available
        if ((_file1 == null) || (_file2 == null))
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "setServiceData", "InternalLogException");
            throw new InternalLogException(null);
        }

        // Cache the service data buffer reference. This class logically 'owns' the buffer.
        _serviceData = serviceData;

        // Pass the reference to the underlying log file objects.
        _file1.setServiceData(serviceData);
        _file2.setServiceData(serviceData);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setServiceData");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.keypoint
    //------------------------------------------------------------------------------
    /**
     * <p>
     * This method forms part of the keypoint processing logic. It must only be called
     * from the RecoveryLog.keypoint() method.
     * </p>
     * 
     * <p>
     * RecoveryLog.keypoint will have re-written all currently active data to the
     * recovery logs keypoint file (the inactive file when the keypoint was triggered)
     * This method will now compelte the keypoint operation by forcing this data to
     * disk and then marking the old log file INACTIVE.
     * </p>
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    public void keypoint() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypoint", this);

        try
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Keypointing with isSnapshotSafe set to " + Configuration._isSnapshotSafe);

            // Attempt to get exclusive lock on the lock object provided by RecoveryLogService
            // to protect access to the isSuspended flag, which is toggled during calls
            // to RecoveryLogService suspend/resume
            synchronized (RLSControllerImpl.SUSPEND_LOCK)
            {
                while (RLSControllerImpl.isSuspended())
                {
                    try
                    {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Waiting for RecoveryLogService to resume");
                        RLSControllerImpl.SUSPEND_LOCK.wait();
                    } catch (InterruptedException exc)
                    {
                        // This exception is received if another thread interrupts this thread by calling this threads 
                        // Thread.interrupt method. The RecoveryLogService class does not use this mechanism for 
                        // breaking out of the wait call - it uses notifyAll to wake up all waiting threads. This
                        // exception should never be generated. If for some reason it is called then ignore it and 
                        // start to wait again.
                        FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.keypoint", "923", this);
                    }
                }

                if (Configuration._isSnapshotSafe)
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Keypointing with isSnapshotSafe set to " + Configuration._isSnapshotSafe);

                    // Check if we've been configured to run in snapshot safe mode
                    // With this in place we synchronize all keypointInternal method calls and
                    // take the synchronization performance hit.  The benefit is that we
                    // can be sure that the log files will be in a consistent state
                    // for snapshotting
                    keypointInternal();
                }
            }

            //  If we're not configured to be snapshot safe then we
            //  run a slight risk here that a force operation could be called
            //  just after the RecoveryLogService has been called to suspend,
            //  as we're outwith the sync block.
            //  The benefit is that we don't have the synchronization performance 
            //  hit when calling the force method 
            if (!Configuration._isSnapshotSafe)
            {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Keypointing with isSnapshotSafe set to " + Configuration._isSnapshotSafe);

                keypointInternal();
            }
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.keypoint", "932", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypoint", exc);
            throw exc;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypoint");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.keypointInternal
    //------------------------------------------------------------------------------
    /**
     * Called by the keypoint() method
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    private void keypointInternal() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypointInternal");

        try
        {
            //  _activeFile is in KEYPOINTING state, other file is in ACTIVE state

            force();

            // _activeFile is in KEYPOINTING state, other file is in ACTIVE state

            _activeFile.keypointComplete();

            // _activeFile is in ACTIVE(time2) state, other file is in ACTIVE(time1) (time2 later than time1)

            if (_activeFile == _file1)
            {
                _file2.becomeInactive();
            }
            else
            {
                _file1.becomeInactive();
            }
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.keypointInternal", "952", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointInternal", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.keypointInternal", "958", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "keypointInternal", "InternalLogException");
            throw new InternalLogException(exc);
        }

        // _activeFile is in ACTIVE state, other file is in INACTIVE state

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypointInternal");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.resizeLog
    //------------------------------------------------------------------------------
    /*
     * Resize the underlying recovery log.
     * 
     * This method it invoked when a keypoint operation detects that there is
     * insufficient room in a recovery log file to form a persistent record
     * of all the active data.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void resizeLog(int targetSize) throws InternalLogException, LogFullException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resizeLog", new Object[] { this, targetSize });

        // Check that both files are actually open
        if (_activeFile == null || _inactiveFile == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "resizeLog", "InternalLogException");
            throw new InternalLogException(null);
        }

        try
        {
            _file1.fileExtend(targetSize);
            _file2.fileExtend(targetSize);
        } catch (LogAllocationException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.resizeLog", "1612", this);

            // "Reset" the _activeFile reference. This is very important because when this keypoint operation
            // was started (keypointStarting) we switched the target file to the inactive file and prepared it
            // for a keypoint operation by updating its header block. Subsequently, the keypoint operation fails
            // which means that _activeFile currently points at an empty file into which we have been unable to
            // keypoint.
            //
            // We must leave this method with _activeFile pointing at the active file (as was the case prior
            // to the keypoint attempt). If we do not do this then any further keypoint operation will re-attempt
            // the same logic by switching from this empty file back to the active file. The active file will
            // be cleared in preparation for the keypoint operation (which will subsequently fail) and all
            // persistent information will be destroyed from disk.

            if (_activeFile == _file1)
            {
                _activeFile = _file2;
            }
            else
            {
                _activeFile = _file1;
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "resizeLog", "WriteOperationFailedException");
            throw new WriteOperationFailedException(exc);
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.resizeLog", "1555", this);

            // Reset the target file reference (see description above for why this must be done)
            if (_activeFile == _file1)
            {
                _activeFile = _file2;
            }
            else
            {
                _activeFile = _file1;
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "resizeLog", "InternalLogException");
            throw new InternalLogException(exc);
        }

        // Now determine how many bytes are currently available in the new target log file.
        _physicalFreeBytes = _activeFile.freeBytes();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Log file " + _activeFile.fileName() + " now has " + _physicalFreeBytes + " bytes of storage available");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "resizeLog");
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.getWritableLogRecord
    //------------------------------------------------------------------------------
    /*
     * <p>
     * Creates a new WritableLogRecord object to allow other parts of the recovery
     * log service to build a new log record. This new log record will be positioned
     * directly after the previous log record.
     * </p>
     * 
     * <p>
     * If there is insufficient space remaining in the active log file to create
     * a record of the required length, a keypoint operation will be triggered
     * to free up storage. In this event, a null WritableLogRecord will be returned.
     * This indicates to the caller that the recovery log now contains a persistent
     * record of all active data and that the write operation is no longer required.
     * </p>
     * 
     * @param recordLength The length of the required log record (excluding LogRecord
     * header and tail)
     * 
     * @exception InternalLogException An unexpected error has occured.
     * 
     * @return The new WritableLogRecord instance, or null if this write operation
     * can be ignored (as a result of a keypoint occuring)
     */
    protected WriteableLogRecord getWriteableLogRecord(int recordLength) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getWriteableLogRecord", new Object[] { this, recordLength });

        WriteableLogRecord writeableLogRecord = null;

        boolean keyPointRequired = false;

        synchronized (this)
        {
            if (_activeFile.freeBytes() < (recordLength + WriteableLogRecord.HEADER_SIZE))
            {
                // There is insufficient space in the active file to accomodate a record
                // of the requested size. We must perform a keypoint operation instead. 
                // We can't perform this directly as we are synchronized under this object
                // so make a note of this fact here instead. This causes a keypoint to
                // occur outside this sync block below.
                keyPointRequired = true;
            }
            else
            {
                try
                {
                    writeableLogRecord = _activeFile.getWriteableLogRecord(recordLength, _recordSequenceNumber++);
                } catch (InternalLogException exc)
                {
                    FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.getWriteableLogRecord", "1112", this);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getWriteableLogRecord", exc);
                    throw exc;
                }
            }
        }

        if (keyPointRequired)
        {
            // There is insufficient space in the active file to accomodate a record
            // of the requested size. We must perform a keypoint operation instead.
            try
            {
                _recoveryLog.keypoint();
            } catch (LogClosedException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.getWriteableLogRecord", "1129", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getWriteableLogRecord", "InternalLogException");
                throw new InternalLogException(exc);
            } catch (InternalLogException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.getWriteableLogRecord", "1135", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getWriteableLogRecord", exc);
                throw exc;
            } catch (LogIncompatibleException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogHandle.getWriteableLogRecord", "1204", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getWriteableLogRecord", exc);
                throw new InternalLogException(exc);
            }

            // Return null to the caller. This indicates that the recovery log now contains
            // a persistent record of all active data and the write operation is no longer
            // required.
            writeableLogRecord = null;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getWriteableLogRecord", writeableLogRecord);
        return writeableLogRecord;
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.getFreespace
    //------------------------------------------------------------------------------
    /*
     * Returns the free space remaining in the underlying recovery log.
     * 
     * @return The free space remaining in the underlying recovery log.
     */
    protected int getFreeSpace()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getFreeSpace", this);

        final int freeBytes = _activeFile.freeBytes();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getFreeSpace", freeBytes);

        return freeBytes;
    }

    //------------------------------------------------------------------------------
    // Method: LogHandle.writeLogRecord
    //------------------------------------------------------------------------------
    /**
     * Do any necessary under the covers work to write the LogRecord.
     */
    protected void writeLogRecord(LogRecord logRecord)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeLogRecord", logRecord);

        _activeFile.writeLogRecord(logRecord);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeLogRecord");
    }

    private void createWarningFile()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createWarningFile", this);

        try
        {
            // The filename used here should be internationalized under 532697.1
            final File file = new File(_logDirectory, "DO NOT DELETE LOG FILES");
            if (!file.exists())
            {
                file.createNewFile();
            }
        } catch (Throwable e)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "createWarningFile", e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createWarningFile");
    }
}
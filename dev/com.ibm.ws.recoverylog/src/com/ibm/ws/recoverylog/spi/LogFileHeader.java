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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: LogFileHeader
//------------------------------------------------------------------------------
/**
 * <p>
 * The LogFileHeader class groups together the information that makes up the physical
 * header in a single recovery log file. The header consists of the following
 * information:-
 * 
 * <ul>
 * <li>A "MAGIC" number (the ASCII codes for "WASLOG");</li>
 * <li>RLS version number</li>
 * <li>The file STATE (INACTIVE, ACTIVE or KEYPOINTING)</li>
 * <li>The time and date of file creation</li>
 * <li>Sequence number of first record in the file or 0 for no records</li>
 * <li>The name of the server in which the file was created</li>
 * <li>The name of the service which owns the file.</li>
 * <li>Service version number</li>
 * <li>The name of the log which owns the file.</li>
 * <li>The length of the "service data" (arbitrary data supplied by the controlling service)</li>
 * <li>The "service data"</li>
 * <li>The time and date of file creation (repeated to allow integrity check)</li>
 * <li>Sequence number of first record in the file or 0 for no records (repeated to allow integrity check)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Information on the valid states and state sequences:-
 * </p>
 * 
 * <p>
 * An example sequence of states is shown below. This example shows a cold start
 * where the recovery process does not write enough information to trigger a
 * keypoint between stages 1 and 2 (note that a keypoint at this stage is
 * supported but would result in a different state sequence)
 * <p>
 * 
 * File1 File2
 * 
 * 1 ACTIVE INACTIVE (when a cold start occurs)
 * 2 ACTIVE KEYPOINTING (when recoveryCompelte executing)
 * 3 INACTIVE ACTIVE (when recoveryComplete finished)
 * 4 KEYPOINTING ACTIVE (when keypoint is in progress) <-|
 * 5 ACTIVE INACTIVE (when keypoint is complete) |
 * 6 ACTIVE KEYPOINTING (when keypoint is in progress) |
 * 7 INACTIVE ACTIVE (when keypoint is complete) -------'
 * 
 * <p>
 * State sequence and server crashes:-
 * </p>
 * 
 * <p>
 * We must concern ourselves with the order in which state transitions are written to disk
 * during the keypoint process. It is important to ensure that there are no 'windows' of
 * opportunity, during which a server failure would result in the log being recovered
 * and deemed to be corrupt.
 * </p>
 * 
 * <p>
 * The table below shows how this must occur. Times of active and keypointing state files
 * are shown as Tn such that Tn is a later time than Tm where n>m. Timestamps are not
 * applicable to inactive files which will never be selected for recovery.
 * </p>
 * 
 * recovery file 1 recovery file 2
 * 1 ACTIVE(T1) INACTIVE (initial state, active file1 created at time T1)
 * 2 ACTIVE(T1) KEYPOINTING(T2) (create new file2 with keypointing state and time T2. Perform keypoint to file2)
 * 3 ACTIVE(T1) ACTIVE(T2) (T2>T1 ensures that failure at this point will result in file2 being used during recovery)
 * 4 INACTIVE ACTIVE(T2) (ensure that file1 can't be used)
 * 
 * <p>
 * So at each of these points the following file would be used for recovery:-
 * 
 * <ul>
 * <li>1 -> recovery file 1</li>
 * <li>2 -> recovery file 1</li>
 * <li>3 -> recovery file 2</li>
 * <li>4 -> recovery file 2</li>
 * </ul>
 * </p>
 * 
 * <p>
 * NOTE: If a failure occurs after point 3 but before point 4, AND file 2 is subsequently corrupted prior to server
 * restart. The code will select file1 for recovery. This is not ideal as it contains a number of 'old'
 * records, but it is safe as the file does contain all the current active information as well.
 * </p>
 * 
 * <p>
 * NOTE: At point 3, instead of chaning the state of file 2 we could have chosen to change the state of file 1 to
 * inactive. This would mean that file 1 was INACTIVE and file2 was KEYPOINTING. WE MUST NOT DO THIS as a server
 * failure between 3 and 4 would mean that the log could not be used for recovery (can't ensure that either file
 * is valid at restart)
 * </p>
 * 
 * <p>
 * NOTE: Clearly, this all works the other way around when keypoinying from file 2 to file 1.
 * </p>
 * 
 * <p>
 * NOTE: THE LogFileHandle.STATUS_FIELD_FILE_OFFSET field is used by the LogFileHandle class to gain direct
 * access to the STATUS field. It is important that it is kept in step with any changes to the log file
 * header.
 * </p>
 */
public class LogFileHeader
{
    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(LogFileHeader.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         null);

    /**
     * The sequence of ASCII codes that make up the string "WASLOG" is written at the top
     * of each recovery log file header and used to confirm that the file really is a
     * valid WebSphere recovery service log file.
     */
    static final byte[] MAGIC_NUMBER = { 87, 65, 83, 76, 79, 71 }; // "WASLOG"

    /**
     * The sequence of ASCII codes that make up the string "VARFIELD". This is used to demarcate
     * the start of the variable field section which includes data used by the RLS as well as
     * service data, as opposed to original header version which only had service data.
     */
    static final byte[] VARIABLE_FIELD_HEADER = { 86, 65, 82, 70, 73, 69, 76, 68 }; // "VARFIELD"

    /**
     * The INACTIVE state is used to mark recovery log files that are not to be used
     * for recovery. Of the two recovery log files for a given log, there can only
     * be one INACTIVE file at a time. The other file must be ACTIVE. See above
     * for more information on valid states and state transitions.
     */
    public static final int STATUS_INACTIVE = 2;

    /**
     * The ACTIVE state is used to mark recovery log files that MAY be used for recovery.
     * Wether an ACTIVE file is actually used for recovery depends on the state of the
     * other log file.
     */
    public static final int STATUS_ACTIVE = 4;

    /**
     * The KEYPOINTING state is used to mark recovery log files that are in the process of
     * having keypoint information written to them. Files in KEYPOINTING state may not be
     * used for recovery.
     */
    public static final int STATUS_KEYPOINTING = 8;

    /**
     * The INVALID state is used internally only to indicate that a log file has an
     * invalid header. This state is never written to the disk
     */
    public static final int STATUS_INVALID = 16;

    /**
     * Field indicating the current state of the associated recovery log file.
     */
    private int _status = STATUS_INACTIVE;

    /**
     * The version number of the RLS service that created the recovery log.
     */
    private int _creatorRLSVersionNumber = 0;

    /**
     * The time of file creation in milliseconds since January 1, 1970, 00:00:00 GMT
     * as provided by the Date class.
     */
    private long _date = 0;

    /**
     * The sequence number of the first record in the log file.
     */
    private long _firstRecordSequenceNumber = 0;

    /**
     * The name of the server which created the log file.
     */
    private String _serverName = null;

    /**
     * The bytes the make up the server name.
     */
    private byte[] _serverNameBytes = null;

    /**
     * The name of the service which created the log file.
     */
    private String _serviceName = null;

    /**
     * The version number of the service which created the log file
     */
    private int _serviceVersion;

    /**
     * The bytes that make up the service name.
     */
    private byte[] _serviceNameBytes = null;

    /**
     * The name of the log
     */
    private String _logName = null;

    /**
     * The name of the log in byte form
     */
    private byte[] _logNameBytes = null;

    /**
     * The RLS variable field data buffer
     */
    private byte[] _variableFieldData = null;

    /**
     * The service data buffer
     */
    private byte[] _serviceData = null;

    /**
     * Boolean flag indicating if the header is compatible. Incompatibility will occur if an attempt
     * is made to open a recovery log file created by a version of the RLS not supported by this version
     * of the RLS (5.1 log on 6.0)
     */
    private boolean _compatible = false;
    private final boolean _valid = false;

    private static boolean _useVariableFieldHeader;

    static
    {
        Boolean b = Boolean.TRUE;
        try
        {
            b = (Boolean) Configuration.getAccessController()
                            .doPrivileged(new java.security.PrivilegedExceptionAction()
                            {

                                @Override
                                public Boolean run() throws Exception {

                                    Boolean bb = Boolean.valueOf(System.getProperty("com.ibm.ws.recoverylog.UseVariableHeader", "true"));
                                    return bb;
                                }

                            });
        } catch (Exception e)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "<clinit>", new Object[] { "Exception reading custom property", e });
        }
        _useVariableFieldHeader = b.booleanValue();
    }

    // shutdown is not clean until this is set to true
    private boolean _shutDownWasClean = false;

    // used for unit testing.  Callbacks can check whether the log file was shutdown cleanly
    private static LogFileHeaderReadCallback _logFileHeaderReadCallback = null;

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.LogFileHeader
    //------------------------------------------------------------------------------
    /**
     * Constructor for a new LogFileHeader object.
     * 
     * @param serverName The name of the server on which the recovery log is being created.
     * @param serviceName The name of the service that is creating the recovery log file.
     * @param serviceVersion The version number of the service that is creating the recovery log file.
     * @param logName The name of the recovery log that is creating the recovery log file.
     */
    public LogFileHeader(String serverName,
                         String serviceName,
                         int serviceVersion,
                         String logName)
    {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "LogFileHeader", new Object[] { serverName, serviceName, new Integer(serviceVersion), logName });

        // When first created, the header is assumed to represent an inactive or non-existant file.
        // This field will be updated as required either by loading the real header from an existing
        // file or by setting the field to STATUS_ACTIVE directly if the log is cold starting and
        // this file will be the first one to have data written to it. See the table at the top of this
        // for more information on these state changes.
        _status = STATUS_INACTIVE;

        // Unless we override the recovery log header by invoking read(). The current version number
        // is assumed.
        _creatorRLSVersionNumber = Configuration.RLS_VERSION;

        // Determine the time of creation of this header. This value will only be used if the log is
        // cold starting and there is no existing header information stored on disk to recover.
        GregorianCalendar currentCal = new GregorianCalendar();
        Date currentDate = currentCal.getTime();
        _date = currentDate.getTime();

        // Until we determine otherwise (by opening the file and retrieving the header contents 
        // assume the first record sequence number is actually 0. If it turns out that the file
        // does not exist yet (ie first create) then this will become the first sequence number
        // to be used in the file. If the file does exist, this value will be replaced with the
        // updated value.
        _firstRecordSequenceNumber = 0;

        // Pre-configure information about this log file.
        _serverName = new String(serverName);
        _serverNameBytes = _serverName.getBytes();
        _serviceName = new String(serviceName);
        _serviceNameBytes = _serviceName.getBytes();
        _serviceVersion = serviceVersion;
        _logName = new String(logName);
        _logNameBytes = _logName.getBytes();
        _variableFieldData = initVariableFieldData();
        _serviceData = null;

        // The header information may be replaced by existing data stored on disk, but at this point
        // its not incompatible.
        _compatible = true;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "LogFileHeader", this);
    }

    public final static int headerSize()
    {
        /**
         * The length of the static portion of the file header. Variable parts such as server name length
         * need to be added to this value. If this structure is modified before the status field, then
         * the LogFileHandle.STATUS_FIELD_FILE_OFFSET field will also need to be updated.
         * 
         */
        int headerSize = MAGIC_NUMBER.length + // "WASLOG"
                         RLSUtils.INT_SIZE + // RLS version number
                         RLSUtils.INT_SIZE + // file status
                         RLSUtils.LONG_SIZE + // datestamp
                         RLSUtils.LONG_SIZE + // sequence number
                         RLSUtils.INT_SIZE + // server name length
                         RLSUtils.INT_SIZE + // service name length
                         RLSUtils.INT_SIZE + // client version number
                         RLSUtils.INT_SIZE + // log name length
                         RLSUtils.INT_SIZE + // variable data length (or service data length)
                         (_useVariableFieldHeader ? VARIABLE_FIELD_HEADER.length + // "VARFIELD"
                                                    RLSUtils.INT_SIZE // RLS var field length
                         : 0) + // not using varfield just service data
                         RLSUtils.LONG_SIZE + // datestamp (repeated)
                         RLSUtils.LONG_SIZE; // sequence number (repeated)

        return headerSize;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.length
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Calculate the length in byte of this header. This method may be used by the
     * log file handle to determine the size of the header when writing all pending
     * records during a force.
     * </p>
     * 
     * @exception LogIncompatibleException The target object represents an incompatible
     *                header.
     * @exception InternalLogException The target object represents an invalid header.
     */
    public int length() throws InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "length");

        if (!_compatible)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "length", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        if (_status == STATUS_INVALID)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "length", "LogHeaderInvalid - throwing InternalLogException");
            throw new InternalLogException(null);
        }

        // Calculate the length based on fixed and variable length data.
        // Include the 4 bytes that are used to store the header's length
        final int length = RLSUtils.INT_SIZE +
                           headerSize() +
                           _serverNameBytes.length +
                           _serviceNameBytes.length +
                           _logNameBytes.length +
                           (_variableFieldData != null ? _variableFieldData.length : 0) + // "VARFIELD" included in HEADER_SIZE even if _variableFieldData is null 
                           (_serviceData != null ? _serviceData.length : 0);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "length", new Integer(length));
        return length;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.write
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Write the full header information contained in the target header to the data
     * output stream. This method is used by the LogFileHandle class to format the
     * header so it can be written to the corresponding log file.
     * </p>
     * 
     * <p>
     * The LogFileHeader.read method can be used to recover this information
     * when the file is reloaded.
     * </p>
     * 
     * @param dataOutput The buffer into which the header information should be
     *            formatted.
     * 
     * @exception LogIncompatibleException The target object represents an incompatible header.
     * @exception InternalLogException An unexpected error has occured.
     */
    public void write(ByteBuffer fileBuffer, boolean maintainPosition) throws InternalLogException, LogIncompatibleException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "write", new Object[] { fileBuffer, new Boolean(maintainPosition), this });

        if (!_compatible)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "write", "LogIncompatibleException");
            throw new LogIncompatibleException();
        }

        if (_status == STATUS_INVALID)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "write", "LogHeaderInvalid - throwing InternalLogException");
            throw new InternalLogException(null);
        }

        try
        {
            int startPosition = 0;

            // If required, record the current position so that it can
            // be restored after updating the header.
            if (maintainPosition)
            {
                startPosition = fileBuffer.position();
            }

            // Move the file's position back to the start of the
            // file so that we write the header in the correct 
            // location.
            fileBuffer.position(0);

            // Write the length of the header.
            fileBuffer.putInt(headerSize() +
                              _serverNameBytes.length +
                              _serviceNameBytes.length +
                              _logNameBytes.length);

            // Write the "MAGIC" number at the top of the header. This is to help
            // identify the file as a WebSphere recovery log file.
            fileBuffer.put(LogFileHeader.MAGIC_NUMBER);

            // Write the version number of the RLS implementation.
            fileBuffer.putInt(Configuration.RLS_VERSION);

            // Status field
            fileBuffer.putInt(_status);

            // Date of file creation/update
            fileBuffer.putLong(_date);

            // Sequence Number of first entry
            fileBuffer.putLong(_firstRecordSequenceNumber);

            // Server Name
            fileBuffer.putInt(_serverNameBytes.length);
            fileBuffer.put(_serverNameBytes);

            // Service Name
            fileBuffer.putInt(_serviceNameBytes.length);
            fileBuffer.put(_serviceNameBytes);

            // Service version
            fileBuffer.putInt(_serviceVersion);

            // Log Name
            fileBuffer.putInt(_logNameBytes.length);
            fileBuffer.put(_logNameBytes);

            // calculate length of the variable data
            int varLength = 0;
            if (_variableFieldData != null && _useVariableFieldHeader)
            {
                varLength += VARIABLE_FIELD_HEADER.length; // not included until we have variable data otherwise header may grow before a keypoint
                varLength += RLSUtils.INT_SIZE; // the length of the variable data
                varLength += _variableFieldData.length;
            }

            // Length of Service Data
            if (_serviceData != null)
            {
                varLength += _serviceData.length;
            }

            // write variable data length
            fileBuffer.putInt(varLength);

            if (_variableFieldData != null && _useVariableFieldHeader)
            {
                fileBuffer.put(VARIABLE_FIELD_HEADER);

                // write RLS variable field
                fileBuffer.putInt(_variableFieldData.length);
                fileBuffer.put(_variableFieldData);
            }

            // write service data
            if (_serviceData != null)
            {
                fileBuffer.put(_serviceData);
            }

            // Date Of Creation (re-written to provide validation of previous information)
            fileBuffer.putLong(_date);

            // Sequence Number of first entry (re-written to provide validation of previous information)
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Writing FRSN " + _firstRecordSequenceNumber + " at position " + fileBuffer.position() + " in log file " + _logName);
            fileBuffer.putLong(_firstRecordSequenceNumber);

            // Reset the file position if required
            if (maintainPosition)
            {
                fileBuffer.position(startPosition);
            }
        } catch (BufferUnderflowException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHeader.write", "449", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "write", "InternalLogException");
            throw new InternalLogException(exc);
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHeader.write", "455", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "write", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "write");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.read
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Read the header information from the data input stream. This method is called
     * by the LogFileHandle class as part of the process of reading a log file into
     * memory. The supplied data input stream can be used to access the header
     * information that has been read from the file.
     * </p>
     * 
     * <p>
     * This information was previously written by the The LogFileHeader.write method.
     * </p>
     * 
     * @return boolean true if the header information was found to be valid otherwise
     *         false.
     */
    public boolean read(ByteBuffer fileBuffer)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "read", new Object[] { fileBuffer, this });

        long checkDate = -1;
        long checkFirstRecordSequenceNumber = -1;
        int dataLength = -1;
        boolean valid = false;
        _status = STATUS_INVALID;

        // Only if the header is decoded sucessfully will the _compatible field be
        // set to true. By default, assume that the header is incompatible.
        _compatible = false;

        try
        {
            // The first n-bytes in the file (typically 4) contain an integer which
            // holds the length of header data. The header data starts on the first
            // byte after this integer.
            int headerLength = fileBuffer.getInt();

            // If the header length is positive and there are enough bytes in the file
            // to support this length header then attempt to load it from the file.
            if ((headerLength > 0) && (headerLength <= (fileBuffer.capacity() - fileBuffer.position())))
            {
                // Read the "MAGIC" number at the top of the header. This is to help
                // identify the file as a WebSphere recovery log file.
                byte[] magicNumberBuffer = new byte[LogFileHeader.MAGIC_NUMBER.length];
                fileBuffer.get(magicNumberBuffer);

                if (validMagicNumber(magicNumberBuffer))
                {
                    // RLS Version number
                    _creatorRLSVersionNumber = fileBuffer.getInt();

                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Recovery log creator version is " + _creatorRLSVersionNumber);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Current recovery log version is " + Configuration.RLS_VERSION);

                    // Now we need to examine this field to see if this version of the file can be parsed
                    // by this level of the code.
                    int i = 0;

                    while (!_compatible && (i < Configuration.COMPATIBLE_RLS_VERSIONS.length))
                    {
                        if (_creatorRLSVersionNumber == Configuration.COMPATIBLE_RLS_VERSIONS[i])
                        {
                            _compatible = true;
                        }
                        i++;
                    }

                    if (_compatible)
                    {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "This version of recovery log file can be parsed by this level of the RLS");

                        // Status field
                        _status = fileBuffer.getInt();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file state is " + LogFileHeader.statusToString(_status));

                        // Date of file creation/update
                        _date = fileBuffer.getLong();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file date (long) is " + _date);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file date (date) is " + new Date(_date).toString());

                        // Sequence Number of first entry
                        _firstRecordSequenceNumber = fileBuffer.getLong();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file first record sequence number is " + _firstRecordSequenceNumber);

                        // Server Name
                        int stringLength = fileBuffer.getInt();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Reading String of length " + stringLength);
                        byte[] serverNameBytes = new byte[stringLength];
                        fileBuffer.get(serverNameBytes);
                        _serverName = new String(serverNameBytes);

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file created on server " + _serverName);

                        // Service Name
                        stringLength = fileBuffer.getInt();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Reading String of length " + stringLength);
                        byte[] serviceNameBytes = new byte[stringLength];
                        fileBuffer.get(serviceNameBytes);
                        _serviceName = new String(serviceNameBytes);

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file created by service " + _serviceName);

                        // Service Version
                        _serviceVersion = fileBuffer.getInt();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file created by service version" + _serviceVersion);

                        // Log Name
                        stringLength = fileBuffer.getInt();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Reading String of length " + stringLength);
                        byte[] logNameBytes = new byte[stringLength];
                        fileBuffer.get(logNameBytes);
                        _logName = new String(logNameBytes);

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log name was " + _logName);

                        // Length of Service Data and Service Data
                        dataLength = fileBuffer.getInt();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file contains " + dataLength + " bytes of service specific information");

                        if (dataLength > 0)
                        {
                            // check if we have RLS variable header
                            byte[] varHeaderTest = new byte[VARIABLE_FIELD_HEADER.length];
                            int currPosition = fileBuffer.position();
                            fileBuffer.get(varHeaderTest);

                            if (_useVariableFieldHeader && Arrays.equals(varHeaderTest, VARIABLE_FIELD_HEADER))
                            {
                                // length of variable RLS header
                                int rlsVarFieldLength = fileBuffer.getInt();

                                if (rlsVarFieldLength > 0)
                                {
                                    _variableFieldData = new byte[rlsVarFieldLength];
                                    fileBuffer.get(_variableFieldData);
                                    parseVariableFieldData();
                                }

                                if (dataLength - rlsVarFieldLength - VARIABLE_FIELD_HEADER.length - RLSUtils.INT_SIZE > 0)
                                {
                                    // we also have service data
                                    _serviceData = new byte[dataLength - rlsVarFieldLength];
                                    fileBuffer.get(_serviceData);
                                }
                            }
                            else
                            {
                                // we just have service data
                                _variableFieldData = null; // not inited because we don't want to write it in this cycle unless it was there already
                                fileBuffer.position(currPosition);
                                _serviceData = new byte[dataLength];
                                fileBuffer.get(_serviceData);
                            }
                        }
                        else
                        {
                            _variableFieldData = null; // not inited because we don't want to write it in this cycle unless it was there already
                            _serviceData = null;
                        }

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "RLS var data Information is [" + RLSUtils.toHexString(_variableFieldData, RLSUtils.MAX_DISPLAY_BYTES) + "]");

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Service Specific Information is [" + RLSUtils.toHexString(_serviceData, RLSUtils.MAX_DISPLAY_BYTES) + "]");

                        // Date Of Creation (re-written to provide validation of previous information)
                        checkDate = fileBuffer.getLong();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file check date (long) is " + checkDate);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file check date (date) is " + new Date(checkDate).toString());

                        // Sequence Number of first entry (re-written to provide validation of previous information)

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Reading FRSN from position " + fileBuffer.position());

                        checkFirstRecordSequenceNumber = fileBuffer.getLong();

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Recovery log file check first record sequence number is " + checkFirstRecordSequenceNumber);

                        // Now determine if this header was valid or not.
                        if ((_date == checkDate) &&
                            (_firstRecordSequenceNumber == checkFirstRecordSequenceNumber) &&
                            (_date > 0))
                        {
                            valid = true;
                        }
                        else
                        {
                            changeStatus(STATUS_INVALID);
                            if (tc.isEventEnabled())
                                Tr.event(tc, "Error processing recovery log file header - integrity check failed");
                        }
                    }
                    else
                    {
                        reset();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "This version of recovery log file can not be parsed by this level of the RLS");
                    }
                }
            }
        } catch (BufferUnderflowException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHeader.read", "603", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "Error processing recovery log file header", exc.toString());
            changeStatus(STATUS_INVALID);
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.LogFileHeader.read", "609", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "Error processing recovery log file header", exc.toString());
            changeStatus(STATUS_INVALID);
        }

        boolean readOK = (_compatible && valid);

        if (_logFileHeaderReadCallback != null)
        {
            // exceptions from here don't change the outcome as they are handled in LogFileHandle.readFileHeader()
            _logFileHeaderReadCallback.readComplete(this, readOK);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "read", new Boolean(readOK));

        return readOK;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.validMagicNumber
    //------------------------------------------------------------------------------
    /**
     * Determines if the supplied magic number is a valid log file header magic number
     * as stored in MAGIC_NUMBER
     * 
     * @param magicNumberBuffer The buffer containing the magic number tio compare
     * 
     * @return boolean true if the headers match, otherwise false
     */
    private boolean validMagicNumber(byte[] magicNumberBuffer)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "validMagicNumber", new java.lang.Object[] { RLSUtils.toHexString(magicNumberBuffer, RLSUtils.MAX_DISPLAY_BYTES), this });

        boolean incorrectByteDetected = false;
        int currentByte = 0;

        while ((!incorrectByteDetected) && (currentByte < LogFileHeader.MAGIC_NUMBER.length))
        {
            if (magicNumberBuffer[currentByte] != LogFileHeader.MAGIC_NUMBER[currentByte])
            {
                incorrectByteDetected = true;
            }
            currentByte++;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "validMagicNumber", new Boolean(!incorrectByteDetected));
        return !incorrectByteDetected;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.date
    //------------------------------------------------------------------------------
    /**
     * Return the date field stored in the target header
     * 
     * @return long The date field.
     */
    public long date()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "date", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "date", new Long(_date));
        return _date;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.firstRecordSequenceNumber
    //------------------------------------------------------------------------------
    /**
     * Return the firstRecordSequenceNumber field stored in the target header
     * 
     * @return long The firstRecordSequenceNumber field.
     */
    public long firstRecordSequenceNumber()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "firstRecordSequenceNumber", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "firstRecordSequenceNumber", new Long(_firstRecordSequenceNumber));
        return _firstRecordSequenceNumber;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.serverName
    //------------------------------------------------------------------------------
    /**
     * Return the serverName field stored in the target header
     * 
     * @return String The serverName field.
     */
    public String serverName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverName", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverName", _serverName);
        return _serverName;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.serviceName
    //------------------------------------------------------------------------------
    /**
     * Return the serviceName field stored in the target header
     * 
     * @return String The serviceName field.
     */
    public String serviceName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serviceName", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "serviceName", _serviceName);
        return _serviceName;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.serviceVersion
    //------------------------------------------------------------------------------
    /**
     * Return the serviceVersion field stored in the target header
     * 
     * @return int The serviceVersion field.
     */
    public int serviceVersion()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serviceVersion", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "serviceVersion", new Integer(_serviceVersion));
        return _serviceVersion;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.logName
    //------------------------------------------------------------------------------
    /**
     * Return the logName field stored in the target header
     * 
     * @return long The logName field.
     */
    public String logName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logName", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "logName", _logName);
        return _logName;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.getServiceData
    //------------------------------------------------------------------------------
    /**
     * Return the service data stored in the target header.
     * 
     * @return The service data refernece.
     */
    public byte[] getServiceData()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getServiceData", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getServiceData", RLSUtils.toHexString(_serviceData, RLSUtils.MAX_DISPLAY_BYTES));
        return _serviceData;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.setServiceData
    //------------------------------------------------------------------------------
    /**
     * Change the service data associated with the target log file header.
     * 
     * @param serviceData The new service data reference.
     */
    public void setServiceData(byte[] serviceData)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setServiceData", new java.lang.Object[] { RLSUtils.toHexString(serviceData, RLSUtils.MAX_DISPLAY_BYTES), this });

        _serviceData = serviceData;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setServiceData");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.compatible
    //------------------------------------------------------------------------------
    /**
     * Test to determine if the target log file header belongs to a compatible RLS
     * file.
     * 
     * @return boolean true if the log file header is compatible, otherwise false.
     */
    public boolean compatible()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "compatible", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "compatible", new Boolean(_compatible));
        return _compatible;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.valid
    //------------------------------------------------------------------------------
    /**
     * Test to determine if the target log file header belongs to a valid RLS
     * file.
     * 
     * @return boolean true if the log file header is valid, otherwise false.
     */
    public boolean valid()
    {
        boolean valid = true;
        if (tc.isEntryEnabled())
            Tr.entry(tc, "valid", this);
        if (_status == STATUS_INVALID)
            valid = false;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "valid", new Boolean(valid));
        return valid;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.status
    //------------------------------------------------------------------------------
    /**
     * Return the status field stored in the target header
     * 
     * @return int The status field.
     */
    public int status()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "status", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "status", new Integer(_status));
        return _status;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.reset
    //------------------------------------------------------------------------------
    /**
     * Destroy the internal state of this header object. Note that we don't reset
     * the _compatible flag as we call this method from points in the code wher
     * its both true and false and we want to ensure that it is represented in
     * the exceptions that get thrown when other calls are made.
     */
    public void reset()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reset", this);

        _status = STATUS_ACTIVE;
        _date = 0;
        _firstRecordSequenceNumber = 0;
        _serverName = null;
        _serviceName = null;
        _serviceVersion = 0;
        _logName = null;
        _variableFieldData = initVariableFieldData();
        _serviceData = null;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reset");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.resetHeader
    //------------------------------------------------------------------------------
    /**
     * Ensure that the internal state of a header if valid, by copying a known
     * good header to an invalid header. This method is called during the initialization
     * and validation of a pair of recovery log files.
     */
    public void resetHeader(LogFileHeader validHeader)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resetHeader", this);
        _date = 0;
        _firstRecordSequenceNumber = 0;
        _serverName = validHeader.serverName();
        _serviceName = validHeader.serviceName();
        _logName = validHeader.logName();
        _variableFieldData = initVariableFieldData();
        _serviceData = null;
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resetHeader");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.changeStatus
    //------------------------------------------------------------------------------
    /**
     * Update the status field stored in the target header.
     * 
     * @param newStatus The new status field value.
     */
    public void changeStatus(int newStatus)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "changeStatus", new java.lang.Object[] { this, new Integer(newStatus) });

        _status = newStatus;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "changeStatus");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.keypointStarting
    //------------------------------------------------------------------------------
    /**
     * Informs the LogFileHeader instance that a keypoint operation is about
     * begin into file associated with this LogFileHeader instance. The status of the
     * header is updated to KEYPOINTING.
     * 
     * @param nextRecordSequenceNumber The sequence number to be used for the first
     *            record in the file.
     */
    public void keypointStarting(long nextRecordSequenceNumber)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypointStarting", new Object[] { this, new Long(nextRecordSequenceNumber) });

        GregorianCalendar currentCal = new GregorianCalendar();
        Date currentDate = currentCal.getTime();
        _date = currentDate.getTime();

        _firstRecordSequenceNumber = nextRecordSequenceNumber;
        _status = STATUS_KEYPOINTING;

        _variableFieldData = initVariableFieldData(); // if var field data was not previously set this is the first time it will become set

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypointStarting");
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.keypointComplete
    //------------------------------------------------------------------------------
    /**
     * Informs the LogFileHeader instance that the keypoint operation has completed.
     * The status of the header is updated to ACTIVE.
     */
    public void keypointComplete()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "keypointComplete", this);

        _status = STATUS_ACTIVE;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "keypointComplete");
    }

    public void setCleanShutdown()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setCleanShutdown", this);
        if (_variableFieldData != null && _variableFieldData.length > 0)
        {
            _variableFieldData[0] = 1;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setCleanShutdown");
    }

    public boolean wasShutdownClean()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "wasShutdownClean", this);
        boolean clean = true;

        // true if not using variable field header
        if (_useVariableFieldHeader)
        {
            clean = _shutDownWasClean;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "wasShutdownClean", clean);

        return clean;

    }

    /**
     * creates a byte array representing the default variable field data header
     * 
     * @return
     */
    private byte[] initVariableFieldData()
    {
        // current implementation is for a single byte with a value of 0
        if (_useVariableFieldHeader)
            return new byte[1];
        else
            return null;
    }

    /**
     * Extract the goodies from the variable header before finally resetting anything
     * that needs to be reset before the header is written again. Note this happens before
     * the next keypoint so the variable header must not grow in size. The write happens before
     * any new log records are written.
     */
    private void parseVariableFieldData()
    {
        if (_variableFieldData != null && _variableFieldData.length > 0)
        {
            // clean shutdown is the first (and only so far) byte
            if (_variableFieldData[0] == 1)
            {
                _shutDownWasClean = true;
                _variableFieldData[0] = 0; // reset for next write
            }
        }
    }

    /**
     * Used by unit tests to enable and disable variable field header.
     * 
     * @param b
     */
    public static void setUseVariableFieldHeader(boolean b)
    {
        _useVariableFieldHeader = b;
    }

    //------------------------------------------------------------------------------
    // Method: LogFileHeader.statusToString
    //------------------------------------------------------------------------------
    /**
     * Utility method to convert a numerical status into a printable string form.
     * 
     * @param status The status to convert
     * 
     * @return String A printable form of the status ("ACTIVE","INACTIVE","KEYPOINTING", "INVALID" or "UNKNOWN")
     */
    static String statusToString(int status)
    {
        String result = null;

        if (status == STATUS_INACTIVE)
        {
            result = "INACTIVE";
        }
        else if (status == STATUS_ACTIVE)
        {
            result = "ACTIVE";
        }
        else if (status == STATUS_KEYPOINTING)
        {
            result = "KEYPOINTING";
        }
        else if (status == STATUS_INVALID)
        {
            result = "INVALID";
        }
        else
        {
            result = "UNKNOWN";
        }

        return result;
    }

    public static void registerReadCallback(LogFileHeaderReadCallback callback)
    {
        _logFileHeaderReadCallback = callback;
    }
}

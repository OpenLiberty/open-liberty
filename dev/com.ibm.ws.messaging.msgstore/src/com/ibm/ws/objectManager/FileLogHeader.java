package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * <p>
 * FileLogHeader is the data at the start of the log. It indicates where
 * LogInput should start reading the first LogRecord. We write two
 * HeaderLogRecords into the log to make sure that at least one is legible, the
 * headerSequence number increases each time a new Header is written, so that we
 * can determine which of the two is the latest. The sequence number is checked
 * with another copy at the end of the header to make sure the whole header has
 * been written.
 * 
 * @author IBM Corporation
 */
public class FileLogHeader
{
    private static final Class cclass = FileLogHeader.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LOG);

    // The length of a log header in bytes when written to disk.
    // Because we write a 4K block, the header cannot be corrupted by a write of
    // adjecant bytes.
    protected static final int headerLength = FileLogOutput.pageSize;
    // Define the version of the log format this code writes.
    protected static final int writeVersion = 0;

    // Header fields.
    // Version number that created this instance.
    protected int instanceVersion = writeVersion;
    // The sector bit, at the end of the sector byte is written at end of each
    // sector throuought the log and alternates between 0 and 1 the change in
    // value indicates the end of the log.
    // The log contains all zeros to begin with, so write 1 as the initial value.
    protected byte sectorByte = 1;
    // The byte address in the log file of the first non truncated log record.
    // The place to start reading log records.
    protected long startByteAddress;
    // The size of the log file in bytes. If we crashed while trying to increase or
    // decrease the size of the log file the actual file might be bigger than this. 
    // FileLogOutput corrects this at startup.
    protected long fileSize;
    // Incremented each time the header is written, used to determine which header is the latest.
    protected long headerSequence = 0;
    // Documents the time when the header was written, used for diagnostics only.
    protected long headerWriteTime;
    // Used to check that the file is likely to be a LogFile (24 characters).
    static final String signature = "++ObjectManager.LogFile++";

    /**
     * Constructor
     */
    protected FileLogHeader()
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "<init>");
            trace.exit(this,
                       cclass,
                       "<init>");
        }
    } // FileLogHeader().

    /**
     * Constructor
     */
    protected FileLogHeader(java.io.RandomAccessFile logFile)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        "LogFile=" + logFile);

        readHeader(logFile);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // FileLogHeader().

    /**
     * Initialise the FileLogHeader from a RandomAccessFile.
     * 
     * @param java.io.RandomAceessFile
     *            the stream that connects to the log.
     * @author IBM Corporation
     * 
     * @throws LogFileExhaustedException
     *             if there are no more logrecords left to read.
     * @throws PermanentIOException
     *             if the is an underlting java.io.IOException.
     */
    private void readHeader(java.io.RandomAccessFile logFile)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "readHeader",
                        new Object[] { logFile });

        boolean validHeader = false; // True if the header is valid
        try {
            logFile.seek(0); // Start reading at the head of the file.
            byte[] headerBytes = new byte[headerLength];

            for (int i = 1; i <= 2 && !validHeader; i++) {
                logFile.readFully(headerBytes);
                validHeader = FileLogInput.restoreSectorBits(headerBytes, headerBytes[5]);

                if (validHeader) {
                    java.io.ByteArrayInputStream byteArrayInputStream = new java.io.ByteArrayInputStream(headerBytes);
                    java.io.DataInputStream inputStream = new java.io.DataInputStream(byteArrayInputStream);

                    inputStream.skipBytes(1);
                    instanceVersion = inputStream.readInt();
                    sectorByte = inputStream.readByte();

                    // Read a known number of signature charaters. 
                    char[] signatureRead = new char[signature.length()];
                    for (int ichar = 0; ichar < signature.length(); ichar++)
                        signatureRead[ichar] = inputStream.readChar();

                    startByteAddress = inputStream.readLong();
                    fileSize = inputStream.readLong();
                    headerSequence = inputStream.readLong();
                    headerWriteTime = inputStream.readLong();

                    // Check the signature.
                    if (!(new String(signatureRead).equals(signature))) {
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "readheader"
                                       , new Object[] { signatureRead });
                        throw new InvalidLogFileSignatureException(this,
                                                                   new String(signatureRead), signature);
                    } // if(!signatureRead).equals(signature))).

                    if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                        trace.debug(this,
                                    cclass,
                                    "readheader",
                                    new Object[] { new Integer(instanceVersion), new Byte(sectorByte)
                                                  , new Long(startByteAddress), new Long(fileSize)
                                                  , new Long(headerSequence), new java.util.Date(headerWriteTime) }
                                        );
                } // if (validHeader).
            } // for 2 log headers.

        } catch (java.io.EOFException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "readHeader", exception, "1:188:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "readHeader",
                           exception);
            throw new LogFileExhaustedException(this, exception);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "readHeader", exception, "1:199:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "readHeader",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch java.io.IOException.

        if (!validHeader) {
            LogFileHeaderCorruptException logFileHeaderCorruptException = new LogFileHeaderCorruptException(this);
            ObjectManager.ffdc.processException(this, cclass, "readHeader", logFileHeaderCorruptException, "1:212:1.8");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "readHeader");
            throw logFileHeaderCorruptException;
        } // if (validHeader).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "readHeader");
    } // readHeader().

    /**
     * <p>
     * Write the FileLogHeader to a FileChannel.
     * 
     * @param java.io.RandomAccessFile the log file.
     * @return long the position of the log file after the write.
     * @author IBM Corporation
     * 
     * @throws PermanentIOException
     *             if the is an underlting java.io.IOException.
     */
    protected long writeHeader(java.io.RandomAccessFile logFile)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "writeHeader",
                        new Object[] { logFile, new Integer(instanceVersion), new Byte(sectorByte),
                                      new Long(startByteAddress), new Long(fileSize) }
                            );

        ObjectManagerByteArrayOutputStream logHeaderStream = new ObjectManagerByteArrayOutputStream(headerLength);
        java.io.DataOutputStream logHeader = new java.io.DataOutputStream(logHeaderStream);

        // Create the data in the log headerBuffer.
        try {
            logHeader.writeByte(0); // Skip over sector byte, for saving sector bits.
            logHeader.writeInt(instanceVersion);
            logHeader.writeByte(sectorByte); // Byte [5].
            logHeader.writeChars(signature);
            logHeader.writeLong(startByteAddress);
            logHeader.writeLong(fileSize);
            logHeader.writeLong(++headerSequence);
            headerWriteTime = System.currentTimeMillis();
            logHeader.writeLong(headerWriteTime);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeHeader", exception, "1:265:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "writeHeader",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch java.io.IOException.

        long filePosition = 0;
        byte[] headerBuffer = logHeaderStream.getBuffer();
        // Set the sector bits in the header to be consistent with the sector bits starting 
        // at the start byte address. This is not much use for validating the header has been
        // written completely because we may rewrite it several times without changing the
        // sector bits, however it does help check that the file is a valid log file.
        // Since we only have data in the first sector, in is only really necessary
        // for the first sector to be written successfully. 
        FileLogOutput.setSectorBits(headerBuffer, 0, sectorByte);

        try {
            logFile.seek(0); // Start writing at the head of the file.
            // Write two LogHeaders.
            for (int i = 0; i < 2; i++) {
                logFile.write(headerBuffer,
                              0,
                              headerLength);
                filePosition = filePosition + headerLength;
            } // for 2 log headers.

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "writeHeader", exception, "1:298:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "writeHeader",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch java.io.IOException.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "writeHeader",
                       new Object[] { new Long(filePosition), new Long(headerSequence), new java.util.Date(headerWriteTime) });
        return filePosition;
    } // writeHeader().
} // End of class LogInput.

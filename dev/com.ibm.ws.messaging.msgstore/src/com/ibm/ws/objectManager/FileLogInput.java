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
 * <p>Implementation of LogInput that reads a file written by FileLogOutput.<\p>
 * 
 * @author IBM Corporation.
 */
public class FileLogInput
                extends LogInput {
    private static final Class cclass = FileLogInput.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LOG);

    // Mask used to pick the sector bit from byte zero for each sector in a page. 
    static final byte[] mask = new byte[] { 1, 2, 4, 8, 16, 32, 64, -128 };
    //Persistent storage.
    private java.io.RandomAccessFile logFile;

    private SectorValidatedInputStream sectorValidatedInputStream;
    // The sequential log file.
    private java.io.DataInputStream dataInputStream;
    // If the logRecord has to be split into multiple parts we hang the list of part buffers here
    // according to the multiPartID of the logRecord.
    private java.util.List[] multiPartLogRecords = new java.util.List[256];
    private boolean checkpointStartSeen = false;

    /**
     * Constructor
     * 
     * @param logFile the log file.
     * @param objectManagerState creating the LogInput.
     * @throws ObjectManagerException
     */
    protected FileLogInput(java.io.RandomAccessFile logFile,
                           ObjectManagerState objectManagerState)
        throws ObjectManagerException {
        super(objectManagerState);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "<init>"
                        , new Object[] { logFile, objectManagerState }
                            );

        this.logFile = logFile;

        // Create the inputStream.
        sectorValidatedInputStream = new SectorValidatedInputStream(logFile);
        dataInputStream = new java.io.DataInputStream(sectorValidatedInputStream);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "<init>"
                            );
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogInput#close()
     */
    public void close()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "Close");

        try {
            dataInputStream.close();
            sectorValidatedInputStream.close();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, "close", exception, "1:98:1.13");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "close"
                           , exception
                                );
            throw new PermanentIOException(this
                                           , exception);
        } // catch (java.io.IOException exception).  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "close"
                            );
    } // close().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogInput#getLogFileSize()
     */
    protected long getLogFileSize()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getLogFileSIze"
                            );

        long logFileSize = sectorValidatedInputStream.header.fileSize;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getLogFileSize"
                       , "returns logFileSize=" + logFileSize + "(long)"
                            );
        return logFileSize;
    } // End of method getLogFileSize().

    /**
     * @see com.ibm.ws.objectManager.LogInput#readNext()
     * 
     * @throws PermanentIOException if the is an underlying java.io.IOException.
     */
    public LogRecord readNext()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "readNext");

        java.io.DataInputStream logRecordDataInputStream;
        int type; // Of the Log Record.
        // Supress log Records until we see Checkpoint Start. Just build up the 
        // parts of any log Records.
        do {
            logRecordDataInputStream = readNextLogRecord();
            // We now have a java.io.DataInputStream containg a complete logRecord.       
            try {
                type = logRecordDataInputStream.readInt();
            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(cclass, "readNext", exception, "1:160:1.13");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "readNext",
                               exception);
                throw new PermanentIOException(this,
                                               exception);
            } // catch java.io.IOException.
            if (type == LogRecord.TYPE_CHECKPOINT_START)
                checkpointStartSeen = true;
        } while (!checkpointStartSeen);

        LogRecord logRecordRead = null; // The log record read from the log. 
        switch (type) {
            case LogRecord.TYPE_USER_DEFINED:
                logRecordRead = LogRecord.getUserLogRecord(logRecordDataInputStream, objectManagerState);
                break;
            case LogRecord.TYPE_ADD:
                logRecordRead = new TransactionAddLogRecord(logRecordDataInputStream
                                                            , objectManagerState
                                );
                break;
            case LogRecord.TYPE_REPLACE:
                logRecordRead = new TransactionReplaceLogRecord(logRecordDataInputStream
                                                                , objectManagerState
                                );
                break;
            case LogRecord.TYPE_OPTIMISTIC_REPLACE:
                logRecordRead = new TransactionOptimisticReplaceLogRecord(logRecordDataInputStream
                                                                          , objectManagerState
                                );
                break;
            case LogRecord.TYPE_DELETE:
                logRecordRead = new TransactionDeleteLogRecord(logRecordDataInputStream
                                                               , objectManagerState
                                );
                break;
            case LogRecord.TYPE_PREPARE:
                logRecordRead = new TransactionPrepareLogRecord(logRecordDataInputStream
                                );
                break;
            case LogRecord.TYPE_COMMIT:
                logRecordRead = new TransactionCommitLogRecord(logRecordDataInputStream);
                break;
            case LogRecord.TYPE_BACKOUT:
                logRecordRead = new TransactionBackoutLogRecord(logRecordDataInputStream);
                break;
            case LogRecord.TYPE_CHECKPOINT_START:
                logRecordRead = new CheckpointStartLogRecord(logRecordDataInputStream
                                                             , objectManagerState
                                );
                break;
            case LogRecord.TYPE_CHECKPOINT_END:
                logRecordRead = new CheckpointEndLogRecord(logRecordDataInputStream);
                break;
            case LogRecord.TYPE_CHECKPOINT_TRANSACTION:
                logRecordRead = new TransactionCheckpointLogRecord(logRecordDataInputStream
                                                                   , objectManagerState
                                );
                break;
            case LogRecord.TYPE_PADDING:
                logRecordRead = new PaddingLogRecord(logRecordDataInputStream);
                break;
            default:
                InvalidLogRecordTypeException invalidLogRecordTypeException = new InvalidLogRecordTypeException(this
                                                                                                                , type
                                );
                ObjectManager.ffdc.processException(cclass, "readNext", invalidLogRecordTypeException, "1:229:1.13");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "readNext"
                               , new Object[] { invalidLogRecordTypeException, logRecordDataInputStream, new Integer(type) }
                                    );
                throw invalidLogRecordTypeException;
        } // switch (type).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "readNext",
                       new Object[] { logRecordRead });
        return logRecordRead;
    } // readNext().  

    /**
     * Read the log until we have a complete logrecord in a dataInputStream.
     * 
     * @return java.io.DataInputStream containing the next logRecord.
     */
    private java.io.DataInputStream readNextLogRecord()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "readNextLogRecord",
                        new Object[] { new Long(sectorValidatedInputStream.currentPage),
                                      new Integer(sectorValidatedInputStream.currentByte) });

        byte[] logRecordBytes = null;
        short partLength = 0;
        byte partType = FileLogOutput.PART_First;
        byte multiPartID = 0;
        java.io.DataInputStream logRecordDataInputStream = null;

        // Keep reading until we have assembled a complete LogRecord.
        while (logRecordDataInputStream == null) {
            try {
                // Read the part header.
                partType = dataInputStream.readByte();
                switch (partType) {
                    case FileLogOutput.PART_Padding:
                        // Padding, do nothing.
                        break;

                    case FileLogOutput.PART_First:
                    case FileLogOutput.PART_Middle:
                    case FileLogOutput.PART_Last:
                        multiPartID = dataInputStream.readByte();
                        partLength = dataInputStream.readShort();

                        // Now get the logRecord itself.
                        // TODO We could avoid doing this if the logRecord is single part as it is in fact just the next bytes
                        //      in the datastream.
                        logRecordBytes = new byte[partLength];
                        // There is a bug in JVM 1.4.2 such that dataInput.readFully(byte[],int,int)
                        // returns a partial buffer rather than throwing EOFExcepton. 
                        // dataInputStream.readFully(logRecordBytes);
                        // Anyhow we would rather not block if the file has data missing at the end.
                        int bytesRead = dataInputStream.read(logRecordBytes);
                        if (bytesRead != partLength) {
                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this, cclass
                                           , "readNextLogRecord"
                                           , new Object[] { "LogFileExhausted_1", new Integer(bytesRead), new Short(partLength) }
                                                );
                            throw new LogFileExhaustedException(this, new java.io.EOFException());
                        } // if ( bytesRead != partLength).

                        if (multiPartID == 0) {
                            // partType will be PART_Last, this is the only part.
                            java.io.ByteArrayInputStream byteArrayInputStream = new java.io.ByteArrayInputStream(logRecordBytes);
                            logRecordDataInputStream = new java.io.DataInputStream(byteArrayInputStream);

                        } else if (partType == FileLogOutput.PART_Last) {
                            if (multiPartLogRecords[multiPartID] != null) {
                                multiPartLogRecords[multiPartID].add(logRecordBytes);
                                MultiByteArrayInputStream inputStream = new MultiByteArrayInputStream(multiPartLogRecords[multiPartID]);
                                logRecordDataInputStream = new java.io.DataInputStream(inputStream);
                                multiPartLogRecords[multiPartID] = null;
                            } // if (multiPartLogRecords[multiPartID] != null).

                        } else if (partType == FileLogOutput.PART_First) {
                            multiPartLogRecords[multiPartID] = new java.util.ArrayList();
                            multiPartLogRecords[multiPartID].add(logRecordBytes);
                        } else {
                            if (multiPartLogRecords[multiPartID] != null) {
                                multiPartLogRecords[multiPartID].add(logRecordBytes);
                            }
                        }

                        break;
                    default:
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this, cclass
                                       , "readNextLogRecord"
                                       , "via InvalidLogPartTypeException partType=" + partType + "(byte)"
                                            );
                        throw new InvalidLogRecordPartTypeException(this
                                                                    , partType);
                        // break;                             // unreachable.
                } // switch (partType).

            } catch (java.io.EOFException exception) {
                // No FFDC Code Needed, condition expected when end of log seen.
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "readNextLogRecord"
                               , new Object[] { "LogFileExhausted_2", new Short(partLength) }
                                    );
                throw new LogFileExhaustedException(this, exception);

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(cclass, "readNext", exception, "1:348:1.13");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "readNextLogRecord"
                               , new Object[] { new Short(partLength), exception }
                                    );
                throw new PermanentIOException(this
                                               , exception);
            } // catch java.io.IOException.

        } // while (logRecordDataInputStream == null).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "readNextLogRecord",
                       new Object[] { logRecordDataInputStream });
        return logRecordDataInputStream;
    } // readNextLogRecord(). 

    /*
     * @return the last complete page read.
     */
    protected long getCurrentPage() {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getCurrentPage"
                            );

        long currentPage = sectorValidatedInputStream.currentPage;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getCurrentPage"
                       , "returns =" + currentPage + "(long)"
                            );
        return currentPage;
    } // method getCurrentPage().

    /*
     * Validate and restore the sectorBits in the page.
     * 
     * @param byte[] the page to be validated and restored.
     * 
     * @param byte the sectorByte the page should contain.
     * 
     * @return boolean true if all of the sector bits are valid and have been reset.
     */
    protected static boolean restoreSectorBits(byte[] page, byte sectorByte)
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        "restoreSectorBits",
                        new Object[] { new Byte(page[0]), new Byte(sectorByte) }
                            );

        // Check that all of the sectors are OK.
        for (int i = 1; i < 9; i++) {
            if ((page[FileLogOutput.sectorSize * i - 1] & 1) != sectorByte) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(cclass
                               , "restoreSectorBits"
                               , "returns false"
                                 + " i=" + i + "(int)"
                                 + " page[FileLogOutput.sectorSize*i -1]=" + page[FileLogOutput.sectorSize * i - 1] + "(byte)"
                                    );
                return false;
            } // if sector bit.   

            if ((page[0] & mask[i - 1]) == 0)
                page[FileLogOutput.sectorSize * i - 1] &= 0xFE;
            else
                page[FileLogOutput.sectorSize * i - 1] |= 1;
        } //  For 8 sectors. 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass
                       , "restoreSectorBits"
                       , "returns true"
                            );
        return true;
    } // restoreSectorBits(). 

    // --------------------------------------------------------------------------
    // inner classes.
    // --------------------------------------------------------------------------  

    /**
     * Validates the sectorBits on an InputStream.
     */
    private class SectorValidatedInputStream
                    extends java.io.InputStream
    {

        private java.io.RandomAccessFile file;
        private FileLogHeader header;
        private byte[] page = new byte[FileLogOutput.pageSize];
        // The next byte we will return, intially set to indate we have finished the previous page.    
        private int currentByte = page.length;
        private long currentPage = -1;

        /**
         * Constructor.
         * 
         * @param java.io.RandomAccessFile to read the data from.
         */
        SectorValidatedInputStream(java.io.RandomAccessFile randomAccessFile)
            throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "<init>"
                            , "randomAccessFile=" + randomAccessFile + "(java.io.RandomAccessFile)"
                                );

            this.file = randomAccessFile;
            // Read the headers.  
            header = new FileLogHeader(logFile);

            // Position the log for sequential reading.
            seek(header.startByteAddress);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "<init>"
                                );
        } // Constructor.

        /*
         * (non-Javadoc)
         * 
         * @see java.io.InputStream#close()
         */
        public void close()
                        throws java.io.IOException
        {
            file.close();
        }

        /*
         * Position the file for the next read.
         * 
         * @param long the byte to be read next.
         */
        private void seek(long position)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "seek"
                            , "position=" + position + "(long)"
                                );

            currentByte = (int) (position % FileLogOutput.pageSize);
            int bytesRead;

            try {
                // Move to the correct page.
                file.seek(position - currentByte);
                bytesRead = file.read(page); // Get another page of data.

            } catch (java.io.IOException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(cclass, "seek", exception, "1:510:1.13");

                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "seek"
                               , exception
                                    );
                throw new PermanentIOException(this
                                               , exception);
            } // catch (java.io.IOException exception).

            if (bytesRead != page.length) {
                // The end of the real log file should not be seen as we preallocate it.
                PrematureEndOfLogFileException prematureEndOfLogFileException = new PrematureEndOfLogFileException(this
                                                                                                                   , objectManagerState.getLogFileName()
                                                                                                                   , header.fileSize
                                                                                                                   , position);
                ObjectManager.ffdc.processException(cclass,
                                                    "seek",
                                                    prematureEndOfLogFileException,
                                                    "1:531:1.13",
                                                    new Object[] { new Long(header.fileSize),
                                                                  new Long(position),
                                                                  new Integer(bytesRead) });
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "seek",
                               prematureEndOfLogFileException);
                throw prematureEndOfLogFileException;
            } // if (bytesRead != page.length).

            // Validate and restore the sector bits.
            if (!restoreSectorBits(page, header.sectorByte)) {
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()) {
                    trace.debug(this,
                                cclass,
                                "seek",
                                "Failed to restore sector bits");
                    trace.bytes(this,
                                cclass,
                                page);
                } // if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled()).  
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "seek",
                               "via LogFileExhaustedException");
                throw new LogFileExhaustedException(this, new Exception("SectorBit"));
            } // if (!restoreSectorBits()).

            if (currentByte == 0) // Do we need to step past the sector bits?   
                currentByte++;
            currentPage = position / FileLogOutput.pageSize;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "seek"
                           , new Object[] { new Long(currentPage), new Integer(currentByte) }
                                );
        } // seek().

        /*
         * (non-Javadoc)
         * 
         * @see java.io.InputStream#read()
         */
        public int read()
                        throws java.io.IOException
        {

            if (currentByte == page.length) { // is there any remaining data in the current page?
                // Move to the next page.
                currentPage++;

                // Do we need to cycle back to the beginning of the logFile to find the next page? 
                if (currentPage * FileLogOutput.pageSize == header.fileSize) {
                    // Reposition the log to continue after the header. 
                    file.seek(FileLogHeader.headerLength * 2);
                    currentPage = 2;
                    if (header.sectorByte == 0) // Now on the next cycle.
                        header.sectorByte = 1;
                    else
                        header.sectorByte = 0;
                } // if (currentPage*FileLogOutput.pageSize == header.fileSize).           

                int bytesRead = file.read(page); // Get another page of data.

                if (bytesRead != page.length) {
                    return -1;
                } // if (bytesRead != page.length).

                if (!restoreSectorBits(page, header.sectorByte)) { // Validate the sector bits.
                    return -1;
                } // if (!restoreSectorBits()).

                currentByte = 1; // Step over the sector bits.
            }

            int byteToReturn = page[currentByte] & 255;
            currentByte++;
            return byteToReturn;
        } // Of method read().

        /*
         * (non-Javadoc)
         * 
         * @see java.io.InputStream#read(byte[], int, int)
         */
        public int read(byte bytes[]
                        , int offset
                        , int length
                        )
                                        throws java.io.IOException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "read"
                            , new Object[] { bytes, new Integer(offset), new Integer(length) }
                                );

//      Following checking not required.
//      if (b == null) {
//        throw new NullPointerException();
//      } else if ((off < 0) || (off > b.length) || (len < 0) ||
//        ((off + len) > b.length) || ((off + len) < 0)) {
//        throw new IndexOutOfBoundsException();
//      } else if (len == 0) {
//        return 0;
//      }

            int lengthRead = 0;
            while (length > 0) {

                if (currentByte == page.length) { // is there any remaining data?
                    // Move to the next page.
                    currentPage++;

                    // Do we need to cycle back to the beginning of the logFile to find the next page? 
                    if (currentPage * FileLogOutput.pageSize == header.fileSize) {
                        // Reposition the log to continue after the header. 
                        file.seek(FileLogHeader.headerLength * 2);
                        currentPage = 2;
                        if (header.sectorByte == 0) // Now on the next cycle.
                            header.sectorByte = 1;
                        else
                            header.sectorByte = 0;
                    } // if (currentPage*FileLogOutput.pageSize == header.fileSize).    

                    int bytesRead = file.read(page); // Get another page of data.

                    if (bytesRead != page.length) {
                        if (lengthRead == 0)
                            lengthRead = -1;
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this, cclass
                                       , "read"
                                       , "returns lengthRead=" + lengthRead + "(int)"
                                         + " bytesRead=" + bytesRead + "(int)"
                                            );
                        return lengthRead;
                    } // if (bytesRead != page.length).

                    // Validate and restore the sector bits.
                    if (!restoreSectorBits(page, header.sectorByte)) {
                        if (lengthRead == 0)
                            lengthRead = -1;
                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this, cclass
                                       , "read"
                                       , "returns lengthRead=" + lengthRead + "(int)"
                                            );
                        return lengthRead;
                    } // if (!restoreSectorBits()).

                    currentByte = 1; // Step over the sector bits.
                } // if (currentByte == page.length).

                int lengthToCopy = Math.min(length, page.length - currentByte);
                System.arraycopy(page
                                 , currentByte
                                 , bytes
                                 , offset
                                 , lengthToCopy
                                );
                length = length - lengthToCopy;
                offset = offset + lengthToCopy;
                currentByte = currentByte + lengthToCopy;
                lengthRead = lengthRead + lengthToCopy;
            } // while (len > 0).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "read"
                           , "returns lengthRead=" + lengthRead + "(int)"
                                );
            return lengthRead;
        } // read().

    } // inner class SectorValidatedInputStream.

    /**
     * Concatenates a list of byteArrays as a single InputStream.
     */
    private class MultiByteArrayInputStream
                    extends java.io.InputStream
    {

        // The list of byteArrays being read.
        private java.util.List byteArrays;

        // The current byteArray in the list, being read.
        private int byteArrayIndex = 0;

        // The buffer containinng the cursor.
        private byte currentBuffer[];

        // Position within the current buffer indicating the next byte to read.
        private int currentByte = 0;

        /**
         * Constructor.
         * 
         * @param java.util.List of byte arrays.
         */
        MultiByteArrayInputStream(java.util.List byteArrays)
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "<init>"
                            , "byteArrays=" + byteArrays + "(java.util.List)"
                                );

            this.byteArrays = byteArrays;
            currentBuffer = (byte[]) byteArrays.get(byteArrayIndex);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "<init>"
                                );
        } // Constructor.

        /*
         * (non-Javadoc)
         * 
         * @see java.io.ByteArrayInputStream#read()
         */
        public int read()
        {
            int byteToReturn = -1;

            if (currentByte < currentBuffer.length) {
                byteToReturn = currentBuffer[currentByte] & 0xff;
                currentByte++;
            } else { // Try next buffer.
                byteArrayIndex++;
                if (byteArrayIndex < byteArrays.size()) {
                    currentBuffer = (byte[]) byteArrays.get(byteArrayIndex);
                    byteToReturn = currentBuffer[0] & 0xff;
                    currentByte = 1;
                } // if (byteArrayIndex < byteArrays.size()) .  
            } // if (currentByte < currentBuffer.length).
            return byteToReturn;
        } // Of method read().

        /*
         * (non-Javadoc)
         * 
         * @see java.io.InputStream#read(byte[], int, int)
         */
        public synchronized int read(byte buffer[]
                                     , int offset
                                     , int length)
        {

            // Folowing checking not required.
            // if (buffer == null) {
            //   throw new NullPointerException();
            // } else if ((offset < 0) || (offset > buffer.length) || (length < 0) ||
            //   ((offset + length) > buffer.length) || ((offset + length) < 0)) {
            //   throw new IndexOutOfBoundsException();
            // }

            int bytesRead = 0;
            while (length > 0) {
                if (currentByte == currentBuffer.length) {
                    byteArrayIndex++;
                    if (byteArrayIndex < byteArrays.size()) {
                        currentBuffer = (byte[]) byteArrays.get(byteArrayIndex);
                        currentByte = 0;
                    } else {
                        if (bytesRead == 0)
                            bytesRead = -1;
                        break;
                    } // if (byteArrayIndex < byteArrays.size()) .      
                } // if (currentByte == currentBuffer.length). 

                int lengthToCopy = Math.min(length, currentBuffer.length - currentByte);
                System.arraycopy(currentBuffer
                                 , currentByte
                                 , buffer
                                 , offset
                                 , lengthToCopy
                                );
                length = length - lengthToCopy;
                offset = offset + lengthToCopy;
                currentByte = currentByte + lengthToCopy;
                bytesRead = bytesRead + lengthToCopy;
            } // while (length > 0).       

            return bytesRead;
        }
    } // Of inner class multiByteArrayInputStream.

} // End of class LogInput.

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
 * <p>LogOutput, Writes log records to the end of the log.
 * 
 * The log is circular, old data, before the truncate point is written over once the end of the
 * file reached. Log records are written in pieces (called parts) in order to allow multile
 * writers to progress at the same time and avoid a long log record blocking a short one.
 * 
 * The log file is written in whole pages, as this makes better use of the underlying hardware,
 * by avoiding the write of a partial page we avoid the disk having to read the page first before
 * it can write the updated page back. we also avoid corrupting any tata in the unwritten part
 * of the page.
 * 
 * The end bit of each sector in the file is reserved for a sector bit. These change value as we make
 * consecutive passes through the file. The file initially has zero at the end of all of its
 * sectors, so the first pass through the file sets these to one, the next pass sets them to zero
 * and so on. This allows us to see where the end of the useful data is, if we need to read the
 * file. we keep reading until we see the value of the sector bit change. If we reach the endof the
 * file we read from the beginning using the opposite sector bit. Data that ocupies the sector bit
 * is collected into a single byte and is placed in the first byte of each page, this each page
 * holds pageSize-1 bytes of data.
 * 
 * The logger allows a single mark to be set which can later be used to truncate the log
 * at that point.The mark becomes the new start of the log. This is used by the checkpoint
 * helper thread to truncate the log and move any in flight data to after the new start
 * point.
 * 
 * This logger allows the size of the log file to be changed while it is use.
 * 
 * Linear logging not implemented.
 * 
 * The ObjectManager must terminate any logHelpers before it starts a new log
 * if a new instance of the ObjectManager is created.
 * 
 * @author IBM Corporation
 */
public class FileLogOutput
                extends LogOutput
{
    private static final Class cclass = FileLogOutput.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_LOG);

    private java.io.RandomAccessFile logFile; // Persistent storage, accessed read write.  
    private FileLogHeader fileLogHeader; // The header data for this log file.

    // To update the reserved count and filePosition a thread must hold a lock on flushing FlushSet.
    private long filePosition; // The byte address in the file next write will fill.
    private byte filePositionSectorByte; // The sector byte at the file position.
    // The location of the start of the buffer in the log file.
    long bufferFilePosition;
    // The sector byte of the first page in the buffer.
    byte bufferSectorByte;

    private long fileSpaceLeft; // the number of bytes in the log File as yet unwritten.
    //TODO Note that we don't use the value "reserved" in LogOutput.
    private long[] fileSpaceAvailable = new long[16];
    private Object[] fileSpaceAvailableLock = new Object[fileSpaceAvailable.length];
    private long logFullReserved = 0; // Reserved to simulate log full. 
    private Object logFullReservedLock = new Object();

    private long fileMark; // This will become the start address after truncation.
    private byte fileMarkSectorByte; // The sector byte at the fileMark position.
    // a lock which is held when we mark or truncate the log file, only one mark point is supported,
    // so we only need one lock. 
    private Object fileMarkLock = new Object();
    // True if the next flush will truncate the log file to the fileMark. 
    // Protected by synchronize (this).
    private boolean truncateRequested = false;

    // Bytes in the log file written with no space checking, not including unchecked bytes before the mar point.
    // Protected by logBufferLock.
    private long uncheckedBytes = 0;
    // Unchecked space that will be returned at the next truncate. Protected by logBufferLock.
    private long uncheckedBytesUpToMarkPoint = 0;
    private ObjectManagerState objectManagerState; // Responsible for this FileLogOutput.
    // True if Log space usage is high compared to ObjectManagerState.logFullPostCheckpointThreshold
    private boolean ocupancyHigh = false;
    private long newFileSize = 0; // A requested new file size. 
    private boolean newFileSizeRequested = false; // True if a newFileSize is requested. 
    private Object newFileSizeRequestLock = new Object();
    private long logFileSizeRequested = 0;
    private PermanentIOException newFileSizeException = null;

    protected final static int pageSize = 4 * 1024; // Writes end on a 4K page.
    protected final static int sectorSize = 512;

    // The logBuffer holds pages of data to be written to disk. There are several cursors that
    // cycle through the logBuffer pages. There is a set of contiguous pages that are being 
    // written. Followed by a set that are complete and ready to be flushed to disk. Followed 
    // by a set that are ready to notify writers that are waiting for them to be flushed to disk.
    // The rest of the buffer is unused space.
    // 
    // Users of the logBuffer lock logBufferLock to update the cursors, but hold locks
    // on the individual pages if they wait for a write to complete. This avoids writers
    // and waiters competing for the same locks and allows more writers
    // to test that pages are complete. The log buffer must be bigger than a logRecord part
    // so that no single part can fill it.

    // LOCKING STRATEGY IS CURRENTLY TO TAKE logBufferLock LAST
    // FlushHelper, CheckpointHelper and NotifyHelper all take this first then
    // call down to methods that take logBufferLock.  They also may take other locks
    // along the way.
    private LogBuffer logBuffer = new LogBuffer(16);
    private LogBufferLock logBufferLock = new LogBufferLock();

    private class LogBufferLock {}

    /**
     * Padding reserved space. Some space needs to be kept available for padding. The
     * design is to take that space from the space pre-reserved for commit/backout
     * log records. Instead of going straight back to the reserved total once the record
     * is added to the buffer, some might be kept back if we are below the defined minimum.
     * 
     * After a checkpoint is done, we also top up the padding reserved space in case we
     * are trying to shrink the log (continuous checkpoints until start and end are at the
     * beginning would sap the padding reserved space very quickly).
     * 
     * If we find we go below the minimum, we cut an FFDC for information purposes only. We
     * do not expect this to ever happen, however, we would want to know that it has happened.
     * It implies our minimum is too low or other bug. We do not fail at this point, instead
     * we try and do a real reserve, and fail at that point if we don't have enough space.
     * 
     * For PADDING_SPACE_TARGET, 7 pages is the minimum magic number because:
     * - at startup there are 2 checkpoints (4 pages mostly padding)
     * - for each CheckpointEnd there are 2 flushes (2 more pages all padding)
     * - threads that start writing log records might be beaten by the flush helper.
     * i.e. the space given back that was pre-reserved is not given back before
     * the flush helper needs it to pad the page with the commit in it. (1 more page)
     * 
     * This is somewhat lessened by space freed by a checkpoint also contributing to
     * the padding reserved space, but we round it up to 10 in case there are edge cases
     * not yet thought of, if only to avoid getting false positives from the FFDC.
     * 
     * However PADDING_SPACE_TARGET after startup is a function of the size of the log buffer and log
     * file (log file size - log file header) / number of log buffer pages * pageSize (to get bytes)
     * This is because when adding a large log record, we will need to pad 1 page for every log buffer.
     * A log record cannot be larger than the log file size. So the worst case is the (space available
     * to record data in the log file) / the size of the log buffer). This formula is used in
     * calculatePaddingSpaceTarget().
     * */
    private long paddingSpaceAvailable = 0;
    private long PADDING_SPACE_TARGET = 0;
    private static final long PADDING_SPACE_MINIMUM = 10;

    private class PaddingSpaceLock {}

    private PaddingSpaceLock paddingSpaceLock = new PaddingSpaceLock();

    // The next page in the logBuffer to write.
    // If this equals firstPageFilling then no pages are ready to write.
    // Advanced by the FlushHelper thread.
    private int firstPageToFlush = 0;

    // The last page who's waiters are to be notified that it has been forced to disk.
    // Also keep the next page we will notify.
    // Advanced by the NotifyHelper thread.
    private int lastPageNotified;
    private int nextPageToNotify;

    // The first page in the log buffer that is being filled by writer threads.
    // Advanced by locking each page and checking that all writers have completed
    // and that the next page has been started. 
    // To avoid an ambiguity where firstPageFilling == lastPageNotified might mean that the 
    // logBuffer is completely full or completely empty we never use the last page in the 
    // logBuffer.
    private int firstPageFilling = 0;

    // The last logBuffer page which will contain the next write to the logBuffer,
    // advanced under the logBuffer lock. This may not contain any useful data yet
    // as we may be positioned at byte 1 just after the sector byte.
    private int lastPageFilling = 0;

    // The next available byte in the log buffer, advanced under the logBuffer lock.
    // Position this after the first sector byte, this never points to the sector byte,
    // byte zero at the start of the page.
    // Advanced under logBufferLock.
    private int nextFreeByteInLogBuffer = 1;

    // If the log buffer fills it is replaced by a larger one up to a maximum size.
    // The logBuffer must always be larger that the part size otherwise a part cannot fit into a 
    // logBuffer, also the logic in addBuffers that checks  whether a LogRecord will fit into the
    // buffer assumes that a single logRecord cannot wrap round the entire buffer. This also ensures
    // that other than the current buffer there is only ever one other larger log buffer.
    //
    // The size log buffer in pages are to be changed to once all of the current one is filled. 
    // No new request to change the size can be accepted until the current one is written
    // and notified.
    private int newLogBufferPages = 0;
    // The largest size logBuffers may grow to.
    private static final int maximumLogBufferPages = 256;

    protected static final int partHeaderLength = 4; // length of part excluding the partHeader.
    // LogRecords are written in parts up to the following number of bytes.
    // Including the part header.
    private static final int maximumLogRecordPart = 16 * 1024
                    - partHeaderLength;
    private static final int batchSizeOfNewPagesToFormat = 1024;
    protected static final byte PART_First = 0;
    protected static final byte PART_Middle = 1;
    protected static final byte PART_Last = 2;
    protected static final byte PART_Padding = 3;

    // Flags to indicate which multipart identifers are in use.
    private boolean[] multiPartIdentifersUsed = new boolean[127];
    private long[] multiPartFileStart = new long[127];
    private byte[] multiPartSectorByte = new byte[127];
    private long[] multiPartUncheckedBytes = new long[127];
    private Object multiPartIDLock = new Object();

    public static long coldStartLogFileSize = 10 * 1024 * 1024; // Initial size of the log.

    // Worker thread for flushing.
    protected FlushHelper flushHelper = null;
    // Worker thread for notify().
    protected NotifyHelper notifyHelper = null; // Worker thread fir notify().
    // The helper threads must run with higher priority so that it can write and data and notify threads 
    // faster than new work is given to FileLogOutput.
    private final int helperThreadPriority = Thread.NORM_PRIORITY + 1;
    // private final int helperThreadPriority = Thread.MAX_PRIORITY;                     

    // For gatherStatistics.
    private long totalBytesWritten = 0; // Number of bytes written so far.
    private long totalNumberOfFlushRequests = 0; // Number of times flush() was invoked.
    private long totalNumberOfLogBufferWrites = 0; // Number of times the logBuffer is has written.
    private long totalNumberOfThreadsFindingFullLogBuffers = 0;
    private long totalNumberOfFlushHelperWaits = 0;// Number of times the FlushHelper had nothing to do.
    private long totalNumberOfLogCycles = 0; // Number of times we have cycled through the log. 
    private long writeStalledMilliseconds = 0; // Time spent waiting to start copying data into logBuffer. 
    private long writeCopyingMilliseconds = 0; // Time spent copying data into logBuffer.
    private long paddingStalledMilliseconds = 0; // Time spent waiting to pad the logBuffer.
    private long writeUpdateStateMilliseconds = 0; // How long we took to update the logBuffer state under pageStateLocks. 
    private long lastFlushMilliseconds = 0; // Time of last timed flush event. 
    private long flushingMilliseconds = 0;
    private long otherMilliseconds = 0;
    private long flushHelperWaitingMilliseconds = 0;

    private long logFullCheckpointsTriggered = 0;
    private long stalledForMultiPartID = 0;
    private long totalPaddingBytesWritten = 0;
    private long totalPaddingRecords = 0;

    private int[] numberOfFlushWaitersFrequency = new int[17];
    private int[] numberOfPagesWrittenFrequency = new int[17];

    // for GetStats...

    /**
     * Constructor, access the log file and cold start it.
     * 
     * @param logFile the log file.
     * @param objectManagerState creating this FileLogInput.
     * @throws ObjectManagerException
     */
    protected FileLogOutput(java.io.RandomAccessFile logFile,
                            ObjectManagerState objectManagerState)
        throws ObjectManagerException {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logFile,
                                      objectManagerState });
        // Use Write and sync of random access file. 
        this.logFile = logFile;
        // Do not start with sequence number zero so that the first write gets a non zero sequence number.
        // If the sequence number is zero it looks like the write was not allocated a sequence number.
        logBuffer.sequenceNumberOfFirstPage = 1;
        this.objectManagerState = objectManagerState;

        // Put a header in the new file. 
        fileLogHeader = new FileLogHeader();
        fileLogHeader.startByteAddress = FileLogHeader.headerLength * 2;
        // Initial size of the log, must be a whole number of pages.
        fileLogHeader.fileSize = (coldStartLogFileSize / pageSize) * pageSize; // Initial size of the log. 

        // Get a buffer of new pages to write.
        byte[] newPages = new byte[(int) Math.min(pageSize * batchSizeOfNewPagesToFormat, fileLogHeader.fileSize)];
        // Set all sector bits to zero, the initialisation of FileLogHeader assumes this has been done.
        for (int iStart = 0; iStart < newPages.length; iStart = iStart + pageSize) {
            setSectorBits(newPages, iStart, (byte) 0);
        } // for (int iStart = 0...

        try {
            logFile.setLength(fileLogHeader.fileSize);
            logFile.seek(0);
            for (long istart = 0; istart < fileLogHeader.fileSize; istart = istart + newPages.length)
                logFile.write(newPages
                              , 0
                              , (int) Math.min(newPages.length, fileLogHeader.fileSize - istart)
                                );

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "<init>",
                                                exception,
                                                "1:335:1.52");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        // Must be on a page boundary. 
        filePosition = fileLogHeader.writeHeader(logFile);
        // Establish the sector Byte for new writes.
        filePositionSectorByte = fileLogHeader.sectorByte;
        bufferFilePosition = filePosition;
        bufferSectorByte = filePositionSectorByte;

        // Position the log for sequential writing.
        try {
            logFile.seek(filePosition);
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:361:1.52");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        open(); // Complete initialisation.  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // FileLogOutput().

    /**
     * Constructor, access the log file and warm start it, from the specified position.
     * 
     * @param logFile log file.
     * @param initialLogSequenceNumber the intial logSequenceNumber, precededs the next logSequenceneumber we will use.
     * @param page in the log file that is to be written next.
     * @param objectManagerState creating this FileLogInput.
     * @throws ObjectManagerException
     */
    protected FileLogOutput(java.io.RandomAccessFile logFile
                            , long initialLogSequenceNumber
                            , long page
                            , ObjectManagerState objectManagerState)
        throws ObjectManagerException
    {
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logFile,
                                      new Long(initialLogSequenceNumber),
                                      new Long(page),
                                      objectManagerState });

        // Use Write and sync of random access file. 
        this.logFile = logFile;
        logBuffer.sequenceNumberOfFirstPage = ++initialLogSequenceNumber;
        this.objectManagerState = objectManagerState;

        // Read the headers.  
        fileLogHeader = new FileLogHeader(logFile);

        // If we failed during an attempt to change the length of the log file we might 
        // have a file longer than it needs to be, correct this now if it is the case.
        try {
            if (logFile.length() > fileLogHeader.fileSize)
                logFile.setLength(fileLogHeader.fileSize);
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "<init>",
                                                exception,
                                                "1:423:1.52");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        // Set the filePostition. This could be the page after the end of the log
        // in which case we should cycle back to the beginning when the first flush is performed.
        filePosition = page * pageSize;
        // Establish the sector Byte for new writes.
        filePositionSectorByte = fileLogHeader.sectorByte;

        // Have we been positioned behind the current start byte address because we have
        // wrapped round the end of the file? If so flip the sector byte.
        if (filePosition < fileLogHeader.startByteAddress) {
            filePositionSectorByte = (filePositionSectorByte == 0 ? (byte) 1 : (byte) 0);
        } // if (filePosition < fileLogHeader.startByteAddress). 
        bufferFilePosition = filePosition;
        bufferSectorByte = filePositionSectorByte;

        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        methodName,
                        new Object[] { new Byte(filePositionSectorByte),
                                      new Long(fileLogHeader.startByteAddress),
                                      new Long(filePosition) });

        // Position the log for sequential writing.
        try {
            logFile.seek(filePosition);
        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                methodName,
                                                exception,
                                                "1:464:1.52");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch (java.io.IOException exception).

        open(); // Complete initialisation.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , methodName
                            );
    } // FileLogOutput().

    /**
     * Initialise.
     * 
     * @throws ObjectManagerException
     */
    protected void open()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "open"
                            );

        // Identifier zero is used by all single part log records, the others are allocated dynamically.
        multiPartIdentifersUsed[0] = true;
        for (int i = 1; i < multiPartIdentifersUsed.length; i++) {
            multiPartIdentifersUsed[i] = false;
            multiPartFileStart[i] = -1;
        } // for... 

        setFileSpaceLeft(); // Calculate the space left in the log file.

        // Allocate the space available in the log but hold back space for... 
        //         Two FileLogHeaders.
        //         A sector byte on each writable page.
        //         One page to prevent us writing on the page that contains the start point. 
        // Any extra space needed by recovered transactions is reserved after recovery once the active transactions
        // are known. See ObjectManagerState.performRecovery().
        long totalFileSpaceAvailable = fileSpaceLeft
                                       - FileLogHeader.headerLength * 2
                                       - (fileLogHeader.fileSize - FileLogHeader.headerLength * 2) / pageSize
                                       - pageSize;
        for (int i = 0; i < fileSpaceAvailable.length; i++) {
            fileSpaceAvailable[i] = totalFileSpaceAvailable / fileSpaceAvailable.length;
            fileSpaceAvailableLock[i] = new Object();
        } // for...  
        fileSpaceAvailable[0] = fileSpaceAvailable[0] + totalFileSpaceAvailable % fileSpaceAvailable.length;

        // prime the paddingSpace reservation
        calculatePaddingSpaceTarget();

        // Create the thread to notify flush completion. 
        notifyHelper = new NotifyHelper();
        // Create the thread that executes the flush.
        flushHelper = new FlushHelper();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "open"
                            );
    } // open().

    /**
     * Calculate the padding space currently required and do reservation if required.
     * The amount of padding space will either grow (eg. when we first startup) or shrink
     * (eg. when the logbuffer increases in size); also, when the log file size changes!
     * 
     * The only time it is expected that reserve() could throw an Exception (because there
     * is not enough space) is when we are starting (the log is so small there is not
     * enough space for padding minimum. Otherwise all other calls to this method should
     * be ok:
     * 
     * 1) increase log buffer - this decreases the amount of padding needed so no reserve
     * 2) shrinking the file - this decreases the amount of padding needed so no reserve
     * 3) growing the file - this increases the amount of padding needed, but we will
     * have just made a load more space available so the reserve should succeed.
     * 
     * @throws ObjectManagerException if reserve() throws an Exception (LogFileFull)
     */
    private void calculatePaddingSpaceTarget() throws ObjectManagerException
    {
        if (trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "calculatePaddingSpaceTarget",
                        new Object[] { Long.valueOf(PADDING_SPACE_TARGET) }
                            );

        long oldTarget;

        synchronized (paddingSpaceLock)
        {
            oldTarget = PADDING_SPACE_TARGET;
            // we could take out sector bytes as they are already reserved, but its hardly
            // worth it.
            // the numberOfBuffersPerLog represents the maximum number of times we will do a
            // flush in the middle of adding a large log record.  This represents the maximum
            // number of pages we might have to pad (probably a gross over-estimation but at
            // least it is safe).
            long numberOfBuffersPerLog = (fileLogHeader.fileSize - FileLogHeader.headerLength) / (logBuffer.numberOfPages * pageSize);
            PADDING_SPACE_TARGET = Math.max(numberOfBuffersPerLog * pageSize,
                                            PADDING_SPACE_MINIMUM * pageSize);
        }

        long paddingSpaceDelta = PADDING_SPACE_TARGET - oldTarget;
        if (paddingSpaceDelta > 0)
        {
            // padding space required has grown
            reserve(paddingSpaceDelta);
            paddingReserveLogSpace(-paddingSpaceDelta);
        }
        else
        {
            // padding space required has shrunk
            // no need to do anything, it will be given back naturally
        }

        if (trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "calculatePaddingSpaceTarget",
                       new Object[] { Long.valueOf(PADDING_SPACE_TARGET) }
                            );
    }

    /**
     * Prohibits further operations on the LogFile.
     * 
     * @throws ObjectManagerException
     */
    protected void close()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "close"
                            );

        if (flushHelper != null)
            flushHelper.shutdown(); // Complete outstanding work.
        if (notifyHelper != null)
            notifyHelper.shutdown(); // No longer need a notify thread.

        logFile = null;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "close"
                            );
    } // method close();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#setLogFileSize(long)
     */
    protected void setLogFileSize(long newFileSize)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "setLogFileSize"
                        , "newFileSize=" + newFileSize + "(long)"
                            );

        // Note where we are in the log file.
        long initialFilePosition = filePosition;

        synchronized (newFileSizeRequestLock) {
            // Force the new file size to be a whole number of pages.
            this.newFileSize = (newFileSize / pageSize) * pageSize;
            newFileSizeRequested = true;
            newFileSizeException = null;
            logFileSizeRequested = newFileSize;
            // We have not yet seen the log file wrap.
            boolean wrapped = false;

            // Keep requesting checkpoints until the start and end of the log data are both 
            // before the new end of the log, without the user log data wrapping round the 
            // end of the logFile.
            while (newFileSizeRequested) {

                // Check there is still some chance of fitting the current log into the new one.
                // and that we have not wrapped round the log file without succeeding.
                long fileSpaceAvailable = getLogFileSpaceLeft();
                long newFileSpaceAvailable = fileSpaceAvailable - fileLogHeader.fileSize + newFileSize;
                float newOcupancy = (float) 1.0 - (float) (newFileSpaceAvailable) / (float) (newFileSize);
                if (newOcupancy > objectManagerState.logFullPostCheckpointThreshold
                    || wrapped) {
                    newFileSizeRequested = false;
                    logFileSizeRequested = 0;
                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "setLogFileSize",
                                   new Object[] { new Long(fileLogHeader.fileSize),
                                                 new Long(newFileSize),
                                                 new Float(newOcupancy) });
                    throw new LogFileSizeTooSmallException(this
                                                           , fileLogHeader.fileSize
                                                           , newFileSize
                                                           , fileSpaceAvailable
                                                           , newOcupancy
                                                           , objectManagerState.logFullPostCheckpointThreshold);
                } // if (   (fileSpaceLeft...

                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                "setLogFileSize",
                                new Object[] { new Long(filePosition),
                                              new Long(initialFilePosition) });

                // If we have wrapped round the log file, give up after the next attempt to satisfy the 
                // request. 
                if (filePosition < initialFilePosition)
                    wrapped = true;

                // We have released the lock on writingFlushSet set so now we can request a checkpoint and
                // see if we are successful.
                objectManagerState.waitForCheckpoint(true);

                // Speed up moving the filePosition by writing a reasonable ammount of data.
                // The folowing is dangerous because it can cause the request to fail if we 
                // are trying to reduce the log file size close to the minimum possible. 
//        PaddingLogRecord paddingLogRecord = new PaddingLogRecord(logBufferSize/2);
//        writeNext(paddingLogRecord
//                 ,0
//                 ,true
//                 );

            } // while (newFileSizeRequested...

            logFileSizeRequested = 0;
        } // synchronized (newFileSizeRequestLock).                                            

        if (newFileSizeException != null) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "setLogFileSize"
                           , "via PermanentIOException newFileSizeException=" + newFileSizeException + "(PermanentIOException)"
                                );
            throw newFileSizeException;
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "setLogFileSize"
                            );
    } // setLogFileSize(). 

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#getLogFileSize()
     */
    protected long getLogFileSize()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getLogFileSIze"
                            );

        long logFileSize = fileLogHeader.fileSize;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getLogFileSize"
                       , "returns logFileSize=" + logFileSize + "(long)"
                            );
        return logFileSize;
    } // End of method getLogFileSize().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.LogOutput#getLogFileSizeRequested()
     */
    protected long getLogFileSizeRequested()
    {
        return logFileSizeRequested;
    }

    protected long getLogFileSpaceLeft()
    {
        long fileSpaceLeft = 0;
        for (int i = 0; i < fileSpaceAvailable.length; i++) {
            synchronized (fileSpaceAvailableLock[i]) {
                fileSpaceLeft = fileSpaceLeft + fileSpaceAvailable[i];
            } // synchronized (fileSpaceAvailableLock[i]).
        } // for ...
        return fileSpaceLeft;
    } // getLogFileSpaceLeft().

    /**
     * @return boolean Returns true if the ocupancy of the log file is high.
     */
    protected boolean isOcupancyHigh()
    {
        return ocupancyHigh;
    }

    /**
     * Sets the amount of space still left in the log file.
     * 
     * @throws ObjectManagerException
     */
    private void setFileSpaceLeft()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "setFileSpaceLeft",
                        new Object[] { new Long(fileLogHeader.fileSize), new Long(fileLogHeader.startByteAddress),
                                      new Long(filePosition) }
                            );

        // Assume we have wrapped around the end of the file, the space left is between the current
        // file position and the start of the log file.
        long newFileSpaceLeft = fileLogHeader.startByteAddress - filePosition;
        if (newFileSpaceLeft <= 0) // If we have not wrapped. 
            newFileSpaceLeft = newFileSpaceLeft + fileLogHeader.fileSize - FileLogHeader.headerLength * 2;
        fileSpaceLeft = newFileSpaceLeft;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "setFileSpaceLeft",
                       new Object[] { new Long(fileSpaceLeft) });
    } // setFileSpaceLeft(). 

    /*
     * Set the sectorBits in a page.
     * The sector bit is the last bit the end of each sector. They are set to one on the first
     * cycle through the log file an then to one on the next cycle, alternating on
     * each successive cycle through the log. They are used to detect whether a
     * sector has bee written yet on the current cycle through the log. The bits that
     * were at the end of each sector are collected together and saved in the first byte
     * of the page, which must not be used for log data.
     * 
     * @param byte[] the byte buffer where the bits are set.
     * 
     * @param int the offset in the logBuffer where the page starts.
     * 
     * @param byte if zero sector bit is set to zero, otherwise set to one.
     */
    protected static void setSectorBits(byte[] byteArray
                                        , int offset
                                        , byte requiredSectorByte
                    )
    {
//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.entry(cclass
//                 ,"setSectorBits"
//                 ,new Object[] {new Integer(byteArray.hashCode()),
//                                new Integer(byteArray.length),
//                                new Integer(offset),
//                                new Byte(requiredSectorByte)}
//                 );

        byte sectorByte = (byte) ((byteArray[offset + sectorSize * 1 - 1] & 1)
                                  + (byteArray[offset + sectorSize * 2 - 1] & 1) * 2
                                  + (byteArray[offset + sectorSize * 3 - 1] & 1) * 4
                                  + (byteArray[offset + sectorSize * 4 - 1] & 1) * 8
                                  + (byteArray[offset + sectorSize * 5 - 1] & 1) * 16
                                  + (byteArray[offset + sectorSize * 6 - 1] & 1) * 32
                                  + (byteArray[offset + sectorSize * 7 - 1] & 1) * 64
                                  + (byteArray[offset + sectorSize * 8 - 1] & 1) * 128
                        );
        byteArray[offset] = sectorByte;

        if (requiredSectorByte == 0) {
            // Set bits off.
            byteArray[offset + sectorSize * 1 - 1] &= 0xFE;
            byteArray[offset + sectorSize * 2 - 1] &= 0xFE;
            byteArray[offset + sectorSize * 3 - 1] &= 0xFE;
            byteArray[offset + sectorSize * 4 - 1] &= 0xFE;
            byteArray[offset + sectorSize * 5 - 1] &= 0xFE;
            byteArray[offset + sectorSize * 6 - 1] &= 0xFE;
            byteArray[offset + sectorSize * 7 - 1] &= 0xFE;
            byteArray[offset + sectorSize * 8 - 1] &= 0xFE;
        } else {
            // Set bits on.
            byteArray[offset + sectorSize * 1 - 1] |= 1;
            byteArray[offset + sectorSize * 2 - 1] |= 1;
            byteArray[offset + sectorSize * 3 - 1] |= 1;
            byteArray[offset + sectorSize * 4 - 1] |= 1;
            byteArray[offset + sectorSize * 5 - 1] |= 1;
            byteArray[offset + sectorSize * 6 - 1] |= 1;
            byteArray[offset + sectorSize * 7 - 1] |= 1;
            byteArray[offset + sectorSize * 8 - 1] |= 1;
        } // if (fileLogHeader.sectorByte == 0).

//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.exit(cclass
//                ,"setSectorBits"
//                ,"sectorByte="+sectorByte+"(byte)"
//                );
    } // setSectorBits. 

    /**
     * Writes buffered output to hardened storage. By the time this method returns all
     * of the data in the logBuffer must have been written to the disk. We mark the
     * last page as having a thread waiting. If there are no threads currently writing
     * to any page we wake the flushHelper otherwise we let the writers wake the
     * flushHelper, if it is stalled. Blocks until the write to disk has completed.
     * 
     * @throws ObjectManagerException
     */
    final void flush()
                    throws ObjectManagerException
    {
        final String methodName = "flush";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        int startPage = 0;
        // The logBuffer we will wait for.
        LogBuffer flushLogBuffer = null;

        synchronized (logBufferLock) {
            startPage = lastPageFilling;

//      // Don't flush a new empty page, where we have stepped past the sector byte
//      // but not put anything in it.
//      if (nextFreeByteInLogBuffer % pageSize == 1) {
//        int newStartPage = startPage -1;
//        if (lastPageNotified == newStartPage ) {
//          System.out.println("FFFFFFFFFFFFFFF Flush bypassed");
//        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//          trace.exit(this,
//                     cclass,
//                     "flush");
//        return; 
//        }
//      } // if (nextFreeByteInLogBuffer % pageSize == 1).

            // Capture the logBuffer containing the page we will flush.
            flushLogBuffer = logBuffer;
            flushLogBuffer.pageWaiterExists[startPage] = true;

            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Object[] { "logBuffer_flushing",
                                          new Integer(startPage),
                                          new Integer(firstPageFilling),
                                          new Integer(lastPageFilling),
                                          new Integer(flushLogBuffer.pageWritersActive.get(startPage)) });

        } // synchronized (logBufferLock).

        flushLogBuffer.waitForFlush(startPage);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName);
    } // flush().

    /**
     * Writes buffered output to hardened storage, then ensure that the logHeader is
     * rewritten to indicate a new start point in the log. This will be at a point
     * previously set by a call to markAndWriteNext(). This blocks until
     * actual truncation has taken place.
     * 
     * @throws ObjectManagerException
     */
    protected void truncate()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "truncate",
                        new Object[] { new Long(fileMark) });

        // Make sure that all of the buffered data in the log is written.
        // The extra flush causes an extra padding log record to be written even if the page is empty
        // so truncate has a two page overhead. This flush may trigger a checkpoint even though the
        // log is in the process of truncating to recover space.
        flush();
        synchronized (fileMarkLock) {
            // The next flush request will cause the actual truncation. 
            truncateRequested = true;
            flush();
        } // synchronized (fileMarkLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "truncate");
    } // truncate(). 

    /**
     * Reserve space in the log file.
     * We don't have to account for sector bytes because those were reserved at startup.
     * 
     * @param reservedDelta the change to the number of reserved bytes if this write is successful.
     * @throws ObjectManagerException
     */
    protected void reserve(long reservedDelta)
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        "reserve",
                        new Object[] { new Long(reservedDelta) }
                            );
        long unavailable = reserveLogFileSpace(reservedDelta);
        if (unavailable != 0) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass,
                           "reserve",
                           new Object[] { "via LogFileFullException", new Long(unavailable), new Long(reservedDelta) }
                                );
            throw new LogFileFullException(this
                                           , reservedDelta
                                           , reservedDelta
                                           , reservedDelta - unavailable);
        } // if (unavailable != 0).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "reserve");
    } // reserve().

    /**
     * Reserve space in the log file. If the required space is not available then
     * no space is acllocated in the log file. The space of freed when the log file is
     * truncated.
     * 
     * @param reservedDelta the change to the number allocated if they are available. Can be
     *            negative, if bytes are being returned, in which case the call is always
     *            successful.
     * @return long zero if the space was available or the number of bytes that could not be found.
     */
    private long reserveLogFileSpace(long reservedDelta)
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "reserveLogFileSpace",
                        new Object[] { new Long(reservedDelta) });

        long stillToReserve = reservedDelta;
        // Pick an arbitrary starting point in the array of sub totals.
        int index = new java.util.Random(Thread.currentThread().hashCode()).nextInt(fileSpaceAvailable.length);
        int startIndex = index;

        while (stillToReserve != 0) {
            synchronized (fileSpaceAvailableLock[index]) {
                if (stillToReserve <= fileSpaceAvailable[index]) {
                    fileSpaceAvailable[index] = fileSpaceAvailable[index] - stillToReserve;
                    stillToReserve = 0;
                } else {
                    stillToReserve = stillToReserve - fileSpaceAvailable[index];
                    fileSpaceAvailable[index] = 0;
                    // Move on to the next subTotal.
                    index++;
                    if (index == fileSpaceAvailable.length)
                        index = 0;
                    if (index == startIndex)
                        break;
                } // if (stillToReserve <= fileSpaceAvailable[index]).
            } // synchronized (fileSpaceAvailableLock[index]).
        } // while...

        // Did we get all we needed?
        if (stillToReserve != 0) {
            // Give back what we got.
            long giveBack = reservedDelta - stillToReserve;
            for (int i = 0; i < fileSpaceAvailable.length; i++) {
                synchronized (fileSpaceAvailableLock[i]) {
                    fileSpaceAvailable[i] = fileSpaceAvailable[i] + giveBack / fileSpaceAvailable.length;
                    if (i == startIndex)
                        fileSpaceAvailable[i] = fileSpaceAvailable[i] + giveBack % fileSpaceAvailable.length;
                } // synchronized (fileSpaceAvailableLock[i]).
            } // for ..
        } // if (stillToReserve != 0).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "reserveLogFileSpace"
                       , new Object[] { new Long(stillToReserve), new Integer(startIndex) });
        return stillToReserve;
    } // reserveLogFileSpace().

    /**
     * Reserve or unreserve log space, but keep back an amount used for padding. This method is used to ensure
     * there is always enough padding space by keeping back space returned by add, delete and replace operations
     * committing or backing out which was reserved up front.
     * This never fails. It gives space even if not available.
     * 
     * @param spaceToReseve either positive or negative. If negative we give to paddingSpace first before returning
     *            the rest. If positive we deduct from the space we have held back.
     * @return the space actually reserved.
     * @throws ObjectManagerException
     * 
     */
    private long paddingReserveLogSpace(long spaceToReserve) throws ObjectManagerException
    {
        if (trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "paddingReserveLogSpace",
                        new Object[] { new Long(spaceToReserve) });
        synchronized (paddingSpaceLock)
        {
            // adjust the padding space
            paddingSpaceAvailable -= spaceToReserve;

            // is this a reserve or unreserve
            if (spaceToReserve > 0)
            {
                // space being reserved          

                // if paddingSpaceAvailable has gone negative we should do a real reserve for the 
                // difference.  Don't let paddingSpaceAvailable go negative!
                // Also cut an FFDC because this should not happen!
                if (paddingSpaceAvailable < 0)
                {
                    NegativePaddingSpaceException exception = new NegativePaddingSpaceException(this, paddingSpaceAvailable);
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        "paddingReserveLogSpace",
                                                        exception,
                                                        "1:1088:1.52");

                    spaceToReserve = -paddingSpaceAvailable;
                    paddingSpaceAvailable = 0;
                }
                else
                {
                    // success case - we don't need to reserve any more
                    spaceToReserve = 0;
                }
            }
            else
            {
                // its an unreserve.

                if (paddingSpaceAvailable > PADDING_SPACE_TARGET)
                {
                    // space being unreserved and we have exceeded our target so need to give some back
                    spaceToReserve = PADDING_SPACE_TARGET - paddingSpaceAvailable;
                    paddingSpaceAvailable = PADDING_SPACE_TARGET;
                }
                else
                {
                    // space being unreserved and we will keep it all
                    spaceToReserve = 0;
                }
            }
        } // drop lock before giving back any space

        if (spaceToReserve != 0)
        {
            // this can throw ObjectManagerException, only if spaceToReserve is positive.
            // This is good, because not only should spaceToReserve never be positive,
            // we must stop now because we are about to write into space that we don't have.
            reserve(spaceToReserve);
        }

        if (trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "paddingReserveLogSpace", new Object[] { new Long(spaceToReserve) });
        return spaceToReserve;
    }

    /**
     * Copy a LogRecord into the LogBuffer ready to write to end of the LogFile.
     * 
     * @param logRecord to be appended.
     * @param reservedDelta the change to the number of reserved bytes if this write is successful.
     * @param checkSpace true is we should check that log file space is available before filling the buffer.
     * @param flush true if the logReciord must be forced to disk before we return.
     * 
     * @return long Log Sequence Number identifies the poition in the log.
     * @throws ObjectManagerException
     */
    protected final long writeNext(LogRecord logRecord
                                   , long reservedDelta
                                   , boolean checkSpace
                                   , boolean flush
                    )
                                    throws ObjectManagerException
    {

//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())  
//      trace.entry(this,
//                  cclass,
//                  "writeNext",
//                  new Object[] {logRecord,
//                                new Long(reservedDelta),
//                                new Boolean(checkSpace),
//                                new Boolean (flush));

        long logSequenceNumber = addLogRecord(logRecord,
                                              reservedDelta,
                                              false,
                                              checkSpace,
                                              flush);

//    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
//      trace.exit(this,
//                 cclass,
//                 "writeNext",
//                 new Object[] {new Long(logSequenceNumber)});
        return logSequenceNumber;
    } // writeNext(). 

    /**
     * Includes a LogRecord in a FlushSet for writing to end of the LogFile,
     * as with writeNext but also sets the truncation mark to immediately befrore
     * the written logRecord.
     * 
     * @param logRecord to be appended.
     * @param reservedDelta the change to the number of reserved bytes if this write is successful.
     * @param checkSpace true is we should check that log file space is available before
     *            filling the buffer.
     * @param flush true if the logRecord must be forced to disk befpre we return.
     * @return long Log Sequence Number identifies the poition in the log.
     * @throws ObjectManagerException
     */
    protected final long markAndWriteNext(LogRecord logRecord,
                                          long reservedDelta,
                                          boolean checkSpace,
                                          boolean flush)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "markAndWriteNext",
                        new Object[] { logRecord,
                                      new Long(reservedDelta),
                                      new Boolean(checkSpace),
                                      new Boolean(flush) });

        long logSequenceNumber;
        synchronized (fileMarkLock) {
            logSequenceNumber = addLogRecord(logRecord,
                                             reservedDelta,
                                             true,
                                             checkSpace,
                                             flush);
        } // synchronized (fileMarkLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "markAndWriteNext"
                       , new Object[] { new Long(logSequenceNumber), new Long(fileMark) }
                            );
        return logSequenceNumber;
    } // markAndWriteNext().

    /**
     * Add the data from a logRecord to the logBuffer.
     * 
     * @param logRecord from which to write the next part.
     * @param reservedDelta the change to the number of reserved bytes if this write is successful.
     * @param setMark true if fileMark is to be set.
     * @param checkSpace true is we should check that log file space is available before
     *            filling the buffer.
     * @param flush true if the logRecord must be forced to disk before we return.
     * 
     * @return long the Log Sequence Number that identifies the position in the log.
     * @throws ObjectManagerException
     */
    private long addLogRecord(LogRecord logRecord,
                              long reservedDelta,
                              boolean setMark,
                              boolean checkSpace,
                              boolean flush)
                    throws ObjectManagerException
    {

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "addLogRecord",
                        new Object[] { logRecord,
                                      new Long(reservedDelta),
                                      new Boolean(setMark),
                                      new Boolean(checkSpace),
                                      new Boolean(flush) });

        // Find the number of bytes we want to write.
        int totalBytes = logRecord.getBytesLeft();

        // Make sure the whole logRecord will fit in the log file, along with any extra space it
        // has reserved. This does not account for any space that will be wasted by writing 
        // PaddingLogRecords. This check ensures that this buffer will not take us past the 
        // start point of the log written in the logHeader overwriting log records that we 
        // need for restart. The transactions reserve space assuming a worst case of a 
        // PaddingLogRecord for each commit. If we discover part way through a transaction 
        // the the log is full we can always successfully release our reserved space and commit the
        // transaction. 
        // Note that we don't have to account for sector bytes because they are already incoporated 
        // into the reserved space.
        long newSpaceAllocatedInLogFile = 0;
        // Include any reserved space, the logData and completely filled parts.
        newSpaceAllocatedInLogFile = reservedDelta + totalBytes + partHeaderLength * (totalBytes / maximumLogRecordPart);
        // Add an extra part if one is partially filled.
        if (totalBytes % maximumLogRecordPart > 0)
            newSpaceAllocatedInLogFile = newSpaceAllocatedInLogFile + partHeaderLength;

        // Make sure the logRecord will fit in the log file if we are requesting new space.
        // If we are returning space then this is done after writing the log record so that 
        // the record will be written and if required, flushed within its already reserved space.
        if (checkSpace && newSpaceAllocatedInLogFile > 0) {
            long unavailable = reserveLogFileSpace(newSpaceAllocatedInLogFile);
            if (unavailable != 0) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "addLogRecord",
                               new Object[] { "via LogFileFullException",
                                             new Long(unavailable),
                                             new Long(newSpaceAllocatedInLogFile),
                                             new Long(reservedDelta) });
                throw new LogFileFullException(this,
                                               newSpaceAllocatedInLogFile,
                                               reservedDelta,
                                               newSpaceAllocatedInLogFile - unavailable);
            } // if (!reserve(newSpaceAllocatedInLogFile).
        } // if (checkSpace...

        // Decide if we need to split the log record into multiple parts. we do not allow long logRecords
        // to be written into the LogBuffer as a single piece because that would stop other log records 
        // from being written if they used up the whole of the log buffer. Additionally this would also mean that 
        // log records must be smaller than the log buffer in order to fit into it.
        if (totalBytes > maximumLogRecordPart) {
            idNotFound: while (logRecord.multiPartID == 0) {
                synchronized (multiPartIDLock) {
                    for (int i = 1; i < multiPartIdentifersUsed.length; i++) {
                        if (!multiPartIdentifersUsed[i]) {
                            logRecord.multiPartID = (byte) i;
                            multiPartIdentifersUsed[i] = true;
                            break idNotFound;
                        } // if(!multiPartIdentifersUsed[i]).  
                    } // for multiPartIdentifersUsed...  
                } // synchronized (multiPartIDLock).

                // Wait and see if an identifier becomes available as other long 
                // logRecords complete.
                Object lock = new Object();
                synchronized (lock) {
                    stalledForMultiPartID++;
                    try {
                        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                            trace.debug(this,
                                        cclass,
                                        "addLogRecord",
                                        "About to wait for 10 milliseconds.");

                        lock.wait(10);
                    } catch (InterruptedException exception) {
                        // No FFDC Code Needed.
                        ObjectManager.ffdc.processException(this,
                                                            cclass,
                                                            "addLogRecord",
                                                            exception,
                                                            "1:1324:1.52");

                        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                            trace.exit(this,
                                       cclass,
                                       "addLogRecord",
                                       exception);
                        throw new UnexpectedExceptionException(this,
                                                               exception);
                    } // catch (InterruptedException exception).
                } // synchronized (lock).

            } // while (logRecord.multiPartID == 0).  
        } // if ( totalBytes > maximumLogRecordPart ).

        // Repeat attempts to add to the logBuffer until we find one that has space
        // for the LogRecord or we have added all of the parts.
        long logSequenceNumber = 0;
        for (;;) {
            logSequenceNumber = addBuffers(logRecord,
                                           setMark,
                                           checkSpace,
                                           flush);
            if (logSequenceNumber == -1)
                flush();
            else if (logSequenceNumber > 0)
                break;
        } // for (;;).   

        // Return any reserved space.
        if (checkSpace && newSpaceAllocatedInLogFile < 0) {
            paddingReserveLogSpace(newSpaceAllocatedInLogFile);
        } // if (checkSpace...

        // Did the logRecord get split?
        if (logRecord.multiPartID != 0) {
            multiPartIdentifersUsed[logRecord.multiPartID] = false;
        } // if (logRecord.multiPartID != 0). 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "addLogRecord",
                       new Object[] { new Long(logSequenceNumber) });
        return logSequenceNumber;
    } // addLogRecord(). 

    /**
     * Add some data from a LogRecord to the logBuffer if there is room.
     * 
     * @param logRecord from which to write the next part.
     * @param setMark true if fileMark is to be set.
     * @param checkSpace true is we should check that log file space is available before
     *            filling the buffer.
     * @param flush true if we must force the last part of the logRecord to disk before we return
     *            with the logSequenceNumber set.
     * 
     * @return long the LogSequenceNumber that identifies the position in the log
     *         or zero if the logRecord is not completely written.
     *         or -1 if the logBuffer was full, and nothing was written.
     * @throws ObjectManagerException
     */
    private long addBuffers(LogRecord logRecord
                            , boolean setMark
                            , boolean checkSpace
                            , boolean flush
                    )
                                    throws ObjectManagerException

    {
        final String methodName = "addBuffers";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logRecord,
                                      new Boolean(setMark),
                                      new Boolean(checkSpace),
                                      new Boolean(flush) });

        // Where we will put the data to be logged.
        int reservedAddress = 0;
        // The first and last pages in the logBuffer that we write into. 
        int startPage = 0;
        int endPage = 0;
        // The LogBuffers we will start and end filling.
        LogBuffer startFillingLogBuffer;
        LogBuffer endFillingLogBuffer;
        // LogSequenceNumber for return; 
        long returnLogSequenceNumber = 0;

        // Find the number of bytes we will write this time.
        int totalBytes = logRecord.getBytesLeft();

        // Decide if we need to split the log record into multiple parts. We do not allow long logRecords
        // to be written into the LogBuffer as a single piece because that would stop other log records 
        // from being written if they used up the whole of the log buffer. Additionally this would also mean that 
        // log records must be smaller than the log buffer in order to fit into it.
        boolean completed = false;
        if (totalBytes > maximumLogRecordPart) {
            // set the length to be written this time.
            totalBytes = maximumLogRecordPart;
        } else {
            completed = true;
        }

        long startWriteMilliseconds;
        if (gatherStatistics) {
            startWriteMilliseconds = System.currentTimeMillis();
        } // if (gatherStatistics). 

        // Lock the logBuffer so that we can work out where to copy this logRecord.
        synchronized (logBufferLock) {
            if (gatherStatistics) {
                // Time how long we took to get the lock on the logBufferLock. 
                long now = System.currentTimeMillis();
                writeStalledMilliseconds += now - startWriteMilliseconds;
                startWriteMilliseconds = now;
            } // if (gatherStatistics). 

            // Set address for the copy.
            reservedAddress = nextFreeByteInLogBuffer;
            // We never set nextFreeByteInLogBuffer to be the first byte, because this
            // is ultimately occupied by the sector bits copied from each sector. Hence there 
            // is no possibility of having to step over the sector byte.
            // if (nextFreeByteInLogBuffer%pageSize == 0) reservedAddress++;
            // startPage = reservedAddress/pageSize;
            startPage = lastPageFilling;

            // Calculate the number of extra page boundaries we will cross each time we start a new page, 
            // because we will need to leave a byte at the beginning of each one to put the sector bits in.
            // Work out pages started here rather than outside synchronize(logBufferLock) because we now know the
            // exact start address within the buffer. The -1 is to allow for the sector byte used in the first page, 
            // the pageSize-1 is the number of data bytes we expect to fit into each page. 
            int pagesStarted = (totalBytes + partHeaderLength + (reservedAddress % pageSize) - 1) / (pageSize - 1);

            // Move the start address on for the next log Record. This will leave us positioned
            // after byte zero, the sector byte but never on the sector byte.
            int updatedNextFreeByteInLogBuffer = reservedAddress + totalBytes + partHeaderLength + pagesStarted;

            // See if we have wrapped around the end of the logBuffer.
            boolean wrappedLogBuffer = false;
            // Capture the logBuffer where we will start to fill.
            startFillingLogBuffer = logBuffer;

            // Capture the current last page we notified, this may move forward while we do the check on the space 
            // in the LogBuffer, that is OK because we will fail to find the space and then come back here again.
            // This is updated by the NotifyHelper after it has cleared pageWritersStarted, so we must capture 
            // it before we advance pageWritersStarted. The notify helper cannot move this past lastPageFilling 
            // because we hold the logBufferLock and will not advance this before we release it.   
            // We only fill up to the nextToLastPageNotified otherwise an empty logBuffer and a full logBuffer
            // both have firstPageFilling == lastPageBNotified and so cannot be distinguished.
            int nextToLastPageNotified = lastPageNotified - 1;
            if (nextToLastPageNotified < 0)
                nextToLastPageNotified = startFillingLogBuffer.numberOfPages - 1;

            if (updatedNextFreeByteInLogBuffer >= startFillingLogBuffer.buffer.length) {
                updatedNextFreeByteInLogBuffer = updatedNextFreeByteInLogBuffer - startFillingLogBuffer.buffer.length;
                wrappedLogBuffer = true;
            } // if (updatedNextFreeByteInLogBuffer >= logBuffer.length). 
            endPage = updatedNextFreeByteInLogBuffer / pageSize;

            // Make sure there is enough room left in the logBuffer and that we have not run into the pages that 
            // are still waiting to be written and notified. This prevents us from copying over over unwritten data.
            //
            // Take the two cases.
            //
            //    Buffer Wrapped Fill < Notify                  Buffer not wrapped Fill > Notify
            //           Fill        Notify                              Notify     Fill
            //    I//////            //////I                    I        ///////////         I
            //
            // The end page must not be in the unwritten hashed pages. We cannot run into the firstPageToNotify 
            // by padding the logBuffer because we only pad if a single page is filling. We always leave a one
            // page gap between the last full page and the lastPaageNotified, hence startPage is never equal
            // to nextToLastPageNotified.
            // We cannot test fillingLogBuffer.pageFlushPending[endPage] because we may be using a different log buffer
            // to the one the flushHelper is using.
            // This test assumes that a single LogRecord part cannot wrap round the entire buffer.
            if (((startPage < nextToLastPageNotified) && (endPage < startPage || endPage >= nextToLastPageNotified))
                || ((startPage > nextToLastPageNotified) && (endPage < startPage && endPage >= nextToLastPageNotified))) {
                if (gatherStatistics)
                    totalNumberOfThreadsFindingFullLogBuffers++;
                // Request a larger logBuffer.
                if (newLogBufferPages == 0
                    && startFillingLogBuffer.numberOfPages < maximumLogBufferPages)
                    newLogBufferPages = Math.min(startFillingLogBuffer.numberOfPages * 2,
                                                 maximumLogBufferPages);

                // Go round again and see if the next attempt finds a logBuffer with enough space left.
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { "logBuffer_full",
                                             new Integer(startPage),
                                             new Integer(endPage),
                                             new Integer(startFillingLogBuffer.pageWritersActive.get(startPage)),
                                             new Integer(nextToLastPageNotified) });
                return -1;
            } // if(  (lastPageFilling < localLastPageNotified)...

            /*
             * Debug
             * // If we stepped into a new page the we must be the first writer.
             * if (logRecord.multiPartID != 0) {
             * if (logRecord.atStart()) {
             * // Preserve the start of any long active LogRecords.
             * multiPartFileStart[logRecord.multiPartID] = bufferFilePosition + reservedAddress;
             * multiPartSectorByte[logRecord.multiPartID] = filePositionSectorByte;
             * if (multiPartFileStart[logRecord.multiPartID] > fileLogHeader.fileSize) {
             * multiPartFileStart[logRecord.multiPartID] = multiPartFileStart[logRecord.multiPartID] - fileLogHeader.fileSize + FileLogHeader.headerLength * 2;
             * multiPartFileStart[logRecord.multiPartID] = (multiPartFileStart[logRecord.multiPartID] == 0 ? (byte) 1 : (byte) 0);
             * } // if (multiPartFileStart[logRecord.multiPartID] > fileLogHeader.fileSize).
             * // Capture unchecked bytes up to the start of this LogRecord,
             * // in case we have to truncate to this point.
             * multiPartUncheckedBytes[logRecord.multiPartID] = uncheckedBytes;
             * 
             * } else if (completed) {
             * multiPartFileStart[logRecord.multiPartID] = -1;
             * } // if (logRecord.atStart()).
             * } // if (logRecord.multiPartID != 0).
             * // Debug
             */

            // Were we asked to set the mark point?
            if (setMark) {
                // Set Mark for first part only, later we truncate up to this point.
                if (logRecord.atStart()) {
                    fileMark = bufferFilePosition + reservedAddress;
                    fileMarkSectorByte = bufferSectorByte;
                    uncheckedBytesUpToMarkPoint = uncheckedBytes;

                    if (fileMark > fileLogHeader.fileSize) {
                        fileMark = fileMark - fileLogHeader.fileSize + FileLogHeader.headerLength * 2;
                        fileMarkSectorByte = (fileMarkSectorByte == 0 ? (byte) 1 : (byte) 0);
                    } // if (fileMark > fileLogHeader.fileSize).

                    if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                        trace.debug(this,
                                    cclass,
                                    methodName,
                                    new Object[] { "setMark",
                                                  new Long(fileMark),
                                                  new Byte(fileMarkSectorByte),
                                                  new Long(bufferFilePosition),
                                                  new Byte(bufferSectorByte) });
                } // if (logRecord.atStart()).

                // Include any long active log records. Find any log long Records that
                // ended after the Mark point or are still being written. Move the mark point
                // back to the earliest start.The actual start of the log file cannot be moved
                // while we do this calculation because we hold the fileMarkLock so we use that
                // as the reference point in the file to decide which LogRecord is the earliest
                // in the active part of the file.
                long fileMarkOffsetFromStart = offsetFromStart(fileMark);
                long smallestFileMarkOffsetFromStart = fileMarkOffsetFromStart;

                for (int i = 1; i < multiPartFileStart.length; i++) {
                    if (multiPartFileStart[i] >= 0) {
                        // Still writing.
                        long offsetFromStart = offsetFromStart(multiPartFileStart[i]);
                        if (offsetFromStart < smallestFileMarkOffsetFromStart) {
                            smallestFileMarkOffsetFromStart = offsetFromStart;
                            fileMark = multiPartFileStart[i];
                            fileMarkSectorByte = multiPartSectorByte[i];
                            uncheckedBytesUpToMarkPoint = multiPartUncheckedBytes[logRecord.multiPartID];
                        } // if (offsetFromStart < smallestFileMarkOffsetFromStart).
                    } // if (multiPartFileStart[i] >= 0).
                } // for...

                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                methodName,
                                new Object[] { "truncateMarkSet",
                                              new Long(fileMark),
                                              new Byte(fileMarkSectorByte),
                                              new Long(uncheckedBytesUpToMarkPoint) });

                // Reset the unchecked bytes remaining after the mark point.
                uncheckedBytes = uncheckedBytes - uncheckedBytesUpToMarkPoint;
            } // if (setMark).

            if (!checkSpace) {
                // Unchecked bytes are not returned to the available space when we truncate the log file as they are already
                // reserved and remain reserved after the truncate.
                uncheckedBytes = uncheckedBytes + totalBytes + partHeaderLength;
            } // if (!checkSpace).    

            if (wrappedLogBuffer) {
                startFillingLogBuffer.wrapped();
            } // if (wrappedLogBuffer).
              // Now capture the logBuffer we end filling, this may be the same as the start one if 
              // we did not wrap or we did not change the logBuffer.
            endFillingLogBuffer = logBuffer;

            if (completed) {
                returnLogSequenceNumber = endFillingLogBuffer.sequenceNumberOfFirstPage + endPage;
                logSequenceNumber = returnLogSequenceNumber;
                if (flush)
                    endFillingLogBuffer.pageWaiterExists[endPage] = true;
            } // if(completed).

            // Count the threads who start writing in each page.
            startFillingLogBuffer.pageWritersActive.incrementAndGet(startPage);

            nextFreeByteInLogBuffer = updatedNextFreeByteInLogBuffer;

            // lastPageFilling is not volatile but checked first in the attempt to move firstPageFilling 
            // forward below. We have already incremented pageWritersStarted which is volatile and checked 
            // second.
            lastPageFilling = endPage;

            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Object[] { "logBuffer_reserved",
                                          new Long(endFillingLogBuffer.sequenceNumberOfFirstPage),
                                          new Integer(firstPageFilling),
                                          new Integer(lastPageFilling),
                                          new Integer(reservedAddress),
                                          new Integer(startPage),
                                          new Integer(endPage),
                                          new Integer(nextToLastPageNotified),
                                          new Integer(startFillingLogBuffer.pageWritersActive.get(startPage)) });
        } // synchronized (logBufferLock).

        // Lock on the logBuffer is now released, filling the buffer takes place in parallel.     

        // Calculation of BytesInLogBuffer above means we will already allowed for any leading byte of sector bits. 
        // Add a header for the logRecord part followed by the logRecord data. 
        addPart(logRecord,
                startFillingLogBuffer.buffer,
                completed,
                reservedAddress,
                totalBytes);

        if (gatherStatistics) {
            // Time how long we took to complete copying the logRecord. 
            long now = System.currentTimeMillis();
            writeCopyingMilliseconds += now - startWriteMilliseconds;
            startWriteMilliseconds = now;
        } // if (gatherStatistics). 

        // Mark our write as finished and restart the flushHelper if it has stalled.
        boolean startFlush = false;
        for (int iPage = startPage;; iPage++) {
            if (iPage == startFillingLogBuffer.numberOfPages) {
                iPage = 0;
                startFillingLogBuffer = logBuffer;
            } // if (iPage == fillingLogBuffer.numberOfPages).  

            synchronized (startFillingLogBuffer.pageStateLock[iPage]) {
                // We can loop through the logBuffer multiple times if the flushHelper writes pages
                // while we are in this for loop, and enables writer threads to keep advancing the 
                // lastPageFilling. On the first pass through the loop startFlush is false so we use 
                // that to determine if we are on the startPage rather than checking if (iPage == startPage)
                // as that would be true on each successive pass through the pages.
                if (!startFlush)
                    startFillingLogBuffer.pageWritersActive.decrementAndGet(startPage);

                // lastPageFilling may have no data in it as yet. It is never completely full either 
                // as we would move to the next page in that case, so there is never any possibility of it being ready 
                // to write without padding it.
                if (iPage == lastPageFilling)
                    break;

                // pageWritersStarted is no longer incrementing because iPage is positioned before lastPageFilling. 
                if (startFillingLogBuffer.pageWritersActive.get(iPage) == 0
                    && iPage == firstPageFilling) {
                    startFillingLogBuffer.pageFlushPending[iPage] = true;
                    // Advance firstPageFilling in a way that is always valid because it is captured in flush()
                    // above without holding a pageStateLock to protect it.
                    if (firstPageFilling + 1 == startFillingLogBuffer.numberOfPages)
                        firstPageFilling = 0;
                    else
                        firstPageFilling++;

                    startFlush = true;
                } else {
                    break;
                } // if (fillingLogBuffer.pageWriterStarted...
            } // synchronized (fillingLogBuffer.pageStateLock[i]).
        } // for...       

        if (gatherStatistics) {
            // Time how long we took to update the logBuffer state.
            long now = System.currentTimeMillis();
            writeUpdateStateMilliseconds += now - startWriteMilliseconds;
            startWriteMilliseconds = now;
        } // if (gatherStatistics). 

        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
            trace.debug(this,
                        cclass,
                        methodName,
                        new Object[] { "firstPageFilling update done",
                                      new Integer(firstPageFilling),
                                      new Integer(lastPageFilling),
                                      new Integer(startPage),
                                      new Integer(endPage),
                                      new Integer(startFillingLogBuffer.pageWritersActive.get(startPage)),
                                      new Boolean(startFillingLogBuffer.pageFlushPending[startPage]) });
        if (startFlush)
            flushHelper.startFlush();

        // See if there is anything to flush.
        if (flush && completed) {
            endFillingLogBuffer.waitForFlush(endPage);
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Long(returnLogSequenceNumber) });
        return returnLogSequenceNumber;
    } // addBuffers().

    /**
     * Add the partHeader before the logRecord, and then the part of the logRecord.
     * If necessary wrap round the end of the log buffer back to the start.
     * 
     * @param logRecord from which the data is to be copied.
     * @param fillingBuffer the logBuffer to fill. @param boolean true if
     *            the logRecord is completed.
     * @param completed true if this is the last part to add.
     * @param offset in the logBuffer where the part header is to go.
     * @param partLength the length of the part of the LogRecord excluding the partHeader.
     * @return int the offset after addition of the partHeader.
     * 
     * @throws ObjectManagerException
     */
    private int addPart(LogRecord logRecord,
                        byte[] fillingBuffer,
                        boolean completed,
                        int offset,
                        int partLength)
                    throws ObjectManagerException
    {
        final String methodName = "addPart";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { logRecord,
                                      fillingBuffer,
                                      new Boolean(completed),
                                      new Integer(offset),
                                      new Integer(partLength) });

        // Make the part header.
        byte[] partHeader = new byte[partHeaderLength];
        if (completed)
            partHeader[0] = PART_Last;
        else if (logRecord.atStart())
            partHeader[0] = PART_First;
        else
            partHeader[0] = PART_Middle;
        partHeader[1] = logRecord.multiPartID;
        partHeader[2] = (byte) (partLength >>> 8);
        partHeader[3] = (byte) (partLength >>> 0);

        // Place the part header into the logBuffer.
        // Calculate how much of the partHeader will fit in this page. 
        int remainder = pageSize - offset % pageSize;
        int length = Math.min(remainder, partHeaderLength);
        System.arraycopy(partHeader,
                         0,
                         fillingBuffer,
                         offset,
                         length);
        offset = offset + length;
        // If we are now positioned at the first byte in a page, skip over it.
        if (remainder <= partHeaderLength)
            offset++;
        // Have we wrapped the logBuffer?
        if (offset >= fillingBuffer.length) {
            fillingBuffer = logBuffer.buffer;
            offset = 1;
        } // if (offset >= logBuffer.length).

        // Did the whole part header fit in the previous page, was there any overflow? 
        if (length < partHeaderLength) {
            // Copy the piece covering the page boundary one byte further down the logBuffer.
            // We may copy the entire part header.
            System.arraycopy(partHeader,
                             length,
                             fillingBuffer,
                             offset,
                             partHeaderLength - length);
            offset = offset + partHeaderLength - length;
        } // if (length < partHeaderLength).

        // Now copy in the the part of the LogRecord. 
        int bytesToAdd = Math.min(pageSize - (offset % pageSize)
                                  , partLength
                        );
        // Fill one page at a time.
        // If offset leaves us about to write on the first byte in a page then we will write 
        // zero bytes first time round the loop and step over the sector byte. 
        for (;;) {
            // See if we have stepped past the end of the log buffer.
            if (offset >= fillingBuffer.length) {
                fillingBuffer = logBuffer.buffer;
                offset = 1;
            } // if (offset >= logBuffer.length).       

            offset = logRecord.fillBuffer(fillingBuffer
                                          , offset
                                          , bytesToAdd
                            );
            partLength = partLength - bytesToAdd;
            if (partLength == 0)
                break;
            bytesToAdd = Math.min(pageSize - 1
                                  , partLength
                            );
            offset++; // Step past the next sector bits.
        } // for (;;). 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Integer(offset) });
        return offset;
    } // addPart().

    /**
     * @param address of a byte in the log file.
     * @return long the offset of the address from the current start of the file.
     */
    final long offsetFromStart(long address) {
        long offsetFromStart = address - fileLogHeader.startByteAddress;
        if (offsetFromStart <= 0) // If we have not wrapped. 
            offsetFromStart = offsetFromStart + fileLogHeader.fileSize - FileLogHeader.headerLength * 2;
        return offsetFromStart;
    } // offsetFromStart().

    /**
     * Builds a set of properties containing the current statistics.
     * 
     * @return java.util.Map the statistics.
     */
    protected java.util.Map captureStatistics()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "captureStatistics"
                            );

        java.util.Map statistics = super.captureStatistics();

        statistics.put("totalBytesWritten", Long.toString(totalBytesWritten));
        statistics.put("totalNumberOfFlushRequests", Long.toString(totalNumberOfFlushRequests));
        statistics.put("totalNumberOfLogBufferWrites", Long.toString(totalNumberOfLogBufferWrites));
        statistics.put("totalNumberOfThreadsFindingFullLogBuffers", Long.toString(totalNumberOfThreadsFindingFullLogBuffers));
        statistics.put("totalNumberOfFlusHelperWaits", Long.toString(totalNumberOfFlushHelperWaits));
        statistics.put("totalNumberOfLogCycles", Long.toString(totalNumberOfLogCycles));

        statistics.put("writeStalledMilliseconds", Long.toString(writeStalledMilliseconds));
        statistics.put("writeCopyingMilliseconds", Long.toString(writeCopyingMilliseconds));

        statistics.put("flushingMilliseconds", Long.toString(flushingMilliseconds));
        statistics.put("otherMilliseconds", Long.toString(otherMilliseconds));
        statistics.put("flushHelperWaitingMilliseconds", Long.toString(flushHelperWaitingMilliseconds));
        statistics.put("paddingStalledMilliseconds", Long.toString(paddingStalledMilliseconds));
        statistics.put("writeUpdateStatedMilliseconds", Long.toString(writeUpdateStateMilliseconds));

        statistics.put("logFullCheckpointsTriggered", Long.toString(logFullCheckpointsTriggered));
        statistics.put("uncheckedBytes", Long.toString(uncheckedBytes));
        statistics.put("uncheckedBytesUpToTruncatePoint", Long.toString(uncheckedBytesUpToMarkPoint));
        statistics.put("getLogFileSpaceLeft()", Long.toString(getLogFileSpaceLeft()));
        statistics.put("fileLogHeader.fileSize", Long.toString(fileLogHeader.fileSize));
        statistics.put("bufferFilePosition+nextFreeByteInLogBuffer", Long.toString(bufferFilePosition + nextFreeByteInLogBuffer));
        statistics.put("stalledForMultiPartID", Long.toString(stalledForMultiPartID));
        statistics.put("totalPaddingBytesWritten", Long.toString(totalPaddingBytesWritten));
        statistics.put("totalPaddingRecords", Long.toString(totalPaddingRecords));

        String histogram = "(0-15 >15) ";
        for (int n = 0; n < numberOfFlushWaitersFrequency.length; n++) {
            histogram += numberOfFlushWaitersFrequency[n] + " ";
            numberOfFlushWaitersFrequency[n] = 0;
        }
        statistics.put("numberOfFlushWaitersFrequency", histogram);

        histogram = "(0-15 >15) ";
        for (int n = 0; n < numberOfPagesWrittenFrequency.length; n++) {
            histogram += numberOfPagesWrittenFrequency[n] + " ";
            numberOfPagesWrittenFrequency[n] = 0;
        }
        statistics.put("numberOfPagesWritttenFrequency", histogram);

        totalBytesWritten = 0; // Number of bytes written so far.
        totalNumberOfFlushRequests = 0; // Number of times flush() was invoked.
        totalNumberOfLogBufferWrites = 0; // Number of times the logBuffer is has written.
        totalNumberOfThreadsFindingFullLogBuffers = 0;
        totalNumberOfFlushHelperWaits = 0; // Number of times the FlushHelper had nothing to do.
        totalNumberOfLogCycles = 0; // Number of times we have cycled through the log.

        writeStalledMilliseconds = 0;
        writeCopyingMilliseconds = 0;
        flushingMilliseconds = 0;
        otherMilliseconds = 0;
        flushHelperWaitingMilliseconds = 0;
        paddingStalledMilliseconds = 0;
        writeUpdateStateMilliseconds = 0;
        stalledForMultiPartID = 0;
        totalPaddingBytesWritten = 0;
        totalPaddingRecords = 0;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "captureStatistics"
                       , new Object[] { statistics }
                            );
        return statistics;
    } // captureStatistics().

    /**
     * When enabled, writes to the log will throw LogFileFullException.
     * Unless objectManagerState.logFullTriggerCheckpointThreshold is set to 1.0
     * calling simulateLogOutputFull(true); will cause the ObjectManager to continually
     * take checkpoints in order to free up space in the log.
     * 
     * @param isFull true subsequent writes to the log throw LogFileFullException.
     *            if false subsequent writes may succeed.
     * @throws ObjectManagerException
     */
    protected void simulateLogOutputFull(boolean isFull)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "simulateLogOutputFull"
                        , new Object[] { new Boolean(isFull) });

        synchronized (logFullReservedLock) {
            if (isFull) {
                // Clear as much space as we can.
                objectManagerState.waitForCheckpoint(true);

                // Reserve all of the free space in the log.
                int numberOfZeros = 3;
                for (int zeroCount = numberOfZeros; zeroCount > 0;) {
                    long available = 0;
                    synchronized (logBufferLock) {
                        available = getLogFileSpaceLeft();
                        if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                            trace.debug(this,
                                        cclass,
                                        "simulateLogOutputFull",
                                        new Object[] { new Long(available) });
                        if (reserveLogFileSpace(available) == 0) {
                            logFullReserved = logFullReserved + available;
                        } // if (writingFlushSet...
                    } // synchronized (logBufferLock).

                    if (available > 0) {
                        flush();
                        zeroCount = numberOfZeros;
                    } else {
                        // Make sure there are also no unchecked bytes.
                        objectManagerState.waitForCheckpoint(true);
                        // keep going until we have cycled through all of the flush sets.
                        zeroCount--;
                    } // if (available > 0).
                } // for (int zeroCount.

            } else {
                reserveLogFileSpace(-logFullReserved);
                logFullReserved = 0;
                flush();

            } // if (isFull).
        } // synchronized (logFullReservedLock).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "simulateLogOutputFull",
                       new Object[] { new Long(logFullReserved) });
    } // simulateLogOutputFull().

    //----------------------------------------------------------------------------------------------
    // Impements Prinatble
    // ----------------------------------------------------------------------------------------------

    /**
     * Print a dump of the state.
     * 
     * @param printWriter to be written to.
     */
    public void print(java.io.PrintWriter printWriter)
    {
        synchronized (flushHelper) {
            synchronized (notifyHelper) {
                synchronized (logBufferLock) {
                    printWriter.println("State Dump for:" + cclass.getName()
                                        + " logFile=" + logFile + "(java.io.RandomaccessFile)"
                                        + " fileLogHeader=" + fileLogHeader + "(FileLogHeader)"
                                        + "\n filePosition=" + filePosition + "(long)"
                                        + " filePositionSectorByte=" + filePositionSectorByte + "(byte)"
                                        + " bufferFilePosition=" + bufferFilePosition + "(long)"
                                        + " bufferSectorByte=" + bufferSectorByte + "(byte)"
                                        + " fileSpaceLeft=" + fileSpaceLeft + "(long)"
                                    );
                    printWriter.println(" firstPageToFlush=" + firstPageToFlush + "(int)"
                                        + " lastPageNotified=" + lastPageNotified + "(int)"
                                        + " nextPageToNotify=" + nextPageToNotify + "(int)"
                                        + " firstPageFilling=" + firstPageFilling + "(int)"
                                        + " lastPageFilling=" + lastPageFilling + "(int)"
                                        + "\n nextFreeByteInLogBuffer=" + nextFreeByteInLogBuffer + "(int)"
                                        + " newLogBufferPages=" + newLogBufferPages + "(int)"
                                    );
                    printWriter.println();

                    logBuffer.print(printWriter);
                } // synchronized (logBufferLock).
            } // synchronized (notifyHelper).
        } // synchronized (flushHelper).
    } // print().

    // ----------------------------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------------------------

    /**
     * The buffer that contains the log records we will write or are writing
     * and any per page state associated with the buffer.
     * Locking strategy is lock logBufferLock then pageLock, if necessary.
     */
    class LogBuffer {

        int numberOfPages;
        long sequenceNumberOfFirstPage;
        byte[] buffer;
        // Writers that started filling in this page, and are currently actively filling it.
        com.ibm.ws.objectManager.utils.concurrent.atomic.AtomicIntegerArray pageWritersActive;
        // True if a page is ready to write and flush but the write and notification is not yet complete.
        boolean[] pageFlushPending;
        // True if there is a thread waiting for this page to be flushed.
        boolean[] pageWaiterExists;
        // Number of threads waiting their turn to flush each page.
        int[] pageFlushWaiters;
        // Lock to protect the page state, give this its own PageStateLock Object so that we can see why 
        // threads are waiting in dumps more easily.
        PageStateLock[] pageStateLock;

        private class PageStateLock {}

        // Lock to wait for flush completion.
        PageWaitLock[] pageWaitLock;

        private class PageWaitLock {}

        /**
         * Allocate a new logBuffer for filling, threads looking at an existing logBuffer switch to the
         * new one as they wrap back to the beginning of the buffer.
         * 
         * @param numberOfPages the number of pages in the new LogBuffer.
         */
        private LogBuffer(int numberOfPages) {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>",
                            new Object[] { new Integer(numberOfPages) });

            this.numberOfPages = numberOfPages;
            buffer = new byte[pageSize * numberOfPages];

            pageWritersActive = new com.ibm.ws.objectManager.utils.concurrent.atomic.AtomicIntegerArrayImpl(numberOfPages);
            pageFlushPending = new boolean[numberOfPages];
            pageWaiterExists = new boolean[numberOfPages];
            if (gatherStatistics)
                pageFlushWaiters = new int[numberOfPages];
            pageStateLock = new PageStateLock[numberOfPages];
            pageWaitLock = new PageWaitLock[numberOfPages];
            for (int iPage = 0; iPage < numberOfPages; iPage++) {
                pageWritersActive.set(iPage, 0);
                pageFlushPending[iPage] = false;
                pageWaiterExists[iPage] = false;
                if (gatherStatistics)
                    pageFlushWaiters[iPage] = 0;
                pageStateLock[iPage] = new PageStateLock();
                pageWaitLock[iPage] = new PageWaitLock();
            } // for...

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>");
        } // LogBuffer().

        /**
         * Reset when writers wrap round back to the beginning of the buffer.
         * Callers must be synchronized on logBufferLock.
         * 
         * @throws ObjectManagerException if calculatePaddingSpaceTarget throws the exception.
         *             It is not expected that it will because the number of log buffer pages only grows
         *             and never shrinks. A larger log buffer requires fewer padding pages, so the call
         *             to calculatePaddingSpaceTarget should unreserve space (no exception) rather than
         *             reserve space (can throw and exception).
         */
        private void wrapped() throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "wrapped",
                            new Object[] { new Integer(newLogBufferPages),
                                          new Long(sequenceNumberOfFirstPage),
                                          new Long(bufferFilePosition) });

            // Move the sequence number forward.
            if (newLogBufferPages != 0) {
                // We need a different size logBuffer, after we abandon this one?
                logBuffer = new LogBuffer(newLogBufferPages);
                logBuffer.sequenceNumberOfFirstPage = sequenceNumberOfFirstPage + numberOfPages;
                // We cannot clear the request to change the log buffer size until the current one has been 
                // notified completely, otherwise a filling thread could request second new buffer before
                // the notify thread has seen the first new one. So no newLogBufferPages = 0;

                // calculate the new padding space required
                calculatePaddingSpaceTarget();
            } else {
                sequenceNumberOfFirstPage = sequenceNumberOfFirstPage + numberOfPages;
            } // if (newLogBufferPages != 0).

            // Move the file position cursor forward.
            bufferFilePosition = bufferFilePosition + buffer.length;
            // Did this buffer wrap the file?
            if (bufferFilePosition >= fileLogHeader.fileSize) {
                // Flip the sector byte.
                bufferSectorByte = (bufferSectorByte == 0 ? (byte) 1 : (byte) 0);
                bufferFilePosition = bufferFilePosition - fileLogHeader.fileSize + FileLogHeader.headerLength * 2;
            } // if (bufferFilePosition > fileLogHeader.fileSize).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "wrapped",
                           new Object[] { new Long(sequenceNumberOfFirstPage),
                                         new Long(bufferFilePosition) });
        } // wrapped().

        /**
         * Set the sector bits add write part of the logBuffer to the logFile.
         * 
         * @param startPage in the logBuffer where we write from.
         * @param endPage the first page in the logBuffer that we do not write, may be equal to the startPage
         *            in which case we don't write anything.
         * @throws ObjectManagerException
         **/
        private void write(int startPage,
                           int endPage)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "write",
                            new Object[] { new Integer(startPage),
                                          new Integer(endPage) });

            if (startPage != endPage) {
                // Now that all writer threads are completed we can go back and set the sector bits for
                // the pages to be written.
                byte[] bufferToSet = buffer;
                for (int iPage = startPage;; iPage++) {
                    if (iPage == numberOfPages) {
                        iPage = 0;
                        bufferToSet = logBuffer.buffer;
                    } // if (iPage == numberOfPages).  
                    if (iPage == endPage)
                        break;
                    setSectorBits(bufferToSet,
                                  iPage * pageSize,
                                  filePositionSectorByte);
                } // for (int iPage = startPage...

                // Write and force the bytes in the logBuffer.
                try {

                    /*
                     * // Simulate a write.
                     * try {
                     * this.wait(1);
                     * } catch (InterruptedException exception) {
                     * // No FFDC Code Needed.
                     * FFDC.processException(this,cclass,"flush",exception,"1:2209:1.52");
                     * 
                     * if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                     * trace.event(this,cclass
                     * ,"writeLogBuffer"
                     * ,exception
                     * );
                     * } // catch (InterruptedException exception).
                     * // End of Simulate a write.
                     */

                    // For ServeRAID 6M disks using raid zero it turns out to be faster to write one page at a time because
                    // they have an optimisation that disables caching if more than one page is written at raid zero!
                    // Use Raid 1E to disable this optimisation.
                    /*
                     * ServerRAID
                     * for (int iPage = startPage;; iPage++) {
                     * if (iPage == numberOfPages)
                     * iPage = 0;
                     * if (iPage == endPage)
                     * break;
                     * logFile.write(buffer,
                     * iPage * pageSize,
                     * pageSize);
                     * filePosition = filePosition + pageSize;
                     * } // for (int iPage = startPage...
                     * // End of ServerRAID.
                     */

                    // See if the buffer is wrapped.
                    if (endPage > startPage) {
                        logFile.write(buffer,
                                      startPage * pageSize,
                                      (endPage - startPage) * pageSize);
                        filePosition = filePosition + (endPage - startPage) * pageSize;
                    } else {
                        logFile.write(buffer,
                                      startPage * pageSize,
                                      (numberOfPages - startPage) * pageSize);
                        // The log buffer may have wrapped into a new bigger one.
                        logFile.write(logBuffer.buffer,
                                      0,
                                      endPage * pageSize);
                        filePosition = filePosition + (numberOfPages - startPage + endPage) * pageSize;
                    } // if (endPage > startPage).

                } catch (java.io.IOException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        "writeLogBuffer",
                                                        exception,
                                                        "1:2258:1.52");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   "writeLogBuffer",
                                   exception);
                    throw new PermanentIOException(this,
                                                   exception);
                } // catch.

            } // if startPage != endPage).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "write",
                           new Object[] { new Long(filePosition) });
        } // write(). 

        /**
         * Wait for a page to flush.
         * 
         * @param page to wait for flush.
         * @throws ObjectManagerException
         */
        private final void waitForFlush(int page)
                        throws ObjectManagerException {
            final String methodName = "waitForFlush";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName,
                            new Object[] { new Integer(page) });

            if (gatherStatistics)
                totalNumberOfFlushRequests++;

            synchronized (pageWaitLock[page]) {

                if (pageWaiterExists[page]) {

                    if (gatherStatistics)
                        pageFlushWaiters[page]++;

                    // If we are about to wait and the endPage is in fact the only page filling then
                    // we need to wake the flushHelper in case it has stalled also waiting for this page.
                    // We might also have got here after our pageWaierExists flag was cleared and another
                    // thread set it on the next wrap of the log buffer.          
                    if (page == firstPageFilling)
                        flushHelper.startFlush();

                    // Now wait until the flushHelper runs the flush and then notifies us that it is complete.
                    if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                        trace.debug(this,
                                    cclass,
                                    methodName,
                                    new Object[] { "wait:2314",
                                                  new Integer(pageWritersActive.get(page)) });

                    // Repeat attempts to wait, in case we are interrupted by thread.interrupt().
                    while (flushHelper.abnormalTerminationException == null) {
                        try {
                            pageWaitLock[page].wait();
                            break;

                        } catch (InterruptedException exception) {
                            // No FFDC Code Needed.
                            ObjectManager.ffdc.processException(this,
                                                                cclass,
                                                                methodName,
                                                                exception,
                                                                "1:2329:1.52");

                            if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                                trace.event(this,
                                            cclass,
                                            "addBuffers",
                                            exception);
                        } // catch (InterruptedException exception).
                    } // while (flushHelper.abnormalTerminationException == null).
                } // if (pageWaiterExists[endPage])
            } // synchronized (pageWaitLock[endPage]).

            if (flushHelper.abnormalTerminationException != null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               methodName,
                               new Object[] { "via UnexpectedExceptionException",
                                             flushHelper.abnormalTerminationException });
                throw new UnexpectedExceptionException(this,
                                                       flushHelper.abnormalTerminationException);
            } // if (flushHelper.abnormalTerminationException...

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { new Long(filePosition) });
        } // waitForFlush().

        /**
         * Print a dump of the state.
         * 
         * @param printWriter to be written to.
         */
        private void print(java.io.PrintWriter printWriter)
        {
            printWriter.println("LogBuffer"
                                + " numberOfPages=" + numberOfPages + "(int)"
                                + " sequenceNumberOfFirstPage=" + sequenceNumberOfFirstPage + "(long)"
                                + " buffer.length=" + buffer.length + "(int)"
                            );
            printWriter.println();

            printWriter.println("page\tWriters\tFlush\tWaiter\tFlush");
            printWriter.println(" \tActive\tPending\tExists\tWaiters");
            for (int i = 0; i < numberOfPages; i++) {
                printWriter.println(i + "\t" + pageWritersActive.get(i) + "\t" + pageFlushPending[i] + "\t" + pageWaiterExists[i] + "\t" + pageFlushWaiters[i]);
            } // for pages...

        } // print().  

        /**
         * Get the value of pageFlushPending for the given page by first taking the
         * appropriate lock. This ensures that threads delivering work into
         * LogBuffers (addBuffers) complete the update of pageFlushPending array AND
         * shuffle the cursor along atomically, relative to threads calling this method.
         * i.e. the FlushHelper.run() method - see d661770.
         * 
         * @param pageToCheck
         * @return boolean page flush pending for given page
         */
        protected boolean getPageFlushPending(int pageToCheck)
        {
            String methodName = "getPageFlushPending";
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.entry(this, cclass, methodName, new Integer(pageToCheck));

            boolean pending = false;
            synchronized (pageStateLock[pageToCheck])
            {
                pending = pageFlushPending[pageToCheck];
            }
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.exit(this, cclass, methodName, new Boolean(pending));
            return pending;
        }

    } // innner class LogBuffer.

    /**
     * The worker thread that executes the writing and flushing of Log data to disk.
     */
    protected class FlushHelper
                    implements Runnable
    {
        // False if we are to shut down.
        private boolean running = true;
        // True if we are currently waiting for a new request.            
        private boolean waiting = false;
        // True if we are actively flushing and will check the logBufferStatus when we are done.
        // If false we are potentially waiting and may need to be notify()'d.
        private volatile boolean flushActive = false;
        // The flushHelper thread.
        Thread flushThread = null;
        // If we terminate abnormally this is the exception that caused the abnormal termination.
        private Exception abnormalTerminationException = null;
        // A reference to the logBuffer being flushed, usually the same as the one being filled unless
        // we have recently resized the logBuffer.
        LogBuffer flushLogBuffer = logBuffer;

        /**
         * Constructor, makes a thread to run flush.
         */
        FlushHelper()
        {
            final String methodName = "<init>";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName);

            flushThread = new Thread(this);
            flushThread.setName("FlushHelper");
            flushThread.setPriority(helperThreadPriority);
            flushThread.start();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName);
        } // FlushHelper().      

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         * 
         * Keep flushing the completed pages in the log buffer. Only wait if the first page in the
         * logBbuffer is incomplete and there are no threads waiting for it to be flushed.
         */
        public void run()
        {
            final String methodName = "run";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName);

            if (gatherStatistics) {
                // Initialise the clock.
                lastFlushMilliseconds = System.currentTimeMillis();
            } // if (gatherStatistics).

            // Outer try/catch block treats all unexpected errors as terminal
            // and shuts down the ObjectManager if any occur.
            try {
                // Keep then flushing nextFlushSet.
                flushLoop: while (running) {
                    if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                        trace.debug(this,
                                    cclass,
                                    methodName,
                                    new Object[] { "flushLoop:2478",
                                                  new Integer(firstPageToFlush),
                                                  new Integer(firstPageFilling),
                                                  new Integer(lastPageFilling) });

                    // Show that we might be waiting and a request to flush should assume that it needs to 
                    // check whether we need to be woken.
                    flushActive = false;

                    // ------------------------------------------------------------------------------------
                    // See if a new request has turned up, or if we have caught up with the writers and
                    // will have to wait. If there are already waiters for the page we bypass the wait.
                    // We are notified if a page becomes complete or if a thread starts waiting for a page
                    // to be flushed. 
                    // We should wait if allPagesUsed is true, otherwise we would spin writing zero pages
                    // until the notifyHelper has allowed firstPageFilling to move forward.
                    // ------------------------------------------------------------------------------------
                    synchronized (this) {
                        waiting = true;
                        // Do not wait if the first page already needs flushing, or there is a thread waiting 
                        // for the only (first) page.
                        // We may have been woken by a thread finding the log buffer full and the 
                        // notify thread has not yet advanced nextPageToNotify, so we must keep checking 
                        // and waiting until there is something to flush.
                        while (!flushLogBuffer.getPageFlushPending(firstPageToFlush) // No page ready to flush.
                               && !(flushLogBuffer.pageWaiterExists[firstPageToFlush] // No waiter...
                                    && (firstPageToFlush == lastPageFilling) // in the only page...                         
                               && (firstPageToFlush == nextPageToNotify)) // with no notifyHelper backlog.                   
                        ) {
                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this,
                                            cclass,
                                            methodName,
                                            new Object[] { "flushLoop:2511 About to wait",
                                                          new Integer(firstPageToFlush),
                                                          new Integer(firstPageFilling),
                                                          new Integer(lastPageFilling),
                                                          new Integer(nextPageToNotify),
                                                          new Boolean(flushLogBuffer.pageWaiterExists[firstPageToFlush]),
                                                          new Boolean(flushLogBuffer.pageFlushPending[firstPageToFlush]) });
                            // InterruptedException caught by outer try/catch block.
                            // before we wait, and now we have the lock, check we are still running
                            if (!running)
                                break flushLoop;

                            wait();

                            if (gatherStatistics) {
                                totalNumberOfFlushHelperWaits++;
                                long now = System.currentTimeMillis();
                                flushHelperWaitingMilliseconds += now - lastFlushMilliseconds;
                                lastFlushMilliseconds = now;
                            } // if (gatherStatistics).

                            // See if we were stopped while we were in a wait state, if we are not running the chances are
                            // that the notifyHelper is not running either.
                            if (!running)
                                break flushLoop;

                        } // while (!flushLogBuffer.pageFlushPending[firstPageToFlush])...

                        waiting = false;
                    } // synchronized (this).

                    if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                        trace.debug(this,
                                    cclass,
                                    methodName,
                                    new Object[] { "flushLoop:2546 woken",
                                                  new Integer(firstPageToFlush),
                                                  new Integer(firstPageFilling),
                                                  new Integer(lastPageFilling),
                                                  new Boolean(flushLogBuffer.pageWaiterExists[firstPageToFlush]),
                                                  new Boolean(flushLogBuffer.pageFlushPending[firstPageToFlush]) });

                    // Show we are active and will check to see if more flushing is required before
                    // we wait again.
                    flushActive = true;

                    // Avoid stalling if the last page is partially filled. This page must also have
                    // a waiter to bring us past the wait above, otherwise it would have flushPending
                    // set because the page is now full.
                    if (!flushLogBuffer.getPageFlushPending(firstPageToFlush) && firstPageToFlush == lastPageFilling) {
                        // If there is nothing in the last page then padding it won't move us forward to the next page.
                        // Any padding comes from the previously reserved set of pages obtained what a transaction started.
                        padLogBuffer();
                    } // if ( !flushLogBuffer.pageFlushPending[firstPageToFlush]...

                    // ---------------------------------------------------------------------------------------
                    // Writers are no longer writing to the pages we are about to flush so no need to lock
                    // the logBuffer.
                    // ---------------------------------------------------------------------------------------

                    // We need to perform the flush even of there is nothing to write because we may need
                    // to truncate the log file.
                    // Take a copy of firstPageFilling in case it is advanced between performing the flush
                    // and requesting the notify.
                    int copyOfFirstPageFilling = firstPageFilling;
                    int copyOfFirstPageToFlush = firstPageToFlush;
                    performFlush(copyOfFirstPageFilling);

                    if (gatherStatistics) {
                        long now = System.currentTimeMillis();
                        flushingMilliseconds += now - lastFlushMilliseconds;
                        lastFlushMilliseconds = now;
                    } // if (gatherStatistics).

                    // Release threads waiting for the flush.
                    // ObjectManagerException caught by outer try/catch block.
                    // Notify pages only if any were flushed, otherwise we will loop over all pages.
                    if (firstPageToFlush != copyOfFirstPageToFlush)
                        notifyHelper.doNotifyAll(copyOfFirstPageFilling);

                    if (gatherStatistics) {
                        long now = System.currentTimeMillis();
                        otherMilliseconds += now - lastFlushMilliseconds;
                        lastFlushMilliseconds = now;
                    } // if (gatherStatistics).

                    // ObjectManagerException caught by outer try/catch block.
                } // flushLoop: while (running).

            } catch (Exception exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    methodName,
                                                    exception,
                                                    "1:2606:1.52");
                if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                    trace.event(this,
                                cclass,
                                methodName,
                                exception);

                running = false;
                abnormalTerminationException = exception;
                // Make one asynchronous request to shutdown the ObjectManager, this has no effect
                // if we are already shutting down.
                objectManagerState.requestShutdown();

                try {
                    if (objectManagerState.inShutdown())
                    {
                        // We have failed during shutdown so we need to notify any threads waiting 
                        // for a flush to complete, they will now see the abnormalTerminationException. 
                        // and should assume that the flush has failed.
                        notifyHelper.doNotifyAll(firstPageFilling);
                    }

                } catch (ObjectManagerException e) {
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        methodName,
                                                        e,
                                                        "1:2633:1.52");
                    if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                        trace.event(this, cclass, methodName, e);
                }
            } // catch (ObjectManagerException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // run().

        /**
         * Write up to but not including the first page that is still being filled in the log buffer.
         * 
         * @throws ObjectManagerException
         */
        void startFlush()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "startFlush");

            // Has something previously gone wrong?
            if (abnormalTerminationException != null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "startFlush",
                               new Object[] { "via UnexpectedExceptionException",
                                             abnormalTerminationException });
                throw new UnexpectedExceptionException(this,
                                                       abnormalTerminationException);
            } // if (abnormalTerminationException != null).

            if (!running) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "startFlush",
                               new Object[] { "via ThreadNotRunningException" });
                throw new ThreadNotRunningException(this,
                                                    flushThread.getName(),
                                                    "startFlush");
            } // if (!running).

            // If the flushHelper is currently in the middle of flushing some pages in the buffer it will
            // check whether there are more pages once its finished. We don't need to synchronize and
            // notify it.
            if (!flushActive) {
                synchronized (this) {
                    if (waiting)
                        notify();
                } // synchronized(this).
            } // if (!flushActive).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "startFlush");
        } // startFlush().

        private void shutdown()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "shutdown");

            synchronized (this) {
                running = false;
                if (waiting)
                    notify();
            } // synchronize (this).

            // Wait for the worker thread to complete.
            try {
                flushThread.join();
            } catch (InterruptedException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "shutdown", exception, "1:2714:1.52");
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "shutdown"
                               , exception
                                    );
                throw new UnexpectedExceptionException(this,
                                                       exception);
            } // catch (InterruptedException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "shutdown");
        } // shutdown().

        /**
         * Complete the last page in the logBuffer by putting paddingBytes or
         * a paddingLogRecord in the free space. The padding space is not checked
         * and must have been previously reserved, for example when the transaction
         * started it reserved one spare page, in case padding is needed for the flush.
         * The padding record is written, even if the page contains no data.
         * 
         * @throws ObjectManagerException
         */
        private void padLogBuffer()
                        throws ObjectManagerException
        {
            final String methodName = "padLogBuffer";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName);

            // Pad the data to a whole number of pages to avoid the need for the I/O subsystem
            // to read the last page from disk before writing it. This also removes any risk of the
            // last page of the previous log force being damaged if the write the new first page
            // fails to complete correctly because it will not be overwriting any existing data.
            // It is not a good idea to try to defer writing the last partial page to the next
            // flush because this involves the flushHelper taking a lock on the logBuffer.
            synchronized (logBufferLock) {
                if (gatherStatistics) {
                    long now = System.currentTimeMillis();
                    paddingStalledMilliseconds += now - lastFlushMilliseconds;
                    lastFlushMilliseconds = now;
                } // if (gatherStatistics).

                // We have locked the logBuffer so check that we still need to pad the first page, 
                // assuming there is no notifyHelper backlog.
                // Currently no need to use getPageFlushPending() but do in case this changes/best practice
                if (!flushLogBuffer.getPageFlushPending(firstPageToFlush)
                    && firstPageToFlush == lastPageFilling
                    && firstPageToFlush == nextPageToNotify) {
                    int padding = pageSize - (nextFreeByteInLogBuffer % pageSize);

                    totalPaddingBytesWritten = totalPaddingBytesWritten + padding;
                    totalPaddingRecords++;

                    paddingReserveLogSpace(padding);

                    // Is it larger than the minimum size padding record.
                    int paddingLogRecordSize = padding - partHeaderLength;
                    // Can we use a PaddingLogRecord?
                    if (paddingLogRecordSize < PaddingLogRecord.overhead) {
                        // Fill with PART_Padding bytes.
                        java.util.Arrays.fill(flushLogBuffer.buffer,
                                              nextFreeByteInLogBuffer,
                                              nextFreeByteInLogBuffer + padding,
                                              PART_Padding);
                        nextFreeByteInLogBuffer = nextFreeByteInLogBuffer + padding;
                    } else { // Use a PaddingLogRecord.
                        PaddingLogRecord paddingLogRecord = new PaddingLogRecord(paddingLogRecordSize);
                        nextFreeByteInLogBuffer = addPart(paddingLogRecord,
                                                          flushLogBuffer.buffer,
                                                          true,
                                                          nextFreeByteInLogBuffer,
                                                          paddingLogRecordSize);
                    } // if (paddingLogRecordSize < PaddingLogRecord.overhead).

                    // Step over the sector byte we are now positioned at.
                    nextFreeByteInLogBuffer++;
                    // See if we have wrapped around the end of the logBuffer.
                    if (nextFreeByteInLogBuffer >= flushLogBuffer.buffer.length) {
                        flushLogBuffer.wrapped();
                        nextFreeByteInLogBuffer = nextFreeByteInLogBuffer - flushLogBuffer.buffer.length;
                    } // if (nextFreeByteInLogBuffer >= flushLogBuffer.length).

                    // We only pad the logBuffer if a single page is being filled so there is never a possibility of
                    // using all of the logBuffer and making all of the pages full and setting 
                    // lastPageFilling == lastPageNotified.
                    lastPageFilling = nextFreeByteInLogBuffer / pageSize;

                    // Advance the firstPageFilling.
                    //TODO Why does this synchronize block have to be inside logBufferLock? It is not in addBuffers().
                    synchronized (flushLogBuffer.pageStateLock[firstPageToFlush]) {
                        if (flushLogBuffer.pageWritersActive.get(firstPageToFlush) == 0
                            && firstPageToFlush == firstPageFilling) {
                            flushLogBuffer.pageFlushPending[firstPageToFlush] = true;
                            // Advance firstPageFilling in a way that is always valid because it is captured in flush()
                            // above without holding a pageStateLock to protect it.
                            if (firstPageFilling + 1 == flushLogBuffer.numberOfPages)
                                firstPageFilling = 0;
                            else
                                firstPageFilling++;

                        } //  if (   flushLogBuffer.pageWritersStarted[firstPageToFlush]...
                    } // synchronized (flushLogBuffer.pageStateLock[firstPageToFlush]).
                } // if (!flushLogBuffer.pageFlushPending[firstPageToFlush]...
            } // synchronized (logBufferLock).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { new Integer(firstPageToFlush),
                                         new Integer(firstPageFilling),
                                         new Integer(lastPageFilling),
                                         new Integer(flushLogBuffer.pageWritersActive.get(firstPageToFlush)) });
        } // padLogBuffer().

        /**
         * Force a set of filled pages in the logBuffer to disk. This is called even if there are is no data to force.
         * This is so that we recover log file space allocated to the flush set.
         * The flushHelper thread is single threaded and calls performFlush or getNextFlushSet,
         * but never both at the same time so there is no need to synchronize for resources used
         * exclusively within these two methods.
         * 
         * @param flushFirstPageFilling The first page after firstPageTowrite that is still filling, and which must
         *            not be written.
         * 
         *            If truncateLogFile is true, rewrite the log header with the new start point.
         * @throws ObjectManagerException
         */
        private final void performFlush(int flushFirstPageFilling)
                        throws ObjectManagerException

        {
            final String methodName = "performFlush";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName,
                            new Object[] { new Integer(firstPageToFlush),
                                          new Integer(flushFirstPageFilling),
                                          new Long(filePosition) });

            // Work out how many pages we will write, this may be zero and the
            // pages may wrap round the end of the log buffer.
            int pagesToWrite = flushFirstPageFilling - firstPageToFlush;
            if (pagesToWrite < 0)
                pagesToWrite = pagesToWrite + flushLogBuffer.numberOfPages; // there is a bug here - it should be +1
            // The net effect is that only stats are wrong and we cycleLog on this flush
            // after doing the first write, and then write nothing.  This is OK so left as is.

            int bytesToWrite = pagesToWrite * pageSize;
            if (gatherStatistics) {
                totalNumberOfLogBufferWrites++;
                totalBytesWritten = totalBytesWritten + bytesToWrite;
                // Bins count 0,1,2,3,4,5,6,7,>7.
                if ((pagesToWrite & 0x7FFFFFF0) != 0)
                    numberOfPagesWrittenFrequency[16]++;
                else
                    numberOfPagesWrittenFrequency[pagesToWrite & 0x0000000F]++;
            } // if (gatherStatistics).

            // Determine whether we should cycle the log during this write.
            // See if we are near to the end of the log file. If this logBuffer will
            // take us past the end of the file we cycle back to the beginning of the file.

            // Don't be fooled by the file size reported on Windows Explorer,
            // have a look at the actual bytes of data reported under file properties.
            // Also remember that we don't update the file meta data while the
            // ObjectManager is running.

            // Assume we don't need to cycle the log file.
            boolean cycleLog = false;
            int endPage = flushFirstPageFilling;
            if (fileLogHeader.fileSize - filePosition <= bytesToWrite) {
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                methodName,
                                new Object[] { "cycle", new Byte(filePositionSectorByte) });
                // Fill the logFile.
                bytesToWrite = (int) (fileLogHeader.fileSize - filePosition);
                endPage = firstPageToFlush + bytesToWrite / pageSize;
                if (endPage >= flushLogBuffer.numberOfPages)
                    endPage = endPage - flushLogBuffer.numberOfPages;
                // We will do actual truncation after the write to the log and then complete the write.
                cycleLog = true;
            } // if (fileLogHeader.fileSize - filePosition <= bytesToWrite).

            doWriteAndBufferCycle(firstPageToFlush, endPage);

            // Did we decide to cycle the log?
            if (cycleLog) {
                // To differentiate from last cycle, switch sector bytes.
                filePositionSectorByte = (filePositionSectorByte == 0 ? (byte) 1 : (byte) 0);

                if (gatherStatistics) {
                    totalNumberOfLogCycles++;
                } // if (gatherStatistics).

                // Reposition the log to continue at the start.
                filePosition = FileLogHeader.headerLength * 2;
                try {
                    logFile.seek(filePosition);

                } catch (java.io.IOException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        methodName,
                                                        exception,
                                                        "1:2924:1.52");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   methodName,
                                   exception);
                    throw new PermanentIOException(this,
                                                   exception);
                } // catch.
                  // Write the remainder of the buffer.
                doWriteAndBufferCycle(endPage, flushFirstPageFilling);
            } // if (cycleLog).

            // Should we truncate the log?
            if (truncateRequested) {
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                methodName,
                                new Object[] { "truncate", new Long(fileMark), new Byte(fileMarkSectorByte) });
                // Calculate the amount of space we will free up by truncating the log file.
                long fileSpaceRegained = offsetFromStart(fileMark) - 1;
                // Now rewrite the log header with the new start point in the log.
                fileLogHeader.startByteAddress = fileMark;
                // Set the sector byte to start reading with.
                // We must never complete a whole cycle of a circular log without truncating
                // and re-establishing a new start point because this would imply we have overwritten
                // log records. If we restart reading the log use the sector byte for the mark point.
                fileLogHeader.sectorByte = fileMarkSectorByte;

                fileLogHeader.writeHeader(logFile);
                try {
                    logFile.seek(filePosition);

                } catch (java.io.IOException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this,
                                                        cclass,
                                                        methodName,
                                                        exception,
                                                        "1:2965:1.52");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this,
                                   cclass,
                                   methodName,
                                   exception);
                    throw new PermanentIOException(this,
                                                   exception);
                } // catch.

                // Add in the new space recovered by truncating the file less the unchecked bytes that were not 
                // reserved in the first place and the sector bytes that were reserved up front. 
                paddingReserveLogSpace(+uncheckedBytesUpToMarkPoint - fileSpaceRegained + fileSpaceRegained / pageSize);
                uncheckedBytesUpToMarkPoint = 0;

                if (newFileSizeRequested)
                    checkNewFileSizeRequest();

                truncateRequested = false;
            } // if (truncateRequested).

            // Force the disk is we are on a system that did not open the log file in mode "rwd".
            if (!objectManagerState.nioAvailable) {
                try {
                    logFile.getFD().sync();

                } catch (java.io.IOException exception) {
                    // No FFDC Code Needed.
                    ObjectManager.ffdc.processException(this, cclass, methodName, exception, "1:2994:1.52");

                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                        trace.exit(this, cclass,
                                   methodName
                                   , exception);
                    throw new PermanentIOException(this,
                                                   exception);
                } // catch.
            } // if (!objectManagerState.nioAvailable).

            // Advance the pointer to the next page we will write.
            firstPageToFlush = flushFirstPageFilling;

            // Now we have written the logBuffer calculate how much space is left in the logFile.
            setFileSpaceLeft();
            // See if a checkpoint is needed.
            checkOccupancy();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName,
                           new Object[] { new Integer(firstPageToFlush),
                                         new Long(filePosition) });
        } // performFlush().

        /**
         * Call flushLogBuffer.write and check if we need to move on to a new
         * improved logBuffer.
         * 
         * @param start index
         * @param end index
         * @throws ObjectManagerException
         */
        private void doWriteAndBufferCycle(int start, int end) throws ObjectManagerException
        {
            flushLogBuffer.write(start, end);

            // If we have wrapped round the logBuffer
            // capture the current filling LogBuffer in case it has been resized.
            if (end < start)
            {
                flushLogBuffer = logBuffer;
            }
        }

        /**
         * Try to honour a request to change the log file size.
         * This is single threaded on the flushHelper thread.
         * 
         * @throws ObjectManagerException
         **/
        private void checkNewFileSizeRequest()
                        throws ObjectManagerException
        {
            final String methodName = "checkNewFileSizeRequest";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName,
                            new Object[] { new Long(newFileSize),
                                          new Long(filePosition),
                                          new Long(fileLogHeader.fileSize),
                                          new Byte(fileLogHeader.sectorByte) });

            // TODO We have to hold up writes and flushers to do this so...
            // TODO This will disrupt the operation of the log as we have to format the whole file, may be we could just reserve
            // TODO the space in the file system then format it as we go?
            // TODO Format a bit more of the file with alternate sector bits and advance FileLogHeader.filesize. 
            // TODO Be careful though if multiple pages are written
            // TODO the one with the inverse bad sector bit must be written before the FileLogHeader.filesize.  

            // Need to stop writers while we do this in case they wrap their file position based on the old size.
            synchronized (logBufferLock) {
                // We must not have wrapped round the end of the file to either lengthen or shorten it.
                // If we lengthened it we would have a gap at the end of the file. If we shorten it 
                // we would loose the data at the end of the file.
                if (fileLogHeader.startByteAddress < filePosition) {

                    // Shorten the file?
                    if (newFileSize < fileLogHeader.fileSize) {
                        // We can shorten the file if the start point is within the shortened file.     
                        if (fileLogHeader.startByteAddress < newFileSize) {
                            // See if there is enough space left after we satisfy the request and remove it 
                            // from the available space if there is. Also correct the available space for 
                            // the fewer sector bytes we need.
                            if (reserveLogFileSpace(-newFileSize + fileLogHeader.fileSize + newFileSize / pageSize - fileLogHeader.fileSize / pageSize) == 0) {
                                // Clear the request.
                                newFileSizeRequested = false;

                                // First write the new shorter length in the header.
                                // If we fail before the file is shortened we will waste the space after the
                                // new length but at least we will have all of the space we need, we just won't
                                // have released what we should have. This will be corrected at restart.
                                fileLogHeader.fileSize = newFileSize;
                                fileLogHeader.writeHeader(logFile);
                                calculatePaddingSpaceTarget();

                                // Release the space in the file system.
                                try {
                                    logFile.seek(filePosition);
                                    logFile.setLength(newFileSize);

                                } catch (java.io.IOException exception) {
                                    // No FFDC Code Needed.
                                    ObjectManager.ffdc.processException(this,
                                                                        cclass,
                                                                        methodName,
                                                                        exception,
                                                                        "1:3102:1.52");

                                    if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                        trace.exit(this,
                                                   cclass,
                                                   methodName,
                                                   exception);
                                    throw new PermanentIOException(this,
                                                                   exception);
                                } // catch (java.io.IOException...
                            } // if (reserve...
                        } // if ( fileLogHeader.startByteAddress < newFileSize).

                    } else {
                        // Lengthen the physical file by padding with the opposite sector bytes to what 
                        // we are currently writing.

                        // Clear the request.
                        newFileSizeRequested = false;

                        // Get a buffer of new pages to write.
                        byte[] newPages = new byte[(int) Math.min(pageSize * batchSizeOfNewPagesToFormat, fileLogHeader.fileSize)];
                        // Set all sector bits to zero, the initialisation of FileLogHeader assumes this has been done.
                        for (int istart = 0; istart < newPages.length; istart = istart + pageSize) {
                            if (fileLogHeader.sectorByte == 0)
                                setSectorBits(newPages, istart, (byte) 1);
                            else
                                setSectorBits(newPages, istart, (byte) 0);
                        } // for (int istart = 0...

                        // Reserve the full amount of the file but do not set logHeader.FileSize yet.
                        try {
                            logFile.setLength(newFileSize);
                            logFile.seek(fileLogHeader.fileSize);
                            for (long istart = fileLogHeader.fileSize; istart < newFileSize; istart = istart + newPages.length)
                                logFile.write(newPages,
                                              0,
                                              (int) Math.min(newPages.length,
                                                             newFileSize - istart));

                        } catch (java.io.IOException exception) {
                            // No FFDC Code Needed.
                            ObjectManager.ffdc.processException(this,
                                                                cclass,
                                                                methodName,
                                                                exception,
                                                                "1:3152:1.52");

                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           methodName,
                                           exception);
                            newFileSizeException = new PermanentIOException(this,
                                                                            exception);
                            return;
                        } // catch (java.io.IOException...

                        // Allow writers to use the new space, they will not be able to flush yet because we are running on
                        // the flushHelper thread. Also correct the available space for the extra sector bytes we need.
                        reserveLogFileSpace(-newFileSize + fileLogHeader.fileSize + newFileSize / pageSize - fileLogHeader.fileSize / pageSize);

                        // If we fail before the header is written the file will be longer than the header
                        // claims so we will waste some file space until restart when we correct this.
                        fileLogHeader.fileSize = newFileSize;
                        fileLogHeader.writeHeader(logFile);
                        calculatePaddingSpaceTarget();

                        try {
                            logFile.seek(filePosition);

                        } catch (java.io.IOException exception) {
                            // No FFDC Code Needed.
                            ObjectManager.ffdc.processException(this,
                                                                cclass,
                                                                methodName,
                                                                exception,
                                                                "1:3183:1.52");

                            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                                trace.exit(this,
                                           cclass,
                                           methodName,
                                           exception);
                            throw new PermanentIOException(this,
                                                           exception);
                        } // catch.
                    } // if( newFileSize < fileLogHeader.fileSize).
                } // if (fileLogHeader.startByteAddress < filePosition).
            } // synchronized (logBufferLock).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           methodName);
        } // checkNewFileSizerequest().

        /**
         * Checks the occupancy of the log. If the fraction of logSpace used
         * is too great request a checkpoint.
         * 
         * @throws ObjectManagerException
         **/
        private void checkOccupancy()
                        throws ObjectManagerException
        {
            final String methodName = "checkOccupancy";
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            methodName,
                            new Object[] { new Long(fileLogHeader.fileSize) });

            // TODO Could do better here by making a rough estimate rather than calling getLogFileSpaceLeft()
            // TODO which synchronizes on the subTotals. Or by finding another way to trigger the checkpoint.
            //TODO Does this unsynchronized test help??
            long fileSpaceLeft = getLogFileSpaceLeft();
//      long fileSpaceLeft = 0;
//      for (int i = 0; i < fileSpaceAvailable.length;i++) {
//        fileSpaceLeft = fileSpaceLeft + fileSpaceAvailable[i];
//      } // for ...

            float occupancy = ((float) 1.0 - (float) (fileSpaceLeft) / (float) (fileLogHeader.fileSize));
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.debug(this,
                            cclass,
                            methodName,
                            new Object[] { new Long(fileSpaceLeft),
                                          new Float(occupancy),
                                          new Float(objectManagerState.logFullTriggerCheckpointThreshold),
                                          new Float(objectManagerState.logFullPostCheckpointThreshold) });

            // Should we trigger backout of transactions.
            if (occupancy > objectManagerState.logFullPostCheckpointThreshold) {
                ocupancyHigh = true;
            } else {
                ocupancyHigh = false;
            }

            // Should we suggest a checkpoint?
            if (occupancy > objectManagerState.logFullTriggerCheckpointThreshold) {
                objectManagerState.suggestCheckpoint(true);
                logFullCheckpointsTriggered++;
            }

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass, methodName);
        } // checkOccupancy().  
    } // class FlushHelper.

    /**
     * Supervises the notification of threads that a flush has completed. Does not block the
     * requester of the notification while it does this.
     */
    protected class NotifyHelper
                    implements Runnable
    {
        private boolean running = true;
        // True if we are currently waiting for a new request.            
        private boolean waiting = false;
        Thread notifyThread = null;
        private Exception abnormalTerminationException = null;

        // We are asked to notify waiters on all pages up to but not including the
        // requested page which is still being filled.
        private int requestedFirstPageFilling = 0;

        // A reference to the logBuffer who's waiters are being notified, 
        // usually the same as the one being filled unless
        // we have recently resized the logBuffer.
        LogBuffer notifyLogBuffer = logBuffer;

        /**
         * Constructor, makes a thread to notify().
         */
        NotifyHelper()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>");

            notifyThread = new Thread(this);
            notifyThread.setName("NotifyHelper");
            notifyThread.setPriority(helperThreadPriority);
            lastPageNotified = notifyLogBuffer.numberOfPages - 1;
            nextPageToNotify = 0;
            notifyThread.start();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "<init>"
                                );
        } // NotifyHelper().

        public void run()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "run");

            // Outer try/catch block treats all unexpected errors as terminal
            // and shuts down the ObjectManager if any occur.
            try {
                requestLoop: for (;;) {
                    synchronized (this) {
                        while (nextPageToNotify == requestedFirstPageFilling) {
                            if (!running)
                                break requestLoop;

                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this,
                                            cclass,
                                            "run",
                                            new Object[] { "waitLoop:3320",
                                                          new Integer(nextPageToNotify),
                                                          new Integer(requestedFirstPageFilling) });
                            // InterruptedException caught by outer try/catch block.
                            waiting = true;
                            wait();
                            waiting = false;

                        } // while (nextPageToNotify == requestedFirstPageFilling).            
                    } // synchronized (this).

                    // Perform Notification for the latest request up to but not including the requestedFirstPageFilling.
                    while (nextPageToNotify != requestedFirstPageFilling) {

                        // Notify waiters in the next page.
                        synchronized (notifyLogBuffer.pageWaitLock[nextPageToNotify]) {
                            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                                trace.debug(this,
                                            cclass,
                                            "run",
                                            new Object[] { "notifyLoop:3340",
                                                          new Integer(nextPageToNotify),
                                                          new Boolean(notifyLogBuffer.pageWaiterExists[nextPageToNotify]) });
                            notifyLogBuffer.pageFlushPending[nextPageToNotify] = false;

                            if (notifyLogBuffer.pageWaiterExists[nextPageToNotify]) {
                                notifyLogBuffer.pageWaitLock[nextPageToNotify].notifyAll();
                                notifyLogBuffer.pageWaiterExists[nextPageToNotify] = false;
                            } // if (notifyLogBuffer.pageWaiterExists[nextPageToNotify]).

                            if (gatherStatistics) {
                                int waiters = notifyLogBuffer.pageFlushWaiters[nextPageToNotify];
                                // Bins count 0,1,2,3...16,>16.
                                if ((waiters & 0x7FFFFFF0) != 0)
                                    numberOfFlushWaitersFrequency[16]++;
                                else
                                    numberOfFlushWaitersFrequency[waiters & 0x0000000F]++;
                                notifyLogBuffer.pageFlushWaiters[nextPageToNotify] = 0;
                            } // if (gatherStatistics).          
                        } // synchronized (notifyLogBuffer.pageWaitLock[nextPageToNotify]).

                        // Advance the last page notified. This is grabbed
                        // asynchronously by the writer threads and is always valid.
                        lastPageNotified = nextPageToNotify;
                        // Advance nextPageToNotify.
                        nextPageToNotify++;
                        if (nextPageToNotify == notifyLogBuffer.numberOfPages) {
                            nextPageToNotify = 0;

                            // If a new logBuffer has been allocated then clear the request to resize it.
                            if (notifyLogBuffer != logBuffer) {
                                synchronized (logBufferLock) {
                                    notifyLogBuffer = logBuffer;
                                    newLogBufferPages = 0;
                                } // synchronized (logBufferLock).
                            } // if (notifyLogBuffer != logBuffer).   
                        } // if (nextPageToNotify == notifyLogBufferPages).

                    } // while (nextPageToNotify...

                    // Restart the FlushHelper in case it stalled waiting for the NotifyHelper to catch up.
                    if (notifyLogBuffer.pageWaiterExists[nextPageToNotify])
                        flushHelper.startFlush();

                } // requestLoop: for (;;)

            } catch (Exception exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this,
                                                    cclass,
                                                    "run",
                                                    exception,
                                                    "1:3390:1.52");
                if (Tracing.isAnyTracingEnabled() && trace.isEventEnabled())
                    trace.event(this,
                                cclass,
                                "run",
                                exception);

                running = false;
                abnormalTerminationException = exception;
                // Make one asynchronous request to shutdown the ObjectManager.
                objectManagerState.requestShutdown();
            } // catch (ObjectManagerException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "run"
                                );
        } // run().

        synchronized void doNotifyAll(int requestedFirstPageFilling)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this, cclass
                            , "doNotifyAll"
                            , new Object[] { new Integer(requestedFirstPageFilling), new Boolean(waiting) });

            // Has something previously gone wrong? 
            if (abnormalTerminationException != null) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this, cclass
                               , "doNotifyAll"
                               , new Object[] { "via UnexpectedExceptionException",
                                               abnormalTerminationException });
                throw new UnexpectedExceptionException(this
                                                       , abnormalTerminationException);
            } // if (abnormalTerminationException != null).

            if (!running) {
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "doNotifyAll",
                               new Object[] { "via ThreadNotRunningException" });
                throw new ThreadNotRunningException(this,
                                                    notifyThread.getName(),
                                                    "startFlush");
            } // if (!running). 

            this.requestedFirstPageFilling = requestedFirstPageFilling;

            // If the notifyHelper is waiting, wake it up.
            if (waiting)
                notify();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this, cclass
                           , "doNotifyAll"
                                );
        } // doNotifyAll().

        private void shutdown()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "shutdown");

            synchronized (this) {
                running = false;
                notify();
            } // synchronize (this).

            // Wait for the worker thread to complete.
            try {
                notifyThread.join();
            } catch (InterruptedException exception) {
                // No FFDC Code Needed.
                ObjectManager.ffdc.processException(this, cclass, "shutdown", exception, "1:3470:1.52");
                if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                    trace.exit(this,
                               cclass,
                               "shutdown",
                               exception);
                throw new UnexpectedExceptionException(this,
                                                       exception);
            } // catch (InterruptedException exception).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "shutdown");
        } // shutdown().  
    } // innerClass NotifyHelper. 
} // class FileLogOutput.

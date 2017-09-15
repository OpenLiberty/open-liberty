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
 * PaddingLogRecord used to fill empty space in the log and pad it write up to a disk cache page boundary.
 * 
 * @author IBM Corporation
 */
class PaddingLogRecord
                extends LogRecord
{
    private static final Class cclass = PaddingLogRecord.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);
    private static final long serialVersionUID = 4725890292906828188L;

    // The overhead when writing a Padding Log Record.
    // It may not be less that than this size.
    protected static final int overhead = 4 + 4;
    // The total length of the resulting log record in bytes.
    int totalSize;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     * 
     * @param totalSize the total number of bytes in the log record.
     * @throws ObjectManagerException
     */
    protected PaddingLogRecord(int totalSize)
        throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        "TotalSize=" + totalSize);

        this.totalSize = totalSize;

        // Get the buffers that the log record wants to write.
        buffers = getBuffers();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // Constructor.

    /**
     * Constructor
     * 
     * @param dataInputStream from which to construct the log record.
     * @throws ObjectManagerException
     */
    protected PaddingLogRecord(java.io.DataInputStream dataInputStream)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { dataInputStream });

        try {
            // LogRecord.type already read.
            totalSize = dataInputStream.readInt();

            // Now get the padding.
            byte[] padBuffer = new byte[totalSize];
            dataInputStream.read(padBuffer);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this, cclass, "<init>", exception, "1:98:1.9");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>"
                           , exception);
            throw new PermanentIOException(this,
                                           exception);
        } //  catch (java.io.IOException exception).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // Constructor.

    /**
     * Gives back the number of bytes left to write if we were to complete the writing in a single part. If we need to
     * write more than one part then we will need extra bytes to describe this.
     * 
     * @return int the number of bytes in the serialized LogRecord.
     * @throws ObjectManagerException
     */
    protected int getBytesLeft()
                    throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getSerializedByteCount");

        int bytesLeft;
        if (bufferCursor == 0)
            bytesLeft = totalSize - bufferByteCursor;
        else
            bytesLeft = totalSize - overhead - bufferByteCursor;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getBytesLeft",
                       "return=" + bytesLeft);
        return bytesLeft;
    } // getBytesLeft().

    /**
     * Fills a buffer with bytes in the next part serialized LogRecord.
     * 
     * @param buffer we are to put the next part of this logRecord into.
     * @param offset into the buffer where we are to put the next part of this LogRecord.
     * @param length of the logBuffer we may fill this time round. If it is not
     *            sufficient to contain the whole logRecord.
     * @return int the new offset once the buffer has been filled.
     * @throws ObjectManagerException
     */
    protected int fillBuffer(byte[] buffer,
                             int offset,
                             int length)
                    throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "fillBuffer");

        if (buffers == null)
            buffers = getBuffers();

        // Only copy the header data, the rest is just padding.
        if (bufferCursor == 0) {
            int lengthToCopy = Math.min(buffers[bufferCursor].getCount() - bufferByteCursor,
                                        length);
            System.arraycopy(buffers[bufferCursor].getBuffer(),
                             bufferByteCursor,
                             buffer,
                             offset,
                             lengthToCopy);
            offset = offset + lengthToCopy;
            length = length - lengthToCopy;
            bufferByteCursor = bufferByteCursor + lengthToCopy;
            if (length > 0) { // Room for some more?
                bufferCursor++;
                bufferByteCursor = 0;
            }
        } // if (bufferCursor == 0).

        // Skip anything else asked for.
        if (length > 0) {
            int lengthToCopy = Math.min(totalSize - overhead - bufferByteCursor,
                                        length);

            offset = offset + lengthToCopy;
            bufferByteCursor = bufferByteCursor + lengthToCopy;
        } // if (length > 0).

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "fillBuffer",
                       "return=" + offset);
        return offset;
    } // fillBuffer().

    /**
     * Gives back the serialized LogRecord as an arrays of bytes.
     * 
     * @return ObjectManagerByteArrayOutputStream[] the buffers containing the serialized LogRecord.
     */
    protected ObjectManagerByteArrayOutputStream[] getBuffers()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getBuffers");

        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[1];

        // Create the buffer to contain the header for this log record.
        buffers[0] = new ObjectManagerByteArrayOutputStream(overhead);

        buffers[0].writeInt(LogRecord.TYPE_PADDING);
        buffers[0].writeInt(totalSize - overhead);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "getBuffers",
                       new Object[] { buffers }
                            );
        return buffers;
    } // getBuffers().

    /**
     * Called to perform recovery action during a warm start of the ObjectManager.
     * 
     * @param objectManagerState of the ObjectManager performing recovery.
     * @throws ObjectManagerException
     */
    public void performRecovery(ObjectManagerState objectManagerState)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "performRecovery",
                        new Object[] { objectManagerState });

        // Nothing to do.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "performRecovery");
    } // Of method performRecovery.
} // End of class PaddingLogRecord.

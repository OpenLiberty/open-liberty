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
 * <p>logRecord contains log information for each type of log record.
 * Defines the action to be taken when recovering the state of the object manager
 * during a warm start.
 * 
 * @author IBM Corporation
 */
public abstract class LogRecord
                implements java.io.Serializable {
    private static final Class cclass = LogRecord.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_TRAN);

    // Log Record types. 
    public static final int TYPE_USER_DEFINED = 0;
    protected static final int TYPE_ADD = 1;
    protected static final int TYPE_REPLACE = 2;
    protected static final int TYPE_OPTIMISTIC_REPLACE = 3;
    protected static final int TYPE_DELETE = 4;
    protected static final int TYPE_PREPARE = 5;
    protected static final int TYPE_COMMIT = 6;
    protected static final int TYPE_BACKOUT = 7;
    protected static final int TYPE_CHECKPOINT_START = 8;
    protected static final int TYPE_CHECKPOINT_END = 9;
    protected static final int TYPE_CHECKPOINT_TRANSACTION = 10;
    protected static final int TYPE_PADDING = 11;

    // The serialized form of the logRecord.
    public ObjectManagerByteArrayOutputStream[] buffers;
    // Cursors indicating the next buffer we should fill the log from and the next byte to fill from.
    int bufferCursor = 0;
    int bufferByteCursor = 0;
    // Identifies the logrecord if it has to be split into multiple parts.
    protected byte multiPartID = 0;

    /**
     * Construct a LogRecord and prepare its buffers ready to write to the log.
     */
    public LogRecord() {}

    /**
     * Recover a user defined Log Record from a DataInputStream
     * 
     * @param dataInputStream from which to construct the log record.
     * @param objectManagerState into which the logRecord is loaded, if
     *            the logRecord references any Tokens they are referenced by this
     *            ObjectManager.
     * @return LogRecord read from the dataInputstream.
     * @throws ObjectManagerException
     */
    protected static LogRecord getUserLogRecord(java.io.DataInputStream dataInputStream,
                                                ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "getUserLogRecord";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        methodName,
                        new Object[] { dataInputStream,
                                      objectManagerState });
        byte[] logRecordBytes;
        try {
            // LogRecord.type already read.      
            long serializedLogRecordLength = dataInputStream.readLong();
            // Now get the serialized form of the LogRecord.
            logRecordBytes = new byte[(int) serializedLogRecordLength];
            dataInputStream.read(logRecordBytes);

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, methodName, exception, "1:94:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(cclass,
                           methodName,
                           new Object[] { exception });
            throw new PermanentIOException("LogRecord",
                                           exception);
        } // catch (java.io.IOException exception).

        LogRecord logRecord = (LogRecord) deserialize(logRecordBytes, objectManagerState);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass,
                       methodName,
                       logRecord);
        return logRecord;
    } // getUserLogRecord(). 

    /**
     * Gives back the number of bytes left to write if we were to complete the writing in a single part. If we need to
     * write more than one part then we will need extra bytes to describe this.
     * 
     * @return int the number of bytes left in the serialized LogRecord.
     * @throws ObjectManagerException
     */
    protected int getBytesLeft()
                    throws ObjectManagerException {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "getBytesLeft");

        // Add up the total number of bytes to write.    
        int totalBytes = -bufferByteCursor; // We have already given up these bytes.
        for (int i = bufferCursor; i < buffers.length; i++) {
            totalBytes = totalBytes + buffers[i].getCount();
        } // for ... buffers.length.   

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getBytesLeft"
                       , "returns totalBytes=" + totalBytes + "(int)"
                            );
        return totalBytes;
    } // getBytesLeft().                                             

    /**
     * Indicates whether the buffer cursors are positioned at the start of the LogRecord buffers
     * indicating that no logBuffers have been filled yet.
     * 
     * @return boolean true if the cursors are positioned ta the start of the LogRecord.
     */
    protected boolean atStart()
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "atStart"
                            );

        boolean atStartOfLogRecord = (bufferCursor == 0) && (bufferByteCursor == 0);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "atStart"
                       , "returns atStartOfLogRecord=" + atStartOfLogRecord + "(boolean)"
                            );
        return atStartOfLogRecord;
    } // atStart().                                             

    /**
     * Fills a buffer with bytes in the next part serialized LogRecord.
     * 
     * @param buffer we are to put the next part of this logRecord into.
     * @param offset into the buffer where we are to put the next part of this LogRecord.
     * @param length of the logBuffer we may fill this time round. If it is not sufficient
     *            to contain the whole logRecord.
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
                        "fillBuffer",
                        new Object[] { buffer,
                                      new Integer(offset),
                                      new Integer(length) });

        // TODO Could do better by collecting the bytes from the log record directly into the logBuffer, or the log file
        // TODO for large logRecords .
        while (length > 0) {

            int lengthToCopy = Math.min(buffers[bufferCursor].getCount() - bufferByteCursor
                                        , length);
            System.arraycopy(buffers[bufferCursor].getBuffer()
                             , bufferByteCursor
                             , buffer
                             , offset
                             , lengthToCopy);
            offset = offset + lengthToCopy;
            length = length - lengthToCopy;
            bufferByteCursor = bufferByteCursor + lengthToCopy;
            if (length > 0) { // Room for some more?
                bufferCursor++;
                bufferByteCursor = 0;
            }
        } // while (length > 0). 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "fillBuffer"
                       , "returns offset=" + offset + "(int)"
                            );
        return offset;
    } // fillBuffer().

    /**
     * Gives back the serialized LogRecord as arrays of bytes.
     * Unless overriden this simply serializes the Log Record.
     * 
     * @return ObjectManagerByteArrayOutputStream[] the buffers containing the serialized LogRecord.
     * @throws ObjectManagerException
     */
    protected ObjectManagerByteArrayOutputStream[] getBuffers()
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass
                        , "getBuffers"
                            );

        // Default log record behaviour.
        // -----------------------------

        // Captute the serialized Log Record.
        ObjectManagerByteArrayOutputStream serializedLogRecord = serialize(this);
        // Create an array for the header and the serialized ManagedObject.
        ObjectManagerByteArrayOutputStream[] buffers = new ObjectManagerByteArrayOutputStream[2];

        // We dont add a version to each logRecord, but use the logHeader version to enable
        // forward migration by assuming all logRecords in the log ore appropriate the the version of the
        // header.
        // Create the buffer to contain the header for this log record.
        buffers[0] = new ObjectManagerByteArrayOutputStream(4 // Log Record Type.
                                                            + 8);
        buffers[0].writeInt(LogRecord.TYPE_USER_DEFINED);
        buffers[0].writeLong(serializedLogRecord.getCount());

        // Now add a buffer containing the serialzed Log Record itself. 
        buffers[1] = serializedLogRecord;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass
                       , "getBuffers"
                       , "returns buffers=" + buffers + "(byte[][])"
                            );
        return buffers;
    } // getBuffers().                                             

    /**
     * Called to perform any recovery action during a warm start of the ObjectManager.
     * 
     * @param objectManagerState of the ObjectManager performing recovery.
     * @throws ObjectManagerException
     */
    public abstract void performRecovery(ObjectManagerState objectManagerState)
                    throws ObjectManagerException;

    /**
     * Calculate the serializedSize of the logRecord excluding any payload.
     * 
     * @return the number of bytes that will be written to the log.
     */
    protected static long maximumSerializedSize()
    {
        // LogRecord is abstract this method must be overriden.
        throw new UnsupportedOperationException();
    }

    private static final byte SimpleSerialVersion = 0;

    /**
     * Serialize's an Object by turning it into an array of bytes.
     * 
     * @param serializableObject to serialize.
     * @return ObjectManagerByteArrayOutputStream containing the serialized form of the Object.
     * @throws ObjectManagerException
     */
    protected ObjectManagerByteArrayOutputStream serialize(java.io.Serializable serializableObject)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "serialize",
                        new Object[] { serializableObject });

        ObjectManagerByteArrayOutputStream byteArrayOutputStream = new ObjectManagerByteArrayOutputStream();
        try {
            java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(serializableObject);
            objectOutputStream.close();

        } catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(this,
                                                cclass,
                                                "serialize",
                                                exception,
                                                "1:303:1.8");
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "serialize",
                           exception);
            throw new PermanentIOException(this,
                                           exception);
        } // catch.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "serialize",
                       new Object[] { byteArrayOutputStream });
        return byteArrayOutputStream;
    } // serialize(). 

    /**
     * Turns a serialized Object from an array of bytes back into an object.
     * 
     * @param objectBytes the serialized form of the serializableObject.
     * @param objectManagerState which owns any tokens referenced by the Object.
     * @return Object the Object recovered from the Byte[].
     * @throws ObjectManagerException
     */
    protected static Object deserialize(byte[] objectBytes, ObjectManagerState objectManagerState)
                    throws ObjectManagerException {
        final String methodName = "deserialize";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(cclass,
                        methodName,
                        new Object[] { objectBytes,
                                      objectManagerState });

        java.io.ByteArrayInputStream byteArrayInputStream = new java.io.ByteArrayInputStream(objectBytes);
        Object object = null;
        try
        {
            ManagedObjectInputStream managedObjectInputStream = new ManagedObjectInputStream(byteArrayInputStream, objectManagerState);
            object = managedObjectInputStream.readObject();
            managedObjectInputStream.close();

        } catch (java.lang.ClassNotFoundException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, methodName, exception, "1:347:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(cclass,
                           methodName,
                           exception);
            throw new ClassNotFoundException(cclass, exception);
        } // catch ClassNotFoundException.
        catch (java.io.IOException exception) {
            // No FFDC Code Needed.
            ObjectManager.ffdc.processException(cclass, methodName, exception, "1:357:1.8");

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(cclass,
                           methodName,
                           exception);
            throw new PermanentIOException(cclass,
                                           exception);
        } // catch.

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(cclass,
                       methodName,
                       new Object[] { object });
        return object;
    } // deserialize().  
} // class LogRecord.

/*******************************************************************************
 * Copyright (c) 2003, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.nio.ByteBuffer;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

//------------------------------------------------------------------------------
// Class: WritableLogRecord
//------------------------------------------------------------------------------
/**
 * <p>
 * The WritableLogRecord class extends LogRecord with additional function to write
 * the record header and tail structures. It also provides an interface to
 * allow other recovery log components to write the record content.
 * </p>
 *
 * <p>
 * Because this class uses a duplicate of the original MappedByteBuffer (see
 * LogRecord), each instance has an indepednent byte cursor. As a result, it
 * is safe to allow concurrent write calls from multiple instances of this
 * class.
 * </p>
 */
public class WriteableLogRecord extends LogRecord {
    private static final TraceComponent tc = Tr.register(WriteableLogRecord.class, TraceConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * The sequence number of this log record (see record structure above)
     */
    private final long _sequenceNumber;

    //------------------------------------------------------------------------------
    // Method: WritableLogRecord.WritableLogRecord
    //------------------------------------------------------------------------------
    /**
     * Constructor for a new WritableLogRecord. As part of construction, this class
     * creates the record header sructure. On exit from construction, the buffers
     * byte cursor will be positioned on the very next byte after the header, ready
     * for other recovery log components to write the record content.
     *
     * @param buffer         The target buffer.
     * @param sequenceNumber The sequence number of this new log record.
     * @param length         The length of the data that other recovery log components will
     *                           write to the record.
     */
    @Trivial
    protected WriteableLogRecord(ByteBuffer buffer, long sequenceNumber, int length, int absolutePosition) {
        super(buffer, absolutePosition);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "WriteableLogRecord {0} {1} {2} {3} {4}", this, buffer, sequenceNumber, length, absolutePosition);

        _buffer.put(RECORD_MAGIC_NUMBER);
        _buffer.putLong(sequenceNumber);
        _buffer.putInt(length);

        _sequenceNumber = sequenceNumber;
    }

    //------------------------------------------------------------------------------
    // Method: WritableLogRecord.put
    //------------------------------------------------------------------------------
    /**
     * Setter method used to write bytes.length bytes from the buffer into the mapped
     * byte buffer at the current byte cursor position. The byte cursor is advanced by
     * bytes.length.
     *
     * @param bytes The source byte array from which the bytes will be written.
     */
    @Trivial
    protected void put(byte[] bytes) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "put {0} {1} {2}", RLSUtils.toHexString(bytes, RLSUtils.MAX_DISPLAY_BYTES), _buffer.position(), this);

        _buffer.put(bytes);
    }

    //------------------------------------------------------------------------------
    // Method: WritableLogRecord.putInt
    //------------------------------------------------------------------------------
    /**
     * Setter method used to write an integer to the mapped byte buffer at the current
     * byte cursor position. The byte cursor is advanced by the size of an integer.
     *
     * @param data The int value to be written.
     */
    @Trivial
    protected void putInt(int data) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Writing at position {0} {1} {2}", _buffer.position(), data, this);

        _buffer.putInt(data);
    }

    //------------------------------------------------------------------------------
    // Method: WritableLogRecord.putLong
    //------------------------------------------------------------------------------
    /**
     * Setter method used to write a long to the mapped byte buffer at the current
     * byte cursor position. The byte cursor is advanced by the size of a long.
     *
     * @param data The long value to be written.
     */
    @Trivial
    protected void putLong(long data) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Writing at position {0} {1} {2}", _buffer.position(), data, this);
        _buffer.putLong(data);
    }

    //------------------------------------------------------------------------------
    // Method: WritableLogRecord.putShort
    //------------------------------------------------------------------------------
    /**
     * Setter method used to write a short to the mapped byte buffer at the current
     * byte cursor position. The byte cursor is advanced by the size of a short.
     *
     * @param data The short value to be written.
     */
    @Trivial
    protected void putShort(short data) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Writing at position {0} {1} {2}", _buffer.position(), data, this);
        _buffer.putShort(data);
    }

    //------------------------------------------------------------------------------
    // Method: WritableLogRecord.putBoolean
    //------------------------------------------------------------------------------
    /**
     * Setter method used to write a boolean to the mapped byte buffer at the current
     * byte cursor position. The byte cursor is advanced by the size of a boolean.
     *
     * @param data The boolean value to be written.
     */
    @Trivial
    protected void putBoolean(boolean data) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Writing at position {0} {1} {2}", _buffer.position(), data, this);
        _buffer.put(data ? TRUE : FALSE);
    }

    //------------------------------------------------------------------------------
    // Method: WritableLogRecord.close
    //------------------------------------------------------------------------------
    /**
     * Close the WritableLogRecord. Creates the record tail sructure. On exit from
     * this method, the buffers byte cursor will be positioned on the very next byte
     * after the log record. No further access to this instance is permitted after
     * this call has been issued.
     */
    @Trivial
    protected void close() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "close {0}", this);

        _buffer.putLong(_sequenceNumber);
    }
}

/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
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

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

//------------------------------------------------------------------------------
// Class: LogRecord
//------------------------------------------------------------------------------
/**
 * <p>
 * The LogRecord class provides common function for both ReadableLogRecords and
 * WritableLogRecords. LogRecords allow low level access the physical files that
 * make up the underlying recovery log.
 * </p>
 *
 * <p>
 * This class provides methods for navigation around the file and for reading
 * from the file.
 * </p>
 *
 * <p>
 * The recovery log service maps the log file directly into memory using a
 * MappedByteBuffer. This class holds a reference to a duplicate of this
 * MappedByteBuffer with its own independent position cursor. The file can
 * be read from or written to by calling methods on this duplicate.
 * </p>
 *
 * <p>
 * A recovery log file is essentially a linier sequence of log "records". This
 * class and its sub-classes manage these records by reading and writing their
 * header and tail fields and then giving other parts of the recovery log service
 * the support required for them to read and write their content into the record.
 * These other classes are the RecoverableUnitImpl, RecoverableUnitSectionImpl
 * and DataItem.
 * <p>
 *
 * <p>
 * The structure for a log record is as follows:
 * </p>
 *
 * <p>
 * ---------------------------------------------------------------------
 * Field Content Field Type Field Size (in bytes)
 * ---------------------------------------------------------------------
 * "Magic Number" ("RCRD") byte[] 4
 * Record Sequence Number long 8
 * Recovery Length int 4
 *
 * < other record data, the structure of >
 * < which is not defined by this class >
 *
 * Record Sequence Number Repeat long 8
 * </p>
 *
 * <p>
 * The "magic number" is used as part of the validation check to ensure that a record
 * is valid. Additionally, each record has an assosiated sequence number which is
 * placed in both the header and tail fields. By ensuring that the tail sequence number
 * matches the header sequence number (given the record length specififed in the header)
 * we can be confident that the entire record is valid. The sequence number of the first
 * log record in the file is stored within the log file header information. Sequence
 * numbers are allocated consecutively.
 * </p>
 */
public abstract class LogRecord {
    /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(LogRecord.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

    /**
     * TRUE and FALSE boolean constants.
     */
    protected static final byte TRUE = 1;
    protected static final byte FALSE = 0;

    /**
     * The mapped buffer into which the log record will be written.
     */
    protected ByteBuffer _buffer;

    /**
     * The absolute position of the ByteBuffer view in the entire ByteBuffer from which this
     * view is a subsequence.
     */
    private final int _absolutePosition;

    /**
     * Magic number written to the disk to indicate the start of a valid record. Used along with
     * other parts of the record header to ensure that the data being recovered is actually a
     * real record. This magic number is the 4 byte string 'RCRD'
     */
    protected static final byte[] RECORD_MAGIC_NUMBER = { 82, 67, 82, 68 };

    /**
     * The size, in bytes, of the "head" and "tail" information needed to encapsulate
     * a log record.
     */
    public static final int HEADER_SIZE = RECORD_MAGIC_NUMBER.length + // RCRD
                                          RLSUtils.LONG_SIZE + // sequence number
                                          RLSUtils.INT_SIZE + // record length
                                          RLSUtils.LONG_SIZE; // tail sequence number

    //------------------------------------------------------------------------------
    // Method: LogRecord.LogRecord
    //------------------------------------------------------------------------------
    /**
     * Constructor for a new LogRecord. Instances of this class are never created
     * directly, instead instances of ReadableLogRecord or WritableLogRecord should
     * be used.
     *
     * @param buffer The target mapped byte buffer. The caller should supply a
     *                   duplicate of the original byte buffer if they want to ensure
     *                   that this log records byte cursor is isolated from other
     *                   log records.
     */
    @Trivial
    protected LogRecord(Buffer buffer, int absolutePosition) {

        _buffer = (ByteBuffer) buffer;
        _absolutePosition = absolutePosition;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "LogRecord {0} {1} {2}", this, buffer, absolutePosition);
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.position
    //------------------------------------------------------------------------------
    /**
     * Returns the position of the byte cursor for the mapped byte buffer.
     *
     * @return The byte cursor position.
     */
    @Trivial
    protected int position() {

        int position = _absolutePosition + _buffer.position();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "position {0} {1}", position, this);
        return position;
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.position
    //------------------------------------------------------------------------------
    /**
     * Sets the position of the byte cursor for the mapped byte buffer.
     *
     * @param newPosition The required byte cursor position
     */
    protected void position(int newPosition) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "position", this, newPosition);

        newPosition -= _absolutePosition;
        _buffer.position(newPosition);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "position");
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.absolutePosition
    //------------------------------------------------------------------------------
    /**
     * Retrieves the absolute position (in the log ByteBuffer) of the beginning
     * of this LogRecord's view buffer.
     */
    @Trivial
    protected int absolutePosition() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "absolutePosition {0} {1}", _absolutePosition, this);
        return _absolutePosition;
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.advancePosition
    //------------------------------------------------------------------------------
    /**
     * Moves the current byte cursor position for the mapped byte buffer forwards.
     *
     * @param bytes The offset in bytes to move to.
     */
    protected void advancePosition(int bytes) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "advancePosition", this, bytes);

        final int newPosition = _buffer.position() + bytes;
        _buffer.position(newPosition);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Buffer's position now " + newPosition);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "advancePosition");
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.get
    //------------------------------------------------------------------------------
    /**
     * Getter method used to read bytes.length bytes from the mapped byte buffer at
     * the current byte cursor position into the supplied byte array. The byte cursor
     * is advanced by bytes.length.
     *
     * @param bytes The target byte array into which the bytes will be read.
     */
    @Trivial
    protected void get(byte[] bytes) {

        _buffer.get(bytes);

        if (tc.isDebugEnabled())
            Tr.debug(tc, RLSUtils.toHexString(bytes, RLSUtils.MAX_DISPLAY_BYTES));
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.getInt
    //------------------------------------------------------------------------------
    /**
     * Getter method used to read an integer from the mapped byte buffer at the
     * current byte cursor position. The byte cursor is advanced by the size of an
     * integer.
     *
     * @return The int value read from the mapped byte buffer.
     */
    @Trivial
    protected int getInt() {

        int data = _buffer.getInt();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getInt {0} {1}", data, this);
        return data;
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.getLong
    //------------------------------------------------------------------------------
    /**
     * Getter method used to a long from the mapped byte buffer at the current
     * byte cursor position. The byte cursor is advanced by the size of a long.
     *
     * @return The long value read from the mapped byte buffer.
     */
    @Trivial
    protected long getLong() {

        long data = _buffer.getLong();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getLong {0} {1}", data, this);
        return data;
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.getShort
    //------------------------------------------------------------------------------
    /**
     * Getter method used to a short from the mapped byte buffer at the current
     * byte cursor position. The byte cursor is advanced by the size of a short.
     *
     * @return The short value read from the mapped byte buffer.
     */
    @Trivial
    protected short getShort() {

        short data = _buffer.getShort();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getShort {0} {1}", data, this);
        return data;
    }

    //------------------------------------------------------------------------------
    // Method: LogRecord.getBoolean
    //------------------------------------------------------------------------------
    /**
     * Getter method used to a boolean from the mapped byte buffer at the current
     * byte cursor position. The byte cursor is advanced by the size of a boolean.
     *
     * @return The boolean value read from the mapped byte buffer.
     */
    @Trivial
    protected boolean getBoolean() {

        byte dataByte = _buffer.get();
        boolean data = (dataByte == TRUE);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getBoolean {0} {1}", data, this);
        return data;
    }
}

/* ************************************************************************** */
/* ********************************************************************************* */
/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.util.ArrayList;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.LogCursor;
import com.ibm.ws.recoverylog.spi.LogCursorImpl;
import com.ibm.ws.recoverylog.spi.PeerLostLogOwnershipException;
import com.ibm.ws.recoverylog.spi.RLSUtils;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;
import com.ibm.ws.recoverylog.spi.TraceConstants;

//------------------------------------------------------------------------------
// Class: SQLRecoverableUnitSectionImpl
//------------------------------------------------------------------------------
/**
 * <p>
 * Information written to a recovery log is grouped into arbitrary number of
 * discrete blocks called "recoverable units". The SQLRecoverableUnitImpl class
 * represents a single recoverable unit within a recovery log. A recoverable
 * unit is identified by a key that must be supplied by the client service and
 * guaranteed to be unique within the recovery log. Client services use recoverable
 * units to group information according to their requirements. Typically, the
 * client service will group information related to a specific unit of work
 * in a single recoverable unit.
 * </p>
 *
 * <p>
 * Each recoverable unit is further subdivided into an arbitrary number of
 * discrete blocks called "recoverable unit sections". The SQLRecoverableUnitSectionImpl
 * class represents a single recoverable unit section within a recoverable unit.
 * A recoverable unit section is identified by a key that must be supplied by
 * the client service and guaranteed to be unique within the recoverable unit.
 * Typically, the client service will group information of a given type into
 * a single recoverable unit section.
 * </p>
 *
 * <p>
 * Information in the form of byte arrays is written to a recoverable unit section
 * rather than directly to recoverable unit.
 * </p>
 *
 * <p>
 * The recoverable unit section categorizes information into two groups - 'written'
 * information and 'unwritten' information. Written information has already been
 * passed to the underlying recovery log (and potentially stored persistently)
 * whereas unwritten information has not. This distinction is made to support two
 * types of log write.
 * </p>
 *
 * <p>
 * <ul>
 * <li>Standard log writes where only unwritten (new) information is written to
 * the underlying recovery log</li>
 * <li>Keypoint log writes where both written and unwritten (old and new)
 * information is written to the underlying recovery log</li>
 * </ul>
 * </p>
 *
 * This class provides the implementation of the SQLRecoverableUnitSection interface.
 */
public class SQLRecoverableUnitSectionImpl implements RecoverableUnitSection {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(SQLRecoverableUnitSectionImpl.class,
                                                         TraceConstants.TRACE_GROUP, null);

    /**
     * Initial size of the ArrayLists holding both written and unwritten data. The
     * ArrayList will grow if the number of elements exceeds this initial value.
     */
    private static final int INITIAL_DATA_CAPACITY = 10;

    /**
     * The identity of the recoverable unit section
     */
    private int _identity = 0;

    /**
     * An array of data items written to the recoverable unit section but not yet
     * written to the underlying recovery log. These data items are stored in the
     * order that they were added.
     */
    private ArrayList _unwrittenData = null;

    /**
     * An array of data items written to the recoverable unit section and subsequently
     * written to the underlying recovery log. These data items are stored in the
     * order that they were added.
     */
    private ArrayList _writtenData = null;

    /**
     * Flag to indicate if a recoverable unit section should hold only a single
     * data item. If this is true then the recoverable unit section will replace
     * any current data item with a new item on an addData call. If this is false,
     * the recoverable unit section will accumulate data items on successive
     * addData calls.
     */
    private boolean _singleData = false;

    private boolean _singleDataUpdated;

    /**
     * A "quick" reference to the last data item that was added to the recoverable
     * unit. This is maintained to allow the lastData() method direct access to it
     * rather than having to itterate search through the arraylist for the last
     * item.
     */
    private byte[] _lastDataItem = null;

    /**
     * Identity of the recoverable unit that contains this recoverable unit section
     */
    private long _recoverableUnitIdentity = 0;

    /**
     * The recovery log to which this recoverable unit section belongs.
     */
    private SQLMultiScopeRecoveryLog _recLog = null;

    /**
     * A reference to the recoverable unit that contains this recoverable unit section.
     */
    private SQLRecoverableUnitImpl _recUnit = null;

    /**
     * The name of the application server to which this recoverable unit belongs.
     */
    private String _serverName = null;

    /**
     * The name of the client service that owns the recoverable unit.
     */
    private String _clientName = null;

    /**
     * The version number of the client service that owns the recoverable unit.
     */
    private int _clientVersion = 0;

    /**
     * The name of the associated recovery log within which this recoverable unit
     * resides.
     */
    private String _logName = null;

    /**
     * The identity of the associated recovery log within which this recoverable unit
     * resides.
     */
    private int _logIdentifier = 0;

    /**
     * The 'traceId' string is output at key trace points to allow easy mapping
     * of recovery log operations to clients logs.
     */
    private final String _traceId;

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitSectionImpl.SQLRecoverableUnitSectionImpl
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Package access constructor for the creation of recoverable unit sections.
     * </p>
     *
     * <p>
     * This method should only be called by the RLS itself during either initial
     * creation of a recoverable unit section or recreation during server startup.
     * </p>
     *
     * @param recLog The recovery log that contains this recoverable unit section.
     * @param recUnit The recoverable unit that contains this recoverable unit section.
     * @param recoverableUnitIdentity The identity of the recoverable unit that contains
     *            this recoverable unit section)
     * @param identity The identity of the new recoverable unit section (unique within
     *            the recoverable unit)
     * @param singleData Boolean flag to indicate if this recoverable unit section can
     *            hold just a single item of data at a time. If this is true
     *            then the recoverable unit section will replace any current
     *            data item with a new item on an addData call. If this is
     *            false, the recoverable unit section will accumulate data
     *            items on successive addData calls.
     */
    SQLRecoverableUnitSectionImpl(SQLMultiScopeRecoveryLog recLog, SQLRecoverableUnitImpl recUnit, long recoverableUnitIdentity, int identity, boolean singleData) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "SQLRecoverableUnitSectionImpl",
                     new java.lang.Object[] { recLog, recUnit, new Long(recoverableUnitIdentity), new Integer(identity), new Boolean(singleData) });

        // Cache the supplied information
        _recLog = recLog;
        _recoverableUnitIdentity = recoverableUnitIdentity;
        _identity = identity;
        _singleData = singleData;
        _recUnit = recUnit;

        // Prepare the two array lists used to hold DataItems.
        _unwrittenData = new ArrayList(INITIAL_DATA_CAPACITY);
        _writtenData = new ArrayList(INITIAL_DATA_CAPACITY);

        // Cache details about the identity of the associated client / recovery log
        _serverName = recLog.serverName();
        _clientName = recLog.clientName();
        _clientVersion = recLog.clientVersion();
        _logName = recLog.logName();
        _logIdentifier = recLog.logIdentifier();

        // Now establish a 'traceId' string. This is output at key trace points to allow
        // easy mapping of recovery log operations to clients logs.
        _traceId = "SQLRecoverableUnitSectionImpl:" + "serverName=" + _serverName + ":"
                   + "clientName=" + _clientName + ":"
                   + "clientVersion=" + _clientVersion + ":"
                   + "logName=" + _logName + ":"
                   + "logIdentifier=" + _logIdentifier + " @"
                   + System.identityHashCode(this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "SQLRecoverableUnitSectionImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitSectionImpl.addData
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Adds new data to the recoverable unit section. Initially, the data is
     * 'unwritten' and cachjed inside the '_unwrittenData' array. Once the data has
     * been passed to the underlying recovery log, its considered 'written' and cached
     * inside the '_writtenData' array.
     * </p>
     *
     * <p>
     * Data added to a recoverable unit section is wrapped inside a DataItem object. This
     * allows the access to the data to be independant of the physical location of the
     * data (which can be cached in memory but may only be held on disk)
     * </p>
     *
     * <p>
     * Data items will be accumulated on successive addData calls, unless the recoverable
     * unit section can hold only a single data item. If this is the case, the old data
     * will be disgarded and replaced with new data.
     * </p>
     *
     * <p>
     * Only non-null and non-empty byte arrays may be passed to this method.
     * </p>
     *
     * @param data The new data item to be stored.
     *
     * @exception InternalLogException An unxpected error has occured
     */
    @Override
    public void addData(byte[] data) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addData", new java.lang.Object[] { RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES), this });

        // NOTE this method and writeData are not sycnhronized in any way - it is via the ControlLock for file system MSRL which makes keypoint and these methods 'synchronized'
        // however our keypoint doesn't go through all RUs re-writing data - it just flushes out the cache and we (and MSRL) rely on sections being updated
        // in a single threaded fashion (should be true for tran data and tran partner data).

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "addData", this);
            throw _recLog.getFailureException();
        }

        if (_singleData) {
            // This recoverable unit section can hold only a single data item.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Section can hold only a single data item.");

            if (_writtenData.size() > 0) {
                // It already contains written data. This must be replaced.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "There is existing WRITTEN data.");

                // Clear out the written data array and move this into the unwritten data array
                _writtenData.clear();
                _unwrittenData.add(0, data);
                _singleDataUpdated = true;

                // Ensure that the last data reference is accurate.
                _lastDataItem = data;
            } else if (_unwrittenData.size() > 0) {
                // It already contains unwritten data. This must be replaced.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "There is existing UNWRITTEN data.");

                _unwrittenData.clear();
                _unwrittenData.add(0, data);

                // Ensure that the last data reference is accurate.
                _lastDataItem = data;
            } else {
                // It contains no existing data. Create a new DataItem wrapper
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "There is no existing data.");

                _unwrittenData.add(0, data);
                _lastDataItem = data;
            }
        } else {
            // This recoverable unit section can hold multiple data item.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Section holds multiple data items");

            _unwrittenData.add(data);
            _lastDataItem = data;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addData");
    }

// recovery method to add data directly to _writtenData array
    public void addData(int index, byte[] data) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addData", new java.lang.Object[] { new Integer(index), RLSUtils.toHexString(data, RLSUtils.MAX_DISPLAY_BYTES), this });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "addData", this);
            throw _recLog.getFailureException();
        }

        // we use an index value of 0 to indicate that it is a singledata RUsection
        // so adjust now ... if (!_singleData) or if(index != 0)
        if (index > 0)
            index--;

        // list items may be added in any order, so it may be necessary to (temporarily) pad the list
        final int currentSize = _writtenData.size();
        if (index == currentSize)
            _writtenData.add(/* index, */ data);
        else if (index < currentSize) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "NMTEST: Replacing item (expect trace 'null') at index: " + index, _writtenData.get(index));
            _writtenData.set(index, data);
        } else // index > currentSize
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "NMTEST: Adding null elements: " + (index - currentSize));
            while (index-- > currentSize)
                _writtenData.add(null);

            _writtenData.add(data);
        }

        // set lastdata.  This method is called during recovery and we shouldn't get asked for
        // any data until all log records are read.  So set lastdata to be the item at the current size
        // of the array.  Items may be added in random order, so lastitem will be correct when
        // all items have been added
        _lastDataItem = (byte[]) _writtenData.get(_writtenData.size() - 1);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addData");
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitSectionImpl.write
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Writes to the underlying recovery log information from the target
     * recoverable unit section that has not already been written by a previous call.
     * This ensures that the recovery log contains an up to date copy of the
     * information retained in the target recoverable unit section.
     * </p>
     *
     * <p>
     * The information is written to the underlying recovery log, but not forced
     * through to persisent storage. After this call, the information is not
     * guaranteed to be retrieved during any post-failure recovery processing.
     * To ensure that this information will be recovered, a force operation
     * should be used instead (eg SQLRecoverableUnitSectionImpl.forceSections)
     * </p>
     *
     * @exception InternalLogException An unexpected error has occured.
     */
    @Override
    public void write() throws InternalLogException {
        write(true);
    }

    void write(boolean throttle) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "write", new java.lang.Object[] { Boolean.valueOf(throttle), this });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "write", this);
            throw _recLog.getFailureException();
        }

        // If there is unwritten data stored within this recoverable unit section then direct the
        // parent recoverable unit to perform a write for this recoverable unit section. We have
        // to go via the parent because this class has no knowledge of the parents recovery log
        // record structure etc..
        while (_unwrittenData.size() > 0) {
            final byte[] data = (byte[]) _unwrittenData.get(0);
            final int index = _writtenData.size();
            try {
                if (_singleData) {
                    if (_singleDataUpdated)
                        _recLog.updateRUSection(_recoverableUnitIdentity, _identity, data, throttle);
                    else
                        _recLog.writeRUSection(_recoverableUnitIdentity, _identity, index, data, throttle);
                } else {
                    _recLog.writeRUSection(_recoverableUnitIdentity, _identity, 1 + index, data, throttle);
                }
            } catch (PeerLostLogOwnershipException ple) {
                // No FFDC in this case
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "write", ple);
                throw ple;
            } catch (InternalLogException exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLRecoverableUnitSectionImpl.write", "437", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "write", exc);
                throw exc;
            }

            // db updated so now update our internal tables
            _unwrittenData.remove(0);
            _writtenData.add(data);

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "write");
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitSectionImpl.force
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Forces to the underlying recovery log any information in the target recoverable
     * unit section that has not already been written by a previous call. This ensures
     * that the recovery log contains an up to date copy of the information retained
     * in the target recoverable unit section.
     * </p>
     *
     * <p>
     * The information is written to the underlying recovery log and forced
     * through to persisent storage. After this call, the information is
     * guaranteed to be retrieved during any post-failure recovery processing.
     * </p>
     *
     * <p>
     * This call my be used as part of an optomization when several recoverable unit
     * sections need to be pushed to disk. For example, the following sequence will
     * ensure that recoverable unit sections 1 through 4 are all persisted to
     * physical storage:-
     * </p>
     * <p>
     * <ul>
     * <li>RecoverableUnitSection1.writeSection()</li>
     * <li>RecoverableUnitSection2.writeSection()</li>
     * <li>RecoverableUnitSection3.writeSection()</li>
     * <li>RecoverableUnitSection4.forceSection()</li>
     * </ul>
     * </p>
     *
     * @exception InternalLogException An unexpected error has occured.
     */
    @Override
    public void force() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "force", this);

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "force", this);
            throw _recLog.getFailureException();
        }

        try {
            _recUnit.forceSections();
        } catch (PeerLostLogOwnershipException ple) {
            // No FFDC in this case
            if (tc.isEntryEnabled())
                Tr.exit(tc, "force", ple);
            throw ple;
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLRecoverableUnitSectionImpl.force", "509", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "force", exc);
            throw exc;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "force");
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitSectionImpl.data
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Returns a LogCursor that can be used to itterate through all data contained
     * by this recoverable unit section (both unwritten and written). The byte arrays
     * will be returned in that they were added (ie FIFO)
     * </p>
     *
     * <p>
     * The LogCursor must be closed when it is no longer needed or its itteration
     * is complete. (See the LogCursor class for more information)
     * </p>
     *
     * <p>
     * Objects returned by <code>LogCursor.next</code> or <code>LogCursor.last</code>
     * must be cast to type byte[].
     * </p>
     *
     * <p>
     * Care must be taken not remove or add data whilst the resulting LogCursor is
     * open. Doing so will result in a ConcurrentModificationException being thrown.
     * </p>
     *
     * @return A LogCursor that can be used to itterate through all contained data.
     *
     *
     */
    @Override
    public LogCursor data() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "data", this);

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "data", this);
            throw _recLog.getFailureException();
        }

        // Trace out the number of written and unwritten data blocks held by this SQLRecoverableUnitSectionImpl
        if (tc.isEventEnabled()) {
            int writtenDataBlocks = 0;
            int unwrittenDataBlocks = 0;

            if (_writtenData != null) {
                writtenDataBlocks = _writtenData.size();
            }
            if (_unwrittenData != null) {
                unwrittenDataBlocks = _unwrittenData.size();
            }

            Tr.event(tc, "#writtenDataBlocks = " + writtenDataBlocks + " #unwrittenDataBlocks = " + unwrittenDataBlocks);
        }

        LogCursor cursor = null;

        if (_singleData) {
            // There is only a single data item. The LogCursor provides an optomized constructor
            // for this case.
            if (_writtenData.size() > 0) {
                cursor = new LogCursorImpl(null, _writtenData.get(0));
            } else {
                cursor = new LogCursorImpl(null, _unwrittenData.get(0));
            }
        } else {
            // There are potentially multiple data items. Use the standard LogCursor constructor.
            cursor = new LogCursorImpl(null, _writtenData, _unwrittenData, false, null);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "data", new Integer(cursor.initialSize()));

        return cursor;
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitSectionImpl.identity
    //------------------------------------------------------------------------------
    /**
     * Returns the identity of the recoverable unit section.
     *
     * @return The identity of the recoverable unit section.
     */
    @Override
    public int identity() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "identity", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "identity", new Integer(_identity));
        return _identity;
    }

    //------------------------------------------------------------------------------
    // Method: SQLtion.lastData
    //------------------------------------------------------------------------------
    /**
     * Returns the data item most recently stored inside the recoverable unit
     * section. Since the data items inside the recoverable unit section are retrieved
     * in the same order in which the they were added (ie FIFO), this method is
     * the logical equivalent of obtaining a LogCursor on the data and repeatedly
     * invoking next() until the last data item is obtained.
     *
     * This class etains ownership of the data buffer. The caller must not modify
     * it as doing so may result in this updated information being written to the
     * recovery log at an undefined time. It is not possible to update the buffer
     * and then ensure its written to disk by calling force.
     *
     * @return The last data item stored inside the recoverable unit section or
     *         null if the recoverable unit section is empty.
     */
    @Override
    public byte[] lastData() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lastData", this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lastData", RLSUtils.toHexString(_lastDataItem, RLSUtils.MAX_DISPLAY_BYTES));
        return _lastDataItem;
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitSectionImpl.toString
    //------------------------------------------------------------------------------
    /**
     * Returns the string representation of this object instance.
     *
     * @return String The string representation of this object instance.
     */
    @Override
    public String toString() {
        return _traceId;
    }
}

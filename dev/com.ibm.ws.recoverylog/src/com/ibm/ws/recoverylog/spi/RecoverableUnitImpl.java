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

import java.io.IOException;
import java.util.Iterator;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: RecoverableUnitImpl
//------------------------------------------------------------------------------
/**
 * <p>
 * Information written to a recovery log is grouped into arbitrary number of
 * discrete blocks called "recoverable units". The RecoverableUnitImpl class
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
 * discrete blocks called "recoverable unit sections". The RecoverableUnitSectionImpl
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
 * This class provides the implementation of the RecoverableUnit interface.
 * </p>
 */
public class RecoverableUnitImpl implements RecoverableUnit
{
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(RecoverableUnitImpl.class,
                                                         TraceConstants.TRACE_GROUP, null);

    /**
     * A record type code inserted into the header information of a recoverable
     * unit when it is written to the recovery log. This type code is a marker
     * to indicate that the record contains information about the recoverable
     * unit that requires recovery.
     */
    protected static final short RECORDTYPENORMAL = 1;

    /**
     * A record type code inserted into the header information of a recoverable
     * unit when its written to the recovery log. This type code is a marker
     * to indicate that the recoverable unit has been deleted.
     */
    protected static final short RECORDTYPEDELETED = 2;

    /**
     * A flag used to indicate that there are no further sections stored in the
     * recoverable unit record on disk.
     */
    private static final int END_OF_SECTIONS = -1;

    /**
     * The size, in bytes, of the "head" and "tail" information needed to encapsulate
     * a recoverable unit written to a record on a persistent storage device.
     */
    private static final int RECORD_HEADER_SIZE = RLSUtils.INT_SIZE + // Serialized FST data length
                                                  RLSUtils.LONG_SIZE + // ID
                                                  RLSUtils.SHORT_SIZE + // type
                                                  RLSUtils.INT_SIZE; // "no more sections" marker (negative id)

    /**
     * The size, in bytes, of the "head" and "tail" information needed to encapsulate
     * a recoverable unit deletion marker written to record on a persistent storage device.
     */
    private static final int REMOVAL_HEADER_SIZE = RLSUtils.INT_SIZE + // Serialized FST data length
                                                   RLSUtils.LONG_SIZE + // ID
                                                   RLSUtils.SHORT_SIZE; // type                                                                                                   

    /**
     * The size, in bytes, of the "head" and "tail" information needed to encapsulate
     * a record on a persistent storate device.
     */
    private static final int TOTAL_HEADER_SIZE = 4 + // "RCRD"
                                                 RLSUtils.LONG_SIZE + // sequence number  
                                                 RLSUtils.INT_SIZE + // record length in bytes                                     
                                                 RECORD_HEADER_SIZE +
                                                 RLSUtils.LONG_SIZE; // tail sequence number;

    /**
     * Constants to identify specific lock get/set pairs. These values must be
     * unique within the RLS code. Rather than use any form of allocation method
     * for these values components use statically defined constants to save
     * CPU cycles. All instances throughout the RLS source should be prefixed
     * with LOCK_REQUEST_ID to assist location and help ensure that no repeats
     * are introduced by mistake. This comment should also be included whereever
     * these values are defined.
     */
    private static final int LOCK_REQUEST_ID_RUI_CREATESECTION = 8;
    private static final int LOCK_REQUEST_ID_RUI_WRITESECTIONS = 9;
    private static final int LOCK_REQUEST_ID_RUI_WRITESECTION = 10;
    private static final int LOCK_REQUEST_ID_RUI_FORCESECTIONS = 11;

    /**
     * The identifier of this RecoverableUnitImpl
     */
    private final long _identity;

    /**
     * A map of RecoverableUnitSection identity to RecoverableInitSection instance.
     * The HashMap provides no ordering and is not synchronized.
     * <p>
     * <ul>
     * <li>String -> RecoverableUnitSection
     * </ul>
     * </p>
     */
    private final java.util.HashMap _recoverableUnitSections;

    /**
     * Flag indicating if this RecoverableUnitImpl holds data that is stored in
     * the underlying recovery log or its assosiated physical media. Note that
     * this flag does not indicate that the data has been forced to the physical
     * media, just written. Corrispondingly, this flag is set to true in
     * writeSection, writeSections, forceSections and recover.
     */
    private boolean _storedOnDisk;

    /**
     * Acccess to the underlying memory mapped recovery log files for the
     * purposes of writing and forcing information to persistent storage.
     */
    private final LogHandle _logHandle;

    /**
     * Lock used to coordinate access to the data structures that make up the
     * in memory recovery log. Operations that wish to access these data
     * structures need to obtain a 'shared' lock. See Lock.java for more
     * information on the locking model.
     */
    private final Lock _controlLock;

    /**
     * The storage mode in use by this recovery log. Default is memory backed.
     */
    private int _storageMode = MultiScopeRecoveryLog.MEMORY_BACKED;

    /**
     * The size, in bytes, required to write all of this recoverable units
     * unwritten recoverable unit sections to persistent storage.
     */
    private int _unwrittenDataSize;

    /**
     * The size, in bytes, required to write, or rewrite, all of this
     * recoverable units recoverable unit sections to persistent storage.
     * This includes both the unwritten recoverable unit sections and
     * those that have already been output to persistent storage.
     */
    private int _totalDataSize;

    /**
     * The RecoveryLog to which this RecoverableUnit belongs.
     */
    private final MultiScopeRecoveryLog _recLog;

    /**
     * The deflated form of the failure scope that this
     * recoverable unit belongs to.
     */
    private final byte[] _deflatedFailureScope;

    /**
     * The failure scope that this recoverable unit belongs
     * to.
     */
    private final FailureScope _failureScope;

    /**
     * The size, in bytes, of the header for a record written
     * by this recoverable unit. This is the RECORD_HEADER_SIZE
     * plus the length, in bytes, of the serialized failure scope.
     */
    private final int _recordHeaderSize;

    /**
     * The size, in bytes, of the header for a removal record written
     * by this recoverable unit. This is the REMOVAL_HEADER_SIZE
     * plus the length, in bytes, of the serialized failure scope.
     */
    private final int _removalHeaderSize;

    /**
     * The size, in bytes, of the total header required to write
     * a record for this recoverable unit to persistent storage. This
     * is the TOTAL_HEADER_SIZE plus the length, in bytes, of the
     * serialized failure scope.
     */
    private final int _totalHeaderSize;

    /**
     * The name of the application server to which this recoverable unit belongs.
     */
    private final String _serverName;

    /**
     * The name of the client service that owns the recoverable unit.
     */
    private final String _clientName;

    /**
     * The version number of the client service that owns the recoverable unit.
     */
    private final int _clientVersion;

    /**
     * The name of the associated recovery log within which this recoverable unit
     * resides.
     */
    private final String _logName;

    /**
     * The identity of the associated recovery log within which this recoverable unit
     * resides.
     */
    private final int _logIdentifier;

    /**
     * The 'traceId' string is output at key trace points to allow easy mapping
     * of recovery log operations to clients logs.
     */
    private String _traceId;

    /**
     * PI68664 - boolean flag to indicate whether we have added payload to log
     * This is NOT volatile because the RUs are recovered on the same thread that
     * reads in the logs and we only use this during deletion during log read.
     * IF someone wants to use this variable at a later stage, it should be made
     * volatile.
     */
    private boolean _payloadAdded = false;

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.RecoverableUnitImpl
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Package access constructor for the creation of recoverable units.
     * </p>
     * 
     * <p>
     * This method should only be called by the RLS itself during either initial
     * creation of a recoverable unit or recreation during server startup.
     * </p>
     * 
     * @param recLog The parent recovery log reference.
     * @param identity The identity of the new recoverable unit (must be unique
     *            within the associated recovery log.)
     * @param failureScope The FailureScope that this recoverable unit should belong to
     * @param logHandle The LogHandle reference that provides access to the
     *            underlying physical recovery log.
     * @param storageMode The required storage mode (defined in the RecoveryLogImpl.java)
     * @param controlLock The lock object, owned by the associated recovery log and used
     *            to coordinate access to it.
     */
    private RecoverableUnitImpl(MultiScopeRecoveryLog recLog, long identity, FailureScope failureScope, LogHandle logHandle, int storageMode, Lock controlLock, boolean recovered)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "RecoverableUnitImpl", new java.lang.Object[] { recLog, new Long(identity), failureScope, logHandle, new Integer(storageMode), controlLock,
                                                                        new Boolean(recovered) });

        // Cache the supplied information
        _deflatedFailureScope = FailureScopeManager.toByteArray(failureScope);
        _failureScope = failureScope;
        _identity = identity;
        _logHandle = logHandle;
        _controlLock = controlLock;
        _recLog = recLog;
        _storageMode = storageMode;

        _recordHeaderSize = RECORD_HEADER_SIZE + _deflatedFailureScope.length;
        _removalHeaderSize = REMOVAL_HEADER_SIZE + _deflatedFailureScope.length;
        _totalHeaderSize = TOTAL_HEADER_SIZE + _deflatedFailureScope.length;

        // Allocate the map used to contain the recoverable unit sections created within
        // the new recoverable unit.
        _recoverableUnitSections = new java.util.HashMap();

        // Cache details about the identity of the associated client / recovery log
        _serverName = recLog.serverName();
        _clientName = recLog.clientName();
        _clientVersion = recLog.clientVersion();
        _logName = recLog.logName();
        _logIdentifier = recLog.logIdentifier();

        _recLog.addRecoverableUnit(this, recovered);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "RecoverableUnitImpl", this);
    }

    RecoverableUnitImpl(MultiScopeRecoveryLog recLog, long identity, FailureScope failureScope, LogHandle logHandle, int storageMode, Lock controlLock)
    {
        this(recLog, identity, failureScope, logHandle, storageMode, controlLock, false);

        if (tc.isEntryEnabled())
            Tr.entry(tc, "RecoverableUnitImpl", new Object[] { recLog, new Long(identity), failureScope, logHandle, new Integer(storageMode), controlLock });
        if (tc.isEntryEnabled())
            Tr.exit(tc, "RecoverableUnitImpl", this);
    }

    RecoverableUnitImpl(MultiScopeRecoveryLog recLog, long identity, FailureScope failureScope, LogHandle logHandle, int storageMode, Lock controlLock, ReadableLogRecord record) throws InternalLogException, LogCorruptedException
    {
        this(recLog, identity, failureScope, logHandle, storageMode, controlLock, true);

        if (tc.isEntryEnabled())
            Tr.entry(tc, "RecoverableUnitImpl", new Object[] { recLog, new Long(identity), failureScope, logHandle, new Integer(storageMode), controlLock, record });

        try
        {
            recover(record);
        } catch (LogCorruptedException lce)
        {
            FFDCFilter.processException(lce, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.RecoverableUnitImpl", "290", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "RecoverableUnitImpl", lce);
            throw lce;
        } catch (InternalLogException ile)
        {
            FFDCFilter.processException(ile, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.RecoverableUnitImpl", "296", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "RecoverableUnitImpl", ile);
            throw ile;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "RecoverableUnitImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.createSection
    //------------------------------------------------------------------------------
    /**
     * Creates a new recoverable unit section.
     * 
     * @param identity Identity of the new recoverable unit section (must be unique
     *            within the recoverable unit)
     * @param singleData Flag indicating if the new recoverable unit section should
     *            retain only a single item of data at any one time. If this
     *            flag is true, only the most recent item of data added to it
     *            is retained and preceeding items of data are thrown away.
     * 
     * @return The new RecoverableUnitSectionImpl instance.
     * 
     * @exception RecoverableUnitSectionExistsException Thrown if a recoverable unit
     *                section already exists with
     *                the supplied identity.
     * @exception InternalLogException An unexpected error has occured.
     */
    @Override
    public RecoverableUnitSection createSection(int identity, boolean singleData) throws RecoverableUnitSectionExistsException, InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSection", new java.lang.Object[] { this, new Integer(identity), new Boolean(singleData) });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed())
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createSection", this);
            throw new InternalLogException(null);
        }

        // Construct a new Integer to wrap the 'id' value in order to use this in the _recoverableUnitSections map.
        Integer sectionId = new Integer(identity);

        RecoverableUnitSectionImpl recoverableUnitSection = null;

        _controlLock.getSharedLock(LOCK_REQUEST_ID_RUI_CREATESECTION);

        if (_recoverableUnitSections.containsKey(sectionId) == false)
        {
            recoverableUnitSection = new RecoverableUnitSectionImpl(_recLog, this, _identity, identity, _controlLock, _logHandle, _storageMode, singleData);
            _recoverableUnitSections.put(sectionId, recoverableUnitSection);
            if (tc.isEventEnabled())
                Tr.event(tc, "RecoverableUnitImpl '" + _identity + "' created a new RecoverableUnitSection with identity '" + identity + "'");
        }
        else
        {
            if (tc.isEventEnabled())
                Tr.event(tc, "RecoverableUnitImpl '" + _identity + "' was unable to create a RecoverableUnitSection with id '" + identity + "' as it already exists");

            try
            {
                _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_CREATESECTION);
            } catch (NoSharedLockException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.createSection", "212", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "createSection", "InternalLogException");
                throw new InternalLogException(exc);
            } catch (Throwable exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.createSection", "218", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "createSection", "InternalLogException");
                throw new InternalLogException(exc);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createSection", "RecoverableUnitSectionExistsException");
            throw new RecoverableUnitSectionExistsException(null);
        }

        try
        {
            _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_CREATESECTION);
        } catch (NoSharedLockException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.createSection", "232", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createSection", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSection", recoverableUnitSection);
        return recoverableUnitSection;
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.removeSection
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Remove a recoverable unit section from the recoverable unit.
     * </p>
     * 
     * <p>
     * The recoverable unit section is no longer considered valid after this
     * call. The client service must not invoke any further methods on it.
     * </p>
     * 
     * <p>
     * The RLS will remove the recoverable unit section from its "in memory" copy of
     * the recovery log and write (but not force) a record of this deletion to
     * underlying recovery log. This means that in the event of a server failure,
     * this recoverable unit section may be reconstructed during recovery processing
     * and client services must be able to cope with this. Any subsequent force
     * operation will ensure that this deletion record is persisted to disk and any
     * subsequent keypoint operation will remove all reference to the recoverable
     * unit from the recovery log.
     * </p>
     * 
     * <p>
     * This method must not be invoked whilst an unclosed LogCursor is held for the
     * recoverable unit sections in this recoverable unit. The
     * <code>LogCursor.remove</code> method should be used instead.
     * </p>
     * 
     * @param identity The identity of the target recoverable unit section.
     * 
     * @exception InvalidRecoverableUnitSectionException The recoverable unit section
     *                does not exist.
     * @exception InternalLogException An unexpected error has occured.
     */
    @Override
    public void removeSection(int identity) throws InvalidRecoverableUnitSectionException, InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeSection", new java.lang.Object[] { this, new Integer(identity) });

        // REQD: Implementation not yet provided. No users of the RLS currently require this operation.

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeSection", "UnsupportedOperationException");
        throw new java.lang.UnsupportedOperationException();
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.lookupSection
    //------------------------------------------------------------------------------
    /**
     * Returns the recoverable unit section previously created with the supplied
     * identity. If no such recoverable unit section exists, this method returns null.
     * 
     * @param identity The identitiy of the required recoverable unit section.
     * 
     * @return The recoverable unit section previously created with the supplied
     *         identity.
     */
    @Override
    public RecoverableUnitSection lookupSection(int identity)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lookupSection", new java.lang.Object[] { this, new Integer(identity) });

        RecoverableUnitSectionImpl recoverableUnitSection = (RecoverableUnitSectionImpl) _recoverableUnitSections.get(new Integer(identity));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lookupSection", recoverableUnitSection);
        return recoverableUnitSection;
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.writeSections
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Writes to the underlying recovery log any information in the recoverable unit
     * sections that has not already been written by a previous call. This ensures
     * that the recovery log contains an up to date copy of the information retained
     * in the target recoverable unit.
     * </p>
     * 
     * <p>
     * The information is written to the underlying recovery log, but not forced
     * through to persisent storage. After this call, the information is not
     * guaranteed to be retrieved during any post-failure recovery processing.
     * To ensure that this information will be recovered, a force operation
     * should be used instead (eg RecoverableUnitImpl.forceSections)
     * </p>
     * 
     * <p>
     * This call my be used as part of an optomization when several recoverable units
     * need to be pushed to disk. For example, the following sequence will ensure that
     * recoverable units 1 through 4 are all persisted to physical storage:-
     * </p>
     * <p>
     * <ul>
     * <li>RecoverableUnit1.writeSections</li>
     * <li>RecoverableUnit2.writeSections</li>
     * <li>RecoverableUnit3.writeSections</li>
     * <li>RecoverableUnit4.forceSections</li>
     * </ul>
     * </p>
     * 
     * <p>
     * This simple version version of the method is exposed on the interfaces and can
     * deligates down to the implementation method.
     * </p>
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    @Override
    public void writeSections() throws InternalLogException
    {
        // Lack of trace or ffdc is deliberate. This method is the external interface
        // for the real writeSections call and as such we don't want to see two entries for the 
        // same method in the trace.
        this.writeSections(false);
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.writeSections
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Writes to the underlying recovery log information from the recoverable unit
     * sections. The amount of information written depends on the input argument
     * 'rewriteRequired'. If this flag is false then only information that has not
     * not previously been written will be passed to the underlying recover log.
     * If this flag is true then all information will be passed to the underlying
     * recovery log. Either way, the the underlying recovery log will contain an up
     * to date copy of the information retained in the target
     * <p>
     * 
     * <p>
     * This extension of the standard writeSections method is required for
     * keypoint support
     * </p>
     * 
     * <p>
     * The information is written to the underlying recovery log, but not forced
     * through to persisent storage. After this call, the information is not
     * guaranteed to be retrieved during any post-failure recovery processing.
     * To ensure that this information will be recovered, a force operation
     * should be used instead (eg RecoverableUnitImpl.forceSections)
     * </p>
     * 
     * <p>
     * This call my be used as part of an optomization when several recoverable units
     * need to be pushed to disk. For example, the following sequence will ensure that
     * recoverable units 1 through 4 are all persisted to physical storage:-
     * </p>
     * <p>
     * <ul>
     * <li>RecoverableUnit1.writeSections(..)</li>
     * <li>RecoverableUnit2.writeSections(..)</li>
     * <li>RecoverableUnit3.writeSections(..)</li>
     * <li>RecoverableUnit4.forceSections(..)</li>
     * </ul>
     * </p>
     * 
     * <p>
     * This internal version of the method is not exposed on the interfaces and can only
     * be called from within the RLS. Client services invoke the simpler version of the
     * method (with no arguments) which deligates down to this method.
     * </p>
     * 
     * @param rewriteRequired Boolean flag indicating if a rewrite is required.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void writeSections(boolean rewriteRequired) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeSections", new java.lang.Object[] { this, new Boolean(rewriteRequired) });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed())
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSections", "InternalLogException");
            throw new InternalLogException(null);
        }

        // If the log was not open then throw an exception
        if (_logHandle == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSections", "InternalLogException");
            throw new InternalLogException(null);
        }

        _controlLock.getSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTIONS);

        // If there is data stored within this recoverable unit that has not yet been
        // persisted to disk or there is existing data and a rewrite is being performed
        // then (re)persist the required data.
        if ((_unwrittenDataSize > 0) || (rewriteRequired && (_totalDataSize > 0)))
        {
            try
            {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Writing recoverable unit '" + _identity + "'");

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Unwritten data size = " + _unwrittenDataSize + " total data size = " + _totalDataSize);

                int requiredRecordSize = _recordHeaderSize;

                if (rewriteRequired)
                {
                    requiredRecordSize += _totalDataSize;
                }
                else
                {
                    requiredRecordSize += _unwrittenDataSize;
                }

                // Obtain a WritableLogRecord that provides direct access to the underlying recovery log.
                // The WritableLogRecord will write the required log record header to the underlying
                // recovery log.
                final WriteableLogRecord logRecord = _logHandle.getWriteableLogRecord(requiredRecordSize);

                // In some situations, there will not be enough space in the underlying recovery to obtain a
                // WritableLogRecord of the required size. The recovery log will need to perform "housekeeping"
                // to clean up the recovery log before this latest record can be written. In such
                // situations, the getWritableLogRecord() will trigger a keypoint operation before returning. 
                // Given that the keypoint operation will actually cause all the information within this
                // recoverable unit to be (re)written to disk, this method need take no further action. This
                // condition is indicated by the return of a null log record.
                if (logRecord != null)
                {
                    writeRecordHeader(logRecord, RECORDTYPENORMAL);

                    // Obtain an iterator that can be used to access each of the recoverable unit sections in turn.
                    Iterator recoverableUnitSectionsIterator = _recoverableUnitSections.values().iterator();

                    while (recoverableUnitSectionsIterator.hasNext())
                    {
                        RecoverableUnitSectionImpl section = (RecoverableUnitSectionImpl) (recoverableUnitSectionsIterator.next());

                        // Now direct the recoverable unit section to write its content. If the recoverable unit 
                        // section has no data to write then this will be a no-op.
                        section.format(rewriteRequired, logRecord);
                    }

                    // Finally write a negative recoverable unit section id to indicate the there are no
                    // more sections.
                    logRecord.putInt(END_OF_SECTIONS);

                    // Tell the WritableLogRecord that we have finished adding recoverable unit sections. This
                    // will cause it to add the appropriate record tail to the underlying recovery log.
                    logRecord.close();

                    // Flag the fact that this recoverable unit has now been written to the underlying recovery log.
                    _storedOnDisk = true;

                    _logHandle.writeLogRecord(logRecord);
                }
            } catch (IOException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSections", "383", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "An unexpected error IO occurred whilst formatting the recovery log buffer", exc);

                _recLog.markFailed(exc); /* @MD19484C */

                try
                {
                    _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTIONS);
                } catch (Throwable exc2)
                {
                    FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSections", "392", this);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "writeSections", "InternalLogException");
                    throw new InternalLogException(exc2);
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "writeSections", "InternalLogException");
                throw new InternalLogException(exc);
            } catch (InternalLogException exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSections", "587", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "An InternalLogException exception occured whilst formatting the recovery log buffer", exc);

                _recLog.markFailed(exc); /* @MD19484C */

                try
                {
                    _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTIONS);
                } catch (Throwable exc2)
                {
                    FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSections", "392", this);
                    // The shared lock release has failed whilst procesing the initial InternalLogExcption failure. Because
                    // this may be a LogFullException (which extends InternalLogException), rather than re-generating the
                    // exception just allow the original to return.
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "writeSections", exc);
                throw exc;
            } catch (Throwable exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSections", "402", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "An unexpected error occurred whilst formatting the recovery log buffer", exc);

                _recLog.markFailed(exc); /* @MD19484C */

                try
                {
                    _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTIONS);
                } catch (Throwable exc2)
                {
                    FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSections", "411", this);
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "writeSections", "InternalLogException");
                    throw new InternalLogException(exc2);
                }

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "writeSections", "InternalLogException");
                throw new InternalLogException(exc);
            }
        }
        else
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "RecoverableUnitImpl has no RecoverableUnitSections that need to be added to the disk record");
        }

        try
        {
            _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTIONS);
        } catch (NoSharedLockException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSections", "474", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSections", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeSections");
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.writeSection
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Writes to the underlying recovery log information from the specified
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
     * should be used instead (eg RecoverableUnitImpl.forceSections)
     * </p>
     * 
     * @param target The target recoverable unit section reference
     * @param unwrittenDataSize The number of bytes of persistent storage that
     *            will be required by the recoverable unit section
     *            to create a persistent record of its unwritten
     *            information.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void writeSection(RecoverableUnitSectionImpl target, int unwrittenDataSize) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeSection", new java.lang.Object[] { this, target, new Integer(unwrittenDataSize) });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed())
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSection", this);
            throw new InternalLogException(null);
        }

        // If the log was not open then throw an exception
        if (_logHandle == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSection", "InternalLogException");
            throw new InternalLogException(null);
        }

        _controlLock.getSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTION);

        try
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Writing recoverable unit '" + target.identity() + "'");

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unwritten data size = " + unwrittenDataSize);

            final int requiredRecordSize = _recordHeaderSize + _unwrittenDataSize;;

            // Obtain a WritableLogRecord that provides direct access to the underlying recovery log.
            // The WritableLogRecord will write the required log record header to the underlying
            // recovery log.
            final WriteableLogRecord logRecord = _logHandle.getWriteableLogRecord(requiredRecordSize);

            // In some situations, there will not be enough space in the underlying recovery to obtain a
            // WritableLogRecord of the required size. The recovery log will need to perform "housekeeping"
            // to clean up the recovery log before this latest record can be written. In such
            // situations, the getWritableLogRecord() will trigger a keypoint operation before returning. 
            // Given that the keypoint operation will actually cause all the information within this
            // recoverable unit to be (re)written to disk, this method need take no further action. This
            // condition is indicated by the return of a null log record.
            if (logRecord != null)
            {
                // Write the records header to disk. This includes the recoverable unit's identity,
                // the failure scope that the unit belongs to, and the record's type.
                writeRecordHeader(logRecord, RECORDTYPENORMAL);

                // Now direct the recoverable unit section to write its content. If the recoverable unit 
                // section has no data to write then this will be a no-op.
                target.format(false, logRecord);

                // Finally write a negative recoverable unit section id to indicate the there are no
                // more sections.
                logRecord.putInt(END_OF_SECTIONS);

                // Tell the WritableLogRecord that we have finished adding recoverable unit sections. This
                // will cause it to add the appropriate record tail to the underlying recovery log.
                logRecord.close();

                // Flag the fact at least part of this recoverable unit has now been written to the 
                // underlying recovery log.
                _storedOnDisk = true;

                _logHandle.writeLogRecord(logRecord);
            }
        } catch (IOException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSection", "755", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error IO occurred whilst formatting the recovery log buffer", exc);

            _recLog.markFailed(exc); /* @MD19484C */

            try
            {
                _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTION);
            } catch (Throwable exc2)
            {
                FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSection", "766", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "writeSection", "InternalLogException");
                throw new InternalLogException(exc2);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSection", "InternalLogException");
            throw new InternalLogException(exc);
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSection", "776", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "An unexpected error occurred whilst formatting the recovery log buffer", exc);

            _recLog.markFailed(exc); /* @MD19484C */

            try
            {
                _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTION);
            } catch (Throwable exc2)
            {
                FFDCFilter.processException(exc2, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSection", "787", this);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "writeSection", "InternalLogException");
                throw new InternalLogException(exc2);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSection", "InternalLogException");
            throw new InternalLogException(exc);
        }

        try
        {
            _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_WRITESECTION);
        } catch (NoSharedLockException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.writeSection", "802", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSection", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeSection");
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.forceSections
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Forces to the underlying recovery log any information in the recoverable unit
     * sections that has not already been written by a previous call. This ensures
     * that the recovery log contains an up to date copy of the information retained
     * in the target recoverable unit.
     * </p>
     * 
     * <p>
     * The information is written to the underlying recovery log and forced
     * through to persisent storage. After this call, the information is
     * guaranteed to be retrieved during any post-failure recovery processing.
     * </p>
     * 
     * <p>
     * This call my be used as part of an optomization when several recoverable units
     * need to be pushed to disk. For example, the following sequence will ensure that
     * recoverable units 1 through 4 are all persisted to physical storage:-
     * </p>
     * <p>
     * <ul>
     * <li>RecoverableUnit1.writeSections</li>
     * <li>RecoverableUnit2.writeSections</li>
     * <li>RecoverableUnit3.writeSections</li>
     * <li>RecoverableUnit4.forceSections</li>
     * </ul>
     * </p>
     * 
     * <p>
     * This simple version version of the method is exposed on the interfaces and can
     * deligates down to the implementation method.
     * </p>
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    @Override
    public void forceSections() throws InternalLogException
    {
        // Lack of trace or exception handling is deliberate. This method is the external interface
        // for the real forceSections call and as such we don't want to see two entries for the 
        // same method in the trace.
        this.forceSections(false);
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.forceSections
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Forces to the underlying recovery log information from the recoverable unit
     * sections. The amount of information written depends on the input argument
     * 'rewriteRequired'. If this flag is false then only information that has not
     * not previously been written will be passed to the underlying recover log.
     * If this flag is true then all information will be passed to the underlying
     * recovery log. Either way, the the underlying recovery log contains an up
     * to date copy of the information retained in the target.
     * <p>
     * 
     * <p>
     * The information is written to the underlying recovery log and forced
     * through to persisent storage. After this call, the information is
     * guaranteed to be retrieved during any post-failure recovery processing.
     * </p>
     * 
     * <p>
     * This call my be used as part of an optomization when several recoverable units
     * need to be pushed to disk. For example, the following sequence will ensure that
     * recoverable units 1 through 4 are all persisted to physical storage:-
     * </p>
     * <p>
     * <ul>
     * <li>RecoverableUnit1.writeSections(..)</li>
     * <li>RecoverableUnit2.writeSections(..)</li>
     * <li>RecoverableUnit3.writeSections(..)</li>
     * <li>RecoverableUnit4.forceSections(..)</li>
     * </ul>
     * </p>
     * 
     * <p>
     * This internal version of the method is not exposed on the interfaces and can only
     * be called from within the RLS. Client services invoke the simpler version of the
     * method (with no arguments) which deligates down to this method.
     * </p>
     * 
     * @param rewriteRequired Boolean flag indicating if a rewrite is required.
     * 
     * @exception InternalLogException An unexpected error has occured.
     */
    void forceSections(boolean rewriteRequired) throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forceSections", new java.lang.Object[] { this, new Boolean(rewriteRequired) });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed())
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", this);
            throw new InternalLogException(null);
        }

        try
        {
            writeSections(rewriteRequired);
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.forceSections", "531", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.forceSections", "537", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", "InternalLogException");
            throw new InternalLogException(exc);
        }

        _controlLock.getSharedLock(LOCK_REQUEST_ID_RUI_FORCESECTIONS);

        try
        {
            _logHandle.force();
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.forceSections", "550", this);
            _recLog.markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.forceSections", "556", this);
            _recLog.markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", "InternalLogException");
            throw new InternalLogException(exc);
        } finally
        {
            try
            {
                _controlLock.releaseSharedLock(LOCK_REQUEST_ID_RUI_FORCESECTIONS);
            } catch (Throwable exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.forceSections", "568", this);
                throw new InternalLogException(exc);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forceSections");
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.sections
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Returns a LogCursor that can be used to itterate through all active
     * recoverable unit sections. The order in which they are returned is not defined.
     * </p>
     * 
     * <p>
     * The LogCursor must be closed when it is no longer needed or its itteration
     * is complete. (See the LogCursor class for more information)
     * </p>
     * 
     * <p>
     * Objects returned by <code>LogCursor.next</code> or <code>LogCursor.last</code>
     * must be cast to type RecoverableUnitSection(Impl).
     * </p>
     * 
     * <p>
     * Care must be taken not remove or add recoverable unit sections whilst the
     * resulting LogCursor is open. Doing so will result in a
     * ConcurrentModificationException being thrown.
     * </p>
     * 
     * <p>
     * If there are no active recoverable unit sections then the resulting LogCursor
     * object will return null from its next() and last() methods, 0 from the initialSize()
     * method and false from hasNext()
     * <p>
     * 
     * @return A LogCursor that can be used to cycle through all active recoverable unit
     *         sections
     */
    @Override
    public LogCursor sections()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sections", this);

        java.util.Collection recoverableUnitSectionsValues = _recoverableUnitSections.values();

        LogCursorImpl cursor = new LogCursorImpl(null, recoverableUnitSectionsValues, false, null);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sections", cursor);

        return cursor;
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.identity
    //------------------------------------------------------------------------------
    /**
     * Returns the identity of this recoverable unit.
     * 
     * @return The identity of this recoverable unit.
     */
    @Override
    public long identity()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "identity", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "identity", new Long(_identity));
        return _identity;
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.recover
    //------------------------------------------------------------------------------
    /**
     * <p>
     * This internal method is called by the RLS to direct the recoverable unit to
     * retrieve recovery data from the underlying recovery log. The ReadableLogRecord
     * supplied by the caller provides direct access to the underlying recovery log.
     * From it, this method can retrieve details of recoverable unit sections and data
     * items that must be ADDED to any already stored in memory.
     * </p>
     * 
     * <p>
     * This method may be called any number of times to complete recovery processing
     * for the target recoverable unit.
     * </p>
     * 
     * <p>
     * This method throws LogCorruptedException to indicate that a failure has occured
     * whilst parsing the data. We assume that this failure has been caused as a result
     * of a previous system crash during a disk write. The calling code will react to
     * this by assmuning that all valid information has now been retireved from the
     * underlying recovery log.
     * </p>
     * 
     * @param logRecord Provides direct access to the underlying recovery log and the
     *            recoverable unit sections / data items that need to be restored.
     * 
     * @exception LogCorruptedException Corrupt log data was detected (see above)
     * @exception InternalLogException An unexpected exception has occured
     */
    private void recover(ReadableLogRecord logRecord) throws LogCorruptedException, InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover", new Object[] { this, logRecord });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed())
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", this);
            throw new InternalLogException(null);
        }

        try
        {
            // Read the record type field. 
            short recordType = logRecord.getShort();

            if (recordType == RecoverableUnitImpl.RECORDTYPEDELETED)
            {
                // This record is a marker to indicate that the recoverable unit was deleted at this point
                // in its lifecycle (ie its lifecycle in relation to the data contained in the recovery
                // log before and after the deletion record). In order to support re-use of a recoverable
                // unit identity before a keypoint operaiton occurs and old data is deleted from the recovery
                // log, we must delete the existing recoverable unit. This ensures that it does not get
                // confused with later instance that uses the same identity value.

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is a DELETION record. Deleting RecoverableUnit from map");
                _recLog.removeRecoverableUnitMapEntries(_identity);
                // PI68664 - if the RU being deleted contains any RU Sections then the DataItem recovery will have
                // caused payload to be added which increments the _totalDataSize of the log.
                // We must set this back down here.
                this.removeDuringLogRead();

            }
            else
            {
                // This record is not a deletion record. It contains new data to be recovered for the recoverable
                // unit. Decode the record accordingly.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "This is a NORMAL record. Decoding contents");

                // Determine the identity of the next section. Ideally, we would decode the entire
                // recoverable unit section from within the RecoverableUnitSectionImpl class, rather
                // than decoding part of it here. Unfortunatly, we must determine if this class 
                // already knows about this recoverable unit section and if not create it and place
                // it into the _recoverableUnitSections map. This means that we must decode both its
                // identity and 'singleData' flag.
                int recoverableUnitSectionIdentity = logRecord.getInt();

                while (recoverableUnitSectionIdentity != END_OF_SECTIONS)
                {
                    // This is a real recoverable unit section record and not just the marker
                    // to indicate that there are no further recoverable unit sections stored
                    // within the record.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Recovering RecoverableUnitSection '" + recoverableUnitSectionIdentity + "'");

                    // Read the 'record type' field. Currently this is not used but is provided for future compatibility.
                    // It would be used to distinguish between a 'normal' write and a 'delete' write. The latter would be used
                    // to remove the recoverable unit section from the recovery log. Ignore this field for now.
                    logRecord.getShort();

                    // Determine if this section can hold multiple data items.
                    final boolean singleData = logRecord.getBoolean();

                    if (tc.isDebugEnabled())
                    {
                        if (singleData)
                        {
                            Tr.debug(tc, "RecoverableUnitSection can hold only a single data item");
                        }
                        else
                        {
                            Tr.debug(tc, "RecoverableUnitSection can hold multiple data items");
                        }
                    }

                    // Determine if the identity has been encountered before and either lookup or create
                    // the corrisponding recoverable unit section.
                    RecoverableUnitSectionImpl recoverableUnitSection = (RecoverableUnitSectionImpl) _recoverableUnitSections.get(new Integer(recoverableUnitSectionIdentity));

                    if (recoverableUnitSection == null)
                    {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "RecoverableUnitSection " + recoverableUnitSectionIdentity + " has not been encountered before. Creating.");

                        try
                        {
                            recoverableUnitSection = (RecoverableUnitSectionImpl) createSection(recoverableUnitSectionIdentity, singleData);
                        } catch (RecoverableUnitSectionExistsException exc)
                        {
                            // This exception should not be generated in practice as we are in the single threaded
                            // recovery process and have already checked that the RecoverableUnitSection does not
                            // exist. If this exception was actually generated then ignore it - it simply indicates
                            // that the creation has failed as the section has already been created. Given that
                            // creation is the goal, this does not seem to be a problem.
                            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "713", this);
                        } catch (InternalLogException exc)
                        {
                            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "717", this);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "An unexpected exception occured when attempting to create a new RecoverableUnitSection");
                            throw exc; // Caught in this method further down.
                        }
                    }
                    else
                    {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "RecoverableUnitSection " + recoverableUnitSectionIdentity + " has been encountered before.");
                    }

                    // Direct the recoverable unit section to recover further information from the log record.
                    recoverableUnitSection.recover(logRecord);

                    // Since this information has been recovered from disk it has been "written to the log". Record this fact
                    // so that any subsequent deletion of the recoverable unit will cause a deletion record to be written
                    // to disk.
                    _storedOnDisk = true;

                    // Retrieve the identity of the next recoverable unit section. This may be the 'END_OF_SECTIONS'
                    // marker and hence indicate that there are no further recoverable unit sections to process
                    // in this record.
                    recoverableUnitSectionIdentity = logRecord.getInt();
                }
            }
        } catch (LogCorruptedException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "740", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "A LogCorruptedException exception occured reconstructng a RecoverableUnitImpl");
            _recLog.markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", exc);
            throw exc;
        } catch (InternalLogException exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "747", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "An InternalLogException exception occured reconstructng a RecoverableUnitImpl");
            _recLog.markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", exc);
            throw exc;
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "753", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "An exception occured reconstructng a RecoverableUnitImpl");
            _recLog.markFailed(exc); /* @MD19484C */
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover");
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.removeDuringLogRead
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the recoverable unit that it is being deleted during initial read of the logs.
     * This method DOES NOT write anything to the logs it is merely used to discard any payload
     * associated with the RU from the totalDataSize of the log.
     * </p>
     * 
     * <p>
     * There is NO NEED to hold the shared lock before invoking this method because we only call this
     * while opening the logs - this method is private and should not be called at ANY other point.
     * </p>
     * 
     */
    private void removeDuringLogRead()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeDuringLogRead", new Object[] { this, String.valueOf(_payloadAdded) });

        // Inform the recovery log of the reduction in the data payload due to this unit being deleted.
        // We should not have any unwritten data at this juncture BUT play it safe - do what remove does.
        // The reason we don't simply call remove is partly because _storedOnDisk will be true and we
        // don't want to write out another deletion record.  More significant is that it is possible that
        // the RU hasn't actually added any payload - that will only have happened if a DataItem in a RU Section
        // has been processed for this RU so we check this via new instance variable _payloadAdded

        if (_payloadAdded)
        {
            if (_unwrittenDataSize > 0)
            {
                _recLog.payloadDeleted(_totalDataSize + _totalHeaderSize, _unwrittenDataSize + _totalHeaderSize);
            }
            else
            {
                _recLog.payloadDeleted(_totalDataSize + _totalHeaderSize, _unwrittenDataSize);
            }
        }
        // Next, "forget" all stored recoverable unit sections. See comments in remove method but in reality
        // since nothing holds a reference to this RU anymore this just helps garbage collection.
        if (tc.isEventEnabled())
            Tr.event(tc, "Remove during log read completed for recoverable unit " + _identity + ". Clear internal state");
        _recoverableUnitSections.clear();
        _totalDataSize = 0;
        _unwrittenDataSize = 0;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeDuringLogRead", this);
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.remove
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the recoverable unit that it is being deleted. This methods writes a
     * special record to the underlying recovery log that indicates this event occured,
     * and allows 'old' information to be ignored during recovery.
     * </p>
     * 
     * <p>
     * Caller MUST hold the shared lock before invoking this method.
     * </p>
     * 
     * @exception InternalLogException An unexpected exception has occured
     */
    void remove() throws InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "remove", this);

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed())
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "remove", this);
            throw new InternalLogException(null);
        }

        if (_storedOnDisk)
        {
            // There is information relating to this recoverable unit stored in the underlying
            // recovery log. We must write a deletion record to indicate that this is no longer
            // valid.
            try
            {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Creating deletion record for recoverable unit '" + _identity + "'");

                final WriteableLogRecord logRecord = _logHandle.getWriteableLogRecord(_removalHeaderSize);

                // A null log record returned by the log handle indicates that the request to get
                // a new writeable log record resulted in a keypoint occuring. As this recoverable
                // unit has already been removed from the map it will not have been written as part
                // of the keypoint process and no longer exists in the log. Therefore there is no
                // need to write the deletion record for this recoverable unit.
                if (logRecord != null)
                {
                    writeRecordHeader(logRecord, RECORDTYPEDELETED);

                    // Tell the WritableLogRecord that we have finished building the removal record. This
                    // will cause it to add the appropriate record tail to the underlying recovery log.
                    logRecord.close();

                    _logHandle.writeLogRecord(logRecord);
                }
            } catch (Throwable exc)
            {
                FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.remove", "801", this);
                if (tc.isEventEnabled())
                    Tr.event(tc, "An unexpected error occurred whilst formatting the recovery log buffer");
                _recLog.markFailed(exc); /* @MD19484C */
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "remove", "InternalLogException");
                throw new InternalLogException(exc);
            }
        }
        else
        {
            // There is no trace of this recoverable unit in the underlying recovery log and so there is
            // no further action to take.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Not writing deletion record because RecoverableUnit '" + _identity + "' is not on disk");
        }

        // Inform the recovery log of the reduction in the data payload due to this unit being deleted.
        if (_unwrittenDataSize > 0)
        {
            _recLog.payloadDeleted(_totalDataSize + _totalHeaderSize, _unwrittenDataSize + _totalHeaderSize);
        }
        else
        {
            _recLog.payloadDeleted(_totalDataSize + _totalHeaderSize, _unwrittenDataSize);
        }

        // Next, "forget" all stored recoverable unit sections. This will ensure that no further
        // reference to this recoverable unit can be written to disk even if the client service
        // invokes a write or force method on it in the future. We also need to clear out the 
        // total and unwritten data size fields to ensure that we don't attempt to begin
        // writing even when there are no sections to write.
        if (tc.isEventEnabled())
            Tr.event(tc, "Remove completed for recoverable unit " + _identity + ". Clear internal state");
        _recoverableUnitSections.clear();
        _totalDataSize = 0;
        _unwrittenDataSize = 0;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "remove");
    }

    //------------------------------------------------------------------------------
    // Method: RecoveryUnitImpl.payloadAdded
    //------------------------------------------------------------------------------
    /**
     * Informs the recoverable unit that data has been added to one of its recoverable
     * unit sections. The recoverable unit must use the supplied information to track
     * the amount of active data that it holds. This information must be passed to its
     * parent recovery log in order that it may track the amount of active data in
     * the entire recovery log.
     * 
     * This call is driven by recoverable unit section to which data has been
     * added and accounts for the additional data and header fields necessary to form
     * a persistent record of the new data item.
     * 
     * This data has not yet been written to persistent storage and must therefour be
     * tracked in both total and unwritten data size fields. It is important to
     * understand why two parameters are required on this call rather than a single
     * value that could be reflected in both fields. The following example can be used
     * to illustrate the reason for this. Consider that data items of sizes D1, D2
     * and D3 have been added to an initially empty recoverable unit. If H represents the
     * size of all the header information that the underlying recoverable unit sections
     * and data items will need to form a persistent record of the data, then the
     * unwritten and total data size fields will be made up as follows:
     * 
     * unwritten total
     * D3 D3
     * D2 D2
     * D1 D1
     * H H
     * 
     * Once this information has been written to disk, D1,D2 and D3 will be deducted
     * from the unwritten total (see payloadWritten) whilst the total data size remains
     * unchanged. Because there is then no further unwritten information there is no
     * requirement for header H, so it also is removed as follows:-
     * 
     * unwritten total
     * D3
     * D2
     * D1
     * - H
     * 
     * Consider that a new data item of size D4 is added. We need to add D4 + H to the
     * unwritten field and D4 alone to the total field. Since the caller takes care of
     * the size of H anyway, we need two parameters to contain this level of detail.
     * At this point the makeup of these two fields is as follows:-
     * 
     * unwritten total
     * D4
     * D3
     * D2
     * D4 D1
     * H H
     * 
     * @param unwrittenPayloadSize The additional number of bytes that would be needed
     *            to form a persistent record of the new data item
     *            that has been added within this recoverable unit when
     *            a writeSections or forceSections operation is driven
     *            by the client service.
     * @param totalPayloadSize The additional number of bytes that would be needed
     *            to form a persistent record of the new data item
     *            that has been added within this recovery log when a
     *            keypoint operation occurs.
     */
    protected void payloadAdded(int unwrittenPayloadSize, int totalPayloadSize)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "payloadAdded", new Object[] { this, new Integer(unwrittenPayloadSize), new Integer(totalPayloadSize) });

        _payloadAdded = true;

        int unwrittenRecLogAdjustment = unwrittenPayloadSize;
        int totalRecLogAdjustment = totalPayloadSize;

        // When adding new payload, if there was currently no unwritten data then we will need to
        // an additional header in order to be able to contain this information. When we pass on
        // this payload adjustment we must account for the header size.
        if (_unwrittenDataSize == 0)
        {
            unwrittenRecLogAdjustment += _totalHeaderSize;
        }

        // When adding new payload, if there was currently no written data then we will need to
        // an additional header in order to be able to contain this information. When we pass on
        // this payload adjustment we must account for the header size.
        if (_totalDataSize == 0)
        {
            totalRecLogAdjustment += _totalHeaderSize;
        }

        // Track the payload increases directly. We take no account for this classes header values
        // in these figures.
        _unwrittenDataSize += unwrittenPayloadSize;
        _totalDataSize += totalPayloadSize;

        // Pass on the payload adjustment to the parent class.
        _recLog.payloadAdded(unwrittenRecLogAdjustment, totalRecLogAdjustment);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize + " totalDataSize = " + _totalDataSize);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "payloadAdded");
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.payloadWritten
    //------------------------------------------------------------------------------
    /**
     * Informs the recoverable unit that previously unwritten data has been written to
     * disk by one of its recoverable unit sections and no longer needs to be tracked
     * in the unwritten data field. The recovery log must use the supplied information
     * to track the amount of unwritten active data it holds. This information must be
     * passed to its parent recovery log in order that it may track the amount of
     * unwritten data in the entire recovery log.
     * 
     * This call is driven by the recoverable unit section from which data has been
     * written and accounts for both the data and header fields necessary to write the
     * data completly.
     * 
     * Writing data in this manner will not change the total amount of active data
     * contained by the recoverable unit so only the unwritten data size will be effected.
     * The following example can be used to illustrate this. Consider that data items of
     * sizes D1, D2 and D3 have been added to an initially empty recoverable unit
     * (see payloadAdded). If H represents the size of all the header information
     * that the underlying recoverable unit sections and data items will need to form
     * a persistent record of the data, then the unwritten and total data size fields
     * will be made up as follows:
     * 
     * unwritten total
     * D3 D3
     * D2 D2
     * D1 D1
     * H H
     * 
     * Suppose that the data item corrisponding to D2 has been written to disk. D2 + h2
     * (where h2 is any component of H that will no longer be required to form a
     * persistent record of the unwritten data) will be removed from the unwritten total
     * so we have:-
     * 
     * unwritten total
     * D3
     * D3 D2
     * D1 D1
     * H-h2 H
     * 
     * If the remaining data items are also written it should be clear that D3+h3 + D1+h1
     * bytes will also be removed from the unwritten total leaving it at zero. Also that
     * h1 + h2 + h3 = H.
     * 
     * @param payloadSize The number of bytes that no longer need to be written in order
     *            to form a persistent record of the remaining unwritten data items
     *            when a writeSections or forceSections operation is driven by the
     *            client service.
     */
    protected void payloadWritten(int payloadSize)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "payloadWritten", new Object[] { this, new Integer(payloadSize) });

        // Track the unwritten payload decrease directly. We take no account for this classes header
        // values in this figure. The total payload remains unchanged since we are not removing the
        // corresponding payload, just writing it to the underlying recovery log.
        _unwrittenDataSize -= payloadSize;

        // When writing existing payload, if the resulting unwritten data size has gone back down to 
        // zero then there will be no further need to account for the unwritten data header.
        // When we pass on this payload adjustment we must account for the header size.
        if (_unwrittenDataSize == 0)
        {
            _recLog.payloadWritten(payloadSize + _totalHeaderSize);
        }
        else
        {
            _recLog.payloadWritten(payloadSize);
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize + " totalDataSize = " + _totalDataSize);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "payloadWritten");
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.payloadDeleted
    //------------------------------------------------------------------------------
    /**
     * Informs the recoverable unit that data has been removed from one of its recoverable
     * unit sections. At present this method is only invoked as a result of the
     * SingleDataItem class removing existing payload before adding back additional
     * payload for its replacement data data.
     * 
     * The recoverable unit must use the supplied information to track the amount of
     * active data it holds. This information must be passed to its parent recovery log
     * in order that it may track the amount of active data in the entire recovery log.
     * 
     * This call is driven by the recoverable unit section from which data has been
     * removed and accounts for both the data and header fields that would have been
     * necessary to form a persistent record of this data content.
     * 
     * This data may or may not have been written to persistent storage and must
     * therefour be tracked in both total and unwritten data size fields. It is important
     * to understand why two parameters are required on this call rather than a single
     * value that could be reflected in both fields. The following example can be used
     * to illustrate the reason for this. Consider that a data item of size D1 has
     * been added to a recoverable unit section contained within this recoverable unit
     * and that this recoverable unit section may hold only a single data item. If
     * H represents the size of all the header information that the recoverable unit
     * section will need to form a persistent record of the data, then the unwritten
     * and total data size fields will be made up as follows:
     * 
     * unwritten total
     * D1 D1
     * H H
     * 
     * If this information is then written to disk, D1+H will be deducted from the
     * unwritten total (see payloadWritten) whilst the total data size remains
     * unchanged.
     * 
     * unwritten total
     * D1
     * - H
     * 
     * If D1 is subsequently deleted, the total will need to be reduced but the unwritten
     * field will remian unchanged. Since it is the callers responsibility to determine
     * the amount that needs to be removed from each, two arguments are required.
     * 
     * @param unwrittenPayloadSize The number of bytes that will no longer be required
     *            to form a persistent record of the recoverable unit
     *            when either the writeSections or forceSections
     *            operation is driven by the client service.
     * @param totalPayloadSize The number of bytes that will no longer be required
     *            to form a persistent record of the recoverable unit
     *            next time a keypoint operation occurs.
     */
    protected void payloadDeleted(int totalPayloadSize, int unwrittenPayloadSize)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "payloadDeleted", new Object[] { this, new Integer(totalPayloadSize), new Integer(unwrittenPayloadSize) });

        // Track the payload decreases directly. We take no account for this classes header values
        // in these figures.
        _totalDataSize -= totalPayloadSize;
        _unwrittenDataSize -= unwrittenPayloadSize;

        // When removing existing payload, if the resulting unwritten data size has gone back down to 
        // zero then there will be no further need to account for the unwritten data header.
        // When we pass on this payload adjustment we must account for the header size.
        if (_unwrittenDataSize == 0 && (unwrittenPayloadSize != 0))
        {
            unwrittenPayloadSize += _totalHeaderSize;
        }

        // When removing existing payload, if the resulting written data size has gone back down to 
        // zero then there will be no further need to account for the written data header.
        // When we pass on this payload adjustment we must account for the header size.
        if (_totalDataSize == 0)
        {
            totalPayloadSize += _totalHeaderSize;
        }

        // Pass on the payload adjustment to the parent class.
        _recLog.payloadDeleted(totalPayloadSize, unwrittenPayloadSize);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "unwrittenDataSize = " + _unwrittenDataSize + " totalDataSize = " + _totalDataSize);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "payloadDeleted");
    }

    protected static void recover(MultiScopeRecoveryLog recoveryLog, ReadableLogRecord record, LogHandle logHandle, int storageMode, Lock controlLock) throws LogCorruptedException, InternalLogException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover", new Object[] { recoveryLog, record, logHandle, new Integer(storageMode), controlLock });

        // First, read the length field of the serialized failure
        // scope to which this recoverable unit section belongs.
        final int failureScopeLength = record.getInt();

        // Create a byte[] that contains the serialized failure scope
        final byte[] failureScopeBytes = new byte[failureScopeLength];
        record.get(failureScopeBytes);

        try
        {
            // Inflate the FailureScope          
            final FailureScope failureScope = FailureScopeManager.toFailureScope(failureScopeBytes);

            // Read the RecoverableUnit identity. 
            final long recoverableUnitIdentity = record.getLong();

            RecoverableUnitImpl recoverableUnit = recoveryLog.getRecoverableUnit(recoverableUnitIdentity);

            if (recoverableUnit == null)
            {
                recoverableUnit = new RecoverableUnitImpl(recoveryLog, recoverableUnitIdentity, failureScope, logHandle, storageMode, controlLock, record);
            }
            else
            {
                recoverableUnit.recover(record);
            }
        } catch (LogCorruptedException lce)
        {
            FFDCFilter.processException(lce, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "1604");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", lce);
            throw lce;
        } catch (InternalLogException ile)
        {
            FFDCFilter.processException(ile, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "1608");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", ile);
            throw ile;
        } catch (Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.RecoverableUnitImpl.recover", "1612");

            if (tc.isEventEnabled())
                Tr.event(tc, "Unexpected exception caught", e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "recover", "InternalLogException");
            throw new InternalLogException(e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover");
    }

    private void writeRecordHeader(WriteableLogRecord logRecord, short recordType)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeRecordHeader", new Object[] { logRecord, new Short(recordType), this });

        // Firstly, write the length of the serialized
        // FailureScope to which this recoverable unit belongs. 
        logRecord.putInt(_deflatedFailureScope.length);
        logRecord.put(_deflatedFailureScope);

        // Next, write the identity of this recoverable unit
        logRecord.putLong(_identity);

        // Next, write the record type field indicating that this is a normal write record
        logRecord.putShort(recordType);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeRecordHeader");
    }

    protected FailureScope failureScope()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "failureScope", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "failureScope", _failureScope);
        return _failureScope;
    }

    //------------------------------------------------------------------------------
    // Method: RecoverableUnitImpl.toString
    //------------------------------------------------------------------------------
    /**
     * Returns the string representation of this object instance.
     * 
     * @return String The string representation of this object instance.
     */
    @Override
    public String toString()
    {
        if (_traceId == null)
            // Now establish a 'traceId' string. This is output at key trace points to allow
            // easy mapping of recovery log operations to clients logs.
            _traceId = "RecoverableUnitImpl:" + "serverName=" + _serverName + ":"
                       + "clientName=" + _clientName + ":"
                       + "clientVersion=" + _clientVersion + ":"
                       + "logName=" + _logName + ":"
                       + "logIdentifier=" + _logIdentifier + " @"
                       + System.identityHashCode(this);

        return _traceId;
    }
}

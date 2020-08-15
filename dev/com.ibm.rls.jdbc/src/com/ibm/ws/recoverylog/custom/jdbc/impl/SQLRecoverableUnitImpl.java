/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.custom.jdbc.impl;

import java.util.Iterator;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.FailureScopeManager;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.InvalidRecoverableUnitSectionException;
import com.ibm.ws.recoverylog.spi.LogCursor;
import com.ibm.ws.recoverylog.spi.LogCursorImpl;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSectionExistsException;
import com.ibm.ws.recoverylog.spi.TraceConstants;

//------------------------------------------------------------------------------
// Class: SQLRecoverableUnitImpl
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
public class SQLRecoverableUnitImpl implements RecoverableUnit {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(SQLRecoverableUnitImpl.class,
                                                         TraceConstants.TRACE_GROUP, null);

    /**
     * The identifier of this SQLRecoverableUnitImpl
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
    private final java.util.HashMap<Integer, SQLRecoverableUnitSectionImpl> _recoverableUnitSections;

    /**
     * The RecoveryLog to which this RecoverableUnit belongs.
     */
    private final SQLMultiScopeRecoveryLog _recLog;

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
    private final String _traceId;

    /**
     * whether this RU was recovered from the logs
     */
    final boolean _recovered;

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.SQLRecoverableUnitImpl
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
     * @param recovered A flag to indicate whether this object is already persisted
     */
    public SQLRecoverableUnitImpl(SQLMultiScopeRecoveryLog recLog, long identity, FailureScope failureScope, boolean recovered) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "SQLRecoverableUnitImpl", new java.lang.Object[] { recLog, new Long(identity), failureScope, new Boolean(recovered) });

        // Cache the supplied information
        _deflatedFailureScope = FailureScopeManager.toByteArray(failureScope);
        _failureScope = failureScope;
        _identity = identity;
        _recLog = recLog;
        _recovered = recovered;

        // Allocate the map used to contain the recoverable unit sections created within
        // the new recoverable unit.
        _recoverableUnitSections = new java.util.HashMap<Integer, SQLRecoverableUnitSectionImpl>();

        _recLog.addRecoverableUnit(this, recovered);

        // Cache details about the identity of the associated client / recovery log
        _serverName = recLog.serverName();
        _clientName = recLog.clientName();
        _clientVersion = recLog.clientVersion();
        _logName = recLog.logName();
        _logIdentifier = recLog.logIdentifier();

        // Now establish a 'traceId' string. This is output at key trace points to allow
        // easy mapping of recovery log operations to clients logs.
        _traceId = "SQLRecoverableUnitImpl:" + "serverName=" + _serverName + ":"
                   + "clientName=" + _clientName + ":"
                   + "clientVersion=" + _clientVersion + ":"
                   + "logName=" + _logName + ":"
                   + "logIdentifier=" + _logIdentifier + " @"
                   + System.identityHashCode(this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "SQLRecoverableUnitImpl", this);
    }

    SQLRecoverableUnitImpl(SQLMultiScopeRecoveryLog recLog, long identity, FailureScope failureScope) {
        this(recLog, identity, failureScope, false);

        if (tc.isEntryEnabled())
            Tr.entry(tc, "SQLRecoverableUnitImpl", new Object[] { recLog, new Long(identity), failureScope });
        if (tc.isEntryEnabled())
            Tr.exit(tc, "SQLRecoverableUnitImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.createSection
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
    public RecoverableUnitSection createSection(int identity, boolean singleData) throws RecoverableUnitSectionExistsException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSection", new java.lang.Object[] { this, new Integer(identity), new Boolean(singleData) });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "createSection", this);
            throw new InternalLogException(null);
        }

        // Construct a new Integer to wrap the 'id' value in order to use this in the _recoverableUnitSections map.
        Integer sectionId = new Integer(identity);

        SQLRecoverableUnitSectionImpl recoverableUnitSection = null;

        if (_recoverableUnitSections.containsKey(sectionId) == false) {
            recoverableUnitSection = new SQLRecoverableUnitSectionImpl(_recLog, this, _identity, identity, singleData);
            _recoverableUnitSections.put(sectionId, recoverableUnitSection);
            if (tc.isEventEnabled())
                Tr.event(tc, "SQLRecoverableUnitImpl '" + _identity + "' created a new RecoverableUnitSection with identity '" + identity + "'");
        } else {
            if (tc.isEventEnabled())
                Tr.event(tc, "SQLRecoverableUnitImpl '" + _identity + "' was unable to create a RecoverableUnitSection with id '" + identity + "' as it already exists");

            if (tc.isEntryEnabled())
                Tr.exit(tc, "createSection", "RecoverableUnitSectionExistsException");
            throw new RecoverableUnitSectionExistsException(null);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSection", recoverableUnitSection);
        return recoverableUnitSection;
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.removeSection
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
    public void removeSection(int identity) throws InvalidRecoverableUnitSectionException, InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeSection", new java.lang.Object[] { this, new Integer(identity) });

        // REQD: Implementation not yet provided. No users of the RLS currently require this operation.

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeSection", "UnsupportedOperationException");
        throw new java.lang.UnsupportedOperationException();
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.lookupSection
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
    public RecoverableUnitSection lookupSection(int identity) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "lookupSection", new java.lang.Object[] { this, new Integer(identity) });

        SQLRecoverableUnitSectionImpl recoverableUnitSection = _recoverableUnitSections.get(new Integer(identity));

        if (tc.isEntryEnabled())
            Tr.exit(tc, "lookupSection", recoverableUnitSection);
        return recoverableUnitSection;
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.writeSections
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
     * should be used instead (eg SQLRecoverableUnitImpl.forceSections)
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
    public void writeSections() throws InternalLogException {
        // Lack of trace or ffdc is deliberate. This method is the external interface
        // for the real writeSections call and as such we don't want to see two entries for the
        // same method in the trace.
        this.writeSections(false);
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.writeSections
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
     * should be used instead (eg SQLRecoverableUnitImpl.forceSections)
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
    void writeSections(boolean rewriteRequired) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writeSections", new java.lang.Object[] { this, new Boolean(rewriteRequired) });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "writeSections", "InternalLogException");
            throw new InternalLogException(null);
        }

        // Obtain an iterator that can be used to access each of the recoverable unit sections in turn.
        Iterator<SQLRecoverableUnitSectionImpl> recoverableUnitSectionsIterator = _recoverableUnitSections.values().iterator();

        _recLog.writeSections(recoverableUnitSectionsIterator);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "writeSections");
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.forceSections
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
    public void forceSections() throws InternalLogException {
        // Lack of trace or exception handling is deliberate. This method is the external interface
        // for the real forceSections call and as such we don't want to see two entries for the
        // same method in the trace.
        this.forceSections(false);
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.forceSections
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
    void forceSections(boolean rewriteRequired) throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forceSections", new java.lang.Object[] { this, new Boolean(rewriteRequired) });

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", this);
            throw new InternalLogException(null);
        }

        try {
            writeSections(rewriteRequired);
            _recLog.forceSections();
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLRecoverableUnitImpl.forceSections", "531", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", exc);
            throw exc;
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.SQLRecoverableUnitImpl.forceSections", "537", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "forceSections", "InternalLogException");
            throw new InternalLogException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forceSections");
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.sections
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
    public LogCursor sections() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "sections", this);

        java.util.Collection recoverableUnitSectionsValues = _recoverableUnitSections.values();

        LogCursorImpl cursor = new LogCursorImpl(null, recoverableUnitSectionsValues, false, null);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "sections", cursor);

        return cursor;
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.identity
    //------------------------------------------------------------------------------
    /**
     * Returns the identity of this recoverable unit.
     *
     * @return The identity of this recoverable unit.
     */
    @Override
    public long identity() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "identity", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "identity", new Long(_identity));
        return _identity;
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.remove
    //------------------------------------------------------------------------------
    /**
     * <p>
     * Informs the recoverable unit that it is being deleted. This methods writes a
     * special record to the underlying recovery log that indicates this event occured,
     * and allows 'old' information to be ignored during recovery.
     * </p>
     *
     * @exception InternalLogException An unexpected exception has occured
     */
    void remove() throws InternalLogException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "remove", this);

        // If the parent recovery log instance has experienced a serious internal error then prevent
        // this operation from executing.
        if (_recLog.failed()) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "remove", this);
            throw new InternalLogException(null);
        }

        // Next, "forget" all stored recoverable unit sections. This will ensure that no further
        // reference to this recoverable unit can be written to disk even if the client service
        // invokes a write or force method on it in the future. We also need to clear out the
        // total and unwritten data size fields to ensure that we don't attempt to begin
        // writing even when there are no sections to write.
        if (tc.isEventEnabled())
            Tr.event(tc, "Remove completed for recoverable unit " + _identity + ". Clear internal state");
        _recoverableUnitSections.clear();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "remove");
    }

    protected FailureScope failureScope() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "failureScope", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "failureScope", _failureScope);
        return _failureScope;
    }

    //------------------------------------------------------------------------------
    // Method: SQLRecoverableUnitImpl.toString
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

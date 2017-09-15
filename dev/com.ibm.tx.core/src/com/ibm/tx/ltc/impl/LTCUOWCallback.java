/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.ltc.impl;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeLTCAware;

public class LTCUOWCallback implements UOWScopeCallback // Defect 130321
{
    private static final TraceComponent tc = Tr.register(LTCUOWCallback.class, TranConstants.TRACE_GROUP, TranConstants.LTC_NLS_FILE);

    protected ThreadLocal<Byte> _beginContext = new ThreadLocal<Byte>(); // Defect 122348.1 PM25461
    protected ThreadLocal<Byte> _endContext = new ThreadLocal<Byte>(); // Defect 122348.1 PM25461

    protected static final LocalTransactionCurrent _ltCurrent = LocalTranCurrentSet.instance();

    protected int _uowType;

    public static final int UOW_TYPE_TRANSACTION = 0;
    // private static final int UOW_TYPE_ACTIVITYSESSION = 1;

    protected static LTCUOWCallback userTranCallback;

    // may be different in derived classes
    public static UOWScopeCallback createUserTransactionCallback() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createUserTransactionCallback");

        if (userTranCallback == null) {
            userTranCallback = new LTCUOWCallback(UOW_TYPE_TRANSACTION);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createUserTransactionCallback", userTranCallback);
        return userTranCallback;
    }

    public static UOWScopeCallback getUserTransactionCallback() {
        return LTCUOWCallback.createUserTransactionCallback();
    }

    public LTCUOWCallback(int uowType) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "LTCUOWCallback", uowType);

        _uowType = uowType;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "LTCUOWCallback", this);
    }

    /*
     * Notification from UserTransaction or UserActivitySession interface implementations
     * that the state of a bean-managed UOW has changed. As a result of this bean-managed
     * UOW change we may have to change the LTC that's on the thread.
     */
    @Override
    public void contextChange(int typeOfChange, UOWScope scope) throws IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "contextChange", new Object[] { typeOfChange, scope, this });

        try {
            // Determine the Tx change type and process.  Ensure we do what
            // we need to do as close to the context switch as possible
            switch (typeOfChange) {
                case PRE_BEGIN:
                    uowPreBegin();
                    break;
                case POST_BEGIN:
                    uowPostBegin(scope);
                    break;
                case PRE_END:
                    uowPreEnd(scope);
                    break;
                case POST_END:
                    uowPostEnd();
                    break;
                default:
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Unknown typeOfChange: " + typeOfChange);
            }
        } catch (IllegalStateException ise) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "contextChange", ise);
            throw ise;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "contextChange");
    }

    protected void uowPreBegin() throws IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "uowPreBegin", this);

        // Get the current Local Transaction
        final LocalTranCoordImpl ltc = (LocalTranCoordImpl) _ltCurrent.getLocalTranCoord();

        if (ltc != null) {

            // Reset the thread level data each time through so we can detect
            // that LTC was ended and what its boundary was on the post begin
            // callback. 
            // RTC 174266: set the context to null in all cases where the ltc is not null.
            _beginContext.set(null);

            if (ltc.hasWork()) {
                if (_uowType == UOW_TYPE_TRANSACTION) {
                    Tr.error(tc, "ERR_BEGIN_GLOBAL_TX");
                }

                final IllegalStateException ise = new IllegalStateException("Cannot start a new UOW. A LocalTransactionContainment is already active with work.");

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "transactionPreBegin", ise);
                throw ise;
            }

            try {
                // Complete the empty LTC before the new UOW is begun
                ltc.complete(LocalTransactionCoordinator.EndModeRollBack);

                // Use thread local storage to record that fact that we've completed an LTC prior to the
                // new UOW beginning. We use this value in uowPostEnd to determine whether or not an LTC
                // should be begun and, by storing the LTC's boundary (either bean method or ActivitySession)
                // here we ensure that the new LTC has the same boundary as the completed one.
                // LIDB2446: for shareable LTC, need to store all metadata so new LTC will match.

                _beginContext.set(ltc.getConfiguredMetadata());

            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.tx.ltc.LTCUOWCallback.uowPreBegin", "243", this);

                final IllegalStateException ise = new IllegalStateException();
                ise.initCause(e);

                if (tc.isEntryEnabled())
                    Tr.exit(tc, "uowPreBegin", ise);
                throw ise;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "uowPreBegin");
    }

    protected void uowPostBegin(UOWScope scope) throws IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "uowPostBegin", new Object[] { scope, this });

        // If we are not passed a reference to a UOWScope, something went south
        // while we were attempting to be the new UOW. Depending on what we did
        // in uowPreBegin we may have to restore an LTC before returning control
        // to the application.

        if (scope == null) {
            if (tc.isEventEnabled())
                Tr.event(tc, "uowPostBegin detected null UOW scope");

            final Byte ltcCompleted = _beginContext.get();

            // We completed an LTC in uowPreBegin prior to attempting to begin the new
            // UOW. We must now replace the completed LTC with a new one with the same
            // boundary, i.e. bean method or ActivitySession.
            if (ltcCompleted != null) {
                final byte ltcData = ltcCompleted.byteValue();

                final boolean asScoped = (LocalTranCoordImpl.BOUNDARY_ACTIVITYSESSION.byteValue() ==
                                (ltcData & LocalTranCoordImpl.LTC_BOUNDARY_BIT));

                final boolean unresActionIsCommit =
                                ((ltcData & LocalTranCoordImpl.LTC_UNRESOLVED_ACTION_BIT) == LocalTranCoordImpl.LTC_UNRESOLVED_ACTION_BIT);

                final boolean resolverIsCAB =
                                ((ltcData & LocalTranCoordImpl.LTC_RESOLVER_BIT) == LocalTranCoordImpl.LTC_RESOLVER_BIT);

                if ((ltcData & LocalTranCoordImpl.LTC_SHAREABLE_BIT) == LocalTranCoordImpl.LTC_SHAREABLE_BIT)
                    _ltCurrent.beginShareable(asScoped, unresActionIsCommit, resolverIsCAB);
                else
                    _ltCurrent.begin(asScoped, unresActionIsCommit, resolverIsCAB);
            }

            final String msg = "Failed to start global transaction";
            final IllegalStateException ise = new IllegalStateException(msg);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "transactionPostBegin", ise);
            throw ise;
        }

        // The new UOW has been begun successfully. Pull the boundary of the LTC that
        // was completed as a result of the new UOW being begun and store it in the
        // new UOW from where it is retrieved during preEnd processing.

        final Byte completedLTCBoundary = _beginContext.get();

        if (scope instanceof UOWScopeLTCAware) {
            ((UOWScopeLTCAware) scope).setCompletedLTCBoundary(completedLTCBoundary);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "uowPostBegin");
    }

    protected void uowPreEnd(UOWScope scope) throws IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "uowPreEnd", new Object[] { scope, this });

        // Reset the thread level data
        _endContext.set(null);

        // RTC178342: We'll reset the _beginContext to null at this point also. RTC178342 showed that as a result
        // of the fix to RTC160085, context can linger on a thread and be picked up by a subsequent operation.
        _beginContext.set(null);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Set the _beginContext and _endContext to NULL");
        if (scope != null) {
            // Retrieve the boundary of the LTC that was completed as a result of the UOW
            // represented by scope being begun. Now that this UOW is completing we must,
            // if appropriate, replace it with an LTC with the same boundary as the one that
            // was completed in uowPreBegin.
            Byte completedLTCBoundary = null;

            if (scope instanceof UOWScopeLTCAware) {
                completedLTCBoundary = ((UOWScopeLTCAware) scope).getCompletedLTCBoundary();
            }

            // Cache the boundary in thread local storage so that we can access it in
            // uowPostEnd and, if appropriate, begin a new LTC.
            _endContext.set(completedLTCBoundary);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "uowPreEnd");
    }

    protected void uowPostEnd() throws IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "uowPostEnd", this);

        final Byte completedLTCBoundary = _endContext.get();

        // Restore the LTC if we're supposed to
        if (completedLTCBoundary != null) {
            final byte ltcData = completedLTCBoundary.byteValue();

            final boolean asScoped = (LocalTranCoordImpl.BOUNDARY_ACTIVITYSESSION.byteValue() ==
                            (ltcData & LocalTranCoordImpl.LTC_BOUNDARY_BIT));

            final boolean unresActionIsCommit =
                            ((ltcData & LocalTranCoordImpl.LTC_UNRESOLVED_ACTION_BIT) == LocalTranCoordImpl.LTC_UNRESOLVED_ACTION_BIT);

            final boolean resolverIsCAB =
                            ((ltcData & LocalTranCoordImpl.LTC_RESOLVER_BIT) == LocalTranCoordImpl.LTC_RESOLVER_BIT);

            if ((ltcData & LocalTranCoordImpl.LTC_SHAREABLE_BIT) == LocalTranCoordImpl.LTC_SHAREABLE_BIT)
                _ltCurrent.beginShareable(asScoped, unresActionIsCommit, resolverIsCAB);
            else
                _ltCurrent.begin(asScoped, unresActionIsCommit, resolverIsCAB);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "uowPostEnd");
    }

    public static void destroyUserTransactionCallback() {
        userTranCallback._endContext.set(null);
        userTranCallback = null;
    }
}
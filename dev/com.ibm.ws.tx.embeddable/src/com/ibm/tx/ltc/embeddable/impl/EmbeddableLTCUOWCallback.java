/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.ltc.embeddable.impl;

import com.ibm.tx.TranConstants;
import com.ibm.tx.ltc.impl.LTCUOWCallback;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;

/**
 *
 */
public class EmbeddableLTCUOWCallback extends LTCUOWCallback {

    private static final TraceComponent tc = Tr.register(EmbeddableLTCUOWCallback.class, TranConstants.TRACE_GROUP, null);

    /** Unfortunately the "userTranCallback" in the super class is private, so we need our own */
    protected static EmbeddableLTCUOWCallback userTranCallback;

    public static UOWScopeCallback getUserTransactionCallback() {
        if (userTranCallback == null) {
            userTranCallback = new EmbeddableLTCUOWCallback(UOW_TYPE_TRANSACTION);
        }
        return userTranCallback;
    }

    /**
     * Strange to have a public method together with the singleton pattern above..but the super
     * class has this public constructor so keeping the same.
     */
    public EmbeddableLTCUOWCallback(int uowType) {
        super(uowType);
    }

    /*
     * Notification from UserTransaction or UserActivitySession interface implementations
     * that the state of a bean-managed UOW has changed. As a result of this bean-managed
     * UOW change we may have to change the LTC that's on the thread.
     */
    @Override
    public void contextChange(int typeOfChange, UOWScope scope) throws IllegalStateException
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "contextChange", new Object[] { typeOfChange, scope, this });

        try {
            // Determine the Tx change type and process.  Ensure we do what
            // we need to do as close to the context switch as possible
            switch (typeOfChange) {
                case PRE_BEGIN:
                    try {
                        LTCCallbacks.instance().contextChange(typeOfChange);
                    } finally {
                        uowPreBegin();
                    }
                    break;
                case POST_BEGIN:
                    try {
                        uowPostBegin(scope);
                    } finally {
                        LTCCallbacks.instance().contextChange(typeOfChange);
                    }
                    break;
                case PRE_END:
                    try {
                        LTCCallbacks.instance().contextChange(typeOfChange);
                    } finally {
                        uowPreEnd(scope);
                    }
                    break;
                case POST_END:
                    try {
                        uowPostEnd();
                    } finally {
                        LTCCallbacks.instance().contextChange(typeOfChange);
                    }
                    break;
                default:
                    if (traceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Unknown typeOfChange: " + typeOfChange);
            }
        } catch (IllegalStateException ise) {
            if (traceOn && tc.isEntryEnabled())
                Tr.exit(tc, "contextChange", ise);
            throw ise;
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "contextChange");
    }

    @Override
    protected void uowPreBegin() throws IllegalStateException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "uowPreBegin", new Object[] { this, new Exception("uowPreBegin") });

        Byte beginContextByte = _beginContext.get();
        super.uowPreBegin();
        beginContextByte = _beginContext.get();

        if (beginContextByte == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "_beginContext= NULL !");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "_beginContext=" + beginContextByte);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "uowPreBegin");
        }
    }

    @Override
    protected void uowPostBegin(UOWScope scope) throws IllegalStateException {
        super.uowPostBegin(scope);
    }

    @Override
    protected void uowPreEnd(UOWScope scope) throws IllegalStateException {
        Byte endContextByte = _endContext.get();
        super.uowPreEnd(scope);
        endContextByte = _endContext.get();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "_endContext=" + endContextByte);
        }
    }

    @Override
    protected void uowPostEnd() throws IllegalStateException {
        Byte endContextByte = _endContext.get();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "_endContext=" + endContextByte);
        }
        super.uowPostEnd();
    }

}

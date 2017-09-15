/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.inbound.impl;

import javax.resource.spi.ResourceAdapterInternalException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.ra.impl.SibRaUtils;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.ra.SibRaNotSupportedException;

/**
 * Wrapper for <code>AbstractConsumerSession</code> to protect resource
 * adapter against actions of the MDB whilst allowing it access to the same
 * session.
 */
final class SibRaAbstractConsumerSession implements AbstractConsumerSession {

    /**
     * The session to which allowed method calls are delegated.
     */
    private final AbstractConsumerSession _delegate;

    /**
     * A clone of the parent connection for the delegate to return from
     * <code>getConnection</code>.
     */
    private final SICoreConnection _connectionClone;

    /**
     * Flag indicating whether the session has gone out of the scope of the
     * <code>onMessage</code> method and should now behave as if closed.
     */
    private boolean _outOfScope = false;

    /**
     * The component to use for trace.
     */
    private static TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaAbstractConsumerSession.class);

    /**
     * Provides access to NLS enabled messages.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String RA_INBOUND_CONS_PROBE_1 = "RA_INB_CONS1";

    private static final String RA_INBOUND_CONS_PROBE_2 = "RA_INB_CONS2";

   
    /**
     * Constructor.
     * 
     * @param delegate
     *            the session to which the wrapper should delegate
     * @throws ResourceAdapterInternalException
     *             if the parent connection could not be cloned
     */
    SibRaAbstractConsumerSession(final AbstractConsumerSession delegate)
            throws ResourceAdapterInternalException {

        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, "SibRaAbstractConsumerSession", delegate);
        }

        _delegate = delegate;

        // Clone connection now as we can't rethrow any exception on
        // getConnection
        try {

            _connectionClone = delegate.getConnection().cloneConnection();

        } catch (final SIException exception) {

            FFDCFilter
                    .processException(
                            exception,
                            "com.ibm.ws.sib.ra.inbound.impl.SibRaAbstractConsumerSession.SibRaAbstractConsumerSession",
                            RA_INBOUND_CONS_PROBE_1, this);

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    ("CLONE_EXCEPTION_CWSIV0700"), new Object[] { exception,
                            delegate }, null), exception);

        } catch (final SIErrorException exception) {

            FFDCFilter
                    .processException(
                            exception,
                            "com.ibm.ws.sib.ra.inbound.impl.SibRaAbstractConsumerSession.SibRaAbstractConsumerSession",
                            RA_INBOUND_CONS_PROBE_2, this);

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    ("CLONE_EXCEPTION_CWSIV0700"), new Object[] { exception,
                            delegate }, null), exception);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, "SibRaAbstractConsumerSession");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.DestinationSession#close()
     */
    public void close() throws SibRaNotSupportedException {

        throw new SibRaNotSupportedException(NLS.getFormattedMessage(
                ("NOT_SUPPORTED_CWSIV0701"), new Object[] { "close" }, null));

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.DestinationSession#getConnection()
     */
    public SICoreConnection getConnection()
            throws SISessionUnavailableException {

        checkInScope();
        return _connectionClone;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.DestinationSession#getDestinationAddress()
     */
    public SIDestinationAddress getDestinationAddress() {

        return _delegate.getDestinationAddress();

    }

    /**
     * Checks that the session is still in scope.
     * 
     * @throws SISessionUnavailableException
     *             if the session has gone out of scope
     */
    private void checkInScope() throws SISessionUnavailableException {

        if (_outOfScope) {
            throw new SISessionUnavailableException(NLS
                    .getString("OUT_OF_SCOPE_CWSIV0702"));
        }

    }

    /**
     * Called to indicate that this session is now out of scope.
     */
    void outOfScope() {

        final String methodName = "outOfScope";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        _outOfScope = true;

        try {

            // Close cloned connection so that it, and any resource created
            // from it, also throw SIObjectClosedException
            _connectionClone.close();

        } catch (final SIException exception) {

            FFDCFilter
                    .processException(
                            exception,
                            "com.ibm.ws.sib.ra.inbound.impl.SibRaAbstractConsumerSession.outOfScope",
                            FFDC_PROBE_1, this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            // Swallow exception

        } catch (final SIErrorException exception) {

            FFDCFilter
                    .processException(
                            exception,
                            "com.ibm.ws.sib.ra.inbound.impl.SibRaAbstractConsumerSession.outOfScope",
                            FFDC_PROBE_2, this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            // Swallow exception

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#deleteSet(com.ibm.wsspi.sib.core.SIMessageHandle[],
     *      com.ibm.wsspi.sib.core.SITransaction)
     */
    public void deleteSet(SIMessageHandle[] msgHandles, SITransaction tran)
            throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIResourceException, SIConnectionLostException,
            SILimitExceededException, SIIncorrectCallException,
            SIMessageNotLockedException {

        final String methodName = "deleteSet";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { msgHandles,
                    tran });
        }

        checkInScope();
        _delegate.deleteSet(msgHandles, tran);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#unlockSet(com.ibm.wsspi.sib.core.SIMessageHandle[])
     */
    public void unlockSet(SIMessageHandle[] msgHandles)
            throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIResourceException, SIConnectionLostException,
            SIIncorrectCallException, SIMessageNotLockedException {

        final String methodName = "unlockSet";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { msgHandles });
        }

        checkInScope();
        _delegate.unlockSet(msgHandles);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#unlockSet(com.ibm.wsspi.sib.core.SIMessageHandle[], boolean incrementLockCount)
     */
    public void unlockSet(SIMessageHandle[] msgHandles, boolean incrementLockCount)
            throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIResourceException, SIConnectionLostException,
            SIIncorrectCallException, SIMessageNotLockedException {

        final String methodName = "unlockSet";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { msgHandles });
        }

        checkInScope();
        _delegate.unlockSet(msgHandles, incrementLockCount);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns a string represenation of this object.
     * 
     * @return the string representation
     */
    public String toString() {

        final SibRaStringGenerator generator = new SibRaStringGenerator(this);

        generator.addParent("delegate", _delegate);
        generator.addField("connectionClone", _connectionClone);
        generator.addField("outOfScope", _outOfScope);

        return generator.getStringRepresentation();

    }

}

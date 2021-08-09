/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BifurcatedConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implementation of <code>BifurcatedConsumerSession</code> for core SPI
 * resource adapter. Holds a real <code>BifurcatedConsumerSession</code> to
 * which methods delegate.
 */
public class SibRaBifurcatedConsumerSession extends SibRaDestinationSession
        implements BifurcatedConsumerSession {

    /**
     * The <code>ConsumerSession</code> to which calls are delegated.
     */
    private final BifurcatedConsumerSession _delegate;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaBifurcatedConsumerSession.class);

   
    /**
     * Constructor.
     * 
     * @param connection
     *            the connection on which this session was created
     * @param delegate
     *            the session to which calls should be delegated
     */
    SibRaBifurcatedConsumerSession(final SibRaConnection connection,
            final BifurcatedConsumerSession delegate) {

        super(connection, delegate);

        final String methodName = "SibRaBifurcatedConsumerSession";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    delegate });
        }

        _delegate = delegate;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.BifurcatedConsumerSession#readSet(com.ibm.wsspi.sib.core.SIMessageHandle[])
     */
    public SIBusMessage[] readSet(SIMessageHandle[] msgHandles)
            throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIResourceException, SIConnectionLostException,
            SIIncorrectCallException, SIMessageNotLockedException {

        final String methodName = "readSet";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, msgHandles);
        }

        checkValid();

        final SIBusMessage[] messages = _delegate.readSet(msgHandles);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, messages);
        }
        return messages;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.BifurcatedConsumerSession#readAndDeleteSet(com.ibm.wsspi.sib.core.SIMessageHandle[],
     *      com.ibm.wsspi.sib.core.SITransaction)
     */
    public SIBusMessage[] readAndDeleteSet(final SIMessageHandle[] msgHandles,
            final SITransaction tran) throws SISessionUnavailableException,
            SISessionDroppedException, SIConnectionUnavailableException,
            SIConnectionDroppedException, SIResourceException,
            SIConnectionLostException, SILimitExceededException,
            SIIncorrectCallException, SIMessageNotLockedException {

        final String methodName = "readAndDeleteSet";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, msgHandles);
        }

        checkValid();

        final SIBusMessage[] messages = _delegate.readAndDeleteSet(msgHandles,
                _parentConnection.mapTransaction(tran));

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, messages);
        }
        return messages;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#deleteSet(com.ibm.wsspi.sib.core.SIMessageHandle[],
     *      com.ibm.wsspi.sib.core.SITransaction)
     */
    public void deleteSet(final SIMessageHandle[] msgHandles,
            final SITransaction tran) throws SISessionUnavailableException,
            SISessionDroppedException, SIConnectionUnavailableException,
            SIConnectionDroppedException, SIResourceException,
            SIConnectionLostException, SILimitExceededException,
            SIIncorrectCallException, SIMessageNotLockedException {

        final String methodName = "deleteSet";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, msgHandles);
        }

        checkValid();

        _delegate.deleteSet(msgHandles, _parentConnection.mapTransaction(tran));

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#unlockSet(com.ibm.wsspi.sib.core.SIMessageHandle[])
     */
    public void unlockSet(final SIMessageHandle[] msgHandles)
            throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIResourceException, SIConnectionLostException,
            SIIncorrectCallException, SIMessageNotLockedException {

        final String methodName = "unlockSet";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, msgHandles);
        }

        checkValid();

        _delegate.unlockSet(msgHandles);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }
    
    public void unlockSet(final SIMessageHandle[] msgHandles, boolean incrementLockCount)
        throws SISessionUnavailableException, SISessionDroppedException,
        SIConnectionUnavailableException, SIConnectionDroppedException,
        SIResourceException, SIConnectionLostException,
        SIIncorrectCallException, SIMessageNotLockedException {

      final String methodName = "unlockSet";
      if (TRACE.isEntryEnabled()) {
          SibTr.entry(this, TRACE, methodName, msgHandles);
      }
      
      checkValid();
      
      _delegate.unlockSet(msgHandles,incrementLockCount);
      
      if (TRACE.isEntryEnabled()) {
          SibTr.exit(this, TRACE, methodName);
      }

}

}

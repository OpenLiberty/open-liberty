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

package com.ibm.ws.sib.ra.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;
import com.ibm.wsspi.sib.ra.SibRaNotSupportedException;

/**
 * Implementation of <code>ConsumerSession</code> for core SPI resource
 * adapter. Holds a real <code>ConsumerSession</code> to which methods
 * delegate. The consumer session does not currently support methods relating to
 * the asynchronous receipt of messages.
 */
class SibRaConsumerSession extends SibRaDestinationSession implements
        ConsumerSession {

    /**
     * The <code>ConsumerSession</code> to which calls are delegated.
     */
    private final ConsumerSession _delegate;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibRaUtils
            .getTraceComponent(SibRaDestinationSession.class);

    /**
     * The <code>TraceNLS</code> to use with trace.
     */
    private static final TraceNLS NLS = SibRaUtils.getTraceNls();

 

    /**
     * Constructor.
     *
     * @param connection
     *            the connection on which this session was created
     * @param delegate
     *            the session to which calls should be delegated
     */
    SibRaConsumerSession(final SibRaConnection connection,
            final ConsumerSession delegate) {

        super(connection, delegate);

        final String methodName = "SibRaConsumerSession";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { connection,
                    delegate });
        }

        _delegate = delegate;

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Receives a message. Checks that the session is valid. Maps the
     * transaction parameter before delegating.
     *
     * @param tran
     *            the transaction to receive the message under
     * @return the message or <code>null</code> if none was available
     *
     * @throws SISessionUnavailableException
     *             if the connection is not valid
     * @throws SIIncorrectCallException
     *             if the transaction parameter is not valid given the current
     *             application and container transactions
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIConnectionUnavailableException
     *             if the delegation fails
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SISessionDroppedException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the current container transaction cannot be determined
     */
    public SIBusMessage receiveNoWait(final SITransaction tran)
            throws SISessionDroppedException, SIConnectionDroppedException,
            SISessionUnavailableException, SIConnectionUnavailableException,
            SIConnectionLostException, SILimitExceededException,
            SINotAuthorizedException, SIResourceException, SIErrorException,
            SIIncorrectCallException {

        final String methodName = "receiveNoWait";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, tran);
        }

        checkValid();

        final SIBusMessage message = _delegate.receiveNoWait(_parentConnection
                .mapTransaction(tran));

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, message);
        }
        return message;

    }

    /**
     * Receives a message. Checks that the session is valid. Maps the
     * transaction parameter before delegating.
     *
     * @param tran
     *            the transaction to receive the message under
     * @param timeout
     *            the wait timeout
     * @return the message or <code>null</code> if none was available
     * @throws SIIncorrectCallException
     *             if the transaction parameter is not valid given the current
     *             application and container transactions
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the current container transaction cannot be determined
     * @throws SINotAuthorizedException
     *             if the delegation fails
     * @throws SILimitExceededException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SISessionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionUnavailableException
     *             if the delegation fails
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SISessionDroppedException
     *             if the delegation fails
     */
    public SIBusMessage receiveWithWait(final SITransaction tran,
            final long timeout) throws SISessionDroppedException,
            SIConnectionDroppedException, SISessionUnavailableException,
            SIConnectionUnavailableException, SIConnectionLostException,
            SILimitExceededException, SINotAuthorizedException,
            SIResourceException, SIErrorException, SIIncorrectCallException {

        final String methodName = "receiveWithWait";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { tran, timeout });
        }

        checkValid();

        final SIBusMessage message = _delegate.receiveWithWait(
                _parentConnection.mapTransaction(tran), timeout);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, message);
        }
        return message;

    }

    /**
     * Starts message delivery. Checks that the session is valid then delegates.
     *
     * @param deliverImmediately
     *            whether a thread should be spun off for message delivery
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SISessionUnavailableException
     *             if the connection is not valid
     * @throws SIConnectionUnavailableException
     *             if the delegation fails
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     */
    public void start(boolean deliverImmediately)
            throws SIConnectionDroppedException, SISessionUnavailableException,
            SIConnectionUnavailableException, SIConnectionLostException,
            SIResourceException, SIErrorException, SIErrorException {

        final String methodName = "start";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, Boolean
                    .valueOf(deliverImmediately));
        }

        checkValid();

        _delegate.start(deliverImmediately);

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Stops message delivery. Checks that the session is valid then delegates.
     *
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SISessionUnavailableException
     *             if the session is not valid
     * @throws SIConnectionUnavailableException
     *             if the delegation fails
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SISessionDroppedException
     *             if the delegation fails
     */
    public void stop() throws SISessionDroppedException,
            SIConnectionDroppedException, SISessionUnavailableException,
            SIConnectionUnavailableException, SIConnectionLostException,
            SIResourceException, SIErrorException {

        final String methodName = "stop";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        checkValid();

        _delegate.stop();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * De-registration of an <code>AsynchConsumerCallback</code> is not
     * supported.
     *
     * @throws SibRaNotSupportedException
     *             always
     */
    public void deregisterAsynchConsumerCallback()
            throws SibRaNotSupportedException {

        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));

    }

    /**
     * Unlocking of messages is not supported.
     *
     * @throws SibRaNotSupportedException
     *             always
     */
    public void unlockAll() throws SibRaNotSupportedException {

        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));

    }

    /**
     * Activation of the asynchronous consumer is not supported.
     *
     * @param deliverImmediately
     *            whether a thread should be spun off for message delivery
     * @throws SibRaNotSupportedException
     *             always
     */
    public void activateAsynchConsumer(boolean deliverImmediately) {

        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));

    }

    /**
     * @see com.ibm.wsspi.sib.core.ConsumerSession#getId()
     *
     * @return the ID associated with the session.
     * @throws SIErrorException
     *             if the delegation fails
     * @throws SIResourceException
     *             if the delegation fails
     * @throws SIConnectionLostException
     *             if the delegation fails
     * @throws SISessionUnavailableException
     *             if the session is not valid
     * @throws SIConnectionUnavailableException
     *             if the delegation fails
     * @throws SIConnectionDroppedException
     *             if the delegation fails
     * @throws SISessionDroppedException
     *             if the delegation fails
     */
    public long getId() throws SISessionDroppedException,
            SIConnectionDroppedException, SISessionUnavailableException,
            SIConnectionUnavailableException, SIConnectionLostException,
            SIResourceException, SIErrorException {

        final String methodName = "getId";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        checkValid();
        final long id = _delegate.getId();

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, id);
        }
        return id;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.sib.core.ConsumerSession#registerAsynchConsumerCallback(com.ibm.wsspi.sib.core.AsynchConsumerCallback,
     *      int, long, int, com.ibm.wsspi.sib.core.OrderingContext)
     */
    public void registerAsynchConsumerCallback(
            final AsynchConsumerCallback callback, final int maxActiveMessages,
            final long messageLockExpiry, final int maxBatchSize,
            final OrderingContext extendedMessageOrderingContext)
            throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIIncorrectCallException {

        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));

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

        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));

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

        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));

    }

    /*
     * (non-Javadoc)
     *
     * Will throw SibRaNotSupportedException
     *
     * @see com.ibm.wsspi.sib.core.ConsumerSession#deregisterStoppableAsynchConsumerCallback()
     */
  public void deregisterStoppableAsynchConsumerCallback()
    throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException,
      SIConnectionDroppedException, SIIncorrectCallException
  {
        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));
  }

    /*
     * (non-Javadoc)
     *
     * Will throw SibRaNotSupportedException
     *
     * @see com.ibm.wsspi.sib.core.ConsumerSession#registerStoppableAsynchConsumerCallback(
     *    com.ibm.wsspi.sib.core.AsynchConsumerCallback,
     *      int, long, int, com.ibm.wsspi.sib.core.OrderingContext)
     */
  public void registerStoppableAsynchConsumerCallback(StoppableAsynchConsumerCallback callback,
      int maxActiveMessages, long messageLockExpiry, int maxBatchSize,
      OrderingContext extendedMessageOrderingContext, int maxSequentialFailures, long failingMessageDelay)
    throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException,
      SIConnectionDroppedException, SIIncorrectCallException
  {
        throw new SibRaNotSupportedException(NLS
                .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wsspi.sib.core.AbstractConsumerSession#unlockSet(com.ibm.wsspi.sib.core.SIMessageHandle[])
   */
  public void unlockSet(SIMessageHandle[] msgHandles, boolean incrementLockCount)
          throws SISessionUnavailableException, SISessionDroppedException,
          SIConnectionUnavailableException, SIConnectionDroppedException,
          SIResourceException, SIConnectionLostException,
          SIIncorrectCallException, SIMessageNotLockedException {

      throw new SibRaNotSupportedException(NLS
              .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));

  }

  /**
   * Unlocking of messages is not supported.
   *
   * @throws SibRaNotSupportedException
   *             always
   */
  @Override
  public void unlockAll(boolean incrementUnlockCount)
      throws SISessionUnavailableException, SISessionDroppedException,
      SIConnectionUnavailableException, SIConnectionDroppedException,
      SIResourceException, SIConnectionLostException, SIIncorrectCallException
  {
    throw new SibRaNotSupportedException(NLS
        .getString("ASYNCHRONOUS_METHOD_CWSIV0250"));
    
  }
}

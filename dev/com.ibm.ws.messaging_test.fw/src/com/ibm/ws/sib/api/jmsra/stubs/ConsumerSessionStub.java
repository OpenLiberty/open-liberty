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
package com.ibm.ws.sib.api.jmsra.stubs;

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.OrderingContext;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.ConsumerSession;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SIMessageHandle;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.StoppableAsynchConsumerCallback;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Stub class for ConsumerSession.
 */
public class ConsumerSessionStub implements ConsumerSession {

    private AsynchConsumerCallback _callback = null;

    private static Set _sessions = new HashSet();

    /**
     * Constructor for the stub
     */
    public ConsumerSessionStub() {
        _sessions.add(this);
    }

    /**
     * Delivers the messages. Calls consume messages on the callback
     * 
     * @param messages
     *            The messages to deliver
     * @throws Throwable
     */
    public void deliverAsynchMessages(SIBusMessage[] messages) throws Throwable {
        if (_callback != null) {
            LockedMessageEnumeration enumeration = new LockedMessageEnumerationStub(
                    this, messages);
            _callback.consumeMessages(enumeration);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.core.ConsumerSession#close()
     */
    public void close() {
        _sessions.remove(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.core.ConsumerSession#stop()
     */
    public void stop() {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.core.ConsumerSession#getConnection()
     */
    public SICoreConnection getConnection() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.core.ConsumerSession#deregisterAsynchConsumerCallback()
     */
    public void deregisterAsynchConsumerCallback() {
        _callback = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.core.ConsumerSession#unlockAll()
     */
    public void unlockAll() {

        // Do nothing
    }

    /**
     * Returns the set of sessions
     * 
     * @return the set of sessions
     */
    public static Set getSessions() {
        return _sessions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.DestinationSession#getDestinationAddress()
     */
    public SIDestinationAddress getDestinationAddress() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#receiveNoWait(com.ibm.wsspi.sib.core.SITransaction)
     */
    public SIBusMessage receiveNoWait(SITransaction tran) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#receiveWithWait(com.ibm.wsspi.sib.core.SITransaction,
     *      long)
     */
    public SIBusMessage receiveWithWait(SITransaction tran, long timeout) {
        return null;
    }

    /**
     * @see com.ibm.wsspi.sib.core.ConsumerSession#getId()
     */
    public long getId() {
        return 0;
    }

    /**
     * @see com.ibm.wsspi.sib.core.ConsumerSession#activateAsynchConsumer(boolean)
     */
    public void activateAsynchConsumer(boolean arg0) {

        // Do nothing
    }

    /**
     * @see com.ibm.wsspi.sib.core.ConsumerSession#start(boolean)
     */
    public void start(boolean arg0) {

        // Do nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.sib.core.ConsumerSession#registerAsynchConsumerCallback(com.ibm.wsspi.sib.core.AsynchConsumerCallback,
     *      int, long, int, com.ibm.wsspi.sib.core.OrderingContext)
     */
    public void registerAsynchConsumerCallback(AsynchConsumerCallback callback,
            int maxActiveMessages, long messageLockExpiry, int maxBatchSize,
            OrderingContext extendedMessageOrderingContext)
            throws SISessionUnavailableException, SISessionDroppedException,
            SIConnectionUnavailableException, SIConnectionDroppedException,
            SIIncorrectCallException {

        _callback = callback;

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

        // Do nothing

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

        // Do nothing

    }

	public void unlockSet(SIMessageHandle[] msgHandles, boolean incrementLockCount) throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIResourceException, SIConnectionLostException, SIIncorrectCallException, SIMessageNotLockedException
    {
      // Do nothing
      
    }

  public void deregisterStoppableAsynchConsumerCallback() throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIIncorrectCallException {
        _callback = null;
		
	}

	public void registerStoppableAsynchConsumerCallback(StoppableAsynchConsumerCallback callback, int maxActiveMessages, long messageLockExpiry, int maxBatchSize, OrderingContext extendedMessageOrderingContext, int maxSequentialFailures, long hiddenMessageDelay) throws SISessionUnavailableException, SISessionDroppedException, SIConnectionUnavailableException, SIConnectionDroppedException, SIIncorrectCallException {
        _callback = callback;
		
	}

	@Override
	public void unlockAll(boolean incrementUnlockCount)
	throws SISessionUnavailableException, SISessionDroppedException,
	SIConnectionUnavailableException, SIConnectionDroppedException,
	SIResourceException, SIConnectionLostException, SIIncorrectCallException
	{
	  // Do nothing
	}
    
}

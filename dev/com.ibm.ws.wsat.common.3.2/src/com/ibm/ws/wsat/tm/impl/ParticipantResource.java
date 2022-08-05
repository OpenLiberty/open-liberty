/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.tm.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATParticipantState;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WebClient;

/**
 * This is an XAResource that wrappers a remote WSATParticipant. The tran mgr
 * will use the ParticipantFactoryService to create instances of these XAResources
 * when it needs to run the 2PC protocol at transaction completion or on
 * recovery of a failed transaction.
 */
public class ParticipantResource implements XAResource {

    private static final String CLASS_NAME = ParticipantResource.class.getName();
    private static final TraceComponent TC = Tr.register(ParticipantResource.class);

    private WSATParticipant participant;
    private long timeoutMillis;
    private long defaultTimeout;

    public ParticipantResource(WSATParticipant participant) {
        defaultTimeout = AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                return Long.parseLong(System.getProperty(WebClient.ASYNC_TIMEOUT, WebClient.DEFAULT_ASYNC_TIMEOUT));
            }
        });
        this.timeoutMillis = defaultTimeout;
        this.participant = participant;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#prepare(javax.transaction.xa.Xid)
     */
    @Override
    public int prepare(Xid xid) throws XAException {
        int vote = XA_RDONLY;
        WebClient webClient = WebClient.getWebClient(participant, participant.getCoordinator());
        try {
            participant.setState(WSATParticipantState.PREPARE);
            webClient.prepare();
            WSATParticipantState state = participant.waitResponse(timeoutMillis, WSATParticipantState.PREPARED, WSATParticipantState.READONLY, WSATParticipantState.ABORTED);
            if (state == WSATParticipantState.PREPARED) {
                vote = XA_OK;
            } else if (state == WSATParticipantState.READONLY) {
                vote = XA_RDONLY;
            } else if (state == WSATParticipantState.ABORTED) {
                throw new XAException(XAException.XA_RBOTHER);
            } else {
                if (TC.isDebugEnabled()) { // Only other option here ought to be TIMEOUT
                    Tr.debug(TC, "Unexpected response state: {0}", state);
                }
                throw new XAException(XAException.XA_RBTIMEOUT);
            }
        } catch (WSATException e) {
            throw new XAException(XAException.XA_RBROLLBACK);
        }
        return vote;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#forget(javax.transaction.xa.Xid)
     */
    @Override
    public void forget(Xid xid) throws XAException {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#commit(javax.transaction.xa.Xid, boolean)
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        WebClient webClient = WebClient.getWebClient(participant, participant.getCoordinator());
        try {
            participant.setState(WSATParticipantState.COMMIT);
            webClient.commit();
            WSATParticipantState state = participant.waitResponse(timeoutMillis, WSATParticipantState.COMMITTED);
            if (state != WSATParticipantState.COMMITTED) {
                if (TC.isDebugEnabled()) { // Only other option here ought to be TIMEOUT
                    Tr.debug(TC, "Unexpected response state: {0}", state);
                }
                throw new XAException(XAException.XA_RBTIMEOUT);
            }
        } catch (WSATException e) {
            throw new XAException(XAException.XA_RBROLLBACK);
        } finally {
            participant.remove();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#rollback(javax.transaction.xa.Xid)
     */
    @Override
    public void rollback(Xid xid) throws XAException {
        WebClient webClient = WebClient.getWebClient(participant, participant.getCoordinator());
        try {
            participant.setState(WSATParticipantState.ROLLBACK);
            webClient.rollback();
            WSATParticipantState state = participant.waitResponse(timeoutMillis, WSATParticipantState.ABORTED);
            if (state != WSATParticipantState.ABORTED) {
                if (TC.isDebugEnabled()) { // Only other option here ought to be TIMEOUT
                    Tr.debug(TC, "Unexpected response state: {0}", state);
                }
                throw new XAException(XAException.XA_RBTIMEOUT);
            }
        } catch (WSATException e) {
            throw new XAException(XAException.XA_RBROLLBACK);
        } finally {
            participant.remove();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#recover(int)
     */
    @Override
    public Xid[] recover(int flag) throws XAException {
        throw new XAException("Recovery not implemented");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#start(javax.transaction.xa.Xid, int)
     */
    @Override
    public void start(Xid xid, int flags) throws XAException {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#end(javax.transaction.xa.Xid, int)
     */
    @Override
    public void end(Xid xid, int flags) throws XAException {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#isSameRM(javax.transaction.xa.XAResource)
     */
    @Override
    public boolean isSameRM(XAResource arg0) throws XAException {
        return (arg0 instanceof ParticipantResource);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#setTransactionTimeout(int)
     */
    @Override
    public boolean setTransactionTimeout(int timeout) throws XAException {
        if (timeout == 0) {
            timeoutMillis = defaultTimeout;
        } else {
            timeoutMillis = timeout * 1000;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.transaction.xa.XAResource#getTransactionTimeout()
     */
    @Override
    public int getTransactionTimeout() throws XAException {
        return (int) (timeoutMillis / 1000);
    }
}
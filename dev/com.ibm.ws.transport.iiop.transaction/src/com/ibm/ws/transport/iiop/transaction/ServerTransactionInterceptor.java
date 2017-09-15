/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.omg.CORBA.LocalObject;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @version $Revision: 476527 $ $Date: 2006-11-18 06:22:47 -0800 (Sat, 18 Nov 2006) $
 */
class ServerTransactionInterceptor extends LocalObject implements ServerRequestInterceptor {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(ServerTransactionInterceptor.class);
    private final Codec codec;
    private final TransactionManager tranManager;
    private final Map<Integer, Transaction> txMap;

    public ServerTransactionInterceptor(Codec codec) {
        this.codec = codec;
        tranManager = EmbeddableTransactionManagerFactory.getTransactionManager();
        txMap = new ConcurrentHashMap<Integer, Transaction>();
    }

    @Override
    public void receive_request(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        try {
            Transaction currentTx = txMap.remove(serverRequestInfo.request_id());
            if (currentTx != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "colocated call, resuming transaction on dispatch thread");
                tranManager.resume(currentTx);
                return;
            }
        } catch (InvalidTransactionException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Could not resume transaction", e);
        } catch (IllegalStateException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Could not resume transaction", e);
        } catch (SystemException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Could not resume transaction", e);
        }
        ServerTransactionPolicy policy = (ServerTransactionPolicy) serverRequestInfo.get_server_policy(ServerTransactionPolicyFactory.POLICY_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "receive_request has retrieved transaction policy - ", policy);
        // it's possible for applications to obtain the orb instance using "java:comp/ORB" and then
        // use the rootPOA to activate an object.  If that happens, then we're not going to see a
        // transaction policy on the request.  Just ignore any request that doesn't have one.
        if (policy != null) {
            ServerTransactionPolicyConfig serverTransactionPolicyConfig = policy.getServerTransactionPolicyConfig();
            serverTransactionPolicyConfig.importTransaction(serverRequestInfo, codec);
        }
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {
        // Stage 2: suspend the tran and squirrel away in ServerRequestInfo
        Transaction currentTx;
        try {
            currentTx = tranManager.suspend();
            if (currentTx != null)
            {
                int requestId = ri.request_id();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "suspend transaction " + currentTx + ", and put in map with requestId " + requestId);
                txMap.put(requestId, currentTx);
            }
        } catch (SystemException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Could not suspend transaction", e);
        }
    }

    @Override
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest
    {
        suspendTxOnSend();
    }

    @Override
    public void send_other(ServerRequestInfo ri) throws ForwardRequest
    {
        suspendTxOnSend();
    }

    @Override
    public void send_reply(ServerRequestInfo ri)
    {
        suspendTxOnSend();
    }

    private void suspendTxOnSend()
    {
        try {
            tranManager.suspend();
        } catch (SystemException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Could not suspend transaction", e);
        }
    }

    @Override
    public void destroy() {}

    /**
     * Returns the name of the interceptor.
     * <p/>
     * Each Interceptor may have a name that may be used administratively
     * to order the lists of Interceptors. Only one Interceptor of a given
     * name can be registered with the ORB for each Interceptor type. An
     * Interceptor may be anonymous, i.e., have an empty string as the name
     * attribute. Any number of anonymous Interceptors may be registered with
     * the ORB.
     * 
     * @return the name of the interceptor.
     */
    @Override
    public String name() {
        return getClass().getName();
    }
}

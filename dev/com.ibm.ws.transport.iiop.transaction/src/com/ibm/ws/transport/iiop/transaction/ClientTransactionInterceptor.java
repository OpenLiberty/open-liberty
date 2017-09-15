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

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CosTSInteroperation.TAG_OTS_POLICY;
import org.omg.CosTransactions.ADAPTS;
import org.omg.CosTransactions.OTSPolicyValueHelper;
import org.omg.IOP.Codec;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * @version $Revision: 502396 $ $Date: 2007-02-01 15:06:06 -0800 (Thu, 01 Feb 2007) $
 */
class ClientTransactionInterceptor extends LocalObject implements ClientRequestInterceptor {
    /**  */
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(ClientTransactionInterceptor.class);
    private final Codec codec;
    private final ThreadLocal<Transaction> _storedTx = new ThreadLocal<Transaction>();
    private volatile TransactionManager transactionManager;

    public ClientTransactionInterceptor(Codec codec) {
        this.codec = codec;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Registered");
    }

    @Override
    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest
    {
        resumeTxOnreply();
    }

    @Override
    public void receive_other(ClientRequestInfo ri) throws ForwardRequest
    {
        resumeTxOnreply();
    }

    @Override
    public void receive_reply(ClientRequestInfo ri)
    {
        resumeTxOnreply();
    }

    private void resumeTxOnreply()
    {
        Transaction suspendedTx = _storedTx.get();
        if (suspendedTx != null && transactionManager != null)
            try {
                Transaction currentTx = transactionManager.getTransaction();
                if (currentTx != null)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "No resume, there is a current transaction " + currentTx + ", the stored transaction is " + suspendedTx);
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "No current transaction, so resume " + _storedTx);
                    transactionManager.resume(suspendedTx);
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

        _storedTx.set(null);
    }

    @Override
    public void send_poll(ClientRequestInfo ri) {}

    @Override
    @FFDCIgnore(BAD_PARAM.class)
    public void send_request(ClientRequestInfo ri) throws ForwardRequest {
        TaggedComponent taggedComponent = null;

        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Checking if target " + ri.operation() + " has a transaction policy");

            taggedComponent = ri.get_effective_component(TAG_OTS_POLICY.value);
        } catch (BAD_PARAM e) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Target has a transaction policy");

        byte[] data = taggedComponent.component_data;
        Any any = null;
        try {
            any = codec.decode_value(data, OTSPolicyValueHelper.type());
        } catch (FormatMismatch formatMismatch) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Mismatched format", formatMismatch);
            throw (INTERNAL) new INTERNAL("Mismatched format").initCause(formatMismatch);
        } catch (TypeMismatch typeMismatch) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Type mismatch", typeMismatch);
            throw (INTERNAL) new INTERNAL("Type mismatch").initCause(typeMismatch);
        }

        short value = OTSPolicyValueHelper.extract(any);
        if (value == ADAPTS.value) {
            ClientTransactionPolicy clientTransactionPolicy = (ClientTransactionPolicy) ri.get_request_policy(ClientTransactionPolicyFactory.POLICY_TYPE);
            if (clientTransactionPolicy == null) {
                return;
            }
            ClientTransactionPolicyConfig clientTransactionPolicyConfig = clientTransactionPolicy.getClientTransactionPolicyConfig();
            if (clientTransactionPolicyConfig == null)
                return;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Client has a transaction policy");
            if (transactionManager == null) {
                transactionManager = clientTransactionPolicyConfig.getTransactionManager();
            }
            Transaction currentTx = clientTransactionPolicyConfig.exportTransaction(ri, codec);
            if (currentTx != null) {
                _storedTx.set(currentTx);
            }
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
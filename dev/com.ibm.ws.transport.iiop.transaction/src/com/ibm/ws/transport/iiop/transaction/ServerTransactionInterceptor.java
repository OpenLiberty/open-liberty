/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.transaction;

import java.util.Map;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.INVALID_TRANSACTION;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.TRANSACTION_ROLLEDBACK;
import org.omg.CosTransactions.PropagationContext;
import org.omg.CosTransactions.PropagationContextHelper;
import org.omg.CosTransactions.TransIdentity;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TransactionService;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.tx.util.TMHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.iiop.transaction.nodistributedtransactions.NoDtxTransactionImportHandler;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionProtocolProvider;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;

/**
 * Server request interceptor for IIOP transaction import (Plan B).
 *
 * <p>Import dispatch:
 * <ol>
 *   <li>Decode {@code PropagationContext} from {@code TransactionService (0)}.</li>
 *   <li>Read {@code isd.type().id()} — pure metadata, no classloader dependency.</li>
 *   <li>Iterate registered providers; skip those whose {@link TransactionProtocolProvider#getExpectedISDTypeId()}
 *       does not match (fast pre-screen). Call {@link TransactionProtocolProvider#importTransaction}
 *       on matching providers.</li>
 *   <li>If no provider handled the context, fall back to the NoDTx handler.</li>
 * </ol>
 */
class ServerTransactionInterceptor extends LocalObject implements ServerRequestInterceptor {

    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(ServerTransactionInterceptor.class, "IIOP", null);

    private final Codec codec;
    /** Stores the provider (or NoDTx handler) that imported the transaction for this thread. */
    private final ThreadLocal<Object> activeHandlers = new ThreadLocal<>();

    private final NoDtxTransactionImportHandler noDtxHandler = new NoDtxTransactionImportHandler();

    public ServerTransactionInterceptor(Codec codec) {
        this.codec = codec;
    }

    // -------------------------------------------------------------------------
    // Intercept points
    // -------------------------------------------------------------------------

    @Override
    public void receive_request(ServerRequestInfo ri) throws ForwardRequest {
        ServerTransactionPolicy policy =
            (ServerTransactionPolicy) ri.get_server_policy(ServerTransactionPolicyFactory.POLICY_TYPE);
        if (tc.isDebugEnabled()) Tr.debug(tc, "receive_request: policy={0}", policy);

        if (policy == null) return;
        if (!policy.getConfig().isTransactionImportEnabled()) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Transaction import disabled by policy");
            return;
        }

        TransactionServiceLocator locator = TransactionServiceLocator.getInstance();
        importTransaction(ri, locator);
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) throws ForwardRequest {}

    @Override
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest { unimportTransaction(ri); }

    @Override
    public void send_other(ServerRequestInfo ri) throws ForwardRequest { unimportTransaction(ri); }

    @Override
    public void send_reply(ServerRequestInfo ri) { unimportTransaction(ri); }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    @FFDCIgnore(BAD_PARAM.class)
    private void importTransaction(ServerRequestInfo ri, TransactionServiceLocator locator) {
        ServiceContext sc;
        try {
            sc = ri.get_request_service_context(TransactionService.value);
        } catch (BAD_PARAM e) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "No TransactionService context");
            return;
        }

        org.omg.CORBA.Any any;
        try {
            any = codec.decode_value(sc.context_data, PropagationContextHelper.type());
        } catch (FormatMismatch | TypeMismatch e) {
            throw (INTERNAL) new INTERNAL("Could not decode PropagationContext").initCause(e);
        }

        PropagationContext pc = PropagationContextHelper.extract(any);
        importTransactionInternal(ri, pc, locator);
    }

    private void importTransactionInternal(ServerRequestInfo ri,
                                           PropagationContext pc,
                                           TransactionServiceLocator locator) {
        if (tc.isDebugEnabled()) Tr.debug(tc, "importTransactionInternal: pc={0}", pc);

        try {
            TMHelper.checkTMState();

            if (pc == null) return;

            if (pc.parents != null && pc.parents.length > 0) {
                throw new INVALID_TRANSACTION(); // nested not supported
            }

            TransIdentity transId = pc.current;
            if (transId == null || transId.otid == null) return;

            byte[] globalTid = transId.otid.tid;
            if (globalTid == null || globalTid.length == 0) return;

            TransactionHandlerContext context = locator.getContext();
            Map<Integer, TransactionProtocolProvider> providers = locator.getProviders();

            // Read ISD type ID once — pure metadata, no materialisation
            String isdTypeId = null;
            try {
                if (pc.implementation_specific_data != null) {
                    isdTypeId = pc.implementation_specific_data.type().id();
                }
            } catch (Exception e) {
                // Leave isdTypeId null — wildcard providers will still be tried
                if (tc.isDebugEnabled()) Tr.debug(tc, "Could not read ISD type id: {0}", e);
            }

            boolean imported = false;
            for (TransactionProtocolProvider provider : providers.values()) {
                // Pre-screen: skip providers whose expected ISD type doesn't match
                String expected = provider.getExpectedISDTypeId();
                if (expected != null && !expected.equals(isdTypeId)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Skipping {0}: ISD type mismatch (expected={1}, actual={2})",
                                 provider.getProtocolName(), expected, isdTypeId);
                    }
                    continue;
                }

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Attempting import with: {0}", provider.getProtocolName());
                }
                if (provider.importTransaction(pc, context)) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Imported with: {0}", provider.getProtocolName());
                    }
                    activeHandlers.set(provider);
                    imported = true;
                    break;
                }
            }

            // NoDTx fallback
            if (!imported) {
                if (noDtxHandler.importTransaction(pc, context)) {
                    if (tc.isDebugEnabled()) Tr.debug(tc, "NoDTx fallback handled context");
                    activeHandlers.set(noDtxHandler);
                }
            }

        } catch (org.omg.CORBA.SystemException se) {
            // Preserve TRANSACTION_ROLLEDBACK, INVALID_TRANSACTION, etc. — do not re-wrap as INTERNAL
            throw se;
        } catch (Exception ex) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Exception during import: {0}", ex);
            throw (INTERNAL) new INTERNAL().initCause(ex);
        }
    }

    // -------------------------------------------------------------------------
    // Unimport
    // -------------------------------------------------------------------------

    private void unimportTransaction(ServerRequestInfo ri) {
        if (tc.isDebugEnabled()) Tr.debug(tc, "unimportTransaction");

        Object handler = activeHandlers.get();
        if (handler == null) return;
        activeHandlers.remove();

        TransactionHandlerContext context =
            TransactionServiceLocator.getInstance().getContext();

        try {
            if (handler instanceof TransactionProtocolProvider) {
                ((TransactionProtocolProvider) handler).unimportTransaction(context);
            } else if (handler instanceof NoDtxTransactionImportHandler) {
                ((NoDtxTransactionImportHandler) handler).unimportTransaction(context);
            } else {
                // Defensive — suspend whatever is on the thread
                context.getTransactionManager().suspend();
            }
        } catch (javax.transaction.SystemException se) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "SystemException in unimport: {0}", se);
        }
    }

    @Override
    public void destroy() {}

    @Override
    public String name() { return getClass().getName(); }
}

// Made with Bob

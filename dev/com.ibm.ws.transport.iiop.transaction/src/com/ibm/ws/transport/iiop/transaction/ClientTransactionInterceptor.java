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

import static com.ibm.ws.transport.iiop.transaction.TransactionIORConstants.SERVER_INSTANCE_UUID;
import static com.ibm.ws.transport.iiop.transaction.TransactionIORConstants.TAG_IBM_SERVER_UUID;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.TRANSACTION_ROLLEDBACK;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TaggedComponent;
import org.omg.IOP.TransactionService;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import org.omg.CosTSInteroperation.TAG_OTS_POLICY;
import org.omg.CosTransactions.ADAPTS;
import org.omg.CosTransactions.OTSPolicyValueHelper;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.iiop.transaction.nodistributedtransactions.NoDTxTransactionExporter;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionProtocolProvider;

/**
 * Client-side IIOP interceptor for transaction propagation (Plan B).
 *
 * <p>Protocol selection logic:
 * <ol>
 *   <li>If the target IOR carries {@code TAG_IBM_SERVER_UUID} matching this server instance,
 *       use the NoDTx exporter (local-call optimisation).</li>
 *   <li>Otherwise iterate registered providers in priority order; the first whose
 *       {@link TransactionProtocolProvider#handlesIOR(ClientRequestInfo)} returns true
 *       is used to export the transaction.</li>
 *   <li>If no provider handles the IOR, fall back to the NoDTx exporter.</li>
 * </ol>
 *
 * <p>The core interceptor has zero knowledge of any protocol's IOR tag format or
 * payload — all such knowledge lives inside the provider implementations.
 */
class ClientTransactionInterceptor extends LocalObject implements ClientRequestInterceptor {

    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(ClientTransactionInterceptor.class, "IIOP", null);

    private final Codec codec;
    /** Stores the provider that exported the transaction for this thread's current call. */
    private final ThreadLocal<TransactionProtocolProvider> activeProviders = new ThreadLocal<>();
    private final NoDTxTransactionExporter noDTxExporter = new NoDTxTransactionExporter();

    public ClientTransactionInterceptor(Codec codec) {
        this.codec = codec;
        if (tc.isDebugEnabled()) Tr.debug(tc, "Registered");
    }

    private TransactionManager getTransactionManager() {
        return TransactionServiceLocator.getInstance().getContext().getTransactionManager();
    }

    // -------------------------------------------------------------------------
    // Reply / exception callbacks
    // -------------------------------------------------------------------------

    @Override
    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
        boolean hadActiveTx = activeProviders.get() != null;
        resumeTxOnReply(ri, true);
        // Only roll back and throw if:
        //   (a) there was actually a transaction in flight on this call, AND
        //   (b) the reply contains NO TransactionService service context.
        //
        // When the server successfully processed the transaction (even if the EJB or
        // container then threw a system exception — e.g. ConcurrentAccessException,
        // EJBTransactionRolledbackException), our ServerTransactionInterceptor echoes
        // the inbound TransactionService context back in the reply.  Its presence here
        // means "I imported and handled the transaction correctly — do not roll back on
        // my behalf."  Its absence means "I never saw the transaction" (ORB-level error,
        // connection failure, server crashed before receive_request), which is the only
        // case where we should forcibly roll back.
        //
        // This matches tWAS TxClientInterceptor.receive_common's else-if structure.
        if (hadActiveTx && ri.reply_status() == SYSTEM_EXCEPTION.value
                && !hasReplyServiceContext(ri)) {
            setRollbackOnly(false);
            throw new TRANSACTION_ROLLEDBACK("Transaction rolled back due to system exception");
        }
    }

    /**
     * Returns true if the reply contains a TransactionService (id=0) service context
     * with non-null data — indicating the server processed the transaction successfully.
     * BAD_PARAM means the context is absent.
     */
    @FFDCIgnore(BAD_PARAM.class)
    private boolean hasReplyServiceContext(ClientRequestInfo ri) {
        try {
            ServiceContext sc = ri.get_reply_service_context(TransactionService.value);
            return sc != null && sc.context_data != null;
        } catch (BAD_PARAM e) {
            return false;
        }
    }

    @Override
    public void receive_other(ClientRequestInfo ri) throws ForwardRequest {
        resumeTxOnReply(ri, false);
    }

    @Override
    public void receive_reply(ClientRequestInfo ri) {
        resumeTxOnReply(ri, false);
    }

    private void resumeTxOnReply(ClientRequestInfo ri, boolean exceptionOccurred) {
        TransactionProtocolProvider provider = activeProviders.get();
        if (provider == null) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "No active provider for request");
            return;
        }
        activeProviders.remove();

        try {
            if (getTransactionManager().getTransaction() != null) {
                TransactionHandlerContext context =
                    TransactionServiceLocator.getInstance().getContext();
                try {
                    if (provider == NODTX_SENTINEL) {
                        noDTxExporter.unexportTransaction(ri, context, exceptionOccurred);
                    } else {
                        provider.unexportTransaction(ri, context, exceptionOccurred);
                    }
                } catch (com.ibm.tx.remote.TRANSACTION_ROLLEDBACK ibmTrb) {
                    // IBM-internal unchecked exception from resumeAssociation — translate to CORBA
                    if (tc.isDebugEnabled()) Tr.debug(tc, "resumeAssociation threw IBM TRANSACTION_ROLLEDBACK", ibmTrb);
                    FFDCFilter.processException(ibmTrb, getClass().getName(), "resumeTxOnReply", this);
                    setRollbackOnly(true);
                    throw new TRANSACTION_ROLLEDBACK(ibmTrb.getMessage());
                } catch (TRANSACTION_ROLLEDBACK corbaTrb) {
                    // Provider threw the CORBA version — mark rollback and propagate
                    setRollbackOnly(true);
                    throw corbaTrb;
                }
            } else {
                if (tc.isDebugEnabled()) Tr.debug(tc, "Provider existed but no current transaction");
            }
        } catch (SystemException se) {
            throw (INTERNAL) new INTERNAL().initCause(se);
        }
    }

    @Override
    public void send_poll(ClientRequestInfo ri) {}

    // -------------------------------------------------------------------------
    // Outbound request
    // -------------------------------------------------------------------------

    @Override
    @FFDCIgnore(BAD_PARAM.class)
    public void send_request(ClientRequestInfo ri) throws ForwardRequest {

        // Gate on policy presence
        ClientTransactionPolicy policy =
            (ClientTransactionPolicy) ri.get_request_policy(ClientTransactionPolicyFactory.POLICY_TYPE);
        if (policy == null) return;

        TransactionServiceLocator locator = TransactionServiceLocator.getInstance();
        TransactionHandlerContext context = locator.getContext();
        TransactionManager tm = context.getTransactionManager();

        try {
            if (tm.getTransaction() == null
                    || !ri.response_expected()
                    || "_is_a".equals(ri.operation())
                    || "_get_handle".equals(ri.operation())
                    || "resolve".equals(ri.operation())) {
                return;
            }
        } catch (SystemException se) {
            throw (INTERNAL) new INTERNAL().initCause(se);
        }

        // Check for OTS ADAPTS policy
        TaggedComponent otsPolicyTag;
        try {
            otsPolicyTag = ri.get_effective_component(TAG_OTS_POLICY.value);
        } catch (BAD_PARAM e) {
            return;
        }

        if (tc.isDebugEnabled()) Tr.debug(tc, "Target has a transaction policy");

        org.omg.CORBA.Any any;
        try {
            any = codec.decode_value(otsPolicyTag.component_data, OTSPolicyValueHelper.type());
        } catch (Exception e) {
            throw (INTERNAL) new INTERNAL("OTS policy decode failed").initCause(e);
        }

        if (OTSPolicyValueHelper.extract(any) != ADAPTS.value) return;

        // Local-server UUID shortcut
        if (isLocalServerUUID(ri)) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Local UUID match — using NoDTx exporter");
            exportWithNoDTx(ri, context);
            return;
        }

        // Try providers in priority order
        List<TransactionProtocolProvider> providers = locator.getSortedProviders();
        for (TransactionProtocolProvider provider : providers) {
            if (provider.handlesIOR(ri)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exporting with provider: {0}", provider.getProtocolName());
                }
                provider.exportTransaction(ri, codec, context);
                activeProviders.set(provider);
                return;
            }
        }

        // No provider matched — fall back to NoDTx
        if (tc.isDebugEnabled()) Tr.debug(tc, "No provider handled IOR — falling back to NoDTx");
        exportWithNoDTx(ri, context);
    }

    private void exportWithNoDTx(ClientRequestInfo ri, TransactionHandlerContext context) {
        noDTxExporter.exportTransaction(ri, codec, context);
        activeProviders.set(NODTX_SENTINEL);
    }

    /**
     * Sentinel value stored in the ThreadLocal to indicate NoDTx was used.
     * Avoids storing the exporter directly since NoDTxTransactionExporter does not
     * implement TransactionProtocolProvider.
     */
    private static final TransactionProtocolProvider NODTX_SENTINEL = new TransactionProtocolProvider() {
        public String getProtocolName()  { return "NoDTx-sentinel"; }
        public int    getPriority()      { return Integer.MAX_VALUE; }
        public int    getIORTagId()      { return 0; }
        public void   contributeToIOR(org.omg.PortableInterceptor.IORInfo i, Codec c) {}
        public boolean handlesIOR(ClientRequestInfo r) { return false; }
        public void   exportTransaction(ClientRequestInfo r, Codec c, TransactionHandlerContext x) {}
        public void   unexportTransaction(ClientRequestInfo r, TransactionHandlerContext x,
                                          boolean exceptionOccurred) {}
        public boolean importTransaction(org.omg.CosTransactions.PropagationContext p, TransactionHandlerContext x) { return false; }
        public void   unimportTransaction(TransactionHandlerContext x) {}
    };

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @FFDCIgnore(BAD_PARAM.class)
    private boolean isLocalServerUUID(ClientRequestInfo ri) {
        try {
            TaggedComponent comp = ri.get_effective_component(TAG_IBM_SERVER_UUID);
            byte[] data = comp.component_data;
            if (data != null && data.length >= 19) {
                ByteBuffer buf = ByteBuffer.wrap(data, 3, 16);
                UUID remote = new UUID(buf.getLong(), buf.getLong());
                boolean local = remote.equals(SERVER_INSTANCE_UUID);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "UUID check: remote={0} local={1} match={2}",
                             remote, SERVER_INSTANCE_UUID, local);
                }
                return local;
            }
        } catch (BAD_PARAM e) {
            // tag absent
        } catch (Exception e) {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Error reading TAG_IBM_SERVER_UUID", e);
        }
        return false;
    }

    private void setRollbackOnly(boolean resumeAssociation) {
        try {
            EmbeddableTransactionImpl tx =
                (EmbeddableTransactionImpl) getTransactionManager().getTransaction();
            if (tx != null) {
                try {
                    if (resumeAssociation) tx.resumeAssociation();
                    tx.setRollbackOnly();
                } catch (IllegalStateException ise) {
                    FFDCFilter.processException(ise, getClass().getName(), "setRollbackOnly", this);
                }
            }
        } catch (SystemException se) {
            throw (INTERNAL) new INTERNAL().initCause(se);
        }
    }

    @Override
    public void destroy() {}

    @Override
    public String name() { return getClass().getName(); }
}

// Made with Bob

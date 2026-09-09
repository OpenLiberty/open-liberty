/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.transaction.nodistributedtransactions;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.Xid;

import org.omg.CORBA.Any;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.ORB;
import org.omg.CosTransactions.PropagationContext;
import org.omg.CosTransactions.PropagationContextHelper;
import org.omg.CosTransactions.TransIdentity;
import org.omg.CosTransactions.otid_t;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TransactionService;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.transaction.extension.TransactionHandlerContext;
/**
 * Fallback transaction exporter for local-server calls and remote calls where
 * no protocol-specific provider handles the target.
 *
 * <p>For local-server calls, it propagates the current transaction identity so
 * the receiving server can look up the existing transaction. For remote calls,
 * it creates an empty propagation identity for legacy non-interoperable
 * transaction behavior.
 *
 * <p>This class is directly instantiated (not OSGi managed) and receives services
 * via the TransactionHandlerContext parameter.
 */
public class NoDTxTransactionExporter {
    
    private static final TraceComponent tc = Tr.register(NoDTxTransactionExporter.class, "IIOP", null);
    private static final TransIdentity[] NO_PARENTS = new TransIdentity[0];
    private static final otid_t NULL_XID = new otid_t(0, 0, new byte[0]);
    
    public String getProtocolName() {
        return "NoDTx";
    }
    
    public int getProtocolId() {
        // NoDTx doesn't have a protocol ID in TAG_IBM_TRANSACTION_EXTENDED
        // Return 0 to indicate "no distributed transaction protocol"
        return 0;
    }
    
    public void exportTransaction(ClientRequestInfo ri, Codec codec,
                                  TransactionHandlerContext context,
                                  boolean localTarget) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Fallback exportTransaction called for operation: {0}", ri.operation());
        }

        // Get TransactionManager from context
        TransactionManager transactionManager = context.getTransactionManager();
        if (transactionManager == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "TransactionManager not available from context");
            }
            return;
        }

        final ORB orb = ORB.init();
        PropagationContext propagationContext;
        EmbeddableTransactionImpl tx;
        try {
            tx = (EmbeddableTransactionImpl) transactionManager.getTransaction();

            if (tx == null)
                return;

            otid_t otid = NULL_XID;
            if (localTarget) {
                Xid xid = tx.getXidImpl(false);
                if (xid != null) {
                    byte[] xidBytes = xid.getGlobalTransactionId();
                    if (xidBytes != null)
                        otid = new otid_t(0, 0, xidBytes);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating {0} NoDTx propagation context",
                         localTarget ? "local" : "remote");
            }

            TransIdentity transIdentity = new TransIdentity(null, null, otid);
            Any implementationSpecificData = orb.create_any();
            implementationSpecificData.insert_boolean(true);
            propagationContext = new PropagationContext(0, transIdentity,
                                                       NO_PARENTS, implementationSpecificData);
        } catch (Exception e) {
            throw (INTERNAL) new INTERNAL("Could not create NoDTx propagation context")
                .initCause(e);
        }
        
        // Encode and add propagation context to service context
        Any any = orb.create_any();
        PropagationContextHelper.insert(any, propagationContext);
        byte[] encodedPropagationContext;
        try {
            encodedPropagationContext = codec.encode_value(any);
        } catch (InvalidTypeForEncoding invalidTypeForEncoding) {
            throw (INTERNAL) new INTERNAL("Could not encode propagationContext")
                .initCause(invalidTypeForEncoding);
        }
        ServiceContext otsServiceContext = new ServiceContext(TransactionService.value,
                                                              encodedPropagationContext);
        ri.add_request_service_context(otsServiceContext, true);
    
        if (tx != null)
            tx.suspendAssociation();
    }
    
    public void unexportTransaction(ClientRequestInfo ri,
                                    TransactionHandlerContext context,
                                    boolean exceptionOccurred) {
        // Get TransactionManager from context
        TransactionManager transactionManager = context.getTransactionManager();
        if (transactionManager == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "TransactionManager not available from context");
            }
            return;
        }
        
        try {
            EmbeddableTransactionImpl tx = (EmbeddableTransactionImpl) transactionManager.getTransaction();
            if(tx != null)
                tx.resumeAssociation();
        } catch (Exception e) {
            // Silently ignore - best effort resume
        }
    }
    
    private static boolean isTransactionActive(TransactionManager transactionManager) {
        try {
            int status = transactionManager.getStatus();
            return status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException ignored) {
            return false;
        }
    }
}

// Made with Bob
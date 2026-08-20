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
 * Fallback transaction exporter that creates empty/minimal propagation contexts.
 *
 * This exporter is used when no protocol-specific exporter is available or suitable.
 * It creates a minimal CORBA propagation context but does not actually propagate
 * the transaction - it's a compatibility fallback for legacy behavior.
 *
 * This exporter is always enabled and has the lowest priority (Integer.MAX_VALUE),
 * ensuring it's only used as a last resort.
 *
 * This class is directly instantiated (not OSGi managed) and receives services
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
    
    public int getPriority() {
        return Integer.MAX_VALUE; // Lowest priority - use only as fallback
    }
    
    public void exportTransaction(ClientRequestInfo ri, Codec codec,
                                  TransactionHandlerContext context) {
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
        PropagationContext propagationContext = null;
        EmbeddableTransactionImpl tx = null;
        try {
            tx = (EmbeddableTransactionImpl) transactionManager.getTransaction();
        
            if (tx == null)
                return;
        
            otid_t otid = NULL_XID;
    
            Xid xid = tx.getXidImpl(false);
            if (xid != null) {
                byte[] xidBytes = xid.getGlobalTransactionId();
                if (xidBytes != null)
                    otid = new otid_t(0, 0, xidBytes);
            }
        
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Creating non-interop propagation context");
            }
            
            TransIdentity transIdentity = new TransIdentity(null, null, otid);
            int timeout = 0;
            Any implementationSpecificData = orb.create_any();
            implementationSpecificData.insert_boolean(true);
            propagationContext = new PropagationContext(timeout, transIdentity,
                                                       NO_PARENTS, implementationSpecificData);
            
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception creating empty propagation context: {0}", e);
            }
            // Create minimal context on error
            TransIdentity transIdentity = new TransIdentity(null, null, NULL_XID);
            Any implementationSpecificData = orb.create_any();
            implementationSpecificData.insert_boolean(true);
            propagationContext = new PropagationContext(0, transIdentity,
                                                       NO_PARENTS, implementationSpecificData);
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
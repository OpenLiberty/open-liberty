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
package com.ibm.ws.transport.iiop.transaction.nodistributedtransactions;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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

import com.ibm.ws.transport.iiop.transaction.ClientTransactionPolicyConfig;

/**
 * @version $Rev: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class NoDTxClientTransactionPolicyConfig implements ClientTransactionPolicyConfig {

    private static final long serialVersionUID = 3330069139634001416L;
    private static final TransIdentity[] NO_PARENTS = new TransIdentity[0];
    private static final otid_t NULL_XID = new otid_t(0, 0, new byte[0]);

    private final TransactionManager transactionManager;

    public static boolean isTransactionActive(TransactionManager transactionManager) {
        try {
            int status = transactionManager.getStatus();
            return status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException ignored) {
            return false;
        }
    }

    public NoDTxClientTransactionPolicyConfig(TransactionManager transactionManager) {
        if (transactionManager == null) {
            throw new IllegalArgumentException("transactionManager must not be null");
        }
        this.transactionManager = transactionManager;
    }

    @Override
    public Transaction exportTransaction(ClientRequestInfo ri, Codec codec) {
        if (isTransactionActive(transactionManager)) {
            //19.6.2.1 (1) propagate an "empty" transaction context.
            //but, it needs an xid!
            TransIdentity transIdentity = new TransIdentity(null, null, NULL_XID);
            int timeout = 0;
            final ORB orb = ORB.init();
            Any implementationSpecificData = orb.create_any();
            // Insert a boolean into the object.
            implementationSpecificData.insert_boolean(true);
            PropagationContext propagationContext = new PropagationContext(timeout, transIdentity, NO_PARENTS, implementationSpecificData);
            Any any = orb.create_any();
            PropagationContextHelper.insert(any, propagationContext);
            byte[] encodedPropagationContext;
            try {
                encodedPropagationContext = codec.encode_value(any);
            } catch (InvalidTypeForEncoding invalidTypeForEncoding) {
                throw (INTERNAL) new INTERNAL("Could not encode propagationContext").initCause(invalidTypeForEncoding);
            }
            ServiceContext otsServiceContext = new ServiceContext(TransactionService.value, encodedPropagationContext);
            ri.add_request_service_context(otsServiceContext, true);
        }
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            return null;
        }

    }

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
}

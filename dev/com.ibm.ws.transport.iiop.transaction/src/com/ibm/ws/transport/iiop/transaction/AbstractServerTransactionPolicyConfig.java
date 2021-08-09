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

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.SystemException;
import org.omg.CosTransactions.PropagationContext;
import org.omg.CosTransactions.PropagationContextHelper;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TransactionService;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ServerRequestInfo;

import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.impl.TranManagerSet;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * @version $Rev: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public abstract class AbstractServerTransactionPolicyConfig implements ServerTransactionPolicyConfig {
    private static final long serialVersionUID = 1L;
    private static final TraceComponent tc = Tr.register(AbstractServerTransactionPolicyConfig.class);

    @Override
    @FFDCIgnore(org.omg.CORBA.BAD_PARAM.class)
    public void importTransaction(ServerRequestInfo serverRequestInfo, Codec codec) throws SystemException {
        ServiceContext serviceContext = null;
        try {
            serviceContext = serverRequestInfo.get_request_service_context(TransactionService.value);
        } catch (BAD_PARAM e) {
            // do nothing
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Caught BAD_PARAM on Transaction Import, infer that there was no Transaction Context");

            // RTC 170893 check whether there is a lingering tran of type TXTYPE_NONINTEROP_GLOBAL. If so, nullify it.
            TransactionImpl incumbentTxImpl = ((TranManagerSet) TransactionManagerFactory.getTransactionManager()).getTransactionImpl();;

            if (incumbentTxImpl != null && incumbentTxImpl.getTxType() == UOWCoordinator.TXTYPE_NONINTEROP_GLOBAL)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "the incumbent tx is NONINTEROP_GLOBAL set it to null");
                }
                // The following call should nullify the current tx on the current thread (in this special case where the
                // TxType is TXTYPE_NONINTEROP_GLOBAL
                ((TranManagerSet) TransactionManagerFactory.getTransactionManager()).suspend();
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "the incumbent tx was null or something other than NONINTEROP_GLOBAL");
                }
            }
        }

        PropagationContext propagationContext;
        if (serviceContext == null) {
            propagationContext = null;
        } else {
            byte[] encoded = serviceContext.context_data;
            Any any;
            try {
                any = codec.decode_value(encoded, PropagationContextHelper.type());
            } catch (FormatMismatch formatMismatch) {
                throw (INTERNAL) new INTERNAL("Could not decode encoded propagation context").initCause(formatMismatch);
            } catch (TypeMismatch typeMismatch) {
                throw (INTERNAL) new INTERNAL("Could not decode encoded propagation context").initCause(typeMismatch);
            }
            propagationContext = PropagationContextHelper.extract(any);
            importTransaction(propagationContext);
        }
    }

    protected abstract void importTransaction(PropagationContext propagationContext) throws SystemException;

}

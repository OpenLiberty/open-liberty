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
package com.ibm.tx.jta.cdi.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.TransactionRequiredException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.TransactionalException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;

@Transactional(value = TxType.MANDATORY)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
@Interceptor
public class Mandatory extends TransactionalInterceptor {

    private static final long serialVersionUID = 1L;

    /**
     * <p>If called outside a transaction context, a TransactionalException with a
     * nested TransactionRequiredException must be thrown.</p>
     * <p>If called inside a transaction context, managed bean method execution will
     * then continue under that context.</p>
     */

    @AroundInvoke
    public Object mandatory(final InvocationContext context) throws Exception {

        if (getUOWM().getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
            throw new TransactionalException("global tx required", new TransactionRequiredException());
        }

        return runUnderUOWManagingEnablement(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, true, context, "MANDATORY");
    }
}
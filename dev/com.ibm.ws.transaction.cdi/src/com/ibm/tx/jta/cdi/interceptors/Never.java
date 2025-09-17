/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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
package com.ibm.tx.jta.cdi.interceptors;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.InvalidTransactionException;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.TransactionalException;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;

@Transactional(value = TxType.NEVER)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
@Interceptor
public class Never extends TransactionalInterceptor {
    private static final long serialVersionUID = 1L;

    /**
     * <p>If called outside a transaction context, managed bean method execution
     * must then continue outside a transaction context.</p>
     * <p>If called inside a transaction context, a TransactionalException with
     * a nested InvalidTransactionException must be thrown.</p>
     */

    @AroundInvoke
    public Object never(final InvocationContext context) throws Exception {

        if (getUOWM().getUOWStatus() != UOWSynchronizationRegistry.UOW_STATUS_NONE && getUOWM().getUOWType() == UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION) {
            throw new TransactionalException("TxType.NEVER method called within a global tx", new InvalidTransactionException());
        }

        return runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION, true, context, "NEVER", true);

    }
}
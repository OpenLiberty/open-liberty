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
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;

@Transactional(value = TxType.SUPPORTS)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
@Interceptor
public class Supports extends TransactionalInterceptor {
    private static final long serialVersionUID = 1L;

    /**
     * <p>If called outside a transaction context, managed bean method execution
     * must then continue outside a transaction context.</p>
     * <p>If called inside a transaction context, the managed bean method execution
     * must then continue inside this transaction context.</p>
     */
    @AroundInvoke
    public Object supports(final InvocationContext context) throws Exception {

        int uowType = UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION;

        if (getUOWM().getUOWStatus() != UOWSynchronizationRegistry.UOW_STATUS_NONE) {
            uowType = getUOWM().getUOWType();
        }
        return runUnderUOW(uowType, true, context, "SUPPORTS", false);

    }
}
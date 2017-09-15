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
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;

@Transactional(value = TxType.NOT_SUPPORTED)
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 200)
@Interceptor
public class NotSupported extends TransactionalInterceptor {
    private static final long serialVersionUID = 1L;

    /**
     * <p>If called outside a transaction context, managed bean method execution
     * must then continue outside a transaction context.</p>
     * <p>If called inside a transaction context, the current transaction context must
     * be suspended, the managed bean method execution must then continue
     * outside a transaction context, and the previously suspended transaction
     * must be resumed by the interceptor that suspended it after the method
     * execution has completed.</p>
     */

    @AroundInvoke
    public Object notSupported(final InvocationContext context) throws Exception {

        return runUnderUOWNoEnablement(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION, true, context, "NOT_SUPPORTED");

    }
}
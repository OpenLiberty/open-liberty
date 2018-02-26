/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.tx.beans;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.ExtendedUOWAction;
import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

@FTTransactional
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 1)
@Interceptor
public class Required {

    @AroundInvoke
    public Object required(final InvocationContext context) throws Exception {

        UOWManager u = UOWManagerFactory.getUOWManager();

        return u.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, true, new ExtendedUOWAction() {

            @Override
            public Object run() throws Exception {

                return context.proceed();
            }
        }, null, null);
    }
}
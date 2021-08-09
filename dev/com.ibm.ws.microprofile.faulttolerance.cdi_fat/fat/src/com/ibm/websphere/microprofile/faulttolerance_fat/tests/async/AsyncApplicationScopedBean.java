/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class AsyncApplicationScopedBean {

    // AsyncRequestScopeClient is a @RequestScoped scope bean
    @Inject
    private AsyncRequestScopedBean requestScopeClient;

    @Asynchronous
    public Future<String> callRequestScopedBeanAsynchronously() {
        return CompletableFuture.completedFuture(requestScopeClient.getString());
    }

    public String testMethod() {
        return "@ApplicationScoped test string";
    }

    @Retry
    public String callRequestScopedBeanWithFTConfigured() {
        return requestScopeClient.getString();
    }

}

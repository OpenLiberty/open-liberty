/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.ejb;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
@Asynchronous
public class AsyncEJBThreadContextBean {

    @Inject
    SecuredEjb securedEjb;

    public Future<String> callSecuredEjb() {
        String result = securedEjb.securedCall();
        return CompletableFuture.completedFuture(result);
    }

    public Future<Principal> getEjbPrincipal() {
        Principal result = securedEjb.getPrincipal();
        return CompletableFuture.completedFuture(result);
    }

    @Fallback(fallbackMethod = "securedEjbFallback")
    public Future<String> fallbackToSecuredEjb() {
        throw new RuntimeException("Test Exception");
    }

    public Future<String> securedEjbFallback() {
        String result = securedEjb.securedCall();
        return CompletableFuture.completedFuture(result);
    }

}

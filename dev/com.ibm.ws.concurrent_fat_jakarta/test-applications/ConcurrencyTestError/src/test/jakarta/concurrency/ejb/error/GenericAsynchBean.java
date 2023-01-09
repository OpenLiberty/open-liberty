/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency.ejb.error;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 * This bean has methods that use the Jakarta Concurrency @Asynchronous annotation.
 * This annotation is only allowed on CDI beans, not EJBs.
 * This application should not be installed and hence none of these methods should be run.
 */
@Stateless
@Local(GenericLocal.class)
public class GenericAsynchBean {

    @Asynchronous
    public void getThreadName() {
        System.out.println("Thread name async: " + Thread.currentThread().getName());
    }

    public void getThreadNameNonAsyc() {
        System.out.println("Thread name non-async:" + Thread.currentThread().getName());
    }

    @Asynchronous
    public CompletableFuture<String> getState(String city) {
        if (city == "Rochester")
            return Asynchronous.Result.complete("Minnesota");
        else
            return Asynchronous.Result.complete(null);
    }

    @Asynchronous(executor = "java:comp/DefaultManagedExecutorService")
    public CompletableFuture<String> getStateFromService(String city) {

        CompletableFuture<String> future = Asynchronous.Result.getFuture();

        assertNotNull(future);

        try {
            if (city == "Rochester")
                future.complete("Minnesota");
            else
                future.complete(null);
        } catch (Exception x) {
            future.completeExceptionally(x);
        }

        return future;
    }
}
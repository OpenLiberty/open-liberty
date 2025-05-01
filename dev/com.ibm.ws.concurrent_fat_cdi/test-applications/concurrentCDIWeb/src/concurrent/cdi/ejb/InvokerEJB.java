/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package concurrent.cdi.ejb;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.inject.Inject;

@Local(Invoker.class)
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class InvokerEJB implements Invoker {

    @Override
    public <T> T runInEJB(Callable<T> testCode) throws Exception {
        return testCode.call();
    }

    @Inject
    ManagedThreadFactory defaultManagedThreadFactory;

    @Override
    public void testDefaultManagedThreadFactoryClassloader(CompletableFuture<String> future) {
        assertNotNull(defaultManagedThreadFactory);

        // Requires the application's classloader (to access application scoped classes)
        Runnable task = () -> {
            System.out.println("KJA1017 Thread classloader is: " + Thread.currentThread().getContextClassLoader());
            System.out.println("KJA1017 Invoker classloader is: " + Invoker.class.getClassLoader());
            try {
                Class.forName("java.lang.Integer"); //Exists as part of JVM
                Class.forName("concurrent.cdi.ejb.Invoker"); //Exists inside EJB Module
                Class.forName("concurrent.cdi.ext.ConcurrentCDIExtension"); // Exists outside EJB Module
                future.complete("SUCCESS");
            } catch (ClassNotFoundException e) {
                future.completeExceptionally(e);
            }
        };

        Thread thread = defaultManagedThreadFactory.newThread(task);
        thread.start();
    }

}

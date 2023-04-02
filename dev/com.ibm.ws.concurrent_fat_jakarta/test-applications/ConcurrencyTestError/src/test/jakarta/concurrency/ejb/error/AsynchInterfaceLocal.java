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

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.concurrent.Asynchronous;

/**
 * This generic interface is using the Jakarta Concurrency @Asynchronous annotations.
 * Since users should not expect annotations on methods to be inherited from interfaces
 * (only super-classes) these annotations should be ignored.
 * The application should be installed, and a warning given to the user.
 */
public interface AsynchInterfaceLocal {

    @Asynchronous
    public void getThreadName();

    public void getThreadNameNonAsyc();

    @Asynchronous
    public CompletableFuture<String> getState(String city);

    @Asynchronous(executor = "java:comp/DefaultManagedExecutorService")
    public CompletableFuture<String> getStateFromService(String city);

}

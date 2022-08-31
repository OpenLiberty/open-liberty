/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.v13.ee10.web;

import java.io.Serializable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
@org.eclipse.microprofile.faulttolerance.Asynchronous
public class MPFTBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Asynchronous
    CompletionStage<String> doublyAsync() {
        return Asynchronous.Result.complete("Should not be able to combine Jakarta Concurrency @Asynchronous on a method " +
                                            "with MicroProfile Fault Tolerance @Asynchronous on the class");
    }

    CompletionStage<Object> ftAsyncLookup(String name) {
        try {
            ManagedExecutorService executor = (ManagedExecutorService) InitialContext.doLookup(name);
            return executor.completedFuture(executor);
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import prototype.enterprise.concurrent.Async;

public class MyManagedBean {

    @Async
    public CompletableFuture<Object> asyncLookup(String jndiName) {
        try {
            return Async.Result.complete(InitialContext.doLookup(jndiName));
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }
}

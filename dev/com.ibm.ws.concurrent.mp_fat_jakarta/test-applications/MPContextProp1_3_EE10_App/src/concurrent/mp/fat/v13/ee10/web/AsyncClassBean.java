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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.context.ApplicationScoped;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
@Asynchronous(executor = "java:comp/eeExecutor") // Should not be allowed at this location
public class AsyncClassBean implements Serializable {
    private static final long serialVersionUID = 1L;

    CompletableFuture<Object> asyncLookup(String name) {
        try {
            return Asynchronous.Result.complete(InitialContext.doLookup(name));
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
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
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.Dependent;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import prototype.enterprise.concurrent.Async;

@Dependent
public class DependentScopedBean {
    private boolean value;

    public boolean getBoolean() {
        return value;
    }

    /**
     * Asynchronously, look up a JNDI name and convert the result to a String value.
     */
    @Async
    public CompletionStage<String> lookupAndConvertToString(String jndiName) {
        CompletableFuture<String> future = Async.Result.getFuture();
        try {
            future.complete(InitialContext.doLookup(jndiName).toString());
        } catch (NamingException x) {
            future.completeExceptionally(x);
        }
        return future;
    }

    public void setBoolean(boolean value) {
        this.value = value;
    }
}

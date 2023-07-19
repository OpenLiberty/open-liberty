/*******************************************************************************
 * Copyright (c) 2017,2023 IBM Corporation and others.
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
package concurrent.cdi4.web;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.ALL_REMAINING;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;
import static jakarta.enterprise.concurrent.ContextServiceDefinition.SECURITY;

import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.context.Dependent;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;

@ContextServiceDefinition(name = "java:module/concurrent/appcontextcleared",
                          propagated = SECURITY,
                          cleared = APPLICATION,
                          unchanged = ALL_REMAINING)
@ContextServiceDefinition(name = "java:module/concurrent/remainingcontextunchanged",
                          propagated = SECURITY,
                          cleared = {},
                          unchanged = ALL_REMAINING)
@ManagedExecutorDefinition(name = "java:comp/concurrent/appcontextclearedexecutor",
                           context = "java:module/concurrent/appcontextcleared")
@ManagedScheduledExecutorDefinition(name = "java:comp/concurrent/remainingcontextunchangedexecutor",
                                    context = "java:module/concurrent/remainingcontextunchanged")
@Dependent
public class DependentScopedBean {
    private boolean value;

    public boolean getBoolean() {
        return value;
    }

    /**
     * Asynchronously, look up a JNDI name and convert the result to a String value.
     */
    @Asynchronous
    public CompletionStage<String> lookupAndConvertToString(String jndiName) {
        CompletableFuture<String> future = Asynchronous.Result.getFuture();
        try {
            future.complete(InitialContext.doLookup(jndiName).toString());
        } catch (NamingException x) {
            future.completeExceptionally(x);
        }
        return future;
    }

    /**
     * Return an entry consisting of (transaction status, lookup result)
     */
    @Asynchronous(executor = "concurrent/sampleExecutor")
    public CompletableFuture<Entry<Integer, Object>> lookUpAndGetTransactionStatus(String jndiName) {
        try {
            UserTransaction tx = InitialContext.doLookup("java:comp/UserTransaction");
            Entry<Integer, Object> result = new AbstractMap.SimpleEntry<Integer, Object>( //
                            tx.getStatus(), InitialContext.doLookup(jndiName));
            return Asynchronous.Result.complete(result);
        } catch (NamingException | SystemException x) {
            throw new CompletionException(x);
        }
    }

    public void setBoolean(boolean value) {
        this.value = value;
    }
}

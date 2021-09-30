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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import prototype.enterprise.concurrent.Async;

@Singleton
public class TaskBean implements Callable<String> {
    @Inject
    private ApplicationScopedBean appScopedBean;
    @Inject
    private DependentScopedBean dependentScopedBean;
    @Inject
    private RequestScopedBean requestScopedBean;
    @Inject
    private SessionScopedBean sessionScopedBean;
    @Inject
    private SingletonScopedBean singletonScopedBean;

    @Override
    public String call() throws Exception {
        ExecutorService executor1 = (ExecutorService) new InitialContext().lookup("java:comp/env/concurrent/executorRef");
        if (executor1 == null)
            throw new Exception("Unexpected resource ref result " + executor1);

        appScopedBean.setCharacter(Character.toUpperCase(appScopedBean.getCharacter()));
        dependentScopedBean.setBoolean(true);

        try {
            requestScopedBean.setNumber(requestScopedBean.getNumber() + 100);
        } catch (ContextNotActiveException x) {
            // pass
        }

        try {
            sessionScopedBean.setText(sessionScopedBean.getText() + " and more text");
        } catch (ContextNotActiveException x) {
            // pass
        }

        singletonScopedBean.put("Key_TaskBean", singletonScopedBean.get("Key_TaskBean") + " and more text");

        return (String) new InitialContext().lookup("java:comp/env/entry1");
    }

    /**
     * Asynchronously, look up a JNDI name and convert the result to a String value.
     */
    @Async
    public CompletionStage<List<String>> lookupAll(String jndiName1, String jndiName2, String jndiName3) {
        if (Async.Result.getFuture().isDone())
            throw new AssertionError("Result CompletableFuture should not be done already!");
        try {
            return dependentScopedBean.lookupAndConvertToString(jndiName1)
                            .thenCombine(CompletableFuture.completedFuture(InitialContext.doLookup(jndiName2).toString()),
                                         (s1, s2) -> {
                                             // application component context must be available to dependent stage
                                             try {
                                                 return Arrays.asList(s1, s2, InitialContext.doLookup(jndiName3).toString());
                                             } catch (NamingException x) {
                                                 throw new CompletionException(x);
                                             }
                                         });
        } catch (NamingException x) {
            throw new CompletionException(x);
        }
    }
}

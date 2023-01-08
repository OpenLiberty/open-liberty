/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
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
package concurrent.cdi3.web;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.naming.InitialContext;

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
}

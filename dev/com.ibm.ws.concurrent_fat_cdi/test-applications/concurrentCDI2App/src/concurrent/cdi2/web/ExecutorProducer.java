/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi2.web;

import java.util.concurrent.ExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Producer that allows injection of ManagedExecutorService.
 */
public class ExecutorProducer {
    @Produces
    @ApplicationScoped
    ExecutorService getDefaultExecutor() throws NamingException {
        return InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
    }
}

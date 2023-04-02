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
package test.jakarta.concurrency.ejb;

import static jakarta.enterprise.concurrent.ContextServiceDefinition.APPLICATION;

import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.simplicity.config.ManagedExecutorService;

import test.context.list.ListContext;
import test.context.location.ZipCode;
import test.context.timing.Timestamp;

// Use the same name as a context service in ConcurrencyTestServlet,
// which must be permitted because it is scoped to a different module.
@ContextServiceDefinition(name = "java:module/concurrent/ZLContextSvc",
                          propagated = { ZipCode.CONTEXT_NAME, ListContext.CONTEXT_NAME, APPLICATION },
                          cleared = Timestamp.CONTEXT_NAME,
                          unchanged = "Priority")
@Stateless
public class ContextServiceDefinerBean {
    public ContextService lookup() {
        try {
            return InitialContext.doLookup("java:module/concurrent/ZLContextSvc");
        } catch (NamingException x) {
            throw new EJBException(x);
        }
    }
}

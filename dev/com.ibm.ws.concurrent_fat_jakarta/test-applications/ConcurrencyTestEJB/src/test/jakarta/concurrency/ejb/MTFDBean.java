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

import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Only use for testEJBAnnoManagedThreadFactoryInitialization. This bean is used to verify behavior when looking
 * up a ManagedThreadFactory that is defined on the bean via annotation, when the bean hasn't been previously used/initialized.
 */
@ManagedThreadFactoryDefinition(name = "java:module/concurrent/testInitTF")
@Stateless
public class MTFDBean {
    public Object lookupThreadFactory() throws NamingException {
        return InitialContext.doLookup("java:module/concurrent/testInitTF");
    }
}

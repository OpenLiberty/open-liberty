/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi12.fat.apps.nonContextualInjectionPointWar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class NonContextualBean {
    @Inject
    private Instance<Baz> baz;

    public void testNonContextualEjbInjectionPointGetBean() throws NamingException {
        Bar bar = (Bar) new InitialContext().lookup("java:module/Bar");
        assertNotNull("bar is null for: java:module/Bar", bar);
        assertNull("bean is NOT null", bar.getFoo().getInjectionPoint().getBean());
    }

    public void testContextualEjbInjectionPointGetBean() {
        InjectionPoint ip = baz.get().getFoo().getInjectionPoint();
        assertNotNull("bean is null for injection point " + ip, ip.getBean());
        assertEquals("Wrong bean class type", Baz.class, ip.getBean().getBeanClass());
    }

}

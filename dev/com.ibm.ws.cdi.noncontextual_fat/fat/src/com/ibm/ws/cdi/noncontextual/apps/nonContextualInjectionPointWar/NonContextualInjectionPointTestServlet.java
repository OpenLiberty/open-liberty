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
package com.ibm.ws.cdi.noncontextual.apps.nonContextualInjectionPointWar;

import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/test")
public class NonContextualInjectionPointTestServlet extends FATServlet {

    @Test
    public void testNonContextualInjectionPoint() throws NamingException {
        Unmanaged<NonContextualBean> unmanaged = new Unmanaged<NonContextualBean>(NonContextualBean.class);
        UnmanagedInstance<NonContextualBean> instance = unmanaged.newInstance();
        NonContextualBean nonContextualBean = instance.produce().inject().postConstruct().get();

        try {
            nonContextualBean.testNonContextualEjbInjectionPointGetBean();
            nonContextualBean.testContextualEjbInjectionPointGetBean();
        } finally {
            instance.preDestroy().dispose();
        }
    }

}

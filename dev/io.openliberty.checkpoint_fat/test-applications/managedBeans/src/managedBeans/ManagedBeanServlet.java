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
package managedBeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ManagedBeanServlet")
public class ManagedBeanServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Resource(name = "TestManagedBean")
    private TestManagedBean testManagedBean;

    @Test
    public void testManagedBeanInjectionAndLookup() throws Exception {
        TestManagedBean mb = null;

        assertNotNull("TestManagedBean was not injected", testManagedBean);

        mb = (TestManagedBean) new InitialContext().lookup("java:comp/env/TestManagedBean");

        assertNotNull("TestManagedBean ref failed on Context.lookup", mb);

        assertNotNull("Injected resource shouldn't be null", testManagedBean.getUserTransaction()); // Injected resource

        assertEquals("State of injected ManagedBean not correct",
                     "TestManagedBean.INITIAL_VALUE", testManagedBean.getValue()); // PostConstruct value

        testManagedBean.setValue("injectedValue");
        mb.setValue("lookupValue");

        assertEquals("State of injected ManagedBean not correct",
                     "injectedValue", testManagedBean.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "lookupValue", mb.getValue());

    }

    @Test
    public void testManagedBeanPreDestroy() throws Exception {
        int numDestroyed = 0;
        for (int i = 0; i < 100; i++) {
            new InitialContext().lookup("java:comp/env/TestManagedBean");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            numDestroyed = TestManagedBean.getDestroyCount();
            if (numDestroyed > 0) {
                break;
            }
        }
        if (numDestroyed == 0) {
            fail("PreDestroy was never called");
        }

    }
}

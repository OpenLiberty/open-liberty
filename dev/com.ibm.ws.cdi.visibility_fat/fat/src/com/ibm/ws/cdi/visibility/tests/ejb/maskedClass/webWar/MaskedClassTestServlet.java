/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.webWar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.junit.Test;

import com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.libJar.TestBean;
import com.ibm.ws.cdi.visibility.tests.ejb.maskedClass.sharedbeans.Type1;

import componenttest.app.FATServlet;

@WebServlet("/WarLibsTestServlet")
public class MaskedClassTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private Type3 type3Injected;

    @Inject
    private Instance<Type1> type1Instance;

    /**
     * There are two implementations of TestBean, one in this war and another in maskedClassAppClient.jar.
     * <p>
     * The version in the app client jar should not be visible to the war so this should not cause an ambiguous bean exception.
     */
    @Inject
    private TestBean testBeanInjected;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public MaskedClassTestServlet() {
        super();
    }

    @Test
    public void testMaskedClass() {

    }

    @Test
    public void testMaskedClassDirectly() {
        Type1 type1 = new Type1();
        assertEquals("from ejb", type1.getMessage());
    }

    @Test
    public void testAppClientBeanNotVisible() {
        assertEquals("This is TestBean in the war", testBeanInjected.getMessage());
    }

    @Test
    public void testMaskedClassInjected() {
        // Check that regular injection is working as expected
        assertEquals("This is Type3, a managed bean in the war", type3Injected.getMessage());

        // Check that Type1 bean from the ejb jar is injectable
        assertFalse("Type1 bean is ambiguous", type1Instance.isAmbiguous());
        assertFalse("Type1 bean is unsatisfied", type1Instance.isUnsatisfied());
        assertEquals("from ejb", type1Instance.get().getMessage());
    }

}

/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.vistest.maskedClass.zservlet;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.junit.Test;

import com.ibm.ws.cdi.vistest.maskedClass.beans.TestBean;
import com.ibm.ws.cdi.vistest.maskedClass.beans.Type1;
import com.ibm.ws.cdi.vistest.maskedClass.beans.Type3;

import componenttest.app.FATServlet;

@WebServlet("/TestServlet")
public class ZMaskedClassTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private Type3 type3Injected;

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
    public ZMaskedClassTestServlet() {
        super();
    }

    @Test
    public void testMaskedClass() {
        Type1 type1 = new Type1();

        assertEquals("from ejb", type1.getMessage());
        assertEquals("This is Type3, a managed bean in the war", type3Injected.getMessage());
        assertEquals("This is TestBean in the war", testBeanInjected.getMessage());
    }

}

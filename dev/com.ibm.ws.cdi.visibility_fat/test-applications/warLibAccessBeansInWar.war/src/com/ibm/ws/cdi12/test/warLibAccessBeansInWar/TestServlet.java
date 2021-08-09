/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.warLibAccessBeansInWar;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.TestInjectionClass;
import com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.TestInjectionClass2;

import componenttest.app.FATServlet;

@WebServlet("/TestServlet")
public class TestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    TestInjectionClass injection;

    @Inject
    TestInjectionClass2 injection2;

    @Test
    public void testWarLibsCanAccessBeansInWar() throws Exception {
        String message1 = injection.getMessage();
        String message2 = injection2.getMessage();

        assertEquals("TestInjectionClass: WarBean", message1);
        assertEquals("TestInjectionClass2: WarBean", message2);
    }
}

/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.basic.warlibs.war;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.visibility.tests.basic.warlibs.maifestLibJar.TestInjectionClass2;
import com.ibm.ws.cdi.visibility.tests.basic.warlibs.webinfLibJar.TestInjectionClass;

import componenttest.app.FATServlet;

@WebServlet("/WarLibsTestServlet")
public class WarLibsTestServlet extends FATServlet {

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

/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.sharedlib.injectionWar;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.visibility.tests.sharedlib.sharedLibraryJar.InjectedHello;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class InjectionTestServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    private InjectedHello injected;

    @Test
    @Mode(TestMode.FULL)
    public void testSharedLibraryWithCDI() {
        String name = "Iain";
        String result = injected.areYouThere(name);
        assertEquals(InjectedHello.PREFIX + name, result);
    }
}

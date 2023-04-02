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
package com.ibm.ws.cdi.visibility.tests.sharedlib.nonInjectionWar;

import static org.junit.Assert.assertEquals;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.visibility.tests.sharedlib.sharedLibraryJar.NonInjectedHello;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class NoInjectionTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    private final NonInjectedHello foo = new NonInjectedHello();

    @Test
    @Mode(TestMode.FULL)
    public void testSharedLibraryNoInjection() {
        String name = "Iain";
        String result = foo.areYouThere(name);
        assertEquals(NonInjectedHello.PREFIX + name, result);
    }
}

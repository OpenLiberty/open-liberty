/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.web1;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi12.test.shared.InjectedHello;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class SharedLibraryServlet extends FATServlet {

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

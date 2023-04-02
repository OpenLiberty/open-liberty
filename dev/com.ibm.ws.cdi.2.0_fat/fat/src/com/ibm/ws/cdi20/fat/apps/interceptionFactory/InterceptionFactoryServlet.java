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
package com.ibm.ws.cdi20.fat.apps.interceptionFactory;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet(urlPatterns = "/interceptionFactoryTest")
public class InterceptionFactoryServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 671383796561361465L;

    @Inject
    Thing t;

    @Test
    @Mode(TestMode.FULL)
    public void testInterceptionFactory() throws Exception {
        t.hello();
        assertTrue("Interceptor was not invoked", Intercepted.get());
    }
}

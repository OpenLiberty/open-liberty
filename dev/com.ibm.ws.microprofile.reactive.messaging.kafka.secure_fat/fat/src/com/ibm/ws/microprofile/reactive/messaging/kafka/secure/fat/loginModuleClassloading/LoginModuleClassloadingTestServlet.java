/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.loginModuleClassloading;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/LoginModuleLoadTest")
public class LoginModuleClassloadingTestServlet extends FATServlet {

    @Inject
    LoginModuleClassloadingExtension ext;

    private static final long serialVersionUID = 1L;
    public static final String LOGIN_MODULE_CLASS = "com.ibm.ws.kafka.security.LibertyLoginModule";

    @Test
    public void testRegularLoad() throws ClassNotFoundException {
        Class.forName(LOGIN_MODULE_CLASS);
    }

    @Test
    public void testTcclLoad() throws ClassNotFoundException {
        Thread.currentThread().getContextClassLoader().loadClass(LOGIN_MODULE_CLASS);
    }

    @Test
    public void testExtension() throws Exception {
        if (ext.getLoginModuleException() != null) {
            throw ext.getLoginModuleException();
        }
        assertNotNull(ext.getLoginModuleClass());
    }

}

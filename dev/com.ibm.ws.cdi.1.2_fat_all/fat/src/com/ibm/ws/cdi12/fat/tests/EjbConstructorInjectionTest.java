/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

public class EjbConstructorInjectionTest extends LoggingTest {

    @ClassRule
    // Create the server and install the CDIOWB Test feature.
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EjbConstructorInjectionServer");

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testTransientReferenceOnEjbConstructor() throws Exception {
        this.verifyResponse("/ejbConstructorInjection/Servlet", new String[] { "destroy called",
                                                                               "First bean message: foo",
                                                                               "Second bean message: bar",
                                                                               "Third bean message: spam",
                                                                               "Forth bean message: eggs" });
    }

}

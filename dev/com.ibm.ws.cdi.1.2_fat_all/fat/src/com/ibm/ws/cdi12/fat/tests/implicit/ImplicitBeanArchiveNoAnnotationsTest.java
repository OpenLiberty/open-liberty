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
package com.ibm.ws.cdi12.fat.tests.implicit;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;

public class ImplicitBeanArchiveNoAnnotationsTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EjbDefInXmlServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testMultipleNamedEJBsInWar() throws Exception {
        this.verifyResponse("/archiveWithNoBeansXml/SimpleServlet", "PASSED");
    }

    @Mode
    @Test
    public void testConstructorInjection() throws Exception {
        this.verifyResponse("/archiveWithNoBeansXml/ConstructorInjectionServlet", "SUCCESSFUL");
    }

    @Test
    public void testMultipleNamesEjbsInEar() throws Exception {
        this.verifyResponse("/ejbArchiveWithNoAnnotations/ejbServlet", "PASSED");
    }

    @Test
    public void testEjbJarInWar() throws Exception {
        this.verifyResponse("/ejbJarInWarNoAnnotations/ejbServlet", "PASSED");
    }
}

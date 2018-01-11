/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class AfterTypeDiscoveryTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12AfterTypeDiscoveryServer");

    /** {@inheritDoc} */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testAfterTypeDecoratorAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "New msg: decorated");
    }

    @Test
    public void testAfterTypeInterceptorAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "intercepted");
    }

    @Test
    public void testAfterTypeBeanAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", "hello world");
    }

    @Test
    public void testAfterTypeAlternativeAddition() throws Exception {
        verifyResponse("/afterTypeDiscovery/", new String[] { "expecting one: alternative one", "expecting two: alternative two" });
    }

}

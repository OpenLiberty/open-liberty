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

/**
 * These tests checks to see if you can use a Decorator on one of the
 * built in beans
 */
@Mode(TestMode.FULL)
public class DecoratorOnBuiltInBeansTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12DecoratorOnBuiltInBeansTestServer");

    /** {@inheritDoc} */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    public void testBeanWasFound() throws Exception {
        verifyResponse("/defaultDecoratorApp/", "decorating");
    }

}

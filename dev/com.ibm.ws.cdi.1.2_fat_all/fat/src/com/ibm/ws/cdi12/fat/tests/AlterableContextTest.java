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
 * These tests use a runtime feature to destroy a contextual object.
 * The test passes if that object exists before destroy is called
 * and is null afterwards
 */

@Mode(TestMode.FULL)
public class AlterableContextTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12AlterableContextServer");

    /** {@inheritDoc} */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @Test
    public void testBeanWasFound() throws Exception {
        this.verifyResponse("/alterableContextApp/", "I got this from my alterablecontext: com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean");
    }

    @Test
    public void testBeanWasDestroyed() throws Exception {
        this.verifyResponse("/alterableContextApp/", "Now the command returns: null");
    }

}

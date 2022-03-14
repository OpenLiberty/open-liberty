/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.suite.simple.war;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.javaee.ddmodel.suite.simple.CommonTests_Simple;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

// Module configuration supplied context root ("cfg").

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DDSimpleWarTests_Config_ConfigExt extends CommonTests_Simple {
    public static final Class<?> TEST_CLASS = DDSimpleWarTests_Config_ConfigExt.class;

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_simple_war_cfg_ext.xml", setUpSimpleWarNoExt);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestModules, NO_ALLOWED_ERRORS);
    }

    @Test
    public void testContextRoot_War_Config_ConfigExt() throws Exception {
        testHello(TEST_CLASS, EXT_CONTEXT_ROOT);
    }
}

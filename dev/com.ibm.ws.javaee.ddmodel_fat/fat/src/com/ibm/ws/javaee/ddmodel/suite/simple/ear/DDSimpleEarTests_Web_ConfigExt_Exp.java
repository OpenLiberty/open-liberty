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
package com.ibm.ws.javaee.ddmodel.suite.simple.ear;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.javaee.ddmodel.suite.simple.CommonTests_Simple;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

// Module extension supplied context root ("web").
// Overridden by module configuration extension supplied context root ("ext").
// AutoExpand is enabled ("exp").

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DDSimpleEarTests_Web_ConfigExt_Exp extends CommonTests_Simple {
    public static final Class<?> TEST_CLASS = DDSimpleEarTests_Web_ConfigExt_Exp.class;

    @BeforeClass
    public static void setUp() throws Exception {
        commonSetUp(TEST_CLASS, "server_simple_ear_ext_exp.xml", setUpSimpleEar_WebExt);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        commonTearDown(TEST_CLASS, tearDownTestModules, NO_ALLOWED_ERRORS);
    }

    //

    @Test
    public void testContextRoot_Ear_Web_ConfigExt_Exp() throws Exception {
        testHello(TEST_CLASS, EXP_CONTEXT_ROOT);
    }
    
    // Not found ...
    
    @Test
    public void testContextRoot_Ear_Web_ConfigExt_Web() throws Exception {
        testHelloNotFound(TEST_CLASS, WEB_CONTEXT_ROOT);
    }    
    
    @Test
    public void testContextRoot_Ear_Web_ConfigExt_App() throws Exception {
        testHelloNotFound(TEST_CLASS, APP_CONTEXT_ROOT);
    }

    @Test
    public void testContextRoot_Ear_Web_ConfigExp_Config() throws Exception {
        testHelloNotFound(TEST_CLASS, CFG_CONTEXT_ROOT);
    }
}

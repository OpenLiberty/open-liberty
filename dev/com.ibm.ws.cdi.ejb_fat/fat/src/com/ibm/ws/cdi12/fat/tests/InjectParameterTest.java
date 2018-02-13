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

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test Method parameter injection.
 * <p>
 * We had an issue where method parameters were sometimes injected in the wrong order.
 * <p>
 * This test tests method parameter injection on a servlet, an EJB and a CDI bean.
 */
@Mode(TestMode.FULL)
public class InjectParameterTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12EJB32Server");


    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class, "injectParameters.war")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestEjb")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestEjbServlet")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestServlet")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestProducer")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestUtils")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestCdiBean")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestCdiBeanServlet");
    }

    private static String EXPECTED_RESPONSE = "test1, test2, test3, test4, test5, test6, test7, test8, test9, test10, test11, test12, test13, test14, test15, test16";

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testServletParameterInjection() throws Exception {
        verifyResponse("/injectParameters/TestServlet", EXPECTED_RESPONSE);
    }

    @Test
    public void testEjbParameterInjection() throws Exception {
        verifyResponse("/injectParameters/TestEjb", EXPECTED_RESPONSE);
    }

    @Test
    public void testCdiBeanParameterInjection() throws Exception {
        verifyResponse("/injectParameters/TestCdiBean", EXPECTED_RESPONSE);
    }
}

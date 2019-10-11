/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tests.anno.caching;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verify basic handling of servlet annotations.
 */
public class MetadataIncompleteTest extends AnnoCachingTest {
    private static final Logger LOG = Logger.getLogger(MetadataIncompleteTest.class.getName());

    //

    public static final String EAR_NAME = "TestServlet40.ear";

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("setUp: ENTER");

        setSharedServer();
        installJvmOptions("JvmOptions_Enabled.txt");

        setEarName(EAR_NAME);
        addToAppsDir( createApp() );
        installServerXml("jandexDefaultsAutoExpandTrue_server.xml");

        startServerScrub();

        LOG.info("Complete");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOG.info("tearDown: ENTER");
        stopServer();
        LOG.info("tearDown: RETURN");
    }

    //

    @Test
    public void metadataIncomplete_testSimpleServlet() throws Exception {
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
    }

    @Test
    public void metadataIncomplete_testMyServlet() throws Exception {
        verifyResponse("/TestServlet40/MyServlet", "Hello World");
    }

    @Test
    public void metadataIncomplete_testServletA() throws Exception {
        verifyResponse("/TestServlet40/ServletA", "Hello From Servlet A");
    }

    @Test
    public void metadataIncomplete_testServletB() throws Exception {
        verifyResponse("/TestServlet40/ServletB", "Hello From Servlet B");
    }

    @Test
    public void metadataIncomplete_testServletC() throws Exception {
        verifyResponse("/TestServlet40/ServletC", "Hello From Servlet C");
    }

    @Test
    public void metadataIncomplete_testServletD() throws Exception {
        verifyResponse("/TestServlet40/ServletD", "Hello From Servlet D");
    }
}

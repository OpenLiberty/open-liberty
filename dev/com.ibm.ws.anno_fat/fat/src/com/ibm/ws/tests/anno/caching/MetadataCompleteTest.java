/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
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
 * Annotation cache test: Verify that a change from metadata-incomplete to
 * metadata-complete is handled by cache invalidation.
 */
public class MetadataCompleteTest extends AnnoCachingTest {
    private static final Logger LOG = Logger.getLogger(MetadataCompleteTest.class.getName());

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

        LOG.info("setUp: RETURN");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        LOG.info("tearDown: ENTER");
        stopServer();
        LOG.info("tearDown: RETURN");
    }

    //

    @Test
    public void metadataComplete_testUpdate() throws Exception {
    	// Step 1: Do a clean start of the server with the pre-set
    	//         metadata-incomplete copy of the web module descriptor.

        startServerScrub();

        // These are available from the descriptor.

        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        // These are available from annotations.

        verifyResponse("/TestServlet40/ServletA", "Hello From Servlet A");
        verifyResponse("/TestServlet40/ServletB", "Hello From Servlet B");
        verifyResponse("/TestServlet40/ServletC", "Hello From Servlet C");
        verifyResponse("/TestServlet40/ServletD", "Hello From Servlet D");

        stopServer();

        // Step 2: Replace the web descriptor with one that has the same
        //         servlets but which is metadata complete.

        useAlternateExpandedWebXml("web-metadata-complete.xml");
        unexpandApp(); // TODO: Temporary: Server start should not re-expand, but it still does.

        // Do a dirty start, then make sure the metadata-complete values are
        // used, instead of the prior annotation based values.

        startServer();

        // These should still be available from the descriptor.

        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
        verifyResponse("/TestServlet40/MyServlet", "Hello World");

        // These should no longer be available, since they were from annotations.

        verifyBadUrl("/TestServlet40/ServletA");
        verifyBadUrl("/TestServlet40/ServletB");
        verifyBadUrl("/TestServlet40/ServletC");
        verifyBadUrl("/TestServlet40/ServletD");

        // "CWWKZ0014W" is normal.  "SRVE0190E" is added because of the 
        // missing servlet URLs.

        stopServer("CWWKZ0014W", "SRVE0190E");
    }
}

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class SimpleMinifiedServerTest {

    static MinifiedServerTestUtils minifyUtils = new MinifiedServerTestUtils();

    @BeforeClass
    public static void setup() throws Exception {
        minifyUtils.setupAndStartMinifiedServer(SimpleMinifiedServerTest.class.getName(),
                                      "com.ibm.ws.kernel.feature.fat.minify",
                                      LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.minify"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        minifyUtils.tearDown();
    }

    @Test
    public void testStaticContentForDefaultServer() throws Exception {
        //a simple test, gets the static file, looks for the eyecatcher & pass lines.
        minifyUtils.testViaHttpGet(new URL(minifyUtils.staticUrlPrefix + "/test.txt"));
    }
}

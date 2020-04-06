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
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import junit.framework.Assert;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * 
 */
@MinimumJavaLevel(javaLevel = 7)
public class HttpSessionAttListenerHttpUnit extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(HttpSessionAttListenerHttpUnit.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_wcServer");

    private static final String LISTENER_SERVLET_URL = "/TestHttpSessionAttrListener/TestAddListener";

    /*
     * This test will first create HttpSessionAttributeListener.
     * Add attribute to it.
     * Call invalidate , which will call removeattribute by server session.
     * 
     * Test will make sure that it passes when Add and Remove Attribute triggers HttpSessionAttributeListener.
     */
    @Test
    @Mode(TestMode.LITE)
    public void test_RemoveAttribute_onInvalidate() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | HttpSessionAttListenerHttpUnit]: test_RemoveAttribute_onInvalidate Start");
        SHARED_SERVER.setExpectedErrors("SRVE8015E:.*");

        try {
            HttpClient HttpClient = new HttpClient();
            PostMethod post = new PostMethod(SHARED_SERVER.getServerUrl(true, LISTENER_SERVLET_URL));

            int responseCode = HttpClient.executeMethod(post);
            LOG.info("Status Code = " + responseCode);

            String responseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + responseBody);

            post.releaseConnection();

            Assert.assertTrue(responseBody.contains("TEST_PASSED"));
        } catch (Exception e)
        {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n [WebContainer | HttpSessionAttListenerHttpUnit]: test_RemoveAttribute_onInvalidate Finish");
        LOG.info("\n /************************************************************************************/");
    }
}

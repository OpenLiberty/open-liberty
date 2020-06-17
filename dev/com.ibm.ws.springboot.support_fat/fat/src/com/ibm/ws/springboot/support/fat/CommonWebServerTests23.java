/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class CommonWebServerTests23 extends CommonWebServerTests {
    @Test
    public void testBasicSpringBootApplication23() throws Exception {
        testBasicSpringBootApplication();
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_23_APP_BASE;
    }

    @Test
    public void test_useJarUrls_enabled() throws FailingHttpStatusCodeException, MalformedURLException, IOException {

        @SuppressWarnings("resource")
        WebClient webClient = new WebClient();
        HtmlButton button = ((HtmlPage) webClient.getPage("http://localhost:" + EXPECTED_HTTP_PORT + "/useJarUrlsTest.html")).getHtmlElementById("button1");
        HtmlPage newPageText = ((HtmlPage) button.click());
        assertTrue("Button click unexpected:" + newPageText.toString(), newPageText.toString().contains("http://localhost:" + EXPECTED_HTTP_PORT + "/buttonClicked"));
        String body = newPageText.getBody().asText();
        assertTrue("Expected content not returned from button push: \n" + body, body.contains("Hello. You clicked a button."));
    }

    @Test
    public void testWebAnnotationsIgnored() throws IOException {
        HttpUtils.findStringInUrl(server, "/testWebListenerAttr", "PASSED");

        // expect a 404 here for a servlet with @WebServlet
        HttpURLConnection conn = HttpUtils.getHttpConnection(server, "/WebServlet");
        assertEquals("Wrong response code.", 404, conn.getResponseCode());
    }

}

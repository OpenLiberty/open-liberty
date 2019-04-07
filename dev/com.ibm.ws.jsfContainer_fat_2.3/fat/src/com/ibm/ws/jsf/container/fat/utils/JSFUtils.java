/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.utils;

import java.net.URL;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 * A utility class for JSF tests.
 */
public class JSFUtils {

    protected static final Class<?> c = JSFUtils.class;

    /**
     * Construct a URL for a test case so a request can be made.
     *
     * @param server - The server that is under test, this is used to get the port and host name.
     * @param contextRoot - The context root of the application
     * @param path - Additional path information for the request.
     * @return - A fully formed URL.
     * @throws Exception
     */
    public static URL createHttpUrl(LibertyServer server, String contextRoot, String path) throws Exception {
        return new URL(createHttpUrlString(server, contextRoot, path));
    }

    /**
     * Construct a URL for a test case so a request can be made.
     *
     * @param server - The server that is under test, this is used to get the port and host name.
     * @param contextRoot - The context root of the application
     * @param path - Additional path information for the request.
     * @return - A fully formed URL string.
     * @throws Exception
     */
    public static String createHttpUrlString(LibertyServer server, String contextRoot, String path) {

        StringBuilder sb = new StringBuilder();
        sb.append("http://")
          .append(server.getHostname())
          .append(":")
          .append(server.getHttpDefaultPort())
          .append("/")
          .append(contextRoot)
          .append("/")
          .append(path);

        return sb.toString();
    }
    
    /**
     * Create a custom wait mechanism that waits for any background JavaScript to finish
     * and verifies a message in the page response.
     *
     * @param page The current HtmlPage
     * @return A boolean value indicating if the response message was found
     * @throws InterruptedException
     */
    public static boolean waitForPageResponse(HtmlPage page, String responseMessage) throws InterruptedException {
        int i = 0;
        boolean isTextFound = false;
        while (!isTextFound && i < 5) {
            isTextFound = page.asText().contains(responseMessage);
            i++;
            Thread.sleep(1000);
            Log.info(c, "waitForPageResponse", "Waiting for: " + responseMessage + " isTextFound: " + isTextFound + " i: " + i);
        }
        return isTextFound;
    }
}


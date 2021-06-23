/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.custom.junit.runner.Mode;

public class WsBndServiceRefOverrideTest extends WsBndServiceRefOverrideTest_Lite {

    /**
     * Test the valid address in serviceRef, and no address defined in port element.
     *
     * @throws Exception
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testXmlValidServiceRefAddress() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testXmlValidServiceRefAddress.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#ENDPOINT_ADDRESS#", getDefaultEndpointAddr());

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertTrue("The returned result is not expected: " + getDefaultEndpointAddr() + "," + result, "Hello".equals(result));
    }

    /**
     * Test the valid address in serviceRef, and no namespace defined in port element.
     *
     * @throws Exception
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testXmlValidServiceRefAddressWithoutPortNamespace() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testXmlValidServiceRefAddressWithoutPortNamespace.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#ENDPOINT_ADDRESS#", getDefaultEndpointAddr());

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertTrue("The returned result is not expected: " + getDefaultEndpointAddr() + "," + result, "Hello".equals(result));
    }

    /**
     * Test the valid address in serviceRef, and no namespace defined in port element.
     *
     * @throws Exception
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testXmlValidServiceRefAddressWithoutMatchedPort() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "WsBndServiceRefOverrideTest", "ibm-ws-bnd_testXmlValidServiceRefAddressWithoutMatchedPort.xml",
                                      "dropins/wsBndServiceRefOverride.war/WEB-INF/", "ibm-ws-bnd.xml");
        TestUtils.replaceServerFileString(server, "dropins/wsBndServiceRefOverride.war/WEB-INF/ibm-ws-bnd.xml", "#ENDPOINT_ADDRESS#", getDefaultEndpointAddr());

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*wsBndServiceRefOverride");
        String result = getServletResponse(getServletAddr());
        assertTrue("The returned result is not expected: " + getDefaultEndpointAddr() + "," + result, "Hello".equals(result));
    }
}

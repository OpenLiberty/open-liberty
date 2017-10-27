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

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class WebBeansBeansXmlInWeldTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12WebBeansBeansXmlServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testInterceptorsWithWebBeansBeansXml() throws Exception {
        this.verifyResponse("/webBeansBeansXmlInterceptors/", "Last Intercepted by: BasicInterceptor");
    }

    @Test
    public void testDecoratorsWithWebBeansBeansXml() throws Exception {
        this.verifyResponse("/webBeansBeansXmlDecorators/", "decorated message");
    }

    /**
     * Stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * W WELD-001208: Error when validating
             * wsjar:file:/C:/workspaces/KateCDOpenStreamLiberty/build.image/wlp/usr/servers/cdi12WebBeansBeansXmlServer/workarea/org.eclipse.osgi
             * /65/data/cache/com.ibm.ws.app.manager_13/.cache/webBeansBeansXmlDecorators.war!/WEB-INF/beans.xml@4 against xsd. cvc-elt.1: Cannot find the declaration of element
             * 'WebBeans'.
             * W WELD-001208: Error when validating
             * wsjar:file:/C:/workspaces/KateCDOpenStreamLiberty/build.image/wlp/usr/servers/cdi12WebBeansBeansXmlServer/workarea/org.eclipse.osgi
             * /65/data/cache/com.ibm.ws.app.manager_12/.cache/webBeansBeansXmlInterceptors.war!/WEB-INF/beans.xml@4 against xsd. cvc-elt.1: Cannot find the declaration of element
             * 'WebBeans'.
             *
             * The following exception has been seen but as long as the test passes
             * then we are happy that the application did manage to start eventually
             * so we will also ignore the following exception:
             * CWWKZ0022W: Application webBeansBeansXmlInterceptors has not started in 30.001 seconds.
             */
            SHARED_SERVER.getLibertyServer().stopServer("WELD-001208", "CWWKZ0022W");
        }
    }
}

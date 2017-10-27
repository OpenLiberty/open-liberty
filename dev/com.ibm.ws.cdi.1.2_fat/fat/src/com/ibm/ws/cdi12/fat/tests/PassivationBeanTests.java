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
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

public class PassivationBeanTests extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("cdi12PassivationServer");

    @Test
    public void testTransientReference() throws Exception {
        //Enable when 158940 is complete.
        this.verifyResponse("/passivaiton/", new String[] { "destroyed-one", "doNothing2destroyed-two" });
        this.verifyResponse("/passivaiton/", new String[] { "destroyed-one" });
    }

    /**
     * Passivation Capability FVT Test Group
     *
     * @throws Exception
     *             if validation fails, or if an unexpected error occurs
     */
    @Test
    public void testTransientReferenceInPassivation() throws Exception {

        //I don't know why adding a shared web browser is nessacary here. It wasn't in the CDI1.0 test I'm copying.
        WebBrowser wb = createWebBrowserForTestCase();

        this.verifyResponse(wb, "/transientReferenceInSessionPersist/PassivationCapability.jsp",
                            new String[] { "Initialized", "PASSED", "MyStatefulSessionBean was destroyed", "injected into PassivatationBean and it has been destroyed" });

        SHARED_SERVER.getApplicationMBean("transientReferenceInSessionPersist").restart();

        this.verifyResponse(wb, "/transientReferenceInSessionPersist/PassivationCapability.jsp",
                            new String[] { "Reused", "PASSED", "destroy" });
    }

    @Override
    protected SharedServer getSharedServer() {

        return SHARED_SERVER;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (SHARED_SERVER.getLibertyServer() != null) {
            SHARED_SERVER.getLibertyServer().stopServer();
        }
    }

}

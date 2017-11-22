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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * Test to ensure that we correctly discover and fire events for types and beans which are EJBs
 */
public class EjbDiscoveryTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12EjbDiscoveryServer");

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    private String getObservations() throws Exception {
        return SHARED_SERVER.getResponse(createWebBrowserForTestCase(), "/ejbDiscovery").getResponseBody();
    }

    @Test
    public void testAnnotatedTypesDiscovered() throws Exception {
        String observations = getObservations();
        assertThat(observations, containsString("Observed type: com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonBean"));
        assertThat(observations, containsString("Observed type: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulBean"));
        assertThat(observations, containsString("Observed type: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessBean"));
    }

    @Test
    public void testDeploymentDescriptorTypesDiscovered() throws Exception {
        String observations = getObservations();
        assertThat(observations, containsString("Observed type: com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonDdBean"));
        assertThat(observations, containsString("Observed type: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulDdBean"));
        assertThat(observations, containsString("Observed type: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessDdBean"));
    }

    @Test
    public void testAnnotatedBeansDiscovered() throws Exception {
        String observations = getObservations();
        assertThat(observations, containsString("Observed bean: com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonBean"));
        assertThat(observations, containsString("Observed bean: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulBean"));
        assertThat(observations, containsString("Observed bean: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessBean"));
    }

    @Test
    public void testDeploymentDescriptorBeansDiscovered() throws Exception {
        String observations = getObservations();
        assertThat(observations, containsString("Observed bean: com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonDdBean"));
        assertThat(observations, containsString("Observed bean: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulDdBean"));
        assertThat(observations, containsString("Observed bean: com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessDdBean"));
    }

    @Test
    public void testNoInterfaceTypesDiscovered() throws Exception {
        String observations = getObservations();
        // The singleton and stateful beans have a no-interface view
        assertThat(observations, containsString("Observed bean type: class com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonBean"));
        assertThat(observations, containsString("Observed bean type: class com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulBean"));
        assertThat(observations, containsString("Observed bean type: class com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonDdBean"));
        assertThat(observations, containsString("Observed bean type: class com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulDdBean"));
    }

    @Test
    public void testInterfaceTypesDiscovered() throws Exception {
        String observations = getObservations();
        // The two stateless beans have a local interface defined
        assertThat(observations, containsString("Observed bean type: interface com.ibm.ws.cdi12.ejbdiscovery.ejbs.interfaces.StatelessLocal"));
        assertThat(observations, containsString("Observed bean type: interface com.ibm.ws.cdi12.ejbdiscovery.ejbs.interfaces.StatelessDdLocal"));

        // The actual bean type should not be visible
        assertThat(observations, not(containsString("Observed bean type: class com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessBean")));
        assertThat(observations, not(containsString("Observed bean type: class com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessDdBean")));
    }

    @Test
    public void testModeNoneNotDiscovered() throws Exception {
        String observations = getObservations();
        // There is a stateless bean that should not be discovered because the .war has discovery-mode=none
        assertThat(observations, not(containsString("Observed bean type: class com.ibm.ws.cdi12.ejbdiscovery.none.ejbs.StatelessBean")));
        assertThat(observations, not(containsString("Observed bean: com.ibm.ws.cdi12.ejbdiscovery.none.ejbs.StatelessBean")));
    }

}

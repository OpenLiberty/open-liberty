/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;

/**
 * Test to ensure that we correctly discover and fire events for types and beans which are EJBs
 */
public class EjbDiscoveryTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapServer SHARED_SERVER = new ShrinkWrapServer("cdi12EjbDiscoveryServer");

    @Override
    protected ShrinkWrapServer getSharedServer() {
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

/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.xml.ejb.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.managedbeans.fat.xml.ejb.StatelessEJBforNonCompManagedBean;
import com.ibm.ws.managedbeans.fat.xml.ejb.StatelessEJBforTestingManagedBean;

import componenttest.app.FATServlet;

@WebServlet("/ManagedBeanXmlServlet")
public class ManagedBeanXmlServlet extends FATServlet {

    private static final long serialVersionUID = 7800020198391473374L;
    private static final String APP = "ManagedBeanXmlApp";
    private static final String MOD = "ManagedBeanXmlWeb";
    private static final String EJB_MOD = "ManagedBeanXmlEJB";
    private static final String BEAN = "NamedManagedBean";

    private static final String JAVA_GLOBAL_EJB = "java:global/" + APP + "/" + EJB_MOD + "/";
    private static final String TEST_INTERFACE = "!" + StatelessEJBforTestingManagedBean.class.getName();

    String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + BEAN;
    String appLookupName = "java:app/" + MOD + "/" + BEAN;
    String modLookupName = "java:module" + "/" + BEAN;

    @Resource(lookup = "java:module/NamedManagedBean")
    private NamedManagedBean ivNMB;
    @Resource
    private SimpleManagedBean ivSMB;

    /**
     * Test lookup of a named ManagedBean in java:global/app/module
     */
    @Test
    public void testLookupOfNamedManagedBeanInWarWithXmlEJB() throws Exception {

        NamedManagedBean globalNMB = null, appNMB = null, modNMB = null;

        try {
            globalNMB = (NamedManagedBean) new InitialContext().lookup(globalLookupName);
            fail("lookup in java:global should have failed; ManagedBean never bound there : " + globalNMB);
        } catch (NamingException ex) {
            // Expected NamingException for java:global lookup
        }

        appNMB = (NamedManagedBean) new InitialContext().lookup(appLookupName);
        assertNotNull("java:app lookup returned null", appNMB);

        modNMB = (NamedManagedBean) new InitialContext().lookup(modLookupName);
        assertNotNull("java:module lookup returned null", modNMB);
    }

    /**
     * Test injection of ManagedBeans into a servlet
     */
    @Test
    public void testInjectionOfManagedBeanInWarWithXmlEJB() throws Exception {
        assertNotNull("Named ManagedBean was not injected", ivNMB);
        assertNotNull("Simple ManagedBean was not injected", ivSMB);
    }

    /**
     * Test that ManagedBeans are stateful
     */
    @Test
    public void testManagedBeansAreStatefulInWarWithXmlEJB() throws Exception {

        NamedManagedBean modNMB = (NamedManagedBean) new InitialContext().lookup(modLookupName);

        ivNMB.setValue("Scooby");
        ivSMB.setValue("Velma");
        modNMB.setValue("Shaggy");

        assertEquals("Named ManagedBean contained incorrect value", "Scooby", ivNMB.getValue());
        assertEquals("Simple ManagedBean contained incorrect value", "Velma", ivSMB.getValue());
        assertEquals("java:module ManagedBean contained incorrect value", "Shaggy", modNMB.getValue());
    }

    /**
     * Tests that a simple ManagedBean (with no injection or interceptors) may
     * be injected into an EJB. A lookup should fail, since the ManagedBean
     * does not have a name. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testSimpleManagedBeanInjectionAndLookupInXmlEJB() throws Exception {
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) new InitialContext().lookup(JAVA_GLOBAL_EJB + "StatelessEJBforSimpleManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected, but not looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean (with no injection or interceptors) may
     * be injected into an EJB and looked up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testNamedManagedBeanInjectionAndLookupInXmlEJB() throws Exception {
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) new InitialContext().lookup(JAVA_GLOBAL_EJB + "StatelessEJBforNamedManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean with simple injection (no interceptors) may
     * be injected into an EJB and looked up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInjectionManagedBeanInjectionAndLookupInXmlEJB() throws Exception {
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) new InitialContext()
                        .lookup(JAVA_GLOBAL_EJB + "StatelessEJBforInjectionManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean with simple injection and PostConstruct
     * (no around invoke interceptors) may be injected into an EJB and looked
     * up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testPostConstructManagedBeanInjectionAndLookupInXmlEJB() throws Exception {
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) new InitialContext()
                        .lookup(JAVA_GLOBAL_EJB + "StatelessEJBforPostConstructManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean with simple injection and PreDestroy
     * (no around invoke interceptors) may be injected into an EJB and looked
     * up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testPreDestroyManagedBeanInjectionAndLookupInXmlEJB() throws Exception {
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) new InitialContext()
                        .lookup(JAVA_GLOBAL_EJB + "StatelessEJBforPreDestroyManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a named ManagedBean with simple injection and AroundInvoke
     * interceptors may be injected into an EJB and looked up in both java:app
     * and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInterceptorManagedBeanInjectionAndLookupInXmlEJB() throws Exception {
        // Locate Stateless local bean
        StatelessEJBforTestingManagedBean bean = (StatelessEJBforTestingManagedBean) new InitialContext()
                        .lookup(JAVA_GLOBAL_EJB + "StatelessEJBforInterceptorManaged" + TEST_INTERFACE);

        // Verify that the ManagedBean may be injected and looked up
        bean.verifyManagedBeanInjectionAndLookup();
    }

    /**
     * Tests that a ManagedBean can declare java:global, java:app, and
     * java:module resources
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testNonCompManagedBeanXml() throws Exception {
        StatelessEJBforNonCompManagedBean ejb = (StatelessEJBforNonCompManagedBean) new InitialContext().lookup(JAVA_GLOBAL_EJB + "StatelessEJBforNonCompManagedBean");
        ejb.verifyNonCompLookup();
    }

}

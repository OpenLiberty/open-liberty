/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.ejbdiscovery.servlet;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.ejb.apps.ejbdiscovery.extension.DiscoveryExtension;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/")
public class DiscoveryServlet extends FATServlet {

    @Inject
    private DiscoveryExtension extension;

    private static void assertContains(Set<?> set, Object contains) {
        assertTrue(contains + " not found in " + set, set.contains(contains));
    }

    private static void assertNotContains(Set<?> set, Object contains) {
        assertTrue(contains + " found in " + set, !set.contains(contains));
    }

    @Test
    public void testAnnotatedTypesDiscovered() throws Exception {
        Set<Class<?>> types = extension.getObservedTypes();
        assertContains(types, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonBean.class);
        assertContains(types, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulBean.class);
        assertContains(types, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessBean.class);
    }

    @Test
    public void testDeploymentDescriptorTypesDiscovered() throws Exception {
        Set<Class<?>> types = extension.getObservedTypes();
        assertContains(types, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonDdBean.class);
        assertContains(types, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulDdBean.class);
        assertContains(types, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessDdBean.class);
    }

    @Test
    public void testAnnotatedBeansDiscovered() throws Exception {
        Set<Class<?>> beans = extension.getObservedBeans();
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonBean.class);
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulBean.class);
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessBean.class);
    }

    @Test
    public void testDeploymentDescriptorBeansDiscovered() throws Exception {
        Set<Class<?>> beans = extension.getObservedBeans();
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonDdBean.class);
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulDdBean.class);
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessDdBean.class);
    }

    @Test
    public void testNoInterfaceTypesDiscovered() throws Exception {
        Set<Class<?>> beans = extension.getObservedBeans();
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonBean.class);
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulBean.class);
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.SingletonDdBean.class);
        assertContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatefulDdBean.class);
    }

    @Test
    public void testInterfaceTypesDiscovered() throws Exception {
        Set<Type> beanTypes = extension.getObservedBeanTypes();
        // The two stateless beans have a local interface defined
        assertContains(beanTypes, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.interfaces.StatelessLocal.class);
        assertContains(beanTypes, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.interfaces.StatelessDdLocal.class);

        // The actual bean type should not be visible
        assertNotContains(beanTypes, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessBean.class);
        assertNotContains(beanTypes, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.ejbs.StatelessDdBean.class);
    }

    @Test
    public void testModeNoneNotDiscovered() throws Exception {
        Set<Class<?>> beans = extension.getObservedBeans();
        // There is a stateless bean that should not be discovered because the .jar has discovery-mode=none
        assertNotContains(beans, com.ibm.ws.cdi.ejb.apps.ejbdiscovery.none.UndiscoveredStatelessBean.class);
    }

}

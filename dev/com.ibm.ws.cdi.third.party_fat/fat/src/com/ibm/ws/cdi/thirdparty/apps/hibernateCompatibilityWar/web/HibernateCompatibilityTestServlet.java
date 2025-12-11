/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.thirdparty.apps.hibernateCompatibilityWar.web;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

//import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.cdi.thirdparty.apps.hibernateCompatibilityWar.model.TestEntity;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test servlet to verify Hibernate CDI compatibility
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/Hibernate6CompatibilityTestServlet")
public class HibernateCompatibilityTestServlet extends FATServlet {

    @PersistenceUnit(unitName = "TestPU")
    private EntityManagerFactory emf;

    @Inject
    @CustomEM
    private EntityManager injectedEm;

    @Inject
    private BeanManager beanManager;

    @Resource
    private UserTransaction tx;

    /**
     * Test to check whether the bean managers are all set properly
     * 
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testBeanManagerProperty() throws Exception {
        // Get the underlying properties from the EntityManagerFactory
        Map<String, Object> properties = emf.getProperties();
        boolean hasBeanManagerInterface = false;
        boolean hasExtendedBeanManagerInterface = false;
        boolean hasDeprecatedExtendedBeanManager = false;

        Assert.assertNotNull("BeanManager property should be set", properties.get("javax.persistence.bean.manager"));

        Object beanManagerProperty = properties.get("javax.persistence.bean.manager");
        Class<?>[] interfaces = beanManagerProperty.getClass().getInterfaces();
        for (Class<?> iface : interfaces) {
            if (iface.getName().equals("javax.enterprise.inject.spi.BeanManager")) {
                hasBeanManagerInterface = true;
            }
            if (iface.getName().equals("org.hibernate.resource.beans.container.spi.ExtendedBeanManager")) {
                hasExtendedBeanManagerInterface = true;
            }
            if (iface.getName().equals("org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager")) {
                hasDeprecatedExtendedBeanManager = true;
            }
        }

        Assert.assertTrue("Proxy should implement BeanManager interface", hasBeanManagerInterface);
        Assert.assertTrue("Proxy should implement ExtendedBeanManager interface", hasExtendedBeanManagerInterface);

        /*
         * The deprecated extended bean manager were removed from hibernate starting from the version
         * 6.6.32. So the deprecated bean manager will be set only for versions before that
         */

        String hibernateVersion = "7.0.9"; //taken from hibernateJakartaSearchLibs in build.gradle. 

        if (isHibernateVersionAtLeast(hibernateVersion, 6, 6, 23)) {
            Assert.assertFalse("Should not implement deprecated ExtendedBeanManager in Hibernate 6.6+", hasDeprecatedExtendedBeanManager);
        } else {
            Assert.assertTrue("Should implement deprecated ExtendedBeanManager in Hibernate version less than 6.6", hasDeprecatedExtendedBeanManager);
        }
    }

    @Test
    @Mode(TestMode.FULL)
    /**
     * Checking if the deprecated interface is present in the classpath and seeing whether the
     * cdi update has set it as extended bean manager
     * 
     * @throws Exception
     */
    public void testDeprecatedExtendedBeanManagerIntegration() throws Exception {

        boolean deprecatedInterfaceAvailable;
        try {
            Class.forName("org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager");
            deprecatedInterfaceAvailable = true;
        } catch (ClassNotFoundException e) {
            deprecatedInterfaceAvailable = false;
        } catch (Exception e) {
            deprecatedInterfaceAvailable = false;
        }

        Map<String, Object> properties = emf.getProperties();
        boolean proxyImplementsDeprecated = false;
        Object proxy = properties.get("javax.persistence.bean.manager");
        Object beanManagerProperty = properties.get("javax.persistence.bean.manager");
        Class<?>[] interfaces = beanManagerProperty.getClass().getInterfaces();
        for (Class<?> iface : interfaces) {
            if (iface.getName().equals("org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager")) {
                proxyImplementsDeprecated = true;
            }
        }

        if (deprecatedInterfaceAvailable) {
            Assert.assertTrue("Proxy should implement deprecated ExtendedBeanManager when available", proxyImplementsDeprecated);
        } else {
            Assert.assertFalse("Proxy should NOT implement deprecated ExtendedBeanManager when not available", proxyImplementsDeprecated);
        }
    }

    /**
     * Method to check whether the hibernate version passed is atleast equalt to or less than the version passed
     * 
     * @param versionString
     * @param major
     * @param minor
     * @param patch
     * @return
     */
    private boolean isHibernateVersionAtLeast(String versionString, int major, int minor, int patch) {
        try {
            String[] parts = versionString.split("\\.");
            int majorVersion = Integer.parseInt(parts[0]);
            int minorVersion = Integer.parseInt(parts[1]);
            int patchVersion = parts.length > 2 ? Integer.parseInt(parts[2].replaceAll("[^0-9]", "")) : 0;

            if (majorVersion > major)
                return true;
            if (majorVersion < major)
                return false;
            if (minorVersion > minor)
                return true;
            if (minorVersion < minor)
                return false;
            return patchVersion >= patch;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Test that the CDI injection works with Hibernate
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCDIInjection() throws Exception {
        Assert.assertNotNull("EntityManager should be injected", injectedEm);

        try {
            tx.begin();
            TestEntity entity = new TestEntity("Test Entity");
            injectedEm.persist(entity);
            tx.commit();

            // Verify the entity was persisted
            TestEntity found = injectedEm.find(TestEntity.class, entity.getId());
            Assert.assertNotNull("Entity should be found", found);
            Assert.assertEquals("Entity name should match", "Test Entity", found.getName());
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    /**
     * Test that the updateProperties method in CDIJPAEMFPropertyProviderImpl correctly
     * handles Hibernate compatibility
     */
    @Test
    @Mode(TestMode.FULL)
    public void testUpdatePropertiesMethod() throws Exception {
        Map<String, Object> props = new HashMap<>();

        // Verify that the BeanManager proxy is set in the EMF properties
        Map<String, Object> emfProps = emf.getProperties();
        Object beanManagerProxy = emfProps.get("javax.persistence.bean.manager");
        Assert.assertNotNull("BeanManager proxy should be set in EMF properties", beanManagerProxy);

        InvocationHandler handler = Proxy.getInvocationHandler(beanManagerProxy);

        // The handler class name should contain BeanManagerInvocationHandler
        Assert.assertTrue("Handler should be a BeanManagerInvocationHandler",
                          handler.getClass().getName().contains("BeanManagerInvocationHandler"));
    }
}

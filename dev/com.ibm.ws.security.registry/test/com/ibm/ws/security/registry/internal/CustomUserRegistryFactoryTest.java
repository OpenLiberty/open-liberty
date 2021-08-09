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
package com.ibm.ws.security.registry.internal;

import java.util.Hashtable;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.security.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

/**
 *
 */
public class CustomUserRegistryFactoryTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    static final String KEY_CONFIG_ID = "config.id";
    static final String KEY_SERVICE_ID = "service.id";
    static final String KEY_TYPE_CUSTOM = "CUSTOM";

    private CustomUserRegistryFactory customUserRegistryFactory;
    private ServiceReference<UserRegistry> userRegistryServiceReference;
    private final UserRegistry customUserRegistry = mockery.mock(UserRegistry.class, "ur1");
    private final UserRegistry anotherCustomUserRegistry = mockery.mock(UserRegistry.class, "ur2");
    private ComponentContext componentContext;
    private BundleContext bundleContext;
    private final String configId = "456";
    private final String anotherConfigId = "789";
    private final Long serviceId = new Long(123);

    @SuppressWarnings({ "unchecked" })
    @Before
    public void setUp() throws Exception {
        customUserRegistryFactory = new CustomUserRegistryFactory();

        componentContext = mockery.mock(ComponentContext.class);
        bundleContext = mockery.mock(BundleContext.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testActivation() {
        createActivationExpectations();
        createRegistryServiceReferenceExpectations(null, serviceId);
        createRegistryConfigurationExpectations(getWrapperProps(serviceId.toString()));
        customUserRegistryFactory.setCustomUserRegistry(customUserRegistry, getCustomProps(serviceId.toString()));
        customUserRegistryFactory.activate(bundleContext);
    }

    @Test
    public void testActivationWithConfigId() {
        createActivationExpectations();
        createRegistryServiceReferenceExpectations(configId, serviceId);
        createRegistryConfigurationExpectations(getWrapperProps(configId));
        customUserRegistryFactory.setCustomUserRegistry(customUserRegistry, getCustomProps(configId));
        customUserRegistryFactory.activate(bundleContext);
    }

    @Test
    public void testActivationWithTwoRegistries() {
        createActivationExpectations();
        createRegistryServiceReferenceExpectations(configId, serviceId);
        createRegistryConfigurationExpectations(getWrapperProps(configId));
        createRegistryServiceReferenceExpectations(anotherConfigId, null);
        createRegistryConfigurationExpectations(getWrapperProps(anotherConfigId));
        customUserRegistryFactory.setCustomUserRegistry(customUserRegistry, getCustomProps(configId));
        customUserRegistryFactory.setCustomUserRegistry(anotherCustomUserRegistry, getCustomProps(anotherConfigId));
        customUserRegistryFactory.activate(bundleContext);
    }

    @Test
    public void testUnsetCustomUserRegistry() {
        createActivationExpectations();
        userRegistryServiceReference = createRegistryServiceReferenceExpectations(null, serviceId);
        ServiceRegistration<com.ibm.ws.security.registry.UserRegistry> registration = createRegistryConfigurationExpectations(getWrapperProps(serviceId.toString()));
        customUserRegistryFactory.setCustomUserRegistry(customUserRegistry, getCustomProps(serviceId.toString()));
        customUserRegistryFactory.activate(bundleContext);

        createUnregistrationExpectations(registration);
        customUserRegistryFactory.unsetCustomUserRegistry(getCustomProps(serviceId.toString()));
    }

    @Test
    public void testDeactivation() {
        createActivationExpectations();
        userRegistryServiceReference = createRegistryServiceReferenceExpectations(null, serviceId);
        ServiceRegistration<com.ibm.ws.security.registry.UserRegistry> registration = createRegistryConfigurationExpectations(getWrapperProps(serviceId.toString()));
        customUserRegistryFactory.setCustomUserRegistry(customUserRegistry, getCustomProps(serviceId.toString()));
        customUserRegistryFactory.activate(bundleContext);

        createUnregistrationExpectations(registration);
        customUserRegistryFactory.deactivate();
    }

    @Test
    public void testDeactivationWithTwoRegistries() {
        createActivationExpectations();
        userRegistryServiceReference = createRegistryServiceReferenceExpectations(configId, serviceId);
        ServiceRegistration<com.ibm.ws.security.registry.UserRegistry> registration = createRegistryConfigurationExpectations(getWrapperProps(configId));
        createRegistryServiceReferenceExpectations(anotherConfigId, null);
        ServiceRegistration<com.ibm.ws.security.registry.UserRegistry> anotherRegistration = createRegistryConfigurationExpectations(getWrapperProps(anotherConfigId));
        customUserRegistryFactory.setCustomUserRegistry(customUserRegistry, getCustomProps(configId));
        customUserRegistryFactory.setCustomUserRegistry(anotherCustomUserRegistry, getCustomProps(anotherConfigId));
        customUserRegistryFactory.activate(bundleContext);

        createUnregistrationExpectations(registration);
        createUnregistrationExpectations(anotherRegistration);
        customUserRegistryFactory.deactivate();
    }

    private void createActivationExpectations() {
        createComponentContextExpectations();
    }

    private void createComponentContextExpectations() {
    }


    @SuppressWarnings("unchecked")
    private ServiceReference<UserRegistry> createRegistryServiceReferenceExpectations(final String configId, final Long serviceId) {
        final ServiceReference<UserRegistry> userRegistryServiceReference = mockery.mock(ServiceReference.class, configId + "-" + serviceId);
        return userRegistryServiceReference;
    }

    @SuppressWarnings("unchecked")
    private ServiceRegistration<com.ibm.ws.security.registry.UserRegistry> createRegistryConfigurationExpectations(final Hashtable<String, Object> userRegistryConfigurationProperties) {
        final ServiceRegistration<com.ibm.ws.security.registry.UserRegistry> userRegistryConfigurationRegistration = mockery.mock(ServiceRegistration.class,
                                                                                                                                  "userRegistryConfigurationRegistration"
                                                                                                                                                             + userRegistryConfigurationProperties.get(KEY_CONFIG_ID));
        mockery.checking(new Expectations() {
            {
                one(bundleContext).registerService(with(com.ibm.ws.security.registry.UserRegistry.class), with(aNonNull(com.ibm.ws.security.registry.UserRegistry.class)),
                                                   with(userRegistryConfigurationProperties));
                will(returnValue(userRegistryConfigurationRegistration));
            }
        });
        return userRegistryConfigurationRegistration;
    }

    Hashtable<String, Object> getCustomProps(String id) {
        final Hashtable<String, Object> userRegistryConfigurationProperties = new Hashtable<String, Object>();
        userRegistryConfigurationProperties.put(KEY_CONFIG_ID, id);
        return userRegistryConfigurationProperties;
    }

    Hashtable<String, Object> getWrapperProps(String id) {
        final Hashtable<String, Object> userRegistryConfigurationProperties = new Hashtable<String, Object>();
        userRegistryConfigurationProperties.put(UserRegistryService.REGISTRY_TYPE, KEY_TYPE_CUSTOM);
        userRegistryConfigurationProperties.put(KEY_CONFIG_ID, KEY_TYPE_CUSTOM + "_" + id);
        return userRegistryConfigurationProperties;
    }

    private void createUnregistrationExpectations(final ServiceRegistration<com.ibm.ws.security.registry.UserRegistry> registration) {
        mockery.checking(new Expectations() {
            {
                one(registration).unregister();
            }
        });
    }

}

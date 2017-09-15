/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.beanvalidation.config.ValidationConfigurationFactory;
import com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface;
import com.ibm.ws.beanvalidation.mock.MockValidationConfigurator;
import com.ibm.ws.beanvalidation.service.BeanValidationRuntimeVersion;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;

@RunWith(JMock.class)
public class OSGiBeanValidationImplTest {

    private final Mockery mockery = new JUnit4Mockery();

    private final MetaDataSlotService slotService = mockery.mock(MetaDataSlotService.class);
    private final MetaDataSlot mmdSlot = mockery.mock(MetaDataSlot.class);
    private final ModuleMetaData mmd = mockery.mock(ModuleMetaData.class);

    private final Container container = mockery.mock(Container.class);
    private final ValidationConfig dd = mockery.mock(ValidationConfig.class);

    private final ComponentContext cc = mockery.mock(ComponentContext.class);

    @SuppressWarnings("unchecked")
    private final ServiceReference<ValidationConfigurationFactory> configFactorySR =
                    mockery.mock(ServiceReference.class, "configFactorySR");
    @SuppressWarnings("unchecked")
    private final ServiceReference<BeanValidationRuntimeVersion> runtimeVersionSR =
                    mockery.mock(ServiceReference.class, "runtimeVersionSR");

    @SuppressWarnings("unchecked")
    private final ServiceReference<ClassLoadingService> classLoadingServiceSR =
                    mockery.mock(ServiceReference.class, "classLoadingServiceSR");

    private final ValidationConfigurationFactory configFactory =
                    mockery.mock(ValidationConfigurationFactory.class);

    private final ClassLoadingService classLoadingService =
                    mockery.mock(ClassLoadingService.class);

    @SuppressWarnings("unchecked")
    private final MetaDataEvent<ModuleMetaData> event = mockery.mock(MetaDataEvent.class);

    private OSGiBeanValidationImpl bval;

    @Before
    public void beforeEachTest() throws Exception {
        bval = new OSGiBeanValidationImpl();

        // When any of these tests fail, toString is called on all objects that have
        // expectations, so these are needed so that the error report is coherent.
        mockery.checking(new Expectations() {
            {
                allowing(container).getName();
                will(returnValue(""));

                allowing(container).getPath();
                will(returnValue(""));
            }
        });
    }

    @After
    public void cleanUpEachTest() throws Exception {
        bval.deactivate(cc);
    }

    /**
     * This test ensures that a ValidatorFactory can still be retrieved after the
     * moduleMetaDataDestroyed method is called. Due to lack of design, an app thread
     * can still be running so for bval-1.0 compatibility continue to allow this.
     * 
     * 1. Get a ValidatorFactory successfully
     * 2. Run moduleMetaDataDestroyed
     * 3. Get a ValidatorFactory successfully
     */
    @Test
    public void testGetVFAfterMetaDataDestroyedV10() throws Exception {
        final OSGiBeanValidationScopeData scopeData = new OSGiBeanValidationScopeData(container);
        final MockValidationConfigurator configurator = new MockValidationConfigurator(scopeData, mockery);

        setupExpectations(mockery, scopeData, configurator, 3, null);

        bval.setMetaDataSlotService(slotService);
        bval.setValidationConfigFactory(configFactorySR);
        bval.setClassLoadingService(classLoadingServiceSR);
        bval.activate(cc);

        ValidatorFactory vf;
        vf = bval.getValidatorFactory(mmd, null);
        assertNotNull("ValidatorFactory returned should not be null", vf);
        assertSame("the configurator set in scopeData should match the passed in one",
                   configurator, scopeData.configurator);
        assertSame("the VF set in scopeData should match the one returned by getValidatorFactory",
                   vf, scopeData.ivValidatorFactory);
        configurator.assertNotReleased();

        bval.moduleMetaDataDestroyed(event);
        assertTrue("scopeData should indicate that the configurator was released",
                   scopeData.configuratorReleased);
        assertSame("the configurator set in scopeData should STILL match the passed in one",
                   configurator, scopeData.configurator);
        assertSame("the VF set in scopeData should STILL match the one returned by getValidatorFactory",
                   vf, scopeData.ivValidatorFactory);
        configurator.assertReleased();

        vf = bval.getValidatorFactory(mmd, null);
        assertNotNull("ValidatorFactory returned after moduleMetaDataComplete should not be null", vf);
        assertTrue("scopeData should indicate that the configurator was released",
                   scopeData.configuratorReleased);
        assertSame("the configurator set in scopeData should STILL match the passed in one",
                   configurator, scopeData.configurator);
        assertSame("the VF set in scopeData should STILL match the one returned by getValidatorFactory",
                   vf, scopeData.ivValidatorFactory);
    }

    /**
     * This test ensures that a ValidatorFactory cannot be retrieved after the
     * moduleMetaDataDestroyed method is called for bval-1.1 and up.
     * 
     * 1. Get a ValidatorFactory successfully
     * 2. Run moduleMetaDataDestroyed
     * 3. Try to get a ValidatorFactory, but fail
     */
    @Test
    public void testGetVFAfterMetaDataDestroyed() throws Exception {
        final OSGiBeanValidationScopeData scopeData = new OSGiBeanValidationScopeData(container);
        final MockValidationConfigurator configurator = new MockValidationConfigurator(scopeData, mockery);

        setupExpectations(mockery, scopeData, configurator, 3, "1.1");

        bval.setMetaDataSlotService(slotService);
        bval.setValidationConfigFactory(configFactorySR);
        bval.setRuntimeVersion(runtimeVersionSR);
        bval.setClassLoadingService(classLoadingServiceSR);
        bval.activate(cc);

        ValidatorFactory vf;
        vf = bval.getValidatorFactory(mmd, null);
        assertNotNull("ValidatorFactory returned should not be null", vf);
        assertSame("the configurator set in scopeData should match the passed in one",
                   configurator, scopeData.configurator);
        assertSame("the VF set in scopeData should match the one returned by getValidatorFactory",
                   vf, scopeData.ivValidatorFactory);
        configurator.assertNotReleased();

        bval.moduleMetaDataDestroyed(event);

        assertTrue("scopeData should indicate that the configurator was released",
                   scopeData.configuratorReleased);
        assertNull("after moduleMetaDataDestroyed, scopeData configurator should be nulled out",
                   scopeData.configurator);
        assertNull("after moduleMetaDataDestroyed, scopeData VF should be nulled out",
                   scopeData.ivValidatorFactory);
        configurator.assertReleased();

        try {
            bval.getValidatorFactory(mmd, null);
            fail("getValidatorFactory should have failed indicating that the VF has already been destroyed");
        } catch (ValidationException e) {
            // expected exception, make sure the scopeData state is still the same
            assertTrue("scopeData should indicate that the configurator was released",
                       scopeData.configuratorReleased);
            assertNull("after moduleMetaDataDestroyed, scopeData configurator should be nulled out",
                       scopeData.configurator);
            assertNull("after moduleMetaDataDestroyed, scopeData VF should be nulled out",
                       scopeData.ivValidatorFactory);
        }
    }

    /**
     * This test fails to get a ValidatorFactory and ensures that the correct
     * things happen in this v10 bval environment.
     */
    @Test
    public void testFailToGetValidatorFactoryV10() throws Exception {
        final OSGiBeanValidationScopeData scopeData = new OSGiBeanValidationScopeData(container);
        final MockValidationConfigurator configurator = new MockValidationConfigurator(scopeData);

        setupExpectations(mockery, scopeData, configurator, 2, null);

        bval.setMetaDataSlotService(slotService);
        bval.setValidationConfigFactory(configFactorySR);
        bval.setClassLoadingService(classLoadingServiceSR);
        bval.activate(cc);

        configurator.assertNotReleased();
        try {
            bval.getValidatorFactory(mmd, null);
            fail("should have failed to get/create VF because bval provider isn't on the classpath");
        } catch (ValidationException e) {
            assertTrue("exception message should include CWNBV0002E id",
                       e.getMessage().startsWith("CWNBV0002E"));
        }
        assertNull("configurator shouldn't still be set when getVF fails",
                   scopeData.configurator);
        assertNull("getVF failed, so this field should still be null",
                   scopeData.ivValidatorFactory);
        configurator.assertReleased();

        bval.moduleMetaDataDestroyed(event);
        assertTrue("scopeData should indicate the configurator was released",
                   scopeData.configuratorReleased);
        assertNull("the configurator shouldn't have been set since getFV failed",
                   scopeData.configurator);
    }

    /**
     * This test fails to get a ValidatorFactory and ensures that the correct
     * things happen in this v11 and higher bval environment.
     */
    @Test
    public void testFailToGetValidatorFactory() throws Exception {
        final OSGiBeanValidationScopeData scopeData = new OSGiBeanValidationScopeData(container);
        final MockValidationConfigurator configurator = new MockValidationConfigurator(scopeData);

        setupExpectations(mockery, scopeData, configurator, 2, "1.1");

        bval.setMetaDataSlotService(slotService);
        bval.setValidationConfigFactory(configFactorySR);
        bval.setRuntimeVersion(runtimeVersionSR);
        bval.setClassLoadingService(classLoadingServiceSR);
        bval.activate(cc);

        configurator.assertNotReleased();
        try {
            bval.getValidatorFactory(mmd, null);
            fail("should have failed to get/create VF because bval provider isn't on the classpath");
        } catch (ValidationException e) {
            assertTrue("", e.getMessage().startsWith("CWNBV0002E"));
        }
        assertNull("configurator shouldn't still be set when getVF fails",
                   scopeData.configurator);
        assertNull("getVF failed, so this field should still be null",
                   scopeData.ivValidatorFactory);
        configurator.assertReleased();

        bval.moduleMetaDataDestroyed(event);
        assertTrue("scopeData should indicate the configurator was released",
                   scopeData.configuratorReleased);
        assertNull("the configurator SHOULD be nulled out for bval>=v11",
                   scopeData.configurator);
    }

    /**
     * Ensure that if a VF hasn't been created before the module is destroyed,
     * nothing fails and a following getVF still returns a VF for v10 compatibility.
     */
    @Test
    public void testMetaDataDestroyedBeforeFirstGetVFV10() throws Exception {
        final OSGiBeanValidationScopeData scopeData = new OSGiBeanValidationScopeData(container);
        final MockValidationConfigurator configurator = new MockValidationConfigurator(scopeData, mockery);

        setupExpectations(mockery, scopeData, configurator, 2, null);

        bval.setMetaDataSlotService(slotService);
        bval.setValidationConfigFactory(configFactorySR);
        bval.setClassLoadingService(classLoadingServiceSR);
        bval.activate(cc);

        bval.moduleMetaDataDestroyed(event);
        assertTrue("scopeData should indicate the configurator was released",
                   scopeData.configuratorReleased);
        assertNull("the configurator should be null", scopeData.configurator);

        ValidatorFactory vf = bval.getValidatorFactory(mmd, null);
        assertNotNull("ValidatorFactory returned should not be null", vf);
        assertSame("the configurator set in scopeData should match the passed in one",
                   configurator, scopeData.configurator);
        assertSame("the VF set in scopeData should match the one returned by getValidatorFactory",
                   vf, scopeData.ivValidatorFactory);
        configurator.assertNotReleased();
    }

    /**
     * Ensure that if a VF hasn't been created before the module is destroyed,
     * nothing fails and a following getVF throws an exception saying the
     * module has been destroyed already.
     */
    @Test
    public void testMetaDataDestroyedBeforeFirstGetVF() throws Exception {
        final OSGiBeanValidationScopeData scopeData = new OSGiBeanValidationScopeData(container);

        // Won't get past initial checking to create VF, so can pass in null for
        // the irrelevant fields here.
        setupExpectations(mockery, scopeData, null, 2, "1.1", null, cc, configFactory, 0, 0);

        bval.setMetaDataSlotService(slotService);
        bval.setRuntimeVersion(runtimeVersionSR);
        bval.setValidationConfigFactory(configFactorySR);
        bval.setClassLoadingService(classLoadingServiceSR);
        bval.activate(cc);

        bval.moduleMetaDataDestroyed(event);
        assertTrue("scopeData should indicate the configurator was released",
                   scopeData.configuratorReleased);
        assertNull("the configurator should be null", scopeData.configurator);

        try {
            bval.getValidatorFactory(mmd, null);
            fail("getValidatorFactory should have failed indicating that we are " +
                 "trying to get the VF after the module is destroyed");
        } catch (ValidationException e) {
            // expected exception, make sure the scopeData state is still the same
            assertTrue("scopeData should indicate that the configurator was released",
                       scopeData.configuratorReleased);
            assertNull("after moduleMetaDataDestroyed, scopeData configurator should be nulled out",
                       scopeData.configurator);
            assertNull("after moduleMetaDataDestroyed, scopeData VF should be nulled out",
                       scopeData.ivValidatorFactory);
        }
    }

    private void setupExpectations(Mockery mockery,
                                   final OSGiBeanValidationScopeData scopeData,
                                   final ValidationConfigurationInterface configurator,
                                   final int numberOfGetMetaDataCalls,
                                   final String runtimeVersion) throws UnableToAdaptException {
        setupExpectations(mockery, scopeData, configurator, numberOfGetMetaDataCalls,
                          runtimeVersion, container, cc, configFactory, 1, 1);
    }

    private void setupExpectations(Mockery mockery,
                                   final OSGiBeanValidationScopeData scopeData,
                                   final ValidationConfigurationInterface configurator,
                                   final int numberOfGetMetaDataCalls,
                                   final String runtimeVersion,
                                   final Container container,
                                   final ComponentContext cc,
                                   final ValidationConfigurationFactory configFactory,
                                   final int numberIsAppClassLoaderCalls,
                                   final int numberCreateValidationConfigurationCalls) throws UnableToAdaptException {
        mockery.checking(new Expectations() {
            {
                oneOf(slotService).reserveMetaDataSlot(ModuleMetaData.class);
                will(returnValue(mmdSlot));

                exactly(numberOfGetMetaDataCalls).of(mmd).getMetaData(mmdSlot);
                will(returnValue(scopeData));

                if (runtimeVersion != null) {
                    oneOf(runtimeVersionSR).getProperty("version");
                    will(returnValue(runtimeVersion));
                }

                if (container != null) {
                    oneOf(container).adapt(ValidationConfig.class);
                    will(returnValue(dd));
                }

                if (cc != null) {
                    oneOf(cc).locateService("validationConfigFactory", configFactorySR);
                    will(returnValue(configFactory));

                    oneOf(cc).locateService("classLoadingService", classLoadingServiceSR);
                    will(returnValue(classLoadingService));

                    oneOf(classLoadingService).isThreadContextClassLoader(Thread.currentThread().getContextClassLoader());
                    will(returnValue(false));

                    exactly(numberIsAppClassLoaderCalls).of(classLoadingService).isAppClassLoader(Thread.currentThread().getContextClassLoader());
                    will(returnValue(true));

                    oneOf(classLoadingService).createThreadContextClassLoader(Thread.currentThread().getContextClassLoader());
                    will(returnValue(Thread.currentThread().getContextClassLoader()));

                    oneOf(classLoadingService).destroyThreadContextClassLoader(with(any(ClassLoader.class)));
                }

                if (configFactory != null) {
                    exactly(numberCreateValidationConfigurationCalls).of(configFactory).createValidationConfiguration(scopeData, dd);
                    will(returnValue(configurator));
                }

                oneOf(event).getMetaData();
                will(returnValue(mmd));
            }
        });
    }
}

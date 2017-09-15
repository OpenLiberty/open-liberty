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
package com.ibm.ws.beanvalidation.v11.config;

import java.util.List;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import com.ibm.ws.beanvalidation.mock.MockConstraintValidatorFactory;
import com.ibm.ws.beanvalidation.mock.MockParameterNameProvider;
import com.ibm.ws.beanvalidation.service.BeanValidationContext;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.beanvalidation.v11.cdi.internal.ReleasableConstraintValidatorFactory;
import com.ibm.ws.javaee.dd.bval.Property;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.managedobject.ManagedObject;

public class ValidationConfiguratorV11Test {

    private final Mockery mockery = new JUnit4Mockery();

    final ValidationConfig config = mockery.mock(ValidationConfig.class);
    final ValidationReleasableFactory releasableFactory = mockery.mock(ValidationReleasableFactory.class);
    final BeanValidationContext context = mockery.mock(BeanValidationContext.class);

    final Configuration<?> apiConfig = mockery.mock(Configuration.class);
    final ManagedObject<?> releasable = mockery.mock(ManagedObject.class);

    @Test
    public void testSetConstraintValidatorFactoryReleasable() throws Exception {
        mockery.checking(new Expectations() {
            {
                oneOf(context).getClassLoader();
                will(returnValue(BeanValidationContext.class.getClassLoader()));
            }
        });

        ValidationConfiguratorV11 configurator = new ValidationConfiguratorV11(context, null, releasableFactory);

        final ReleasableConstraintValidatorFactory cvf = new ReleasableConstraintValidatorFactory(releasableFactory);

        mockery.checking(new Expectations() {
            {
                oneOf(releasableFactory).createConstraintValidatorFactory();
                will(returnValue(cvf));

                oneOf(apiConfig).constraintValidatorFactory(cvf);
            }
        });

        configurator.setConstraintValidatorFactory(apiConfig);
    }

    @Test
    public void testSetConstraintValidatorFactoryNotReleasable() throws Exception {
        mockery.checking(new Expectations() {
            {
                oneOf(context).getClassLoader();
                will(returnValue(BeanValidationContext.class.getClassLoader()));
            }
        });

        ValidationConfiguratorV11 configurator = new ValidationConfiguratorV11(context, null, null);
        configurator.setConstraintValidatorFactory(apiConfig);
    }

    @Test
    public void testSetConstraintValidatorFactoryCustomAndReleasable() throws Exception {
        setUpConfigExpectations(11, null, null, null,
                                MockConstraintValidatorFactory.class.getName(),
                                null, null, null);

        ValidationConfiguratorV11 configurator = new ValidationConfiguratorV11(context, config, releasableFactory);

        final ConstraintValidatorFactory cvf = new MockConstraintValidatorFactory();

        mockery.checking(new Expectations() {
            {
                oneOf(releasableFactory).createValidationReleasable(cvf.getClass());
                will(returnValue(releasable));

                oneOf(releasable).getObject();
                will(returnValue(cvf));

                oneOf(apiConfig).constraintValidatorFactory(cvf);
            }
        });

        configurator.setConstraintValidatorFactory(apiConfig);
    }

    @Test
    public void testSetConstraintValidatorFactoryCustomNotReleasable() throws Exception {
        setUpConfigExpectations(11, null, null, null,
                                MockConstraintValidatorFactory.class.getName(),
                                null, null, null);

        ValidationConfiguratorV11 configurator = new ValidationConfiguratorV11(context, config, null);

        mockery.checking(new Expectations() {
            {
                oneOf(apiConfig).constraintValidatorFactory(with(any(MockConstraintValidatorFactory.class)));
            }
        });

        configurator.setConstraintValidatorFactory(apiConfig);
    }

    @Test
    public void testSetParameterNameProviderCustomV10() throws Exception {
        setUpConfigExpectations(10, null, null, null, null,
                                MockParameterNameProvider.class.getName(),
                                null, null);

        ValidationConfiguratorV11 configurator = new ValidationConfiguratorV11(context, config, null);
        configurator.setParameterNameProvider(apiConfig);
    }

    @Test
    public void testSetParameterNameProviderCustomV11() throws Exception {
        setUpConfigExpectations(11, null, null, null, null,
                                MockParameterNameProvider.class.getName(),
                                null, null);

        ValidationConfiguratorV11 configurator = new ValidationConfiguratorV11(context, config, null);

        mockery.checking(new Expectations() {
            {
                oneOf(apiConfig).parameterNameProvider(with(any(MockParameterNameProvider.class)));
            }
        });

        configurator.setParameterNameProvider(apiConfig);
    }

    @Test
    public void testSetParameterNameProviderNotCustom() throws Exception {
        setUpConfigExpectations(11, null, null, null, null, null, null, null);

        ValidationConfiguratorV11 configurator = new ValidationConfiguratorV11(context, config, null);
        configurator.setParameterNameProvider(apiConfig);
    }

    private void setUpConfigExpectations(final int version,
                                         final String provider,
                                         final String resolver,
                                         final String interpolator,
                                         final String factory,
                                         final String parameter,
                                         final List<String> mappings,
                                         final List<Property> properties) {
        mockery.checking(new Expectations() {
            {
                oneOf(config).getVersionID();
                will(returnValue(version));

                oneOf(config).getDefaultProvider();
                will(returnValue(provider));

                oneOf(config).getTraversableResolver();
                will(returnValue(resolver));

                oneOf(config).getMessageInterpolator();
                will(returnValue(interpolator));

                oneOf(config).getConstraintValidatorFactory();
                will(returnValue(factory));

                oneOf(config).getParameterNameProvider();
                will(returnValue(parameter));

                oneOf(config).getConstraintMappings();
                will(returnValue(mappings));

                oneOf(config).getProperties();
                will(returnValue(properties));

                oneOf(context).getClassLoader();
                will(returnValue(BeanValidationContext.class.getClassLoader()));
            }
        });
    }

}

/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.beanvalidation;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import javax.validation.ParameterNameProvider;
import javax.validation.ValidatorFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The purpose of this class is to delay obtaining obtaining the
 * javax.validation.ValidatorFactory instance for as long as possible.
 *
 * This is only used for JPA providers that are doing bean validation,
 * and the point is to delay doing real bean validation work until after
 * the classes have been transformed and the MMD has been placed on the thread.
 */
public class JPAParameterNameProvider implements ParameterNameProvider
{
    private static final TraceComponent tc = Tr.register(JPAParameterNameProvider.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME );

    /**
     * The real ParameterNameProvider instance.
     */
    private ParameterNameProvider ivParameterNameProvider;

    private final ValidatorFactoryLocator ivValidatorFactoryLocator;

    JPAParameterNameProvider(ValidatorFactoryLocator locator) {
       ivValidatorFactoryLocator = locator;
    }

    @Override
    public List<String> getParameterNames(Constructor<?> constructor) {
        if (ivParameterNameProvider == null)
            obtainParameterNameProvider();

        return ivParameterNameProvider.getParameterNames(constructor);
    }

    @Override
    public List<String> getParameterNames(Method method) {
        if (ivParameterNameProvider == null)
            obtainParameterNameProvider();

        return ivParameterNameProvider.getParameterNames(method);
    }

    private void obtainParameterNameProvider() {
        ValidatorFactory validatorFactory = ivValidatorFactoryLocator.getValidatorFactory();
        ivParameterNameProvider = validatorFactory.getParameterNameProvider();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Obtained the ParameterNameProvider: " + ivParameterNameProvider);
    }
}

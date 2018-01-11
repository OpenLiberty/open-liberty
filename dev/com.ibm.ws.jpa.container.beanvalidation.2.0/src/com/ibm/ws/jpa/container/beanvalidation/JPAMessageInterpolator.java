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

import java.util.Locale;

import javax.validation.MessageInterpolator;
import javax.validation.ValidatorFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The purpose of this class is to delay obtaining obtaining the
 * javax.validation.ValidatorFactory instance for as long as possible.
 * 
 * This is only used for JPA providers that are doing bean validation,
 * and the point is to delay doing real bean validation work until after
 * the classes have been 'transformed' and the MMD has been placed on the thread.
 */
public class JPAMessageInterpolator implements MessageInterpolator
{
    private static final TraceComponent tc = Tr.register(JPAMessageInterpolator.class,
                                                         JPA_TRACE_GROUP,
                                                         JPA_RESOURCE_BUNDLE_NAME);

    /**
     * The real MessageInterpolator instance.
     * 
     * A real javax.validation.ValidatorFactory instance has a one-to-one association
     * with a module. A real javax.validation.ValidatorFactory always hands back the
     * same javax.validation.MessageInterpolator instance. Thus, the
     * javax.validation.MessageInterpolator instance also has a one-to-one
     * association with a module.
     * 
     * Our wrappers mimic this relationship structure. The JPAValidatorFactory wrapper
     * stands-in for the javax.validation.ValidatorFactory instance, and thus has a
     * one-to-one relationship with a module. The JPAValidatorFactory wrapper always
     * hands back the same instance of this JPAMessageInterpolator, which
     * in turn always uses the same MessageInterpolator instance.
     * Thus, the javax.validation.MessageInterpolator instance has a one-to-one
     * association with a module in the wrapper scenario as well.
     */
    private MessageInterpolator ivMessageInterpolator = null;

    private final ValidatorFactoryLocator ivValidatorFactoryLocator;

    JPAMessageInterpolator(ValidatorFactoryLocator locator)
    {
        ivValidatorFactoryLocator = locator;
    }

    private void obtainMessageInterpolator()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        ValidatorFactory validatorFactory = ivValidatorFactoryLocator.getValidatorFactory();
        ivMessageInterpolator = validatorFactory.getMessageInterpolator();

        if (isTraceOn && tc.isDebugEnabled())
        {
            Tr.debug(tc, "Obtained the MessageInterpolator: " + ivMessageInterpolator);
        }

    }

    @Override
    public String interpolate(String messageTemplate, MessageInterpolator.Context context)
    {
        if (ivMessageInterpolator == null)
        {
            obtainMessageInterpolator();
        }

        return ivMessageInterpolator.interpolate(messageTemplate, context);
    }

    @Override
    public String interpolate(String messageTemplate, MessageInterpolator.Context context, Locale locale)
    {
        if (ivMessageInterpolator == null)
        {
            obtainMessageInterpolator();
        }

        return ivMessageInterpolator.interpolate(messageTemplate, context, locale);
    }
}

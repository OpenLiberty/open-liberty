/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import javax.validation.ValidatorFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ejs.util.Util;
import com.ibm.ws.beanvalidation.service.BeanValidation;

public class ValidatorFactoryObjectFactory implements ObjectFactory
{
    private static final TraceComponent tc = Tr.register(ValidatorFactoryObjectFactory.class,
                                                         "BeanValidation",
                                                         BVNLSConstants.BV_RESOURCE_BUNDLE);

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + Util.identity(obj));

        ValidatorFactory validatorFactory = AbstractBeanValidation.getValidatorFactory();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance: " + Util.identity(validatorFactory));

        return validatorFactory;
    }

    /**
     * This method will return null if no BeanValidationService is available in the process
     * 
     * @deprecated Use {@link BeanValidation#getValidatorFactory} instead
     */
    @Deprecated
    public static ValidatorFactory getValidatorFactory()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getValidatorFactory");

        ValidatorFactory validatorFactory = AbstractBeanValidation.getValidatorFactory();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getValidatorFactory: " + Util.identity(validatorFactory));

        return validatorFactory;
    }
}

/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.beanvalidation.component;

import java.util.List;

import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationFeature;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.cxf.validation.BeanValidationProvider;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jaxrs20.api.JaxRsBeanValidationService;

@Component(name = "com.ibm.ws.jaxrs20.JaxRsBeanValidationServiceImpl", property = { "service.vendor=IBM" })
public class JaxRsBeanValidationServiceImpl implements JaxRsBeanValidationService {

    @Override
    public boolean enableBeanValidationProviders(List<Object> providers) {

        providers.add(new JAXRSBeanValidationFeature());
        providers.add(new ValidationExceptionMapper());

        return true;

    }

    @Override
    public Class<?> getBeanValidationProviderClass() {// throws Exception {
        BeanValidationProvider provider = new BeanValidationProvider();
        Class<?> clazz = provider.getClass();

        return clazz;
    }

}

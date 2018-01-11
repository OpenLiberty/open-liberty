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

import java.util.Map;

import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.jpa.management.JPAEMFPropertyProvider;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/*
 * This class is factored out of AbstractJPAComponent and exists
 * in order to pass a ValidatorFactory to the JPA provider.
 */
@Component(service={JPAEMFPropertyProvider.class})
public class JPABV20ComponentImpl implements ValidatorFactoryLocator, JPAEMFPropertyProvider{
    
    private BeanValidation bvalService;

    @Override
    public ValidatorFactory getValidatorFactory() // d727932
    {
        if (bvalService == null) {
            throw new ValidationException("bean validation provider is not available");
        }
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return bvalService.getValidatorFactoryOrDefault(cmd);
    }

    
    @Reference(cardinality=ReferenceCardinality.MANDATORY)
    protected void setBeanValidationService(BeanValidation bv) {
        bvalService = bv;
    }
    
    protected void unsetBeanValidationService(BeanValidation bv) {
        bvalService = null;
    }


    @Override
    public void updateProperties(Map<String, Object> props) {
        props.put("javax.persistence.validation.factory", new JPAValidatorFactory(this));
    }
}

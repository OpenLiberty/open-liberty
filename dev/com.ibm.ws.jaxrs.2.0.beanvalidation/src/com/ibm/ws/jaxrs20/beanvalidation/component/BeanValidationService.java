/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.beanvalidation.component;

import java.lang.reflect.Method;

import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * DS to provide bean validation service to the JAX-RS BeanValidationProvider.
 */
@Component(name = "com.ibm.ws.jaxrs20.BeanValidationService", property = { "service.vendor=IBM" })
public class BeanValidationService {

    private static final TraceComponent tc = Tr.register(BeanValidationService.class);

    private static final String REFERENCE_BEANVALIDATION_SERVICE = "jaxrsbeanValidation";

    protected final AtomicServiceReference<BeanValidation> beanValidation = new AtomicServiceReference<BeanValidation>(REFERENCE_BEANVALIDATION_SERVICE);

    private static volatile BeanValidationService svInstance = null;

    public static BeanValidationService instance()
    {
        return svInstance;
    }

    static void setInstance(BeanValidationService instance)
    {
        if (svInstance != null && instance != null) {
            throw new IllegalStateException("instance already set");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setInstance : " + Util.identity(instance));
        svInstance = instance;
    }

    /**
     * Called by DS to activate this service
     *
     * @param compcontext the context of this component
     */
    protected void activate(ComponentContext compcontext) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating " + this.getClass().getName());
        }
        setInstance(this);
        this.beanValidation.activate(compcontext);
    }

    /**
     * Called by DS to deactivate this service
     *
     * @param compcontext the context of this component
     */
    protected void deactivate(ComponentContext compcontext) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating " + this.getClass().getName());
        }
        setInstance(null);
        this.beanValidation.deactivate(compcontext);
    }

    /**
     * Called by DS to set the service reference
     *
     * @param ref the reference from DS
     */
    @Reference(name = REFERENCE_BEANVALIDATION_SERVICE, service = BeanValidation.class)
    protected void setBeanValidation(ServiceReference<BeanValidation> ref) {
        this.beanValidation.setReference(ref);
    }

    /**
     * Called by DS to remove the service reference
     *
     * @param ref the reference from DS
     */
    protected void unsetBeanValidation(ServiceReference<BeanValidation> ref) {
        this.beanValidation.unsetReference(ref);
    }

    public Validator getDefaultValidator() {
        BeanValidation beanValidation = instance().beanValidation.getService();
        if (beanValidation == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null Validator because the BeanValidation service is not currently unavailable. Is the bean validation feature enabled?");
            }
            return null;
        }
        try {
            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return beanValidation.getValidator(componentMetaData);
        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null Validator: " + e.getMessage());
            }
            return null;
        }
    }

    public ValidatorFactory getDefaultValidatorFactory() {
        BeanValidation beanValidation = instance().beanValidation.getService();
        if (beanValidation == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null ValidatorFactory because the BeanValidation service is not currently available.  Is the bean validation feature enabled?");
            }
            return null;
        }
        try {
            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return beanValidation.getValidatorFactoryOrDefault(componentMetaData);
        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null ValidatorFactory: " + e.getMessage());
            }
            return null;
        }
    }

    public boolean isMethodConstrained(Method method) {
        BeanValidation beanValidation = instance().beanValidation.getService();
        if (beanValidation == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning false for isMethodConstrained() because the BeanValidation service is not currently available.  Is the bean validation feature enabled?");
            }
            return false;
        }
        try {
            ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return beanValidation.isMethodConstrained(method);
        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning false for isMethodConstrained(): " + e.getMessage());
            }
            return false;
        }
    }
}

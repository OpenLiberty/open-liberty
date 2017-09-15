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
package com.ibm.ws.jsf.beanvalidation.extprocessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.osgi.osgi.ExtensionFactoryServiceListener;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

//This service class will allow the WebApp WebContainer class to set the needed bean validation setting for JSF
//The only time this will be added is when jsf-2.2 and beanValidation-1.1 is enabled in the server.xml
public class JSFBeanValidationExtensionFactory implements ExtensionFactory {
	// Log instance for this class
    protected static final Logger log = Logger.getLogger("com.ibm.ws.jsf.beanvalidation.extprocessor");
    protected static final String CLASS_NAME = "com.ibm.ws.jsf.beanvalidation.extprocessor.JSFBeanValidationExtensionFactory";
	
    private final AtomicServiceReference<BeanValidation> beanValidationSRRef = new AtomicServiceReference<BeanValidation>("beanValidation");
	
    @SuppressWarnings("unchecked")
	public void activate(ComponentContext compcontext, Map<String, Object> properties) {
        this.beanValidationSRRef.activate(compcontext);
    }

    public void deactivate(ComponentContext compcontext) {
        this.beanValidationSRRef.deactivate(compcontext);
    }
	
	// declarative service
	public void setBeanValidation(ServiceReference<BeanValidation> ref) {
		beanValidationSRRef.setReference(ref);
	}

	// declarative service
	public void unsetBeanValidation(ServiceReference<BeanValidation> ref) {
		beanValidationSRRef.unsetReference(ref);
	}

	public BeanValidation getBeanValidation() {
		return beanValidationSRRef.getService();
	}
	   
	private void setValidatorFactoryAttribute(IServletContext ctxt) {

       BeanValidation bv = getBeanValidation();
       ComponentMetaData cmd = null;
       ValidatorFactory vf = null;
       if (bv != null) {
           cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
           if (cmd != null) {
               try {
                   vf = bv.getValidatorFactory(cmd);
                   ctxt.setAttribute(javax.faces.validator.BeanValidator.VALIDATOR_FACTORY_KEY, vf);
                   if (log.isLoggable(Level.FINE)) {
                       log.logp(Level.FINE, CLASS_NAME, "setValidatorFactoryAttribute", "VALIDATOR_FACTORY_KEY set to: " + vf);
                   }
               } catch (ValidationException bve) {
                   if (log.isLoggable(Level.FINE)) {
                       log.logp(Level.FINE, CLASS_NAME, "setValidatorFactoryAttribute", "exception thrown while attempting to set the validator factory", bve);
                   }
               }
           }
       } else {
           if (log.isLoggable(Level.FINE)) {
               log.logp(Level.FINE, CLASS_NAME, "setValidatorFactoryAttribute", "bean validation service was null");
           }
       }
   }
	
	@Override
	public ExtensionProcessor createExtensionProcessor(IServletContext webapp)
			throws Exception {
		
		if (log.isLoggable(Level.FINE)) {
            log.logp(Level.FINE, CLASS_NAME, "createExtensionProcessor", "Setting up the extension processor for bean validation");
        }
        setValidatorFactoryAttribute(webapp);
		
        //We want to return null here because we don't want to add an ExtenstionProcessor to WebApp
        //We just want the VALIDATOR_FACTORY_KEY set so JSF can access it later
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List getPatternList() {
		return Collections.EMPTY_LIST;
	}
}

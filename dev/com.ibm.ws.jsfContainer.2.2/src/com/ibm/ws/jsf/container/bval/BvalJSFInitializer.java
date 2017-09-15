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
package com.ibm.ws.jsf.container.bval;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.FacesContext;
import javax.faces.validator.BeanValidator;
import javax.validation.ValidatorFactory;

import com.ibm.ws.beanvalidation.accessor.BeanValidationAccessor;
import com.ibm.ws.jsf.container.cdi.CDIJSFInitializer;

public class BvalJSFInitializer {

    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf.container.bval");

    public static void initialize() {
        if (log.isLoggable(Level.FINEST))
            log.logp(Level.FINEST, CDIJSFInitializer.class.getName(), "initializeJSF",
                     "Initializing application with BeanVal");

        Map<String, Object> ctx = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
        ValidatorFactory vf = BeanValidationAccessor.getValidatorFactory();

        if (vf != null) {
            if (log.isLoggable(Level.FINEST))
                log.logp(Level.FINEST, CDIJSFInitializer.class.getName(), "initializeJSF",
                         "Setting validator factory in JSF context: ", vf);
            ctx.put(BeanValidator.VALIDATOR_FACTORY_KEY, vf);
        } else {
            if (log.isLoggable(Level.FINEST))
                log.logp(Level.FINEST, CDIJSFInitializer.class.getName(), "initializeJSF",
                         "Validator factory from BeanValidationAccessor was null");
        }
    }

}

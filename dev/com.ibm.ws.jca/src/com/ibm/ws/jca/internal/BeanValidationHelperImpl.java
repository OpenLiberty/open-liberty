/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.BeanValidationUsingClassLoader;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Bean Validation Helper to avoid loading javax.validation.* dependency if beanValidation-1.0 feature is not enabled.
 */
public class BeanValidationHelperImpl implements BeanValidationHelper {
    private static final TraceComponent tc = Tr.register(BeanValidationHelperImpl.class);

    /**
     * The BeanValidation service for this resource adapter.
     */
    private BeanValidationUsingClassLoader beanValidationSvc;

    @Override
    public void setBeanValidationSvc(Object svc) {
        this.beanValidationSvc = (BeanValidationUsingClassLoader) svc;
    }

    @Override
    public void validateInstance(ModuleMetaData mmd, ClassLoader loader, Object instance) {
        // perform BeanValidation function
        Validator validator = beanValidationSvc.getValidator(mmd, loader);
        Set<ConstraintViolation<Object>> cvSet = null;
        try {
            cvSet = validator.validate(instance);
        } catch (ValidationException ve) {
            // Method validate() will throw a ValidationException when the validator fails
            // unexpectedly, not when the bean configuration violates constraints (i.e. fails validation.)
            Object[] msgArgs = new Object[] {
                                             Util.identity(validator),
                                             ve,
                                             Util.identity(instance)
            };
            Tr.error(tc, "BEAN_VALIDATION_VALIDATOR_FAILED_J2CA1008", msgArgs);
        }
        if (cvSet != null && !cvSet.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            for (ConstraintViolation<?> constraintViolation : cvSet) {
                msg.append("\n\t" + constraintViolation);
            }
            ConstraintViolationException cve = new ConstraintViolationException(msg.toString(), (Set) cvSet);
            Object[] msgArgs = new Object[] {
                                             Util.identity(instance),
                                             cve.getMessage()
            };
            Tr.error(tc, "BEAN_VALIDATION_FAILED_J2CA0238", msgArgs);
            throw cve;
        }
    }

}

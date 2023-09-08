/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.persistence.validation;

import java.lang.reflect.Method;
import java.util.Set;

import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.data.internal.persistence.EntityValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.executable.ExecutableValidator;

/**
 * Abstraction for a Jakarta Validation Validator.
 */
public class EntityValidatorImpl implements EntityValidator {
    private final BeanValidation validation;

    /**
     * Construct a new instance.
     *
     * @param validation instance of com.ibm.ws.beanvalidation.service.BeanValidation.
     */
    public EntityValidatorImpl(Object validation) {
        this.validation = (BeanValidation) validation;
    }

    @Override
    public final Object getValidation() {
        return validation;
    }

    /**
     * Validates method parameters where the method or its class is annotated with ValidateOnExecution.
     *
     * @param object instance that has the method with parameters to validate.
     * @param method the method.
     * @param args   the method parameters.
     * @throws ConstraintValidationException if any of the constraints are violated.
     */
    @Override
    public <T> void validateParameters(T object, Method method, Object[] args) {
        ComponentMetaData cdata = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cdata != null) {
            ExecutableValidator validator = validation.getValidator(cdata).forExecutables();
            Set<ConstraintViolation<Object>> violations = validator.validateParameters(object, method, args);
            if (violations != null && !violations.isEmpty())
                throw new ConstraintViolationException(violations); // TODO better message? Ensure that message includes at least the first violation.
        }
    }

    /**
     * Validates the return value where the method or its class is annotated with ValidateOnExecution.
     *
     * @param object      instance that has the method with the return value to validate.
     * @param method      the method.
     * @param returnValue the return value of the method.
     * @throws ConstraintValidationException if any of the constraints are violated.
     */
    @Override
    public <T> void validateReturnValue(T object, Method method, Object returnValue) {
        ComponentMetaData cdata = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cdata != null) {
            ExecutableValidator validator = validation.getValidator(cdata).forExecutables();
            Set<ConstraintViolation<Object>> violations = validator.validateReturnValue(object, method, returnValue);
            if (violations != null && !violations.isEmpty())
                throw new ConstraintViolationException(violations); // TODO better message? Ensure that message includes at least the first violation.
        }
    }
}
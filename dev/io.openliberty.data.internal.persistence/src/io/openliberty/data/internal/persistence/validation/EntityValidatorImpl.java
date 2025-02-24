/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.data.internal.persistence.EntityValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
import jakarta.validation.metadata.MethodDescriptor;

/**
 * Abstraction for a Jakarta Validation Validator.
 */
public class EntityValidatorImpl implements EntityValidator {
    private static final TraceComponent tc = Tr.register(EntityValidatorImpl.class);

    private final BeanDescriptor classDesc;
    private final ExecutableValidator methodValidator;

    /**
     * Construct a new instance.
     *
     * @param validationService   instance of com.ibm.ws.beanvalidation.service.BeanValidation.
     * @param repositoryInterface interface that is annotated with Repository.
     */
    public EntityValidatorImpl(Object validationService, Class<?> repositoryInterface) {
        BeanValidation validation = (BeanValidation) validationService;
        ComponentMetaData cdata = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cdata == null) // internal error
            throw new IllegalStateException("No component metadata found on thread");
        Validator validator = validation.getValidator(cdata);
        classDesc = validator.getConstraintsForClass(repositoryInterface);
        methodValidator = validator.forExecutables();
    }

    /**
     * Determines whether validation is needed for the method return value and parameters.
     *
     * @param method a repository method.
     * @return pair of booleans where the first is whether to validate the parameters
     *         and the second is whether to validate the result.
     */
    @Override
    @Trivial
    public boolean[] isValidatable(Method method) {
        MethodDescriptor methodDesc = classDesc.getConstraintsForMethod(method.getName(), method.getParameterTypes());
        boolean validateParams = methodDesc != null && method.getParameterCount() > 0 && methodDesc.hasConstrainedParameters();
        boolean validateResult = methodDesc != null && methodDesc.hasConstrainedReturnValue();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isValidatable: " + method.getName(),
                     "validate params? " + validateParams,
                     "validate result? " + validateResult);
        return new boolean[] { validateParams, validateResult };
    }

    /**
     * Obtains the messages for a set of constraint violations.
     *
     * @param violations constraint violations.
     * @return constraint violation messages.
     */
    @Trivial
    private static List<String> toMessages(Set<ConstraintViolation<Object>> violations) {
        List<String> violationMessages = new ArrayList<>(violations.size());
        for (ConstraintViolation<Object> v : violations)
            violationMessages.add(v.getPropertyPath() + ": " + v.getMessage());
        return violationMessages;
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
    @Trivial // avoid logging customer data
    public <T> void validateParameters(T object, Method method, Object[] args) {
        Set<ConstraintViolation<Object>> violations = //
                        methodValidator.validateParameters(object, method, args);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "validateParameters violations: " +
                               toMessages(violations));
        if (violations != null && !violations.isEmpty())
            throw new ConstraintViolationException(violations);
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
    @Trivial // avoid logging customer data
    public <T> void validateReturnValue(T object, Method method, Object returnValue) {
        Set<ConstraintViolation<Object>> violations = //
                        methodValidator.validateReturnValue(object, method, returnValue);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "validateReturnValue violations: " +
                               toMessages(violations));
        if (violations != null && !violations.isEmpty())
            throw new ConstraintViolationException(violations);
    }
}
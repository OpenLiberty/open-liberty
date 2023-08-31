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
package io.openliberty.data.internal.persistence;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Abstraction for a Jakarta Validation Validator.
 */
public interface EntityValidator {
    /**
     * @return the com.ibm.ws.beanvalidation.service.BeanValidation service that this EntityValidator uses.
     */
    Object getValidation();

    /**
     * Construct a provider for the EntityValidator abstraction.
     *
     * @param validation com.ibm.ws.beanvalidation.service.BeanValidation service for this provider to use.
     * @return the provider.
     */
    static EntityValidator newInstance(Object validation) {
        try {
            @SuppressWarnings("unchecked")
            Class<EntityValidator> EntityValidatorImpl = (Class<EntityValidator>) EntityValidator.class.getClassLoader() //
                            .loadClass("io.openliberty.data.internal.persistence.validation.EntityValidatorImpl");
            return EntityValidatorImpl.getConstructor(Object.class).newInstance(validation);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException x) {
            throw new RuntimeException(x); // internal error
        } catch (InvocationTargetException x) {
            throw new RuntimeException(x.getCause()); // internal error
        }
    }

    /**
     * Validates each entity instance per its specified constraints (if any).
     *
     * @param entities instances to validate.
     * @throws ConstraintValidationException if any of the constraints are violated for an entity.
     */
    void validate(Iterable<?> entities);

    /**
     * Validates the entity instance per its specified constraints (if any).
     *
     * @param entity instance to validate.
     * @throws ConstraintValidationException if any of the constraints are violated.
     */
    void validate(Object entity);

    /**
     * Validates each entity instance per its specified constraints (if any).
     *
     * @param entities instances to validate.
     * @param length   number of instances in the array to validate.
     * @throws ConstraintValidationException if any of the constraints are violated for an entity.
     */
    void validate(Object entityArray, int length);

    /**
     * Validates method parameters where the method or its class is annotated with ValidateOnExecution.
     *
     * @param object instance that has the method with parameters to validate.
     * @param method the method.
     * @param args   the method parameters.
     * @throws ConstraintValidationException if any of the constraints are violated.
     */
    public <T> void validateParameters(T object, Method method, Object[] args);

    /**
     * Validates the return value where the method or its class is annotated with ValidateOnExecution.
     *
     * @param object      instance that has the method with the return value to validate.
     * @param method      the method.
     * @param returnValue the return value of the method.
     * @throws ConstraintValidationException if any of the constraints are violated.
     */
    public <T> void validateReturnValue(T object, Method method, Object returnValue);
}
/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.service;

import javax.validation.ConstraintValidatorFactory;

import com.ibm.ws.managedobject.ManagedObject;

/**
 * Interface to specify the contract for creating validation releasable objects.
 */
public interface ValidationReleasableFactory {

    /**
     * Create a validation releasable object out of the class type passed in.
     * 
     * @param clazz the type of validation releasable to create
     * @return the releasable object
     */
    public <T> ManagedObject<T> createValidationReleasable(Class<T> clazz);

    /**
     * Create a ConstraintValidatorFactory as a ValidationReleasable.
     * 
     * @return the releasable ConstraintValidatorFactory
     */
    public ValidationReleasable<ConstraintValidatorFactory> createConstraintValidatorFactory();
}

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

/**
 * Interface to allow any implementer to specify how to release the particular
 * kind of validation releasable.
 *
 * @param <T> the validation object that is stored to be released
 */
public interface ValidationReleasable<T> {

    /**
     * Release any resources that creating this {@link ValidationReleasable} required.
     */
    public void release();

    /**
     * Get the instance represented by this {@link ValidationReleasable}
     *
     * @return the instance
     */
    public T getInstance();

    //public ValidatorFactory injectValidatorFactoryResources(ValidatorFactory vf);
}

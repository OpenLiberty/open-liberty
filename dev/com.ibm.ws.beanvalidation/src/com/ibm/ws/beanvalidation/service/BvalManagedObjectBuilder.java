/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.service;

import javax.validation.Configuration;
import javax.validation.ValidatorFactory;

/**
 * Interface for creating a ValidatorFactory using CDI managed objects.
 */
public interface BvalManagedObjectBuilder {

    /**
     * Inject a ValidatorFactory with CDI managed objects for its configuration.
     *
     * @return CDI enabled ValidatorFactory
     */
    public ValidatorFactory injectValidatorFactoryResources(Configuration<?> config, ClassLoader appClassLoader);
}

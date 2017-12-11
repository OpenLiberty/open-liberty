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
package com.ibm.ws.jpa.container.beanvalidation;

import javax.validation.ValidatorFactory;

/**
 * The ValidatorFactoryLocator makes use of the BeanValidation service to
 * obtain a ValidatorFactory instance. <p>
 */
interface ValidatorFactoryLocator
{
    /**
     * Returns the container managed ValidatorFactory that has been configured
     * for the current Java EE application module. <p>
     */
    public ValidatorFactory getValidatorFactory();
}

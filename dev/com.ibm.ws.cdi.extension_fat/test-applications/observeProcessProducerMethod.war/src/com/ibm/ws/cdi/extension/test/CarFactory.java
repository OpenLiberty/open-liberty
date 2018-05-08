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
package com.ibm.ws.cdi.extension.test;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

@Stateless
public class CarFactory implements FactoryLocal {

    @Resource
    BeanManager beanManager;

    @Inject
    Bus bus;

    /** {@inheritDoc} */
    @Override
    @Produces
    public Car produceCar() {
        return new Car();
    }

    /** {@inheritDoc} */
    @Override
    public BeanManager getBeanManager() {
        assertNotNull(bus);
        return beanManager;
    }

}

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

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/")
public class ObserverTestServlet extends FATServlet {
    private static final long serialVersionUID = 5122339389348638017L;

    @Inject
    Car car;

    @EJB
    FactoryLocal factory;

    @Inject
    PlainExtension extension;

    @Test
    public void testCar() {
        assertNotNull(car);
    }

    @Test
    public void testBeanManager() {
        assertNotNull(factory.getBeanManager());
    }

    @Test
    public void testExtension() {
        assertNotNull(extension);
        assertNotNull(extension.getProducerMethod());
    }
}

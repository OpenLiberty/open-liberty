/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.apps.basic;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructLogger;

import componenttest.app.FATServlet;

/**
 * These tests use {@link AroundConstructLogger} to record what happens while intercepting constructors.
 * <p>{@link AroundConstructLogger} is <code>@RequestScoped</code> so a new instance is created for every test.
 */
@WebServlet("/ejbTestServlet")
public class EjbServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    Ejb ejb;

    /**
     * Test that CDI interceptors work on stateless beans
     */
    @Test
    public void testEjbFoundWithTooManyJars() {
        assertEquals("EJB should exist",
                     ejb.getValue(), 1);
    }

}

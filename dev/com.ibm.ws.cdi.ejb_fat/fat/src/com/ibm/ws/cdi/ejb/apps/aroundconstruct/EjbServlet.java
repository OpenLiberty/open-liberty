/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.aroundconstruct;

import static com.ibm.ws.cdi.ejb.utils.Utils.id;
import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

/**
 * These tests use {@link AroundConstructLogger} to record what happens while intercepting constructors.
 * <p>{@link AroundConstructLogger} is <code>@RequestScoped</code> so a new instance is created for every test.
 */
@WebServlet("/ejbTestServlet")
public class EjbServlet extends AroundConstructTestServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    StatelessAroundConstructLogger logger;

    @Inject
    Ejb ejb;

    @Inject
    StatelessEjb stateless;

    @Override
    protected void before() {
        ejb.doSomething(); // need to actually use the injected bean, otherwise things go a bit funny
        stateless.doSomething();
    }

    /**
     * Test that CDI interceptors work on stateless beans
     */
    @Test
    public void testStatelessAroundConstruct() {
        assertEquals("Stateless bean should be intercepted.",
                     logger.getInterceptedBean(),
                     id(StatelessEjb.class));
    }

}

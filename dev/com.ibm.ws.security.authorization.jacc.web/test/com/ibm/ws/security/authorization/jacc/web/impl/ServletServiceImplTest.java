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

package com.ibm.ws.security.authorization.jacc.web.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authorization.jacc.web.WebSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityValidator;

public class ServletServiceImplTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final ComponentContext cc = context.mock(ComponentContext.class);

    /**
     * Tests activate/deactivate method
     * Expected result: true
     */
    @Test
    public void activateDeactivateNormal() {
        try {
            ServletServiceImpl ws = new ServletServiceImpl();
            ws.activate(cc);
            ws.deactivate(cc);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught : " + e);
        }
    }

    /**
     * Tests getPropagator method
     * Expected result: true
     */
    @Test
    public void getPropagatorNormal() {
        try {
            ServletServiceImpl ws = new ServletServiceImpl();
            WebSecurityPropagator wsp = ws.getPropagator();
            // make sure that it returns the singleton.
            assertEquals(wsp, ws.getPropagator());
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught : " + e);
        }
    }

    /**
     * Tests getValidator method
     * Expected result: true
     */
    @Test
    public void getValidatorNormal() {
        try {
            ServletServiceImpl ws = new ServletServiceImpl();
            WebSecurityValidator wsv = ws.getValidator();
            // make sure that it returns the singleton.
            assertEquals(wsv, ws.getValidator());
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught : " + e);
        }
    }
}

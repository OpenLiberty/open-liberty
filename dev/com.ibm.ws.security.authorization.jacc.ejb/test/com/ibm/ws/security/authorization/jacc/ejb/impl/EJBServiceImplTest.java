/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.ejb.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityValidator;

import test.common.SharedOutputManager;

public class EJBServiceImplTest {
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
            EJBServiceImpl es = new EJBServiceImpl();
            es.activate(cc);
            es.deactivate(cc);
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
            EJBServiceImpl es = new EJBServiceImpl();
            EJBSecurityPropagator esp = es.getPropagator();
            // make sure that it returns the singleton.
            assertEquals(esp, es.getPropagator());
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
            EJBServiceImpl es = new EJBServiceImpl();
            EJBSecurityValidator esv = es.getValidator();
            // make sure that it returns the singleton.
            assertEquals(esv, es.getValidator());
        } catch (Exception e) {
            e.printStackTrace();
            fail("An exception is caught : " + e);
        }
    }
}

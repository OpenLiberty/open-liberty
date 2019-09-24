/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.testtooling.vehicle.web;

import javax.naming.InitialContext;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.ejb.EJBTestVehicle;

public class EJBTestVehicleServlet extends JPATestServlet {
    private static final long serialVersionUID = 7626680108917278937L;

    protected void executeTestVehicle(TestExecutionContext ctx, String ejbJndiName) {
        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/" + ejbJndiName;

            System.out.println("Looking up \"" + jndiName + "\" ...");
            EJBTestVehicle ejb = (EJBTestVehicle) ic.lookup(jndiName);
            try {
                ejb.executeTestLogic(ctx);
            } finally {
                try {
                    ejb.release();
                } catch (Throwable t) {
                }
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            Assert.fail("EJBTestVehicleServlet Caught Exception: " + t);
        }
    }
}

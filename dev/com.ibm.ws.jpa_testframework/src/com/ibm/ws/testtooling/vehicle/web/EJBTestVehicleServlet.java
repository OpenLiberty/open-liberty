/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.testtooling.vehicle.web;

import javax.naming.InitialContext;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.ejb.EJBTestVehicle;

public class EJBTestVehicleServlet extends JPATestServlet {
    private static final long serialVersionUID = 7626680108917278937L;
    protected String ejbJNDIName;

    @Override
    protected void executeTestVehicle(TestExecutionContext ctx) {
        executeTestVehicle(ctx, ejbJNDIName);
    }

    protected void executeTestVehicle(TestExecutionContext ctx, String ejbJNDIName) {
        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/" + ejbJNDIName;

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
        } catch (Throwable t) {
            logException(t, ctx);
        }
    }
}

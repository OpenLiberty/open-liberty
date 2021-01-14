/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.injectparameters;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/TestEjb")
public class TestEjbServlet extends FATServlet {

    @EJB
    TestEjb ejb;

    @Test
    public void testEjbParameterInjection() throws Exception {
        ejb.getResult().validate();
    }

}

/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi.ejb.apps.constructorInjection;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Servlet
 */
@WebServlet("/Servlet")
public class Servlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    @EJB
    BeanEJB ejb;

    @Test
    public void testTransientReferenceOnEjbConstructor() {
        ejb.test();
        String output = StaticState.getOutput();

        assertThat(output, containsString("destroy called"));
        assertThat(output, containsString("First bean message: foo"));
        assertThat(output, containsString("Second bean message: bar"));
        assertThat(output, containsString("Third bean message: spam"));
        assertThat(output, containsString("Forth bean message: eggs"));
    }
}

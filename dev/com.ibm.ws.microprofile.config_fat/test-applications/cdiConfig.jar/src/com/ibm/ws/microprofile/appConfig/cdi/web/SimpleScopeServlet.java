/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.web;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.cdi.beans.TestBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/scope")
public class SimpleScopeServlet extends FATServlet {

    @Inject
    TestBean testBean;

    private boolean initial = true;
    private int expected = 0;

    @Test
    public void testScope() {
        for (int i = 0; i < 5; i++) {
            int actual = Integer.parseInt(System.getProperty("SYS_PROP_ONE"));
            if (initial) {
                expected = actual;
                initial = false;
            }
            int sysPropOne = testBean.getSysPropOne();
            System.out.println("SYS_PROP_ONE: " + sysPropOne); //we expect sysPropOne from the bean to remain the same no matter what the actual system property is
            System.out.println("Expected: " + expected);
            System.out.println("Actual: " + actual);
            assertEquals("ConfigProperty did not remain the same", expected, sysPropOne);

            System.setProperty("SYS_PROP_ONE", "" + (++actual));
        }
    }
}

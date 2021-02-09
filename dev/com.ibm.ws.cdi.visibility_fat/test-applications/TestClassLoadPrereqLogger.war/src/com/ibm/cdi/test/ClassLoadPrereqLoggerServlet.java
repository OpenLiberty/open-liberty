/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.cdi.test;

import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/testservlet")
public class ClassLoadPrereqLoggerServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Test
    @Mode(TestMode.FULL)
    public void testLoggingAPINotVisible() {
        try {
            Class.forName("org.jboss.logging.Slf4jLogger"); //should not be visible to app
            fail("org.jboss.logging.Slf4jLogger was visible");
        } catch (ClassNotFoundException e) {
            //expected
        }

    }

}

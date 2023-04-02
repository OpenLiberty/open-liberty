/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
package com.ibm.ws.cdi.visibility.tests.basic.classloadPrereqWar;

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

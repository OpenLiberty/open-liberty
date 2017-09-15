/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package app1.web;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestServletA")
public class TestServletA extends FATServlet {

    @Test
    public void testServer1() throws Exception {
        System.out.println("Test is running.");
    }

    @Test
    @Mode(TestMode.LITE)
    public void liteTest() throws Exception {
        System.out.println("LITE test is running.");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testFull() throws Exception {
        System.out.println("This test should only run in Full or higher mode!");
    }

    @Test
    @Mode(TestMode.QUARANTINE)
    public void testQuarantine() throws Exception {
        System.out.println("This test should only run in Quarantine mode!");
    }
}

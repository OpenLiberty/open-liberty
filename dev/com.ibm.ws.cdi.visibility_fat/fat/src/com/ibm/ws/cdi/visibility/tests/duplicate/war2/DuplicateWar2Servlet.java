/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.visibility.tests.duplicate.war2;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.visibility.tests.duplicate.lib.DuplicateLibraryBean;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet("/war2")
public class DuplicateWar2Servlet extends FATServlet {

    @Inject
    private DuplicateLibraryBean bean;

    @Test
    @Mode(TestMode.FULL)
    public void testBeanWar2() {
        assertEquals("OK", bean.test());
    }

}

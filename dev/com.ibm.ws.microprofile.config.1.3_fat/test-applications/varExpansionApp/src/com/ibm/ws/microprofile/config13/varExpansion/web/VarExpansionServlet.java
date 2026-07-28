/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config13.varExpansion.web;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Servlet for VarExpansionTest
 */
@WebServlet("/VarExpansionServlet")
public class VarExpansionServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Test
    public void testVarExpansion() throws Exception {
        // TODO: implement test
    }

}

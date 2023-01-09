package com.ibm.ws.microprofile.config13.serverXMLWebApp.web;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/ServerXMLWebAppServlet")
public class ServerXMLWebAppServlet extends FATServlet {

    @Inject
    private ServerXMLWebAppBean bean;

    @Test
    public void testWebApplicationInjection() {
        assertEquals("serverXMLValue1", bean.getValue1());
    }

}

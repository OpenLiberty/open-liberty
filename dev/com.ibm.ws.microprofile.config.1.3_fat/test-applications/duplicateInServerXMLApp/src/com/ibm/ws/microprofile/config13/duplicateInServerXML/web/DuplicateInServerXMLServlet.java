/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config13.duplicateInServerXML.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/DuplicateInServerXMLServlet")
public class DuplicateInServerXMLServlet extends FATServlet {

    @Inject
    DuplicateInServerXMLBean bean;

    @Test
    public void duplicateAppPropertiesTest() throws Exception {
        bean.duplicateAppPropertiesTest();
    }

}

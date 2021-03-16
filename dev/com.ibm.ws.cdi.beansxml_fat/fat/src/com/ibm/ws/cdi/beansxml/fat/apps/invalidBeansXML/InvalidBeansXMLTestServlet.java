/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.apps.invalidBeansXML;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/TestServlet")
public class InvalidBeansXMLTestServlet extends FATServlet {

    private static final String MESSAGE = "Hello World!";

    @Inject
    TestBean bean;

    @Test
    public void testDisablingBeansXmlValidation() throws Exception {
        bean.setMessage(MESSAGE);
        assertEquals(MESSAGE, bean.getMessage());
    }
}

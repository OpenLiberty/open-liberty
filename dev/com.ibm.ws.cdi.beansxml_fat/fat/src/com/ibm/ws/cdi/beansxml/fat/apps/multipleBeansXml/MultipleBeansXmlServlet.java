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
package com.ibm.ws.cdi.beansxml.fat.apps.multipleBeansXml;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet("/")
public class MultipleBeansXmlServlet extends FATServlet {

    @Inject
    MyBean bean;
    /**  */
    private static final long serialVersionUID = 7425821714177410701L;

    @Test
    public void testMultipleBeansXml() {
        assertEquals("MyBean", bean.getMessage());
    }
}

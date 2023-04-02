/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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

package com.ibm.ws.cdi.beansxml.implicit.fat.noAnnotations.archiveWithNoBeansXml;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class NoCDIAnnotationsServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    private static final String VALUE = "value";

    @EJB(beanName = "SimpleEJB")
    private ManagedSimpleBean bean;

    @Test
    public void testFieldInjection() throws ServletException, IOException {
        bean.setValue(VALUE);
        String beanValue = bean.getValue();
        if (!beanValue.equals(VALUE)) {
            fail("Test FAILED bean value is " + beanValue);
        }
    }
}

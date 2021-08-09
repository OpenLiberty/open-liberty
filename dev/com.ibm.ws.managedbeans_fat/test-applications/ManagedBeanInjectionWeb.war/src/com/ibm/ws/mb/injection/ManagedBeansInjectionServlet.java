/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mb.injection;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/ManagedBeansInjectionServlet")
public class ManagedBeansInjectionServlet extends FATServlet {

    @Resource
    private TestManagedBean testManagedBean;

    @Test
    public void testInjectionWithCDI() {
        assertEquals("OK", testManagedBean.getTestString());
    }
}

/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.cdi.test.basic.injection;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.cdi.test.basic.injection.jar.AppScopedBean;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class WebServ extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    AppScopedBean myBean;

    @Test
    @Mode(TestMode.FULL)
    public void testValidatorInJar() {
        assertEquals(AppScopedBean.MSG, myBean.getMsg());
    }

}

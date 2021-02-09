/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.cdi.injectInjectionPointParam;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@WebServlet("/")
public class InjectInjectionPointAsParamServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    TestBean bean;

    @Test
    @Mode(TestMode.FULL)
    public void testInjectInjectionPointAsParam() throws IOException {
        bean.assertBeanManagerAndInjectionPoint();
    }

}

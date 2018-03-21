/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.cdi.beans.ConfigConstructorInjectionBean;

@SuppressWarnings("serial")
@WebServlet("/xtor")
public class XtorTestServletNamed extends AbstractBeanServlet {

    @Inject
    ConfigConstructorInjectionBean configBean2;

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean2;
    }

    @Test
    public void testMethod() throws Exception {
        test("SIMPLE_KEY6", "VALUE6");
    }

    @Test
    public void testConstructor() throws Exception {
        test("SIMPLE_KEY4", "VALUE4");
    }

    @Test
    public void testConstructorConfig() throws Exception {
        test("SIMPLE_KEY5", "VALUE5");
    }
}

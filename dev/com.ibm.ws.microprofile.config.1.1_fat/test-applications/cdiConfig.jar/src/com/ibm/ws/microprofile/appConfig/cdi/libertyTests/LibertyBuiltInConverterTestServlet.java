/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.libertyTests;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.cdi.web.AbstractBeanServlet;

@SuppressWarnings("serial")
@WebServlet("/libertyBuiltIn")
public class LibertyBuiltInConverterTestServlet extends AbstractBeanServlet {

    @Inject
    LibertyBuiltInConverterInjectionBean configBean;

    @Test
    public void testAtomicInteger() throws Exception {
        test("ATOMIC_INTEGER_KEY", "1");
    }

    @Test
    public void testAtomicLong() throws Exception {
        test("ATOMIC_LONG_KEY", "1");
    }

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean;
    }

}

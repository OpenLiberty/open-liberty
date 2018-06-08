/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.implicit.ejb;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.cdi12.test.implicit.ejb.SimpleEJB;
import com.ibm.ws.cdi12.test.utils.SimpleAbstract;
import componenttest.app.FATServlet;

@WebServlet("/")
public class Web1Servlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @EJB
    private SimpleEJB ejb;

    public void testImplicitEJB() {
        assertBeanWasInjected(ejb, SimpleEJB.class);
    }

    private void assertBeanWasInjected(final SimpleAbstract bean, Class<?> beanType) {
        assertThat("A " + beanType + " should have been injected.",
                   bean,
                   is(notNullValue()));
        bean.setData("test");
        assertThat("A " + beanType + " should have been injected, but simple method calls aren't working.",
                   bean.getData(),
                   is(equalTo("test")));
    }
}

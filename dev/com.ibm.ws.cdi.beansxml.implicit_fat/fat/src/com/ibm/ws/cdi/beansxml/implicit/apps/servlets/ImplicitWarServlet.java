/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.implicit.apps.servlets;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi.beansxml.implicit.apps.beans.AnnotatedBean;
import com.ibm.ws.cdi.beansxml.implicit.utils.SimpleAbstract;

import componenttest.app.FATServlet;

@WebServlet("/")
public class ImplicitWarServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST = "test";

    @Inject
    private AnnotatedBean annotatedBean;

    @Test
    public void testNoBeansXml() {
        assertBeanWasInjected(annotatedBean, AnnotatedBean.class);
    }

    private void assertBeanWasInjected(final SimpleAbstract bean, Class<?> beanType) {
        assertThat("A " + beanType + " should have been injected.",
                   bean,
                   is(notNullValue()));
        bean.setData(TEST);
        assertThat("A " + beanType + " should have been injected, but simple method calls aren't working.",
                   bean.getData(),
                   is(equalTo(TEST)));
    }

}

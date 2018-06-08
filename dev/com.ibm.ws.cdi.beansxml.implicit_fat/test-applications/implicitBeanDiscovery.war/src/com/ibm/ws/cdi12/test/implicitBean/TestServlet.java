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
package com.ibm.ws.cdi12.test.implicitBean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.cdi12.test.utils.SimpleAbstract;
import componenttest.app.FATServlet;

@WebServlet("/")
public class TestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST = "test";

    @Inject
    private InExplicitBeanArchive inExplicitArchive;

    @Inject
    private AnnotatedModeBean annotatedModeBean;

    @Inject
    private NoBeansXmlBean noBeansXmlBean;

    public void testExplicitBeanArchive() {
        assertBeanWasInjected(inExplicitArchive, InExplicitBeanArchive.class);
    }

    public void testAnnotatedBeanDiscoveryMode() {
        assertBeanWasInjected(annotatedModeBean, AnnotatedModeBean.class);
    }

    public void testNoBeansXml() {
        assertBeanWasInjected(noBeansXmlBean, NoBeansXmlBean.class);
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

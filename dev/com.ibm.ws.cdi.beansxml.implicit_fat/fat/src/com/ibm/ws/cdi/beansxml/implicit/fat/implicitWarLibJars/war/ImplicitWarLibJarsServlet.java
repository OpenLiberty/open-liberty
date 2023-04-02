/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

import org.junit.Test;

import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war.discoveryModeAll.InExplicitBeanArchive;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war.discoveryModeAnnotated.AnnotatedModeBean;
import com.ibm.ws.cdi.beansxml.implicit.fat.implicitWarLibJars.war.noBeansXml.NoBeansXmlBean;
import com.ibm.ws.cdi.beansxml.implicit.fat.utils.SimpleAbstract;

@WebServlet("/")
public class ImplicitWarLibJarsServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    private static final String TEST = "test";

    @Inject
    private InExplicitBeanArchive inExplicitArchive;

    @Inject
    private AnnotatedModeBean annotatedModeBean;

    @Inject
    private NoBeansXmlBean noBeansXmlBean;

    @Test
    public void testExplicitBeanArchive() {
        assertBeanWasInjected(inExplicitArchive, InExplicitBeanArchive.class);
    }

    @Test
    public void testAnnotatedBeanDiscoveryMode() {
        assertBeanWasInjected(annotatedModeBean, AnnotatedModeBean.class);
    }

    @Test
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

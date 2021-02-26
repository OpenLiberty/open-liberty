/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.withAnnotations;

import static com.ibm.ws.cdi12.test.utils.Utils.id;
import static componenttest.matchers.Matchers.does;
import static componenttest.matchers.Matchers.haveItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/testServlet")
public class WithAnnotationsServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Test
    public void testBasicProcessAnnotatedTypeEvent() {
        assertThat("There should be a ProcessAnnotatedType event for NonAnnotatedBean.",
                   WithAnnotationsExtension.getAllAnnotatedTypes(),
                   hasItems(id(NonAnnotatedBean.class), id(RequestScopedBean.class), id(ApplicationScopedBean.class)));
    }

    @Test
    public void testNoAnnotations() {
        assertThat("When observing ProcessAnnotatedType events for @RequestScoped annotated types, " +
                   "an event should not be fired for types with no annotations.",
                   WithAnnotationsExtension.getRequestScopedTypes(),
                   does(not(haveItem(id(NonAnnotatedBean.class)))));
    }

    @Test
    public void testNonSpecifiedAnnotation() {
        assertThat("When observing ProcessAnnotatedType events for @RequestScoped annotated types, " +
                   "an event should not be fired for types which are annotated with @ApplicationScoped, but not @RequestScoped.",
                   WithAnnotationsExtension.getRequestScopedTypes(),
                   does(not(haveItem(id(ApplicationScopedBean.class)))));
    }

    @Test
    public void testWithAnnotations() {
        assertThat("When observing ProcessAnnotatedType events for @RequestScoped annotated types, " +
                   "an event should be fired for types with the @RequestScoped annotation.",
                   WithAnnotationsExtension.getRequestScopedTypes(),
                   hasItem(id(RequestScopedBean.class)));
    }

}

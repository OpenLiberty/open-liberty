/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.withAnnotations;

import static com.ibm.ws.cdi12.test.utils.Utils.id;
import static componenttest.matchers.Matchers.does;
import static componenttest.matchers.Matchers.haveItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@WebServlet("/testServlet")
public class WithAnnotationsServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    public void testBasicProcessAnnotatedTypeEvent() {
        assertThat("There should be a ProcessAnnotatedType event for NonAnnotatedBean.",
                   WithAnnotationsExtension.getAllAnnotatedTypes(),
                   hasItems(id(NonAnnotatedBean.class), id(RequestScopedBean.class), id(ApplicationScopedBean.class)));
    }

    public void testNoAnnotations() {
        assertThat("When observing ProcessAnnotatedType events for @RequestScoped annotated types, " +
                   "an event should not be fired for types with no annotations.",
                   WithAnnotationsExtension.getRequestScopedTypes(),
                   does(not(haveItem(id(NonAnnotatedBean.class)))));
    }

    public void testNonSpecifiedAnnotation() {
        assertThat("When observing ProcessAnnotatedType events for @RequestScoped annotated types, " +
                   "an event should not be fired for types which are annotated with @ApplicationScoped, but not @RequestScoped.",
                   WithAnnotationsExtension.getRequestScopedTypes(),
                   does(not(haveItem(id(ApplicationScopedBean.class)))));
    }

    public void testWithAnnotations() {
        assertThat("When observing ProcessAnnotatedType events for @RequestScoped annotated types, " +
                   "an event should be fired for types with the @RequestScoped annotation.",
                   WithAnnotationsExtension.getRequestScopedTypes(),
                   hasItem(id(RequestScopedBean.class)));
    }

}

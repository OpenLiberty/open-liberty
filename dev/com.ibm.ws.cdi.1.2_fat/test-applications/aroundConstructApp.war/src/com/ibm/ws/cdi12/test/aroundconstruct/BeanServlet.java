/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.aroundconstruct;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

/**
 * These tests use {@link AroundConstructLogger} to record what happens while intercepting constructors.
 * <p>{@link AroundConstructLogger} is <code>@RequestScoped</code> so a new instance is created for every test.
 */
@WebServlet("/beanTestServlet")
public class BeanServlet extends AroundConstructTestServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    Bean bean;

    @Override
    protected void before() {
        bean.doSomething(); // need to actually use the injected bean, otherwise things go a bit funny
    }
}

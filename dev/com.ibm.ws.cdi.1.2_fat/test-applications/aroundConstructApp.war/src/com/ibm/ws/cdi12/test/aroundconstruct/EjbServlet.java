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

import static com.ibm.ws.cdi12.test.utils.Utils.id;
import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

/**
 * These tests use {@link AroundConstructLogger} to record what happens while intercepting constructors.
 * <p>{@link AroundConstructLogger} is <code>@RequestScoped</code> so a new instance is created for every test.
 */
@WebServlet("/ejbTestServlet")
public class EjbServlet extends AroundConstructTestServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    StatelessAroundConstructLogger logger;

    @Inject
    Ejb ejb;

    @Inject
    StatelessEjb stateless;

    @Override
    protected void before() {
        ejb.doSomething(); // need to actually use the injected bean, otherwise things go a bit funny
        stateless.doSomething();
    }

    /**
     * Test that CDI interceptors work on stateless beans
     */
    public void testStatelessAroundConstruct() {
        assertEquals("Stateless bean should be intercepted.",
                     logger.getInterceptedBean(),
                     id(StatelessEjb.class));
    }

    /**
     * Test that the correct error message is thrown when an exception is thrown from post construct lifecycle callback in interceptor
     */
    public void testPostConstructErrorMessage() {}
}

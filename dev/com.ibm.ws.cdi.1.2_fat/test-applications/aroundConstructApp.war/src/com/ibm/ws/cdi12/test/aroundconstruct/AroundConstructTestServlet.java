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
package com.ibm.ws.cdi12.test.aroundconstruct;

import static com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructLogger.ConstructorType.INJECTED;
import static com.ibm.ws.cdi12.test.utils.Utils.id;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.ConstructInterceptor;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.DirectBindingConstructInterceptor;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorOne;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorTwo;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.NonCdiInterceptor;
import com.ibm.ws.cdi12.test.aroundconstruct.interceptors.SubConstructInterceptor;
import componenttest.app.FATServlet;

/**
 * These tests use {@link AroundConstructLogger} to record what happens while intercepting constructors.
 * <p>{@link AroundConstructLogger} is <code>@RequestScoped</code> so a new instance is created for every test.
 */
@WebServlet("/testServlet")
public class AroundConstructTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    AroundConstructLogger logger;

    public void testBasicAroundConstruct() {
        assertThat("ConstructInterceptor should be activated for Bean.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(ConstructInterceptor.class)));
    }

    public void testNonCdiAroundConstruct() {
        assertThat("NonCdiInterceptor should be activated for Bean.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(NonCdiInterceptor.class)));
    }

    public void testAroundConstructInSuperClass() {
        assertThat("SubConstructInterceptor (with @AroundConstruct in its superclass) should intercept Bean construction.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(SubConstructInterceptor.class)));
    }

    public void testInjectionConstructorIsCalled() {
        assertThat("Should call the Bean constructor annotated with @Inject so injection works.",
                   logger.getConstructorType(),
                   equalTo(INJECTED));
    }

    public void testGetConstructor() {
        assertThat("'context.getConstructor()' should not return null in @AroundConstruct callbacks.",
                   logger.getConstructor(),
                   notNullValue());
    }

    public void testGetTarget() {
        assertThat("'context.getTarget()' should not return null after calling 'context.proceed()' in @AroundConstruct callbacks.",
                   logger.getTarget(),
                   notNullValue());
    }

    public void testBindingInterceptorToConstructor() {
        assertThat("Using @DirectlyIntercepted on Bean's constructor should cause it to be intercepted by DirectBindingConstructInterceptor.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(DirectBindingConstructInterceptor.class)));
    }

    public void testInterceptorOrder() {
        assertEquals("Non CDI interceptor should be called first. Order was " + logger.getConstructorInterceptors().toString(),
                     logger.getConstructorInterceptors().indexOf(id(NonCdiInterceptor.class)),
                     0);

        assertTrue("Interceptor annotated @Priority should be called before interceptors declared in beans.xml. Order was " + logger.getConstructorInterceptors().toString(),
                   logger.getConstructorInterceptors().indexOf(id(ConstructInterceptor.class)) < logger.getConstructorInterceptors().indexOf(id(InterceptorOne.class)));

        assertTrue("Order declared in beans.xml should match calling order. Order was " + logger.getConstructorInterceptors().toString(),
                   logger.getConstructorInterceptors().indexOf(id(InterceptorOne.class)) < logger.getConstructorInterceptors().indexOf(id(InterceptorTwo.class)));

    }

    public void testInterceptorNotCalledTwice() {
        Set<String> listAsSet = new HashSet<String>(logger.getConstructorInterceptors());
        //Test checks there are no duplicates in the list of inteceptors called by putting the list into a set to remove duplicates, and then checking if the set is still the same size as the list
        assertTrue("Interceptor should not be called twice. Interceptors called were " + logger.getConstructorInterceptors().toString(),
                   logger.getConstructorInterceptors().size() == listAsSet.size());
    }
}

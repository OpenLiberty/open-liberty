/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.aroundconstruct;

import static com.ibm.ws.cdi.ejb.apps.aroundconstruct.AroundConstructLogger.ConstructorType.INJECTED;
import static com.ibm.ws.cdi.ejb.utils.Utils.id;
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

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.ConstructInterceptor;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.DirectBindingConstructInterceptor;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorOne;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.InterceptorTwo;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.NonCdiInterceptor;
import com.ibm.ws.cdi.ejb.apps.aroundconstruct.interceptors.SubConstructInterceptor;

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

    @Test
    public void testBasicAroundConstruct() {
        assertThat("ConstructInterceptor should be activated for Bean.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(ConstructInterceptor.class)));
    }

    /**
     * Test that AroundConstruct works for Non CDI interceptors
     */
    @Test
    public void testNonCdiAroundConstruct() {
        assertThat("NonCdiInterceptor should be activated for Bean.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(NonCdiInterceptor.class)));
    }

    /**
     * Interceptors 1.2 - "AroundConstruct lifecycle callback interceptor methods may be defined on superclasses of interceptor
     * classes."
     */
    @Test
    public void testAroundConstructInSuperClass() {
        assertThat("SubConstructInterceptor (with @AroundConstruct in its superclass) should intercept Bean construction.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(SubConstructInterceptor.class)));
    }

    /**
     * Test that intercepting a constructor annotated with <code>@Inject</code> works.
     */
    @Test
    public void testInjectionConstructorIsCalled() {
        assertThat("Should call the Bean constructor annotated with @Inject so injection works.",
                   logger.getConstructorType(),
                   equalTo(INJECTED));
    }

    /**
     * Interceptors 1.2 - "The getConstructor method returns the constructor of the target class for which the AroundConstruct
     * interceptor was invoked."
     */
    @Test
    public void testGetConstructor() {
        assertThat("'context.getConstructor()' should not return null in @AroundConstruct callbacks.",
                   logger.getConstructor(),
                   notNullValue());
    }

    /**
     * Interceptors 1.2 - "The getTarget method returns the associated target instance. For the AroundConstruct lifecycle
     * callback interceptor method, getTarget returns null if called before the proceed method returns."
     */
    @Test
    public void testGetTarget() {
        assertThat("'context.getTarget()' should not return null after calling 'context.proceed()' in @AroundConstruct callbacks.",
                   logger.getTarget(),
                   notNullValue());
    }

    /**
     * Test that we can apply an interceptor binding annotation directly to a constructor rather than the class.
     */
    @Test
    public void testBindingInterceptorToConstructor() {
        assertThat("Using @DirectlyIntercepted on Bean's constructor should cause it to be intercepted by DirectBindingConstructInterceptor.",
                   logger.getConstructorInterceptors(),
                   hasItem(id(DirectBindingConstructInterceptor.class)));
    }

    /**
     * Interceptors should be called in the correct order as determined by the @Priority annotation and the order declared in the beans.xml
     */
    @Test
    public void testInterceptorOrder() {
        assertEquals("Non CDI interceptor should be called first. Order was " + logger.getConstructorInterceptors().toString(),
                     logger.getConstructorInterceptors().indexOf(id(NonCdiInterceptor.class)),
                     0);

        assertTrue("Interceptor annotated @Priority should be called before interceptors declared in beans.xml. Order was " + logger.getConstructorInterceptors().toString(),
                   logger.getConstructorInterceptors().indexOf(id(ConstructInterceptor.class)) < logger.getConstructorInterceptors().indexOf(id(InterceptorOne.class)));

        assertTrue("Order declared in beans.xml should match calling order. Order was " + logger.getConstructorInterceptors().toString(),
                   logger.getConstructorInterceptors().indexOf(id(InterceptorOne.class)) < logger.getConstructorInterceptors().indexOf(id(InterceptorTwo.class)));

    }

    /**
     * Interceptors should only be called once for each constructor
     */
    @Test
    @Ignore //TODO need to find out why this test does not pass
    public void testInterceptorNotCalledTwice() {
        Set<String> listAsSet = new HashSet<String>(logger.getConstructorInterceptors());
        //Test checks there are no duplicates in the list of inteceptors called by putting the list into a set to remove duplicates, and then checking if the set is still the same size as the list
        assertTrue("Interceptor should not be called twice. Interceptors called were " + logger.getConstructorInterceptors().toString(),
                   logger.getConstructorInterceptors().size() == listAsSet.size());
    }
}

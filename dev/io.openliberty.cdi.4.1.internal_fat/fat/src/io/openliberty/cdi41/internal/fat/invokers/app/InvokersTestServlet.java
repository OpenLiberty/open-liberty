/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.invokers.app;

import static io.openliberty.cdi41.internal.fat.invokers.app.ExceptionAssertions.assertThrows;
import static io.openliberty.cdi41.internal.fat.invokers.app.ExceptionAssertions.exception;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/invokersTest")
public class InvokersTestServlet extends FATServlet {

    @Inject
    private InvokersExtension ext;

    @Inject
    private InvokedBean bean;

    @Test
    public void testNullInstance() {
        // Cannot invoke {0} because the instance passed to the Invoker was null
        assertThrows(() -> {
            ext.getBasic().invoke(null, new Object[] { 1 });
        }, exception().ofType(NullPointerException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("basicMethod")
                      .messageIncludes("the instance passed to the Invoker was null"));
    }

    @Test
    public void testWrongInstanceType() {
        // Cannot invoke {0} because the instance passed to the Invoker has type {1} which cannot be cast to {2}
        assertThrows(() -> {
            ext.getBasic().invoke(new Object(), new Object[] { 1 });
        }, exception().ofType(ClassCastException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("basicMethod")
                      .messageIncludes("java.lang.Object")
                      .messageIncludes("cannot be cast")
                      .messageIncludes("InvokedBean"));
    }

    @Test
    public void testMissingArguments() {
        // Cannot invoke {0} because {1} arguments were expected but only {2} were provided
        assertThrows(() -> {
            ext.getBasic().invoke(bean, new Object[] {});
        }, exception().ofType(IllegalArgumentException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("basicMethod")
                      .messageIncludes("1 arguments were expected but only 0 were provided"));
    }

    @Test
    public void testNullArgumentsArrayWhenArgsRequired() {
        // Cannot invoke {0} because the args parameter is null and arguments are required
        assertThrows(() -> {
            ext.getBasic().invoke(bean, null);
        }, exception().ofType(NullPointerException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("basicMethod")
                      .messageIncludes("the args parameter is null and arguments are required"));
    }

    @Test
    public void testNullArgsArrayWithNoargs() throws Exception {
        // arguments may be null if the target method declares no parameter
        // No exception expected
        ext.getNoargs().invoke(bean, null);
    }

    @Test
    public void testNullPrimitiveArgument() {
        // Cannot invoke {0} because parameter {1} is a primitive type but the argument is null
        assertThrows(() -> {
            ext.getBasic().invoke(bean, new Object[] { null });
        }, exception().ofType(NullPointerException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("basicMethod")
                      .messageIncludes("parameter 1 is a primitive type but the argument is null"));
    }

    @Test
    public void testWrongPrimitveArgumentType() {
        // Cannot invoke {0} because argument {1} has type {2} which cannot be cast to {3}",
        assertThrows(() -> {
            ext.getBasic().invoke(bean, new Object[] { new Object() });
        }, exception().ofType(ClassCastException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("basicMethod")
                      .messageIncludes("argument 1 has type")
                      .messageIncludes("java.lang.Object")
                      .messageIncludes("which cannot be cast to")
                      .messageIncludes("int"));
    }

    @Test
    public void testWideningPrimitveArgumentType() throws Exception {
        // A widening primitive conversion is permitted
        // No exception expected
        ext.getBasic().invoke(bean, new Object[] { (short) 1 });
    }

    @Test
    public void testNarrowingPrimitveArgumentType() {
        // Cannot invoke {0} because argument {1} has type {2} which cannot be cast to {3}",
        // Exception message is not ideal here, but it's what we output at the moment
        assertThrows(() -> {
            ext.getBasic().invoke(bean, new Object[] { 1L });
        }, exception().ofType(ClassCastException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("basicMethod")
                      .messageIncludes("argument 1 has type")
                      .messageIncludes("java.lang.Long")
                      .messageIncludes("which cannot be cast to")
                      .messageIncludes("int"));
    }

    @Test
    public void testWrongReferenceArgumentType() {
        // Cannot invoke {0} because argument {1} has type {2} which cannot be cast to {3}",
        assertThrows(() -> {
            ext.getReference().invoke(bean, new Object[] { new Object() });
        }, exception().ofType(ClassCastException.class)
                      .messageIncludes("Cannot invoke")
                      .messageIncludes("referenceMethod")
                      .messageIncludes("argument 1 has type")
                      .messageIncludes("java.lang.Object")
                      .messageIncludes("which cannot be cast to")
                      .messageIncludes("java.lang.CharSequence"));
    }

    @Test
    public void testSubclassReferenceArgumentType() throws Exception {
        // Passing a subtype as the argument is valid
        // No exception expected
        ext.getReference().invoke(bean, new Object[] { "string" });
    }

}

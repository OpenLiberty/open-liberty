/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.optionalelresolver.servlets;

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import io.openliberty.el60.fat.optionalelresolver.beans.AnotherSimpleBean;
import io.openliberty.el60.fat.optionalelresolver.beans.SimpleBeanWithOptionalProperty;
import jakarta.el.ELContext;
import jakarta.el.ELProcessor;
import jakarta.el.ELResolver;
import jakarta.el.MethodNotFoundException;
import jakarta.el.OptionalELResolver;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet for the Jakarta Expression Language 6.0 OptionalELResolver.
 *
 * This Servlet is used to test functionality with and without the OptionalELResolver added to the
 * ELResolver list in order to verify the new behavior of the OptionalELResolver.
 */
@WebServlet({ "/EL60OptionalELResolverTest" })
public class EL60OptionalELResolverTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional property that is not Empty.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_OptionalStringProperty_WithoutOptionalELResolver() throws Exception {
        String expectedResult = "Test String!";

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(expectedResult));

        Optional<Object> actualResult = elp.eval("testBean.testString");
        log("actualResult: " + actualResult);

        // Without the OptionalELResolver a Java Optional is returned.
        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.get().equals(expectedResult));
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional property that is not Empty.
     *
     * The OptionalELResolver concertToType method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns the result of coercing {@code null} to the requested {@code type}.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_OptionalStringProperty_WithOptionalELResolver() throws Exception {
        String expectedResult = "Test String!";

        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(expectedResult));

        Object actualResult = elp.eval("testBean.testString");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver the contents of the Java Optional are returned in this case a String.
        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.equals(expectedResult));
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional String property that is Empty.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_EmptyOptionalStringProperty_WithoutOptionalELResolver() throws Exception {
        Object expectedResult = Optional.empty();

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        Object actualResult = elp.eval("testBean.testString");
        log("actualResult: " + actualResult);

        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.equals(expectedResult));
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional String property that is Empty.
     *
     * The OptionalELResolver convertToType method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns the result of coercing {@code null} to the requested {@code type}.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_EmptyOptionalStringProperty_WithOptionalELResolver() throws Exception {
        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        elp.eval("testBean.testString");

        Object result = elp.eval("testBean.testString");
        log("result: " + result);

        assertTrue("The result was: " + result + " but was expected to be: null", result == null);
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional bean property that is not empty.
     * Evaluate a String property on the Optional bean property.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_OptionalBeanProperty_WithoutOptionalELResolver() throws Exception {
        boolean propertyNotFoundExceptionThrown = false;

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String!")));

        try {
            Object actualResult = elp.eval("testBean.anotherSimpleBean.testString");
            log("actualResult: " + actualResult);
        } catch (PropertyNotFoundException e) {
            propertyNotFoundExceptionThrown = true;
        }

        // Without the OptionalELResolver a PropertyNotFoundException is thrown.
        // jakarta.el.PropertyNotFoundException: Property [testString] not found on type [java.util.Optional]
        assertTrue("A PropertyNotFoundException was not thrown as expected.", propertyNotFoundExceptionThrown);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional bean property that is not empty.
     * Evaluate a String property on the Optional bean property.
     *
     * The OptionalELResolver getValue method states the following:
     *
     * If the base object is an {@link Optional}, {@link Optional#isPresent()} returns {@code true} and the
     * property is not {@code null} then the resulting value is the result of calling
     * {@link ELResolver#getValue(ELContext, Object, Object)} using the {@link ELResolver} obtained from
     * {@link ELContext#getELResolver()} with the following parameters:
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_OptionalBeanProperty_WithOptionalELResolver() throws Exception {
        String expectedResult = "AnotherSimpleBean Test String";

        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        Object actualResult = elp.eval("testBean.anotherSimpleBean.testString");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver the testString from AnotherSimpleBean should be returned.
        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.equals(expectedResult));
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional bean property that is empty.
     * Evaluate a String property on the Optional bean property.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_EmptyOptionalBeanProperty_WithoutOptionalELResolver() throws Exception {
        boolean propertyNotFoundExceptionThrown = false;

        ELProcessor elp = new ELProcessor();;
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        try {
            Object actualResult = elp.eval("testBean.anotherSimpleBean.testString");
            log("actualResult: " + actualResult);
        } catch (PropertyNotFoundException e) {
            propertyNotFoundExceptionThrown = true;
        }

        // Without the OptionalELResolver a PropertyNotFoundException is thrown.
        // jakarta.el.PropertyNotFoundException: Property [testString] not found on type [java.util.Optional]
        assertTrue("A PropertyNotFoundException was not thrown as expected.", propertyNotFoundExceptionThrown);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional bean property that is empty.
     * Evaluate a String property on the Optional bean property.
     *
     * The OptionalELResolver getValue method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then the
     * resulting value is {@code null}.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_EmptyOptionalBeanProperty_WithOptionalELResolver() throws Exception {
        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        Object actualResult = elp.eval("testBean.anotherSimpleBean.testString");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver null should be returned.
        assertTrue("The actual result was: " + actualResult + " but was expected to be: null", actualResult == null);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Evaluate an Optional bean property that is not empty.
     * Evaluate a missing property on the Optional bean property.
     *
     * Verify that a PropertyNotFoundException is thrown.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_OptionalBeanProperty_Missing_WithOptionalELResolver() throws Exception {
        boolean propertyNotFoundExceptionThrown = false;

        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        try {
            Object actualResult = elp.eval("testBean.anotherSimpleBean.testStringMissing");
            log("actualResult: " + actualResult);
        } catch (PropertyNotFoundException e) {
            propertyNotFoundExceptionThrown = true;
        }

        // jakarta.el.PropertyNotFoundException: Property [testStringMissing] not found on type [io.openliberty.el60.fat.optionalelresolver.beans.AnotherSimpleBean]
        assertTrue("A PropertyNotFoundException was not thrown as expected.", propertyNotFoundExceptionThrown);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Test to ensure that the OptionalELResolver is read only.
     *
     * The OptionalELResolver setValue method states the following:
     *
     * If the base object is an {@link Optional} this method always throws a {@link PropertyNotWritableException} since
     * instances of this resolver are always read-only.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_PropertyNotWritableException() throws Exception {
        boolean propertyNotWritableExceptionThrown = false;

        // Use a unique ELProcessor since we have to import some classes.
        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());

        // Import the SimpleBeanWithOptionalProperty and AnotherSimpleBean so we can use them within the ELProcessor.
        elp.getELManager().importClass("io.openliberty.el60.fat.optionalelresolver.beans.SimpleBeanWithOptionalProperty");
        elp.getELManager().importClass("io.openliberty.el60.fat.optionalelresolver.beans.AnotherSimpleBean");

        try {
            elp.setVariable("testBean", "AnotherSimpleBean(\"AnotherSimpleBean Test String\")");
            elp.setVariable("testBean2", "SimpleBeanWithOptionalProperty(testBean)");

            log("testBean: " + elp.getValue("testBean", AnotherSimpleBean.class));
            log("testBean2: " + elp.getValue("testBean2", SimpleBeanWithOptionalProperty.class));

            elp.setValue("testBean2.anotherSimpleBean.testString", "test");

        } catch (PropertyNotWritableException e) {
            propertyNotWritableExceptionThrown = true;

            // Verify that the RecordELResolver is actually in the stack trace.
            // jakarta.el.PropertyNotWritableException: ELResolver not writable for type [java.util.Optional]
            // at jakarta.el.OptionalELResolver.setValue(OptionalELResolver.java:109)
            assertTrue("The OptionalELResolver was not found in the exception stack trace!", isOptionalELResolverInStackTrace(e.getStackTrace()));
        }

        assertTrue("A PropertyNotWritableException was not thrown as expected!", propertyNotWritableExceptionThrown);
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the toString() method on an Optional property, in this case a bean.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_InvokeOptionalMethod_WithoutOptionalELResolver() throws Exception {
        String expectedResult = "Optional\\[io.openliberty.el60.fat.optionalelresolver.beans.AnotherSimpleBean.*\\]";

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        Object actualResult = elp.eval("testBean.anotherSimpleBean.toString()");
        log("actualResult: " + actualResult);

        // Without the OptionalELResolver the toString() method is invoked on the Java Optional: Optional\[io.openliberty.el60.fat.optionalelresolver.beans.AnotherSimpleBean@7f20604c]
        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.toString().matches(expectedResult));
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the toString() method on an Optional property, in this case a bean.
     *
     * The OptionalELResolver invoke method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isPresent()} returns {@code true} then
     * this method returns the result of invoking the specified method on the object obtained by calling
     * {@link Optional#get()} with the specified parameters.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_InvokeOptionalMethod_WithOptionalELResolver() throws Exception {
        String expectedResult = "io.openliberty.el60.fat.optionalelresolver.beans.AnotherSimpleBean.*";

        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        Object actualResult = elp.eval("testBean.anotherSimpleBean.toString()");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver the toString() method is invoked on the Object within the Java Optional: io.openliberty.el60.fat.optionalelresolver.beans.AnotherSimpleBean@4afc616e
        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.toString().matches(expectedResult));
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the toString() method on an empty Optional property, in this case a bean.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_InvokeEmptyOptionalMethod_WithoutOptionalELResolver() throws Exception {
        String expectedResult = "Optional.empty";

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        Object actualResult = elp.eval("testBean.anotherSimpleBean.toString()");
        log("actualResult: " + actualResult);

        // Without the OptionalELResolver the toString() method is invoked on the Java Optional: Optional.empty
        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.toString().equals(expectedResult));
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the toString() method on an empty Optional property, in this case a bean.
     *
     * The OptionalELResolver invoke method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns {@code null}.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_InvokeEmptyOptionalMethod_WithOptionalELResolver() throws Exception {
        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        Object actualResult = elp.eval("testBean.anotherSimpleBean.toString()");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver null is returned.
        assertTrue("The actual result was: " + actualResult + " but was expected to be: null", actualResult == null);
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the doSomething() method on an Optional property, in this case a bean.
     * The return type of the doSomething() method is void.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_InvokeOptionalObjectMethod_WithoutOptionalELResolver_VoidReturn() throws Exception {
        boolean methodNotFoundExceptionThrown = false;

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        try {
            Object actualResult = elp.eval("testBean.anotherSimpleBean.doSomething()");
            log("actualResult: " + actualResult);
        } catch (MethodNotFoundException e) {
            methodNotFoundExceptionThrown = true;
        }

        // Without the OptionalELResolver a MethodNotFoundException is thrown.
        // jakarta.el.MethodNotFoundException: Method not found: class java.util.Optional.doSomething()
        assertTrue("A MethodNotFoundException was not thrown as expected!", methodNotFoundExceptionThrown);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the doSomething() method on an Optional property, in this case a bean.
     * The return type of the doSomething() method is void.
     *
     * The OptionalELResolver invoke method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns {@code null}.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_InvokeOptionalObjectMethod_WithOptionalELResolver_VoidReturn() throws Exception {
        String expectedResult = "AnotherSimpleBean.doSomething() called!";

        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        Object actualResult = elp.eval("testBean.anotherSimpleBean.doSomething()");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver null is returned.
        assertTrue("The actual result was: " + actualResult + " but was expected to be: null", actualResult == null);

        // The doSomething() method sets the testString property to a new value so let's check it to verify.
        actualResult = elp.eval("testBean.anotherSimpleBean.testString");
        log("actualResult: " + actualResult);

        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.equals(expectedResult));
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the doSomething() method on an Optional property that is empty, in this case a bean.
     * The return type of the doSomething() method is void.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_InvokeEmptyOptionalObjectMethod_WithoutOptionalELResolver_VoidReturn() throws Exception {
        boolean methodNotFoundExceptionThrown = false;

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        try {
            Object actualResult = elp.eval("testBean.anotherSimpleBean.doSomething()");
            log("actualResult: " + actualResult);
        } catch (

        MethodNotFoundException e) {
            methodNotFoundExceptionThrown = true;
        }

        // Without the OptionalELResolver a MethodNotFoundException is thrown.
        // jakarta.el.MethodNotFoundException: Method not found: class java.util.Optional.doSomething()
        assertTrue("A MethodNotFoundException was not thrown as expected!", methodNotFoundExceptionThrown);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the doSomething() method on an Optional property that is empty, in this case a bean.
     * The return type of the doSomething() method is void.
     *
     * The OptionalELResolver invoke method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns {@code null}.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_InvokeEmptyOptionalObjectMethod_WithOptionalELResolver_VoidReturn() throws Exception {
        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        Object actualResult = elp.eval("testBean.anotherSimpleBean.doSomething()");
        log("actualResult: " + actualResult);

        assertTrue("The actual result was: " + actualResult + " but was expected to be: null", actualResult == null);
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the returnSomething() method on an Optional property, in this case a bean.
     * The return type of the returnSomething() method is String.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_InvokeOptionalObjectMethod_WithoutOptionalELResolver_StringReturn() throws Exception {
        boolean methodNotFoundExceptionThrown = false;

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        try {
            Object actualResult = elp.eval("testBean.anotherSimpleBean.returnSomething()");
            log("actualResult: " + actualResult);
        } catch (MethodNotFoundException e) {
            methodNotFoundExceptionThrown = true;
        }

        // Without the OptionalELResolver a MethodNotFoundException is thrown.
        // jakarta.el.MethodNotFoundException: Method not found: class java.util.Optional.returnSomething()
        assertTrue("A MethodNotFoundException was not thrown as expected!", methodNotFoundExceptionThrown);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the returnSomething() method on an Optional property, in this case a bean.
     * The return type of the returnSomething() method is String.
     *
     * The OptionalELResolver invoke method statues the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isPresent()} returns {@code true} then
     * this method returns the result of invoking the specified method on the object obtained by calling
     * {@link Optional#get()} with the specified parameters.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_InvokeOptionalObjectMethod_WithOptionalELResolver_StringReturn() throws Exception {
        String expectedResult = "AnotherSimpleBean.returnSomething called!";

        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty(new AnotherSimpleBean("AnotherSimpleBean Test String")));

        Object actualResult = elp.eval("testBean.anotherSimpleBean.returnSomething()");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver the method is invoked as we'd expect!
        assertTrue("The actual result was: " + actualResult + " but was expected to be: " + expectedResult, actualResult.equals(expectedResult));
    }

    /**
     * Do not add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the returnSomething() method on an Optional property that is empty, in this case a bean.
     * The return type of the returnSomething() method is String.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60OptionalELResolver_InvokeEmptyOptionalObjectMethod_WithoutOptionalELResolver_StringReturn() throws Exception {
        boolean methodNotFoundExceptionThrown = false;

        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        try {
            Object actualResult = elp.eval("testBean.anotherSimpleBean.returnSomething()");
            log("actualResult: " + actualResult);
        } catch (MethodNotFoundException e) {
            methodNotFoundExceptionThrown = true;
        }

        // Without the OptionalELResolver a MethodNotFoundException is thrown.
        // jakarta.el.MethodNotFoundException: Method not found: class java.util.Optional.returnSomething()
        assertTrue("A MethodNotFoundException was not thrown as expected!", methodNotFoundExceptionThrown);
    }

    /**
     * Add the OptionalELResolver to the list of ELResolvers.
     *
     * Invoke the returnSomething() method on an Optional property that is empty, in this case a bean.
     * The return type of the returnSomething() method is String.
     *
     * The OptionalELResolver invoke method states the following:
     *
     * If the base object is an {@link Optional} and {@link Optional#isEmpty()} returns {@code true} then this
     * method returns {@code null}.
     *
     * @throws Exception
     */
    @Test
    public void testEL60OptionalELResolver_InvokeEmptyOptionalObjectMethod_WithOptionalELResolver_StringReturn() throws Exception {
        ELProcessor elp = new ELProcessor();
        elp.getELManager().addELResolver(new jakarta.el.OptionalELResolver());
        elp.defineBean("testBean", new SimpleBeanWithOptionalProperty());

        Object actualResult = elp.eval("testBean.anotherSimpleBean.returnSomething()");
        log("actualResult: " + actualResult);

        // With the OptionalELResolver null is returned
        assertTrue("The actual result was: " + actualResult + " but was expected to be: nulll", actualResult == null);
    }

    /*
     * Return true if the jakarta.el.OptionalELResolver is found in the stack trace.
     */
    private boolean isOptionalELResolverInStackTrace(StackTraceElement[] stack) {
        boolean optionalELResolverInStackTrace = false;

        for (StackTraceElement element : stack) {
            if (element.getClassName().equals(OptionalELResolver.class.getCanonicalName())) {
                optionalELResolverInStackTrace = true;
                log("StackTraceElement containing OptionalELResolver: " + element.toString());
                break;
            }
        }
        return optionalELResolverInStackTrace;
    }
}

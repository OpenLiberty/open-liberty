/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el50.fat.method.reference.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.el.ELContext;
import jakarta.el.ELProcessor;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.MethodReference;
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.annotation.WebServlet;

/**
 * A set of tests to test the new Expression Language 5.0
 * jakarta.el.MethodReference.
 */
@WebServlet({ "/EL50MethodReferenceServlet" })
public class EL50MethodReferenceServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private MethodReference getValueMethodReference;
    private MethodReference setValueMethodReference;
    private MethodReference setValueMethodReference2;

    public EL50MethodReferenceServlet() {
        super();
    }

    /**
     * Test to ensure that a MethodReference was returned from MethodExpression
     * objects.
     *
     * @throws Exception
     */
    @Test
    public void testMethodExpression_MethodReference() throws Exception {
        initMethodReferences();

        assertTrue("The getValueMethodReference was null and should not have been.", getValueMethodReference != null);
        assertTrue("The setValueMethodReference was null and should not have been.", setValueMethodReference != null);
    }

    /**
     * Test to ensure that the hashCode method of MethodReference returns a unique hash code for each MethodReference.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_hashCode() throws Exception {
        initMethodReferences();

        assertTrue("The getValueMethodReference hash code and setValueMethodReference hash code were the same and should not have been.",
                   getValueMethodReference.hashCode() != setValueMethodReference.hashCode());

    }

    /**
     * Test to ensure that the getBase method of MethodReference returns a valid value.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodRefenence_getBase() throws Exception {
        String baseClassName = "io.openliberty.el50.fat.method.reference.servlet.EL50MethodReferenceServlet$TestBean";

        initMethodReferences();
        String setValueBaseClassName = setValueMethodReference.getBase().getClass().getName();
        String getValueBaseClassName = getValueMethodReference.getBase().getClass().getName();

        assertTrue("The setValueMethodReference base Class should be: " + baseClassName + " but was: " + setValueBaseClassName, setValueBaseClassName.equals(baseClassName));
        assertTrue("The getValueMethodReference base Class should be: " + baseClassName + " but was: " + getValueBaseClassName, getValueBaseClassName.equals(baseClassName));

    }

    /**
     * Test to ensure that the getAnnotations method of MethodReference returns an empty Array
     * when there are no Annotations on a method.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_getAnnotations_none() throws Exception {
        initMethodReferences();

        Annotation[] annotations = setValueMethodReference.getAnnotations();
        assertTrue("The setValueMethodReference should have no Annotations but had: " + annotations.length,
                   annotations.length == 0);
    }

    /**
     * Test to ensure that the getAnnotations method of MethodReference returns an Array of
     * Annotations when there are one or more on a method.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_getAnnotations_exists() throws Exception {
        initMethodReferences();

        Annotation[] annotations = getValueMethodReference.getAnnotations();
        assertTrue("The getValueMethodReference should have 1 Annotation but had: " + annotations.length,
                   annotations.length == 1);
    }

    /**
     * Test to ensure that the equals method of MethodReference returns false when two different MethodReference objects
     * reference different methods on the same base class.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_equals_notequal() throws Exception {
        initMethodReferences();

        assertFalse("The setValueMethodReference and getValueMethodExpression should not be equal but they were.", setValueMethodReference.equals(getValueMethodReference));
    }

    /**
     * Test to ensure that the equals method of MethodReference returns true when two MethodReference objects
     * reference the same method on the same base class.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_equals_equal() throws Exception {
        initMethodReferences();

        assertTrue("The setValueMethodReference should be equal to itself but was not.", setValueMethodReference.equals(setValueMethodReference));
        assertTrue("The setValueMethodReference should be equal to setValueMethodReference2 but was not.", setValueMethodReference.equals(setValueMethodReference2));
    }

    /**
     * Test to ensure that the getEvaluatedParameters method of MethodReference returns 0 when
     * there are no parameters passed to the method.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_getEvaluatedParameters_none() throws Exception {
        initMethodReferences();

        Object[] getValueParams = getValueMethodReference.getEvaluatedParameters();
        assertTrue("The getValueMethodReference should have no evaluated parameters but had: " + getValueParams.length, getValueParams.length == 0);
    }

    /**
     * Test to ensure that the getEvaluatedParameters method of MethodReference returns 1 when there
     * is one parameter passed to the method.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_getEvaluatedParameters_exists() throws Exception {
        initMethodReferences();

        Object[] setValueParams = setValueMethodReference.getEvaluatedParameters();
        assertTrue("The setValueMethodReference should have one evaluated parameters but had: " + setValueParams.length, setValueParams.length == 1);
    }

    /**
     * Test to ensure that the getMethodInfo method of MethodReference does not return null.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodReference_getMethodInfo() throws Exception {
        initMethodReferences();

        assertTrue("The getValueMethodReference MethodInfo was null and should not have been.", getValueMethodReference.getMethodInfo() != null);
        assertTrue("The setValueMethodReference MethodInfo was null and should not have been.", setValueMethodReference.getMethodInfo() != null);

    }

    /**
     * https://jakarta.ee/specifications/expression-language/5.0/apidocs/jakarta.el/jakarta/el/methodexpression#getMethodReference(jakarta.el.ELContext)
     *
     * NullPointerException - If the supplied context is null
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodExpression_MethodReference_NullPointerException() throws Exception {
        boolean passed = false;
        TestBean bean = new TestBean();
        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", bean);
        ExpressionFactory factory = ExpressionFactory.newInstance();
        ELContext context = elp.getELManager().getELContext();
        MethodExpression getValueExpression = factory.createMethodExpression(context, "${testBean.getValue()}", String.class, new Class[0]);

        try {
            @SuppressWarnings("unused")
            MethodReference reference = getValueExpression.getMethodReference(null);
        } catch (NullPointerException npe) {
            passed = true;
        }

        assertTrue("A NullPointerException was not thrown when the ELContext was null.", passed);
    }

    /**
     * https://jakarta.ee/specifications/expression-language/5.0/apidocs/jakarta.el/jakarta/el/methodexpression#getMethodReference(jakarta.el.ELContext)
     *
     * PropertyNotFoundException - If a property/variable resolution failed because no match was found or a match was found but was not readable
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodExpression_MethodReference_PropertyNotFoundException() throws Exception {
        boolean passed = false;
        TestBean bean = new TestBean();
        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", bean);
        ExpressionFactory factory = ExpressionFactory.newInstance();
        ELContext context = elp.getELManager().getELContext();
        MethodExpression getValueExpression = factory.createMethodExpression(context, "${testBeanNotFound.getValue()}", String.class, new Class[0]);

        try {
            @SuppressWarnings("unused")
            MethodReference reference = getValueExpression.getMethodReference(context);
        } catch (PropertyNotFoundException pnfe) {
            passed = true;
        }

        assertTrue("A PropertyNotFoundException was not thrown when the variable resolution failed because no match was found.", passed);
    }

    /**
     * https://jakarta.ee/specifications/expression-language/5.0/apidocs/jakarta.el/jakarta/el/methodexpression#getMethodReference(jakarta.el.ELContext)
     *
     * MethodNotFoundException - If no matching method can be found
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testMethodExpression_MethodReference_MethodNotFoundException() throws Exception {
        boolean passed = false;
        TestBean bean = new TestBean();
        ELProcessor elp = new ELProcessor();
        elp.defineBean("testBean", bean);
        ExpressionFactory factory = ExpressionFactory.newInstance();
        ELContext context = elp.getELManager().getELContext();

        // method getValues() vs getValue()
        MethodExpression getValueExpression = factory.createMethodExpression(context, "${testBean.getValues()}", String.class, new Class[0]);

        try {
            @SuppressWarnings("unused")
            MethodReference reference = getValueExpression.getMethodReference(context);
        } catch (MethodNotFoundException mnfe) {
            passed = true;
        }

        assertTrue("A MethodNotFoundException was not thrown when there was no matching method.", passed);
    }

    /*
     * Initialize the MethodReference objects used by the tests.
     */
    private void initMethodReferences() {
        if (setValueMethodReference == null || getValueMethodReference == null) {
            TestBean bean = new TestBean();
            ELProcessor elp = new ELProcessor();
            ELContext context = elp.getELManager().getELContext();

            elp.defineBean("testBean", bean);

            ExpressionFactory factory = ExpressionFactory.newInstance();

            MethodExpression getValueExpression = factory.createMethodExpression(context, "${testBean.getValue()}", String.class, new Class[0]);
            getValueMethodReference = getValueExpression.getMethodReference(context);

            MethodExpression setValueExpression = factory.createMethodExpression(context, "${testBean.setValue(\"test\")}", null, new Class[] { String.class });
            setValueMethodReference = setValueExpression.getMethodReference(context);
            setValueMethodReference2 = setValueExpression.getMethodReference(context);
        }
    }

    public class TestBean {
        private String value;

        @Deprecated
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}

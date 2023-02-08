/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.ParentRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * Alternate implementation of {@link Parameterized}. This implementation
 * changes the naming implementation to include the test parameters in the
 * test names.
 *
 * Use of the 'name' attribute of {@link Parameterized.Parameter} is preferred,
 * but the current in use JUNIT version does not yet support that. See
 * {@link "https://junit.org/junit4/javadoc/4.12/index.html?org/junit/runners/Parameterized.html"}
 */
public class NamedParameterized extends Suite {
    /**
     * Generate naming text for a parameters array. Answer one of
     * the following based on whether the parameters is null or empty,
     * has one, or has multiple elements:
     *
     * <pre>
     *     ()
     *     (parm[0])
     *     (parm[0], parm[1], ...)
     * </pre>
     *
     * @param parms The parameters for which to generate naming text.
     *
     * @return Naming text for the parameters.
     */
    public static String generateParmsText(Object[] parms) {
        if ((parms == null) || (parms.length == 0)) {
            return "()";
        } else if (parms.length == 1) {
            return "(" + parms[0] + ")";
        } else {
            StringBuilder builder = new StringBuilder('(');
            boolean isFirst = true;
            for (Object parm : parms) {
                if (!isFirst) {
                    builder.append(',');
                    builder.append(' ');
                } else {
                    isFirst = false;
                }
                builder.append(parm);
            }
            return builder.toString();
        }
    }

    /**
     * Create a runner for a test class using a collection of parameters.
     *
     * @param testClass The test class which is to be run.
     * @param parmsNo   The index of the the parameters in the overall parameters collection.
     * @param parms     The parameters collection which were selected using the parameters
     *                      index and which will be used to instantiate the test class for
     *                      running tests.
     *
     * @throws InitializationError Thrown in case of a problem initializing the test runner.
     */
    public class TestRunner extends BlockJUnit4ClassRunner {
        public TestRunner(Class<?> testClass, int parmsNo, Object[] parms) throws InitializationError {
            super(testClass);

            this.parmsNo = parmsNo;
            this.parms = parms;

            this.parmsNoText = "[" + Integer.toString(parmsNo) + "]";
            this.parmsText = generateParmsText(parms);
            this.name = parmsNoText + parmsText;
        }

        private final int parmsNo;

        public int getParmsNo() {
            return parmsNo;
        }

        private final Object[] parms;

        public Object[] getParms() {
            return parms;
        }

        private final String parmsNoText;
        private final String parmsText;

        public String getParmNoText() {
            return parmsNoText;
        }

        public String getParmsText() {
            return parmsText;
        }

        // Update naming methods:

        private final String name;

        /**
         * {@link ParentRunner} API: Answer a name for the test
         * runner. This implementation answers text which contains
         * the parameters collection number and which includes the
         * parameter values. That is, one of:
         *
         * <pre>
         *     [parmsNo]()
         *     [parmsNo](parm[0])
         *     [parmsNo](parm[0], parm[1], ...)
         * </pre>
         *
         * @return A name for this test runner.
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@link ParentRunner} API: Answer a name for a test method.
         * This implementation answers the method name plus the runner
         * name. That is, one of:
         *
         * <pre>
         *     testName[parmsNo]()
         *     testName[parmsNo](parm0)
         *     testName[parmsNo](parm0, parm1, ...)
         * </pre>
         *
         * @param method The method which is to be named.
         *
         * @return A name for the test method.
         */
        @Override
        protected String testName(FrameworkMethod method) {
            return method.getName() + getName();
        }

        // Unchanged standard implementations.

        /**
         * Subclass API: Create a test: Implement by instantiating the test class
         * using the parameters collection.
         *
         * @return The test class instance.
         *
         * @throws Exception Thrown in case the test class instance cannot be created.
         */
        @Override
        public Object createTest() throws Exception {
            return getTestClass().getOnlyConstructor().newInstance(getParms());
        }

        /**
         * Subclass API: Validate the test class. The test class must have only
         * one constructor. Add an error to the errors collection if multiple
         * constructors are found.
         *
         * See {@link #validateOnlyOneConstructor(List)}.
         *
         * @param errors Storage for generated errors.
         */
        @Override
        protected void validateConstructor(List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
        }

        /**
         * Subclass API: Run tests on a class. Implement by delegating to
         * {@link #childrenInvoker(RunNotifier)}.
         *
         * @param notifier A notifier encapsulating a test class.
         */
        @Override
        protected Statement classBlock(RunNotifier notifier) {
            return childrenInvoker(notifier);
        }
    }

    // Values for reflectively invoking a static method which has no parameters.
    private static final Object STATIC_RECEIVER = null;
    private static final Object[] NULL_ARGS = new Object[0];

    /**
     * Retrieve the parameters from a test class.
     *
     * At least one public static method which has an {@link Parameters} annotation
     * must be present on the test class. The method must take no arguments and
     * return a list of object arrays.
     *
     * A runtime exception is thrown if no such method is available. The first
     * method which is located is used.
     *
     * Invoke and return the value returned by the located parameters method.
     *
     * @param testClass The test class from which to retrieve parameters.
     *
     * @return Parameters from the test class.
     *
     * @throws Throwable Thrown in case of a failure to invoke the parameters method.
     */
    @SuppressWarnings("unchecked")
    public static List<Object[]> getParameters(TestClass testClass) throws Throwable {
        FrameworkMethod parmsMethod = locateParametersMethod(testClass);
        if (parmsMethod == null) {
            throw new IllegalArgumentException("Test class [ " + testClass + " ] has no public static parameters method.");
        }
        return (List<Object[]>) parmsMethod.invokeExplosively(STATIC_RECEIVER, NULL_ARGS); // throws Throwable
    }

    /**
     * Locate and return the parameters method of a test class. If multiple parameters
     * methods are located, answer the first such method. (This is unpredictable.)
     * If no parameters are located, answer null.
     *
     * See {@link #isParametersMethod(FrameworkMethod)}.
     *
     * @param testClass The test class from which to locate a parameters method.
     *
     * @return The parameters method of the test class. Null if none is found.
     */
    public static FrameworkMethod locateParametersMethod(TestClass testClass) {
        for (FrameworkMethod fMethod : testClass.getAnnotatedMethods(Parameterized.Parameters.class)) {
            if (isParametersMethod(fMethod)) {
                return fMethod;
            }
        }
        return null;
    }

    /**
     * Tell if a method is a parameters method. The method must be public, static,
     * must have no arguments, and must return an {@link Iterable} type object.
     *
     * The list must be of arrays of objects. The types of the list values are not
     * checked.
     *
     * @param fMethod The method to test as a parameters method.
     *
     * @return True or false telling if the method is a parameters method.
     */
    public static boolean isParametersMethod(FrameworkMethod fMethod) {
        int modifiers = fMethod.getMethod().getModifiers();
        if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
            return false;
        }
        Method jMethod = fMethod.getMethod();
        if (jMethod.getParameterTypes().length != 0) {
            return false;
        }
        Class<?> jReturnType = jMethod.getReturnType();
        if ((jReturnType == null) || !Iterable.class.isAssignableFrom(jReturnType)) {
            return false;
        }
        return true;
    }

    //

    /**
     * Create a new parameterized test runner for a specified
     * test class.
     *
     * Locate the parameters collections specified on the test class.
     * Iterate across those parameter collections and instantiate the
     * test class on each parameter collection, then invoke tests on
     * the test class as usual.
     *
     * @param testClass The class which is to be run.
     *
     * @throws Throwable Thrown in case of an error, either when retrieving
     *                       parameters or when creating test runners.
     */
    public NamedParameterized(Class<?> testClass) throws Throwable {
        super(testClass, Collections.emptyList());

        TestClass useTestClass = getTestClass();
        Class<?> testJavaClass = useTestClass.getJavaClass();

        List<Object[]> parms = getParameters(useTestClass);
        int numParms = parms.size();

        List<Runner> useRunners = new ArrayList<>(numParms);
        for (int parmNo = 0; parmNo < numParms; parmNo++) {
            useRunners.add(new TestRunner(testJavaClass, parmNo, parms.get(parmNo)));
        }
        this.runners = useRunners;
    }

    //

    private final List<Runner> runners;

    /**
     * Suite subclass API: Answer the test runners of the test suite. This
     * implementation answers runners populated with one runner for each
     * parameters collection.
     *
     * @return The child runners of this test suite.
     */
    @Override
    protected List<Runner> getChildren() {
        return runners;
    }
}

/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package com.ibm.test.spring;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;
import java.util.Set;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebListener;

/**
 *
 */
@WebListener
public class SpringLoaderHookTestSCI implements ServletContainerInitializer, ServletContextListener {

    private final static String DUMMY_CLASS = "com.ibm.test.spring.MyDummyClass1";
    private final static String DUMMY_CLASS_2 = "com.ibm.test.spring.MyDummyClass2";

    /*
     * (non-Javadoc)
     * 
     * @see jakarta.servlet.ServletContainerInitializer#onStartup(java.util.Set, jakarta.servlet.ServletContext)
     */
    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        System.out.println("SpringLoaderHookTestSCI - onStartup");
        doStuff();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jakarta.servlet.ServletContextListener#contextDestroyed(jakarta.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // no-op
    }

    /*
     * (non-Javadoc)
     * 
     * @see jakarta.servlet.ServletContextListener#contextInitialized(jakarta.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.out.println("SpringLoaderHookTestSCI - contextInitialized");
        doStuff();
    }

    private void doStuff() {
        ClassFileTransformer cft = new MyTransformer("this");
        ClassLoader slThisCL = this.getClass().getClassLoader();
        addTransformer(slThisCL, cft);

        System.out.println("SpringLoaderHookTestSCI: this.classloader - loading " + DUMMY_CLASS);
        Class<?> c = null;
        try {
            c = slThisCL.loadClass(DUMMY_CLASS); // this should invoke the ClassFileTransformer
        } catch (ClassNotFoundException ex) {
            // no op - c will be null in this case
        }
        System.out.println("SpringLoaderHookTestSCI: this.classloader - loaded " + c);

        ClassLoader throwawayCL = getThrowawayClassLoader(slThisCL);
        System.out.println("SpringLoaderHookTestSCI: this.getThrowawayClassLoader = " + throwawayCL);

        // now do it again for the thread context classloader:
        cft = new MyTransformer("tccl");
        ClassLoader slTCCL = Thread.currentThread().getContextClassLoader();
        addTransformer(slTCCL, cft);

        System.out.println("SpringLoaderHookTestSCI: tccl.classloader - loading " + DUMMY_CLASS_2);
        c = null;
        try {
            c = slTCCL.loadClass(DUMMY_CLASS_2); // this should invoke the ClassFileTransformer
        } catch (ClassNotFoundException ex) {
            // no op - c will be null in this case
        }
        System.out.println("SpringLoaderHookTestSCI: tccl.classloader - loaded " + c);

        throwawayCL = getThrowawayClassLoader(slTCCL);
        System.out.println("SpringLoaderHookTestSCI: tccl.getThrowawayClassLoader = " + throwawayCL);
    }

    /*
     * Reflectively invoke the addTransform method - simulating what Spring would do.
     */
    private void addTransformer(ClassLoader loader, ClassFileTransformer cft) {
        try {
            Method m = loader.getClass().getMethod("addTransformer", ClassFileTransformer.class);
            m.invoke(loader, cft);
        } catch (Throwable t) {
            System.out.println("SpringLoaderHookTestSCI - addTransformer method failed");
            t.printStackTrace();
        }
    }

    /*
     * Reflectively invoke the getThrowawayClassLoader method - simulating what Spring would do.
     */
    private ClassLoader getThrowawayClassLoader(ClassLoader loader) {
        ClassLoader throwawayLoader;
        try {
            Method m = loader.getClass().getMethod("getThrowawayClassLoader");
            throwawayLoader = (ClassLoader) m.invoke(loader);
        } catch (Throwable t) {
            System.out.println("SpringLoaderHookTestSCI - getThrowawayClassLoader method failed");
            t.printStackTrace();
            throwawayLoader = null;
        }
        return throwawayLoader;
    }
}

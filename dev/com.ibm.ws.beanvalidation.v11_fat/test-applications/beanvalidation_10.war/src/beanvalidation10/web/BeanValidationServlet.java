/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package beanvalidation10.web;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import beanvalidation10.web.beans.AValidationAnnTestBean;
import beanvalidation10.web.beans.AValidationMixTestBean;
import beanvalidation10.web.beans.AValidationXMLTestBean;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public class BeanValidationServlet extends FATServlet {

    /**
     * Verify that the Bean Validation feature is loaded properly and is
     * functional by calling Validation.buildDefaultValidatorFactory. This
     * gives no guarantees as to if/which validation.xml is found because
     * the container doesn't manage the bootstrap of the configuration.
     */
    public void testBuildDefaultValidatorFactory() throws Exception {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("BeanValidationTest ClassLoader : " + toString(cl));
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("META-INF/services/javax.validation.spi.ValidationProvider");
        System.out.println("BeanValidationTest Found : " + resources);
        if (resources != null) {
            while (resources.hasMoreElements()) {
                System.out.println("BeanValidationTest Found : " + resources.nextElement());
            }
        }
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        if (vf == null) {
            throw new IllegalStateException("Valication.buildDefaultValidatorFactory returned null.");
        }
    }

    /**
     * Verify that the module ValidatorFactory may be looked up at:
     *
     * java:comp/ValidatorFactory
     */
    public void testLookupJavaCompValidatorFactory() throws Exception {
        Context context = new InitialContext();
        ValidatorFactory vfactory = (ValidatorFactory) context.lookup("java:comp/ValidatorFactory");
        if (vfactory == null) {
            throw new IllegalStateException("lookup(java:comp/ValidatorFactory) returned null.");
        }
    }

    /**
     * Verify that the module Validator may be looked up at:
     *
     * java:comp/Validator
     */
    public void testLookupJavaCompValidator() throws Exception {
        Context context = new InitialContext();
        Validator validator = (Validator) context.lookup("java:comp/Validator");
        if (validator == null) {
            throw new IllegalStateException("lookup(java:comp/Validator) returned null.");
        }
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml.
     */
    public void testValidatingXMLBeanWithConstraints() throws Exception {
        AValidationXMLTestBean bean = new AValidationXMLTestBean();
        bean.checkInjectionValidation();
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml
     * where validation failures are expected.
     */
    public void testValidatingXMLBeanWithConstraintsToFail() throws Exception {
        AValidationXMLTestBean bean = new AValidationXMLTestBean();
        bean.checkInjectionValidationFail();
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml and annotations.
     */
    public void testValidatingMixBeanWithConstraints() throws Exception {
        AValidationMixTestBean bean = new AValidationMixTestBean();
        bean.checkInjectionValidation();
    }

    /**
     * Test validation of a bean with constraints defined in validation.xml and annotations
     * where validation failures are expected.
     */
    public void testValidatingMixBeanWithConstraintsToFail() throws Exception {
        AValidationMixTestBean bean = new AValidationMixTestBean();
        bean.checkInjectionValidationFail();
    }

    /**
     * Test validation of a bean with constraints defined in annotations.
     */
    public void testValidatingAnnBeanWithConstraints() throws Exception {
        AValidationAnnTestBean bean = new AValidationAnnTestBean();
        bean.checkInjectionValidation();
    }

    /**
     * Test validation of a bean with constraints defined in annotations
     * where validation failures are expected.
     */
    public void testValidatingAnnBeanWithConstraintsToFail() throws Exception {
        AValidationAnnTestBean bean = new AValidationAnnTestBean();
        bean.checkInjectionValidationFail();
    }

    private static String toString(ClassLoader cl) {
        if (cl instanceof URLClassLoader) {
            URL[] classpath = ((URLClassLoader) cl).getURLs();
            return cl.toString() + " : " + Arrays.toString(classpath);
        }
        return cl.toString();
    }
}
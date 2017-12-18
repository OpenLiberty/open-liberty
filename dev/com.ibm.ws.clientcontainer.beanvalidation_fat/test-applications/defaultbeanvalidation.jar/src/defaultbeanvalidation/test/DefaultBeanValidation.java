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
package defaultbeanvalidation.test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import defaultbeanvalidation.client.beans.AValidationAnnTestBean;


public class DefaultBeanValidation {

    /**
     * Verify that the Bean Validation feature is loaded properly and is
     * functional by calling Validation.buildDefaultValidatorFactory.
     */
    public void testDefaultBuildDefaultValidatorFactory() throws Exception {

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
    public void testDefaultLookupJavaCompValidatorFactory() throws Exception {
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
    public void testDefaultLookupJavaCompValidator() throws Exception {
        Context context = new InitialContext();
        Validator validator = (Validator) context.lookup("java:comp/Validator");
        if (validator == null) {
            throw new IllegalStateException("lookup(java:comp/Validator) returned null.");
        }
    }

    /**
     * Test validation of a bean with constraints defined in annotations.
     */
    public void testDefaultValidatingBeanWithConstraints() throws Exception {
        AValidationAnnTestBean bean = new AValidationAnnTestBean();
        bean.checkInjectionValidation();
    }

    /**
     * Test validation of a bean with constraints defined in annotations
     * where validation failures are expected.
     */
    public void testDefaultValidatingBeanWithConstraintsToFail() throws Exception {
        AValidationAnnTestBean bean = new AValidationAnnTestBean();
        bean.checkInjectionValidationFail();
    }

//    /**
//     * Test that EL expressions are evaluated in this 1.1 app running on the 1.1 feature.
//     */
//    public void testELValidationViolationMessage(HttpServletRequest request,
//                                                 HttpServletResponse response) throws Exception {
//        boolean isELEnabled = Boolean.parseBoolean(request.getParameter("isELEnabled"));
//        AValidationAnnTestBean bean = new AValidationAnnTestBean();
//        bean.checkELValidationMessage(isELEnabled);
//    }

    private static String toString(ClassLoader cl) {
        if (cl instanceof URLClassLoader) {
            URL[] classpath = ((URLClassLoader) cl).getURLs();
            return cl.toString() + " : " + Arrays.toString(classpath);
        }
        return cl.toString();
    }
}
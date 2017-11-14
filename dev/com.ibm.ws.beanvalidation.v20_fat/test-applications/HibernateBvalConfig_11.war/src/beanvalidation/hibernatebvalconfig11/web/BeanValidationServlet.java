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
package beanvalidation.hibernatebvalconfig11.web;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.validation.MessageInterpolator;
import javax.validation.ValidatorFactory;

import componenttest.app.FATServlet;

@WebServlet("/BeanValidationServlet")
@SuppressWarnings("serial")
public class BeanValidationServlet extends FATServlet {

    @Resource
    ValidatorFactory validatorFactory;

    /**
     * Test that this web module that has a validaion.xml specified with all
     * classes available in the hibernate bval 1.1 implementation can properly load the
     * classes and build the validator factory accordingly.
     */
    public void testBuildHibernateConfiguredValidatorFactory() throws Exception {
        if (validatorFactory == null) {
            throw new IllegalStateException("Injection of ValidatorFactory never occurred.");
        }

        MessageInterpolator mi = validatorFactory.getMessageInterpolator();
        String expectedInterpolator = "org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator";
        String actualInterpolator = mi.getClass().getName();
        assertEquals("the correct message interpolator wasn't provided: " + actualInterpolator,
                     expectedInterpolator, actualInterpolator);
    }

    /**
     * Ensure that the hibernate bval impl classes have the proper/expected visibility
     * to the application.
     */
    public void testHibernateBvalImplClassVisibility() throws Exception {
        String providerImpl = "org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator";
        try {
            Class.forName(providerImpl);
            throw new Exception("application should not be able to load provider class with default classloader");
        } catch (ClassNotFoundException e) {
            // expected
        }

        // Thread context classloader should, however have visibility
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Class.forName(providerImpl, false, tccl);
    }
}

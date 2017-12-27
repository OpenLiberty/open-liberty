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
package beanvalidation.cdi.web;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.validation.ValidatorFactory;

import componenttest.app.FATServlet;

/**
 * Servlet that obtains a ValidatorFactory through using the @Resource annotation.
 */
@WebServlet("/BValAtResourceServlet")
@SuppressWarnings("serial")
public class BValAtResourceServlet extends FATServlet {

    @Resource
    ValidatorFactory resourceValidatorFactory;

    /**
     * Test that a ValidatorFactory obtained with @Resource has the custom message
     * interpolator set and that the custom message interpolator was able to inject
     * another bean in this application.
     */
    public void testCDIInjectionInInterpolatorAtResource() throws Exception {
        String interpolatedValue = resourceValidatorFactory.getMessageInterpolator().interpolate(null, null);

        if (!interpolatedValue.equals("something")) {
            throw new Exception("custom interpolator should have returned the value " +
                                "'something', but returned : " + interpolatedValue);
        }

    }

    /**
     * Test that a ValidatorFactory obtained with @Resource has the custom message
     * interpolator set and that the custom message interpolator was NOT able to inject
     * another bean in this application due to the CDI feature being disabled.
     */
    public void testDynamicStopOfCDI() throws Exception {
        try {
            resourceValidatorFactory.getMessageInterpolator().interpolate(null, null);
            throw new Exception("custom interpolator should have thrown an IllegalStateException since CDI is disabled.");
        } catch (IllegalStateException ex) {
            assertEquals("IllegalStateException message should be, \"bean is null, CDI must not have injected it\"",
                         "bean is null, CDI must not have injected it",
                         ex.getMessage());
            System.out.println("testDynamicStopOfCDI() threw expected IllegalStateException.");
        }
    }
}
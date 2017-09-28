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

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.ValidatorFactory;

import componenttest.app.FATServlet;

/**
 * Servlet that obtains a ValidatorFactory through using the CDI @Inject annotation.
 */
@WebServlet("/BValAtInjectServlet")
@SuppressWarnings("serial")
public class BValAtInjectServlet extends FATServlet {

    @Inject
    ValidatorFactory injectValidatorFactory;

    /**
     * Test that a ValidatorFactory obtained with @Inject has the custom message
     * interpolator set and that the custom message interpolator was able to inject
     * another bean in this application.
     */
    public void testCDIInjectionInInterpolatorAtInject() throws Exception {
        String interpolatedValue = injectValidatorFactory.getMessageInterpolator().interpolate(null, null);

        if (!interpolatedValue.equals("something")) {
            throw new Exception("custom interpolator should have returned the value " +
                                "'something', but returned : " + interpolatedValue);
        }

    }

}
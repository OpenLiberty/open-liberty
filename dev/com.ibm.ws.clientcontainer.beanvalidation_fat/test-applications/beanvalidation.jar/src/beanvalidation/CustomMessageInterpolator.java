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
package beanvalidation;

import java.util.Locale;

import javax.validation.MessageInterpolator;

import beanvalidation.test.BeanValidationInjection;

/**
 * A custom MessageInterpolator implementation to verify that it can be
 * plugged in and used.
 */
public class CustomMessageInterpolator implements MessageInterpolator {

    /**
     * Default constuctor needed since a non-default one below is specified.
     */
    public CustomMessageInterpolator() {}

    /**
     * Non-default constructor provided to test the {@link CustomParameterNameProvider} in
     * the servlet test {@link BeanValidationInjection#testCustomParameterNameProvider()}
     */
    public CustomMessageInterpolator(String x, String y, String z) {}

    @Override
    public String interpolate(String arg0, Context arg1) {
        return arg0 + "### interpolator added message ###";
    }

    @Override
    public String interpolate(String arg0, Context arg1, Locale arg2) {
        return arg0 + "### interpolator added message ###";
    }

}

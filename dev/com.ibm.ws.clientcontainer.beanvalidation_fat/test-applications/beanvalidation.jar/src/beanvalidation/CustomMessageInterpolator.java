/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

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

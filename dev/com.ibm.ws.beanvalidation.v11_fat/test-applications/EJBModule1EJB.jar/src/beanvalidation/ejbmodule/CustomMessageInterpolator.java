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
package beanvalidation.ejbmodule;

import java.util.Locale;

import javax.validation.MessageInterpolator;

/**
 * Provide a custom implementation to easily test the ValidatorFactory
 * configuration.
 */
public class CustomMessageInterpolator implements MessageInterpolator {

    @Override
    public String interpolate(String arg0, Context arg1) {
        return arg0 + "### interpolator added message ###";
    }

    @Override
    public String interpolate(String arg0, Context arg1, Locale arg2) {
        return arg0 + "### interpolator added message ###";
    }

}

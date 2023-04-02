/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

package bval.v20.multixml;

import java.util.Locale;

import javax.validation.MessageInterpolator;

/**
 * Provide a custom implementation to easily test the ValidatorFactory
 * configuration.
 */
public class CustomMessageInterpolator3 implements MessageInterpolator {

    @Override
    public String interpolate(String arg0, Context arg1) {
        return arg0 + "### interpolator3 added message ###";
    }

    @Override
    public String interpolate(String arg0, Context arg1, Locale arg2) {
        return arg0 + "### interpolator3 added message ###";
    }

}

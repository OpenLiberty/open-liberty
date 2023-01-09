/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.provisioning;

import java.util.Locale;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public enum ActivationType {
    PARALLEL,
    SEQUENTIAL;

    @FFDCIgnore(IllegalArgumentException.class)
    public static ActivationType fromString(String s) {
        if (s == null)
            return SEQUENTIAL;

        ActivationType result;
        try {
            result = ActivationType.valueOf(s.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException iae) {
            result = SEQUENTIAL;
        }
        return result;
    }
}

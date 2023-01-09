/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature;

import java.util.Locale;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * An enum indicating visibility information
 */
public enum Visibility {
    /** Visible to all */
    PUBLIC,
    /** Visible to other features, but not visible externally */
    PROTECTED,
    /** Visible only to the product that contributes it */
    PRIVATE,
    /** Visible to installers but not the runtime */
    INSTALL;

    @FFDCIgnore(IllegalArgumentException.class)
    public static Visibility fromString(String s) {
        if (s == null)
            return PRIVATE;

        Visibility result;
        try {
            result = Visibility.valueOf(s.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException iae) {
            result = PRIVATE;
        }
        return result;
    }
}

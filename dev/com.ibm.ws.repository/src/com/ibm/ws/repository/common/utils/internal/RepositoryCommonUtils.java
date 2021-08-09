/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.common.utils.internal;

import java.util.Locale;

/**
 * A collection of utilities for using in the repository.
 */
public class RepositoryCommonUtils {

    /**
     * Creates a locale based on a String of the form language_country_variant,
     * either of the last two parts can be omitted.
     *
     * @param localeString The locale string
     * @return The locale
     */
    public static Locale localeForString(String localeString) {
        if (localeString == null || localeString.isEmpty()) {
            return null;
        }
        Locale locale;
        String[] localeParts = localeString.split("_");
        switch (localeParts.length) {
            case 1:
                locale = new Locale(localeParts[0]);
                break;
            case 2:
                locale = new Locale(localeParts[0], localeParts[1]);
                break;
            default:
                // Use default for 3 and above, merge the parts back and put them all in the varient
                StringBuilder varient = new StringBuilder(localeParts[2]);
                for (int i = 3; i < localeParts.length; i++) {
                    varient.append("_");
                    varient.append(localeParts[i]);
                }
                locale = new Locale(localeParts[0], localeParts[1], varient.toString());
                break;
        }
        return locale;
    }
}

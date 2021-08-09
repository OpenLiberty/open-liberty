/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.sources;

import java.util.regex.Pattern;

import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;
import com.ibm.ws.microprofile.config.sources.EnvConfigSource;

/**
 * Enhancements to the EnvConfigSource class for MP Config 1.3.
 */
public class EnvConfig13Source extends EnvConfigSource {

    private static Pattern p = null;

    static {
        p = Pattern.compile(ConfigConstants.CONFIG13_ALLOWABLE_CHARS_IN_ENV_VAR_SOURCE);
    }

    @Override
    public String toString() {
        return "Environment Variables Config 1.3 Source";
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        String theValue = null;
        String theModifiedPropertyName = null;

        // First pass
        theValue = super.getValue(propertyName);

        // Second pass, replace Non-Alphanumeric characters
        if (theValue == null) {
            theModifiedPropertyName = replaceNonAlpha(propertyName);
            theValue = super.getValue(theModifiedPropertyName);
        }

        // Third pass, convert to upper case and search
        if (theValue == null && theModifiedPropertyName != null) {
            theModifiedPropertyName = theModifiedPropertyName.toUpperCase();
            theValue = super.getValue(theModifiedPropertyName);
        }

        return theValue;
    }

    /**
     * Replace non-alphanumeric characters in a string with underscores.
     *
     * @param name
     * @return modified name
     */
    private String replaceNonAlpha(String name) {
        String modifiedName = null;
        if (p != null)
            modifiedName = p.matcher(name).replaceAll("_");
        return modifiedName;
    }
}

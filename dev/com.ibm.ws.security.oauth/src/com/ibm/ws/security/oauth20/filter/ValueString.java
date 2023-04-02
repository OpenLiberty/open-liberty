/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package com.ibm.ws.security.oauth20.filter;

public class ValueString implements IValue {
    String value;

    /**
     * 
     */
    public ValueString(String value) {
        super();
        this.value = value;
    }

    public boolean equals(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;

        return value.equals(((ValueString) str).value);
    }

    public boolean greaterThan(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;

        return (value.compareTo(((ValueString) str).value) > 0);
    }

    public boolean lessThan(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;
        return (value.compareTo(((ValueString) str).value) < 0);
    }

    /**
     * Determine if the input string (str) contains this.
     * 
     */
    public boolean containedBy(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;
        return (((ValueString) str).value.indexOf(value) != -1);
    }

    public String toString() {
        return value;
    }
}

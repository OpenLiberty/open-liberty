/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

public class ValueString implements IValue {
    String value;

    /**
     * 
     */
    public ValueString(String value) {
        super();
        this.value = value;
    }

    @Override
    public boolean equals(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;

        return value.equals(((ValueString) str).value);
    }

    @Override
    public boolean greaterThan(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;

        return (value.compareTo(((ValueString) str).value) > 0);
    }

    @Override
    public boolean lessThan(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;
        return (value.compareTo(((ValueString) str).value) < 0);
    }

    /**
     * Determine if the input string (str) contains this.
     * 
     */
    @Override
    public boolean containedBy(IValue str) {
        if (str.getClass() != ValueString.class)
            return false;
        return (((ValueString) str).value.indexOf(value) != -1);
    }

    @Override
    public String toString() {
        return value;
    }
}

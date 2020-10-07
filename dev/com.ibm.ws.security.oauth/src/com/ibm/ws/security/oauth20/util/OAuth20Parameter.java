/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OAuth20Parameter implements Serializable {
    private static final long serialVersionUID = 4583937066373430973L;

    protected String name;
    protected String type;
    // Mark if user can edit, in addition to consuming product
    protected String customizable;
    protected List<String> values;

    public OAuth20Parameter(String parameterName, String parameterType,
            String customizable) {
        name = parameterName;
        type = parameterType;
        this.customizable = customizable;
        values = new ArrayList<String>();
    }

    public OAuth20Parameter(OAuth20Parameter paramIn) {
        name = paramIn.getName();
        type = paramIn.getType();
        customizable = paramIn.getCustomizable();
        values = paramIn.getValues();
    }

    public void addValue(String value) {
        values.add(value);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getCustomizable() {
        return customizable;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public String toString() {
        String ret = "";
        ret += "name: " + name + ", ";
        ret += "type: " + type + ", ";
        ret += "customizable: " + customizable + ", ";
        ret += "values: ";
        for (String value : values) {
            ret += value + ", ";
        }
        return ret;
    }
}
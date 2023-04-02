/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.config.xml.internal.variables;

import java.util.regex.Pattern;

import com.ibm.ws.config.xml.LibertyVariable;

public abstract class AbstractLibertyVariable implements LibertyVariable {

    final Pattern obscuredValuePattern = Pattern.compile("(\\{aes\\}|\\{xor\\}).*");
    final String OBSCURED_VALUE = "*****";
    final String ENCRYPTION_KEY = "wlp.password.encryption.key";

    private String getObscuredValue(String value) {
        if (isSensitive())
            return OBSCURED_VALUE;

        if (ENCRYPTION_KEY.equals(getName()))
            return OBSCURED_VALUE;

        if (value == null)
            return null;

        if (obscuredValuePattern.matcher(value).matches())
            return OBSCURED_VALUE;

        return value;
    }

    @Override
    public String getObscuredValue() {
        return getObscuredValue(getValue());
    }

    @Override
    public String getObscuredDefaultValue() {
        return getObscuredValue(getDefaultValue());
    }

}

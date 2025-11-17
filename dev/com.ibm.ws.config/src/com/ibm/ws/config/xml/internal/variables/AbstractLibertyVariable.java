/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.LibertyVariable;

public abstract class AbstractLibertyVariable implements LibertyVariable {

    final Pattern obscuredValuePattern = Pattern.compile("(\\{aes\\}|\\{xor\\}).*");
    final String OBSCURED_VALUE = "*****";
    final Pattern encryptionKeyPattern = Pattern.compile("wlp.password.encryption.key|wlp.aes.encryption.key");

    @Trivial
    private String getObscuredValue(String value) {
        if (isSensitive())
            return OBSCURED_VALUE;

        if (encryptionKeyPattern.matcher(getName()).matches())
            return OBSCURED_VALUE;

        if (value == null)
            return null;

        if (obscuredValuePattern.matcher(value).matches())
            return OBSCURED_VALUE;

        return value;
    }

    // Prevent 'calling traceable methods' warning from ConfigVariable.toString
    @Trivial
    @Override
    public String getObscuredValue() {
        return getObscuredValue(getValue());
    }

    // Prevent 'calling traceable methods' warning from ConfigVariable.toString
    @Trivial
    @Override
    public String getObscuredDefaultValue() {
        return getObscuredValue(getDefaultValue());
    }

}

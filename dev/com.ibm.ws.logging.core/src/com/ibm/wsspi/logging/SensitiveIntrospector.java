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
package com.ibm.wsspi.logging;

import java.util.regex.Pattern;

import com.ibm.websphere.ras.annotation.Sensitive;

public abstract class SensitiveIntrospector implements Introspector {

    final Pattern obscuredValuePattern = Pattern.compile("(\\{aes\\}|\\{xor\\}).*");
    final String OBSCURED_VALUE = "*****";
    final Pattern encryptionKeyPattern = Pattern.compile("wlp.password.encryption.key|wlp.aes.encryption.key");

    @Sensitive
    protected String getObscuredValue(String name, Object o) {
        if (encryptionKeyPattern.matcher(name).matches())
            return OBSCURED_VALUE;

        String value = String.valueOf(o);
        if (obscuredValuePattern.matcher(value).matches())
            return OBSCURED_VALUE;
        return value;
    }

}

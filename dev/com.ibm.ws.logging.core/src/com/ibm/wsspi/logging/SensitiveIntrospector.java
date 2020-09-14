/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    final String ENCRYPTION_KEY = "wlp.password.encryption.key";
    
    @Sensitive
    protected String getObscuredValue(String name, Object o) {
    	if ( ENCRYPTION_KEY.equals(name))
    		return OBSCURED_VALUE;
    	
        String value = String.valueOf(o);
        if (obscuredValuePattern.matcher(value).matches())
            return OBSCURED_VALUE;
        return value;
    }
    

}

/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.util;

import com.ibm.ws.crypto.util.AESKeyManager.KeyStringResolver;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

/**
 *
 */
public class VariableResolver implements KeyStringResolver {
    private VariableRegistry registry;

    public void setVariableRegistry(VariableRegistry vr) {
        registry = vr;
        AESKeyManager.setKeyStringResolver(this);
    }

    public void unsetVariableRegistry(VariableRegistry vr) {
        AESKeyManager.setKeyStringResolver(null);
    }

    @Override
    public char[] getKey(String val) {
        return registry.resolveString(val).toCharArray();
    }
}
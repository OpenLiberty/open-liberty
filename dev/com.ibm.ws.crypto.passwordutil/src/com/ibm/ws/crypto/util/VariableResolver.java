/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.crypto.util;

import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.security.crypto.KeyStringResolver;

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
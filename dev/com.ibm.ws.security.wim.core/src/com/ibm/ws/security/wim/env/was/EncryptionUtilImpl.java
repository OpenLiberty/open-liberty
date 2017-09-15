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
package com.ibm.ws.security.wim.env.was;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.security.wim.env.IEncryptionUtil;

/**
 * Encryption utility for WAS environment
 */
public class EncryptionUtilImpl implements IEncryptionUtil {

    public EncryptionUtilImpl() {}

    @Override
    public String decode(String encodedValue) {
        return PasswordUtil.passwordDecode(encodedValue);
    }

    @Override
    public String encode(String decodedValue) {
        return PasswordUtil.passwordEncode(decodedValue);
    }
}

/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

import java.security.Key;

import com.ibm.ws.crypto.ltpakeyutil.AesLTPAKeyEncryptor;

/**
 * Package-local alias kept for backward compatibility.
 * All logic lives in {@link AesLTPAKeyEncryptor}.
 */
class AesKeyEncryptor extends AesLTPAKeyEncryptor {

    AesKeyEncryptor(Key aesKey) {
        super(aesKey);
    }
}

/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa;

import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;

/**
 *
 */
public class LTPAKey {
    String filename = null;
    private byte[] secretKey = null;
    private byte[] privateKey = null;
    private byte[] publicKey = null;
    private LTPAPrivateKey ltpaPrivateKey = null;
    private LTPAPublicKey ltpaPublicKey = null;
    String notUseAfterDate = null;

    LTPAKey(String filename, byte[] secretKey, byte[] privateKey, byte[] publicKey, String notUseAfterDate) {
        this.filename = filename;
        this.secretKey = secretKey;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.notUseAfterDate = notUseAfterDate;
        ltpaPrivateKey = new LTPAPrivateKey(privateKey);
        ltpaPublicKey = new LTPAPublicKey(publicKey);
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public byte[] getprivateKey() {
        return privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public LTPAPrivateKey getLTPAPrivateKey() {
        return ltpaPrivateKey;
    }

    public LTPAPublicKey getLTPAPublicKey() {
        return ltpaPublicKey;
    }

    public boolean isNotUseAfterData() {
        return LTPAKeyInfoManager.isNotUseAfterDate(filename, notUseAfterDate);
    }
}

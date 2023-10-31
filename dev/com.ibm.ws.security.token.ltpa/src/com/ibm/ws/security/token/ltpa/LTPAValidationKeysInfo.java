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

import java.time.OffsetDateTime;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPrivateKey;
import com.ibm.ws.crypto.ltpakeyutil.LTPAPublicKey;

/**
 *
 */
public class LTPAValidationKeysInfo {
    private static final TraceComponent tc = Tr.register(LTPAValidationKeysInfo.class);

    String filename = null;
    private byte[] secretKey = null;
    private byte[] privateKey = null;
    private byte[] publicKey = null;
    private LTPAPrivateKey ltpaPrivateKey = null;
    private LTPAPublicKey ltpaPublicKey = null;
    OffsetDateTime validUntilDateOdt = null;

    LTPAValidationKeysInfo(String filename, byte[] secretKey, byte[] privateKey, byte[] publicKey, OffsetDateTime validUntilDateOdt) {
        this.filename = filename;
        this.secretKey = secretKey;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.validUntilDateOdt = validUntilDateOdt;
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

    // Check if the validUntilDate0dt has already passed the current time.
    // If so, then they key is expired, and will return true with a warning message.
    // Otherwise, the key is valid and will return false.
    // If the validUntilDateOdt is null, then the key is forever valid and will return false.
    public boolean isValidUntilDateExpired() {
        if (validUntilDateOdt == null) // If not specified, then it is forever valid.
            return false;

        OffsetDateTime currentTime = OffsetDateTime.now(validUntilDateOdt.getOffset());

        if (validUntilDateOdt.isBefore(currentTime)) {
            Tr.warning(tc, "LTPA_VALIDATION_KEYS_VALID_UNTIL_DATE_IS_IN_THE_PAST", validUntilDateOdt, filename);
            return true;
        } else {
            return false;
        }
    }
}

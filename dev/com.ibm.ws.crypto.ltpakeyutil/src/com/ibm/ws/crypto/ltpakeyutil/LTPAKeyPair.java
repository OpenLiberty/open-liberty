/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.ltpakeyutil;

/**
 * Represents an LTPA key pair based on RSA. Encoding format is non-standard. Understood only by LTPA
 * specific classes
 */
public final class LTPAKeyPair {

    private final LTPAPublicKey publicKey;
    private final LTPAPrivateKey privateKey;

    LTPAKeyPair(LTPAPublicKey pubKey, LTPAPrivateKey privKey) {
        publicKey = pubKey;
        privateKey = privKey;
    }

    public LTPAPrivateKey getPrivate() {
        return privateKey;
    }

    public LTPAPublicKey getPublic() {
        return publicKey;
    }
}

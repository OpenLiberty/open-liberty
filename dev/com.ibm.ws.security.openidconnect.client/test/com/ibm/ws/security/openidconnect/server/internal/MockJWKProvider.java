/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import com.ibm.ws.security.common.jwk.impl.JWKProvider;
import com.ibm.ws.security.common.jwk.impl.Jose4jEllipticCurveJWK;
import com.ibm.ws.security.common.jwk.interfaces.JWK;

/**
 *
 */
public class MockJWKProvider extends JWKProvider {
    // private static final TraceComponent tc = Tr.register(MockJWKProvider.class);

    public MockJWKProvider(int keysize, String alg, long rotation) {
        super(keysize, alg, rotation);
    }

    String alg = "ES256";

    @Override
    protected JWK generateJWK(String alg1, int size) {
        JWK jwk = Jose4jEllipticCurveJWK.getInstance(this.alg, null);
        System.out.println("generateJWK:" + jwk);
        return jwk;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

}

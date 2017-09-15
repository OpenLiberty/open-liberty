/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.security.common.jwk.interfaces.JSONWebKey;
import com.ibm.ws.security.common.jwk.interfaces.JWK;

/**
 *
 */
public class JWKSet {
    protected List<JWK> jwks = Collections.synchronizedList(new ArrayList<JWK>());

    private final long Stale = 10 * 60 * 1000; //10 minutes

    public JWKSet() {

    }

    public List<JWK> getJWKs() {
        return jwks;
    }

    public synchronized void addJWK(JWK jwk) {

        //clean up first
        long current = (new Date()).getTime();
        List<JWK> jwksToBeRemoved = new ArrayList<JWK>();
        Iterator<JWK> it = jwks.iterator();
        while (it.hasNext()) {
            JWK oldJwk = it.next();
            if (current - oldJwk.getCreated() > Stale) {
                jwksToBeRemoved.add(oldJwk);
            }
        }

        Iterator<JWK> itremoved = jwksToBeRemoved.iterator();
        while (itremoved.hasNext()) {
            JSONWebKey removed = itremoved.next();
            jwks.remove(removed);
        }

        jwks.add(0, jwk);
    }

    public JSONWebKey getJWKByKid(String id) {
        if (id == null) {
            if (jwks.size() == 1) {
                return jwks.get(0);
            } else {
                return null;
            }
        }

        Iterator<JWK> it = jwks.iterator();
        JSONWebKey jwk = null;
        while (it.hasNext()) {
            jwk = it.next();
            if (id.equals(jwk.getKeyID())) {
                return jwk;
            }
        }

        return null;
    }

    public PublicKey getPublicKeyByKid(String id) {
        JSONWebKey jwk = getJWKByKid(id);
        if (jwk != null) {
            return jwk.getPublicKey();
        }
        return null;
    }

    public JSONWebKey getJWKByx5t(String id) {
        Iterator<JWK> it = jwks.iterator();
        while (it.hasNext()) {
            JSONWebKey jwk = it.next();
            if (id.equals(jwk.getKeyX5t())) {
                return jwk;
            }
        }
        return null;
    }

    public PublicKey getPublicKeyByx5t(String id) {
        JSONWebKey jwk = getJWKByx5t(id);
        if (jwk != null) {
            return jwk.getPublicKey();
        }
        return null;
    }

    public void remove(JSONWebKey jwk) {
        jwks.remove(jwk);
    }

    public void remove(int i) {
        jwks.remove(i);
    }

    public void add(JWK jwk) {
        jwks.add(jwk);
    }

    public void add(int i, JWK jwk) {
        jwks.add(i, jwk);
    }
}

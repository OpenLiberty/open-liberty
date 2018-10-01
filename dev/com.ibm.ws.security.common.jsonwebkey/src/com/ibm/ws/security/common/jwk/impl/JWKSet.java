/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

public class JWKSet {
    private static final TraceComponent tc = Tr.register(JWKSet.class);

    protected List<JWK> jwks = Collections.synchronizedList(new ArrayList<JWK>());

    private Map<String, Set<JWK>> jwksBySetId = Collections.synchronizedMap(new HashMap<String, Set<JWK>>());

    private final long Stale = 10 * 60 * 1000; //10 minutes

    public JWKSet() {

    }

    public List<JWK> getJWKs() {
        return null;// jwks;
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

        //return null;
        return getPEMKey(); // temporary
    }

    private JSONWebKey getJWKByKidInCollection(String kid, Collection<JWK> jwkCollection) {
        if (kid == null) {
            if (jwkCollection.size() == 1) {
                return (JSONWebKey) jwkCollection.toArray()[0];
            } else {
                return null;
            }
        }

        Iterator<JWK> it = jwkCollection.iterator();
        JSONWebKey jwk = null;
        while (it.hasNext()) {
            jwk = it.next();
            if (kid.equals(jwk.getKeyID())) {
                return jwk;
            }
        }

        // If kid is not included in token, and if the subset of keys contains one key, then return the single key
        if (jwkCollection.size() == 1) {
            return (JSONWebKey) jwkCollection.toArray()[0];
        }

        return null;
    }

    private JSONWebKey getJWKByx5tInCollection(String x5t, Collection<JWK> jwkCollection) {
        Iterator<JWK> it = jwkCollection.iterator();
        JSONWebKey jwk = null;
        while (it.hasNext()) {
            jwk = it.next();
            if (x5t.equals(jwk.getKeyX5t())) {
                return jwk;
            }
        }
        return null;
    }

    private JSONWebKey getJWKByUseInCollection(String use, Collection<JWK> jwkCollection) {
        if (use == null) {
            return null;
        }
        JSONWebKey key = null;
        Iterator<JWK> it = jwkCollection.iterator();
        JSONWebKey jwk = null;
        while (it.hasNext()) {
            jwk = it.next();
            String thisKeyUse = jwk.getKeyUse();
            if (key != null && use.equals(thisKeyUse)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found more than one key matching the signature algorithm and use, so do not know which key to use");
                }
                return null;
            }
            if (use.equals(thisKeyUse)) {
                key = jwk;
            }
        }
        return key;
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

    public PublicKey getPublicKeyBySetId(String setId) {
        PublicKey publicKey = null;
        Set<JWK> jwks = jwksBySetId.get(setId);

        if (jwks != null && jwks.size() == 1) {
            JSONWebKey jwk = jwks.iterator().next();
            publicKey = jwk.getPublicKey();
        }

        return publicKey;
    }

    public PublicKey getPublicKeyBySetIdAndKid(String setId, String kid) {
        PublicKey publicKey = null;
        Set<JWK> jwks = jwksBySetId.get(setId);

        if (jwks != null) {
            JSONWebKey jwk = getJWKByKidInCollection(kid, jwks);
            if (jwk != null) {
                publicKey = jwk.getPublicKey();
            }
        }

        return publicKey;
    }

    public PublicKey getPublicKeyBySetIdAndx5t(String setId, String x5t) {
        PublicKey publicKey = null;
        Set<JWK> jwks = jwksBySetId.get(setId);

        if (jwks != null) {
            JSONWebKey jwk = getJWKByx5tInCollection(x5t, jwks);
            if (jwk != null) {
                publicKey = jwk.getPublicKey();
            }
        }

        return publicKey;
    }

    public PublicKey getPublicKeyBySetIdAndUse(String setId, String use) {
        PublicKey publicKey = null;
        Set<JWK> jwks = jwksBySetId.get(setId);
        if (jwks != null) {
            JSONWebKey jwk = getJWKByUseInCollection(use, jwks);
            if (jwk != null) {
                publicKey = jwk.getPublicKey();
            }
        }
        return publicKey;
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

    // TODO: Add cleanup
    public void add(String setId, JWK jwk) {
        if (jwksBySetId.containsKey(setId) == false) {
            jwksBySetId.put(setId, Collections.synchronizedSet(new HashSet<JWK>()));
        }

        jwksBySetId.get(setId).add(jwk);
    }

    // the code below here is temporary until cache enhancements for mpjwt-1.1 can be completed.
    JWK theOnePEMJwk = null;

    public void add(JWK jwk, boolean isFromPEM) {
        jwks.add(jwk);
        if (isFromPEM) {
            theOnePEMJwk = jwk;
        }
    }

    protected JWK getPEMKey() {
        if (theOnePEMJwk != null) {
            return theOnePEMJwk;
        }
        return null;
    }
    // end temporary code.

}

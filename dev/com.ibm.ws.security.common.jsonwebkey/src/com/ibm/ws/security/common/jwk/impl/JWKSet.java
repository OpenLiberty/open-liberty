/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
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
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;

public class JWKSet {
    private static final TraceComponent tc = Tr.register(JWKSet.class);

    private Map<String, Map<String, JWK>> pemJwksBySetId = Collections.synchronizedMap(new HashMap<String, Map<String, JWK>>());

    private Map<String, Set<JWK>> jwksBySetId = Collections.synchronizedMap(new HashMap<String, Set<JWK>>());

    private final long Stale = 10 * 60 * 1000; //10 minutes

    public JWKSet() {

    }

    public List<JWK> getJWKs() {
        return null;// jwks;
    }

    synchronized void removeStaleEntries(Collection<JWK> collection) {
        long current = (new Date()).getTime();
        List<JWK> jwksToBeRemoved = new ArrayList<JWK>();
        Iterator<JWK> it = collection.iterator();
        while (it.hasNext()) {
            JWK oldJwk = it.next();
            if (current - oldJwk.getCreated() > Stale) {
                jwksToBeRemoved.add(oldJwk);
            }
        }

        Iterator<JWK> itremoved = jwksToBeRemoved.iterator();
        while (itremoved.hasNext()) {
            JSONWebKey removed = itremoved.next();
            collection.remove(removed);
        }
    }

    synchronized void removeStalePemEntries(Map<String, JWK> pemsForLocation) {
        long currentTime = (new Date()).getTime();
        List<String> pemJwksToRemove = new ArrayList<String>();
        for (Entry<String, JWK> entry : pemsForLocation.entrySet()) {
            JWK oldJwk = entry.getValue();
            if (currentTime - oldJwk.getCreated() > Stale) {
                pemJwksToRemove.add(entry.getKey());
            }
        }
        for (String pemKeyToRemove : pemJwksToRemove) {
            pemsForLocation.remove(pemKeyToRemove);
        }
    }

    private JSONWebKey getJWKByKidInCollection(String kid, Collection<JWK> jwkCollection) {
        if (kid == null) {
            if (jwkCollection.size() == 1) {
                // If kid is not included in token, and if the subset of keys contains one key, then return the single key
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

    private JSONWebKey getPEMKey(String location, String keyTextOrKid) {
        if (keyTextOrKid == null) {
            return null;
        }
        if (location == null) {
            location = keyTextOrKid;
        }
        Map<String, JWK> pemsForLocation = pemJwksBySetId.get(location);
        if (pemsForLocation != null) {
            return pemsForLocation.get(keyTextOrKid);
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

    public PublicKey getPublicKeyBySetIdAndKeyText(String setId, String keyText) {
        PublicKey publicKey = null;
        JSONWebKey jwk = getPEMKey(setId, keyText);
        if (jwk != null) {
            publicKey = jwk.getPublicKey();
        }
        return publicKey;
    }

    public void add(String setId, JWK jwk) {
        if (jwksBySetId.containsKey(setId) == false) {
            jwksBySetId.put(setId, Collections.synchronizedSet(new HashSet<JWK>()));
        } else {
            removeStaleEntries(jwksBySetId.get(setId));
        }
        jwksBySetId.get(setId).add(jwk);
    }

    public void addPemKey(String location, String keyTextOrKid, JWK jwk) {
        if (location == null) {
            location = keyTextOrKid;
        }
        Map<String, JWK> pemsForLocation = pemJwksBySetId.get(location);
        if (pemsForLocation == null) {
            pemsForLocation = new HashMap<String, JWK>();
        } else {
            removeStalePemEntries(pemsForLocation);
        }
        pemsForLocation.put(keyTextOrKid, jwk);
        pemJwksBySetId.put(location, pemsForLocation);
    }

}

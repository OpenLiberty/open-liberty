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
package com.ibm.ws.security.jwt.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.utils.JwtUtils;

public class ClaimsImpl implements Claims {
    // private final String issuer = null;
    private static final TraceComponent tc = Tr.register(ClaimsImpl.class,
            TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private final Map<String, Object> claimsMap = new ConcurrentHashMap<String, Object>();

    public ClaimsImpl() {

    }

    @Override
    public Object put(String key, Object value) {

        return claimsMap.put(key, value);
        // return this.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        // TODO Auto-generated method stub
        return claimsMap.remove(key);
        // return this.remove(key);
    }

    @Override
    @FFDCIgnore(NullPointerException.class)
    public void putAll(Map<? extends String, ? extends Object> m) {
        // TODO Auto-generated method stub
        try {
            claimsMap.putAll(m);
        } catch (NullPointerException npe) {
            //Tr.warning(tc, "JWT_CLAIMSMAP_NULL_KEY_OR_VALUE");
        }

        // this.putAll(m);

    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        claimsMap.clear();
        // this.clear();
    }

    @Override
    public Set<String> keySet() {
        // TODO Auto-generated method stub
        return claimsMap.keySet();
        // return this.keySet();
    }

    @Override
    public Collection<Object> values() {
        // TODO Auto-generated method stub
        return claimsMap.values();
        // return this.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        // TODO Auto-generated method stub
        return claimsMap.entrySet();
        // return this.entrySet();
    }

    @Override
    public String getIssuer() {
        // TODO Auto-generated method stub
        return (String) claimsMap.get(Claims.ISSUER);
        // return (String) this.get(Claims.ISSUER);
    }

    @Override
    public String getSubject() {
        // TODO Auto-generated method stub
        return (String) claimsMap.get(Claims.SUBJECT);
        // return (String) this.get(Claims.SUBJECT);
    }

    @Override
    public List<String> getAudience() {
        // TODO Auto-generated method stub
        List<String> audiences = null;
        Object obj = claimsMap.get(Claims.AUDIENCE);
        if (obj instanceof String) {
            audiences = new ArrayList<String>();
            audiences.add((String) obj);
        }
        else if (obj instanceof List) {
            audiences = (List) obj;
        }
        return audiences;
        // return (List<String>) this.get(Claims.AUDIENCE);
    }

    @Override
    public long getExpiration() {
        // TODO Auto-generated method stub
        Long exp = (Long) claimsMap.get(Claims.EXPIRATION);
        // Long exp = (Long) this.get(Claims.EXPIRATION);
        if (exp != null) {
            return exp.longValue();
        } else {
            return -1;
        }

    }

    @Override
    public long getNotBefore() {
        // TODO Auto-generated method stub
        Long nbf = (Long) claimsMap.get(Claims.NOT_BEFORE);
        // Long exp = (Long) this.get(Claims.EXPIRATION);
        if (nbf != null) {
            return nbf.longValue();
        } else {
            return -1;
        }
    }

    @Override
    public long getIssuedAt() {
        // TODO Auto-generated method stub
        Long iat = (Long) claimsMap.get(Claims.ISSUED_AT);
        // Long exp = (Long) this.get(Claims.EXPIRATION);
        if (iat != null) {
            return iat.longValue();
        } else {
            return -1;
        }
    }

    @Override
    public String getJwtId() {
        // TODO Auto-generated method stub
        return (String) claimsMap.get(Claims.ID);
        // return (String) this.get(Claims.ID);
    }

    @Override
    public String getAuthorizedParty() {
        return (String) claimsMap.get(Claims.AZP);
    }

    @Override
    public <T> T getClaim(String claimName, Class<T> requiredType) {
        // TODO Auto-generated method stub
        try {
            if (claimsMap.get(claimName) != null) {
                return (T) claimsMap.get(claimName);
            }
        } catch (ClassCastException cce) {

        }

        return null;
    }

    @Override
    public Map<String, Object> getAllClaims() {
        // TODO Auto-generated method stub
        return claimsMap;
        // return this;
    }

    @Override
    public String toJsonString() {
        // TODO Auto-generated method stub
        return JwtUtils.toJson(claimsMap);

    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return claimsMap.size();
        // return this.size();
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return claimsMap.isEmpty();
        // return this.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        // TODO Auto-generated method stub
        return claimsMap.containsKey(key);
        // return this.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        // TODO Auto-generated method stub
        return claimsMap.containsValue(value);
        // return this.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        // TODO Auto-generated method stub
        return claimsMap.get(key);
        // return this.get(key);
    }

}

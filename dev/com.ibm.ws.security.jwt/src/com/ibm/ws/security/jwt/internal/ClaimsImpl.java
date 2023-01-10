/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.jwt.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.jwt.utils.JwtUtils;

public class ClaimsImpl implements Claims, Serializable {

	private static final long serialVersionUID = 1L;

	// private final String issuer = null;
	private static final TraceComponent tc = Tr.register(ClaimsImpl.class, TraceConstants.TRACE_GROUP,
			TraceConstants.MESSAGE_BUNDLE);
	private final Map<String, Object> claimsMap = Collections.synchronizedMap(new HashMap<>());

	public ClaimsImpl() {
	}

	@Override
	public Object put(String key, Object value) {

		return claimsMap.put(key, value);
		// return this.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return claimsMap.remove(key);
		// return this.remove(key);
	}

	@Override
	@FFDCIgnore(NullPointerException.class)
	public void putAll(Map<? extends String, ? extends Object> m) {
		try {
			claimsMap.putAll(m);
		} catch (NullPointerException npe) {
			// Tr.warning(tc, "JWT_CLAIMSMAP_NULL_KEY_OR_VALUE");
		}

		// this.putAll(m);

	}

	@Override
	public void clear() {
		claimsMap.clear();
		// this.clear();
	}

	@Override
	public Set<String> keySet() {
		return claimsMap.keySet();
		// return this.keySet();
	}

	@Override
	public Collection<Object> values() {
		return claimsMap.values();
		// return this.values();
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return claimsMap.entrySet();
		// return this.entrySet();
	}

	@Override
	public String getIssuer() {
		return (String) claimsMap.get(Claims.ISSUER);
		// return (String) this.get(Claims.ISSUER);
	}

	@Override
	public String getSubject() {
		return (String) claimsMap.get(Claims.SUBJECT);
		// return (String) this.get(Claims.SUBJECT);
	}

	@Override
	public List<String> getAudience() {
		List<String> audiences = null;
		Object obj = claimsMap.get(Claims.AUDIENCE);
		if (obj instanceof String) {
			audiences = new ArrayList<String>();
			audiences.add((String) obj);
		} else if (obj instanceof List) {
			audiences = (List) obj;
		}
		return audiences;
		// return (List<String>) this.get(Claims.AUDIENCE);
	}

	@Override
	public long getExpiration() {
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
		return (String) claimsMap.get(Claims.ID);
		// return (String) this.get(Claims.ID);
	}

	@Override
	public String getAuthorizedParty() {
		return (String) claimsMap.get(Claims.AZP);
	}

	@Override
	public <T> T getClaim(String claimName, Class<T> requiredType) {
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
		return claimsMap;
		// return this;
	}

	@Override
	public String toJsonString() {
		return JwtUtils.toJson(claimsMap);

	}

	@Override
	public int size() {
		return claimsMap.size();
		// return this.size();
	}

	@Override
	public boolean isEmpty() {
		return claimsMap.isEmpty();
		// return this.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return claimsMap.containsKey(key);
		// return this.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return claimsMap.containsValue(value);
		// return this.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return claimsMap.get(key);
		// return this.get(key);
	}
}

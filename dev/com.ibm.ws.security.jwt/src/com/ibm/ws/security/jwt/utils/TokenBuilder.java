/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.ws.security.jwt.config.JwtConfig;

/**
 * Convenience class to build an MP-JWT token using the authenticated user's id
 * and groups, and other parameters from the specified builder configuration.
 *
 */
public class TokenBuilder {
	private static TraceComponent tc = Tr.register(TokenBuilder.class);
	protected static final String USER_CLAIM = "upn"; // mp-jwt format
	protected static final String GROUP_CLAIM = "groups"; // mp-jwt format
	protected static final String CCK_CLAIM = "sid"; // custom cache key
	protected static final String APR_CLAIM = "apr"; // custom auth provider
	protected static final String REALM_CLAIM = "realm"; // realm
	private final static String GROUP_PREFIX = "group:";

	/**
	 * create an MP-JWT token using the Builder API. Assumes the user is already
	 * authenticated.
	 *
	 * @param jwtConfig
	 *            - a builder configuration from server.xml
	 * @return the token string, or null if a mandatory param was null or empty.
	 */
	public String createTokenString(JwtConfig config) {
		return createTokenString(config.getId());
	}

	/**
	 * create an MP-JWT token using the builder API. Assumes the user is already
	 * authenticated.
	 *
	 * @param builderConfigId
	 *            - the id of the builder element in server.xml
	 * @return the token string, or null if a mandatory param was null or empty.
	 */
	public String createTokenString(String builderConfigId) {
		try {
			return createTokenString(builderConfigId, WSSubject.getRunAsSubject(), null, null);

			// JwtBuilder builder = JwtBuilder.create(builderConfigId);
			//
			// // all the "normal" stuff like issuer, aud, etc. is handled
			// // by the builder, we only need to add the mp-jwt things
			// // that the builder is not already aware of.
			// String user = getUserName();
			// builder.subject(user);
			// builder.claim(USER_CLAIM, user);
			//
			// ArrayList<String> groups = getGroups();
			// if (isValidList(groups)) {
			// builder.claim(GROUP_CLAIM, groups);
			// }
			//
			// return builder.buildJwt().compact();

		} catch (Exception e) {
			// ffdc
			return null;
		}

	}

	@Trivial
	private boolean isValidList(List<String> in) {
		return (in != null && in.size() > 0);
	}

	/**
	 * get the username from the WSSubject
	 *
	 * @return the user name, UNAUTHENTICATED, or null if something went wrong.
	 */
	public String getUserName(Subject subject) {
		// Subject subject = null;
		try {
			// subject = WSSubject.getRunAsSubject();
			WSCredential wsCred = getWSCredential(subject);
			if (wsCred == null) {
				wsCred = getPrivateWSCredential(subject);
			}
			return wsCred != null ? wsCred.getSecurityName() : null;
		} catch (Exception e) {
			// ffdc
			return null;
		}
	}

	private WSCredential getPrivateWSCredential(Subject subject) {
		WSCredential wsCredential = null;
		Set<WSCredential> wsCredentials = subject.getPrivateCredentials(WSCredential.class);
		Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
		if (wsCredentialsIterator.hasNext()) {
			wsCredential = wsCredentialsIterator.next();
		}
		return wsCredential;
	}

	private ArrayList<String> getGroups(Subject subject) {
		// Subject subject = null;
		try {
			// subject = WSSubject.getRunAsSubject();
			WSCredential wsCred = getWSCredential(subject);
			if (wsCred == null) {
				wsCred = getPrivateWSCredential(subject);
			}
			if (wsCred != null) {
				@SuppressWarnings("unchecked")
				ArrayList<String> groupIds = wsCred.getGroupIds();
				ArrayList<String> groups = new ArrayList<String>();
				Iterator<String> it = groupIds.listIterator();
				while (it.hasNext()) {
					String origGroup = it.next();
					if (origGroup != null && origGroup.startsWith(GROUP_PREFIX)) {
						int groupIndex = origGroup.indexOf("/");
						if (groupIndex > 0) {
							origGroup = origGroup.substring(groupIndex + 1);
						}
					}
					groups.add(origGroup);
				}
				return groups;
			}

		} catch (Exception e) {
			// ffdc
			return null;
		}
		return null;
	}

	private WSCredential getWSCredential(Subject subject) {
		WSCredential wsCredential = null;
		Set<WSCredential> wsCredentials = subject.getPublicCredentials(WSCredential.class);
		Iterator<WSCredential> wsCredentialsIterator = wsCredentials.iterator();
		if (wsCredentialsIterator.hasNext()) {
			wsCredential = wsCredentialsIterator.next();
		}
		return wsCredential;
	}

	public String createTokenString(String builderId, Subject subject, String customCacheKey, String customAuthProvider)
			throws Exception {
		try {
			JwtBuilder builder = JwtBuilder.create(builderId);

			// all the "normal" stuff like issuer, aud, etc. is handled
			// by the builder, we only need to add the mp-jwt things
			// that the builder is not already aware of.
			String user = getUserName(subject);
			builder.subject(user);
			builder.claim(USER_CLAIM, user);

			String realm = getRealm(subject);
			if (realm != null) {
				builder.claim(REALM_CLAIM, realm);
			}

			ArrayList<String> groups = getGroups(subject);
			if (isValidList(groups)) {
				builder.claim(GROUP_CLAIM, groups);
			}
			if (customCacheKey != null) {
				builder.claim(CCK_CLAIM, customCacheKey);
			}
			if (customAuthProvider != null) {
				builder.claim(APR_CLAIM, customAuthProvider);
			}

			return builder.buildJwt().compact();

		} catch (Exception e) {
			// ffdc
			throw e;
		}
	}

	private String getRealm(Subject subject) {
		try {
			WSCredential wsCred = getWSCredential(subject);
			if (wsCred == null) {
				wsCred = getPrivateWSCredential(subject);
			}
			return wsCred != null ? wsCred.getRealmName() : null;
		} catch (Exception e) {
			// ffdc
			return null;
		}
	}

}

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
	private static final String USER_CLAIM = "usr"; // mp-jwt format
	private static final String GROUP_CLAIM = "groups"; // mp-jwt format
	private final static String GROUP_PREFIX = "group:";

	/**
	 * create an MP-JWT token using the Builder API.
	 *
	 * @param jwtConfig
	 *            - a builder configuration from server.xml
	 * @return the token string, or null if a mandatory param was null or empty.
	 */
	public String createTokenString(JwtConfig config) {
		try {
			JwtBuilder builder = JwtBuilder.create(config.getId());

			// all the "normal" stuff like issuer, aud, etc. is handled
			// by the builder, we only need to add the mp-jwt things
			// that the builder is not already aware of.
			String user = getUserName();
			builder.subject(user);
			builder.claim(USER_CLAIM, user);

			ArrayList<String> groups = getGroups();
			if (isValidList(groups)) {
				builder.claim(GROUP_CLAIM, groups);
			}

			return builder.buildJwt().compact();

		} catch (Exception e) {
			// ffdc
			return null;
		}
	}

	@Trivial
	private boolean isValidList(List<String> in) {
		return (in != null && in.size() > 0);
	}

	private String getUserName() {
		Subject subject = null;
		try {
			subject = WSSubject.getRunAsSubject();
			WSCredential wsCred = getWSCredential(subject);
			return wsCred.getSecurityName();
		} catch (Exception e) {
			// ffdc
			return null;
		}
	}

	private ArrayList<String> getGroups() {
		Subject subject = null;
		try {
			subject = WSSubject.getRunAsSubject();
			WSCredential wsCred = getWSCredential(subject);
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

		} catch (Exception e) {
			// ffdc
			return null;
		}
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

}

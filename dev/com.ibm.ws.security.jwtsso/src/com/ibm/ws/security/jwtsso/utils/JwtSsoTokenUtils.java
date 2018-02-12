/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.utils;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidBuilderException;
import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.InvalidConsumerException;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.security.jwt.JwtBuilder;
import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl;

/**
 * A class to aid in creation and consumption of JWT tokens.
 *
 */
public class JwtSsoTokenUtils {
	JwtBuilder builder = null;
	JwtConsumer consumer = null;

	// q: how does the caller instantiate the builder and consumer? answer:
	// call
	// builder.createFromId
	JwtSsoTokenUtils(JwtBuilder builder, JwtConsumer consumer) {
		this.builder = builder;
		this.consumer = consumer;
	}

	/**
	 * Given a JwtToken string, build a JsonWebToken from it using Consumer API.
	 *
	 * jwtTokenString - an already validated jwt token. No signature checking is
	 * performed here.
	 *
	 */
	public JsonWebToken buildSecurityPrincipalFromToken(@Sensitive String jwtTokenString) {

		JwtToken token = null;
		try {
			token = consumer.createJwt(jwtTokenString);
		} catch (InvalidTokenException | InvalidConsumerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String type = (String) token.getClaims().get(Claims.TOKEN_TYPE);
		String name = (String) token.getClaims().get("upn");
		if (name == null) {
			name = (String) token.getClaims().get("subject");
		}
		return new DefaultJsonWebTokenImpl(jwtTokenString, type, name);
	}

	/**
	 *
	 * Build a JsonWebToken from the current Thread's RunAsSubject?????
	 */
	public JsonWebToken buildTokenFromSecuritySubject()
			throws WSSecurityException, CredentialExpiredException, CredentialDestroyedException {
		Subject subj = WSSubject.getRunAsSubject();
		Set<Principal> principals = subj.getPrincipals();
		for (Principal p : principals) {
			if (p instanceof JsonWebToken) {
				return (JsonWebToken) p;
			}
		}
		Principal prin = null;
		for (Principal p : principals) {
			if (p instanceof WSPrincipal) {
				prin = p;
				break;
			}
		}
		if (prin == null) {
			// todo: ffdc or log exception here.
			return null;
		}

		// we have a WSPrincipal, build the JsonWebToken
		String name = prin.getName();
		ArrayList<String> groups = getGroups(subj);
		ArrayList<String> filteredGroups = filterGroups(groups);
		return buildToken(name, filteredGroups);

	}

	/*
	 * Filter the groups per feature configuration. We do this to keep the token
	 * from becoming too large.
	 *
	 * @param groups
	 * 
	 * @return
	 */
	private ArrayList<String> filterGroups(ArrayList<String> groups) {
		return groups; // TODO: implement me
	}

	private DefaultJsonWebTokenImpl buildToken(String name, ArrayList<String> groups) {
		JwtToken token = null;
		try {
			builder.subject(name);
			builder.claim("upn", name);
			// other attributes come from the jwtbuilder's configuration in
			// server.xml
			// possible attribs are audiences, claims, scope, sigalg, jti
			// TODO: what about the groups???

			token = builder.buildJwt();

		} catch (InvalidClaimException | JwtException | InvalidBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String jwtTokenString = token.compact();
		String type = (String) token.getClaims().get(Claims.TOKEN_TYPE);

		return new DefaultJsonWebTokenImpl(jwtTokenString, type, name);
	}

	private ArrayList<String> getGroups(Subject subject)
			throws CredentialExpiredException, CredentialDestroyedException {
		WSCredential wsCred = getWSCredential(subject);
		@SuppressWarnings("unchecked")
		ArrayList<String> groupIds = wsCred.getGroupIds();
		ArrayList<String> groups = new ArrayList<String>();
		Iterator<String> it = groupIds.listIterator();
		while (it.hasNext()) {
			String origGroup = it.next();
			if (origGroup != null && origGroup.startsWith(JwtSsoConstants.GROUP_PREFIX)) {
				int groupIndex = origGroup.indexOf("/");
				if (groupIndex > 0) {
					origGroup = origGroup.substring(groupIndex + 1);
				}
			}
			groups.add(origGroup);
		}
		return groups;
	}

	// why does this work??
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

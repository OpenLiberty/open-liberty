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
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidConsumerException;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.jwt.utils.TokenBuilder;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl;
import com.ibm.ws.security.mp.jwt.tai.TAIMappingHelper;

/**
 * A class to aid in creation and consumption of JWT tokens.
 *
 */
public class JwtSsoTokenUtils {
	private static TraceComponent tc = Tr.register(JwtSsoTokenUtils.class);
	JwtConsumer consumer = null;
	String builderId = null;
	String consumerId = null;
	boolean isValid = true;

	public JwtSsoTokenUtils() {

	}

	public JwtSsoTokenUtils(String builderId, String consumerId) {
		this.builderId = builderId;
		this.consumerId = consumerId;
		// try {
		// JwtBuilder.create(builderId); // fail fast if id or config is
		// consumer = JwtConsumer.create(consumerId);
		// } catch (InvalidConsumerException | InvalidBuilderException e) {
		// // ffdc
		// isValid = false;
		// }
	}

	/**
	 * return true if the object was constructed successfully
	 *
	 * @return
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * Given a JwtToken string, build a JsonWebToken Principal from it using
	 * Consumer API.
	 *
	 *
	 * @param -
	 *            jwtTokenString - String representation of a JWT token.
	 * @return - a DefaultJsonWebTokenImpl or null if a validation or processing
	 *         error occurs.
	 *
	 */
	public JsonWebToken buildSecurityPrincipalFromToken(String jwtTokenString) {
		// if (!isValid) {
		// return null;
		// }
		JwtToken token = null;
		try {
			token = consumer.createJwt(jwtTokenString);
		} catch (InvalidTokenException | InvalidConsumerException e) {
			// ffdc
			return null;
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
	 * Build a JsonWebToken Principal from the current Thread's RunAsSubject
	 *
	 * @return - a DefaultJsonWebTokenImpl, or null if user wasn't
	 *         authenticated.
	 */
	public JsonWebToken buildTokenFromSecuritySubject() throws Exception {

		Subject subj = WSSubject.getRunAsSubject();
		Set<Principal> principals = subj.getPrincipals();
		// maybe we already have one, check.
		for (Principal p : principals) {
			if (p instanceof JsonWebToken) {
				return (JsonWebToken) p;
			}
		}
		return buildTokenFromSecuritySubject(subj);

	}

	public JsonWebToken buildTokenFromSecuritySubject(Subject subject) throws Exception {
		// TODO Auto-generated method stub
		if (!isValid) {
			return null;
		}
		TokenBuilder tb = new TokenBuilder();
		SubjectUtil subjectUtil = new SubjectUtil(subject);
		String customCacheKey = subjectUtil.getCustomCacheKey();
		String tokenString = tb.createTokenString(builderId, subject, customCacheKey);
		if (tokenString == null) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "returning null because tokenString was null, creation failed.");
			}
			return null;
		}
		String userName = tb.getUserName(subject);

		if (userName == null || userName.compareTo(JwtSsoConstants.UNAUTHENTICATED) == 0) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "returning null because username = " + userName);
			}
			return null;
		}
		return new DefaultJsonWebTokenImpl(tokenString, JwtSsoConstants.TOKEN_TYPE_JWT, userName);

	}

	private boolean checkForClaim(JsonWebToken jwt, String claimName) {
		return (jwt.containsClaim(claimName));

	}

	public Subject handleJwtSsoTokenValidation(String tokenstr) throws Exception {
		Subject tempSubject = null;
		JwtToken jwttoken = recreateJwt(tokenstr);
		if (jwttoken != null) {
			String decodedPayload = null;
			String payload = JsonUtils.getPayload(tokenstr);
			decodedPayload = JsonUtils.decodeFromBase64String(payload);
			if (decodedPayload != null) {
				TAIMappingHelper mappingHelper;
				try {
					mappingHelper = new TAIMappingHelper(decodedPayload);
					mappingHelper.createJwtPrincipalAndPopulateCustomProperties(jwttoken);
					tempSubject = mappingHelper.createSubjectFromCustomProperties(jwttoken);

				} catch (MpJwtProcessingException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			}
		}

		// JsonWebToken principal = buildSecurityPrincipalFromToken(tokenstr);

		return tempSubject;

	}

	public Subject handleJwtSsoTokenValidationWithSubject(Subject subject, String tokenstr) throws Exception {
		JwtToken jwttoken = recreateJwt(tokenstr);
		if (jwttoken != null) {
			TokenBuilder tb = new TokenBuilder();
			String user = tb.getUserName(subject);
			JsonWebToken principal = new DefaultJsonWebTokenImpl(tokenstr, JwtSsoConstants.TOKEN_TYPE_JWT, user);
			subject.getPrincipals().add(principal);
			return subject;
		}

		// JsonWebToken principal = buildSecurityPrincipalFromToken(tokenstr);

		return null;

	}

	private JwtToken recreateJwt(String tokenstr) throws Exception {
		// TODO Auto-generated method stub
		try {
			return consumer.createJwt(tokenstr);
		} catch (InvalidTokenException | InvalidConsumerException e) {
			// ffdc
			throw e;
		}
	}

	public boolean isJwtValid(String tokenstr) {
		// TODO Auto-generated method stub
		try {
			if (recreateJwt(tokenstr) == null) {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String getCustomCacheKeyFromToken(String tokenstr) {
		String customCacheKey = null;
		String payload = decodedPayload(tokenstr);
		if (payload != null) {
			customCacheKey = (String) getClaim(payload, Constants.CCK_CLAIM);
		}
		return customCacheKey;
	}

	private Object getClaim(String payload, String claim) {
		// TODO Auto-generated method stub
		try {
			return JsonUtils.claimFromJsonObject(payload, claim);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return null;
	}

	public String decodedPayload(String tokenstr) {
		if (tokenstr != null) {
			String payload = JsonUtils.getPayload(tokenstr);
			return JsonUtils.decodeFromBase64String(payload);
		}
		return null;
	}
}

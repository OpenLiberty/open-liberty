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
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.jwt.utils.TokenBuilder;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;
import com.ibm.ws.security.mp.jwt.impl.DefaultJsonWebTokenImpl;
import com.ibm.ws.security.mp.jwt.tai.MicroProfileJwtTAI;
import com.ibm.ws.security.mp.jwt.tai.TAIJwtUtils;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * A class to aid in creation and consumption of JWT tokens.
 *
 */
public class JwtSsoTokenUtils {
	private static TraceComponent tc = Tr.register(JwtSsoTokenUtils.class);
	// JwtConsumer consumer = null;
	String builderId = null;
	String consumerId = null;
	boolean isValid = true;
	MicroProfileJwtTAI mpjwttai = null;
	TAIJwtUtils taiJwtUtils = new TAIJwtUtils();

	public JwtSsoTokenUtils() {

	}

	// public JwtSsoTokenUtils(String builderId, String consumerId) {
	// this.builderId = builderId;
	// this.consumerId = consumerId;
	// // try {
	// // JwtBuilder.create(builderId); // fail fast if id or config is
	// // consumer = JwtConsumer.create(consumerId);
	// // } catch (InvalidConsumerException | InvalidBuilderException e) {
	// // // ffdc
	// // isValid = false;
	// // }
	// }

	public JwtSsoTokenUtils(String builder) {
		builderId = builder;
	}

	public JwtSsoTokenUtils(String consumer, AtomicServiceReference<TrustAssociationInterceptor> mpjwttaiserviceref) {
		consumerId = consumer;
		mpjwttai = (MicroProfileJwtTAI) mpjwttaiserviceref.getService();
	}

	/**
	 * return true if the object was constructed successfully
	 *
	 * @return
	 */
	public boolean isValid() {
		return isValid;
	}

	// private JwtConsumer getConsumer() throws InvalidConsumerException {
	// if (consumer == null) {
	// consumer = JwtConsumer.create(consumerId);
	// }
	// return consumer;
	// }

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
		tempSubject = handleValidationUsingMPjwtConsumer(tokenstr);
		// JwtToken jwttoken = recreateJwt(tokenstr);
		// if (jwttoken != null) {
		// String decodedPayload = null;
		// String payload = JsonUtils.getPayload(tokenstr);
		// decodedPayload = JsonUtils.decodeFromBase64String(payload);
		// if (decodedPayload != null) {
		// TAIMappingHelper mappingHelper;
		// try {
		// mappingHelper = new TAIMappingHelper(decodedPayload);
		// mappingHelper.createJwtPrincipalAndPopulateCustomProperties(jwttoken);
		// tempSubject =
		// mappingHelper.createSubjectFromCustomProperties(jwttoken);
		//
		// } catch (MpJwtProcessingException e) {
		// // TODO Auto-generated catch block
		// // e.printStackTrace();
		// }
		// }
		// }

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

		return null;

	}

	private Subject handleValidationUsingMPjwtConsumer(String tokenstr) throws Exception {
		MicroProfileJwtConfig mpjwtConfig = getMpJwtConsumer();
		return mpjwttai.handleMicroProfileJwtValidation(null, null, mpjwtConfig, tokenstr, true).getSubject();
		// return getConsumer().createJwt(tokenstr);

	}

	private JwtToken recreateJwt(String tokenstr) throws Exception {
		JwtToken jwttoken = null;
		try {

			jwttoken = taiJwtUtils.createJwt(tokenstr, getMpJwtConsumer().getUniqueId());
		} catch (MpJwtProcessingException e) {
			// TODO
			// Tr.error(tc, "ERROR_CREATING_JWT_USING_TOKEN_IN_REQ", new
			// Object[] { e.getLocalizedMessage() });
		}
		return jwttoken;
	}

	private MicroProfileJwtConfig getMpJwtConsumer() {

		return mpjwttai.getMicroProfileJwtConfig(consumerId);
	}

	public boolean isJwtValid(String tokenstr) {
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

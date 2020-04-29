/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.grpc.client.security.oauth;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Adapted from com.ibm.ws.jaxrs20.client.security.oauth.OAuthPropagationHelper
 */
public class GrpcOAuthPropagationHelper {
	private static final TraceComponent tc = Tr.register(GrpcOAuthPropagationHelper.class);
	public static final String ISSUED_JWT_TOKEN = "issuedJwt"; // new jwt token

	/**
	 * Get the type of access token which the runAsSubject authenticated
	 *
	 * @return the Type of Token, such as: Bearer
	 */
	public static String getAccessTokenType() {
		return getSubjectAttributeString("token_type", true);
	}

	public static String getAccessToken() {
		return getSubjectAttributeString("access_token", true);
	}

	public static String getJwtToken() {
		String jwt = getIssuedJwtToken();
		if (jwt == null) {
			jwt = getAccessToken(); // the one that the client received
			if (!isJwt(jwt)) {
				jwt = null;
			}
		}
		return jwt;
	}

	private static boolean isJwt(String jwt) {
		if (jwt != null && jwt.indexOf(".") >= 0) {
			return true;
		}
		return false;
	}

	public static String getIssuedJwtToken() {
		return getSubjectAttributeString(ISSUED_JWT_TOKEN, true); // the newly issued token
	}

	public static String getScopes() {
		return getSubjectAttributeString("scope", true);
	}

	static Subject getRunAsSubject() {
		try {
			return WSSubject.getRunAsSubject();
		} catch (WSSecurityException e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "Exception while getting runAsSubject:", e.getCause());
			}
			// OIDC_FAILED_RUN_AS_SUBJCET=CWWKS1772W: An exception occurred while attempting
			// to get RunAsSubject. The exception was: [{0}]
			Tr.warning(tc, "failed_run_as_subject", e.getLocalizedMessage());
		}
		return null;
	}

	/**
	 * @param string
	 * @return
	 */
	static String getSubjectAttributeString(String attribKey, boolean bindWithAccessToken) {
		try {
			Subject runAsSubject = getRunAsSubject();
			if (runAsSubject != null) {
				return getSubjectAttributeObject(runAsSubject, attribKey, bindWithAccessToken);
			}
		} catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Exception caught", e);
			}
		}
		return null;
	}

	/**
	 * @param runAsSubject
	 * @param attribKey
	 * @return object
	 */
	@FFDCIgnore({ PrivilegedActionException.class })
	static String getSubjectAttributeObject(Subject subject, String attribKey, boolean bindWithAccessToken) {
		try {
			Set<Object> publicCredentials = subject.getPublicCredentials();
			String result = getCredentialAttribute(publicCredentials, attribKey, bindWithAccessToken,
					"publicCredentials");
			if (result == null || result.isEmpty()) {
				Set<Object> privateCredentials = subject.getPrivateCredentials();
				result = getCredentialAttribute(privateCredentials, attribKey, bindWithAccessToken,
						"privateCredentials");
			}
			return result;
		} catch (PrivilegedActionException e) {
			// TODO do we need an error handling in here?
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Did not find a value for the attribute (" + attribKey + ")");
			}
		}
		return null;
	}

	static String getCredentialAttribute(final Set<Object> credentials, final String attribKey,
			final boolean bindWithAccessToken, final String msg) throws PrivilegedActionException {
		// Since this is only for jaxrs client internal usage, it's OK to override java2
		// security
		Object obj = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
			@Override
			public Object run() throws Exception {
				int iCnt = 0;
				for (Object credentialObj : credentials) {
					iCnt++;
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, msg + "(" + iCnt + ") class:" + credentialObj.getClass().getName());
					}
					if (credentialObj instanceof Map) {
						if (bindWithAccessToken) {
							Object accessToken = ((Map<?, ?>) credentialObj).get("access_token");
							if (accessToken == null)
								continue; // on credentialObj
						}
						Object value = ((Map<?, ?>) credentialObj).get(attribKey);
						if (value != null)
							return value;
					}
				}
				return null;
			}
		});
		if (obj != null)
			return obj.toString();
		else
			return null;
	}

}

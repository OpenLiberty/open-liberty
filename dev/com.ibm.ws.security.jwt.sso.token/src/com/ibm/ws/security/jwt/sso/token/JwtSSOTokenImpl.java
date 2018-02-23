/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.sso.token;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;
//import org.eclipse.microprofile.jwt.JsonWebToken;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.common.crypto.HashUtils;
import com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken;
import com.ibm.ws.security.jwtsso.config.JwtSsoConfig;
import com.ibm.ws.security.jwtsso.utils.JwtSsoTokenUtils;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/*
 * This is a utility service to retrieve MicroProfile JsonWebToken in a subject
 */
@Component(service = JwtSSOToken.class, name = "jwtSSOToken", immediate = true, property = "service.vendor=IBM")
public class JwtSSOTokenImpl implements JwtSSOToken {
	private static final TraceComponent tc = Tr.register(JwtSSOTokenImpl.class);

	public static final String JSON_WEB_TOKEN_SSO_CONFIG = "jwtSsoConfig";
	public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
	protected final static AtomicServiceReference<JwtSsoConfig> jwtSSOConfigRef = new AtomicServiceReference<JwtSsoConfig>(
			JSON_WEB_TOKEN_SSO_CONFIG);
	private final SubjectHelper subjectHelper = new SubjectHelper();
	private static final String[] hashtableProperties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };

	@Reference(service = JwtSsoConfig.class, name = JSON_WEB_TOKEN_SSO_CONFIG, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setJwtSsoConfig(ServiceReference<JwtSsoConfig> ref) {
		jwtSSOConfigRef.setReference(ref);
	}

	protected void unsetJwtSsoConfig(ServiceReference<JwtSsoConfig> ref) {
		jwtSSOConfigRef.unsetReference(ref);
	}

	@Activate
	protected void activate(ComponentContext cc) {
		jwtSSOConfigRef.activate(cc);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Jwt SSO config service is activated");
			Tr.debug(tc, "Jwt SSO token (impl) service is being activated!!");
		}
	}

	@Modified
	protected void modified(Map<String, Object> props) {
	}

	@Deactivate
	protected void deactivate(ComponentContext cc) {
		jwtSSOConfigRef.deactivate(cc);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Jwt SSO config service is deactivated");
			Tr.debug(tc, "Jwt SSO token (impl) service is being deactivated!!");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.ws.security.sso.cookie.JwtSSOToken#createSSOToken(javax.security.
	 * auth.Subject)
	 */
	@Override
	public void createJwtSSOToken(Subject subject) throws WSSecurityException {
		// TODO Auto-generated method stub
		if (subject != null) {
			if (isSubjectUnauthenticated(subject) || subjectHasJwtPrincipal(subject)) {
				return;
			}

			JwtSsoTokenUtils tokenUtil = getJwtSsoTokenUtils();
			if (tokenUtil != null) {
				JsonWebToken ssotoken = tokenUtil.buildTokenFromSecuritySubject(subject); // TODO
				updateSubject(subject, ssotoken);
			}
		}
	}

	/**
	 * @param subject
	 * @return
	 */
	private boolean subjectHasJwtPrincipal(Subject subject) {
		// TODO Auto-generated method stub
		return (!getJwtPrincipals(subject).isEmpty());
	}

	/**
	 * @param subject
	 * @return
	 */
	private boolean isSubjectUnauthenticated(Subject subject) {
		// TODO Auto-generated method stub
		Set<WSPrincipal> principals = getWSPrincipals(subject);
		if (principals != null && !principals.isEmpty()) { // TODO : multiple
															// principals
															// error??
			if (!UNAUTHENTICATED.equals(principals.iterator().next().getName())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param subject
	 * @param ssotoken
	 */
	private void updateSubject(Subject subject, JsonWebToken ssotoken) {
		// TODO Auto-generated method stub

		if (subject != null && ssotoken != null) {
			@SuppressWarnings("unchecked")
			Hashtable<String, Object> customProperties = (Hashtable<String, Object>) customPropertiesFromSubject(
					subject);
			if (customProperties == null) {
				customProperties = new Hashtable<String, Object>();
			}
			addCustomCacheKeyToCustomProperties(customProperties, ssotoken);
			addCustomPropertiesToSubject(subject, customProperties);
			addJwtSSOTokenToSubject(subject, ssotoken);
		}

	}

	/**
	 * @param subject
	 * @param ssotoken
	 */
	private void addJwtSSOTokenToSubject(Subject subject, JsonWebToken ssotoken) {
		// TODO Auto-generated method stub
		if (subject != null && ssotoken != null) {
			subject.getPrivateCredentials().add(ssotoken);
			subject.getPrincipals().add(ssotoken);
		}

	}

	/**
	 * @param subject
	 * @param customProperties
	 */
	private void addCustomPropertiesToSubject(Subject subject, Hashtable<String, Object> customProperties) {
		// TODO Auto-generated method stub
		if (subject != null && customProperties != null) {
			subject.getPrivateCredentials().add(customProperties);
		}

	}

	private void addCustomCacheKeyToCustomProperties(Hashtable<String, Object> customProperties,
			JsonWebToken ssotoken) {

		if (customProperties != null && ssotoken != null) {
			String customCacheKey = HashUtils.digest(ssotoken.toString());
			customProperties.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
		}
	}

	/**
	 * @param subject
	 */
	private Hashtable<String, ?> customPropertiesFromSubject(Subject subject) {
		// TODO Auto-generated method stub
		return subjectHelper.getHashtableFromSubject(subject, hashtableProperties);
	}

	/**
	 * @param builder
	 * @param consumer
	 */
	private JwtSsoTokenUtils getJwtSsoTokenUtils() {
		JwtSsoConfig jwtssoconfig = getJwtSSOConfig();
		String builder = null;
		String consumer = null;
		if (jwtssoconfig != null) {
			builder = getJwtBuilder(jwtssoconfig);
			consumer = getJwtConsumer(jwtssoconfig);
		}
		if (builder != null && consumer != null) {
			JwtSsoTokenUtils result = new JwtSsoTokenUtils(builder, consumer);
			return result.isValid() ? result : null;
		}
		return null;
	}

	/**
	 * @param jwtssoconfig
	 * @return
	 */
	private String getJwtConsumer(JwtSsoConfig jwtssoconfig) {
		// TODO Auto-generated method stub
		return jwtssoconfig.getJwtConsumerRef();
	}

	/**
	 * @param jwtssoconfig
	 * @return
	 */
	private String getJwtBuilder(JwtSsoConfig jwtssoconfig) {
		// TODO Auto-generated method stub
		return jwtssoconfig.getJwtBuilderRef();
	}

	/**
	 *
	 */
	private JwtSsoConfig getJwtSSOConfig() {
		if (jwtSSOConfigRef.getService() != null) {
			return jwtSSOConfigRef.getService();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.ws.security.sso.cookie.JwtSSOToken#getJwtSSOToken(javax.security.
	 * auth.Subject)
	 */
	@Override
	public String getJwtSSOToken(Subject subject) {
		// TODO Auto-generated method stub
		String encodedjwtprincipal = null;
		Set<JsonWebToken> jsonWebTokenPrincipalSet = getJwtPrincipals(subject);
		if (!jsonWebTokenPrincipalSet.isEmpty()) {
			if (hasMultiplePrincipals(jsonWebTokenPrincipalSet)) {
				// TODO error
			} else {
				encodedjwtprincipal = convertToEncoded(jsonWebTokenPrincipalSet.iterator().next());
			}
		}
		return encodedjwtprincipal;
	}

	/**
	 * @param next
	 */
	private String convertToEncoded(JsonWebToken jwtprincipal) {
		// TODO Auto-generated method stub
		String rawtoken = null;

		// if ((rawtoken = getRawJwtToken(jwtprincipal)) != null) {
		// return JsonUtils.convertToBase64(rawtoken);
		// }
		rawtoken = getRawJwtToken(jwtprincipal);
		return rawtoken;
	}

	/**
	 * @param jwtprincipal
	 * @return
	 */
	private String getRawJwtToken(JsonWebToken jwtprincipal) {
		if (jwtprincipal != null) {
			return jwtprincipal.getRawToken();
		}
		return null;
	}

	/**
	 * @param jsonWebTokenPrincipalSet
	 * @return
	 */
	private boolean hasMultiplePrincipals(Set<JsonWebToken> jsonWebTokenPrincipalSet) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param subject
	 * @return
	 */
	private Set<JsonWebToken> getJwtPrincipals(Subject subject) {
		// TODO Auto-generated method stub
		return subject != null ? subject.getPrincipals(JsonWebToken.class) : null;

	}

	private Set<WSPrincipal> getWSPrincipals(Subject subject) {
		// TODO Auto-generated method stub
		return subject != null ? subject.getPrincipals(WSPrincipal.class) : null;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#handleJwtSSOToken(
	 * java.lang.String)
	 */
	@Override
	public Subject handleJwtSSOToken(String encodedjwt) {
		// TODO Auto-generated method stub
		Subject subject = null;
		JwtSsoTokenUtils tokenUtil = getJwtSsoTokenUtils();
		if (tokenUtil != null) {
			subject = tokenUtil.handleJwtSsoTokenValidation(encodedjwt);
		}
		return subject;
		// authenticateWithJwt(subject);

	}

	/**
	 * @param subject
	 */
	// private void authenticateWithJwt(Subject subject) {
	// // TODO Auto-generated method stub
	// AuthenticationResult authResult;
	// try {
	// AuthenticationData authenticationData = createAuthenticationData(req,
	// res, subject);
	// Subject new_subject =
	// authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND,
	// authenticationData, subject);
	// authResult = new AuthenticationResult(AuthResult.SUCCESS, new_subject);
	// // if DISABLE_LTPA_AND_SESSION_NOT_ON_OR_AFTER then do not
	// // callSSOCookie
	// if (addLtpaCookieToResponse(new_subject, taiId)) {
	// ssoCookieHelper.addSSOCookiesToResponse(new_subject, req, res);
	// }
	// } catch (AuthenticationException e) {
	// authResult = new AuthenticationResult(AuthResult.FAILURE,
	// e.getMessage());
	// }
	// return authResult;
	//
	// }

}

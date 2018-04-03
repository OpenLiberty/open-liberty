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
package com.ibm.ws.security.jwtsso.token;

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
import com.ibm.ws.security.jwtsso.config.JwtSsoBuilderConfig;
import com.ibm.ws.security.jwtsso.config.JwtSsoConfig;
import com.ibm.ws.security.jwtsso.token.proxy.JwtSSOTokenProxy;
import com.ibm.ws.security.jwtsso.utils.JwtSsoTokenUtils;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/*
 * This is a utility service to retrieve MicroProfile JsonWebToken in a subject
 */
@Component(service = JwtSSOTokenProxy.class, name = "JwtSSOTokenProxy", immediate = true, property = "service.vendor=IBM")
public class JwtSSOTokenImpl implements JwtSSOTokenProxy {
	private static final TraceComponent tc = Tr.register(JwtSSOTokenImpl.class);

	public static final String JSON_WEB_TOKEN_SSO_CONFIG = "jwtSsoConfig";
	public static final String JSON_WEB_TOKEN_SSO_BUILDER_CONFIG = "jwtSsoBuilderConfig";
	public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
	protected final static AtomicServiceReference<JwtSsoConfig> jwtSSOConfigRef = new AtomicServiceReference<JwtSsoConfig>(
			JSON_WEB_TOKEN_SSO_CONFIG);
	protected final static AtomicServiceReference<JwtSsoBuilderConfig> jwtSSOBuilderConfigRef = new AtomicServiceReference<JwtSsoBuilderConfig>(
			JSON_WEB_TOKEN_SSO_BUILDER_CONFIG);
	private final SubjectHelper subjectHelper = new SubjectHelper();
	private static final String[] hashtableProperties = { AttributeNameConstants.WSCREDENTIAL_CACHE_KEY };

	@Reference(service = JwtSsoConfig.class, name = JSON_WEB_TOKEN_SSO_CONFIG, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setJwtSsoConfig(ServiceReference<JwtSsoConfig> ref) {
		jwtSSOConfigRef.setReference(ref);
	}

	protected void unsetJwtSsoConfig(ServiceReference<JwtSsoConfig> ref) {
		jwtSSOConfigRef.unsetReference(ref);
	}

	@Reference(service = JwtSsoBuilderConfig.class, name = JSON_WEB_TOKEN_SSO_BUILDER_CONFIG, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setJwtSsoBuilderConfig(ServiceReference<JwtSsoBuilderConfig> ref) {
		jwtSSOBuilderConfigRef.setReference(ref);
	}

	protected void unsetJwtSsoBuilderConfig(ServiceReference<JwtSsoBuilderConfig> ref) {
		jwtSSOBuilderConfigRef.unsetReference(ref);
	}

	@Activate
	protected void activate(ComponentContext cc) {
		jwtSSOConfigRef.activate(cc);
		jwtSSOBuilderConfigRef.activate(cc);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Jwt SSO config consumer service is activated");
			Tr.debug(tc, "Jwt SSO config builder service is activated");
			Tr.debug(tc, "Jwt SSO token (impl) service is being activated!!");
		}
	}

	@Modified
	protected void modified(Map<String, Object> props) {
	}

	@Deactivate
	protected void deactivate(ComponentContext cc) {
		jwtSSOConfigRef.deactivate(cc);
		jwtSSOBuilderConfigRef.deactivate(cc);
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Jwt SSO config consumer service is deactivated");
			Tr.debug(tc, "Jwt SSO config builder service is deactivated");
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
				JsonWebToken ssotoken = null;
				try {
					ssotoken = tokenUtil.buildTokenFromSecuritySubject(subject);
				} catch (Exception e) {
					// TODO ffdc
					throw new WSSecurityException(e);
				}
				updateSubject(subject, ssotoken);
			} else {
				// TODO : nls
				String msg = "jwtsso configuration is not valid";
				throw new WSSecurityException(msg);
			}
		}
	}

	/**
	 * @param subject
	 * @param ssotoken
	 */
	private void updateSubject(Subject subject, JsonWebToken ssotoken) {
		// TODO Auto-generated method stub
		if (subject != null && ssotoken != null) {
			addJwtSSOTokenToSubject(subject, ssotoken);
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
	private void addJwtSSOTokenToSubject(Subject subject, JsonWebToken ssotoken) {
		// TODO Auto-generated method stub
		if (subject != null && ssotoken != null) {
			// subject.getPrivateCredentials().add(ssotoken);
			subject.getPrincipals().add(ssotoken);
		}

	}

	/**
	 * @param builder
	 * @param consumer
	 */
	private JwtSsoTokenUtils getJwtSsoTokenUtils() {
		JwtSsoBuilderConfig jwtssobuilderConfig = getJwtSSOBuilderConfig();
		String builder = null;
		if (jwtssobuilderConfig != null) {
			builder = getJwtBuilder(jwtssobuilderConfig);
		}
		JwtSsoConfig jwtssoconsumerConfig = getJwtSSOConsumerConfig();
		String consumer = null;
		if (jwtssoconsumerConfig != null) {
			consumer = getJwtConsumer(jwtssoconsumerConfig);
		}
		if (builder != null && consumer != null) {
			JwtSsoTokenUtils result = new JwtSsoTokenUtils(builder, consumer);
			return result.isValid() ? result : null;
		}
		return null;
	}

	private JwtSsoTokenUtils getSimpleJwtSsoTokenUtils() {
		return new JwtSsoTokenUtils();
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
	private String getJwtBuilder(JwtSsoBuilderConfig jwtssobuilderconfig) {
		// TODO Auto-generated method stub
		return jwtssobuilderconfig.getJwtBuilderRef();
	}

	/**
	 *
	 */
	private JwtSsoConfig getJwtSSOConsumerConfig() {
		if (jwtSSOConfigRef.getService() != null) {
			return jwtSSOConfigRef.getService();
		}
		return null;
	}

	private JwtSsoBuilderConfig getJwtSSOBuilderConfig() {
		if (jwtSSOBuilderConfigRef.getService() != null) {
			return jwtSSOBuilderConfigRef.getService();
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

		// if ((rawtoken = getRawJwtToken(jwtprincipal)) != null) {
		// return JsonUtils.convertToBase64(rawtoken);
		// }
		return getRawJwtToken(jwtprincipal); // this is already encoded

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
	public Subject handleJwtSSOTokenValidation(Subject subject, String encodedjwt) throws WSSecurityException {
		// TODO Auto-generated method stub
		JwtSsoTokenUtils tokenUtil = getJwtSsoTokenUtils();
		if (tokenUtil != null && encodedjwt != null) {
			if (subject != null) {
				try {
					return tokenUtil.handleJwtSsoTokenValidationWithSubject(subject, encodedjwt);
				} catch (Exception e) {
					throw new WSSecurityException(e);
				}
			} else {
				try {
					return tokenUtil.handleJwtSsoTokenValidation(encodedjwt);
				} catch (Exception e) {
					throw new WSSecurityException(e);

				}
			}
		} else {
			// TODO : nls
			String msg = "jwtsso configuration is not valid or token is not valid";
			throw new WSSecurityException(msg);
		}
		// authenticateWithJwt(subject);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#
	 * getCustomCacheKeyFromJwtSSOToken(java.lang.String)
	 */
	@Override
	public String getCustomCacheKeyFromJwtSSOToken(String encodedjwt) {
		// TODO Auto-generated method stub
		JwtSsoTokenUtils tokenUtil = getSimpleJwtSsoTokenUtils();
		if (encodedjwt != null) {
			return tokenUtil.getCustomCacheKeyFromToken(encodedjwt);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#
	 * getCacheKeyForJwtSSOToken(javax.security.auth.Subject, java.lang.String)
	 */
	@Override
	public String getCacheKeyForJwtSSOToken(Subject subject, String encodedjwt) {
		// TODO Auto-generated method stub
		if (encodedjwt != null) {
			return HashUtils.digest(encodedjwt);
		} else if (subject != null) {
			String jwtssotoken = getJwtSSOToken(subject);
			if (jwtssotoken != null) {
				return HashUtils.digest(jwtssotoken);
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#
	 * addCustomCacheKeyToJwtSSOToken(javax.security.auth.Subject,
	 * java.lang.String)
	 */
	@Override
	public void addCustomCacheKeyToJwtSSOToken(Subject subject, String cacheKeyValue) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#isJwtSSOTokenValid(
	 * javax.security.auth.Subject)
	 */
	@Override
	public boolean isJwtSSOTokenValid(Subject subject) {
		// TODO Auto-generated method stub
		String encodedjwt = getJwtSSOToken(subject);
		JwtSsoTokenUtils tokenUtil = getJwtSsoTokenUtils();
		if (tokenUtil != null) {
			return tokenUtil.isJwtValid(encodedjwt);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#getJwtCookieName()
	 */
	@Override
	public String getJwtCookieName() {
		// TODO Auto-generated method stub
		JwtSsoBuilderConfig jwtssobuilderConfig = getJwtSSOBuilderConfig();
		if (jwtssobuilderConfig != null) {
			return jwtssobuilderConfig.getCookieName();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#
	 * shouldSetJwtCookiePathToWebAppContext()
	 */
	@Override
	public boolean shouldSetJwtCookiePathToWebAppContext() {
		// TODO Auto-generated method stub
		JwtSsoBuilderConfig jwtssobuilderConfig = getJwtSSOBuilderConfig();
		if (jwtssobuilderConfig != null) {
			return jwtssobuilderConfig.isSetCookiePathToWebAppContextPath();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#
	 * shouldAlsoIncludeLtpaCookie()
	 */
	@Override
	public boolean shouldAlsoIncludeLtpaCookie() {
		// TODO Auto-generated method stub
		JwtSsoBuilderConfig jwtssobuilderConfig = getJwtSSOBuilderConfig();
		if (jwtssobuilderConfig != null) {
			return jwtssobuilderConfig.isIncludeLtpaCookie();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.ws.security.jwt.sso.token.utils.JwtSSOToken#
	 * shouldFallbackToLtpaCookie()
	 */
	@Override
	public boolean shouldFallbackToLtpaCookie() {
		// TODO Auto-generated method stub
		JwtSsoBuilderConfig jwtssobuilderConfig = getJwtSSOBuilderConfig();
		if (jwtssobuilderConfig != null) {
			return jwtssobuilderConfig.isFallbackToLtpa();
		}
		return true;
	}

}

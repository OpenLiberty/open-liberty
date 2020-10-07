/*******************************************************************************
 * ˇ * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.internal;

import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.jwtsso.config.JwtSsoConfig;
import com.ibm.ws.security.jwtsso.utils.ConfigUtils;
import com.ibm.ws.security.jwtsso.utils.IssuerUtil;
import com.ibm.ws.security.jwtsso.utils.JwtSsoConstants;
import com.ibm.ws.security.mp.jwt.MicroProfileJwtConfig;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

@Component(service = { JwtSsoConfig.class, MicroProfileJwtConfig.class,
		JwtConsumerConfig.class }, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.security.jwtsso", name = "jwtSsoConfig", property = "service.vendor=IBM")
public class JwtSsoComponent implements JwtSsoConfig {

	private static final TraceComponent tc = Tr.register(JwtSsoComponent.class);

	private DynamicMBean httpsendpointInfoMBean;

	private DynamicMBean httpendpointInfoMBean;

	private ServerInfoMBean serverInfoMBean;

	public static final String KEY_SSL_SUPPORT = "sslSupport";
	private final AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(
			KEY_SSL_SUPPORT);
	public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
	private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(
			KEY_KEYSTORE_SERVICE);

	private boolean setCookiePathToWebAppContextPath;
	private boolean includeLtpaCookie;
	private boolean fallbackToLtpa;
	private boolean cookieSecureFlag;
	// private String jwtBuilderRef;
	private String mpjwtConsumerRef;
	private String cookieName;
	private boolean disableJwtCookie;

	protected static final String KEY_UNIQUE_ID = "id";
	protected String uniqueId = null;

	private String signatureAlgorithm;

	ConsumerUtils consumerUtils = null; // lazy init

	private IssuerUtil issuerUtil;

	@Override
	public boolean isHttpOnlyCookies() {
		return WebConfigUtils.getWebAppSecurityConfig().getHttpOnlyCookies();
	}

	@Override
	public boolean isSsoUseDomainFromURL() {
		return WebConfigUtils.getWebAppSecurityConfig().getSSOUseDomainFromURL();
	}

	@Override
	public List<String> getSsoDomainNames() {
		return WebConfigUtils.getWebAppSecurityConfig().getSSODomainList();
	}

	@Override
	public boolean isSetCookiePathToWebAppContextPath() {
		return setCookiePathToWebAppContextPath;
	}

	@Override
	public boolean isIncludeLtpaCookie() {
		return includeLtpaCookie;
	}

	@Override
	public boolean isFallbackToLtpa() {
		return fallbackToLtpa;
	}

	@Override
	public boolean isCookieSecured() {
		return cookieSecureFlag;
	}

	/** {@inheritDoc} */
	@Override
	public String getJwtConsumerRef() {
		return mpjwtConsumerRef;
	}

	// todo: base sec is going to make WebAppSecurityConfig an osgi service, but
	// it's not there yet.
	// Meanwhile we'll get it in a non-dynamic way from WebConfigUtils
	/*
	 * @org.osgi.service.component.annotations.Reference(cardinality =
	 * ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC,
	 * policyOption = ReferencePolicyOption.GREEDY) protected void
	 * setWebAppSecConfig(WebAppSecurityConfig config) {
	 * System.out.println("**** webappSecConfig set called"); webAppSecConfig =
	 * config; }
	 *
	 * protected void unsetWebAppSecConfig(WebAppSecurityConfig config) {
	 * webAppSecConfig = null; }
	 */

	// todo: remove if not needed
	@org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
		httpendpointInfoMBean = endpointInfoMBean;
	}

	// todo: remove if not needed
	protected void unsetEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
		if (httpendpointInfoMBean == endpointInfoMBean) {
			httpendpointInfoMBean = null;
		}
	}

	// todo: remove if not needed
	@org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint-ssl)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setHttpsEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
		httpsendpointInfoMBean = endpointInfoMBean;
	}

	// todo: remove if not needed
	protected void unsetHttpsEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
		if (httpsendpointInfoMBean == endpointInfoMBean) {
			httpsendpointInfoMBean = null;
		}
	}

	/**
	 * DS injection WebSphere:feature=kernel,name=ServerInfo
	 */
	@org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=kernel,name=ServerInfo)", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY)
	protected void setServerInfoMBean(ServerInfoMBean serverInfoMBean) {
		this.serverInfoMBean = serverInfoMBean;
	}

	protected void unsetServerInfoMBean(ServerInfoMBean serverInfoMBean) {
		if (this.serverInfoMBean == serverInfoMBean) {
			this.serverInfoMBean = null;
		}
	}

	@Reference(service = KeyStoreService.class, name = KEY_KEYSTORE_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
		keyStoreServiceRef.setReference(ref);
		// keyStoreServiceMapRef.putReference((String) ref.getProperty(ID),
		// ref);
	}

	protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
		keyStoreServiceRef.unsetReference(ref);
		// keyStoreServiceMapRef.removeReference((String) ref.getProperty(ID),
		// ref);
	}

	@Reference(service = SSLSupport.class, name = KEY_SSL_SUPPORT, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
	protected void setSslSupport(ServiceReference<SSLSupport> ref) {
		sslSupportRef.setReference(ref);
	}

	protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
		sslSupportRef.setReference(ref);
	}

	protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
		sslSupportRef.unsetReference(ref);
	}

	@Activate
	protected void activate(Map<String, Object> properties, ComponentContext cc) {
		uniqueId = (String) properties.get(KEY_UNIQUE_ID);
		process(properties);
		keyStoreServiceRef.activate(cc);
		sslSupportRef.activate(cc);

		JwtUtils.setKeyStoreService(keyStoreServiceRef);
		JwtUtils.setSSLSupportService(sslSupportRef);
	}

	@Modified
	protected void modify(Map<String, Object> properties) {
		process(properties);
	}

	@Deactivate
	protected void deactivate(int reason, ComponentContext cc) {

		keyStoreServiceRef.deactivate(cc);
		sslSupportRef.deactivate(cc);
		JwtUtils.setKeyStoreService(null);
		JwtUtils.setSSLSupportService(null);
	}

	private void process(Map<String, Object> props) {
		if (tc.isEntryEnabled()) {
			Tr.entry(tc, "process", props);
		}
		if (props == null || props.isEmpty()) {
			return;
		}
		setCookiePathToWebAppContextPath = (Boolean) props
				.get(JwtSsoConstants.CFG_KEY_SETCOOKIEPATHTOWEBAPPCONTEXTPATH);
		includeLtpaCookie = (Boolean) props.get(JwtSsoConstants.CFG_KEY_INCLUDELTPACOOKIE);
		fallbackToLtpa = (Boolean) props.get(JwtSsoConstants.CFG_USE_LTPA_IF_JWT_ABSENT);
		cookieSecureFlag = (Boolean) props.get(JwtSsoConstants.CFG_KEY_COOKIESECUREFLAG);
		disableJwtCookie = (Boolean) props.get(JwtSsoConstants.CFG_KEY_DISABLE_JWT_COOKIE);
		// jwtBuilderRef = JwtUtils.trimIt((String)
		// props.get(JwtSsoConstants.CFG_KEY_JWTBUILDERREF));
		mpjwtConsumerRef = JwtUtils.trimIt((String) props.get(JwtSsoConstants.CFG_KEY_JWTCONSUMERREF)); // hmm,
																										// this
																										// does
																										// not
																										// exist
																										// in
																										// metatype.
		cookieName = JwtUtils.trimIt((String) props.get(JwtSsoConstants.CFG_KEY_COOKIENAME));
		cookieName = (new ConfigUtils()).validateCookieName(cookieName, true);
		if (mpjwtConsumerRef == null) {
			setJwtSsoConsumerDefaults();
		}
		if (tc.isEntryEnabled()) {
			Tr.exit(tc, "process");
		}
	}

	private void setJwtSsoConsumerDefaults() {
		mpjwtConsumerRef = getId();
		signatureAlgorithm = "RS256";
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "consumer id = ", mpjwtConsumerRef);
		}
		issuerUtil = new IssuerUtil();
	}

	/** {@inheritDoc} */

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return getUniqueId();
	}

	/** {@inheritDoc} */
	@Override
	public List<String> getAudiences() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public boolean ignoreAudClaimIfNotConfigured() {
        return false;
    }

	/** {@inheritDoc} */
	@Override
	public String getSignatureAlgorithm() {
		// TODO Auto-generated method stub
		return signatureAlgorithm;
	}

	/** {@inheritDoc} */
	@Override
	public String getSharedKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getTrustStoreRef() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getTrustedAlias() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getUniqueId() {
		// TODO Auto-generated method stub
		return uniqueId;
	}

	/** {@inheritDoc} */
	@Override
	public String getUserNameAttribute() {
		return "upn";
	}

	/** {@inheritDoc} */
	@Override
	public String getGroupNameAttribute() {
		return "groups";
	}

	/** {@inheritDoc} */
	@Override
	public boolean ignoreApplicationAuthMethod() {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean getMapToUserRegistry() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public String getIssuer() {
		// TODO Auto-generated method stub
		return issuerUtil.getResolvedHostAndPortUrl(httpsendpointInfoMBean, httpendpointInfoMBean, serverInfoMBean,
				uniqueId);
	}

	/** {@inheritDoc} */
	@Override
	public long getClockSkew() {
		// 5 minutes, in milliseconds
		return 5 * 60 * 1000;
	}

	/** {@inheritDoc} */
	@Override
	public boolean getJwkEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public String getJwkEndpointUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public ConsumerUtils getConsumerUtils() {
		if (consumerUtils == null) { // lazy init
			consumerUtils = new ConsumerUtils(keyStoreServiceRef);
		}
		return consumerUtils;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isValidationRequired() {
		// TODO Auto-generated method stub
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isHostNameVerificationEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public String getSslRef() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public JWKSet getJwkSet() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean getTokenReuse() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public String getCookieName() {
		// TODO Auto-generated method stub
		return cookieName;
	}

	@Override
	public String getAuthFilterRef() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getUseSystemPropertiesForHttpClientConnections() {
		return false; // jwk retrieval is not in play here
	}

	@Override
	public String getAuthorizationHeaderScheme() {
		// TODO Auto-generated method stub
		return "Bearer ";
	}

	@Override
	public boolean isDisableJwtCookie() {
		// TODO Auto-generated method stub
		return disableJwtCookie;
	}

    @Override
    public String getTokenHeader() {
        return null;
    }

    @Override
	public List<String> getAMRClaim() {
		// TODO Auto-generated method stub
		return null;
	}

}

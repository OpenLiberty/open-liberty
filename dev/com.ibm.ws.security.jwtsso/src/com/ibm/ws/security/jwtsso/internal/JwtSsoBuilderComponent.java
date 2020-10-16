/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * This class handles the builder objects, the JwtSsoComponent class handles the
 * consumer objects. This class functions as both a jwtsso component and as the
 * default builder entity for jwtsso, which has different settings than the
 * default builder entity for jwt.
 */
package com.ibm.ws.security.jwtsso.internal;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;

import javax.management.DynamicMBean;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.jwtsso.config.JwtSsoBuilderConfig;
import com.ibm.ws.security.jwtsso.utils.ConfigUtils;
import com.ibm.ws.security.jwtsso.utils.IssuerUtil;
import com.ibm.ws.security.jwtsso.utils.JwtSsoConstants;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.jwk.JSONWebKey;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

// This class handles the builder objects, the JwtSsoComponent class handles the consumer objects.
@Component(service = { JwtSsoBuilderConfig.class,
		JwtConfig.class }, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.security.jwtsso", name = "jwtSsoBuilderConfig", property = "service.vendor=IBM")
public class JwtSsoBuilderComponent implements JwtSsoBuilderConfig {

	private static final TraceComponent tc = Tr.register(JwtSsoBuilderComponent.class);

	private DynamicMBean httpsendpointInfoMBean;

	private DynamicMBean httpendpointInfoMBean;

	private ServerInfoMBean serverInfoMBean;

	private boolean setCookiePathToWebAppContextPath;
	private boolean includeLtpaCookie;
	private boolean useLtpaIfJwtAbsent;
	private boolean cookieSecureFlag;
	private String jwtBuilderRef;
	private String jwtConsumerRef;
	private String cookieName;
	private WebAppSecurityConfig webAppSecConfig;
	private String signatureAlgorithm = "RS256";
	private final static String KEY_JWT_SERVICE = "jwtConfig";
	private final static String CFG_KEY_ID = "id";
	private final JwtConfig builderConfig = null;
	private final Object initlock = new Object();
	ConcurrentServiceReferenceMap<String, JwtConfig> jwtServiceMapRef = new ConcurrentServiceReferenceMap<String, JwtConfig>(
			KEY_JWT_SERVICE);
	protected static final String KEY_UNIQUE_ID = "id";
	protected String uniqueId = null;
	private IssuerUtil issuerUtil;
	private boolean isDefaultBuilder = false;
	private boolean disableJwtCookie = false;

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
	public boolean isUseLtpaIfJwtAbsent() {
		return useLtpaIfJwtAbsent;
	}

	@Override
	public boolean isCookieSecured() {
		return cookieSecureFlag;
	}

	@Override
	public String getJwtBuilderRef() {
		return jwtBuilderRef;
	}

	// /** {@inheritDoc} */
	// @Override
	// public String getJwtConsumerRef() {
	// return jwtConsumerRef;
	// }

	// we track the builder config so we can get the token expiration time
	@org.osgi.service.component.annotations.Reference(service = JwtConfig.class, name = KEY_JWT_SERVICE, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.RELUCTANT)
	protected void setJwtConfig(org.osgi.framework.ServiceReference<JwtConfig> ref) {
		synchronized (initlock) {
			jwtServiceMapRef.putReference((String) ref.getProperty(CFG_KEY_ID), ref);
		}
	}

	protected void unsetJwtConfig(org.osgi.framework.ServiceReference<JwtConfig> ref) {
		synchronized (initlock) {
			jwtServiceMapRef.removeReference((String) ref.getProperty(CFG_KEY_ID), ref);
		}
	}

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

	@Activate
	public void activate(Map<String, Object> properties, ComponentContext cc) {
		uniqueId = (String) properties.get(KEY_UNIQUE_ID);
		process(properties);
	}

	@Modified
	protected void modify(Map<String, Object> properties) {
		process(properties);
	}

	@Deactivate
	protected void deactivate(int reason, ComponentContext cc) {

	}

	private void process(Map<String, Object> props) {
		if (tc.isEntryEnabled()) {
			Tr.entry(tc, "process");
		}

		if (props == null || props.isEmpty()) {
			return;
		}
		setCookiePathToWebAppContextPath = (Boolean) props
				.get(JwtSsoConstants.CFG_KEY_SETCOOKIEPATHTOWEBAPPCONTEXTPATH);
		includeLtpaCookie = (Boolean) props.get(JwtSsoConstants.CFG_KEY_INCLUDELTPACOOKIE);
		useLtpaIfJwtAbsent = (Boolean) props.get(JwtSsoConstants.CFG_USE_LTPA_IF_JWT_ABSENT);
		cookieSecureFlag = (Boolean) props.get(JwtSsoConstants.CFG_KEY_COOKIESECUREFLAG);
		jwtBuilderRef = JwtUtils.trimIt((String) props.get(JwtSsoConstants.CFG_KEY_JWTBUILDERREF));
		disableJwtCookie = (Boolean) props.get(JwtSsoConstants.CFG_KEY_DISABLE_JWT_COOKIE);
		isDefaultBuilder = false;
		if (jwtBuilderRef == null) {
			setJwtSsoBuilderDefaults();
			isDefaultBuilder = true;
		}
		jwtConsumerRef = JwtUtils.trimIt((String) props.get(JwtSsoConstants.CFG_KEY_JWTCONSUMERREF));
		cookieName = JwtUtils.trimIt((String) props.get(JwtSsoConstants.CFG_KEY_COOKIENAME));
		cookieName = (new ConfigUtils()).validateCookieName(cookieName, false);
		if (tc.isEntryEnabled()) {
			Tr.exit(tc, "process");
		}
	}

	private void setJwtSsoBuilderDefaults() {
		jwtBuilderRef = getId();
		signatureAlgorithm = "RS256";
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "builder id = ", jwtBuilderRef);
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

	public String getUniqueId() {
		// TODO Auto-generated method stub
		return uniqueId;
	}

	/** {@inheritDoc} */
	@Override
	public String getIssuerUrl() {
		return getResolvedHostAndPortUrl();

	}

	/** {@inheritDoc} */
	@Override
	public boolean isJwkEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public long getValidTime() {
		long result = 0;
		if (isDefaultBuilder) {
			result = 2 * 3600;
		} else {
			boolean haveNull = jwtServiceMapRef.getReference(jwtBuilderRef) == null
					|| jwtServiceMapRef.getReference(jwtBuilderRef).getProperty(JwtUtils.CFG_KEY_VALID) == null;

			result = (haveNull) ? 0
					: ((Long) jwtServiceMapRef.getReference(jwtBuilderRef).getProperty(JwtUtils.CFG_KEY_VALID))
							.longValue();
		}
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public List<String> getClaims() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getScope() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean getJti() {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public String getKeyStoreRef() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getKeyAlias() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public String getJwkJsonString() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public JSONWebKey getJSONWebKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public long getJwkRotationTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public int getJwkSigningKeySize() {
		// TODO Auto-generated method stub
		return 0;
	}

	/** {@inheritDoc} */

	@Override
	public String getResolvedHostAndPortUrl() {
		return issuerUtil.getResolvedHostAndPortUrl(httpsendpointInfoMBean, httpendpointInfoMBean, serverInfoMBean,
				uniqueId);
		// TODO Auto-generated method stub
	}

	/** {@inheritDoc} */
	@Override
	public PrivateKey getPrivateKey() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public PublicKey getPublicKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCookieName() {
		// TODO Auto-generated method stub
		return cookieName;
	}

	@Override
	public boolean isDisableJwtCookie() {
		// TODO Auto-generated method stub
		return disableJwtCookie;
	}

	@Override
	public long getElapsedNbfTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<String> getAMRAttributes() {
		return null;
	}

    @Override
    public String getKeyManagementKeyAlgorithm() {
        return null;
    }

    @Override
    public String getKeyManagementKeyAlias() {
        return null;
    }

    @Override
    public String getContentEncryptionAlgorithm() {
        return null;
    }

}

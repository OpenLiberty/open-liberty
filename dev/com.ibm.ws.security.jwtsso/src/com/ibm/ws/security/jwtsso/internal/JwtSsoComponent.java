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
import com.ibm.ws.security.common.jwk.impl.JWKProvider;
import com.ibm.ws.security.jwtsso.config.JwtSsoConfig;

@Component(service = JwtSsoConfig.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.security.jwtsso", name = "jwtSsoConfig", property = "service.vendor=IBM")
public class JwtSsoComponent implements JwtSsoConfig {

	// todo: clean this up, implement the interface.
	private static final TraceComponent tc = Tr.register(JwtSsoComponent.class);

	private final String issuer = null;
	private final String issuerUrl = null;
	private long valid;
	private boolean isJwkEnabled;
	private boolean jti;
	private List<String> audiences;
	private String sigAlg;
	private List<String> claims;
	private String scope;
	private String sharedKey;
	private String keyStoreRef;
	private String trustStoreRef;
	private String keyAlias;
	private String trustedAlias;
	private long jwkRotationTime;
	private int jwkSigningKeySize;
	private boolean tokenEndpointHttpsRequired;

	private final PublicKey publicKey = null;
	private final PrivateKey privateKey = null;

	private final JWKProvider jwkProvider = null;

	private DynamicMBean httpsendpointInfoMBean;

	private DynamicMBean httpendpointInfoMBean;

	private ServerInfoMBean serverInfoMBean;

	@org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
		httpendpointInfoMBean = endpointInfoMBean;
	}

	protected void unsetEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
		if (httpendpointInfoMBean == endpointInfoMBean) {
			httpendpointInfoMBean = null;
		}
	}

	@org.osgi.service.component.annotations.Reference(target = "(jmx.objectname=WebSphere:feature=channelfw,type=endpoint,name=defaultHttpEndpoint-ssl)", cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	protected void setHttpsEndPointInfoMBean(DynamicMBean endpointInfoMBean) {
		httpsendpointInfoMBean = endpointInfoMBean;
	}

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
	protected void activate(Map<String, Object> properties, ComponentContext cc) {
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
		// TODO Auto-generated method stub
		if (props == null || props.isEmpty()) {
			return;
		}
		// todo: implement
		// issuer = JwtUtils.trimIt((String) props.get(JwtUtils.CFG_KEY_ID));

	}

}

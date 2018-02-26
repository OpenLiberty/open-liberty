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
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.security.jwtsso.config.JwtSsoConfig;
import com.ibm.ws.security.jwtsso.utils.JwtSsoUtils;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;

@Component(service = JwtSsoConfig.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "com.ibm.ws.security.jwtsso", name = "jwtSsoConfig", property = "service.vendor=IBM")
public class JwtSsoComponent implements JwtSsoConfig {

	private static final TraceComponent tc = Tr.register(JwtSsoComponent.class);

	private DynamicMBean httpsendpointInfoMBean;

	private DynamicMBean httpendpointInfoMBean;

	private ServerInfoMBean serverInfoMBean;

	private boolean setCookiePathToWebAppContextPath;
	private boolean includeLtpaCookie;
	private boolean fallbackToLtpa;
	private String jwtBuilderRef;
	private String jwtConsumerRef;
	private WebAppSecurityConfig webAppSecConfig;

	@Override
	public boolean isHttpOnlyCookies() {
		return WebConfigUtils.getWebAppSecurityConfig().getHttpOnlyCookies();
	}

	@Override
	public boolean isSsoUseDomainFromURL() {
		return WebConfigUtils.getWebAppSecurityConfig().getSSOUseDomainFromURL();
	}

	@Override
	public boolean isSsoRequiresSSL() {
		return WebConfigUtils.getWebAppSecurityConfig().getSSORequiresSSL();
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
	public String getJwtBuilderRef() {
		return jwtBuilderRef;
	}

	@Override
	public String getJwtConsumerRef() {
		return jwtConsumerRef;
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

	@Activate
	protected void activate(Map<String, Object> properties, ComponentContext cc) {
		System.out.println("***** JWTSSO activate *****"); // todo: removeme
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
		if (props == null || props.isEmpty()) {
			return;
		}
		setCookiePathToWebAppContextPath = (Boolean) props.get(JwtSsoUtils.CFG_KEY_SETCOOKIEPATHTOWEBAPPCONTEXTPATH);
		includeLtpaCookie = (Boolean) props.get(JwtSsoUtils.CFG_KEY_INCLUDELTPACOOKIE);
		fallbackToLtpa = (Boolean) props.get(JwtSsoUtils.CFG_KEY_FALLBACKTOLTPA);
		jwtBuilderRef = JwtUtils.trimIt((String) props.get(JwtSsoUtils.CFG_KEY_JWTBUILDERREF));
		jwtConsumerRef = JwtUtils.trimIt((String) props.get(JwtSsoUtils.CFG_KEY_JWTCONSUMERREF));

	}

}

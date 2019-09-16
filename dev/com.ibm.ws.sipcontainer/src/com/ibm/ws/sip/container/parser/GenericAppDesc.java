/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.parser;

import java.util.List;

import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.ConnectionFactory;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;
import com.ibm.ws.javaee.dd.common.JMSDestination;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.ws.javaee.dd.common.Listener;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.dd.jsp.JSPConfig;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.*;

/**
 * A parent for the sipAppDesc. is a decorator for WebApp. 
 * This is used during the installation phase - we must return a WebApp even if there is no web.xml file.
 * if there is no web.xml the DefaultSipWebApp.
 * @author SAGIA
 *
 */
public class GenericAppDesc implements WebApp {


	protected WebApp _ddWebApp = null;
	public GenericAppDesc() {
		_ddWebApp = new DefaultSipWebApp();
	}
	public void setddWebApp(WebApp webApp) {
		_ddWebApp = webApp;
	}

	public List<Description> getDescriptions() {
		return _ddWebApp.getDescriptions();
	}

	public List<DisplayName> getDisplayNames() {
		return _ddWebApp.getDisplayNames();
	}

	public List<LifecycleCallback> getPostConstruct() {
		return _ddWebApp.getPostConstruct();
	}

	public List<Icon> getIcons() {
		return _ddWebApp.getIcons();
	}

	public List<EnvEntry> getEnvEntries() {
		return _ddWebApp.getEnvEntries();
	}

	public List<LifecycleCallback> getPreDestroy() {
		return _ddWebApp.getPreDestroy();
	}

	public List<EJBRef> getEJBRefs() {
		return _ddWebApp.getEJBRefs();
	}

	public String getDeploymentDescriptorPath() {
		return _ddWebApp.getDeploymentDescriptorPath();
	}

	public Object getComponentForId(String id) {
		return _ddWebApp.getComponentForId(id);
	}

	public List<EJBRef> getEJBLocalRefs() {
		return _ddWebApp.getEJBLocalRefs();
	}

	public String getIdForComponent(Object ddComponent) {
		return _ddWebApp.getIdForComponent(ddComponent);
	}

	public String getVersion() {
		return _ddWebApp.getVersion();
	}

	public boolean isSetMetadataComplete() {
		return _ddWebApp.isSetMetadataComplete();
	}

	public List<ServiceRef> getServiceRefs() {
		return _ddWebApp.getServiceRefs();
	}

	public boolean isSetDistributable() {
		return _ddWebApp.isSetDistributable();
	}

	public List<ResourceRef> getResourceRefs() {
		return _ddWebApp.getResourceRefs();
	}

	public boolean isMetadataComplete() {
		return _ddWebApp.isMetadataComplete();
	}

	public List<ParamValue> getContextParams() {
		return _ddWebApp.getContextParams();
	}

	public List<ResourceEnvRef> getResourceEnvRefs() {
		return _ddWebApp.getResourceEnvRefs();
	}

	public List<Filter> getFilters() {
		return _ddWebApp.getFilters();
	}

	public String getModuleName() {
		return _ddWebApp.getModuleName();
	}

	public List<FilterMapping> getFilterMappings() {
		return _ddWebApp.getFilterMappings();
	}

	public List<MessageDestinationRef> getMessageDestinationRefs() {
		return _ddWebApp.getMessageDestinationRefs();
	}

	public AbsoluteOrdering getAbsoluteOrdering() {
		return _ddWebApp.getAbsoluteOrdering();
	}

	public List<Listener> getListeners() {
		return _ddWebApp.getListeners();
	}

	public List<PersistenceContextRef> getPersistenceContextRefs() {
		return _ddWebApp.getPersistenceContextRefs();
	}

	public List<Servlet> getServlets() {
		return _ddWebApp.getServlets();
	}

	public List<ServletMapping> getServletMappings() {
		return _ddWebApp.getServletMappings();
	}

	public List<PersistenceUnitRef> getPersistenceUnitRefs() {
		return _ddWebApp.getPersistenceUnitRefs();
	}

	public SessionConfig getSessionConfig() {
		return _ddWebApp.getSessionConfig();
	}

	public List<DataSource> getDataSources() {
		return _ddWebApp.getDataSources();
	}

	public List<MimeMapping> getMimeMappings() {
		return _ddWebApp.getMimeMappings();
	}

	public List<JMSConnectionFactory> getJMSConnectionFactories() {
		return _ddWebApp.getJMSConnectionFactories();
	}

	public WelcomeFileList getWelcomeFileList() {
		return _ddWebApp.getWelcomeFileList();
	}

	public List<JMSDestination> getJMSDestinations() {
		return _ddWebApp.getJMSDestinations();
	}

	public List<ErrorPage> getErrorPages() {
		return _ddWebApp.getErrorPages();
	}

	public List<MailSession> getMailSessions() {
		return _ddWebApp.getMailSessions();
	}

	public JSPConfig getJSPConfig() {
		return _ddWebApp.getJSPConfig();
	}

	public List<ConnectionFactory> getConnectionFactories() {
		return _ddWebApp.getConnectionFactories();
	}

	public List<com.ibm.ws.javaee.dd.web.common.SecurityConstraint> getSecurityConstraints() {
		return _ddWebApp.getSecurityConstraints();
	}

	public List<AdministeredObject> getAdministeredObjects() {
		return _ddWebApp.getAdministeredObjects();
	}

	public LoginConfig getLoginConfig() {
		return _ddWebApp.getLoginConfig();
	}

	public List<SecurityRole> getSecurityRoles() {
		return _ddWebApp.getSecurityRoles();
	}

	public List<MessageDestination> getMessageDestinations() {
		return _ddWebApp.getMessageDestinations();
	}

	public LocaleEncodingMappingList getLocaleEncodingMappingList() {
		return _ddWebApp.getLocaleEncodingMappingList();
	}

	public boolean isSetDenyUncoveredHttpMethods() {
		
		return _ddWebApp.isSetDenyUncoveredHttpMethods();
	}

	@Override
	public DefaultContextPath getDefaultContextPath() {
		return null;
	}

	@Override
	public RequestEncoding getRequestEncoding() {
		return null;
	}

	@Override
	public ResponseEncoding getResponseEncoding() {
		return null;
	}




}

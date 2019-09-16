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

import java.util.Collections;
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
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;

/**
 * Default WebApp for sip applications. 
 * we must have a web app for each sip application if there is a sip.xml file. 
 * each function returns:
 * if the function should have return collection then empty list
 * if an object null
 * except couple of key methods
 * 
 * @author SAGIA
 *
 */
public class DefaultSipWebApp implements WebApp {

	@Override
	public List<DisplayName> getDisplayNames() {
		
		return Collections.emptyList();
	}

	@Override
	public List<Icon> getIcons() {
	
		return null;
	}

	@Override
	public List<Description> getDescriptions() {
		
		return Collections.emptyList();
	}

	@Override
	public String getDeploymentDescriptorPath() {
	
		return null;
	}

	@Override
	public Object getComponentForId(String id) {
	
		return null;
	}

	@Override
	public String getIdForComponent(Object ddComponent) {

		return null;
	}

	@Override
	public boolean isSetDistributable() {
		
		return false;
	}

	@Override
	public List<ParamValue> getContextParams() {

		return Collections.emptyList();
	}

	@Override
	public List<Filter> getFilters() {
		
		return Collections.emptyList();
	}

	@Override
	public List<FilterMapping> getFilterMappings() {
		
		return Collections.emptyList();
	}

	@Override
	public List<Listener> getListeners() {
		
		return Collections.emptyList();
	}

	@Override
	public List<Servlet> getServlets() {
		
		return Collections.emptyList();
	}

	@Override
	public List<ServletMapping> getServletMappings() {
		
		return Collections.emptyList();
	}

	@Override
	public SessionConfig getSessionConfig() {
		
		return null;
	}

	@Override
	public List<MimeMapping> getMimeMappings() {
		
		return Collections.emptyList();
	}

	@Override
	public WelcomeFileList getWelcomeFileList() {
		
		return null;
	}

	@Override
	public List<ErrorPage> getErrorPages() {
		
		return Collections.emptyList();
	}

	@Override
	public JSPConfig getJSPConfig() {
		
		return null;
	}

	@Override
	public List<SecurityConstraint> getSecurityConstraints() {
		
		return Collections.emptyList();
	}

	@Override
	public LoginConfig getLoginConfig() {
		
		return null;
	}

	@Override
	public List<SecurityRole> getSecurityRoles() {
		
		return Collections.emptyList();
	}

	@Override
	public List<MessageDestination> getMessageDestinations() {
		
		return Collections.emptyList();
	}

	@Override
	public LocaleEncodingMappingList getLocaleEncodingMappingList() {
		
		return null;
	}

	@Override
	public List<LifecycleCallback> getPostConstruct() {
		
		return Collections.emptyList();
	}

	@Override
	public List<LifecycleCallback> getPreDestroy() {
		
		return Collections.emptyList();
	}

	@Override
	public List<EnvEntry> getEnvEntries() {
	
		return Collections.emptyList();
	}

	@Override
	public List<EJBRef> getEJBRefs() {
	
		return Collections.emptyList();
	}

	@Override
	public List<EJBRef> getEJBLocalRefs() {
	
		return Collections.emptyList();
	}

	@Override
	public List<ServiceRef> getServiceRefs() {
		
		return Collections.emptyList();
	}

	@Override
	public List<ResourceRef> getResourceRefs() {
		
		return Collections.emptyList();
	}

	@Override
	public List<ResourceEnvRef> getResourceEnvRefs() {
		
		return Collections.emptyList();
	}

	@Override
	public List<MessageDestinationRef> getMessageDestinationRefs() {
	
		return Collections.emptyList();
	}

	@Override
	public List<PersistenceContextRef> getPersistenceContextRefs() {
		
		return Collections.emptyList();
	}

	@Override
	public List<PersistenceUnitRef> getPersistenceUnitRefs() {
	
		return Collections.emptyList();
	}

	@Override
	public List<DataSource> getDataSources() {

		return Collections.emptyList();
	}

	@Override
	public List<JMSConnectionFactory> getJMSConnectionFactories() {
	
		return Collections.emptyList();
	}

	@Override
	public List<JMSDestination> getJMSDestinations() {
		
		return Collections.emptyList();
	}

	@Override
	public List<MailSession> getMailSessions() {
	
		return Collections.emptyList();
	}

	@Override
	public List<ConnectionFactory> getConnectionFactories() {
		
		return Collections.emptyList();
	}

	@Override
	public List<AdministeredObject> getAdministeredObjects() {
		
		return Collections.emptyList();
	}

	/** return the version of servlets supported by sip container.
	 * currently 2.5
	 * @see com.ibm.ws.javaee.dd.web.WebApp#getVersion()
	 */
	@Override
	public String getVersion() {
		
		return "2.5";
	}


	@Override
	public boolean isSetMetadataComplete() {
		return false;
	}

	@Override
	public boolean isMetadataComplete() {
		return false;
	}

	@Override
	public String getModuleName() {
		return null;
	}

	@Override
	public AbsoluteOrdering getAbsoluteOrdering() {		
		return null;
	}

	@Override
	public boolean isSetDenyUncoveredHttpMethods() {
		
		return false;
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

/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.webcontainer;

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
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.ws.javaee.dd.web.common.ErrorPage;
import com.ibm.ws.javaee.dd.web.common.Filter;
import com.ibm.ws.javaee.dd.web.common.FilterMapping;
import com.ibm.ws.javaee.dd.web.common.LocaleEncodingMappingList;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.dd.web.common.MimeMapping;
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;
import com.ibm.ws.javaee.dd.web.common.Servlet;
import com.ibm.ws.javaee.dd.web.common.ServletMapping;
import com.ibm.ws.javaee.dd.web.common.SessionConfig;
import com.ibm.ws.javaee.dd.web.common.WelcomeFileList;

/**
 * The mock WebApp is used to imitate web.xm file for router web application.
 * It is always created and save in NoPersistenceCache.
 */
public class JaxWsRouterWebApp implements WebApp {

    private final String moduleName;

    public JaxWsRouterWebApp(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public String getModuleName() {
        return moduleName;
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
    public boolean isSetDenyUncoveredHttpMethods() {
        return false;
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
    public List<DisplayName> getDisplayNames() {
        return Collections.emptyList();
    }

    @Override
    public List<Icon> getIcons() {
        return Collections.emptyList();
    }

    @Override
    public List<Description> getDescriptions() {
        return Collections.emptyList();
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

    @Override
    public String getVersion() {
        return "3.0";
    }

    @Override
    public boolean isSetMetadataComplete() {
        return true;
    }

    @Override
    public boolean isMetadataComplete() {
        return true;
    }

    @Override
    public AbsoluteOrdering getAbsoluteOrdering() {
        return null;
    }
}

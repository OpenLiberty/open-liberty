/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.internal;

import java.util.Collections;
import java.util.List;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.clientcontainer.metadata.ClientModuleMetaData;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
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
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;

public class ClientModuleMetaDataImpl extends MetaDataImpl implements ClientModuleMetaData, ApplicationClient {
    protected ApplicationClient appClient;
    protected ModuleInfo moduleInfo;
    private final J2EEName j2eeName;

    public ClientModuleMetaDataImpl(ApplicationClient appClient, ModuleInfo moduleInfo, J2EEName j2eeName) {
        super(0);
        this.appClient = appClient;
        this.moduleInfo = moduleInfo;
        this.j2eeName = j2eeName;
    }

    @Override
    public String getName() {
        return moduleInfo.getName();
    }

    @Override
    public J2EEName getJ2EEName() {
        return j2eeName;
    }

    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return ((ExtendedApplicationInfo) moduleInfo.getApplicationInfo()).getMetaData();
    }

    @Override
    public ComponentMetaData[] getComponentMetaDatas() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    /** {@inheritDoc} */
    @Override
    public ApplicationClient getAppClient() {
        return appClient != null ? appClient : this;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // The following methods are implementations of the ApplicationClient interface.   //
    // If the client module contains an XML deployment descriptor (usually             //
    // META-INF/application-client.xml), then the appClient passed in to this object   //
    // will be non-null, and the above getAppClient() method will return it.  In the   //
    // case where there is no XML DD, then the appClient field will be null and this   //
    // class will provide default (annotation-only) responses to the same              //
    // ApplicationClient methods.                                                      //
    /////////////////////////////////////////////////////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    public String getModuleName() {
        return moduleInfo.getName();
    }

    /** {@inheritDoc} */
    @Override
    public List<DisplayName> getDisplayNames() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<Icon> getIcons() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public String getDeploymentDescriptorPath() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Object getComponentForId(String id) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getIdForComponent(Object ddComponent) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<LifecycleCallback> getPostConstruct() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<LifecycleCallback> getPreDestroy() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<EnvEntry> getEnvEntries() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<EJBRef> getEJBRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<ServiceRef> getServiceRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<ResourceRef> getResourceRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<ResourceEnvRef> getResourceEnvRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<MessageDestinationRef> getMessageDestinationRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<PersistenceUnitRef> getPersistenceUnitRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<DataSource> getDataSources() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<JMSConnectionFactory> getJMSConnectionFactories() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<JMSDestination> getJMSDestinations() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<MailSession> getMailSessions() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<ConnectionFactory> getConnectionFactories() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<AdministeredObject> getAdministeredObjects() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public int getVersionID() {
        return ApplicationClient.VERSION_7;
    }

    /** {@inheritDoc} */
    @Override
    public List<EJBRef> getEJBLocalRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public List<PersistenceContextRef> getPersistenceContextRefs() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public String getCallbackHandler() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<MessageDestination> getMessageDestinations() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        return "7.0";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSetMetadataComplete() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMetadataComplete() {
        return false;
    }

}

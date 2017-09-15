/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ConnectorModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.javaee.dd.connector.Connector;
import com.ibm.ws.jca.metadata.ConnectorModuleMetaData;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * The adapter for ConnectorModuleMetadata
 */
//as documentation only at this point:
//@Component(pid="com.ibm.ws.jca.ConnectorModuleMetaDataAdapter")
public class ConnectorModuleMetaDataAdapter implements ContainerAdapter<ConnectorModuleMetaData> {

    private final AtomicServiceReference<J2EENameFactory> j2eeNameFactory = new AtomicServiceReference<J2EENameFactory>("j2eeNameFactory");

    public void setJ2eeNameFactory(ServiceReference<J2EENameFactory> ref) {
        j2eeNameFactory.setReference(ref);
    }

    public void unsetJ2eeNameFactory(ServiceReference<J2EENameFactory> ref) {
        j2eeNameFactory.unsetReference(ref);
    }

    protected void activate(ComponentContext cc) {
        j2eeNameFactory.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        j2eeNameFactory.deactivate(cc);
    }

    @Override
    public ConnectorModuleMetaData adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer,
                                         Container containerToAdapt) throws UnableToAdaptException {
        RaConnector connector = (RaConnector) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), Connector.class);
        if (connector == null) {
            connector = (RaConnector) containerToAdapt.adapt(Connector.class);
        }
        ConnectorModuleMetaDataImpl cmmd = (ConnectorModuleMetaDataImpl) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ConnectorModuleMetaData.class);
        if (cmmd == null) {
            ExtendedApplicationInfo appInfo = (ExtendedApplicationInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ApplicationInfo.class);
            ConnectorModuleInfo cmInfo = (ConnectorModuleInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ConnectorModuleInfo.class);
            cmmd = new ConnectorModuleMetaDataImpl(appInfo, cmInfo, connector, j2eeNameFactory.getServiceWithException(), containerToAdapt);
            rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), ConnectorModuleMetaData.class, cmmd);
        }
        return cmmd;
    }
}

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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.config.WSConfigurationHelper;
import com.ibm.ws.container.service.app.deploy.ConnectorModuleInfo;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * The adapter for ConnectorModuleMetatype
 */
//as documentation only at this point:
//@Component(pid = "com.ibm.ws.jca.ConnectorModuleMetatypeAdapter")
public class ConnectorModuleMetatypeAdapter implements ContainerAdapter<ConnectorModuleMetatype> {

    private BundleContext bundleContext = null;

    private WSConfigurationHelper configurationHelper;

    private final ConcurrentHashMap<String, String> bootstrapContextFactoryPids = new ConcurrentHashMap<String, String>();

    /**
     * Mapping of resource adapter id to countdown latch that indicates when a metatype provider has been removed.
     */
    private final ConcurrentHashMap<String, CountDownLatch> metatypeRemovedLatches = new ConcurrentHashMap<String, CountDownLatch>();

    protected void activate(ComponentContext context) {
        bundleContext = PrivHelper.getBundleContext(context);
    }

    protected void deactivate(ComponentContext context) {

    }

    protected void setConfigurationHelper(WSConfigurationHelper configurationHelper) {
        this.configurationHelper = configurationHelper;
    }

    protected void unsetConfigurationHelper(WSConfigurationHelper configurationHelper) {
        this.configurationHelper = null;
    }

    @Override
    public ConnectorModuleMetatype adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer,
                                         Container containerToAdapt) throws UnableToAdaptException {
        ConnectorModuleMetatype cmmt = (ConnectorModuleMetatype) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ConnectorModuleMetatype.class);
        if (cmmt == null) {
            ConnectorModuleInfo cmInfo = (ConnectorModuleInfo) rootOverlay.getFromNonPersistentCache(artifactContainer.getPath(), ConnectorModuleInfo.class);
            cmmt = new ConnectorModuleMetatypeBundleImpl(bundleContext, cmInfo, configurationHelper, bootstrapContextFactoryPids, metatypeRemovedLatches);
            rootOverlay.addToNonPersistentCache(artifactContainer.getPath(), ConnectorModuleMetatype.class, cmmt);
        }
        return cmmt;
    }
}

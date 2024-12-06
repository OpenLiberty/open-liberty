/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.wsspi.anno.service.AppKey;

@Component
public class ApplicationKeyServiceImpl implements AnnotationService_KeyService, ApplicationMetaDataListener {

    static final TraceComponent tc = Tr.register(ApplicationKeyServiceImpl.class);
    private final boolean isDebug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();

    private final AppKey nullKey = new AppKey(null);
    private final Map<String, AppKey> keys = new ConcurrentHashMap<String, AppKey>();

    /**
     * {@inheritDoc}
     */
    @Override
    public com.ibm.wsspi.anno.service.AppKey getKeyForApp(String appName) {

        // appName will come from the input to
        // com.ibm.ws.container.service.annocache.internal.ClassSourceImpl_Aggregate.<init>

        // That is called by from AnnotationsImpl.createRootClassSource()

        // Which in turn gets the appName from AnnotationsImpl.setAppName()
        // There are two paths into that method.
        // CDIArchiveImpl calls it inside getAnnotatedClassesPostBeta()

        // CDIArchiveImpl gets the name from ApplicationInfo.getDeploymentName()

        // app manager calls it in EARDeployedAppInfo.hasAnnotationsPostBeta()
        // That class goes through a chain of getters ending in com.ibm.ws.app.manager.internal.ApplicationConfig
        // which reads the server.xml.
        // This will match ApplicationInfo.getDeploymentName()

        // The deployment name is also what we get from event.getMetaData().getJ2EEName().getApplication()
        // So we can add to the map here, and rely on applicationMetaDataDestroyed to remove items when they
        // are no longer needed.

        // However we know that the every path into this method uses the application deployment name for appName
        // this is the same string as we'll get from event.getMetaData().getJ2EEName().getApplication().
        // So we can create it now and it will be destroyed when the metadata is destroyed.
        if (isDebug && appName != null && !keys.containsKey(appName)) {
            Tr.debug(tc, "A key for for {0}, was not found. It will be created.");
        }

        if (appName == null) {
            return nullKey;
        }
        return keys.computeIfAbsent(appName, AppKey::new);
    }

    @Override
    @Trivial
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        // With EAR apps we first look for a key inside EARApplicationHandlerImpl.setUpApplicationMonitoring
        // before the application metadata is created inside EARApplicationHandlerImpl.install
        // Both methods are called from com.ibm.ws.app.manager.internal.statemachine.StartAction

        // Since this means applicationMetaDataCreated is called to late, we create our keys in getKeyForApp
        // And this is a no-op.
    }

    @Override
    @Trivial
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        String appDeploymentName = event.getMetaData().getJ2EEName().getApplication();
        if (isDebug) {
            Tr.debug(tc, "Destroying key for {0}, already exists? {1}", appDeploymentName, keys.containsKey(appDeploymentName));
        }
        keys.remove(appDeploymentName);
    }
}

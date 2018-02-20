/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.app.deploy.internal;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.metadata.extended.MetaDataGetter;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.wsspi.adaptable.module.Container;

/**
 *
 */
class ApplicationInfoImpl implements ExtendedApplicationInfo, MetaDataGetter<ApplicationMetaData> {

    private final String appName;
    private final ApplicationMetaData appMetaData;
    private final Container appContainer;
    private final NestedConfigHelper configHelper;
    private final boolean useJandex;

    //ApplicationInfoImpl(String appName, J2EEName j2eeName, Container appContainer, NestedConfigHelper configHelper) {
    //    this(appName, j2eeName, appContainer, configHelper, null);
    //}

    ApplicationInfoImpl(String appName, J2EEName j2eeName, Container appContainer, NestedConfigHelper configHelper, boolean useJandex) {
        this.appName = appName;
        this.appMetaData = new ApplicationMetaDataImpl(j2eeName);
        this.appContainer = appContainer;
        this.configHelper = configHelper;
        this.useJandex = useJandex;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + appName + ']';
    }

    @Override
    public String getName() {
        return appName;
    }

    @Override
    public boolean getUseJandex() {
        return useJandex;
    }

    @Override
    public Container getContainer() {
        return appContainer;
    }

    @Override
    public ApplicationMetaData getMetaData() {
        return appMetaData;
    }

    @Override
    public NestedConfigHelper getConfigHelper() {
        return configHelper;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.app.deploy.ApplicationInfo#getDeploymentName()
     */
    @Override
    public String getDeploymentName() {
        if (appMetaData.getJ2EEName() == null)
            return null;
        return appMetaData.getJ2EEName().getApplication();
    }

}

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public final class AppStructureHelper {
    public static final String getAppVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        Application app = ddAdaptRoot.adapt(Application.class);
        return ((app == null) ? null : app.getVersion());
    }

    public static final String getAppClientVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        ApplicationClient appClient = ddAdaptRoot.adapt(ApplicationClient.class);
        return ( (appClient == null) ? null : appClient.getVersion() );
    }

    public static final boolean isWebModule(OverlayContainer ddOverlay, ArtifactContainer ddArtifactRoot) {
        return ( ddOverlay.getFromNonPersistentCache(ddArtifactRoot.getPath(), WebModuleInfo.class) != null );
    }    
    
    public static final String getWebVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        WebApp webApp = ddAdaptRoot.adapt(WebApp.class);
        return ( (webApp == null) ? null : webApp.getVersion() );
    }
    
    public static final Integer getEJBVersion(Container ddAdaptRoot) throws UnableToAdaptException {
        EJBJar ejbJar = ddAdaptRoot.adapt(EJBJar.class);
        return ( (ejbJar == null) ? null : Integer.valueOf( ejbJar.getVersionID() ) ); 
    }
}

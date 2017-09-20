/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.osgi.container.config;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.DefaultContextPath;
import com.ibm.ws.javaee.dd.web.common.RequestEncoding;
import com.ibm.ws.javaee.dd.web.common.ResponseEncoding;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * WebAppConfigurator processes all the required web application related configurations from web.xml, web-fragment.xml and annotations.
 * and configure them into the WebAppConfiguration.
 */
public class WebAppConfiguratorHelper40 extends WebAppConfiguratorHelper {

    private static final TraceComponent tc = Tr.register(WebAppConfiguratorHelper40.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public WebAppConfiguratorHelper40(ServletConfigurator configurator,
                                      ResourceRefConfigFactory resourceRefConfigFactory, List<Class<?>> listenerInterfaces) {
        super(configurator, resourceRefConfigFactory, listenerInterfaces);

        WebModuleInfo moduleInfo = (WebModuleInfo) configurator.getFromModuleCache(WebModuleInfo.class);

        String moduleName = moduleInfo.getName();
        boolean isDefaultContextRootUsed = moduleInfo.isDefaultContextRootUsed();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "moduleName --> " + moduleName + " : isDefaultContextRootUsed --> " + isDefaultContextRootUsed);
        }

        webAppConfiguration.setDefaultContextRootUsed(isDefaultContextRootUsed);
    }

    @Override
    public void configureFromWebApp(WebApp webApp) throws UnableToAdaptException {
        super.configureFromWebApp(webApp);

        String methodName = "configureFromWebApp";
        String moduleName = webAppConfiguration.getDisplayName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + ": moduleName --> " + moduleName + "; defaultContextPathObject --> " + webApp.getDefaultContextPath());
        }
        /** If default context path is set, then configure it */
        if (webApp.getDefaultContextPath() != null)
            configureDefaultContextPath(webApp.getDefaultContextPath());

        configureRequestEncoding(webApp.getRequestEncoding());
        configureResponseEncoding(webApp.getResponseEncoding());
    }

    /**
     * Configuration of the default context path. The default context path
     * starts with a / character. If it is not rooted at the root of the
     * server’s name space, the path does not end with a / character.
     *
     * @param dcp
     */
    private void configureDefaultContextPath(DefaultContextPath dcp) {
        String methodName = "configureDefaultContextPath";
        String defaultContextPath = dcp.getValue();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + ": defaultContextPath --> " + defaultContextPath);
        }

        if (defaultContextPath != null && !defaultContextPath.isEmpty()) {
            webAppConfiguration.setDefaultContextPath(defaultContextPath);
        }
    }

    private void configureRequestEncoding(RequestEncoding reqEncoding) {
        if (reqEncoding != null) {
            String reqEnc = reqEncoding.getValue();
            if (!reqEnc.isEmpty())
                webAppConfiguration.setRequestEncoding(reqEnc);
        }
    }

    private void configureResponseEncoding(ResponseEncoding respEncoding) {
        if (respEncoding != null) {
            String respEnc = respEncoding.getValue();
            if (!respEnc.isEmpty())
                webAppConfiguration.setResponseEncoding(respEnc);
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.osgi.container.config;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfigurator.ConfigItem;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.AttributeValue;
import com.ibm.ws.javaee.dd.web.common.CookieConfig;
import com.ibm.ws.javaee.dd.web.common.SessionConfig;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.container.config.WebAppConfiguratorHelper40;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import io.openliberty.session.impl.SessionCookieConfigImpl60;
import jakarta.servlet.SessionCookieConfig;

public class WebAppConfiguratorHelper60 extends WebAppConfiguratorHelper40 {

    private static final TraceComponent tc = Tr.register(WebAppConfiguratorHelper60.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public WebAppConfiguratorHelper60(ServletConfigurator configurator,
                                      ResourceRefConfigFactory resourceRefConfigFactory, List<Class<?>> listenerInterfaces) {
        super(configurator, resourceRefConfigFactory, listenerInterfaces);

        WebModuleInfo moduleInfo = (WebModuleInfo) configurator.getFromModuleCache(WebModuleInfo.class);

        String moduleName = moduleInfo.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Constructor , web module--> " + moduleName);
        }

        if (webAppConfiguration.getSessionCookieConfig() == null) {
            SessionCookieConfig sessionCookieConfig = new SessionCookieConfigImpl60();
            webAppConfiguration.setSessionCookieConfig(sessionCookieConfig);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, " Created sessionCookieConfig [{0}]", sessionCookieConfig);
            }
        }
    }

    @Override
    public void configureFromWebApp(WebApp webApp) throws UnableToAdaptException {
        super.configureFromWebApp(webApp);

        configureSessionConfig6(webApp.getSessionConfig());
    }

    @Override
    public void configureFromWebFragment(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {
        super.configureFromWebFragment(webFragmentItem);

        configureSessionConfig6(webFragmentItem.getWebFragment().getSessionConfig());
    }

    /*
     * Extend parent configureSessionConfig() to set cookie-config attributes
     */
    private void configureSessionConfig6(SessionConfig sessionConfig) {
        if (sessionConfig == null) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "configureSessionConfig6", "");
        }

        Map<String, ConfigItem<String>> sessionConfigItemMap = configurator.getConfigItemMap("session-config");
        CookieConfig cookieConfig = sessionConfig.getCookieConfig();

        if (cookieConfig != null) {
            SessionCookieConfigImpl60 sessionCookieConfigImpl = (SessionCookieConfigImpl60) webAppConfiguration.getSessionCookieConfig();

            List<AttributeValue> attributes = cookieConfig.getAttributes();
            if (attributes != null) {
                String attName;
                String attValue;
                Iterator<AttributeValue> attIterator = attributes.iterator();
                while (attIterator.hasNext()) {
                    AttributeValue attribute = attIterator.next();
                    attName = attribute.getAttributeName();
                    attValue = attribute.getAttributeValue();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, " configureSessionConfig6 , attribute name [{0}] , attribute value [{1}]", attName, attValue);
                    }
                    ConfigItem<String> existedAttValue = sessionConfigItemMap.get(attName);

                    if (existedAttValue == null) {
                        sessionCookieConfigImpl.setAttribute(attName, attValue, false);
                        sessionConfigItemMap.put(attName, createConfigItem(attValue));
                    } else {
                        validateDuplicateConfiguration("cookie-config", attName, attValue, existedAttValue);
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "configureSessionConfig6");
        }
    }
}

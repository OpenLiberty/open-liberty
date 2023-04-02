/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.ws.javaee.dd.web.common.AttributeValue;
import com.ibm.ws.javaee.dd.web.common.CookieConfig;
import com.ibm.ws.javaee.dd.web.common.SessionConfig;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.container.config.WebAppConfiguratorHelper40;

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
            Tr.debug(tc, "WebAppConfiguratorHelper60 Constructor , web module--> " + moduleName);
        }
    }

    @Override
    protected void configureSessionConfig(SessionConfig sessionConfig) {
        if (sessionConfig == null) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(tc, "WebAppConfiguratorHelper60 configureSessionConfig", "");
        }

        CookieConfig cookieConfig = sessionConfig.getCookieConfig();

        if (cookieConfig != null) {
            SessionCookieConfig sessionCookieConfigImpl = webAppConfiguration.getSessionCookieConfig();

            //create SessionCookieConfigImpl60 only if the <cookie-config> presents in web.xml
            // AND if the webApp does not have SCC yet. create here so the super can use it.
            if (sessionCookieConfigImpl == null) {
                sessionCookieConfigImpl = new SessionCookieConfigImpl60();
                webAppConfiguration.setSessionCookieConfig(sessionCookieConfigImpl);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, " Created sessionCookieConfig [{0}]", sessionCookieConfigImpl);
                }
            }

            super.configureSessionConfig(sessionConfig);

            Map<String, ConfigItem<String>> sessionConfigItemMap = configurator.getConfigItemMap("session-config");

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
                        Tr.debug(tc, " configureSessionConfig , attribute name [{0}] , attribute value [{1}]", attName, attValue);
                    }
                    ConfigItem<String> existedAttValue = sessionConfigItemMap.get(attName);

                    if (existedAttValue == null) {
                        ((SessionCookieConfigImpl60) sessionCookieConfigImpl).setAttribute(attName, attValue, false);
                        sessionConfigItemMap.put(attName, createConfigItem(attValue));
                    } else {
                        validateDuplicateConfiguration("cookie-config", attName, attValue, existedAttValue);
                    }
                }
            }
        } else {
            super.configureSessionConfig(sessionConfig);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "configureSessionConfig");
        }
    }

    @Override
    public void finish() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, " finish");
        }

        super.finish();
        webAppConfiguration.setSkipEncodedCharVerification();
    }
}

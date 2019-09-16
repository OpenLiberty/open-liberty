/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

//@Component(service = { ConfigSource.class }, configurationPid = "com.ibm.ws.microprofile.metrics.config", configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, property = { "service.vendor=IBM" })
public class MetricAppNameConfigSource implements ConfigSource {

    private static final String METRICS_APPNAME_CONFIG_KEY = "mp.metrics.appName";

    private static final int CONFIG_ORDINAL = 80;

    private static Map<String, String> applicationContextRootMap = new HashMap<String, String>();

    @Override
    public int getOrdinal() {
        return CONFIG_ORDINAL;
    }

    @Override
    public String getName() {
        return "Metric Instrumented Application's Name";
    }

    @Override
    public Set<String> getPropertyNames() {
        return applicationContextRootMap.keySet();
    }

    @Override
    public Map<String, String> getProperties() {
        return applicationContextRootMap;
    }

    @Override
    public String getValue(String propertyName) {
        if (propertyName.equals(METRICS_APPNAME_CONFIG_KEY)) {
            String appName = null;
            String contextRoot = null;

            //Will resolve contextRoot if we're running in a WAR
            contextRoot = resolveContextRoot();

            /*
             * If contextRoot is null, maybe running in a Jar
             * in which case we need to resolve to
             * <ApplicationName>#<moduleName>.jar
             * as the contextRoot
             */
            if (contextRoot == null) {
                contextRoot = resolveApplicationModuleString();
            }
            return contextRoot;
        }
        return null;
    }

    /**
     * Resolves the Application name and Module name and
     * concatenates together with a #.
     *
     * @return String <ApplicationName>#<ModuleName>[.jar]
     */
    private String resolveApplicationModuleString() {

        String applicationModuleString = null;
        try {
            String applicationName;
            String modulename;
            ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            applicationName = cmdai.getComponentMetaData().getJ2EEName().getApplication();
            modulename = cmdai.getComponentMetaData().getJ2EEName().getModule();
            applicationModuleString = applicationName + "#" + modulename;

        } catch (NullPointerException e) {
        } catch (Exception e) {
        }
        return applicationModuleString;
    }

    /**
     *
     * @return String contexRoot of the Web Application
     */
    private String resolveContextRoot() {
        String contextRoot = null;
        try {
            ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

            ModuleMetaData mmd = cmdai.getComponentMetaData().getModuleMetaData();
            if (mmd instanceof WebModuleMetaData) {
                WebModuleMetaData wmmd = (WebModuleMetaData) mmd;
                WebAppConfig appCfg = wmmd.getConfiguration();
                contextRoot = appCfg.getContextRoot();
            }
        } catch (NullPointerException e) {
        } catch (Exception e) {
        }
        return contextRoot;
    }
}

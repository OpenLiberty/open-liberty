/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.util;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * This class contains methods for getting web app config information
 */
public class WebConfigUtils {
    private static ThreadLocal<MetaDataThreadContext> metaDataThreadLocal = new MetaDataThreadLocal();

    public static final String ATTR_WEB_MODULE_METADATA = "com.ibm.ws.webcontainer.security.webmodulemetadata";

    /**
     * Get the web application config
     *
     * @return the web application config
     */
    public static WebAppConfig getWebAppConfig() {
        WebAppConfig wac = null;
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd instanceof WebComponentMetaData) { // Only get the header for web modules, i.e. not for EJB
            WebModuleMetaData wmmd = (WebModuleMetaData) ((WebComponentMetaData) cmd).getModuleMetaData();
            wac = wmmd.getConfiguration();
            if (!(wac instanceof com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration)) {
                wac = null;
            }
        }
        return wac;
    }

    /**
     * Get the web app security config
     *
     * @return the web app security config
     */
    public static WebAppSecurityConfig getWebAppSecurityConfig() {
        return WebSecurityHelperImpl.getWebAppSecurityConfig();
    }

    /**
     * Get the security metadata
     *
     * @return the security metadata
     */
    public static SecurityMetadata getSecurityMetadata() {
        SecurityMetadata secMetadata = null;
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        if (mmd instanceof WebModuleMetaData) {
            secMetadata = (SecurityMetadata) ((WebModuleMetaData) mmd).getSecurityMetaData();
        } else {
            // ejb environment, check threadlocal.
            WebModuleMetaData wmmd = getWebModuleMetaData();
            if (wmmd != null) {
                secMetadata = (SecurityMetadata) wmmd.getSecurityMetaData();
            }
        }
        return secMetadata;
    }

    public static void setWebModuleMetaData(Object key, WebModuleMetaData wmmd) {
        MetaDataThreadContext mdtc = metaDataThreadLocal.get();
        if (mdtc == null) {
            mdtc = new MetaDataThreadContext();
            metaDataThreadLocal.set(mdtc);
        }
        mdtc.setMetaData(key, wmmd);
    }

    public static WebModuleMetaData getWebModuleMetaData() {
        MetaDataThreadContext mdtc = metaDataThreadLocal.get();
        if (mdtc != null) {
            return mdtc.getMetaData();
        }
        return null;
    }

    public static void removeWebModuleMetaData(Object key) {
        MetaDataThreadContext mdtc = metaDataThreadLocal.get();
        if (mdtc != null) {
            mdtc.clearMetaData(key);
        }
    }

    // For unit test only
    static void resetMetaData() {
        metaDataThreadLocal = new MetaDataThreadLocal();
    }

    private static final class MetaDataThreadLocal extends ThreadLocal<MetaDataThreadContext> {
        @Override
        protected MetaDataThreadContext initialValue() {
            return new MetaDataThreadContext();
        }
    }

}

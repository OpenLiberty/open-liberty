/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.internal.WebSecurityHelperImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.webcontainer.metadata.WebCollaboratorComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * This class contains methods for getting web app config information
 */
public class WebConfigUtils {

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
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (!(cmd instanceof WebComponentMetaData || cmd instanceof WebCollaboratorComponentMetaData)) {
            // if there is not webcollaborator on the top of threadcontext, peek one of these classes in the stack.
            cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData(WebComponentMetaData.class);
            if (cmd == null) {
                cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData(WebCollaboratorComponentMetaData.class);
            }
        }
        if (cmd != null) {
            WebModuleMetaData wmmd = (WebModuleMetaData) cmd.getModuleMetaData();
            secMetadata = (SecurityMetadata) wmmd.getSecurityMetaData();
        }
        return secMetadata;

    }
}

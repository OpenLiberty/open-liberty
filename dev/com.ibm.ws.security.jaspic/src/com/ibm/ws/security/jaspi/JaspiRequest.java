/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaspi;

import java.util.List;

import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.webcontainer.security.WebRequest;
import com.ibm.ws.webcontainer.security.WebSecurityContext;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * This class contains the JASPI related items that on twas
 * were added to the WebRequest object.
 */
public class JaspiRequest {

    private WebRequest webRequest = null;
    private MessageInfo msgInfo = null;
    private boolean isLogoutMethod = false;
    private String userid = null;
    @Sensitive
    private String password = null;
    private String appContext = null;
    private String appName = null;
    private String moduleName = null;
    private WebAppConfig wac = null;

    public JaspiRequest(WebRequest webRequest, WebAppConfig wac) {
        this.webRequest = webRequest;
        this.wac = wac;
    }

    public String getAppContext() {
        if (appContext == null) {
            String vHost = null;
            String contextRoot = null;
            WebAppConfig appCfg = WebConfigUtils.getWebAppConfig();
            if (appCfg != null) {
                vHost = appCfg.getVirtualHostName();
                contextRoot = appCfg.getContextRoot();
                appContext = vHost + " " + contextRoot;
            } else {
                if (wac != null) {
                    vHost = wac.getVirtualHostName();
                    contextRoot = wac.getContextRoot();
                    appContext = vHost + " " + contextRoot;
                }
            }
        }
        return appContext;
    }

    public WebSecurityContext getWebSecurityContext() {
        return webRequest.getWebSecurityContext();
    }

    public MessageInfo getMessageInfo() {
        return msgInfo;
    }

    public void setMessageInfo(MessageInfo messageInfo) {
        msgInfo = messageInfo;
    }

    public HttpServletRequest getHttpServletRequest() {
        return webRequest.getHttpServletRequest();
    }

    public HttpServletResponse getHttpServletResponse() {
        return webRequest.getHttpServletResponse();
    }

    public boolean isLogoutMethod() {
        return isLogoutMethod;
    }

    public void setLogoutMethod(boolean isLogout) {
        isLogoutMethod = isLogout;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String id) {
        userid = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(@Sensitive String pwd) {
        password = pwd;
    }

    public LoginConfiguration getLoginConfig() {
        return webRequest.getLoginConfig();
    }

    /**
     * The request is protected if there are required roles
     * or it's not mapped everyones role
     * @return true if there is a proected url.
     */
    public boolean isProtected() {
        List<String> requiredRoles = null;
        return !webRequest.isUnprotectedURI() &&
               webRequest.getMatchResponse() != null &&
               (requiredRoles = webRequest.getRequiredRoles()) != null &&
               !requiredRoles.isEmpty();
    }

    /**
     * Per section 3.9.3 of the JSR-196 (JASPIC) specification, when handling an HttpServletRequest.authenticate:
     * The MessageInfo map must unconditionally contain the javax.security.auth.message.MessagePolicy.isMandatory key (with associated true value).
     */
    public boolean isMandatory() {
        return isProtected() || webRequest.isRequestAuthenticate();
    }

    public String getApplicationName() {
        if (appName == null) {
            WebAppConfig appCfg = WebConfigUtils.getWebAppConfig();
            if (appCfg != null) {
                appName = appCfg.getModuleName();
            } else {
                if (wac != null) {
                    appName = wac.getModuleName();
                }
            }
        }
        return appName;
    }

    public String getModuleName() {
        if (moduleName == null) {
            WebAppConfig appCfg = WebConfigUtils.getWebAppConfig();
            if (appCfg != null) {
                moduleName = appCfg.getModuleName();
            } else {
                if (wac != null) {
                    moduleName = wac.getModuleName();
                }
            }
        }
        return moduleName;
    }

    public WebRequest getWebRequest() {
        return webRequest;
    }

}

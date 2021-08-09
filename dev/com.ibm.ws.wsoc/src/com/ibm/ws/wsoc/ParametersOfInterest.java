/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.servlet.http.HttpSession;
import javax.websocket.Extension;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.http.dispatcher.classify.DecoratedExecutorThread;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

public class ParametersOfInterest {

    private URI requestURI = null;
    private String queryString = null;
    private String sessionID = null;
    private String wsocProtocolVersion = null;
    private Principal userPrincipal = null;
    private Map<String, Object> userProperties = null;
    private Map<String, List<String>> parameterMap = null;

    private String agreedSubProtocol = null;
    private List<String> localSubProtocolList = null;

    private List<Extension> negotiatedExtensions = Collections.emptyList();

    private EndpointManager endpointManager = null;
    private ClassLoader tccl = null;
    private ComponentMetaData cmd = null;
    private boolean isSecure = false;

    private HttpSession httpSession = null;

    private Executor executor = null;

    public ParametersOfInterest() {
        // Get an associated Classified Executor if present
        this.setExecutor(DecoratedExecutorThread.getExecutor());
    }

    public void setURI(URI value) {
        requestURI = value;
    }

    public URI getURI() {
        return requestURI;
    }

    public void setQueryString(String value) {
        queryString = value;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setUserPrincipal(@Sensitive Principal value) {
        userPrincipal = value;
    }

    @Sensitive
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    public void setParameterMap(Map<String, List<String>> value) {
        parameterMap = value;
    }

    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }

    public void setSessionID(String value) {
        sessionID = value;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setHttpSession(HttpSession value) {
        httpSession = value;
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public void setWsocProtocolVersion(String value) {
        wsocProtocolVersion = value;
    }

    public String getWsocProtocolVersion() {
        return wsocProtocolVersion;
    }

    public void setUserProperties(@Sensitive Map<String, Object> value) {
        userProperties = value;
    }

    @Sensitive
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }

    public void setAgreedSubProtocol(String value) {
        agreedSubProtocol = value;
    }

    public String getAgreedSubProtocol() {
        return agreedSubProtocol;
    }

    public void setNegotiatedExtensions(List<Extension> value) {
        if (value != null) {
            negotiatedExtensions = value;
        }
    }

    public List<Extension> getNegotiatedExtensions() {
        return negotiatedExtensions;
    }

    public void setLocalSubProtocols(List<String> value) {
        localSubProtocolList = value;
    }

    public List<String> getLocalSubProtocols() {
        return localSubProtocolList;
    }

    public void setEndpointManager(EndpointManager value) {
        endpointManager = value;
    }

    public EndpointManager getEndpointManager() {
        return endpointManager;
    }

    public void setTccl(ClassLoader value) {
        tccl = value;
    }

    public ClassLoader getTccl() {
        return tccl;
    }

    public void setCmd(ComponentMetaData value) {
        cmd = value;
    }

    public ComponentMetaData getCmd() {
        return cmd;
    }

    public void setSecure(boolean secure) {
        this.isSecure = secure;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public String getHttpSessionID() {
        if (httpSession != null) {
            return httpSession.getId();
        }
        return null;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return this.executor;
    }
}

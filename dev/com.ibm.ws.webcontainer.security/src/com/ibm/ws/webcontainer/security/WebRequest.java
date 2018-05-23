/*******************************************************************************
 * Copyright (c) 2011, 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.security.metadata.FormLoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;

/**
 * Encapsulates a Web request and associated data.
 * <p>An instance of this class is created during EJSWebCollaborator.preInvoke() before authentication is performed and
 * during WebCollaborator's authenticate() and login() methods.
 * It provides a convenient data-object for holding parameters & object references needed to perform authentication.
 *
 * @author IBM Corp.
 *
 */
public interface WebRequest {

    WebSecurityContext getWebSecurityContext();

    HttpServletRequest getHttpServletRequest();

    HttpServletResponse getHttpServletResponse();

    SecurityMetadata getSecurityMetadata();

    String getApplicationName();

    boolean isFormLoginRedirectEnabled();

    void disableFormLoginRedirect();

    List<String> getRequiredRoles();

    boolean isSSLRequired();

    boolean isAccessPrecluded();

    MatchResponse getMatchResponse();

    LoginConfiguration getLoginConfig();

    FormLoginConfiguration getFormLoginConfiguration();

    /**
     * Answers whether or not the web request has any authentication data.
     *
     * @return {@code true} if some authentication data is available, {@code false} otherwise.
     */
    boolean hasAuthenticationData();

    boolean isUnprotectedURI();

    public void setUnprotectedURI(boolean unprotectedURI);

    public boolean isProviderSpecialUnprotectedURI();

    public void setProviderSpecialUnprotectedURI(boolean specialUnprotectedURI);

    public boolean isCallAfterSSO();

    public void setCallAfterSSO(boolean callAfterSSO);

    public Map<String, Object> getProperties();

    public void setProperties(Map<String, Object> props);

    public boolean isRequestAuthenticate();

    public void setRequestAuthenticate(boolean requestAuthenticate);

    public boolean isDisableClientCertFailOver();

    public void setDisableClientCertFailOver(boolean isDisable);

}
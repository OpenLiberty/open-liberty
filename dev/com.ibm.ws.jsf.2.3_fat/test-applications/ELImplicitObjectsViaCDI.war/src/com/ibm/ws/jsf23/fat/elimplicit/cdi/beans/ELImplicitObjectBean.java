/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.elimplicit.cdi.beans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.faces.annotation.ApplicationMap;
import javax.faces.annotation.FacesConfig;
import javax.faces.annotation.FlowMap;
import javax.faces.annotation.HeaderMap;
import javax.faces.annotation.HeaderValuesMap;
import javax.faces.annotation.InitParameterMap;
import javax.faces.annotation.RequestCookieMap;
import javax.faces.annotation.RequestMap;
import javax.faces.annotation.RequestParameterMap;
import javax.faces.annotation.RequestParameterValuesMap;
import javax.faces.annotation.SessionMap;
import javax.faces.annotation.ViewMap;
import javax.faces.application.FacesMessage;
import javax.faces.application.ResourceHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

/**
 * Bean that tests if EL implicit objects are injectable
 */
@Named("elImplicitObjectBean")
@RequestScoped
@FacesConfig
public class ELImplicitObjectBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(ELImplicitObjectBean.class.getName());

    @Inject
    private FacesContext facesContext; // #{facesContext}

    @Inject
    private ExternalContext externalContext; // #{externalContext}

    @Inject
    private UIViewRoot viewRoot; // #{view}

    @Inject
    private ServletContext servletContext; // #{application}

    @Inject
    private Flash flash; // #{flash}

    @Inject
    private HttpSession httpSession; // #{session}

    @Inject
    @ApplicationMap
    private Map<String, Object> applicationMap; // #{applicationScope}

    @Inject
    @SessionMap
    private Map<String, Object> sessionMap; // #{sessionScope}

    @Inject
    @ViewMap
    private Map<String, Object> viewMap; // #{viewScope}

    @Inject
    @RequestMap
    private Map<String, Object> requestMap; // #{requestScope}

    @Inject
    @FlowMap
    private Map<Object, Object> flowMap; // #{flowScope}

    @Inject
    @HeaderMap
    private Map<String, String> headerMap; // #{header}

    @Inject
    @RequestCookieMap
    private Map<String, Object> cookieMap; // #{cookie}

    @Inject
    @InitParameterMap
    private Map<String, String> initParameterMap; // #{initParam}

    @Inject
    @RequestParameterMap
    private Map<String, String> requestParameterMap; // #{param}

    @Inject
    @RequestParameterValuesMap
    private Map<String, String[]> requestParameterValuesMap; // #{paramValues}

    @Inject
    @HeaderValuesMap
    private Map<String, String[]> headerValuesMap; // #{headerValues}

    @Inject
    private ResourceHandler resourceHandler; // #{"resource"}

    public void execute() {
        if (facesContext == null) {
            LOGGER.log(Level.INFO, "FacesContext was not initialized -> {0}", facesContext);
        } else {
            facesContext.addMessage(null, new FacesMessage("FacesContext project stage: " + facesContext.getApplication().getProjectStage()));

            facesContext.addMessage(null, new FacesMessage("ServletContext context path: " + servletContext.getContextPath()));

            facesContext.addMessage(null, new FacesMessage("ExternalContext app context path: " + externalContext.getApplicationContextPath()));

            facesContext.addMessage(null, new FacesMessage("UIViewRoot viewId: " + viewRoot.getViewId()));

            facesContext.addMessage(null, new FacesMessage("Flash isRedirect: " + flash.isRedirect()));

            facesContext.addMessage(null, new FacesMessage("HttpSession isNew: " + httpSession.isNew()));

            facesContext.addMessage(null, new FacesMessage("Application name from ApplicationMap: " + applicationMap.get("com.ibm.websphere.servlet.enterprise.application.name")));

            // handle jakarta/javax namespace switch
            Object charset = sessionMap.get("javax.faces.request.charset");

            // handle jakarta/javax namespace switch
            if(charset == null){
                charset = sessionMap.get("jakarta.faces.request.charset");
            }

            facesContext.addMessage(null, new FacesMessage("Char set from SessionMap: " +  charset ));

            facesContext.addMessage(null, new FacesMessage("ViewMap isEmpty: " + viewMap.isEmpty()));

            facesContext.addMessage(null, new FacesMessage("URI from RequestMap: " + requestMap.get("com.ibm.websphere.servlet.uri_non_decoded")));

            try {
                facesContext.addMessage(null, new FacesMessage("Flow map isEmpty: " + flowMap.isEmpty()));
            } catch (Exception e) {
                facesContext.addMessage(null, new FacesMessage("Flow map object is null: Exception: " + e.getMessage()));
            }

            facesContext.addMessage(null, new FacesMessage("Message from HeaderMap: " + headerMap.get("headerMessage")));

            facesContext.addMessage(null, new FacesMessage("Cookie object from CookieMap: " + cookieMap.get("JSESSIONID")));

            facesContext.addMessage(null, new FacesMessage("WELD_CONTEXT_ID_KEY from InitParameterMap: " + initParameterMap.get("WELD_CONTEXT_ID_KEY")));

            facesContext.addMessage(null, new FacesMessage("Message from RequestParameterMap: " + requestParameterMap.get("message")));

            facesContext.addMessage(null, new FacesMessage("Message from RequestParameterValuesMap: " + Arrays.toString(requestParameterValuesMap.get("message"))));

            facesContext.addMessage(null, new FacesMessage("Message from HeaderValuesMap: " + Arrays.toString(headerValuesMap.get("headerMessage"))));

            facesContext.addMessage(null, new FacesMessage("Resource handler JSF_SCRIPT_LIBRARY_NAME constant: " + resourceHandler.JSF_SCRIPT_LIBRARY_NAME));

        }
    }

}

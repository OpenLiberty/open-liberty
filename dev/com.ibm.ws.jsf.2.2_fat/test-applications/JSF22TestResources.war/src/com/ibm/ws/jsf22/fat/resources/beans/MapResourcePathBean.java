/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.resources.beans;

import java.net.URISyntaxException;
import java.net.URL;

import javax.faces.application.ResourceHandler;
import javax.faces.application.ViewResource;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;
import javax.faces.view.facelets.ResourceResolver;

/**
 * The test bean for mapping a viewId to resource path in order to be used in a Facelet view.
 */
@ManagedBean
@SessionScoped
public class MapResourcePathBean extends ResourceResolver {

    private final FacesContext facesContext = FacesContext.getCurrentInstance();

    public MapResourcePathBean() {}

    /**
     * Map a given viewId to a URL through the View Resource object
     * 
     * @param viewId the viewId (path) to the resource file
     * @return a URL object
     */
    @Override
    public URL resolveUrl(String viewId) {

        ResourceHandler resourceHandler = facesContext.getApplication().getResourceHandler();

        ViewDeclarationLanguage vdl = facesContext.getApplication().getViewHandler().getViewDeclarationLanguage(facesContext, viewId);
        ViewResource viewResource = null;
        if (vdl.viewExists(facesContext, viewId)) {
            viewResource = resourceHandler.createViewResource(facesContext, viewId);
        }

        // Get the URL of the resource
        if (viewResource != null) {
            return viewResource.getURL();
        }

        return null;
    }

    /**
     * Map a resource file to a URL by calling the ResourceResolver.resolveUrl method
     * so it can be used in a Facelet view
     * 
     * @param resourceName Name of the resource
     * @return a String containing the URI path used in the Facelet view
     * @throws URISyntaxException
     */
    public String mapViewIdToResource(String resourceName) throws URISyntaxException {
        // Construct the viewId of the resource file and map it to a URL
        URL url = this.resolveUrl("/WEB-INF/resources/templates/" + resourceName);
        return url.toURI().toString();
    }

}

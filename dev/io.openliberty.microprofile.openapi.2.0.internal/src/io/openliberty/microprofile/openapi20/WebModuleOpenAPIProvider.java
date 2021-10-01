/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;

public class WebModuleOpenAPIProvider implements OpenAPIProvider {

    // The WebModuleInfo for the application/module that the OpenAPI model was generated from
    private final WebModuleInfo webModuleInfo;
    
    // The OpenAPI model
    private final OpenAPI openAPIModel;
    
    /**
     * Constructor
     * 
     * @param webModuleInfo
     *          The WebModuleInfo for the application/module that the OpenAPI model was generated from
     * @param openAPIModel
     *          The OpenAPI model itself
     */
    public WebModuleOpenAPIProvider(final WebModuleInfo webModuleInfo, final OpenAPI openAPIModel) {
        this.webModuleInfo = webModuleInfo;
        this.openAPIModel = openAPIModel;
    }

    @Override
    public String getApplicationPath() {
        // Create the variable to return
        String applicationPath = null;

        /*
         * Now check if the model contains any paths and, if so, whether the first one starts with the context root.
         * If it does not, we use the context root as the application path and add it to the URL of the server. 
         */
        final String contextRoot = webModuleInfo.getContextRoot();
        Paths paths = openAPIModel.getPaths();
        if (  paths == null
           || paths.getPathItems() == null
           || paths.getPathItems().isEmpty()
           || !paths.getPathItems().keySet().iterator().next().startsWith(contextRoot)
           ) {
            applicationPath = contextRoot;
        }
        
        return applicationPath;
    }

    @Override
    public OpenAPI getModel() {
        return openAPIModel;
    }

    @Override
    public String toString() {
        return "WebModule [" + webModuleInfo.getApplicationInfo().getDeploymentName() + "/" + webModuleInfo.getName() + "]";
    }

    @Override
    public List<String> getMergeProblems() {
        return Collections.emptyList();
    }
}

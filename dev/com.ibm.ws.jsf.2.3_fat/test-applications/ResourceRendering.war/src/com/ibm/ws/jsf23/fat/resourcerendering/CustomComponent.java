/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.resourcerendering;

import javax.faces.application.FacesMessage;
import javax.faces.application.ResourceDependency;
import javax.faces.application.ResourceHandler;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.html.HtmlInputText;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

/**
 * Custom component that is being used to test new API methods
 * ResourceHandler.markResourceRendered(), ResourceHandler.isResourceRendered(),
 * and UIViewRoot.getComponentResources()
 */
@FacesComponent(createTag = true, namespace = "test")
@ResourceDependency(library = "css", name = "test-style.css")
public class CustomComponent extends HtmlInputText {

    @Override
    public void processEvent(ComponentSystemEvent event) {
        super.processEvent(event);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ResourceHandler resourceHanlder = facesContext.getApplication().getResourceHandler();
        UIViewRoot view = facesContext.getViewRoot();

        // verify if library css with resource test-style.css has been rendered
        facesContext.addMessage(null, new FacesMessage("Message from " + CustomComponent.class.getSimpleName() + ": isResourceRendered library=css name=test-style.css --> "
                                                       + resourceHanlder.isResourceRendered(facesContext, "test-style.css", "css")));

        // mark library css with resource test-style.css as rendered
        resourceHanlder.markResourceRendered(facesContext, "test-style.css", "css");

        // verify again if library css with resource test-style.css has been rendered. It should be true now.
        facesContext.addMessage(null, new FacesMessage("Message from " + CustomComponent.class.getSimpleName() + ": isResourceRendered library=css name=test-style.css --> "
                                                       + resourceHanlder.isResourceRendered(facesContext, "test-style.css", "css")));

        // get the component resources
        facesContext.addMessage(null, new FacesMessage("Message from " + CustomComponent.class.getSimpleName() + ": getComponentResources List size --> "
                                                       + view.getComponentResources(facesContext).size()));
    }
}

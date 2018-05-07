/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.resourcerendering;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * Simple request scoped bean
 */
@Named
@RequestScoped
public class MyBean {

    ResourceHandler resourceHanlder = null;
    FacesContext facesContext = null;

    @PostConstruct
    public void init() {
        facesContext = FacesContext.getCurrentInstance();
        resourceHanlder = facesContext.getApplication().getResourceHandler();
    }

    public void action() {
        verifyIsCSSLibTestStyleResourceRendered();
        verifyIsCSSLibAnotherResourceRendered();
        verifyIsAnotherLibTestStyleResourceRendered();
    }

    private void verifyIsCSSLibTestStyleResourceRendered() {
        // verify if library css with resource test-style.css has been rendered
        facesContext.addMessage(null, new FacesMessage("Message from " + MyBean.class.getSimpleName() + ": isResourceRendered library=css name=test-style.css --> "
                                                       + resourceHanlder.isResourceRendered(facesContext, "test-style.css", "css")));
    }

    private void verifyIsCSSLibAnotherResourceRendered() {
        // verify if library css with resource another.css (same library but inexistent resource) has been rendered
        facesContext.addMessage(null, new FacesMessage("Message from " + MyBean.class.getSimpleName() + ": isResourceRendered library=css name=another.css --> "
                                                       + resourceHanlder.isResourceRendered(facesContext, "another.css", "css")));
    }

    private void verifyIsAnotherLibTestStyleResourceRendered() {
        // verify if library another with resource test-style.css (inexistent library but same resource) has been rendered
        facesContext.addMessage(null, new FacesMessage("Message from " + MyBean.class.getSimpleName() + ": isResourceRendered library=another name=test-style.css --> "
                                                       + resourceHanlder.isResourceRendered(facesContext, "test-style.css", "another")));
    }

}

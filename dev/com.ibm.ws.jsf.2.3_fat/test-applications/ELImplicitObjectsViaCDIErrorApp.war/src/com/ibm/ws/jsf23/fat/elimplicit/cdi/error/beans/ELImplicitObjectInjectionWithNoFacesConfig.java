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
package com.ibm.ws.jsf23.fat.elimplicit.cdi.error.beans;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Bean that tests that EL implicit objects cannot be injected if annotation @FacesConfig is not present.
 */
@Named("elImplicitObjectWithNoFacesConfigBean")
@RequestScoped
public class ELImplicitObjectInjectionWithNoFacesConfig implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(ELImplicitObjectInjectionWithNoFacesConfig.class.getName());

    @Inject
    private FacesContext facesContext; // #{facesContext}

    public void execute() {
        if (facesContext == null) {
            LOGGER.log(Level.INFO, "FacesContext was not initialized -> {0}", facesContext);
        } else {
            facesContext.addMessage(null, new FacesMessage("FacesContext object: " + facesContext.toString()));

        }
    }

}

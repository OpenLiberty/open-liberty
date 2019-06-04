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
package com.ibm.ws.jsf22.fat.flashevents.flash;

import java.io.Serializable;

import javax.faces.FactoryFinder;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.FlashFactory;

@ManagedBean
@RequestScoped
public class EmployeeBean implements Serializable {

    private static final long serialVersionUID = 1L;
    protected String firstName = null;

    public EmployeeBean() {}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String goToPage2NoFlash() {
        //simple redirect to page2NoFlash.
        //This method is called from indexNoFlash        
        return "page2NoFlash?faces-redirect=true";
    }

    public String goToPage2Flash() {
        //getting the custom FlashFactory and Flash implementation and adding their class names to the 
        //Flash object which will then be displayed on page2Flash.
        //This method is called from indexFlash
        FlashFactory testFlashFactory = (FlashFactory) FactoryFinder.getFactory(FactoryFinder.FLASH_FACTORY);
        Flash flash = testFlashFactory.getFlash(true);
        flash.put("flashFactory", testFlashFactory.getClass());
        flash.put("flashImpl", flash.getClass());
        flash.put("firstName", firstName);

        return "page2Flash?faces-redirect=true";
    }

    public String goToPage2FlashAndKeep() {
        //getting the Flash implementation directly from the ExternalContext and adding its class name to the 
        //Flash object which will then be displayed on page2FlashAndKeep.
        //This method is called from indexFlashAndKeep
        Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        flash.put("flashImpl", flash.getClass());
        flash.put("firstName", firstName);

        return "page2FlashAndKeep?faces-redirect=true";
    }
}
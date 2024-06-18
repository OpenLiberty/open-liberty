/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.statelessview.beans.faces40;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Named;

/**
 * A simple CDI bean that will be used to test
 * stateless views.
 *
 * When running in the EE10 repeat, ManagedBeans are no longer available as of Faces 4.0. As such
 * this bean is a CDI bean. The bean has been explicitly put in the RequestScope as that is the default
 * scope when this bean was a @ManagedBean and did not explicitly state a scope. The default scope for a CDI
 * bean is @Dependent so using @RequestScoped ensures the test is testing the same functionality during the EE10 repeat.
 *
 * @author Bill Lucy
 *
 */
@Named
@RequestScoped
public class StatelessViewBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private String stateless;

    public void setStateless(String state) {
        stateless = state;
    }

    public String getStateless() {
        return stateless;
    }

    public void statelessQuestion() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        UIViewRoot uiViewRoot = facesContext.getViewRoot();
        Boolean isTransient = uiViewRoot.isTransient();
        Boolean isStateless = facesContext.getRenderKit().getResponseStateManager().isStateless(facesContext, null);
        String output = "isTransient returns " + isTransient.toString();
        output += " and isStateless returns  " + isStateless.toString();
        stateless = output;
    }
}

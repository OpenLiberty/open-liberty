/*
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
/**
 * A simple managed bean that will be used to test
 * a view scoped managed bean.
 *
 * @author Bill Lucy
 *
 */
package com.ibm.ws.jsf22.fat.statelessview.beans.faces40;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.inject.Named;

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

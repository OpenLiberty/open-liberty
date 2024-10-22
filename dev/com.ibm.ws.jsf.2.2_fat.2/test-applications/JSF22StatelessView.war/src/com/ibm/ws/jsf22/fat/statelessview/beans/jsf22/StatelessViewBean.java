/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.statelessview.beans.jsf22;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * A simple managed bean that will be used to test
 * stateless views.
 *
 * @author Bill Lucy
 *
 */
@ManagedBean
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

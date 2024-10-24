/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jsf.view.beans.faces40;

import java.io.Serializable;

import javax.inject.Named;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.enterprise.context.RequestScoped;

@Named("statelessViewBean")
@RequestScoped
public class StatelessViewBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private String stateless  = "someValue";

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

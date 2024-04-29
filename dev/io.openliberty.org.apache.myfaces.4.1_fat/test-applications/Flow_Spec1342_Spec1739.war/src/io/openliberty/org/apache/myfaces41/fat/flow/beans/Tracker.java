/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.flow.beans;

import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.flow.FlowScoped;
import jakarta.faces.flow.Flow;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.BeforeDestroyed;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

@Dependent
public class Tracker {

    public void observeInitialized(@Observes @Initialized(FlowScoped.class) Flow flow) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Initialized FLOW"));
    }

    /*
     * Not working -- Invesigation Needed
     */
    public void observeBeforeDestroyed(@Observes @BeforeDestroyed(FlowScoped.class) Flow flow) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("BeforeDestroyed FLOW"));
    }

    public void observeDestroyed(@Observes @Destroyed(FlowScoped.class) Flow flow) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Destroyed FLOW"));
    }
}

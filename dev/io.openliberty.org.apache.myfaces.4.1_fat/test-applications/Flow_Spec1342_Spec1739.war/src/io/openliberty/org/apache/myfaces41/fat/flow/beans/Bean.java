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

import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.UUID;
import jakarta.inject.Inject;

import jakarta.enterprise.context.SessionScoped;

import jakarta.faces.flow.Flow;

@SessionScoped
@Named("bean")
public class Bean implements Serializable {
    
    @Inject
    Flow currentFlow;

    public Flow getCurrentFlow() {
        return currentFlow;
    }

    // public void setCurrentFlow(Flow currentFlow) {
    //     this.currentFlow = currentFlow;
    // }

    
}

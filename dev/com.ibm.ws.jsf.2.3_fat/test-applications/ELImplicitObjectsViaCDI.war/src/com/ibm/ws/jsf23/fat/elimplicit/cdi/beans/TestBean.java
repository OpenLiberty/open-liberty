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
package com.ibm.ws.jsf23.fat.elimplicit.cdi.beans;

import java.io.Serializable;
import java.util.Map;

import javax.faces.annotation.FacesConfig;
import javax.faces.annotation.FlowMap;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.flow.FlowScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * A simple FlowScoped bean used to test basic functionality.
 */
@Named(value = "testBean")
@FlowScoped(value = "simpleBean")
@FacesConfig
public class TestBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private FacesContext facesContext; // #{facesContext}

    @Inject
    @FlowMap
    private Map<Object, Object> flowMap; // #{flowScope}

    public void test() {
        facesContext.addMessage(null, new FacesMessage("Flow map isEmpty: " + flowMap.isEmpty()));
    }
}

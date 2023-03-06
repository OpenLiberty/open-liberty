/*
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package com.ibm.ws.jsf22.fat.flows.beans.faces40;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@Named
@RequestScoped
public class InitializerBean {

    public InitializerBean() {
    }

    public void initialize() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<Object, Object> inboundParameters = facesContext.getApplication().getFlowHandler().getCurrentFlowScope();

        // See if the inbound-parameter is set correctly.
        String testParameter = (String) inboundParameters.get("testValue");
        if ((testParameter == null) || !testParameter.contains("test string"))
            throw new IllegalArgumentException("initializer:   did NOT find inbound-parameter");
    }
}

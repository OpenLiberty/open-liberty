/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.client.window;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.faces.context.FacesContext;
import jakarta.faces.flow.FlowScoped;
import jakarta.inject.Named;

@Named(value = "confirmNextBean")
@FlowScoped(value = ConfirmNext.flowID)
public class ConfirmNextBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String result = "";

    @PostConstruct
    public void init() {
        result = FacesContext.getCurrentInstance().getExternalContext().getClientWindow().getId();
    }

    @PreDestroy
    public void reset() {
        result = null;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}

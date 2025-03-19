/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.functionmapper;

import java.io.Serializable;

import javax.el.ELContext;
import javax.el.FunctionMapper;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@SessionScoped
@Named(value = "bean")
public class SampleBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean functionMapperExists = false;

    public Boolean getFunctionMapperExists() {
        return this.functionMapperExists;
    }

    public String execute() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ELContext elContext = facesContext.getELContext();
        FunctionMapper functionMapper = elContext.getFunctionMapper();
        System.out.println(functionMapper);
        if (functionMapper != null) {
            this.functionMapperExists = true;
        }
        return "/index.xhtml";
    }

}

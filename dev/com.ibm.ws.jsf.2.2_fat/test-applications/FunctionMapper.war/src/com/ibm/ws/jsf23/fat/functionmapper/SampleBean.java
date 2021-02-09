/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.functionmapper;

import javax.el.FunctionMapper;

import java.io.Serializable;

import javax.el.ELContext;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@SessionScoped
@Named(value="bean")
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
        if(functionMapper != null){
            this.functionMapperExists = true;
        }
        return "/index.xhtml";
    }

}

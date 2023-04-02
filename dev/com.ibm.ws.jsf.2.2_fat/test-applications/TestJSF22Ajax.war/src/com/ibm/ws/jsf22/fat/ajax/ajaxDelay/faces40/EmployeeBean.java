/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.ajax.ajaxDelay.faces40;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@Named
@RequestScoped
public class EmployeeBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected List<String> employees;
    protected String name;

    public EmployeeBean() {
        employees = Arrays.asList("john doe", "john jones", "joe smith");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMatchingEmployees() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.log("AjaxDelayTest getMatchingEmployees");

        if (name == null) {
            return null;
        }
        List<String> matches = new ArrayList<String>();
        for (String emp : employees) {
            if (emp.startsWith(name)) {
                matches.add(emp);
            }
        }
        return matches;
    }

}

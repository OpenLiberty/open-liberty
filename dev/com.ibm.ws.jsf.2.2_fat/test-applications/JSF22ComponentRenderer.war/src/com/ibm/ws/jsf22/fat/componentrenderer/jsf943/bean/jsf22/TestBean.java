/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.componentrenderer.jsf943.bean.jsf22;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean(name = "jsf943")
@SessionScoped
public class TestBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private Boolean exists = false;

    public Boolean getClassExists() {
        try {
            Class.forName("javax.faces.view.ViewDeclarationLanguageWrapper");
            this.exists = true;
        } catch (ClassNotFoundException e) {
            this.exists = false;
            e.printStackTrace();
        }
        return exists;
    }
}

/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.PH63238.bean;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@Named("testBean")
@RequestScoped
public class TestBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private boolean clicked = false;

    public TestBean() {
    }

    public String confirm() {
        System.out.println("confirm invoked!");
        return "result";
    }

    public void listener() {
        System.out.println("listener invoked!");
    }

}

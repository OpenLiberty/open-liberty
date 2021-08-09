/*
 * Copyright (c)  2015  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.miscbean.jar;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.html.HtmlCommandButton;

@ManagedBean(name = "miscellaneousViewScopeBean")
@ViewScoped
public class MiscellaneousViewScopeBean implements Serializable {
//    private static final Logger LOGGER = Logger.getLogger(MiscellaneousViewScopeBean.class.getName());
    private static final long serialVersionUID = -3118004082493064756L;

    private HtmlCommandButton button;
    private Integer counter = 0;

    public void setCounter(Integer results) {
        this.counter = results;
    }

    public String getCounter() {
        return ("PostConstruct counter = " + counter);
    }

    public HtmlCommandButton getButton() {
        return button;
    }

    public void setButton(HtmlCommandButton button) {
        this.button = button;
    }

    @PostConstruct
    public void init() {
        counter++;
    }

}
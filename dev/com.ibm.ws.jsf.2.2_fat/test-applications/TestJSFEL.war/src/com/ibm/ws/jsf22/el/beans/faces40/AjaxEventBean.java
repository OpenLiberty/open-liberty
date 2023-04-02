/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.el.beans.faces40;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * Test case for SPEC_PUBLIC-1026/SPEC_PUBLIC-1026 spec issue
 *
 */
@Named("ajaxeventbean")
@SessionScoped
public class AjaxEventBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ajaxEvent = "valueChange";

    private boolean checked = false;

    public void setAjaxEvent(String ajaxEvent) {
        this.ajaxEvent = ajaxEvent;
    }

    public String getAjaxEvent() {
        return ajaxEvent;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;

    }

}

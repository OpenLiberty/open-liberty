/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.miscbean;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

@FacesComponent(tagName = "valueBindingTag", createTag = true, namespace = "http://openliberty.io/jsf")
public class ValueBindingTag extends UIComponentBase {

    public ValueBindingTag() {
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ValueBinding test = getFacesContext().getApplication().createValueBinding("test");
        this.setValueBinding("test", test);
    }

    @Override
    public String getFamily() {
        return "test";
    }
}

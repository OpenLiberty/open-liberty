/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.componentrenderer.jsf703;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

@FacesComponent
public class Capitalize extends UIComponentBase {

    @Override
    public String getFamily() {
        return "test.component.capitalize";
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {

        String value = (String) getAttributes().get("value");

        if (value != null) {
            ResponseWriter writer = context.getResponseWriter();
            writer.write(value.toUpperCase());
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.componentrenderer.jsf599;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

/**
 * This is a simple JSF component. Its used by PlaceCompositeComponent to
 * test the programmatic creation of a component via the component type.
 */
@FacesComponent(value = "placeComponent")
public class PlaceComponent extends UIComponentBase {

    String value = null;

    @Override
    public String getFamily() {
        return "testComponents";
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.write("I'm a place!");
    }

    @Override
    public void encodeEnd(FacesContext arg0) throws IOException {
        super.encodeEnd(arg0);
    }
}

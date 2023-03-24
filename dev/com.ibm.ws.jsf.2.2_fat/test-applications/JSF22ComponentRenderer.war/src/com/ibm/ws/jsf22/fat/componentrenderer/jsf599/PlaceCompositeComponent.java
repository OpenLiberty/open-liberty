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
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

@FacesComponent(value = "placeCompositeComponent")
public class PlaceCompositeComponent extends UIComponentBase {

    @Override
    public String getFamily() {
        return "testComponents";
    }

    /**
     * The code here is testing programmatic creation of a JSF component using the component type.
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        UIComponent composite = context.getApplication().createComponent(context, "placeComponent", null);
        composite.setId("placeId");
        this.getChildren().add(composite);
    }

    @Override
    public void encodeEnd(FacesContext arg0) throws IOException {
        super.encodeEnd(arg0);
    }
}

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
package com.ibm.ws.jsf22.fat.componentrenderer.jsf599;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

@FacesComponent(value = "personCompositeComponent")
public class PersonCompositeComponent extends UIComponentBase {

    @Override
    public String getFamily() {
        return "testComponents";
    }

    /**
     * The code here is testing programmatic creation of a JSF component resource using a tag.
     */
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        java.util.Map<java.lang.String, java.lang.Object> attributes = new java.util.HashMap<java.lang.String, java.lang.Object>();
        UIComponent composite = context.getApplication().getViewHandler()
                        .getViewDeclarationLanguage(context, context.getViewRoot().getViewId())
                        .createComponent(context, "http://xmlns.jcp.org/jsf/composite/test", "person", attributes);
        composite.setId("personId");
        this.getChildren().add(composite);
    }

    @Override
    public void encodeEnd(FacesContext arg0) throws IOException {
        super.encodeEnd(arg0);
    }
}
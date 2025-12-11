/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.componentrenderer.myfaces4117;

import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

/*
 * For MYFACES-4117 - No specific tag name is specified, so "testFacesComponent"
 * will be used instead. 
 */
@FacesComponent(value=TestFacesComponent.COMPONENT_TYPE, createTag=true)
public class TestFacesComponent extends UIComponentBase {


    public static final String COMPONENT_FAMILY = "test.component";
    public static final String COMPONENT_TYPE = "custom.type";

    public TestFacesComponent() {
        setRendererType("test.renderer");
    }
    @Override
    public String getFamily() {        
        return COMPONENT_FAMILY;
    }
}

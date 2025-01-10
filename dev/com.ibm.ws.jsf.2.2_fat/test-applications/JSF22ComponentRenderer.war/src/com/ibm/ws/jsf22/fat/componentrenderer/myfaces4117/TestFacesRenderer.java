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

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;
import javax.faces.render.Renderer;

/*
 * No existing FATS use @FacesRenderer, so this is added to verify the annotation works.
 */
@FacesRenderer(componentFamily="test.component", rendererType="test.renderer")
public class TestFacesRenderer extends Renderer {

	public static final String RENDERER_TYPE = "test.renderer";

	@Override
	public boolean getRendersChildren() {
		return super.getRendersChildren();
	}

	@Override
	public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        System.out.println("Within TestFacesRenderer#encodeBegin");
		ResponseWriter writer = context.getResponseWriter();
        writer.startElement("div",component);
		writer.writeText("Renderer Works!", null);
		super.encodeBegin(context, component);
	}

	@Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
		System.out.println("Within TestFacesRenderer#encodeEnd");
		ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
		super.encodeEnd(context, component);
	}

}

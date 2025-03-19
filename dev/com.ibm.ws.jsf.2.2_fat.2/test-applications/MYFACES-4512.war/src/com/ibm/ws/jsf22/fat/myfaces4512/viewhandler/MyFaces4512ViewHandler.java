/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.myfaces4512.viewhandler;

import java.io.IOException;
import java.util.Locale;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * Custom ViewHandler for MYFACES-4512. We will not override the getViewDeclarationLanguage
 * method to ensure we use the default implementation from the javax.faces.application.ViewHandler
 * class which returns null.
 *
 * This should cause an NPE when navigating to a facelet view.
 */
public class MyFaces4512ViewHandler extends ViewHandler {

    private final ViewHandler wrapped;

    public MyFaces4512ViewHandler(ViewHandler viewHandler) {
        wrapped = viewHandler;
        System.out.println("MyFaces4512ViewHandler was invoked!");
    }

    @Override
    public Locale calculateLocale(FacesContext context) {
        return wrapped.calculateLocale(context);
    }

    @Override
    public String calculateRenderKitId(FacesContext context) {
        return wrapped.calculateRenderKitId(context);
    }

    @Override
    public UIViewRoot createView(FacesContext context, String viewId) {
        return wrapped.createView(context, viewId);
    }

    @Override
    public String getActionURL(FacesContext context, String viewId) {
        return wrapped.getActionURL(context, viewId);
    }

    @Override
    public String getResourceURL(FacesContext context, String path) {
        return wrapped.getResourceURL(context, path);
    }

    @Override
    public void renderView(FacesContext context, UIViewRoot viewToRender) throws IOException, FacesException {
        wrapped.renderView(context, viewToRender);
    }

    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId) {
        return wrapped.restoreView(context, viewId);
    }

    @Override
    public void writeState(FacesContext context) throws IOException {
        wrapped.writeState(context);
    }

}

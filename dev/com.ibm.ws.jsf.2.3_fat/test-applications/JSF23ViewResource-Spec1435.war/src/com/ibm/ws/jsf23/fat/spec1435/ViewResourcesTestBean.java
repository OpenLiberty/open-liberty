/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.spec1435;

import java.io.Serializable;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.ResourceHandler;
import javax.faces.application.ResourceVisitOption;
import javax.faces.application.ViewHandler;
import javax.faces.application.ViewVisitOption;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewDeclarationLanguage;
import javax.inject.Named;

/**
 * A RequestScoped bean that will be used to test Spec Issue 1435:
 * https://github.com/javaee/javaserverfaces-spec/issues/1435
 *
 * Test the following:
 *
 * ResourceHandler -> java.util.stream.Stream<java.lang.String> getViewResources(FacesContext facesContext, java.lang.String path, int maxDepth, ResourceVisitOption... options)
 * ResourceHandler -> java.util.stream.Stream<java.lang.String> getViewResources(FacesContext facesContext, java.lang.String path, ResourceVisitOption... options)
 *
 * ViewHandler -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, int maxDepth, ViewVisitOption... options)
 * ViewHandler -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, ViewVisitOption... options)
 *
 * ViewDeclarationLanguage -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, int maxDepth, ViewVisitOption... options)
 * ViewDeclarationLanguage -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, ViewVisitOption... options)
 *
 */
@Named
@RequestScoped
public class ViewResourcesTestBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    // ResourceHandler.getViewResources
    private String resourceHandlerViewResources;
    private String resourceHandlerViewResourcesOption;
    private String resourceHandlerViewResourcesDepth1;
    private String resourceHandlerViewResourcesDepth2;
    private String resourceHandlerViewResourcesDepthMax;
    private String resourceHandlerViewResourcesDepthTooLarge;
    private String resourceHandlerViewResourcesDepthTooSmall;

    // ViewHandler.getViewResources
    private String viewHandlerViewResources;
    private String viewHandlerViewResourcesOption;
    private String viewHandlerViewResourcesDepth1;
    private String viewHandlerViewResourcesDepth2;
    private String viewHandlerViewResourcesDepthMax;
    private String viewHandlerViewResourcesDepthTooLarge;
    private String viewHandlerViewResourcesDepthTooSmall;

    // ViewDeclarationLanguage
    private String vdlViewResources;
    private String vdlViewResourcesOption;
    private String vdlViewResourcesDepth1;
    private String vdlViewResourcesDepth2;
    private String vdlViewResourcesDepthMax;
    private String vdlViewResourcesDepthTooLarge;
    private String vdlViewResourcesDepthTooSmall;

    /**
     *
     */
    public ViewResourcesTestBean() {
        FacesContext context = FacesContext.getCurrentInstance();

        // ResourceHandler getViewResources
        ResourceHandler resourceHandler = context.getApplication().getResourceHandler();

        resourceHandlerViewResources = resourceHandler.getViewResources(context, "/").collect(Collectors.joining(","));
        resourceHandlerViewResourcesOption = resourceHandler.getViewResources(context, "/", ResourceVisitOption.TOP_LEVEL_VIEWS_ONLY).collect(Collectors.joining(","));
        resourceHandlerViewResourcesDepth1 = resourceHandler.getViewResources(context, "/", 1).collect(Collectors.joining(","));
        resourceHandlerViewResourcesDepth2 = resourceHandler.getViewResources(context, "/", 2).collect(Collectors.joining(","));
        resourceHandlerViewResourcesDepthMax = resourceHandler.getViewResources(context, "/", Integer.MAX_VALUE).collect(Collectors.joining(","));
        resourceHandlerViewResourcesDepthTooLarge = resourceHandler.getViewResources(context, "/", 10).collect(Collectors.joining(","));
        resourceHandlerViewResourcesDepthTooSmall = resourceHandler.getViewResources(context, "/", 0).collect(Collectors.joining(","));

        // ViewHandler getViews
        ViewHandler viewHandler = context.getApplication().getViewHandler();

        viewHandlerViewResources = viewHandler.getViews(context, "/").collect(Collectors.joining(","));
        viewHandlerViewResourcesOption = viewHandler.getViews(context, "/", ViewVisitOption.RETURN_AS_MINIMAL_IMPLICIT_OUTCOME).collect(Collectors.joining(","));
        viewHandlerViewResourcesDepth1 = viewHandler.getViews(context, "/", 1).collect(Collectors.joining(","));
        viewHandlerViewResourcesDepth2 = viewHandler.getViews(context, "/", 2).collect(Collectors.joining(","));
        viewHandlerViewResourcesDepthMax = viewHandler.getViews(context, "/", Integer.MAX_VALUE).collect(Collectors.joining(","));
        viewHandlerViewResourcesDepthTooLarge = viewHandler.getViews(context, "/", 10).collect(Collectors.joining(","));
        viewHandlerViewResourcesDepthTooSmall = viewHandler.getViews(context, "/", 0).collect(Collectors.joining(","));

        // ViewDeclarationLanguage getViews
        ViewDeclarationLanguage vdl = viewHandler.getViewDeclarationLanguage(context, "/index.xhtml");

        vdlViewResources = vdl.getViews(context, "/").collect(Collectors.joining(","));
        vdlViewResourcesOption = vdl.getViews(context, "/", ViewVisitOption.RETURN_AS_MINIMAL_IMPLICIT_OUTCOME).collect(Collectors.joining(","));
        vdlViewResourcesDepth1 = vdl.getViews(context, "/", 1).collect(Collectors.joining(","));
        vdlViewResourcesDepth2 = vdl.getViews(context, "/", 2).collect(Collectors.joining(","));
        vdlViewResourcesDepthMax = vdl.getViews(context, "/", Integer.MAX_VALUE).collect(Collectors.joining(","));
        vdlViewResourcesDepthTooLarge = vdl.getViews(context, "/", 10).collect(Collectors.joining(","));
        vdlViewResourcesDepthTooSmall = vdl.getViews(context, "/", 0).collect(Collectors.joining(","));
    }

    public void setResourceHandlerViewResources(String resourceHandlerViewResources) {
        this.resourceHandlerViewResources = resourceHandlerViewResources;
    }

    public String getResourceHandlerViewResources() {
        return this.resourceHandlerViewResources;
    }

    public void setResourceHandlerViewResourcesOption(String resourceHandlerViewResourcesOption) {
        this.resourceHandlerViewResourcesOption = resourceHandlerViewResourcesOption;
    }

    public String getResourceHandlerViewResourcesOption() {
        return this.resourceHandlerViewResourcesOption;
    }

    public void setResourceHandlerViewResourcesDepth1(String resourceHandlerViewResourcesDepth1) {
        this.resourceHandlerViewResourcesDepth1 = resourceHandlerViewResourcesDepth1;
    }

    public String getResourceHandlerViewResourcesDepth1() {
        return this.resourceHandlerViewResourcesDepth1;
    }

    public void setResourceHandlerViewResourcesDepth2(String resourceHandlerViewResourcesDepth2) {
        this.resourceHandlerViewResourcesDepth2 = resourceHandlerViewResourcesDepth2;
    }

    public String getResourceHandlerViewResourcesDepth2() {
        return this.resourceHandlerViewResourcesDepth2;
    }

    public void setResourceHandlerViewResourcesDepthMax(String resourceHandlerViewResourcesDepthMax) {
        this.resourceHandlerViewResourcesDepthMax = resourceHandlerViewResourcesDepthMax;
    }

    public String getResourceHandlerViewResourcesDepthMax() {
        return this.resourceHandlerViewResourcesDepthMax;
    }

    public void setResourceHandlerViewResourcesDepthTooLarge(String resourceHandlerViewResourcesDepthTooLarge) {
        this.resourceHandlerViewResourcesDepthTooLarge = resourceHandlerViewResourcesDepthTooLarge;
    }

    public String getResourceHandlerViewResourcesDepthTooLarge() {
        return this.resourceHandlerViewResourcesDepthTooLarge;
    }

    public void setResourceHandlerViewResourcesDepthTooSmall(String resourceHandlerViewResourcesDepthTooSmall) {
        this.resourceHandlerViewResourcesDepthTooSmall = resourceHandlerViewResourcesDepthTooSmall;
    }

    public String getResourceHandlerViewResourcesDepthTooSmall() {
        return this.resourceHandlerViewResourcesDepthTooSmall;
    }

    // ViewHandler
    public void setViewHandlerViewResources(String viewHandlerViewResources) {
        this.viewHandlerViewResources = viewHandlerViewResources;
    }

    public String getViewHandlerViewResources() {
        return this.viewHandlerViewResources;
    }

    public void setViewHandlerResourceHandlerViewResourcesOption(String viewHandlerViewResourcesOption) {
        this.viewHandlerViewResourcesOption = viewHandlerViewResourcesOption;
    }

    public String getViewHandlerViewResourcesOption() {
        return this.viewHandlerViewResourcesOption;
    }

    public void setViewHandlerViewResourcesDepth1(String viewHandlerViewResourcesDepth1) {
        this.viewHandlerViewResourcesDepth1 = viewHandlerViewResourcesDepth1;
    }

    public String getViewHandlerViewResourcesDepth1() {
        return this.viewHandlerViewResourcesDepth1;
    }

    public void setViewHandlerViewResourcesDepth2(String viewHandlerViewResourcesDepth2) {
        this.viewHandlerViewResourcesDepth2 = viewHandlerViewResourcesDepth2;
    }

    public String getViewHandlerViewResourcesDepth2() {
        return this.viewHandlerViewResourcesDepth2;
    }

    public void setViewHandlerViewResourcesDepthMax(String viewHandlerViewResourcesDepthMax) {
        this.viewHandlerViewResourcesDepthMax = viewHandlerViewResourcesDepthMax;
    }

    public String getViewHandlerViewResourcesDepthMax() {
        return this.viewHandlerViewResourcesDepthMax;
    }

    public void setViewHandlerViewResourcesDepthTooLarge(String viewHandlerViewResourcesDepthTooLarge) {
        this.viewHandlerViewResourcesDepthTooLarge = viewHandlerViewResourcesDepthTooLarge;
    }

    public String getViewHandlerViewResourcesDepthTooLarge() {
        return this.viewHandlerViewResourcesDepthTooLarge;
    }

    public void setViewHandlerViewResourcesDepthTooSmall(String viewHandlerViewResourcesDepthTooSmall) {
        this.viewHandlerViewResourcesDepthTooSmall = viewHandlerViewResourcesDepthTooSmall;
    }

    public String getViewHandlerViewResourcesDepthTooSmall() {
        return this.viewHandlerViewResourcesDepthTooSmall;
    }

    // ViewDeclationLanguage
    public void setVdlViewResources(String vdlViewResources) {
        this.vdlViewResources = vdlViewResources;
    }

    public String getVdlViewResources() {
        return this.vdlViewResources;
    }

    public void setVdlResourceHandlerViewResourcesOption(String vdlViewResourcesOption) {
        this.vdlViewResourcesOption = vdlViewResourcesOption;
    }

    public String getVdlViewResourcesOption() {
        return this.vdlViewResourcesOption;
    }

    public void setVdlViewResourcesDepth1(String vdlViewResourcesDepth1) {
        this.vdlViewResourcesDepth1 = vdlViewResourcesDepth1;
    }

    public String getVdlViewResourcesDepth1() {
        return this.vdlViewResourcesDepth1;
    }

    public void setVdlViewResourcesDepth2(String vdlViewResourcesDepth2) {
        this.vdlViewResourcesDepth2 = vdlViewResourcesDepth2;
    }

    public String getVdlViewResourcesDepth2() {
        return this.vdlViewResourcesDepth2;
    }

    public void setVdlViewResourcesDepthMax(String vdlViewResourcesDepthMax) {
        this.vdlViewResourcesDepthMax = vdlViewResourcesDepthMax;
    }

    public String getVdlViewResourcesDepthMax() {
        return this.vdlViewResourcesDepthMax;
    }

    public void setVdlViewResourcesDepthTooLarge(String vdlViewResourcesDepthTooLarge) {
        this.vdlViewResourcesDepthTooLarge = vdlViewResourcesDepthTooLarge;
    }

    public String getVdlViewResourcesDepthTooLarge() {
        return this.vdlViewResourcesDepthTooLarge;
    }

    public void setVdlViewResourcesDepthTooSmall(String vdlViewResourcesDepthTooSmall) {
        this.vdlViewResourcesDepthTooSmall = vdlViewResourcesDepthTooSmall;
    }

    public String getVdlViewResourcesDepthTooSmall() {
        return this.vdlViewResourcesDepthTooSmall;
    }
}

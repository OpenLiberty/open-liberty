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
package com.ibm.ws.jsf23.fat.searchexpression.beans;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.faces.annotation.FacesConfig;
import javax.faces.application.FacesMessage;
import javax.faces.component.ContextCallback;
import javax.faces.component.UIComponent;
import javax.faces.component.search.SearchExpressionContext;
import javax.faces.component.search.SearchExpressionHandler;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Simple RequestScoped bean that uses programmatic API of SearchExpressionHandler
 */
@Named
@RequestScoped
@FacesConfig
public class ProgrammaticSearchExpressionBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    private FacesContext facesContext;

    public void testProgrammatic() {

        SearchExpressionContext searchContext = SearchExpressionContext.createSearchExpressionContext(facesContext, facesContext.getViewRoot());

        SearchExpressionHandler handler = facesContext.getApplication().getSearchExpressionHandler();

        // test SearchExpressionHandler.resolveClientId()
        facesContext.addMessage(null,
                                new FacesMessage("TEST resolveClientId with search expression 'form1:@parent' -> "
                                                 + handler.resolveClientId(searchContext, "form1:@parent")));

        // test SearchExpressionHandler.resolveClientIds()
        facesContext.addMessage(null,
                                new FacesMessage("TEST resolveClientIds with search expression 'form1:inputFirstNameId' -> "
                                                 + handler.resolveClientIds(searchContext, "form1:inputFirstNameId").get(0)));

        // test SearchExpressionHandler.resolveComponent()
        handler.resolveComponent(
                                 searchContext,
                                 "form1:@parent",
                                 new ContextCallback() {
                                     @Override
                                     public void invokeContextCallback(FacesContext context, UIComponent target) {
                                         context.addMessage(null, new FacesMessage("TEST resolveComponent with search expression 'form1:@parent' -> "
                                                                                   + target.getId()));
                                     }
                                 });

        // test SearchExpressionHandler.resolveComponents()
        handler.resolveComponents(
                                  searchContext,
                                  "form1:@parent form1:submitButton",
                                  new ContextCallback() {
                                      @Override
                                      public void invokeContextCallback(FacesContext context, UIComponent target) {
                                          context.addMessage(null, new FacesMessage("TEST resolveComponents with search expression 'form1:@parent form1:submitButton' -> "
                                                                                    + target.getId()));
                                      }
                                  });

        // test SearchExpressionHandler.isValidExpression()
        facesContext.addMessage(null, new FacesMessage("TEST if expression 'form1:@parent' is valid -> " +
                                                       handler.isValidExpression(searchContext, "form1:@parent")));

        // test SearchExpressionHandler.isPassthroughExpression()
        facesContext.addMessage(null, new FacesMessage("TEST if expression 'form1:@parent' is passthrough -> " +
                                                       handler.isPassthroughExpression(searchContext, "form1:@parent")));

    }

}

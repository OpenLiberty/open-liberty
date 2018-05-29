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
package com.ibm.ws.jsf23.fat.searchexpression;

import java.util.Map;
import java.util.Set;

import javax.faces.component.UIComponent;
import javax.faces.component.search.SearchExpressionContext;
import javax.faces.component.search.SearchExpressionContextFactory;
import javax.faces.component.search.SearchExpressionHint;
import javax.faces.component.visit.VisitHint;
import javax.faces.context.FacesContext;

/**
 * Custom SearchExpressionHanlder defined in the faces-config.xml
 */
public class CustomSearchExpressionContextFactory extends SearchExpressionContextFactory {

    private final SearchExpressionContextFactory wrapped;

    public CustomSearchExpressionContextFactory(SearchExpressionContextFactory wrapped) {
        super(wrapped);
        this.wrapped = wrapped;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.component.search.SearchExpressionContextFactory#getSearchExpressionContext(javax.faces.context.FacesContext, javax.faces.component.UIComponent,
     * java.util.Set, java.util.Set)
     */
    @Override
    public SearchExpressionContext getSearchExpressionContext(FacesContext context, UIComponent source, Set<SearchExpressionHint> expressionHints, Set<VisitHint> visitHints) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        requestMap.put("CustomSearchExpressionContextFactory", "getSearchExpressionContext Invoked!");
        return wrapped.getSearchExpressionContext(context, source, expressionHints, visitHints);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.component.search.SearchExpressionContextFactory#getWrapped()
     */
    @Override
    public SearchExpressionContextFactory getWrapped() {
        return wrapped;
    }

}

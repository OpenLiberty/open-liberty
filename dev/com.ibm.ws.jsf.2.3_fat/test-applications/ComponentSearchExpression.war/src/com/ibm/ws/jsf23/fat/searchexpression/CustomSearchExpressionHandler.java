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

import javax.faces.component.search.SearchExpressionContext;
import javax.faces.component.search.SearchExpressionHandler;
import javax.faces.component.search.SearchExpressionHandlerWrapper;
import javax.faces.context.FacesContext;

/**
 * Custom SearchExpressionHanlder defined in the faces-config.xml
 */
public class CustomSearchExpressionHandler extends SearchExpressionHandlerWrapper {

    /**
     * @param delegate
     */
    public CustomSearchExpressionHandler(SearchExpressionHandler delegate) {
        super(delegate);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.component.search.SearchExpressionHandler#resolveClientId(javax.faces.component.search.SearchExpressionContext, java.lang.String)
     */
    @Override
    public String resolveClientId(SearchExpressionContext searchExpressionContext, String expression) {
        FacesContext facesContext = searchExpressionContext.getFacesContext();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put("CustomSearchExpressionHandler", "resolveClientId Invoked!");
        return getWrapped().resolveClientId(searchExpressionContext, expression);
    }

}

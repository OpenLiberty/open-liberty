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

import javax.faces.component.UIComponent;
import javax.faces.component.search.SearchExpressionContext;
import javax.faces.component.search.SearchKeywordContext;
import javax.faces.component.search.SearchKeywordResolver;
import javax.faces.context.FacesContext;

/**
 * Custom SearchKeywordResolver defined in the faces-config.xml
 */
public class CustomSearchKeywordResolver extends SearchKeywordResolver {

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.component.search.SearchKeywordResolver#isResolverForKeyword(javax.faces.component.search.SearchExpressionContext, java.lang.String)
     */
    @Override
    public boolean isResolverForKeyword(SearchExpressionContext searchExpressionContext, String keyword) {
        FacesContext facesContext = searchExpressionContext.getFacesContext();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put("CustomSearchKeywordResolver", "isResolverForKeyword Invoked!");
        return keyword.equalsIgnoreCase("grandparent");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.component.search.SearchKeywordResolver#resolve(javax.faces.component.search.SearchKeywordContext, javax.faces.component.UIComponent, java.lang.String)
     */
    @Override
    public void resolve(SearchKeywordContext searchKeywordContext, UIComponent current, String keyword) {
        UIComponent parent = current.getParent();
        if (parent != null) {
            searchKeywordContext.invokeContextCallback(parent.getParent());
        } else {
            searchKeywordContext.setKeywordResolved(true);
        }
    }

}

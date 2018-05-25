/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.myfaces.component.search;

import java.util.HashSet;
import java.util.Set;
import javax.faces.component.UIComponent;
import javax.faces.component.search.SearchExpressionContext;
import javax.faces.component.search.SearchExpressionContextFactory;
import javax.faces.component.search.SearchExpressionHint;
import javax.faces.component.visit.VisitHint;
import javax.faces.context.FacesContext;

/**
 *
 */
public class SearchExpressionContextFactoryImpl extends SearchExpressionContextFactory
{

    @Override
    public SearchExpressionContext getSearchExpressionContext(
            FacesContext context, UIComponent source,
            Set<SearchExpressionHint> expressionHints, Set<VisitHint> visitHints)
    {
        SearchExpressionContextImpl searchExpressionContext = new SearchExpressionContextImpl(context);
        searchExpressionContext.setSource(source);
        searchExpressionContext.setExpressionHints(expressionHints == null ?
                new HashSet<SearchExpressionHint>(2) : expressionHints);
        searchExpressionContext.setVisitHints(visitHints);
        return searchExpressionContext;
    }

    @Override
    public SearchExpressionContextFactory getWrapped()
    {
        return null;
    }

}

/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;

/**
 * Repository method special parameters combined with equivalent annotations
 * and Query-by-Method-Name patterns, such as First and OrderBy.
 *
 * @param limit       special parameter of type Limit, otherise null
 * @param maxResults  maximum results (First, @First, or Limit param), otherwise 0
 * @param pageRequest page request (PageRequest param), otherwise null
 * @param restriction special parameter of type Restriction, otherwise null
 * @param sorts       sort criteria (OrderBy, @OrderBy, Order param, and/or Sort
 *                        param), otherwise null or empty list
 */
@Trivial // Restriction can include customer data
record QueryCustomization(
                Limit limit,
                int maxResults,
                PageRequest pageRequest,
                Object restriction,
                List<Sort<Object>> sorts) {

    private static final TraceComponent tc = Tr.register(QueryCustomization.class);

    // method args include customer data
    static QueryCustomization from(QueryInfo info, Object[] args) {
        DataVersionCompatibility compat = info.producer.compat();
        Limit limit = null;
        int max = info.maxResults;
        PageRequest pageReq = null;
        Object restriction = null;
        List<Sort<Object>> sorts = null;

        // The first method parameters are used as query parameters.
        // Beyond that, they can have other purposes such as
        // pagination and sorting.
        for (int i = info.specialParamsStartAt; //
                        i < (args == null ? 0 : args.length); //
                        i++) {
            Object param = args[i];
            if (param instanceof Limit) {
                if (max == 0 && limit == null && pageReq == null)
                    max = (limit = (Limit) param).maxResults();
                else
                    throw Fail.methodParamIncompat(info, param, limit, pageReq);
            } else if (param instanceof Order) {
                @SuppressWarnings("unchecked")
                Iterable<Sort<Object>> order = (Iterable<Sort<Object>>) param;
                sorts = info.supplySorts(sorts, order);
            } else if (param instanceof PageRequest) {
                if (max == 0 && pageReq == null && limit == null)
                    max = (pageReq = (PageRequest) param).size();
                else
                    throw Fail.methodParamIncompat(info, param, limit, pageReq);
            } else if (param instanceof Sort) {
                @SuppressWarnings("unchecked")
                List<Sort<Object>> newList = //
                                info.supplySorts(sorts, (Sort<Object>) param);
                sorts = newList;
            } else if (param instanceof Sort[]) {
                @SuppressWarnings("unchecked")
                List<Sort<Object>> newList = //
                                info.supplySorts(sorts, (Sort<Object>[]) param);
                sorts = newList;
            } else if (compat.isRestriction(param)) {
                if (restriction == null)
                    restriction = param;
                else
                    throw Fail.duplicateSpecialParam(info, "Restriction");
            } else if (param == null) {
                // ignore null for empty Sort...
                boolean isSort = false;
                for (int s = 0; s < info.sortPositions.length; s++)
                    isSort |= info.sortPositions[s] == i;
                if (!isSort)
                    // BasicRepository.findAll requires NullPointerException
                    throw Fail.nullMethodParameter(info, i);
            } else {
                throw Fail.extraMethodParam(info, i);
            }
        }

        if (sorts == null && info.sortPositions.length > 0)
            sorts = info.sorts;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "QueryCustomization",
                     limit, max, pageReq, info.loggable(restriction), sorts);

        return new QueryCustomization(limit, max, pageReq, restriction, sorts);
    }

}

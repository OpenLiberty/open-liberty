/*******************************************************************************
 * Copyright (c) 2022,2026 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence;

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SortedMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Page with the ability to create cursors from the elements on the page.
 * A cursor can be used to request next and previous pages relative to the cursor.
 */
public class CursoredPageImpl<T> extends PageImpl<T> implements CursoredPage<T> {
    private static final TraceComponent tc = Tr.register(CursoredPageImpl.class);

    /**
     * Construct a new CursoredPage.
     *
     * @param queryInfo            query information.
     * @param em                   the entity manager.
     * @param pageRequest          the request for this page.
     * @param args                 values that are supplied to the repository method.
     * @param deferredConstraints  map of method parameter index to non-Literal
     *                                 Constraints that are supplied at execution time.
     * @param constraintJPQLParams map of JPQL parameter names/indices and values that are
     *                                 added due to Constraints and Restrictions.
     *                                 Null indicates none are added.
     * @throws Exception if an error occurs.
     */
    @Trivial // avoid tracing customer data
    CursoredPageImpl(QueryInfo queryInfo,
                     EntityManager em,
                     PageRequest pageRequest,
                     Object[] args,
                     Map<Integer, Object> deferredConstraints,
                     Map<Object, Object> constraintJPQLParams) throws Exception {
        super(queryInfo, pageRequest, args, deferredConstraints, constraintJPQLParams);

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "<init>",
                     queryInfo,
                     pageRequest,
                     queryInfo.loggable(args),
                     deferredConstraints.keySet(),
                     constraintJPQLParams == null ? null : constraintJPQLParams.keySet());

        int maxPageSize = this.pageRequest.size();
        int firstResult = this.pageRequest.mode() == PageRequest.Mode.OFFSET //
                        ? queryInfo.computeOffset(this.pageRequest) //
                        : 0;

        String jpql;
        Optional<PageRequest.Cursor> cursor = this.pageRequest.cursor();
        Map<Object, Object> addedJPQLParams;

        if (cursor.isPresent()) {
            jpql = isForward ? queryInfo.jpqlAfterCursor : queryInfo.jpqlBeforeCursor;

            addedJPQLParams = constraintJPQLParams == null //
                            ? new LinkedHashMap<>() //
                            : new LinkedHashMap<>(constraintJPQLParams);

            addParametersForCursor(cursor.get(), addedJPQLParams);
        } else { // no Cursor in PageRequest
            jpql = queryInfo.jpql;

            addedJPQLParams = constraintJPQLParams;
        }

        @SuppressWarnings("unchecked")
        TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, Object.class);
        queryInfo.setParameters(query, args, deferredConstraints, addedJPQLParams);

        query.setFirstResult(firstResult);
        query.setMaxResults(maxPageSize == Integer.MAX_VALUE //
                        ? Integer.MAX_VALUE //
                        : (maxPageSize + 1)); // extra result indicates if next page exists

        results = query.getResultList();

        // Cursor-based pagination in the previous page direction is implemented
        // by reversing the ORDER BY to obtain the previous page. A side-effect
        // of that is that the resulting entries for the page are reversed,
        // so we need to reverse again to correct that.
        if (!isForward)
            for (int size = results.size(),
                            i = 0,
                            j = size - (size > maxPageSize ? 2 : 1); //
                            i < j; //
                            i++, j--)
                Collections.swap(results, i, j);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    /**
     * Generates query parameters for the cursor element values and adds them to
     * the addedJPQLParams map.
     *
     * @param cursor          the cursor
     * @param addedJPQLParams JPQL parameters added for repository special parameters.
     *                            This method adds parameters for the elements of a
     *                            Cursor that is present on a PageRequest.
     * @throws Exception if an error occurs
     */
    private void addParametersForCursor(Cursor cursor,
                                        @Sensitive Map<Object, Object> addedJPQLParams) //
                    throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        int cursorSize = cursor.size();

        // Expand ID(THIS) for composite IdClass into separate attributes
        SortedMap<String, Member> idClassAttributeAccessors = //
                        queryInfo.entityInfo.idClassAttributeAccessors;
        if (idClassAttributeAccessors != null) {
            boolean foundIdClass = false;
            ArrayList<Object> cursorValues = new ArrayList<>(cursorSize + 3);
            for (int c = 0; c < cursorSize; c++) {
                Object value = cursor.get(c);
                if (queryInfo.entityInfo.idType.isInstance(value)) {
                    foundIdClass = true;
                    for (Member accessor : idClassAttributeAccessors.values()) {
                        Object v = accessor instanceof Field //
                                        ? ((Field) accessor).get(value) //
                                        : ((Method) accessor).invoke(value);
                        cursorValues.add(v);
                    }
                } else {
                    cursorValues.add(value);
                }
            }
            if (foundIdClass) {
                cursor = Cursor.forKey(cursorValues.toArray());
                cursorSize = cursor.size();
            }
        }

        if (queryInfo.sorts.size() != cursorSize)
            cursorSizeMismatchError(cursor);

        Object[] paramNames = queryInfo.jpqlParamNames.isEmpty() //
                        ? null //
                        : queryInfo.jpqlParamNames.toArray();

        int paramNum = queryInfo.jpqlParamCount + 1;
        for (int c = 0; c < cursorSize; c++, paramNum++) {
            Object key = paramNames == null //
                            ? paramNum // positional parameters
                            : paramNames[paramNum - 1]; // named parameters
            addedJPQLParams.put(key, cursor.get(c));

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "added parameter " + key + " for cursor");
        }
    }

    /**
     * Creates a Cursor for the specified entity.
     *
     * @param entity the entity.
     * @return Cursor with cursor element values according to the sort criteria.
     */
    @Trivial
    private Cursor createCursor(Object entity) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "createCursor", queryInfo.loggable(entity));

        EntityInfo entityInfo = queryInfo.entityInfo;
        Object[] cursorElements = new Object[queryInfo.sorts.size()];
        for (int c = 0; c < cursorElements.length; c++)
            try {
                Sort<?> sort = queryInfo.sorts.get(c);
                List<Member> accessors = //
                                entityInfo.attributeAccessors.get(sort.property());
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "get cursor element " + accessors);
                if (accessors == null)
                    throw exc(MappingException.class,
                              "CWWKD1123.sort.incompat.with.cursor",
                              queryInfo.method.getName(),
                              queryInfo.repositoryInterface.getName(),
                              sort.property(),
                              "Cursor.forKey",
                              entityInfo.getType().getName(),
                              entityInfo.attributeTypes.keySet());
                Object value = entity;
                for (Member accessor : accessors)
                    if (accessor instanceof Method)
                        value = ((Method) accessor).invoke(value);
                    else
                        value = ((Field) accessor).get(value);
                cursorElements[c] = value;
            } catch (IllegalAccessException | //
                            IllegalArgumentException | //
                            InvocationTargetException x) {
                throw new DataException(x instanceof InvocationTargetException //
                                ? x.getCause() : x);
            }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "createCursor", queryInfo.loggable(cursorElements));
        return Cursor.forKey(cursorElements);
    }

    @Override
    public PageRequest.Cursor cursor(int index) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (index < 0 || index >= pageRequest.size() || index >= results.size())
            throw new IllegalArgumentException("index: " + index);

        T entity = results.get(index);

        final Object[] keyElements = new Object[queryInfo.sorts.size()];
        int k = 0;
        for (Sort<?> keyInfo : queryInfo.sorts)
            try {
                List<Member> accessors = queryInfo.entityInfo.attributeAccessors.get(keyInfo.property());
                Object value = entity;
                for (Member accessor : accessors)
                    if (accessor instanceof Method)
                        value = ((Method) accessor).invoke(value);
                    else
                        value = ((Field) accessor).get(value);
                keyElements[k++] = value;

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "key element " + k + ": " +
                                       queryInfo.loggable(value));
            } catch (IllegalAccessException | IllegalArgumentException x) {
                throw new DataException(x);
            } catch (InvocationTargetException x) {
                throw new DataException(x.getCause());
            }

        return Cursor.forKey(keyElements);
    }

    /**
     * Raises an error because the number of cursor elements does not match the
     * number of sort parameters.
     *
     * @param cursor cursor
     */
    @Trivial
    private void cursorSizeMismatchError(PageRequest.Cursor cursor) {
        throw exc(IllegalArgumentException.class,
                  "CWWKD1036.cursor.size.mismatch",
                  cursor.size(),
                  queryInfo.method.getName(),
                  queryInfo.repositoryInterface.getName(),
                  queryInfo.sorts.size(),
                  queryInfo.loggable(cursor.elements()),
                  queryInfo.sorts);
    }

    @Override
    public boolean hasNext() {
        // The extra position is only available for identifying a next page
        // if the current page was obtained in the forward direction
        int maxResults = pageRequest.size() == Integer.MAX_VALUE //
                        ? Integer.MAX_VALUE //
                        : (pageRequest.size() + 1);
        int minToHaveNextPage = isForward ? maxResults : 1;
        return results.size() >= minToHaveNextPage;
    }

    @Override
    public boolean hasPrevious() {
        // The extra position is only available for identifying a previous page
        // if the current page was obtained in the reverse direction
        int maxResults = pageRequest.size() == Integer.MAX_VALUE //
                        ? Integer.MAX_VALUE //
                        : (pageRequest.size() + 1);
        int minToHavePreviousPage = isForward ? 1 : maxResults;
        return results.size() >= minToHavePreviousPage;
    }

    @Override
    public PageRequest nextPageRequest() {
        if (!hasNext())
            throw exc(NoSuchElementException.class,
                      "CWWKD1040.no.next.page",
                      queryInfo.method.getName(),
                      queryInfo.repositoryInterface.getName(),
                      "CursoredPage.hasNext");

        // CURSOR_PREVIOUS that reads a partial page can have a next page
        int maxPageSize = pageRequest.size();
        int endingResultIndex = Math.min(maxPageSize, results.size()) - 1;
        long pageNum = pageRequest.page() == Long.MAX_VALUE //
                        ? Long.MAX_VALUE //
                        : (pageRequest.page() + 1);

        return PageRequest.afterCursor(createCursor(results.get(endingResultIndex)),
                                       pageNum,
                                       maxPageSize,
                                       pageRequest.requestTotal());
    }

    @Override
    public PageRequest previousPageRequest() {
        if (!hasPrevious())
            throw exc(NoSuchElementException.class,
                      "CWWKD1039.no.prev.cursor.page",
                      queryInfo.method.getName(),
                      queryInfo.repositoryInterface.getName());

        // Decrement page number by 1 unless it would go below 1.
        long pageNum = pageRequest.page() == 1 ? 1 : pageRequest.page() - 1;

        return PageRequest.beforeCursor(createCursor(results.get(0)),
                                        pageNum,
                                        pageRequest.size(),
                                        pageRequest.requestTotal());
    }

    /**
     * Convert to readable text of the form:
     *
     * CursoredPage 4/10 of MyEntity, size 10/10, CURSOR_NEXT(name ASC IgnoreCase, id ASC) @ff22b3c5
     *
     * @return textual representation of the page.
     */
    @Override
    @Trivial
    public String toString() {
        int maxPageSize = pageRequest.size();
        int size = Math.min(results.size(), maxPageSize);
        StringBuilder s = new StringBuilder(200) //
                        .append("CursoredPage ").append(pageRequest.page());
        if (totalElements >= 0) {
            s.append('/');
            s.append(totalElements / maxPageSize +
                     (totalElements % maxPageSize > 0 ? 1 : 0));
        }
        if (!results.isEmpty()) {
            s.append(" of ").append(results.get(0).getClass().getSimpleName());
        }
        s.append(", size ").append(size);
        s.append('/').append(maxPageSize);
        s.append(isForward ? ", CURSOR_NEXT(" : " CURSOR_PREVIOUS(");

        boolean firstSort = true;
        if (queryInfo.sorts != null)
            for (Sort<?> sort : queryInfo.sorts) {
                if (firstSort)
                    firstSort = false;
                else
                    s.append(", ");
                s.append(sort.property()); //
                s.append(sort.isAscending() //
                                ? sort.ignoreCase() ? " ASC IgnoreCase" : " ASC" //
                                : sort.ignoreCase() ? " DESC IgnoreCase" : " DESC");
            }

        s.append(") @").append(Integer.toHexString(hashCode()));
        return s.toString();
    }
}

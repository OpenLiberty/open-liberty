/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Cursor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Page with the ability to create cursors from the elements on the page.
 * A cursor can be used to request next and previous pages relative to the cursor.
 */
public class CursoredPageImpl<T> implements CursoredPage<T> {
    private static final TraceComponent tc = Tr.register(CursoredPageImpl.class);

    private final Object[] args;
    private final boolean isForward;
    private final PageRequest pageRequest;
    private final QueryInfo queryInfo;
    private final List<T> results;
    private long totalElements = -1;

    @FFDCIgnore(Exception.class)
    CursoredPageImpl(QueryInfo queryInfo, PageRequest pageRequest, Object[] args) {

        if (pageRequest == null)
            PageImpl.missingPageRequest(queryInfo);

        this.args = args;
        this.queryInfo = queryInfo;
        this.pageRequest = pageRequest;
        this.isForward = this.pageRequest.mode() != PageRequest.Mode.CURSOR_PREVIOUS;

        Optional<PageRequest.Cursor> cursor = this.pageRequest.cursor();

        int maxPageSize = this.pageRequest.size();
        int firstResult = this.pageRequest.mode() == PageRequest.Mode.OFFSET //
                        ? RepositoryImpl.computeOffset(this.pageRequest) //
                        : 0;

        EntityManager em = queryInfo.entityInfo.builder.createEntityManager();
        try {
            String jpql = cursor.isEmpty() ? queryInfo.jpql : //
                            isForward ? queryInfo.jpqlAfterCursor : //
                                            queryInfo.jpqlBeforeCursor;

            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, queryInfo.entityInfo.entityClass);
            queryInfo.setParameters(query, args);

            if (cursor.isPresent())
                queryInfo.setParametersFromCursor(query, cursor.get());

            query.setFirstResult(firstResult);
            query.setMaxResults(maxPageSize + (maxPageSize == Integer.MAX_VALUE ? 0 : 1)); // extra position is for knowing whether to expect another page

            results = query.getResultList();

            // Cursor-based pagination in the previous page direction is implemented
            // by reversing the ORDER BY to obtain the previous page. A side-effect
            // of that is that the resulting entries for the page are reversed,
            // so we need to reverse again to correct that.
            if (!isForward)
                for (int size = results.size(), i = 0, j = size - (size > maxPageSize ? 2 : 1); i < j; i++, j--)
                    Collections.swap(results, i, j);
        } catch (Exception x) {
            throw RepositoryImpl.failure(x);
        } finally {
            em.close();
        }
    }

    /**
     * Query for count of total elements across all pages.
     *
     * @param jpql count query.
     * @throws IllegalStateException if not configured to request a total count of elements.
     */
    @FFDCIgnore(Exception.class)
    private long countTotalElements() {
        if (!pageRequest.requestTotal())
            throw new IllegalStateException("A total count of elements and pages is not retreived from the database because the " +
                                            pageRequest + " page request specifies a value of 'false' for 'requestTotal'. " +
                                            "To request a page with the total count included, use the " +
                                            "PageRequest.withTotal method instead of the PageRequest.withoutTotal method."); // TODO NLS

        EntityManager em = queryInfo.entityInfo.builder.createEntityManager();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "query for count: " + queryInfo.jpqlCount);
            TypedQuery<Long> query = em.createQuery(queryInfo.jpqlCount, Long.class);
            queryInfo.setParameters(query, args);

            return query.getSingleResult();
        } catch (Exception x) {
            throw RepositoryImpl.failure(x);
        } finally {
            em.close();
        }
    }

    @Override
    public List<T> content() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? new ResultList(max) : results;
    }

    @Override
    public PageRequest.Cursor cursor(int index) {
        if (index < 0 || index >= pageRequest.size())
            throw new IllegalArgumentException("index: " + index);

        T entity = results.get(index);

        final Object[] keyValues = new Object[queryInfo.sorts.size()];
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
                keyValues[k++] = value;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x.getCause());
            }

        return Cursor.forKey(keyValues);
    }

    @Override
    public boolean hasNext() {
        // The extra position is only available for identifying a next page if the current page was obtained in the forward direction
        int minToHaveNextPage = isForward ? (pageRequest.size() + (pageRequest.size() == Integer.MAX_VALUE ? 0 : 1)) : 1;
        return results.size() >= minToHaveNextPage;
    }

    @Override
    public boolean hasPrevious() {
        // The extra position is only available for identifying a previous page if the current page was obtained in the reverse direction
        int minToHavePreviousPage = isForward ? 1 : (pageRequest.size() + (pageRequest.size() == Integer.MAX_VALUE ? 0 : 1));
        return results.size() >= minToHavePreviousPage;
    }

    @Override
    public boolean hasTotals() {
        return pageRequest.requestTotal();
    }

    @Override
    public int numberOfElements() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? max : size;
    }

    @Override
    public PageRequest pageRequest() {
        return pageRequest;
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
            s.append(totalElements / maxPageSize + (totalElements % maxPageSize > 0 ? 1 : 0));
        }
        if (!results.isEmpty()) {
            s.append(" of ").append(results.get(0).getClass().getSimpleName());
        }
        s.append(", size ").append(size);
        s.append('/').append(maxPageSize);
        s.append(isForward ? ", CURSOR_NEXT(" : " CURSOR_PREVIOUS(");

        boolean firstSort = true;
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

    @Override
    public long totalElements() {
        if (totalElements == -1)
            totalElements = countTotalElements();
        return totalElements;
    }

    @Override
    public long totalPages() {
        if (totalElements == -1)
            totalElements = countTotalElements();
        return totalElements / pageRequest.size() + (totalElements % pageRequest.size() > 0 ? 1 : 0);
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? new ResultIterator(max) : results.iterator();
    }

    @Override
    public PageRequest nextPageRequest() {
        if (!hasNext())
            throw new NoSuchElementException("Cannot request a next page. To avoid this error, check for a " +
                                             "true result of CursoredPage.hasNext before attempting this method."); // TODO NLS

        int maxPageSize = pageRequest.size();
        int endingResultIndex = Math.min(maxPageSize, results.size()) - 1; // CURSOR_PREVIOUS that reads a partial page can have a next page

        return PageRequest.afterCursor(Cursor.forKey(queryInfo.getCursorValues(results.get(endingResultIndex))),
                                       pageRequest.page() == Long.MAX_VALUE ? Long.MAX_VALUE : pageRequest.page() + 1,
                                       maxPageSize,
                                       pageRequest.requestTotal());
    }

    @Override
    public PageRequest previousPageRequest() {
        if (!hasPrevious())
            throw new NoSuchElementException("Cannot request a previous page. To avoid this error, check for a " +
                                             "true result of CursoredPage.hasPrevious before attempting this method."); // TODO NLS

        // Decrement page number by 1 unless it would go below 1.
        return PageRequest.beforeCursor(Cursor.forKey(queryInfo.getCursorValues(results.get(0))),
                                        pageRequest.page() == 1 ? 1 : pageRequest.page() - 1,
                                        pageRequest.size(),
                                        pageRequest.requestTotal());
    }

    @Override
    public Stream<T> stream() {
        return content().stream();
    }

    /**
     * Iterator that restricts the number of results to the specified amount.
     */
    @Trivial
    private class ResultIterator implements Iterator<T> {
        private int index;
        private final Iterator<T> iterator;
        private final int size;

        private ResultIterator(int size) {
            this.size = size;
            this.iterator = results.iterator();
        }

        @Override
        public boolean hasNext() {
            return index < size && iterator.hasNext();
        }

        @Override
        public T next() {
            if (index >= size)
                throw new NoSuchElementException("Element at index " + index);
            T result = iterator.next();
            index++;
            return result;
        }
    }

    /**
     * List that restricts the number of results to the specified amount.
     */
    @Trivial
    private class ResultList extends AbstractList<T> implements RandomAccess {
        private final int size;

        private ResultList(int size) {
            this.size = size;
        }

        @Override
        public T get(int index) {
            if (index < size)
                return results.get(index);
            else
                throw new IndexOutOfBoundsException(index);
        }

        @Override
        public int size() {
            return size;
        }
    }
}

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
package io.openliberty.data.internal;

import static io.openliberty.data.internal.cdi.DataExtension.exc;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.PageRequest.Mode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * A page of results.
 */
public class PageImpl<T> implements Page<T> {
    private static final TraceComponent tc = Tr.register(PageImpl.class);

    /**
     * Map of JPQL parameter names/indices and values that are added due to
     * repository special parameters. Null indicates none are added.
     */
    final Map<Object, Object> addedJPQLParams;

    /**
     * Values that are supplied when invoking the repository method that
     * requests the page.
     */
    final Object[] args;

    /**
     * Map of method parameter index to non-Literal Constraints that are supplied
     * at execution time.
     */
    final Map<Integer, Object> deferredConstraints;

    /**
     * Indicates the direction of pagination relative to a cursor.
     * In the case of a first page requested with offset pagination,
     * where there is no cursor, the direction is forward.
     */
    final boolean isForward;

    /**
     * The request for this page.
     */
    final PageRequest pageRequest;

    /**
     * Query information.
     */
    final QueryInfo queryInfo;

    /**
     * Results of the query for this page.
     */
    List<T> results;

    /**
     * Total number of elements across all pages. This value is computed lazily,
     * with -1 indicating it has not been computed yet.
     */
    long totalElements = -1;

    /**
     * Construct a new Page.
     *
     * @param queryInfo           query information.
     * @param em                  the entity manager.
     * @param pageRequest         the request for this page.
     * @param args                values that are supplied to the repository method.
     * @param deferredConstraints map of method parameter index to non-Literal
     *                                Constraints that are supplied at execution time.
     * @param addedJPQLParams     map of JPQL parameter names/indices and values that are
     *                                added due to repository special parameters.
     *                                Null indicates none are added.
     * @throws Exception if an error occurs.
     */
    @Trivial
    PageImpl(QueryInfo queryInfo,
             EntityManager em,
             PageRequest pageRequest,
             Object[] args,
             Map<Integer, Object> deferredConstraints,
             Map<Object, Object> addedJPQLParams) {
        this(queryInfo, pageRequest, args, deferredConstraints, addedJPQLParams);

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(tc, "<init>", queryInfo, pageRequest, queryInfo.loggable(args));

        if (pageRequest.mode() != Mode.OFFSET)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1035.incompat.page.mode",
                      pageRequest.mode(),
                      queryInfo.method.getName(),
                      queryInfo.repositoryInterface.getName(),
                      queryInfo.method.getGenericReturnType().getTypeName(),
                      CursoredPage.class.getName());

        @SuppressWarnings("unchecked")
        TypedQuery<T> query = (TypedQuery<T>) em.createQuery(queryInfo.jpql,
                                                             Object.class);
        queryInfo.setParameters(query, args, deferredConstraints, addedJPQLParams);

        int maxPageSize = pageRequest.size();
        query.setFirstResult(queryInfo.computeOffset(pageRequest));
        query.setMaxResults(maxPageSize == Integer.MAX_VALUE //
                        ? Integer.MAX_VALUE //
                        : (maxPageSize + 1)); // extra result indicates if next page exists

        results = query.getResultList();

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>");
    }

    /**
     * Common constructor with CursoredPageImpl that initializes fields.
     *
     * @param queryInfo           query information.
     * @param pageRequest         the request for this page.
     * @param args                values that are supplied to the repository method.
     * @param deferredConstraints map of method parameter index to non-Literal
     *                                Constraints that are supplied at execution time.
     * @param addedJPQLParams     map of JPQL parameter names/indices and values that are
     *                                added due to repository special parameters.
     *                                Null indicates none are added.
     * @throws Exception if an error occurs.
     */
    @Trivial
    PageImpl(QueryInfo queryInfo,
             PageRequest pageRequest,
             Object[] args,
             Map<Integer, Object> deferredConstraints,
             Map<Object, Object> addedJPQLParams) {
        this.addedJPQLParams = addedJPQLParams;
        this.args = args;
        this.deferredConstraints = deferredConstraints;
        this.queryInfo = queryInfo;
        this.pageRequest = pageRequest;

        if (pageRequest == null)
            throw Fail.missingPageRequest(queryInfo);

        this.isForward = pageRequest.mode() != PageRequest.Mode.CURSOR_PREVIOUS;
    }

    @Override
    @Trivial
    public List<T> content() {
        int size = results.size();
        int max = pageRequest.size();
        List<T> content = size > max ? new ResultList(max) : results;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "content", queryInfo.loggable(content));
        return content;
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
            throw exc(IllegalStateException.class,
                      "CWWKD1042.no.totals",
                      queryInfo.method.getName(),
                      queryInfo.repositoryInterface.getName(),
                      pageRequest);

        if (pageRequest.cursor().isEmpty() &&
            pageRequest.page() == 1L &&
            results.size() <= pageRequest.size() &&
            pageRequest.size() < Integer.MAX_VALUE)
            return results.size();

        if (queryInfo.jpqlCount.length() < Util.MIN_COUNT_QUERY_LENGTH)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1119.keyword.prevents.count",
                      queryInfo.method.getName(),
                      queryInfo.repositoryInterface.getName(),
                      queryInfo.jpqlCount,
                      queryInfo.jpql);

        EntityManagerBuilder builder = queryInfo.entityInfo.builder;
        boolean stateful = queryInfo.producer.stateful();
        EntityManager em = null;
        try {
            em = builder.getEntityManager(stateful);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "query for count: " + queryInfo.jpqlCount);

            TypedQuery<Long> query = em.createQuery(queryInfo.jpqlCount, Long.class);
            queryInfo.setParameters(query, args, deferredConstraints, addedJPQLParams);

            return query.getSingleResult();
        } catch (Exception x) {
            throw RepositoryImpl.failure(x, builder);
        } finally {
            if (!stateful && em != null)
                em.close();
        }
    }

    @Override
    public boolean hasContent() {
        return !results.isEmpty();
    }

    @Override
    public boolean hasNext() {
        int minToHaveNextPage = pageRequest.size() == Integer.MAX_VALUE //
                        ? Integer.MAX_VALUE //
                        : (pageRequest.size() + 1);
        return results.size() >= minToHaveNextPage;
    }

    @Override
    public boolean hasPrevious() {
        return pageRequest.page() > 1;
    }

    @Override
    public boolean hasTotals() {
        return queryInfo.jpqlCount.length() >= Util.MIN_COUNT_QUERY_LENGTH &&
               pageRequest.requestTotal();
    }

    @Override
    public Iterator<T> iterator() {
        int size = results.size();
        int max = pageRequest.size();
        return size > max ? new ResultIterator(max) : results.iterator();
    }

    @Override
    public PageRequest nextPageRequest() {
        if (hasNext())
            return PageRequest.ofPage(pageRequest.page() + 1,
                                      pageRequest.size(),
                                      pageRequest.requestTotal());
        else
            throw exc(NoSuchElementException.class,
                      "CWWKD1040.no.next.page",
                      queryInfo.method.getName(),
                      queryInfo.repositoryInterface.getName(),
                      "Page.hasNext");
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

    @Override
    public PageRequest previousPageRequest() {
        if (pageRequest.page() > 1)
            return PageRequest.ofPage(pageRequest.page() - 1,
                                      pageRequest.size(),
                                      pageRequest.requestTotal());
        else
            throw exc(NoSuchElementException.class,
                      "CWWKD1038.no.prev.offset.page",
                      pageRequest.page(),
                      queryInfo.method.getName(),
                      queryInfo.repositoryInterface.getName());
    }

    @Override
    public Stream<T> stream() {
        return content().stream();
    }

    /**
     * Convert to readable text of the form:
     *
     * Page 4/10 of MyEntity, size 10/10 @ff22b3c5
     *
     * @return textual representation of the page.
     */
    @Override
    @Trivial
    public String toString() {
        int maxPageSize = pageRequest.size();
        int size = Math.min(results.size(), maxPageSize);
        StringBuilder s = new StringBuilder(80) //
                        .append("Page ").append(pageRequest.page());
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
        s.append(" @").append(Integer.toHexString(hashCode()));
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
        return totalElements / pageRequest.size() +
               (totalElements % pageRequest.size() > 0 ? 1 : 0);
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

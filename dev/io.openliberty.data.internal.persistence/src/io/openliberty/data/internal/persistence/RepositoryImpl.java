/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.persistence;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.BaseStream;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.Delete;
import jakarta.data.Inheritance;
import jakarta.data.Result;
import jakarta.data.Select;
import jakarta.data.Select.Aggregate;
import jakarta.data.Update;
import jakarta.data.Where;
import jakarta.data.exceptions.DataConnectionException;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.Limit;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Slice;
import jakarta.data.repository.Sort;
import jakarta.data.repository.Streamable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R, E> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private static final Set<Class<?>> SPECIAL_PARAM_TYPES = new HashSet<>(Arrays.asList //
    (Collector.class, Consumer.class, Limit.class, Pageable.class, Sort.class, Sort[].class));

    AtomicBoolean isDisposed = new AtomicBoolean();
    private final PersistenceDataProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    private final Class<R> repositoryInterface;

    public RepositoryImpl(PersistenceDataProvider provider, Class<R> repositoryInterface, Class<E> defaultEntityClass) {
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        boolean inheritance = defaultEntityClass.getAnnotation(Inheritance.class) != null ||
                              defaultEntityClass.getAnnotation(jakarta.persistence.Inheritance.class) != null;

        CompletableFuture<EntityInfo> defaultEntityInfoFuture = provider.entityInfoMap.computeIfAbsent(defaultEntityClass, EntityInfo::newFuture);

        for (Method method : repositoryInterface.getMethods()) {
            Class<?> returnType = method.getReturnType();
            Class<?> returnArrayType = returnType.getComponentType();
            Class<?> returnTypeParam = null;
            Class<?> entityClass = returnType;
            if (returnArrayType == null) {
                Type type = method.getGenericReturnType();
                Type typeParams[] = type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments() : null;
                if (typeParams != null && typeParams.length == 1) {
                    Type paramType = typeParams[0] instanceof ParameterizedType ? ((ParameterizedType) typeParams[0]).getRawType() : typeParams[0];
                    if (paramType instanceof Class) {
                        entityClass = returnTypeParam = (Class<?>) paramType;
                        returnArrayType = returnTypeParam.getComponentType();
                        if (returnArrayType != null)
                            entityClass = returnArrayType;
                    }
                }
            } else {
                entityClass = returnArrayType;
            }

            if (!inheritance || !defaultEntityClass.isAssignableFrom(entityClass)) // TODO allow other entity types from model
                entityClass = defaultEntityClass;

            CompletableFuture<EntityInfo> entityInfoFuture = entityClass.equals(defaultEntityClass) //
                            ? defaultEntityInfoFuture //
                            : provider.entityInfoMap.computeIfAbsent(entityClass, EntityInfo::newFuture);

            queries.put(method, entityInfoFuture.thenCombine(CompletableFuture.completedFuture(new QueryInfo(method, returnArrayType, returnTypeParam)),
                                                             this::completeQueryInfo));
        }
    }

    /**
     * Gathers the information that is needed to perform the query that the repository method represents.
     *
     * @param entityInfo entity information
     * @param queryInfo  partially populated query information
     * @return information about the query.
     */
    private QueryInfo completeQueryInfo(EntityInfo entityInfo, QueryInfo queryInfo) {

        queryInfo.entityInfo = entityInfo;
        boolean needsKeysetQueries = KeysetAwarePage.class.equals(queryInfo.method.getReturnType())
                                     || KeysetAwarePage.class.equals(queryInfo.returnTypeParam);
        boolean countPages = needsKeysetQueries
                             || Page.class.equals(queryInfo.method.getReturnType())
                             || Page.class.equals(queryInfo.returnTypeParam);
        StringBuilder q = null;

        // TODO would it be more efficient to invoke method.getAnnotations() once?

        // @Query annotation
        Query query = queryInfo.method.getAnnotation(Query.class);
        if (query == null) {
            // @Delete/@Update/@Where/@OrderBy annotations
            Update update = queryInfo.method.getAnnotation(Update.class);
            Where where = queryInfo.method.getAnnotation(Where.class);
            if (update == null) {
                if (queryInfo.method.getAnnotation(Delete.class) == null) {
                    if (queryInfo.hasWhere = (where != null)) {
                        queryInfo.type = QueryInfo.Type.SELECT;
                        q = generateSelect(queryInfo).append(" WHERE (").append(where.value()).append(')');
                        if (countPages)
                            generateCount(queryInfo, " WHERE (", where.value(), ")");
                    }
                } else {
                    queryInfo.type = QueryInfo.Type.DELETE;
                    q = new StringBuilder(200).append("DELETE FROM ").append(entityInfo.name).append(" o");
                    if (queryInfo.hasWhere = (where != null))
                        q.append(" WHERE ").append(where.value());
                }
            } else {
                queryInfo.type = QueryInfo.Type.UPDATE;
                q = new StringBuilder(200).append("UPDATE ").append(entityInfo.name).append(" o SET ").append(update.value());
                if (queryInfo.hasWhere = (where != null))
                    q.append(" WHERE ").append(where.value());
            }

            if (q == null)
                if (queryInfo.method.getName().startsWith("save")) {
                    queryInfo.type = QueryInfo.Type.MERGE;
                    Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
                    if (paramTypes.length == 0)
                        throw new UnsupportedOperationException(queryInfo.method.getName() + " without any parameters");
                    queryInfo.saveParamType = paramTypes[0];
                } else {
                    // Repository method name pattern queries
                    q = generateRepositoryQuery(queryInfo, countPages);//keyset queries before orderby

                    // @Select annotation only
                    if (q == null) {
                        Select select = queryInfo.method.getAnnotation(Select.class);
                        if (select != null) {
                            queryInfo.type = QueryInfo.Type.SELECT;
                            q = generateSelect(queryInfo);
                            if (countPages)
                                generateCount(queryInfo);
                        }
                    }
                }
        } else { // @Query annotation
            queryInfo.jpql = query.value();

            String upper = queryInfo.jpql.toUpperCase();
            String upperTrimmed = upper.stripLeading();
            if (upperTrimmed.startsWith("SELECT")) {
                queryInfo.type = QueryInfo.Type.SELECT;

                queryInfo.jpqlCount = query.count().length() > 0 ? query.count() : null;
                if (countPages && queryInfo.jpqlCount == null) {
                    // Attempt to infer from provided query
                    int select = upper.indexOf("SELECT");
                    int from = upper.indexOf("FROM", select);
                    if (from > 0) {
                        String s = queryInfo.jpql.substring(select + 6, from);
                        int comma = s.indexOf(',');
                        if (comma > 0)
                            s = s.substring(0, comma);
                        int orderBy = upper.lastIndexOf("ORDER BY");
                        queryInfo.jpqlCount = new StringBuilder(queryInfo.jpql.length() + 7) //
                                        .append("SELECT COUNT(").append(s.trim()).append(") ") //
                                        .append(orderBy > from ? queryInfo.jpql.substring(from, orderBy) : queryInfo.jpql.substring(from)) //
                                        .toString();
                    }
                }
            } else if (upperTrimmed.startsWith("UPDATE")) {
                queryInfo.type = QueryInfo.Type.UPDATE;
            } else if (upperTrimmed.startsWith("DELETE")) {
                queryInfo.type = QueryInfo.Type.DELETE;
            } else {
                throw new UnsupportedOperationException(queryInfo.jpql);
            }

            queryInfo.hasWhere = upperTrimmed.contains("WHERE");
        }

        // If we don't already know from generating the JPQL, find out how many
        // parameters the JPQL takes and which parameters are named parameters.
        if (queryInfo.paramCount < 0 && queryInfo.type != QueryInfo.Type.MERGE) {
            Parameter[] params = queryInfo.method.getParameters();
            for (int i = 0; i < params.length && !SPECIAL_PARAM_TYPES.contains(params[i].getType()); i++) {
                List<String> paramNames = i == 0 ? (queryInfo.paramNames = new ArrayList<>()) : queryInfo.paramNames;
                Param param = params[i].getAnnotation(Param.class);
                paramNames.add(param == null ? null : param.value());
            }
            queryInfo.paramCount = queryInfo.paramNames.size();
        }

        // The Sorts parameter is from JNoSQL and might get added to Jakarta Data.
        // The @OrderBy annotation from Jakarta Data defines the same information annotatively.
        OrderBy[] orderBy = queryInfo.method.getAnnotationsByType(OrderBy.class);
        if (orderBy.length > 0) {
            queryInfo.type = queryInfo.type == null ? QueryInfo.Type.SELECT : queryInfo.type;
            if (q == null)
                if (queryInfo.jpql == null) {
                    q = generateSelect(queryInfo);
                    if (countPages)
                        generateCount(queryInfo);
                } else {
                    q = new StringBuilder(queryInfo.jpql);
                }

            StringBuilder o = needsKeysetQueries ? new StringBuilder(100) : q;
            StringBuilder r = needsKeysetQueries ? new StringBuilder(100) : null; // reverse order
            List<Sort> keyset = needsKeysetQueries ? new ArrayList<>(orderBy.length) : null;

            for (int i = 0; i < orderBy.length; i++) {
                o.append(i == 0 ? " ORDER BY o." : ", o.").append(orderBy[i].value());
                if (orderBy[i].descending())
                    o.append(" DESC");
                if (needsKeysetQueries) {
                    r.append(i == 0 ? " ORDER BY o." : ", o.").append(orderBy[i].value());
                    if (!orderBy[i].descending())
                        r.append(" DESC");

                    keyset.add(orderBy[i].descending() ? Sort.desc(orderBy[i].value()) : Sort.asc(orderBy[i].value()));
                }
            }

            if (needsKeysetQueries) {
                generateKeysetQueries(queryInfo, keyset, q, o, r);
                q.append(o);
            }
        }

        queryInfo.jpql = q == null ? queryInfo.jpql : q.toString();

        return queryInfo;
    }

    /**
     * Compute the zero-based offset to use as a starting point for a Limit range.
     *
     * @param limit limit that was specified by the application.
     * @return offset value.
     * @throws UnsupportedOperationException if the starting point for the limited range is not positive or would overflow Integer.MAX_VALUE.
     */
    static int computeOffset(Limit range) {
        long startIndex = range.startAt() - 1;
        if (startIndex >= 0 && startIndex <= Integer.MAX_VALUE)
            return (int) startIndex;
        else
            throw new UnsupportedOperationException("The starting point for " + range + " is not within 1 to Integer.MAX_VALUE (2147483647)."); // TODO
    }

    /**
     * Compute the zero-based offset for the start of a page.
     *
     * @param pageNumber  requested page number.
     * @param maxPageSize maximum size of pages.
     * @return offset for the specified page.
     * @throws UnsupportedOperationException if the offset exceeds Integer.MAX_VALUE.
     */
    static int computeOffset(long pageNumber, int maxPageSize) {
        long pageIndex = pageNumber - 1; // zero-based
        if (Integer.MAX_VALUE / maxPageSize >= pageIndex)
            return (int) (pageIndex * maxPageSize);
        else
            throw new UnsupportedOperationException("The offset for " + pageNumber + " pages of size " + maxPageSize + " exceeds Integer.MAX_VALUE (2147483647)."); // TODO
    }

    /**
     * Indicates if the characters leading up to, but not including, the endBefore position
     * in the text matches the searchFor. For example, a true value will be returned by
     * endsWith("Not", "findByNameNotNullAndPriceLessThan", 13).
     * But for any number other than 13, the above will return false because no position
     * other than 13 immediately follows a string ending with "Not".
     *
     * @param searchFor string to search for in the position immediately prior to the endBefore position.
     * @param text      the text to search.
     * @param minStart  the earliest possible starting point for the searchFor string in the text.
     * @param endBefore position before which to match.
     * @return true if found, otherwise false.
     */
    @Trivial
    private static boolean endsWith(String searchFor, String text, int minStart, int endBefore) {
        int searchLen = searchFor.length();
        return endBefore - minStart >= searchLen && text.regionMatches(endBefore - searchLen, searchFor, 0, searchLen);
    }

    /**
     * Replaces an exception with a Jakarta Data specification-defined exception,
     * chaining the original exception as the cause.
     * This method replaces all exceptions that are not RuntimeExceptions.
     * For RuntimeExceptions, it only replaces those that are
     * jakarta.persistence.PersistenceException (and subclasses).
     *
     * @param original exception to possibly replace.
     * @return exception to replace with, if any. Otherwise, the original.
     */
    @Trivial
    static RuntimeException failure(Exception original) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        RuntimeException x = null;
        if (original instanceof PersistenceException) {
            for (Throwable cause = original; x == null && cause != null; cause = cause.getCause()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "checking " + cause.getClass().getName() + " with message " + cause.getMessage());

                // TODO If this ever becomes real code, it should be delegated to the JDBC integration layer
                // where there is more thorough logic that takes into account configuration and database differences
                if (cause instanceof SQLRecoverableException
                    || cause instanceof SQLNonTransientConnectionException
                    || cause instanceof SQLTransientConnectionException)
                    x = new DataConnectionException(original);
            }
            if (x == null) {
                if (original instanceof NoResultException)
                    x = new EmptyResultException(original);
                else if (original instanceof jakarta.persistence.NonUniqueResultException)
                    x = new NonUniqueResultException(original);
                else
                    x = new DataException(original);
            }
        } else if (original instanceof CompletionException) {
            x = new MappingException(original); // TODO categorization is too broad
        } else if (original instanceof RuntimeException) {
            x = (RuntimeException) original;
        } else {
            x = new DataException(original);
        }

        if (trace && tc.isDebugEnabled() && x != original)
            Tr.debug(tc, "replaced with " + x.getClass().getName());
        return x;
    }

    /**
     * Generates a query to select the COUNT of all entities matching the
     * supplied WHERE condition(s), or all entities if no WHERE conditions.
     * Populates the jpqlCount of the query information with the result.
     *
     * @param queryInfo query information.
     * @param where     text to append together that makes up the WHERE clause
     */
    private void generateCount(QueryInfo queryInfo, String... where) {
        int len = 50;
        if (where != null)
            for (String w : where)
                len += w.length();

        StringBuilder q = new StringBuilder(len).append("SELECT COUNT(o) FROM ").append(queryInfo.entityInfo.name).append(" o");

        if (where != null)
            for (String w : where)
                q.append(w);

        queryInfo.jpqlCount = q.toString();
    }

    /**
     * Generates the before/after keyset queries and populates them into the query information.
     * Example conditions to add for forward keyset of (lastName, firstName, ssn):
     * AND ((o.lastName > ?5)
     * _ OR (o.lastName = ?5 AND o.firstName > ?6)
     * _ OR (o.lastName = ?5 AND o.firstName = ?6 AND o.ssn > ?7) )
     *
     * @param queryInfo query information
     * @param keyset    key names and direction
     * @param q         query up to the WHERE clause, if present
     * @param o         ORDER BY clause in forward direction. Null if forward direction is not needed.
     * @param r         ORDER BY clause in reverse direction. Null if reverse direction is not needed.
     */
    private void generateKeysetQueries(QueryInfo queryInfo, List<Sort> keyset, StringBuilder q, StringBuilder o, StringBuilder r) {
        int numKeys = keyset.size();
        String paramPrefix = queryInfo.paramNames.isEmpty() || queryInfo.paramNames.get(0) == null ? "?" : ":keyset";
        StringBuilder a = o == null ? null : new StringBuilder(200).append(queryInfo.hasWhere ? " AND (" : " WHERE (");
        StringBuilder b = r == null ? null : new StringBuilder(200).append(queryInfo.hasWhere ? " AND (" : " WHERE (");
        for (int i = 0; i < numKeys; i++) {
            if (a != null)
                a.append(i == 0 ? "(" : " OR (");
            if (b != null)
                b.append(i == 0 ? "(" : " OR (");
            for (int k = 0; k <= i; k++) {
                Sort keyInfo = keyset.get(k);
                String name = keyInfo.property();
                boolean asc = keyInfo.isAscending();
                if (a != null) {
                    a.append(k == 0 ? "o." : " AND o.").append(name);
                    a.append(k < i ? '=' : (asc ? '>' : '<'));
                    a.append(paramPrefix).append(queryInfo.paramCount + 1 + k);
                }
                if (b != null) {
                    b.append(k == 0 ? "o." : " AND o.").append(name);
                    b.append(k < i ? '=' : (asc ? '<' : '>'));
                    b.append(paramPrefix).append(queryInfo.paramCount + 1 + k);
                }
            }
            if (a != null)
                a.append(')');
            if (b != null)
                b.append(')');
        }
        if (a != null)
            queryInfo.jpqlAfterKeyset = new StringBuilder(q).append(a).append(')').append(o).toString();
        if (b != null)
            queryInfo.jpqlBeforeKeyset = new StringBuilder(q).append(b).append(')').append(r).toString();
        queryInfo.keyset = keyset;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "forward & reverse keyset queries", queryInfo.jpqlAfterKeyset, queryInfo.jpqlBeforeKeyset);
    }

    private StringBuilder generateRepositoryQuery(QueryInfo queryInfo, boolean countPages) {
        String methodName = queryInfo.method.getName();
        StringBuilder q = null;
        if (methodName.startsWith("find")) {
            int by = methodName.indexOf("By", 4);
            int c = by < 0 ? 4 : by + 2;
            if (by > 4) {
                if ("findAllById".equals(methodName) && Iterable.class.equals(queryInfo.method.getParameterTypes()[0]))
                    methodName = "findAllByIdIn"; // CrudRepository.findAllById(Iterable)
                else
                    parseFindBy(queryInfo, methodName.substring(4, by));
            }
            int orderBy = methodName.lastIndexOf("OrderBy");
            q = generateSelect(queryInfo);
            if (orderBy > c || orderBy == -1 && methodName.length() > c) {
                int where = q.length();
                generateRepositoryQueryConditions(queryInfo, methodName, c, orderBy > 0 ? orderBy : methodName.length(), q);
                if (countPages)
                    generateCount(queryInfo, q.substring(where));
            }
            if (orderBy >= c)
                generateRepositoryQueryOrderBy(queryInfo, orderBy, q);
            queryInfo.type = QueryInfo.Type.SELECT;
        } else if (methodName.startsWith("delete")) {
            int by = methodName.indexOf("By", 6);
            int c = by < 0 ? 6 : by + 2;
            if (by > 6) {
                if ("deleteAllById".equals(methodName) && Iterable.class.equals(queryInfo.method.getParameterTypes()[0]))
                    methodName = "deleteAllByIdIn"; // CrudRepository.deleteAllById(Iterable)
            } else if (methodName.length() == 6) {
                Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
                if (paramTypes.length == 1 && Object.class.equals(paramTypes[0])) {
                    methodName = "deleteById"; // CrudRepository.delete(entity)
                    queryInfo.paramsNeedConversionToId = true;
                    c = 8;
                }
            } else if (methodName.length() == 9 && methodName.endsWith("All")) {
                Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
                if (paramTypes.length == 1 && Iterable.class.equals(paramTypes[0])) {
                    methodName = "deleteByIdIn"; // CrudRepository.deleteAll(Iterable)
                    queryInfo.paramsNeedConversionToId = true;
                    c = 8;
                }
            }
            q = new StringBuilder(150).append("DELETE FROM ").append(queryInfo.entityInfo.name).append(" o");
            if (methodName.length() > c)
                generateRepositoryQueryConditions(queryInfo, methodName, c, methodName.length(), q);
            queryInfo.type = QueryInfo.Type.DELETE;
        } else if (methodName.startsWith("update")) {
            int by = methodName.indexOf("By", 6);
            int c = by < 0 ? 6 : by + 2;
            q = generateRepositoryUpdateQuery(queryInfo, methodName, c);
            queryInfo.type = QueryInfo.Type.UPDATE;
        } else if (methodName.startsWith("count")) {
            int by = methodName.indexOf("By", 5);
            int c = by < 0 ? 5 : by + 2;
            q = new StringBuilder(150).append("SELECT COUNT(o) FROM ").append(queryInfo.entityInfo.name).append(" o");
            if (methodName.length() > c)
                generateRepositoryQueryConditions(queryInfo, methodName, c, methodName.length(), q);
            queryInfo.type = QueryInfo.Type.COUNT;
        } else if (methodName.startsWith("exists")) {
            int by = methodName.indexOf("By", 6);
            int c = by < 0 ? 6 : by + 2;
            q = new StringBuilder(200).append("SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END FROM ").append(queryInfo.entityInfo.name).append(" o");
            if (methodName.length() > c)
                generateRepositoryQueryConditions(queryInfo, methodName, c, methodName.length(), q);
            queryInfo.type = QueryInfo.Type.EXISTS;
        }

        return q;
    }

    /**
     * Generates JPQL for a *By condition such as MyColumn[Not]Like[IgnoreCase]
     */
    private void generateRepositoryQueryCondition(QueryInfo queryInfo, String methodName, int start, int endBefore, StringBuilder q) {
        char last = methodName.charAt(endBefore - 1);
        boolean ignoreCase;
        if (ignoreCase = last == 'e' && endsWith("IgnoreCas", methodName, start, endBefore - 1)) {
            if (0 == (endBefore -= 10))
                throw new MappingException("Entity property name or condition is required before IgnoreCase."); // TODO
            last = methodName.charAt(endBefore - 1);
        }
        int length = endBefore - start;

        Condition condition = Condition.EQUALS;
        switch (last) {
            case 'n': // GreaterThan | LessThan | In | Between
                if (length > 2) {
                    char ch = methodName.charAt(endBefore - 2);
                    if (ch == 'a') { // GreaterThan | LessThan
                        if (endsWith("GreaterTh", methodName, start, endBefore - 2))
                            condition = Condition.GREATER_THAN;
                        else if (endsWith("LessTh", methodName, start, endBefore - 2))
                            condition = Condition.LESS_THAN;
                    } else if (ch == 'I') { // In
                        condition = Condition.IN;
                    } else if (ch == 'e' && endsWith("Betwe", methodName, start, endBefore - 2)) {
                        condition = Condition.BETWEEN;
                    }
                }
                break;
            case 'l': // GreaterThanEqual | LessThanEqual | Null
                if (length > 4) {
                    char ch = methodName.charAt(endBefore - 2);
                    if (ch == 'a') { // GreaterThanEqual | LessThanEqual
                        if (endsWith("GreaterThanEqu", methodName, start, endBefore - 2))
                            condition = Condition.GREATER_THAN_EQUAL;
                        else if (endsWith("LessThanEqu", methodName, start, endBefore - 2))
                            condition = Condition.LESS_THAN_EQUAL;
                    } else if (ch == 'l' & methodName.charAt(endBefore - 3) == 'u' && methodName.charAt(endBefore - 4) == 'N') {
                        condition = Condition.NULL;
                    }
                }
                break;
            case 'e': // Like, True, False
                if (length > 4) {
                    char ch = methodName.charAt(endBefore - 4);
                    if (ch == 'L') {
                        if (methodName.charAt(endBefore - 3) == 'i' && methodName.charAt(endBefore - 2) == 'k')
                            condition = Condition.LIKE;
                    } else if (ch == 'T') {
                        if (methodName.charAt(endBefore - 3) == 'r' && methodName.charAt(endBefore - 2) == 'u')
                            condition = Condition.TRUE;
                    } else if (endsWith("Fals", methodName, start, endBefore - 1)) {
                        condition = Condition.FALSE;
                    }
                }
                break;
            case 'h': // StartsWith | EndsWith
                if (length > 8) {
                    char ch = methodName.charAt(endBefore - 8);
                    if (ch == 'E') {
                        if (endsWith("ndsWit", methodName, start, endBefore - 1))
                            condition = Condition.ENDS_WITH;
                    } else if (endBefore > 10 && ch == 'a' && endsWith("StartsWit", methodName, start, endBefore - 1)) {
                        condition = Condition.STARTS_WITH;
                    }
                }
                break;
            case 's': // Contains
                if (endsWith("Contain", methodName, start, endBefore - 1))
                    condition = Condition.CONTAINS;
                break;
            case 'y': // Empty
                if (endsWith("Empt", methodName, start, endBefore - 1))
                    condition = Condition.EMPTY;
        }

        boolean negated = endsWith("Not", methodName, start, endBefore - condition.length);

        String attribute = methodName.substring(start, endBefore - condition.length - (negated ? 3 : 0));

        if (negated) {
            Condition negatedCondition = condition.negate();
            if (negatedCondition != null) {
                condition = negatedCondition;
                negated = false;
            }
        }

        // TODO expecting that Upper/Lower will be removed in favor of IgnoreCase
        boolean upper = false;
        boolean lower = false;
        if (attribute.length() > 5 && ((upper = attribute.startsWith("Upper")) || (lower = attribute.startsWith("Lower"))))
            attribute = attribute.substring(5);

        String name = queryInfo.entityInfo.getAttributeName(attribute);
        if (name == null) {
            // Special case for CrudRepository.deleteAll and CrudRepository.findAll
            int len = q.length(), where = q.lastIndexOf(" WHERE (");
            if (where + 8 == len)
                q.delete(where, len); // Remove " WHERE " because there are no conditions
            queryInfo.hasWhere = false;
            return;
        }

        StringBuilder a = new StringBuilder();
        if (upper || ignoreCase)
            a.append("UPPER(o.").append(name).append(')');
        else if (lower)
            a.append("LOWER(o.").append(name).append(')');
        else
            a.append("o.").append(name);

        String attributeExpr = a.toString();

        boolean isCollection = queryInfo.entityInfo.collectionAttributeNames.contains(name);
        if (isCollection)
            condition.verifyCollectionsSupported(name, ignoreCase);

        switch (condition) {
            case STARTS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT(");
                appendParam(q, ignoreCase, ++queryInfo.paramCount).append(", '%')");
                break;
            case ENDS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                appendParam(q, ignoreCase, ++queryInfo.paramCount).append(")");
                break;
            case LIKE:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE ");
                appendParam(q, ignoreCase, ++queryInfo.paramCount);
                break;
            case BETWEEN:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ");
                appendParam(q, ignoreCase, ++queryInfo.paramCount).append(" AND ");
                appendParam(q, ignoreCase, ++queryInfo.paramCount);
                break;
            case CONTAINS:
                if (isCollection) {
                    q.append(" ?").append(++queryInfo.paramCount).append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
                } else {
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                    appendParam(q, ignoreCase, ++queryInfo.paramCount).append(", '%')");
                }
                break;
            case NULL:
            case NOT_NULL:
            case TRUE:
            case FALSE:
                q.append(attributeExpr).append(condition.operator);
                break;
            case EMPTY:
                q.append(attributeExpr).append(isCollection ? Condition.EMPTY.operator : Condition.NULL.operator);
                break;
            case NOT_EMPTY:
                q.append(attributeExpr).append(isCollection ? Condition.NOT_EMPTY.operator : Condition.NOT_NULL.operator);
                break;
            case IN:
                if (ignoreCase)
                    throw new MappingException(new UnsupportedOperationException("Repository keyword IgnoreCase cannot be combined with the In keyword.")); // TODO
            default:
                q.append(attributeExpr).append(negated ? " NOT " : "").append(condition.operator);
                appendParam(q, ignoreCase, ++queryInfo.paramCount);
        }
    }

    /**
     * Appends JQPL for a repository method parameter. Either of the form ?1 or UPPER(?1)
     *
     * @param q     builder for the JPQL query.
     * @param upper indicates if the query parameter should be compared in upper case.
     * @param num   parameter number.
     * @return the same builder for the JPQL query.
     */
    @Trivial
    private static StringBuilder appendParam(StringBuilder q, boolean upper, int num) {
        q.append(upper ? "UPPER(?" : '?').append(num);
        return upper ? q.append(')') : q;
    }

    /**
     * Generates the JPQL WHERE clause for all findBy, deleteBy, or updateBy conditions such as MyColumn[Not]Like[IgnoreCase]
     */
    private void generateRepositoryQueryConditions(QueryInfo queryInfo, String methodName, int start, int endBefore, StringBuilder q) {
        queryInfo.paramCount = 0;
        queryInfo.hasWhere = true;
        q.append(" WHERE (");
        for (int and = start, or = start, iNext = start, i = start; queryInfo.hasWhere && i >= start && iNext < endBefore; i = iNext) {
            and = and == -1 || and > i ? and : methodName.indexOf("And", i);
            or = or == -1 || or > i ? or : methodName.indexOf("Or", i);
            iNext = Math.min(and, or);
            if (iNext < 0)
                iNext = Math.max(and, or);
            generateRepositoryQueryCondition(queryInfo, methodName, i, iNext < 0 || iNext >= endBefore ? endBefore : iNext, q);
            if (iNext > 0 && iNext < endBefore) {
                q.append(iNext == and ? " AND " : " OR ");
                iNext += (iNext == and ? 3 : 2);
            }
        }
        if (queryInfo.hasWhere)
            q.append(')');
    }

    /**
     * Generates the JPQL ORDER BY clause for a repository findBy method such as findByLastNameLikeOrderByLastNameAscFirstNameDesc
     */
    private void generateRepositoryQueryOrderBy(QueryInfo queryInfo, int orderBy, StringBuilder q) {
        String methodName = queryInfo.method.getName();
        boolean needsKeysetQueries = KeysetAwarePage.class.equals(queryInfo.method.getReturnType())
                                     || KeysetAwarePage.class.equals(queryInfo.returnTypeParam);
        StringBuilder o = needsKeysetQueries ? new StringBuilder(100) : q; // forward order
        StringBuilder r = needsKeysetQueries ? new StringBuilder(100).append(" ORDER BY ") : null; // reverse order
        List<Sort> keyset = needsKeysetQueries ? new ArrayList<>() : null;

        o.append(" ORDER BY ");

        for (int length = methodName.length(), asc = 0, desc = 0, iNext, i = orderBy + 7; i >= 0 && i < length; i = iNext) {
            asc = asc == -1 || asc > i ? asc : methodName.indexOf("Asc", i);
            desc = desc == -1 || desc > i ? desc : methodName.indexOf("Desc", i);
            iNext = Math.min(asc, desc);
            if (iNext < 0)
                iNext = Math.max(asc, desc);

            String attribute = iNext < 0 ? methodName.substring(i) : methodName.substring(i, iNext);
            String name = queryInfo.entityInfo.getAttributeName(attribute);
            o.append("o.").append(name);

            if (needsKeysetQueries) {
                r.append("o.").append(name);
                keyset.add(iNext > 0 && iNext == desc ? Sort.desc(name) : Sort.asc(name));
            }

            if (iNext > 0) {
                if (iNext == desc)
                    o.append(" DESC");
                else if (needsKeysetQueries)
                    r.append(" DESC");
                iNext += (iNext == desc ? 4 : 3);
                if (iNext < length) {
                    o.append(", ");
                    if (needsKeysetQueries)
                        r.append(", ");
                }
            }
        }

        if (needsKeysetQueries) {
            generateKeysetQueries(queryInfo, keyset, q, o, r);
            q.append(o);
        }
    }

    /**
     * Generates JPQL for a repository updateBy method such as updateByProductIdSetProductNameMultiplyPrice
     */
    private StringBuilder generateRepositoryUpdateQuery(QueryInfo queryInfo, String methodName, int c) {
        int set = methodName.indexOf("Set", c);
        int add = methodName.indexOf("Add", c);
        int mul = methodName.indexOf("Multiply", c);
        int div = methodName.indexOf("Divide", c);
        int uFirst = Integer.MAX_VALUE;
        if (set > 0 && set < uFirst)
            uFirst = set;
        if (add > 0 && add < uFirst)
            uFirst = add;
        if (mul > 0 && mul < uFirst)
            uFirst = mul;
        if (div > 0 && div < uFirst)
            uFirst = div;
        if (uFirst == Integer.MAX_VALUE)
            throw new IllegalArgumentException(methodName); // updateBy that lacks updates

        // Compute the WHERE clause first due to its parameters being ordered first in the repository method signature
        StringBuilder where = new StringBuilder(150);
        generateRepositoryQueryConditions(queryInfo, methodName, c, uFirst, where);

        StringBuilder q = new StringBuilder(250);
        q.append("UPDATE ").append(queryInfo.entityInfo.name).append(" o SET");

        for (int u = uFirst; u > 0;) {
            boolean first = u == uFirst;
            String op;
            if (u == set) {
                op = null;
                set = methodName.indexOf("Set", u += 3);
            } else if (u == add) {
                op = "+";
                add = methodName.indexOf("Add", u += 3);
            } else if (u == div) {
                op = "/";
                div = methodName.indexOf("Divide", u += 6);
            } else if (u == mul) {
                op = "*";
                mul = methodName.indexOf("Multiply", u += 8);
            } else {
                throw new IllegalStateException(methodName); // internal error
            }

            int next = Integer.MAX_VALUE;
            if (set > u && set < next)
                next = set;
            if (add > u && add < next)
                next = add;
            if (mul > u && mul < next)
                next = mul;
            if (div > u && div < next)
                next = div;

            String attribute = next == Integer.MAX_VALUE ? methodName.substring(u) : methodName.substring(u, next);
            String name = queryInfo.entityInfo.getAttributeName(attribute);
            q.append(first ? " o." : ", o.").append(name == null ? attribute : name).append("=");

            if (op != null)
                q.append("o.").append(name == null ? attribute : name).append(op);
            q.append('?').append(++queryInfo.paramCount);

            u = next == Integer.MAX_VALUE ? -1 : next;
        }

        return q.append(where);
    }

    private StringBuilder generateSelect(QueryInfo queryInfo) {
        StringBuilder q = new StringBuilder(200);
        // TODO entityClass now includes inheritance subtypes and much of the following was already computed.
        Result result = queryInfo.method.getAnnotation(Result.class);
        Select select = queryInfo.method.getAnnotation(Select.class);
        Class<?> type = result == null ? null : result.value();
        String[] cols = select == null ? null : select.value();
        boolean distinct = select != null && select.distinct();
        String function = select == null ? null : toFunctionName(select.function());

        if (type == null) {
            Class<?> returnType = queryInfo.method.getReturnType();
            if (!Iterable.class.isAssignableFrom(returnType)) {
                Class<?> arrayType = returnType.getComponentType();
                returnType = arrayType == null ? returnType : arrayType;
                if (!returnType.isPrimitive()
                    && !returnType.isInterface()
                    && !returnType.isAssignableFrom(queryInfo.entityInfo.type)
                    && !returnType.getName().startsWith("java"))
                    type = returnType;
            }
        }

        q.append("SELECT ");

        if (type == null ||
            queryInfo.entityInfo.inheritance && queryInfo.entityInfo.type.isAssignableFrom(type))
            if (cols == null || cols.length == 0) {
                q.append(distinct ? "DISTINCT o" : "o");
            } else {
                for (int i = 0; i < cols.length; i++) {
                    generateSelectExpression(q, i == 0, function, distinct, cols[i]);
                }
            }
        else {
            q.append("NEW ").append(type.getName()).append('(');
            boolean first = true;
            if (cols == null || cols.length == 0)
                for (String name : queryInfo.entityInfo.attributeNames.values()) {
                    generateSelectExpression(q, first, function, distinct, name);
                    first = false;
                }
            else
                for (int i = 0; i < cols.length; i++) {
                    String name = queryInfo.entityInfo.getAttributeName(cols[i]);
                    generateSelectExpression(q, i == 0, function, distinct, name == null ? cols[i] : name);
                }
            q.append(')');
        }
        q.append(" FROM ").append(queryInfo.entityInfo.name).append(" o");
        return q;
    }

    private void generateSelectExpression(StringBuilder q, boolean isFirst, String function, boolean distinct, String attributeName) {
        if (!isFirst)
            q.append(", ");
        if (function != null)
            q.append(function).append('(');
        q.append(distinct ? "DISTINCT o." : "o.");
        q.append(attributeName);
        if (function != null)
            q.append(')');
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        CompletableFuture<QueryInfo> queryInfoFuture = queries.get(method);

        if (queryInfoFuture == null) {
            String methodName = method.getName();
            if (args == null) {
                if ("hashCode".equals(methodName))
                    return System.identityHashCode(proxy);
                else if ("toString".equals(methodName))
                    return repositoryInterface.getName() + "(Proxy)@" + Integer.toHexString(System.identityHashCode(proxy));
            } else if (args.length == 1) {
                if ("equals".equals(methodName))
                    return proxy == args[0];
            }
            throw new UnsupportedOperationException(method.toString());
        }

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), args);
        try {
            if (isDisposed.get())
                throw new IllegalStateException("Repository instance " + repositoryInterface.getName() +
                                                "(Proxy)@" + Integer.toHexString(System.identityHashCode(proxy)) +
                                                " is no longer in scope."); // TODO

            QueryInfo queryInfo = queryInfoFuture.join();

            LocalTransactionCoordinator suspendedLTC = null;
            EntityManager em = null;
            Object returnValue;
            Class<?> returnType = method.getReturnType();
            boolean failed = true;

            boolean requiresTransaction;
            switch (queryInfo.type) {
                case SELECT:
                case COUNT:
                case EXISTS:
                    requiresTransaction = false;
                    break;
                default:
                    requiresTransaction = Status.STATUS_NO_TRANSACTION == provider.tranMgr.getStatus();
            }

            try {
                if (requiresTransaction) {
                    suspendedLTC = provider.localTranCurrent.suspend();
                    provider.tranMgr.begin();
                }

                switch (queryInfo.type) {
                    case MERGE: {
                        em = queryInfo.entityInfo.persister.createEntityManager();

                        if (queryInfo.saveParamType.isArray()) {
                            ArrayList<Object> results = new ArrayList<>();
                            Object a = args[0];
                            int length = Array.getLength(a);
                            for (int i = 0; i < length; i++)
                                results.add(em.merge(Array.get(a, i)));
                            em.flush();
                            returnValue = results;
                        } else if (Iterable.class.isAssignableFrom(queryInfo.saveParamType)) {
                            ArrayList<Object> results = new ArrayList<>();
                            for (Object e : ((Iterable<?>) args[0]))
                                results.add(em.merge(e));
                            em.flush();
                            returnValue = results;
                        } else {
                            returnValue = em.merge(args[0]);
                            em.flush();
                        }

                        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
                        } else {
                            returnValue = returnType.isInstance(returnValue) ? returnValue : null;
                        }
                        break;
                    }
                    case SELECT: {
                        Collector<Object, Object, Object> collector = null;
                        Consumer<Object> consumer = null;
                        Limit limit = null;
                        Pageable pagination = null;
                        List<Sort> sortList = null;

                        // Jakarta Data allows the method parameter positions after those used as query parameters
                        // to be used for purposes such as pagination and sorting.
                        // Collector is added here for experimentation.
                        for (int i = queryInfo.paramCount; i < (args == null ? 0 : args.length); i++) {
                            Object param = args[i];
                            if (param instanceof Collector)
                                collector = (Collector<Object, Object, Object>) param;
                            else if (param instanceof Consumer)
                                consumer = (Consumer<Object>) param;
                            else if (param instanceof Limit)
                                limit = (Limit) param;
                            else if (param instanceof Pageable)
                                pagination = (Pageable) param;
                            else if (param instanceof Sort)
                                (sortList == null ? (sortList = new ArrayList<>()) : sortList).add((Sort) param);
                            else if (param instanceof Sort[])
                                Collections.addAll(sortList == null ? (sortList = new ArrayList<>()) : sortList, (Sort[]) param);
                        }

                        if (pagination != null)
                            if (sortList == null)
                                sortList = pagination.sorts();
                            else if (sortList != null && !pagination.sorts().isEmpty())
                                throw new DataException("Repository method signature cannot specify Sort parameters if Pageable also has Sort parameters."); // TODO

                        if (sortList != null && !sortList.isEmpty()) {
                            boolean forward = pagination == null || pagination.mode() != Pageable.Mode.CURSOR_PREVIOUS;
                            StringBuilder q = new StringBuilder(queryInfo.jpql);
                            StringBuilder o = null; // ORDER BY clause based on Sorts
                            for (Sort sort : sortList)
                                if (sort == null)
                                    throw new NullPointerException("Sort: null");
                                else {
                                    o = o == null ? new StringBuilder(100).append(" ORDER BY o.") : o.append(", o.");
                                    o.append(sort.property());
                                    if (forward ? sort.isDescending() : sort.isAscending())
                                        o.append(" DESC");
                                }

                            if (pagination == null || pagination.mode() == Pageable.Mode.OFFSET)
                                queryInfo = queryInfo.withJPQL(q.append(o).toString());
                            else // CURSOR_NEXT or CURSOR_PREVIOUS
                                generateKeysetQueries(queryInfo = queryInfo.withJPQL(null), sortList, q, forward ? o : null, forward ? null : o);
                        }

                        boolean asyncCompatibleResultForPagination = pagination != null &&
                                                                     (void.class.equals(returnType) || CompletableFuture.class.equals(returnType)
                                                                      || CompletionStage.class.equals(returnType));

                        if (asyncCompatibleResultForPagination && collector != null)
                            return runAndCollect(queryInfo, pagination, collector, args);
                        else if (asyncCompatibleResultForPagination && consumer != null)
                            return runWithConsumer(queryInfo, pagination, consumer, args);

                        Class<?> type = queryInfo.returnTypeParam != null && (Optional.class.equals(returnType)
                                                                              || CompletableFuture.class.equals(returnType)
                                                                              || CompletionStage.class.equals(returnType)) //
                                                                                              ? queryInfo.returnTypeParam //
                                                                                              : returnType;

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "results to be returned as " + type.getName());

                        if (pagination != null && Iterator.class.equals(type))
                            returnValue = new PaginatedIterator<E>(queryInfo, pagination, args); // TODO keyset pagination
                        else if (KeysetAwareSlice.class.equals(type) || KeysetAwarePage.class.equals(type))
                            returnValue = new KeysetAwarePageImpl<E>(queryInfo, pagination, args);
                        else if (Slice.class.equals(type) || Page.class.equals(type) || pagination != null && Streamable.class.equals(type))
                            returnValue = new PageImpl<E>(queryInfo, pagination, args); // TODO Limit with Page as return type
                        else {
                            em = queryInfo.entityInfo.persister.createEntityManager();

                            TypedQuery<?> query = em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
                            queryInfo.setParameters(query, args);

                            int maxResults = limit != null ? (int) limit.maxResults() // TODO fix data type in spec
                                            : pagination != null ? pagination.size() //
                                                            : queryInfo.maxResults;

                            int startAt = limit != null ? computeOffset(limit) //
                                            : pagination != null && pagination.mode() == Pageable.Mode.OFFSET //
                                                            ? computeOffset(pagination.page(), maxResults) //
                                                            : 0;
                            // TODO keyset pagination without returning KeysetAwareSlice/Page - raise error?
                            if (maxResults > 0)
                                query.setMaxResults(maxResults);
                            if (startAt > 0)
                                query.setFirstResult(startAt);

                            if (collector != null) { // Does not provide much value over directly returning Stream
                                try (Stream<?> stream = query.getResultStream()) {
                                    returnValue = stream.collect(collector);
                                }
                            } else if (consumer != null) { // Does not provide much value over directly returning Stream
                                try (Stream<?> stream = query.getResultStream()) {
                                    stream.forEach(consumer::accept);
                                    returnValue = null;
                                }
                            } else if (BaseStream.class.isAssignableFrom(type)) {
                                Stream<?> stream = query.getResultStream();
                                if (Stream.class.equals(type))
                                    returnValue = stream;
                                else if (IntStream.class.equals(type))
                                    returnValue = stream.mapToInt(RepositoryImpl::toInt);
                                else if (LongStream.class.equals(type))
                                    returnValue = stream.mapToLong(RepositoryImpl::toLong);
                                else if (DoubleStream.class.equals(type))
                                    returnValue = stream.mapToDouble(RepositoryImpl::toDouble);
                                else
                                    throw new UnsupportedOperationException("Stream type " + type.getName());
                            } else {
                                List<?> results = query.getResultList();

                                if (queryInfo.entityInfo.type.equals(type)) {
                                    returnValue = results.isEmpty() ? nullIfOptional(returnType) : oneResult(results);
                                } else if (type.isInstance(results) && (results.isEmpty() || !(results.get(0) instanceof Object[]))) {
                                    returnValue = results;
                                } else if (queryInfo.returnArrayType != null) {
                                    int size = results.size();
                                    if (size == 1 && results.get(0) instanceof Object[]) {
                                        Object[] a = (Object[]) results.get(0);
                                        returnValue = Array.newInstance(queryInfo.returnArrayType, a.length);
                                        for (int i = 0; i < a.length; i++)
                                            Array.set(returnValue, i, queryInfo.returnArrayType.isInstance(a[i]) ? a[i] : to(queryInfo.returnArrayType, a[i]));
                                    } else {
                                        returnValue = Array.newInstance(queryInfo.returnArrayType, size);
                                        int i = 0;
                                        for (Object result : results)
                                            Array.set(returnValue, i++, result);
                                    }
                                } else if (Streamable.class.equals(type)) {
                                    returnValue = new StreamableImpl<>(results);
                                } else if (Iterable.class.isAssignableFrom(type)) {
                                    try {
                                        Collection<Object> list;
                                        if (type.isInterface()) {
                                            if (type.isAssignableFrom(ArrayList.class)) // covers Iterable, Collection, List
                                                list = new ArrayList<>(results.size());
                                            else if (type.isAssignableFrom(ArrayDeque.class)) // covers Queue, Deque
                                                list = new ArrayDeque<>(results.size());
                                            else if (type.isAssignableFrom(LinkedHashSet.class)) // covers Set
                                                list = new LinkedHashSet<>(results.size());
                                            else
                                                throw new UnsupportedOperationException(type + " is an unsupported return type.");
                                        } else {
                                            @SuppressWarnings("unchecked")
                                            Constructor<? extends Collection<Object>> c = (Constructor<? extends Collection<Object>>) type.getConstructor();
                                            list = c.newInstance();
                                        }
                                        if (results.size() == 1 && results.get(0) instanceof Object[]) {
                                            Object[] a = (Object[]) results.get(0);
                                            for (int i = 0; i < a.length; i++)
                                                list.add(queryInfo.returnTypeParam.isInstance(a[i]) ? a[i] : to(queryInfo.returnTypeParam, a[i]));
                                        } else {
                                            list.addAll(results);
                                        }
                                        returnValue = list;
                                    } catch (NoSuchMethodException x) {
                                        throw new UnsupportedOperationException(type + " lacks public zero parameter constructor.");
                                    }
                                } else if (results.isEmpty()) {
                                    returnValue = nullIfOptional(returnType);
                                } else { // single result of other type
                                    returnValue = oneResult(results);
                                    if (returnValue != null && !type.isAssignableFrom(returnValue.getClass())) {
                                        // TODO these conversions are not all safe
                                        if (double.class.equals(type) || Double.class.equals(type))
                                            returnValue = ((Number) returnValue).doubleValue();
                                        else if (float.class.equals(type) || Float.class.equals(type))
                                            returnValue = ((Number) returnValue).floatValue();
                                        else if (long.class.equals(type) || Long.class.equals(type))
                                            returnValue = ((Number) returnValue).longValue();
                                        else if (int.class.equals(type) || Integer.class.equals(type))
                                            returnValue = ((Number) returnValue).intValue();
                                        else if (short.class.equals(type) || Short.class.equals(type))
                                            returnValue = ((Number) returnValue).shortValue();
                                        else if (byte.class.equals(type) || Byte.class.equals(type))
                                            returnValue = ((Number) returnValue).byteValue();
                                    }
                                }
                            }
                        }

                        if (Optional.class.equals(returnType)) {
                            returnValue = returnValue == null
                                          || returnValue instanceof Collection && ((Collection<?>) returnValue).isEmpty()
                                          || returnValue instanceof Slice && !((Slice<?>) returnValue).hasContent() //
                                                          ? Optional.empty() : Optional.of(returnValue);
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue);
                        }
                        break;
                    }
                    case DELETE:
                    case UPDATE: {
                        em = queryInfo.entityInfo.persister.createEntityManager();

                        jakarta.persistence.Query update = em.createQuery(queryInfo.jpql);
                        queryInfo.setParameters(update, args);

                        int updateCount = update.executeUpdate();

                        returnValue = toReturnValue(updateCount, returnType, queryInfo);
                        break;
                    }
                    case COUNT: {
                        em = queryInfo.entityInfo.persister.createEntityManager();

                        TypedQuery<Long> query = em.createQuery(queryInfo.jpql, Long.class);
                        queryInfo.setParameters(query, args);

                        Long result = query.getSingleResult();

                        Class<?> type = queryInfo.returnTypeParam != null && (Optional.class.equals(returnType)
                                                                              || CompletableFuture.class.equals(returnType)
                                                                              || CompletionStage.class.equals(returnType)) //
                                                                                              ? queryInfo.returnTypeParam //
                                                                                              : returnType;
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "result " + result + " to be returned as " + type.getName());

                        returnValue = result;

                        if (!type.isInstance(result) && !type.isAssignableFrom(returnValue.getClass())) {
                            // TODO these conversions are not all safe
                            if (int.class.equals(type) || Integer.class.equals(type))
                                returnValue = result.intValue();
                            else if (short.class.equals(type) || Short.class.equals(type))
                                returnValue = result.shortValue();
                            else if (byte.class.equals(type) || Byte.class.equals(type))
                                returnValue = result.byteValue();
                        }

                        if (Optional.class.equals(returnType)) {
                            returnValue = Optional.of(returnValue);
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue);
                        }
                        break;
                    }
                    case EXISTS: {
                        em = queryInfo.entityInfo.persister.createEntityManager();

                        TypedQuery<Boolean> query = em.createQuery(queryInfo.jpql, Boolean.class);
                        queryInfo.setParameters(query, args);

                        returnValue = query.getSingleResult();

                        if (Optional.class.equals(returnType)) {
                            returnValue = Optional.of(returnValue);
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            returnValue = CompletableFuture.completedFuture(returnValue);
                        }
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException(queryInfo.type.name());
                }

                failed = false;
            } finally {
                if (em != null)
                    em.close();

                if (requiresTransaction) {
                    try {
                        int status = provider.tranMgr.getStatus();
                        if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                            provider.tranMgr.rollback();
                        else if (status != Status.STATUS_NO_TRANSACTION)
                            provider.tranMgr.commit();
                    } finally {
                        if (suspendedLTC != null)
                            provider.localTranCurrent.resume(suspendedLTC);
                    }
                } else {
                    if (failed && Status.STATUS_ACTIVE == provider.tranMgr.getStatus())
                        provider.tranMgr.setRollbackOnly();
                }
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), returnValue);
            return returnValue;
        } catch (Throwable x) {
            if (x instanceof Exception)
                x = failure((Exception) x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), x);
            throw x;
        }
    }

    /**
     * Handles the cases where no results are found by return null or raising EmptyResultException.
     *
     * @param returnType return type of repository method.
     * @return null if the specified return type is java.util.Optional.
     * @throws EmptyResultException if not Optional.
     */
    @Trivial
    private final Void nullIfOptional(Class<?> returnType) {
        if (Optional.class.equals(returnType))
            return null;
        else
            throw new EmptyResultException("Query with return type of " + returnType.getName() +
                                           " returned no results. If this is expected, specify a return type of array, Collection, or Optional for the repository method.");
    }

    @Trivial
    private final Object oneResult(List<?> results) {
        if (results.size() == 1)
            return results.get(0);
        else
            throw new NonUniqueResultException("Found " + results.size() +
                                               " results. To limit to a single result, specify Limit.of(1) as a parameter or use the findFirstBy name pattern.");
    }

    /**
     * Parses and handles the text between find___By of a repository method.
     * Currently this is only "First" or "First#".
     *
     * @param queryInfo partially complete query information to populate with a maxResults value for findFirst(#)By...
     * @param s         the portion of the method name between find and By to parse.
     */
    private void parseFindBy(QueryInfo queryInfo, String s) {
        int first = s.indexOf("First");
        if (first >= 0) {
            int length = s.length();
            int num = first + 5 == length ? 1 : 0;
            if (num == 0)
                for (int c = first + 5; c < length; c++) {
                    char ch = s.charAt(c);
                    if (ch >= '0' && ch <= '9') {
                        if (num <= (Integer.MAX_VALUE - (ch - '0')) / 10)
                            num = num * 10 + (ch - '0');
                        else
                            throw new UnsupportedOperationException(s + " exceeds Integer.MAX_VALUE (2147483647)."); // TODO
                    } else {
                        if (c == first + 5)
                            num = 1;
                        break;
                    }
                }
            if (num == 0)
                throw new DataException(s); // First0
            else
                queryInfo.maxResults = num;
        }
    }

    /**
     * This is an experiment with allowing a collector to perform reduction
     * on the contents of each page as the page is read in. This avoids having
     * all pages loaded at once and gives the application a completion stage
     * with a final result that can be awaited, or to which dependent stages
     * can be added to run once the final result is ready.
     *
     * @param queryInfo
     * @param pagination
     * @param collector
     * @param args
     * @return completion stage that is already completed if only being used to
     *         supply the result to an Asynchronous method. If the database supports
     *         asynchronous patterns, it could be a not-yet-completed completion stage
     *         that is controlled by the database's async support.
     */
    private CompletableFuture<Object> runAndCollect(QueryInfo queryInfo, Pageable pagination,
                                                    Collector<Object, Object, Object> collector,
                                                    Object[] args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Object r = collector.supplier().get();
        BiConsumer<Object, Object> accumulator = collector.accumulator();

        // TODO it would be possible to process multiple pages in parallel if we wanted to and if the collector supports it
        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<E> query = (TypedQuery<E>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
            queryInfo.setParameters(query, args);

            List<E> page;
            int maxPageSize;
            do {
                // TODO Keyset pagination
                maxPageSize = pagination.size();
                query.setFirstResult(computeOffset(pagination.page(), maxPageSize));
                query.setMaxResults(maxPageSize);
                pagination = pagination.next();

                page = query.getResultList();

                for (Object item : page)
                    accumulator.accept(r, item);

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Processed page with " + page.size() + " results");
            } while (pagination != null && page.size() == maxPageSize);
        } finally {
            em.close();
        }

        return void.class.equals(queryInfo.method.getReturnType()) ? null : CompletableFuture.completedFuture(collector.finisher().apply(r));
    }

    /**
     * Copies the Jakarta NoSQL pattern of invoking a Consumer with the value of each result.
     *
     * @param queryInfo
     * @param pagination
     * @param consumer
     * @param args
     * @return completion stage that is already completed if only being used to
     *         run as an Asynchronous method. If the database supports
     *         asynchronous patterns, it could be a not-yet-completed completion stage
     *         that is controlled by the database's async support.
     */
    private CompletableFuture<Void> runWithConsumer(QueryInfo queryInfo, Pageable pagination, Consumer<Object> consumer, Object[] args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // TODO it would be possible to process multiple pages in parallel if we wanted to and if the consumer supports it
        EntityManager em = queryInfo.entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<E> query = (TypedQuery<E>) em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
            queryInfo.setParameters(query, args);

            List<E> page;
            int maxPageSize;
            do {
                // TODO Keyset pagination
                maxPageSize = pagination.size();
                query.setFirstResult(computeOffset(pagination.page(), maxPageSize));
                query.setMaxResults(maxPageSize);
                pagination = pagination.next();

                page = query.getResultList();

                for (Object item : page)
                    consumer.accept(item);

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Processed page with " + page.size() + " results");
            } while (pagination != null && page.size() == maxPageSize);
        } finally {
            em.close();
        }

        return void.class.equals(queryInfo.method.getReturnType()) ? null : CompletableFuture.completedFuture(null);
    }

    /**
     * Converts to the specified type, raising an error if the conversion cannot be made.
     *
     * @param type type to convert to.
     * @param item item to convert.
     * @return new instance of the requested type.
     */
    private static final Object to(Class<?> type, Object item) {
        Object result = item;
        if (item == null) {
            if (type.isPrimitive())
                throw new NullPointerException();
        } else if (item instanceof Number && (type.isPrimitive() || Number.class.isAssignableFrom(type))) {
            Number n = (Number) item;
            if (long.class.equals(type) || Long.class.equals(type))
                result = n.longValue();
            else if (double.class.equals(type) || Double.class.equals(type))
                result = n.doubleValue();
            else if (float.class.equals(type) || Float.class.equals(type))
                result = n.floatValue();
            else if (int.class.equals(type) || Integer.class.equals(type))
                result = n.intValue();
            else if (short.class.equals(type) || Short.class.equals(type))
                result = n.shortValue();
            else if (byte.class.equals(type) || Byte.class.equals(type))
                result = n.byteValue();
            else if (boolean.class.equals(type) || Boolean.class.equals(type))
                result = n.longValue() != 0L;
        } else if (type.isAssignableFrom(String.class)) {
            result = item.toString();
        }
        if (result == item && item != null)
            throw new DataException("Query returned a result of type " + item.getClass().getName() +
                                    " which is not compatible with the type that is expected by the repository method signature: " +
                                    type.getName()); // TODO
        return result;
    }

    @Trivial
    private static final double toDouble(Object o) {
        if (o instanceof Number)
            return ((Number) o).doubleValue();
        else if (o instanceof String)
            return Double.parseDouble((String) o);
        else
            throw new IllegalArgumentException("Not representable as a double value: " + o.getClass().getName());
    }

    @Trivial
    private static final String toFunctionName(Aggregate function) {
        switch (function) {
            case UNSPECIFIED:
                return null;
            case AVERAGE:
                return "AVG";
            case MAXIMUM:
                return "MAX";
            case MINIMUM:
                return "MIN";
            default: // COUNT, SUM
                return function.name();
        }
    }

    @Trivial
    private static final int toInt(Object o) {
        if (o instanceof Number)
            return ((Number) o).intValue();
        else if (o instanceof String)
            return Integer.parseInt((String) o);
        else
            throw new IllegalArgumentException("Not representable as an int value: " + o.getClass().getName());
    }

    @Trivial
    private static final long toLong(Object o) {
        if (o instanceof Number)
            return ((Number) o).longValue();
        else if (o instanceof String)
            return Long.parseLong((String) o);
        else
            throw new IllegalArgumentException("Not representable as a long value: " + o.getClass().getName());
    }

    private static final Object toReturnValue(int i, Class<?> returnType, QueryInfo queryInfo) {
        Object result;
        if (int.class.equals(returnType) || Integer.class.equals(returnType) || Number.class.equals(returnType))
            result = i;
        else if (long.class.equals(returnType) || Long.class.equals(returnType))
            result = Long.valueOf(i);
        else if (boolean.class.equals(returnType) || Boolean.class.equals(returnType))
            result = i != 0;
        else if (void.class.equals(returnType) || Void.class.equals(returnType))
            result = null;
        else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType))
            result = CompletableFuture.completedFuture(toReturnValue(i, queryInfo.returnTypeParam, null));
        else
            throw new UnsupportedOperationException("Return update count as " + returnType);

        return result;
    }
}
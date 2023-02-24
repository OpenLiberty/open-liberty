/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.data.exceptions.DataConnectionException;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.repository.Compare;
import jakarta.data.repository.Count;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Exists;
import jakarta.data.repository.Filter;
import jakarta.data.repository.KeysetAwarePage;
import jakarta.data.repository.KeysetAwareSlice;
import jakarta.data.repository.Limit;
import jakarta.data.repository.Operation;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Page;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Select;
import jakarta.data.repository.Select.Aggregate;
import jakarta.data.repository.Slice;
import jakarta.data.repository.Sort;
import jakarta.data.repository.Streamable;
import jakarta.data.repository.Update;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Inheritance;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R, E> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private static final Set<Class<?>> SPECIAL_PARAM_TYPES = new HashSet<>(Arrays.asList //
    (Limit.class, Pageable.class, Sort.class, Sort[].class));

    private static final Set<Compare> SUPPORTS_COLLECTIONS = Set.of //
    (Compare.Equal, Compare.Contains, Compare.Empty, Compare.Not, Compare.NotContains, Compare.NotEmpty);

    AtomicBoolean isDisposed = new AtomicBoolean();
    private final PersistenceDataProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    private final Class<R> repositoryInterface;

    public RepositoryImpl(PersistenceDataProvider provider, Class<R> repositoryInterface, Class<E> defaultEntityClass) {
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        boolean inheritance = defaultEntityClass.getAnnotation(Inheritance.class) != null;

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
     * Appends JQPL for a repository method parameter. Either of the form ?1 or LOWER(?1)
     *
     * @param q     builder for the JPQL query.
     * @param lower indicates if the query parameter should be compared in lower case.
     * @param num   parameter number.
     * @return the same builder for the JPQL query.
     */
    @Trivial
    private static StringBuilder appendParam(StringBuilder q, boolean lower, int num) {
        q.append(lower ? "LOWER(?" : '?').append(num);
        return lower ? q.append(')') : q;
    }

    /**
     * Appends JQPL for parameters/values, of one of the following forms, depending on the filter.
     * <li> ?1 or LOWER(?1)
     * <li> :name or LOWER(:name)
     * <li> 'value' or LOWER('value')
     * In the case of Between, does this for both arguments, with AND between them.
     *
     * @param q     builder for the JPQL query.
     * @param lower indicates if the query parameter should be compared in lower case.
     * @param num   parameter number.
     * @return the same builder for the JPQL query.
     */
    @Trivial
    private static StringBuilder appendParamOrValue(StringBuilder q, QueryInfo queryInfo, Filter filter) {
        boolean lower = filter.ignoreCase();
        String[] params = filter.param();
        String[] values = filter.value();
        int numArgs = filter.op() == Compare.Between || filter.op() == Compare.NotBetween ? 2 : 1;
        for (int i = 0; i < numArgs; i++) {
            if (i > 0)
                q.append(" AND "); // BETWEEN ?1 AND ?2
            if (lower)
                q.append("LOWER(");
            if (params.length > i) {
                if (queryInfo.paramNames == null)
                    queryInfo.paramNames = new ArrayList<>(); // content is computed later from method signature
                if (numArgs == 1 && params.length > 1) { // IN (:param1, :param2, :param3)
                    for (int p = 0; p < params.length; p++)
                        q.append(p == 0 ? "(" : ", ").append(':').append(params[p]);
                    q.append(')');
                } else {
                    q.append(':').append(params[i]); // TODO if this is null, could use values[i]
                }
            } else if (values.length > i) {
                if (numArgs == 1 && values.length > 1) { // IN ('value1', 'value2', 'value3')
                    for (int v = 0; v < values.length; v++) {
                        q.append(v == 0 ? "(" : ", ");
                        char c = values[v].length() == 0 ? ' ' : values[v].charAt(0);
                        boolean enquote = (c < '0' || c > '9') && c != '\'';
                        if (enquote)
                            q.append("'");
                        q.append(values[v]);
                        if (enquote)
                            q.append("'");
                    }
                    q.append(')');
                } else {
                    char c = values[i].length() == 0 ? ' ' : values[i].charAt(0);
                    boolean enquote = (c < '0' || c > '9') && c != '\'';
                    if (enquote)
                        q.append("'");
                    q.append(values[i]);
                    if (enquote)
                        q.append("'");
                }
            } else { // positional parameter
                q.append('?').append(++queryInfo.paramCount);
            }
            if (lower)
                q.append(")");
        }
        return q;
    }

    /**
     * Appends JQPL to sort based on the specified entity attribute.
     * For most properties will be of a form such as o.Name or LOWER(o.Name) DESC or ...
     *
     * @param q             builder for the JPQL query.
     * @param o             variable referring to the entity.
     * @param Sort          sort criteria for a single attribute (name must already be converted to a valid entity attribute name).
     * @param sameDirection indicate to append the Sort in the normal direction. Otherwise reverses it (for keyset pagination in previous page direction).
     * @return the same builder for the JPQL query.
     */
    @Trivial
    private void appendSort(StringBuilder q, String o, Sort sort, boolean sameDirection) {

        q.append(sort.ignoreCase() ? "LOWER(" : "").append(o).append('.').append(sort.property());

        if (sort.ignoreCase())
            q.append(")");

        if (sameDirection) {
            if (sort.isDescending())
                q.append(" DESC");
        } else {
            if (sort.isAscending())
                q.append(" DESC");
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

        boolean countPages = Page.class.equals(queryInfo.method.getReturnType())
                             || KeysetAwarePage.class.equals(queryInfo.method.getReturnType())
                             || Page.class.equals(queryInfo.returnTypeParam)
                             || KeysetAwarePage.class.equals(queryInfo.returnTypeParam);
        StringBuilder q = null;

        // TODO would it be more efficient to invoke method.getAnnotations() once?

        Query query = queryInfo.method.getAnnotation(Query.class);
        if (query == null) {
            // Query by annotations
            Filter[] filters = queryInfo.method.getAnnotationsByType(Filter.class);
            StringBuilder whereClause = filters.length > 0 ? generateWhereClause(queryInfo, filters) : null;
            String o = queryInfo.entityVar;

            Update[] updates = queryInfo.method.getAnnotationsByType(Update.class);
            if (updates.length > 0) {
                queryInfo.type = QueryInfo.Type.UPDATE;
                q = generateUpdateClause(queryInfo, updates);
                if (whereClause != null)
                    q.append(whereClause);
            } else if (queryInfo.method.getAnnotation(Delete.class) != null) {
                queryInfo.type = QueryInfo.Type.DELETE;
                q = new StringBuilder(13 + o.length() + entityInfo.name.length() + (whereClause == null ? 0 : whereClause.length())) //
                                .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
                if (whereClause != null)
                    q.append(whereClause);
            } else if (queryInfo.method.getAnnotation(Count.class) != null) {
                queryInfo.type = QueryInfo.Type.COUNT;
                q = new StringBuilder(21 + 2 * o.length() + entityInfo.name.length() + (whereClause == null ? 0 : whereClause.length())) //
                                .append("SELECT COUNT(").append(o).append(") FROM ") //
                                .append(entityInfo.name).append(' ').append(o);
                if (whereClause != null)
                    q.append(whereClause);
            } else if (queryInfo.method.getAnnotation(Exists.class) != null) {
                queryInfo.type = QueryInfo.Type.EXISTS;
                String attrName = entityInfo.getAttributeName(entityInfo.idClass == null ? "id" : entityInfo.idClassAttributeAccessors.firstKey());
                q = new StringBuilder(15 + 2 * o.length() + attrName.length() + entityInfo.name.length() + (whereClause == null ? 0 : whereClause.length())) //
                                .append("SELECT ").append(o).append('.').append(attrName) //
                                .append(" FROM ").append(entityInfo.name).append(' ').append(o);
                if (whereClause != null)
                    q.append(whereClause);
            } else if (whereClause != null) {
                queryInfo.type = QueryInfo.Type.SELECT;
                q = generateSelectClause(queryInfo).append(whereClause);
                if (countPages && queryInfo.type == QueryInfo.Type.SELECT)
                    generateCount(queryInfo, whereClause.toString());
            } else if (queryInfo.method.getName().startsWith("save")) {
                queryInfo.type = QueryInfo.Type.MERGE;
                Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
                if (paramTypes.length == 0)
                    throw new UnsupportedOperationException(queryInfo.method.getName() + " without any parameters");
                queryInfo.saveParamType = paramTypes[0];
            } else {
                // Query by method name
                q = generateMethodNameQuery(queryInfo, countPages);//keyset queries before orderby

                // @Select annotation only
                if (q == null) {
                    Select select = queryInfo.method.getAnnotation(Select.class);
                    if (select != null) {
                        queryInfo.type = QueryInfo.Type.SELECT;
                        q = generateSelectClause(queryInfo);
                        if (countPages)
                            generateCount(queryInfo, null);
                    }
                }
            }
        } else { // @Query annotation
            queryInfo.jpql = query.value();

            String upper = queryInfo.jpql.toUpperCase();
            String upperTrimmed = upper.stripLeading();
            if (upperTrimmed.startsWith("SELECT")) {
                int orderBy = upper.lastIndexOf("ORDER BY");
                queryInfo.type = QueryInfo.Type.SELECT;
                queryInfo.sorts = queryInfo.sorts == null ? new ArrayList<>() : queryInfo.sorts;
                queryInfo.jpqlCount = query.count().length() > 0 ? query.count() : null;

                int select = upper.length() - upperTrimmed.length();
                int from = find("FROM", upper, select + 9);
                if (from > 0) {
                    int entityName = find(entityInfo.name.toUpperCase(), upper, from + 5);
                    if (entityName > 0) {
                        String entityVar = findEntityVariable(queryInfo.jpql, entityName + entityInfo.name.length() + 1);
                        if (entityVar != null)
                            queryInfo.entityVar = entityVar;
                    }

                    if (countPages && queryInfo.jpqlCount == null) {
                        // Attempt to infer from provided query
                        String s = queryInfo.jpql.substring(select + 6, from);
                        int comma = s.indexOf(',');
                        if (comma > 0)
                            s = s.substring(0, comma);
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
        if (query != null || queryInfo.paramNames != null) {
            int initialParamCount = queryInfo.paramCount;
            Parameter[] params = queryInfo.method.getParameters();
            Class<?> paramType;
            for (int i = 0; i < params.length && !SPECIAL_PARAM_TYPES.contains(paramType = params[i].getType()); i++) {
                Param param = params[i].getAnnotation(Param.class);
                if (param != null) {
                    if (queryInfo.paramNames == null)
                        queryInfo.paramNames = new ArrayList<>();
                    if (paramType.equals(queryInfo.entityInfo.idClass))
                        for (int p = 1, numIdClassParams = queryInfo.entityInfo.idClassAttributeAccessors.size(); p <= numIdClassParams; p++) {
                            queryInfo.paramNames.add(new StringBuilder(param.value()).append('_').append(p).toString());
                            if (p > 1) {
                                queryInfo.paramCount++;
                                queryInfo.paramAddedCount++;
                            }
                        }
                    else
                        queryInfo.paramNames.add(param.value());
                }
                queryInfo.paramCount++;
                if (initialParamCount != 0)
                    throw new MappingException("Cannot mix positional and named parameters on repository method " +
                                               queryInfo.method.getDeclaringClass().getName() + '.' + queryInfo.method.getName()); // TODO NLS
            }
        }

        // The @OrderBy annotation from Jakarta Data provides sort criteria statically
        OrderBy[] orderBy = queryInfo.method.getAnnotationsByType(OrderBy.class);
        if (orderBy.length > 0) {
            queryInfo.type = queryInfo.type == null ? QueryInfo.Type.SELECT : queryInfo.type;
            queryInfo.sorts = queryInfo.sorts == null ? new ArrayList<>(orderBy.length + 2) : queryInfo.sorts;
            if (q == null)
                if (queryInfo.jpql == null) {
                    q = generateSelectClause(queryInfo);
                    if (countPages)
                        generateCount(queryInfo, null);
                } else {
                    q = new StringBuilder(queryInfo.jpql);
                }

            for (int i = 0; i < orderBy.length; i++)
                queryInfo.addSort(orderBy[i].ignoreCase(), orderBy[i].value(), orderBy[i].descending());

            if (!queryInfo.hasDynamicSortCriteria())
                generateOrderBy(queryInfo, q);
        }

        queryInfo.jpql = q == null ? queryInfo.jpql : q.toString();

        if (queryInfo.type == null)
            throw new MappingException("Repository method name " + queryInfo.method.getName() +
                                       " does not map to a valid query. Some examples of valid method names are:" +
                                       " save(entity), findById(id), findByPriceLessThanEqual(maxPrice), deleteById(id)," +
                                       " existsById(id), countByPriceBetween(min, max), updateByIdSetPrice(id, newPrice)"); // TODO NLS

        return queryInfo;
    }

    /**
     * Compute the zero-based offset to use as a starting point for a Limit range.
     *
     * @param limit limit that was specified by the application.
     * @return offset value.
     * @throws DataException with chained IllegalArgumentException if the starting point for
     *                           the limited range is not positive or would overflow Integer.MAX_VALUE.
     */
    static int computeOffset(Limit range) {
        long startIndex = range.startAt() - 1;
        if (startIndex >= 0 && startIndex <= Integer.MAX_VALUE)
            return (int) startIndex;
        else
            throw new DataException(new IllegalArgumentException("The starting point for " + range + " is not within 1 to Integer.MAX_VALUE (2147483647).")); // TODO
    }

    /**
     * Compute the zero-based offset for the start of a page.
     *
     * @param pagination requested pagination.
     * @return offset for the specified page.
     * @throws DataException with chained IllegalArgumentException if the offset exceeds Integer.MAX_VALUE
     *                           or the Pageable requests keyset pagination.
     */
    static int computeOffset(Pageable pagination) {
        if (pagination.mode() != Pageable.Mode.OFFSET)
            throw new DataException(new IllegalArgumentException("Keyset pagination mode " + pagination.mode() +
                                                                 " can only be used with repository methods with the following return types: " +
                                                                 KeysetAwarePage.class.getName() + ", " + KeysetAwareSlice.class.getName() +
                                                                 ", " + Iterator.class.getName() +
                                                                 ". For offset pagination, use a Pageable without a keyset.")); // TODO NLS
        int maxPageSize = pagination.size();
        long pageIndex = pagination.page() - 1; // zero-based
        if (Integer.MAX_VALUE / maxPageSize >= pageIndex)
            return (int) (pageIndex * maxPageSize);
        else
            throw new DataException(new IllegalArgumentException("The offset for " + pagination.page() + " pages of size " + maxPageSize +
                                                                 " exceeds Integer.MAX_VALUE (2147483647).")); // TODO
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
                else if (cause instanceof SQLSyntaxErrorException)
                    x = new MappingException(original);
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
            Throwable cause = original.getCause();
            if (cause == null)
                x = new MappingException(original);
            else if (DataException.class.equals(cause.getClass()))
                x = new DataException(cause.getMessage(), cause);
            else if (DataConnectionException.class.equals(cause.getClass()))
                x = new DataConnectionException(cause.getMessage(), cause);
            else if (EmptyResultException.class.equals(cause.getClass()))
                x = new EmptyResultException(cause.getMessage(), cause);
            else if (MappingException.class.equals(cause.getClass()))
                x = new MappingException(cause.getMessage(), cause);
            else if (NonUniqueResultException.class.equals(cause.getClass()))
                x = new NonUniqueResultException(cause.getMessage(), cause);
            else
                x = new MappingException(cause);
        } else if (original instanceof IllegalArgumentException) {
            // Example: Problem compiling [SELECT o FROM Account o WHERE (o.accountId>?1)]. The
            // relationship mapping 'o.accountId' cannot be used in conjunction with the > operator
            x = new MappingException(original);
        } else if (original instanceof RuntimeException) {
            // Per EclipseLink, "This exception is used for any problem that is detected with a descriptor or mapping"
            if ("org.eclipse.persistence.exceptions.DescriptorException".equals(original.getClass().getName()))
                x = new MappingException(original);
            else
                x = (RuntimeException) original;
        } else {
            x = new DataException(original);
        }

        if (trace && tc.isDebugEnabled())
            if (x == original)
                Tr.debug(tc, "Failure occurred: " + x.getClass().getName());
            else
                Tr.debug(tc, original.getClass().getName() + " replaced with " + x.getClass().getName());
        return x;
    }

    /**
     * Finds the first occurrence of the text followed by a non-alphanumeric/non-underscore character.
     *
     * @param lookFor text to find.
     * @param findIn  where to look for it.
     * @param startAt starting position.
     * @return index where found, otherwise -1.
     */
    private static int find(String lookFor, String findIn, int startAt) {
        int totalLength = findIn.length();
        for (int foundAt; startAt < totalLength && (foundAt = findIn.indexOf(lookFor, startAt)) > 0; startAt = foundAt + 1) {
            int nextPosition = foundAt + lookFor.length();
            if (nextPosition >= totalLength)
                break;
            char ch = findIn.charAt(nextPosition);
            if (!Character.isLowerCase(ch) && !Character.isUpperCase(ch) && !Character.isDigit(ch) && ch != '_')
                return foundAt;
        }
        return -1;
    }

    /**
     * Finds the entity variable name after the start of the entity name.
     * Examples of JPQL:
     * ... FROM Order o, Product p ...
     * ... FROM Product AS p ...
     *
     * @param findIn  where to look for it.
     * @param startAt position after the end of the entity name.
     * @return entity variable name. Null if none is found.
     */
    private static String findEntityVariable(String findIn, int startAt) {
        int length = findIn.length();
        boolean foundStart = false;
        for (int c = startAt; c < length; c++) {
            char ch = findIn.charAt(c);
            if (Character.isLowerCase(ch) || Character.isUpperCase(ch) || Character.isDigit(ch) || ch == '_') {
                if (!foundStart) {
                    startAt = c;
                    foundStart = true;
                }
            } else { // not part of the entity variable name
                if (foundStart) {
                    String found = findIn.substring(startAt, c);
                    if ("AS".equalsIgnoreCase(found))
                        foundStart = false;
                    else
                        return found;
                }
            }
        }
        return foundStart ? findIn.substring(startAt) : null;
    }

    /**
     * Generates JPQL for a *By condition such as MyColumn[IgnoreCase][Not]Like
     */
    private void generateCondition(QueryInfo queryInfo, String methodName, int start, int endBefore, StringBuilder q) {
        int length = endBefore - start;

        Condition condition = Condition.EQUALS;
        switch (methodName.charAt(endBefore - 1)) {
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

        boolean ignoreCase = endsWith("IgnoreCase", methodName, start, endBefore - condition.length - (negated ? 3 : 0));

        String attribute = methodName.substring(start, endBefore - condition.length - (ignoreCase ? 10 : 0) - (negated ? 3 : 0));

        if (attribute.length() == 0)
            throw new MappingException("Entity property name is missing."); // TODO possibly combine with unknown entity property name

        String name = queryInfo.entityInfo.getAttributeName(attribute);
        if (name == null) {
            if (attribute.length() == 3) {
                // Special case for CrudRepository.deleteAll and CrudRepository.findAll
                int len = q.length(), where = q.lastIndexOf(" WHERE (");
                if (where + 8 == len)
                    q.delete(where, len); // Remove " WHERE " because there are no conditions
                queryInfo.hasWhere = false;
            } else if (queryInfo.entityInfo.idClass != null && attribute.equalsIgnoreCase("id")) {
                generateConditionsForIdClass(queryInfo, null, condition, ignoreCase, negated, q);
            }
            return;
        }

        String o = queryInfo.entityVar;
        StringBuilder attributeExpr = new StringBuilder();
        if (ignoreCase)
            attributeExpr.append("LOWER(").append(o).append('.').append(name).append(')');
        else
            attributeExpr.append(o).append('.').append(name);

        if (negated) {
            Condition negatedCondition = condition.negate();
            if (negatedCondition != null) {
                condition = negatedCondition;
                negated = false;
            }
        }

        boolean isCollection = Collection.class.equals(queryInfo.entityInfo.attributeTypes.get(name));
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
     * Generates JPQL for a *By condition on the IdClass, which expands to multiple conditions in JPQL.
     */
    private void generateConditionsForIdClass(QueryInfo queryInfo, Filter filter, Condition condition, boolean ignoreCase, boolean negate, StringBuilder q) {
        if (filter != null && filter.value().length != 0)
            throw new MappingException("IdClass parameter cannot be represented as a hard-coded value of the @Filter annotation."); // TODO NLS

        String paramName = filter == null || filter.param().length == 0 ? null : filter.param()[0];
        String o = queryInfo.entityVar;

        q.append(negate ? "NOT (" : "(");

        int count = 0;
        for (String idClassAttr : queryInfo.entityInfo.idClassAttributeAccessors.keySet()) {
            if (++count != 1)
                q.append(" AND ");

            String name = queryInfo.entityInfo.getAttributeName(idClassAttr);
            if (ignoreCase)
                q.append("LOWER(").append(o).append('.').append(name).append(')');
            else
                q.append(o).append('.').append(name);

            switch (condition) {
                case EQUALS:
                case NOT_EQUALS:
                    q.append(condition.operator);
                    if (paramName == null) { // positional parameter
                        appendParam(q, ignoreCase, ++queryInfo.paramCount);
                        if (count != 1)
                            queryInfo.paramAddedCount++;
                    } else { // named parameter
                        q.append(ignoreCase ? "LOWER(:" : ":");
                        q.append(paramName).append('_').append(count);
                        if (ignoreCase)
                            q.append(')');
                    }
                    break;
                case NULL:
                case EMPTY:
                    q.append(Condition.NULL.operator);
                    break;
                case NOT_NULL:
                case NOT_EMPTY:
                    q.append(Condition.NOT_NULL.operator);
                    break;
                default:
                    throw new MappingException("Repository keyword " + condition.name() +
                                               " cannot be used when the Id of the entity is an IdClass."); // TODO NLS
            }
        }

        q.append(')');
    }

    /**
     * Generates a query to select the COUNT of all entities matching the
     * supplied WHERE condition(s), or all entities if no WHERE conditions.
     * Populates the jpqlCount of the query information with the result.
     *
     * @param queryInfo query information.
     * @param where     the WHERE clause
     */
    private void generateCount(QueryInfo queryInfo, String where) {
        String o = queryInfo.entityVar;
        StringBuilder q = new StringBuilder(21 + 2 * o.length() + queryInfo.entityInfo.name.length() + (where == null ? 0 : where.length())) //
                        .append("SELECT COUNT(").append(o).append(") FROM ") //
                        .append(queryInfo.entityInfo.name).append(' ').append(o);

        if (where != null)
            q.append(where);

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
     * @param q         query up to the WHERE clause, if present
     * @param fwd       ORDER BY clause in forward page direction. Null if forward page direction is not needed.
     * @param prev      ORDER BY clause in previous page direction. Null if previous page direction is not needed.
     */
    private void generateKeysetQueries(QueryInfo queryInfo, StringBuilder q, StringBuilder fwd, StringBuilder prev) {
        int numKeys = queryInfo.sorts.size();
        String paramPrefix = queryInfo.paramNames == null ? "?" : ":keyset";
        StringBuilder a = fwd == null ? null : new StringBuilder(200).append(queryInfo.hasWhere ? " AND (" : " WHERE (");
        StringBuilder b = prev == null ? null : new StringBuilder(200).append(queryInfo.hasWhere ? " AND (" : " WHERE (");
        String o = queryInfo.entityVar;
        for (int i = 0; i < numKeys; i++) {
            if (a != null)
                a.append(i == 0 ? "(" : " OR (");
            if (b != null)
                b.append(i == 0 ? "(" : " OR (");
            for (int k = 0; k <= i; k++) {
                Sort keyInfo = queryInfo.sorts.get(k);
                String name = keyInfo.property();
                boolean asc = keyInfo.isAscending();
                boolean lower = keyInfo.ignoreCase();
                if (a != null)
                    if (lower) {
                        a.append(k == 0 ? "LOWER(" : " AND LOWER(").append(o).append('.').append(name).append(')');
                        a.append(k < i ? '=' : (asc ? '>' : '<'));
                        a.append("LOWER(").append(paramPrefix).append(queryInfo.paramCount + 1 + k).append(')');
                    } else {
                        a.append(k == 0 ? "" : " AND ").append(o).append('.').append(name);
                        a.append(k < i ? '=' : (asc ? '>' : '<'));
                        a.append(paramPrefix).append(queryInfo.paramCount + 1 + k);
                    }
                if (b != null)
                    if (lower) {
                        b.append(k == 0 ? "LOWER(" : " AND LOWER(").append(o).append('.').append(name).append(')');
                        b.append(k < i ? '=' : (asc ? '<' : '>'));
                        b.append("LOWER(").append(paramPrefix).append(queryInfo.paramCount + 1 + k).append(')');
                    } else {
                        b.append(k == 0 ? "" : " AND ").append(o).append('.').append(name);
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
            queryInfo.jpqlAfterKeyset = new StringBuilder(q).append(a).append(')').append(fwd).toString();
        if (b != null)
            queryInfo.jpqlBeforeKeyset = new StringBuilder(q).append(b).append(')').append(prev).toString();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "forward & previous keyset queries", queryInfo.jpqlAfterKeyset, queryInfo.jpqlBeforeKeyset);
    }

    private StringBuilder generateMethodNameQuery(QueryInfo queryInfo, boolean countPages) {
        EntityInfo entityInfo = queryInfo.entityInfo;
        String o = queryInfo.entityVar;
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
            q = generateSelectClause(queryInfo);
            if (orderBy > c || orderBy == -1 && methodName.length() > c) {
                int where = q.length();
                generateWhereClause(queryInfo, methodName, c, orderBy > 0 ? orderBy : methodName.length(), q);
                if (countPages)
                    generateCount(queryInfo, q.substring(where));
            }
            if (orderBy >= c)
                parseOrderBy(queryInfo, orderBy, q);
            queryInfo.type = QueryInfo.Type.SELECT;
        } else if (methodName.startsWith("delete")) {
            int by = methodName.indexOf("By", 6);
            int c = by < 0 ? 6 : by + 2;
            if (by > 6) {
                if ("deleteAllById".equals(methodName) && Iterable.class.isAssignableFrom(queryInfo.method.getParameterTypes()[0]))
                    if (entityInfo.idClass == null)
                        methodName = "deleteAllByIdIn"; // CrudRepository.deleteAllById(Iterable)
                    else
                        throw new MappingException("The deleteAllById operation cannot be used on entities with composite IDs."); // TODO NLS
            } else if (methodName.length() == 6) {
                Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
                if (paramTypes.length == 1 && (Object.class.equals(paramTypes[0]) || entityInfo.type.equals(paramTypes[0]))) {
                    methodName = "deleteById"; // CrudRepository.delete(entity)
                    queryInfo.paramsNeedConversionToId = true;
                    c = 8;
                }
            } else if (methodName.length() == 9 && methodName.endsWith("All")) {
                Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
                if (paramTypes.length == 1 && Iterable.class.isAssignableFrom(paramTypes[0]))
                    if (entityInfo.idClass == null) {
                        methodName = "deleteByIdIn"; // CrudRepository.deleteAll(Iterable)
                        queryInfo.paramsNeedConversionToId = true;
                        c = 8;
                    } else {
                        throw new MappingException("The deleteAll operation cannot be used on entities with composite IDs."); // TODO NLS
                    }
            }
            q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
            if (methodName.length() > c)
                generateWhereClause(queryInfo, methodName, c, methodName.length(), q);
            queryInfo.type = QueryInfo.Type.DELETE;
        } else if (methodName.startsWith("update")) {
            int by = methodName.indexOf("By", 6);
            int c = by < 0 ? 6 : by + 2;
            q = generateUpdateClause(queryInfo, methodName, c);
            queryInfo.type = QueryInfo.Type.UPDATE;
        } else if (methodName.startsWith("count")) {
            int by = methodName.indexOf("By", 5);
            int c = by < 0 ? 5 : by + 2;
            q = new StringBuilder(150).append("SELECT COUNT(").append(o).append(") FROM ").append(entityInfo.name).append(' ').append(o);
            if (methodName.length() > c)
                generateWhereClause(queryInfo, methodName, c, methodName.length(), q);
            queryInfo.type = QueryInfo.Type.COUNT;
        } else if (methodName.startsWith("exists")) {
            int by = methodName.indexOf("By", 6);
            int c = by < 0 ? 6 : by + 2;
            String attrName = entityInfo.getAttributeName(entityInfo.idClass == null ? "id" : entityInfo.idClassAttributeAccessors.firstKey());
            q = new StringBuilder(200).append("SELECT ").append(o).append('.').append(attrName) //
                            .append(" FROM ").append(entityInfo.name).append(' ').append(o);
            if (methodName.length() > c)
                generateWhereClause(queryInfo, methodName, c, methodName.length(), q);
            queryInfo.type = QueryInfo.Type.EXISTS;
        }

        return q;
    }

    /**
     * Generates the JPQL ORDER BY clause. This method is common between the OrderBy annotation and keyword.
     */
    private void generateOrderBy(QueryInfo queryInfo, StringBuilder q) {
        boolean needsKeysetQueries = KeysetAwarePage.class.equals(queryInfo.method.getReturnType())
                                     || KeysetAwareSlice.class.equals(queryInfo.method.getReturnType())
                                     || Iterator.class.equals(queryInfo.method.getReturnType())
                                     || KeysetAwarePage.class.equals(queryInfo.returnTypeParam)
                                     || KeysetAwareSlice.class.equals(queryInfo.returnTypeParam)
                                     || Iterator.class.equals(queryInfo.returnTypeParam);

        StringBuilder fwd = needsKeysetQueries ? new StringBuilder(100) : q; // forward page order
        StringBuilder prev = needsKeysetQueries ? new StringBuilder(100) : null; // previous page order

        boolean first = true;
        for (Sort sort : queryInfo.sorts) {
            fwd.append(first ? " ORDER BY " : ", ");
            appendSort(fwd, queryInfo.entityVar, sort, true);

            if (needsKeysetQueries) {
                prev.append(first ? " ORDER BY " : ", ");
                appendSort(prev, queryInfo.entityVar, sort, false);
            }
            first = false;
        }

        if (needsKeysetQueries) {
            generateKeysetQueries(queryInfo, q, fwd, prev);
            q.append(fwd);
        }
    }

    private StringBuilder generateSelectClause(QueryInfo queryInfo) {
        StringBuilder q = new StringBuilder(200);
        String o = queryInfo.entityVar;

        Select select = queryInfo.method.getAnnotation(Select.class);
        String[] cols = select == null ? null : select.value();
        boolean distinct = select != null && select.distinct();
        String function = select == null ? null : toFunctionName(select.function());

        Class<?> type = queryInfo.returnArrayType != null ? queryInfo.returnArrayType //
                        : queryInfo.returnTypeParam != null ? queryInfo.returnTypeParam //
                                        : queryInfo.method.getReturnType();

        q.append("SELECT ");

        if (type.isAssignableFrom(queryInfo.entityInfo.type)
            || type.isInterface()
            || type.isPrimitive()
            || type.getName().startsWith("java")
            || queryInfo.entityInfo.inheritance && queryInfo.entityInfo.type.isAssignableFrom(type)) {
            if (cols == null || cols.length == 0) {
                q.append(distinct ? "DISTINCT " : "").append(o);
            } else {
                for (int i = 0; i < cols.length; i++) {
                    generateSelectExpression(q, i == 0, function, distinct, o, cols[i]);
                }
            }
        } else {
            // It would be preferable if the spec included the Select annotation to explicitly identify parameters, but if that doesn't happen
            // TODO we could compare attribute types with known constructor to improve on guessing a correct order of parameters
            q.append("NEW ").append(type.getName()).append('(');
            List<String> embAttrNames;
            boolean first = true;
            if (cols != null && cols.length > 0)
                for (int i = 0; i < cols.length; i++) {
                    String name = queryInfo.entityInfo.getAttributeName(cols[i]);
                    generateSelectExpression(q, i == 0, function, distinct, o, name == null ? cols[i] : name);
                }
            else if (type.equals(queryInfo.entityInfo.idClass))
                for (String idClassAttributeName : queryInfo.entityInfo.idClassAttributeAccessors.keySet()) {
                    String name = queryInfo.entityInfo.getAttributeName(idClassAttributeName);
                    generateSelectExpression(q, first, function, distinct, o, name);
                    first = false;
                }
            else if ((embAttrNames = queryInfo.entityInfo.embeddableAttributeNames.get(type)) != null)
                for (String name : embAttrNames) {
                    generateSelectExpression(q, first, function, distinct, o, name);
                    first = false;
                }
            else
                for (String name : queryInfo.entityInfo.attributeTypes.keySet()) {
                    generateSelectExpression(q, first, function, distinct, o, name);
                    first = false;
                }
            q.append(')');
        }
        q.append(" FROM ").append(queryInfo.entityInfo.name).append(' ').append(o);
        return q;
    }

    private void generateSelectExpression(StringBuilder q, boolean isFirst, String function, boolean distinct, String o, String attributeName) {
        if (!isFirst)
            q.append(", ");
        if (function != null)
            q.append(function).append('(');
        q.append(distinct ? "DISTINCT " : "");
        q.append(o).append('.').append(attributeName);
        if (function != null)
            q.append(')');
    }

    /**
     * Generates the JPQL UPDATE clause for a repository updateBy method such as updateByProductIdSetProductNameMultiplyPrice
     */
    private StringBuilder generateUpdateClause(QueryInfo queryInfo, String methodName, int c) {
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
        generateWhereClause(queryInfo, methodName, c, uFirst, where);

        String o = queryInfo.entityVar;
        StringBuilder q = new StringBuilder(250);
        q.append("UPDATE ").append(queryInfo.entityInfo.name).append(' ').append(o).append(" SET");

        for (int u = uFirst; u > 0;) {
            boolean first = u == uFirst;
            char op;
            if (u == set) {
                op = '=';
                set = methodName.indexOf("Set", u += 3);
            } else if (u == add) {
                op = '+';
                add = methodName.indexOf("Add", u += 3);
            } else if (u == div) {
                op = '/';
                div = methodName.indexOf("Divide", u += 6);
            } else if (u == mul) {
                op = '*';
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

            if (name == null) {
                if (op == '=') {
                    generateUpdatesForIdClass(queryInfo, null, first, q);
                } else {
                    String opName = op == '+' ? Operation.Add.name() : op == '*' ? Operation.Multiply.name() : Operation.Divide.name();
                    throw new MappingException("The " + opName +
                                               " repository update operation cannot be used on the Id of the entity when the Id is an IdClass."); // TODO NLS
                }
            } else {
                q.append(first ? " " : ", ").append(o).append('.').append(name).append("=");

                switch (op) {
                    case '+':
                        if (CharSequence.class.isAssignableFrom(queryInfo.entityInfo.attributeTypes.get(name))) {
                            q.append("CONCAT(").append(o).append('.').append(name).append(',') //
                                            .append('?').append(++queryInfo.paramCount).append(')');
                            break;
                        }
                        // else fall through
                    case '*':
                    case '/':
                        q.append(o).append('.').append(name).append(op);
                        // fall through
                    case '=':
                        q.append('?').append(++queryInfo.paramCount);
                }
            }

            u = next == Integer.MAX_VALUE ? -1 : next;
        }

        return q.append(where);
    }

    /**
     * Generates the JPQL UPDATE clause from @Update annotations.
     *
     * @param queryInfo query information
     * @param updates   Update annotations
     * @return the JPQL UPDATE clause
     */
    private StringBuilder generateUpdateClause(QueryInfo queryInfo, Update[] updates) {
        String o = queryInfo.entityVar;
        StringBuilder q = new StringBuilder(400).append("UPDATE ").append(queryInfo.entityInfo.name).append(' ').append(o).append(" SET");

        boolean first = true;
        for (Update update : updates) {
            String attribute = update.attr();
            Operation op = update.op();
            String name = queryInfo.entityInfo.getAttributeName(attribute);

            if (name == null) {
                if (op == Operation.Assign)
                    generateUpdatesForIdClass(queryInfo, update, first, q);
                else
                    throw new MappingException("The " + op.name() +
                                               " repository update operation cannot be used on the Id of the entity when the Id is an IdClass."); // TODO NLS
            } else {
                q.append(first ? " " : ", ").append(o).append('.').append(name).append("=");

                boolean withFunction = false;
                switch (op) {
                    case Assign:
                        break;
                    case Add:
                        if (withFunction = CharSequence.class.isAssignableFrom(queryInfo.entityInfo.attributeTypes.get(name)))
                            q.append("CONCAT(").append(o).append('.').append(name).append(',');
                        else
                            q.append(o).append('.').append(name).append('+');
                        break;
                    case Multiply:
                        q.append(o).append('.').append(name).append('*');
                        break;
                    case Subtract:
                        q.append(o).append('.').append(name).append('-');
                        break;
                    case Divide:
                        q.append(o).append('.').append(name).append('/');
                        break;
                    default:
                        throw new UnsupportedOperationException(op.name());
                }

                String param = update.param();
                String[] values = update.value();
                if (param.length() > 0) { // named parameter
                    q.append(':').append(param);
                    if (queryInfo.paramNames == null)
                        queryInfo.paramNames = new ArrayList<>(); // content is computed later from method signature
                } else if (values.length == 1) { // single value
                    char c = values[0].length() == 0 ? ' ' : values[0].charAt(0);
                    boolean enquote = (c < '0' || c > '9') && c != '\'';
                    if (enquote)
                        q.append("'");
                    q.append(values[0]);
                    if (enquote)
                        q.append("'");
                } else if (values.length > 1) { // multiple value list // TODO should we even allow this if there is no way to supply a single value list?
                    for (int v = 0; v < values.length; v++) {
                        q.append(v == 0 ? "(" : ", ");
                        char c = values[v].length() == 0 ? ' ' : values[v].charAt(0);
                        boolean enquote = (c < '0' || c > '9') && c != '\'';
                        if (enquote)
                            q.append("'");
                        q.append(values[v]);
                        if (enquote)
                            q.append("'");
                    }
                    q.append(')');
                } else { // positional parameter
                    q.append('?').append(++queryInfo.paramCount);
                }

                if (withFunction)
                    q.append(')');
            }
            first = false;
        }

        return q;
    }

    /**
     * Generates JPQL to assign the entity properties of which the IdClass consists.
     */
    private void generateUpdatesForIdClass(QueryInfo queryInfo, Update update, boolean firstOperation, StringBuilder q) {
        if (update != null && update.value().length != 0)
            throw new MappingException("IdClass parameter cannot be represented as a hard-coded value of the @Update annotation."); // TODO NLS

        String paramName = update == null || update.param().length() == 0 ? null : update.param();

        int count = 0;
        for (String idClassAttr : queryInfo.entityInfo.idClassAttributeAccessors.keySet()) {
            count++;
            String name = queryInfo.entityInfo.getAttributeName(idClassAttr);

            q.append(firstOperation ? " " : ", ").append(queryInfo.entityVar).append('.').append(name);
            if (paramName == null) { // positional parameter
                q.append("=?").append(++queryInfo.paramCount);
                if (count != 1)
                    queryInfo.paramAddedCount++;
            } else { // named parameter
                q.append("=:").append(paramName).append('_').append(count);
            }

            firstOperation = false;
        }
    }

    /**
     * Generates the JPQL WHERE clause for all findBy, deleteBy, or updateBy conditions such as MyColumn[IgnoreCase][Not]Like
     */
    private void generateWhereClause(QueryInfo queryInfo, String methodName, int start, int endBefore, StringBuilder q) {
        queryInfo.hasWhere = true;
        q.append(" WHERE (");
        for (int and = start, or = start, iNext = start, i = start; queryInfo.hasWhere && i >= start && iNext < endBefore; i = iNext) {
            // The extra character (+1) below allows for entity property names that begin with Or or And.
            // For example, findByOrg and findByPriceBetweenAndOrderNumber
            and = and == -1 || and > i + 1 ? and : methodName.indexOf("And", i + 1);
            or = or == -1 || or > i + 1 ? or : methodName.indexOf("Or", i + 1);
            iNext = Math.min(and, or);
            if (iNext < 0)
                iNext = Math.max(and, or);
            generateCondition(queryInfo, methodName, i, iNext < 0 || iNext >= endBefore ? endBefore : iNext, q);
            if (iNext > 0 && iNext < endBefore) {
                q.append(iNext == and ? " AND " : " OR ");
                iNext += (iNext == and ? 3 : 2);
            }
        }
        if (queryInfo.hasWhere)
            q.append(')');
    }

    /**
     * Generates the JPQL WHERE clause based on conditions in the Filter annotations
     *
     * @param queryInfo query information
     * @param filters   Filter annotations
     * @return the JPQL WHERE clause
     */
    private StringBuilder generateWhereClause(QueryInfo queryInfo, Filter[] filters) {
        queryInfo.hasWhere = true;
        StringBuilder q = new StringBuilder(250).append(" WHERE (");

        boolean first = true;
        for (Filter filter : filters) {
            if (first)
                first = false;
            else
                q.append(' ').append(filter.as().name()).append(' '); // AND / OR between conditions

            String attribute = filter.by();
            boolean ignoreCase = filter.ignoreCase();
            Compare comparison = filter.op();
            Compare negatedFrom = comparison.negatedFrom();
            boolean negated = negatedFrom != null;
            if (negated)
                comparison = negatedFrom;

            if (attribute.length() == 0)
                throw new MappingException("Entity property name is missing."); // TODO possibly combine with unknown entity property name

            String name = queryInfo.entityInfo.getAttributeName(attribute);
            if (name == null) {
                generateConditionsForIdClass(queryInfo, filter, Condition.forIdClass(comparison), ignoreCase, negated, q);
                continue;
            }

            String o = queryInfo.entityVar;
            StringBuilder attributeExpr = new StringBuilder();
            if (ignoreCase)
                attributeExpr.append("LOWER(").append(o).append('.').append(name).append(')');
            else
                attributeExpr.append(o).append('.').append(name);

            boolean isCollection = Collection.class.equals(queryInfo.entityInfo.attributeTypes.get(name));
            if (isCollection)
                verifyCollectionsSupported(name, ignoreCase, comparison);

            switch (comparison) {
                case Equal:
                    q.append(attributeExpr).append(negated ? Condition.NOT_EQUALS.operator : Condition.EQUALS.operator);
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case GreaterThan:
                    q.append(attributeExpr).append(Condition.GREATER_THAN.operator);
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case GreaterThanEqual:
                    q.append(attributeExpr).append(Condition.GREATER_THAN_EQUAL.operator);
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case LessThan:
                    q.append(attributeExpr).append(Condition.LESS_THAN.operator);
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case LessThanEqual:
                    q.append(attributeExpr).append(Condition.LESS_THAN_EQUAL.operator);
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case StartsWith:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT(");
                    appendParamOrValue(q, queryInfo, filter).append(", '%')");
                    break;
                case EndsWith:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                    appendParamOrValue(q, queryInfo, filter).append(")");
                    break;
                case Like:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE ");
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case Between:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ");
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case Contains:
                    if (isCollection) {
                        q.append(' ');
                        appendParamOrValue(q, queryInfo, filter).append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
                    } else {
                        q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                        appendParamOrValue(q, queryInfo, filter).append(", '%')");
                    }
                    break;
                case In:
                    if (ignoreCase)
                        throw new MappingException(new UnsupportedOperationException("Repository keyword IgnoreCase cannot be combined with the In keyword.")); // TODO
                    q.append(attributeExpr).append(negated ? " NOT " : "").append(Condition.IN.operator);
                    appendParamOrValue(q, queryInfo, filter);
                    break;
                case Null:
                    q.append(attributeExpr).append(negated ? Condition.NOT_NULL.operator : Condition.NULL.operator);
                    break;
                case True:
                    q.append(attributeExpr).append(Condition.TRUE.operator);
                    break;
                case False:
                    q.append(attributeExpr).append(Condition.FALSE.operator);
                    break;
                case Empty:
                    if (isCollection)
                        q.append(attributeExpr).append(negated ? Condition.NOT_EMPTY.operator : Condition.EMPTY.operator);
                    else
                        q.append(attributeExpr).append(negated ? Condition.NOT_NULL.operator : Condition.NULL.operator);
                    break;
                default:
                    throw new MappingException(new UnsupportedOperationException(comparison.name())); // should be unreachable
            }
        }

        return q.append(')');
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

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, queryInfo.toString());

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
                        Limit limit = null;
                        Pageable pagination = null;
                        List<Sort> sortList = null;

                        // Jakarta Data allows the method parameter positions after those used as query parameters
                        // to be used for purposes such as pagination and sorting.
                        for (int i = queryInfo.paramCount - queryInfo.paramAddedCount; i < (args == null ? 0 : args.length); i++) {
                            Object param = args[i];
                            if (param instanceof Limit)
                                if (limit == null)
                                    limit = (Limit) param;
                                else
                                    throw new DataException("Repository method " + method + " cannot have multiple Limit parameters."); // TODO NLS
                            else if (param instanceof Pageable)
                                if (pagination == null)
                                    pagination = (Pageable) param;
                                else
                                    throw new DataException("Repository method " + method + " cannot have multiple Pageable parameters."); // TODO NLS
                            else if (param instanceof Sort)
                                sortList = queryInfo.combineSorts(sortList, (Sort) param);
                            else if (param instanceof Sort[])
                                sortList = queryInfo.combineSorts(sortList, (Sort[]) param);
                        }

                        if (pagination != null) {
                            if (limit != null)
                                throw new DataException("Repository method " + method + " cannot have both Limit and Pageable as parameters."); // TODO NLS
                            if (sortList == null)
                                sortList = queryInfo.combineSorts(sortList, pagination.sorts());
                            else if (sortList != null && !pagination.sorts().isEmpty())
                                throw new DataException("Repository method " + method + " cannot specify Sort parameters if Pageable also has Sort parameters."); // TODO NLS
                        }

                        if (sortList == null && queryInfo.hasDynamicSortCriteria())
                            sortList = queryInfo.sorts;

                        if (sortList != null && !sortList.isEmpty()) {
                            boolean forward = pagination == null || pagination.mode() != Pageable.Mode.CURSOR_PREVIOUS;
                            StringBuilder q = new StringBuilder(queryInfo.jpql);
                            StringBuilder order = null; // ORDER BY clause based on Sorts
                            for (Sort sort : sortList) {
                                order = order == null ? new StringBuilder(100).append(" ORDER BY ") : order.append(", ");
                                appendSort(order, queryInfo.entityVar, sort, forward);
                            }

                            if (pagination == null || pagination.mode() == Pageable.Mode.OFFSET)
                                queryInfo = queryInfo.withJPQL(q.append(order).toString(), sortList); // offset pagination can be a starting point for keyset pagination
                            else // CURSOR_NEXT or CURSOR_PREVIOUS
                                generateKeysetQueries(queryInfo = queryInfo.withJPQL(null, sortList), q, forward ? order : null, forward ? null : order);
                        }

                        boolean asyncCompatibleResultForPagination = pagination != null &&
                                                                     (void.class.equals(returnType) || CompletableFuture.class.equals(returnType)
                                                                      || CompletionStage.class.equals(returnType));

                        Class<?> type = queryInfo.returnTypeParam != null && (Optional.class.equals(returnType)
                                                                              || CompletableFuture.class.equals(returnType)
                                                                              || CompletionStage.class.equals(returnType)) //
                                                                                              ? queryInfo.returnTypeParam //
                                                                                              : returnType;

                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "results to be returned as " + type.getName());

                        if (pagination != null && Iterator.class.equals(type))
                            returnValue = new PaginatedIterator<E>(queryInfo, pagination, args);
                        else if (KeysetAwareSlice.class.equals(type) || KeysetAwarePage.class.equals(type))
                            returnValue = new KeysetAwarePageImpl<E>(queryInfo, limit == null ? pagination : toPageable(limit), args);
                        else if (Slice.class.equals(type) || Page.class.equals(type) || pagination != null && Streamable.class.equals(type))
                            returnValue = new PageImpl<E>(queryInfo, limit == null ? pagination : toPageable(limit), args);
                        else {
                            em = queryInfo.entityInfo.persister.createEntityManager();

                            TypedQuery<?> query = em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
                            queryInfo.setParameters(query, args);

                            int maxResults = limit != null ? limit.maxResults() //
                                            : pagination != null ? pagination.size() //
                                                            : queryInfo.maxResults;

                            int startAt = limit != null ? computeOffset(limit) //
                                            : pagination != null ? computeOffset(pagination) //
                                                            : 0;

                            if (maxResults > 0)
                                query.setMaxResults(maxResults);
                            if (startAt > 0)
                                query.setFirstResult(startAt);

                            if (BaseStream.class.isAssignableFrom(type)) {
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
                                } else if (Iterator.class.equals(type)) {
                                    returnValue = results.iterator();
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

                        jakarta.persistence.Query query = em.createQuery(queryInfo.jpql);
                        query.setMaxResults(1);
                        queryInfo.setParameters(query, args);

                        returnValue = !query.getResultList().isEmpty();

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
     * Identifies the statically specified sort criteria for a repository findBy method such as
     * findByLastNameLikeOrderByLastNameAscFirstNameDesc
     */
    private void parseOrderBy(QueryInfo queryInfo, int orderBy, StringBuilder q) {
        String methodName = queryInfo.method.getName();

        queryInfo.sorts = queryInfo.sorts == null ? new ArrayList<>() : queryInfo.sorts;

        for (int length = methodName.length(), asc = 0, desc = 0, iNext, i = orderBy + 7; i >= 0 && i < length; i = iNext) {
            asc = asc == -1 || asc > i ? asc : methodName.indexOf("Asc", i);
            desc = desc == -1 || desc > i ? desc : methodName.indexOf("Desc", i);
            iNext = Math.min(asc, desc);
            if (iNext < 0)
                iNext = Math.max(asc, desc);

            boolean ignoreCase;
            boolean descending = iNext > 0 && iNext == desc;
            int endBefore = iNext < 0 ? methodName.length() : iNext;
            if (ignoreCase = endsWith("IgnoreCase", methodName, i, endBefore))
                endBefore -= 10;

            String attribute = methodName.substring(i, endBefore);

            queryInfo.addSort(ignoreCase, attribute, descending);

            if (iNext > 0)
                iNext += (iNext == desc ? 4 : 3);
        }

        if (!queryInfo.hasDynamicSortCriteria())
            generateOrderBy(queryInfo, q);
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

    /**
     * Converts a Limit to a Pageable if possible.
     *
     * @param limit Limit.
     * @return Pageable.
     * @throws DataException with chained IllegalArgumentException if the Limit is a range with a starting point above 1.
     */
    private static final Pageable toPageable(Limit limit) {
        if (limit.startAt() != 1L)
            throw new DataException(new IllegalArgumentException("Limit with starting point " + limit.startAt() +
                                                                 ", which is greater than 1, cannot be used to request pages or slices."));
        return Pageable.ofSize(limit.maxResults());
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

    /**
     * Confirm that collections are supported for this condition,
     * based on whether case insensitive comparison is requested.
     *
     * @param attributeName entity attribute to which the condition is to be applied.
     * @param ignoreCase    indicates if the condition is to be performed ignoring case.
     * @param condition     the type of condition.
     * @throws MappingException with chained UnsupportedOperationException if not supported.
     */
    @Trivial
    private static void verifyCollectionsSupported(String attributeName, boolean ignoreCase, Compare condition) {
        if (!SUPPORTS_COLLECTIONS.contains(condition) || ignoreCase)
            throw new MappingException(new UnsupportedOperationException("Repository keyword " +
                                                                         (ignoreCase ? "IgnoreCase" : condition.name()) +
                                                                         " which is applied to entity property " + attributeName +
                                                                         " is not supported for collection properties.")); // TODO
    }
}
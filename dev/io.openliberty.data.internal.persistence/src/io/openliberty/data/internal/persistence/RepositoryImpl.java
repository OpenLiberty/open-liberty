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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.cdi.DataExtension;
import io.openliberty.data.internal.persistence.cdi.DataExtensionProvider;
import io.openliberty.data.repository.Compare;
import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Filter;
import io.openliberty.data.repository.Function;
import io.openliberty.data.repository.Select;
import io.openliberty.data.repository.Select.Aggregate;
import io.openliberty.data.repository.update.Add;
import io.openliberty.data.repository.update.Assign;
import io.openliberty.data.repository.update.Divide;
import io.openliberty.data.repository.update.Multiply;
import io.openliberty.data.repository.update.SubtractFrom;
import jakarta.data.Limit;
import jakarta.data.Sort;
import jakarta.data.Streamable;
import jakarta.data.exceptions.DataConnectionException;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.KeysetAwarePage;
import jakarta.data.page.KeysetAwareSlice;
import jakarta.data.page.Page;
import jakarta.data.page.Pageable;
import jakarta.data.page.Slice;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Inheritance;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private static final Map<Function, String> FUNCTION_CALLS = new HashMap<>();
    static {
        FUNCTION_CALLS.put(Function.AbsoluteValue, "ABS(");
        FUNCTION_CALLS.put(Function.CharCount, "LENGTH(");
        FUNCTION_CALLS.put(Function.ElementCount, "SIZE(");
        FUNCTION_CALLS.put(Function.IgnoreCase, "LOWER(");
        FUNCTION_CALLS.put(Function.Rounded, "ROUND(");
        FUNCTION_CALLS.put(Function.RoundedDown, "FLOOR(");
        FUNCTION_CALLS.put(Function.RoundedUp, "CEILING(");
        FUNCTION_CALLS.put(Function.Trimmed, "TRIM(");
        FUNCTION_CALLS.put(Function.WithDay, "EXTRACT (DAY FROM ");
        FUNCTION_CALLS.put(Function.WithHour, "EXTRACT (HOUR FROM ");
        FUNCTION_CALLS.put(Function.WithMinute, "EXTRACT (MINUTE FROM ");
        FUNCTION_CALLS.put(Function.WithMonth, "EXTRACT (MONTH FROM ");
        FUNCTION_CALLS.put(Function.WithQuarter, "EXTRACT (QUARTER FROM ");
        FUNCTION_CALLS.put(Function.WithSecond, "EXTRACT (SECOND FROM ");
        FUNCTION_CALLS.put(Function.WithWeek, "EXTRACT (WEEK FROM ");
        FUNCTION_CALLS.put(Function.WithYear, "EXTRACT (YEAR FROM ");
    }

    private static final Set<Class<?>> SPECIAL_PARAM_TYPES = new HashSet<>(Arrays.asList //
    (Limit.class, Pageable.class, Sort.class, Sort[].class));

    private static final Set<Compare> SUPPORTS_COLLECTIONS = Set.of //
    (Compare.Equal, Compare.Contains, Compare.Empty, Compare.Not, Compare.NotContains, Compare.NotEmpty);

    // Valid types for when a repository method computes an update count
    private static final Set<Class<?>> UPDATE_COUNT_TYPES = new HashSet<>(Arrays.asList //
    (boolean.class, Boolean.class, int.class, Integer.class, long.class, Long.class, void.class, Void.class, Number.class));

    private static final ThreadLocal<Deque<EntityManager>> defaultMethodResources = new ThreadLocal<>();

    private final AtomicBoolean isDisposed = new AtomicBoolean();
    private final CompletableFuture<EntityInfo> primaryEntityInfoFuture;
    private final DataExtensionProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    private final Class<R> repositoryInterface;
    private final EntityValidator validator;

    public RepositoryImpl(DataExtensionProvider provider, DataExtension extension, EntityDefiner definer,
                          Class<R> repositoryInterface, Class<?> primaryEntityClass,
                          Map<Class<?>, List<QueryInfo>> queriesPerEntityClass) {
        this.primaryEntityInfoFuture = primaryEntityClass == null ? null : definer.entityInfoMap.computeIfAbsent(primaryEntityClass, EntityInfo::newFuture);
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        Object validation = provider.validationService();
        this.validator = validation == null ? null : EntityValidator.newInstance(validation, repositoryInterface);

        for (Entry<Class<?>, List<QueryInfo>> entry : queriesPerEntityClass.entrySet()) {
            Class<?> entityClass = entry.getKey();
            for (QueryInfo queryInfo : entry.getValue()) {
                if (queryInfo.type == QueryInfo.Type.RESOURCE_ACCESS) {
                    queryInfo.validateParams = validator != null && validator.isValidatable(queryInfo.method)[1];
                    queries.put(queryInfo.method, CompletableFuture.completedFuture(queryInfo));
                } else {
                    boolean inheritance = entityClass.getAnnotation(Inheritance.class) != null; // TODO what do we need to do this with?

                    Class<?> jpaEntityClass;
                    Class<?> recordClass = null;
                    if (entityClass.isRecord())
                        try {
                            recordClass = entityClass;
                            jpaEntityClass = recordClass.getClassLoader().loadClass(recordClass.getName() + "Entity");
                        } catch (ClassNotFoundException x) {
                            // TODO figure out how to best report this error to the user
                            throw new MappingException("Unable to load generated entity class for record " + recordClass, x); // TODO NLS
                        }
                    else
                        jpaEntityClass = entityClass;

                    CompletableFuture<EntityInfo> entityInfoFuture = definer.entityInfoMap.computeIfAbsent(jpaEntityClass, EntityInfo::newFuture);

                    queries.put(queryInfo.method, entityInfoFuture.thenCombine(CompletableFuture.completedFuture(queryInfo),
                                                                               this::completeQueryInfo));
                }
            }
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
    private static StringBuilder appendParamOrValue(StringBuilder q, QueryInfo queryInfo, Filter filter, boolean lower) {
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
     * Invoked when the bean for the repository is disposed.
     */
    public void beanDisposed() {
        // TODO re-enable when using a single bean for the repository rather than sharing the repository across multiple beans
        // isDisposed.set(true);
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

        if (validator != null) {
            boolean[] v = validator.isValidatable(queryInfo.method);
            queryInfo.validateParams = v[0];
            queryInfo.validateResult = v[1];
        }

        Method method = queryInfo.method;
        Class<?> multiType = queryInfo.getMultipleResultType();
        boolean countPages = Page.class.equals(multiType) || KeysetAwarePage.class.equals(multiType);
        StringBuilder q = null;

        // TODO would it be more efficient to invoke method.getAnnotations() once?

        // spec-defined annotation types
        Delete delete = method.getAnnotation(Delete.class);
        Insert insert = method.getAnnotation(Insert.class);
        Update update = method.getAnnotation(Update.class);
        Save save = method.getAnnotation(Save.class);
        Query query = method.getAnnotation(Query.class);
        OrderBy[] orderBy = method.getAnnotationsByType(OrderBy.class);

        // experimental annotation types
        Filter[] filters = method.getAnnotationsByType(Filter.class);
        Count count = method.getAnnotation(Count.class);
        Exists exists = method.getAnnotation(Exists.class);
        Select select = method.getAnnotation(Select.class);

        queryInfo.validateAnnotationCombinations(delete, insert, update, save, query, orderBy,
                                                 filters, count, exists, select);

        // Lifecycle annotations
        if (save != null) { // @Save annotation
            queryInfo.init(Save.class, QueryInfo.Type.SAVE);
        } else if (insert != null) { // @Insert annotation
            queryInfo.init(Insert.class, QueryInfo.Type.INSERT);
        } else if (update != null) { // @Update annotation
            q = generateUpdateEntity(queryInfo);
        } else if (delete != null && filters.length == 0) { // @Delete annotation with no @Filter annotations
            q = generateDeleteEntity(queryInfo);
        } else if (query != null) { // @Query annotation
            queryInfo.jpql = query.value();

            String upper = queryInfo.jpql.toUpperCase();
            String upperTrimmed = upper.stripLeading();
            if (upperTrimmed.startsWith("SELECT")) {
                int order = upper.lastIndexOf("ORDER BY");
                queryInfo.type = QueryInfo.Type.FIND;
                queryInfo.sorts = queryInfo.sorts == null ? new ArrayList<>() : queryInfo.sorts;
                queryInfo.jpqlCount = query.count().length() > 0 ? query.count() : null;

                int selectIndex = upper.length() - upperTrimmed.length();
                int from = find("FROM", upper, selectIndex + 9);
                if (from > 0) {
                    // TODO support for multiple entity types
                    int entityName = find(entityInfo.name.toUpperCase(), upper, from + 5);
                    if (entityName > 0) {
                        String entityVar = findEntityVariable(queryInfo.jpql, entityName + entityInfo.name.length() + 1);
                        if (entityVar != null)
                            queryInfo.entityVar = entityVar;
                    }

                    if (countPages && queryInfo.jpqlCount == null) {
                        // Attempt to infer from provided query
                        String s = queryInfo.jpql.substring(selectIndex + 6, from);
                        int comma = s.indexOf(',');
                        if (comma > 0)
                            s = s.substring(0, comma);
                        queryInfo.jpqlCount = new StringBuilder(queryInfo.jpql.length() + 7) //
                                        .append("SELECT COUNT(").append(s.trim()).append(") ") //
                                        .append(order > from ? queryInfo.jpql.substring(from, order) : queryInfo.jpql.substring(from)) //
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
        } else {
            // Query by annotations
            StringBuilder whereClause = filters.length > 0 ? generateWhereClause(queryInfo, filters) : null;
            String o = queryInfo.entityVar;

            if (delete != null) {
                if (queryInfo.isFindAndDelete()) {
                    queryInfo.type = QueryInfo.Type.FIND_AND_DELETE;
                    q = generateSelectClause(queryInfo, null); // TODO select annotation parameter would be limited by collision with update count/boolean
                    queryInfo.jpqlDelete = generateDeleteById(queryInfo);
                } else { // DELETE
                    queryInfo.type = QueryInfo.Type.DELETE;
                    q = new StringBuilder(13 + o.length() + entityInfo.name.length() + (whereClause == null ? 0 : whereClause.length())) //
                                    .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
                }
                if (whereClause != null)
                    q.append(whereClause);
            } else if (count != null) {
                queryInfo.type = QueryInfo.Type.COUNT;
                q = new StringBuilder(21 + 2 * o.length() + entityInfo.name.length() + (whereClause == null ? 0 : whereClause.length())) //
                                .append("SELECT COUNT(").append(o).append(") FROM ") //
                                .append(entityInfo.name).append(' ').append(o);
                if (whereClause != null)
                    q.append(whereClause);
            } else if (exists != null) {
                queryInfo.type = QueryInfo.Type.EXISTS;
                String name = entityInfo.idClassAttributeAccessors == null ? "id" : entityInfo.idClassAttributeAccessors.firstKey();
                String attrName = entityInfo.getAttributeName(name, true);
                q = new StringBuilder(15 + 2 * o.length() + attrName.length() + entityInfo.name.length() + (whereClause == null ? 0 : whereClause.length())) //
                                .append("SELECT ").append(o).append('.').append(attrName) //
                                .append(" FROM ").append(entityInfo.name).append(' ').append(o);
                if (whereClause != null)
                    q.append(whereClause);
            } else if (whereClause != null) {
                queryInfo.type = QueryInfo.Type.FIND;
                q = generateSelectClause(queryInfo, select).append(whereClause);
                if (countPages && queryInfo.type == QueryInfo.Type.FIND)
                    generateCount(queryInfo, whereClause.toString());
            } else {
                // Query by method name or Query by parameters
                q = generateQueryFromMethod(queryInfo, countPages);//keyset queries before orderby

                // @Select annotation only
                if (q == null && queryInfo.type == null) {
                    if (select != null) {
                        queryInfo.type = QueryInfo.Type.FIND;
                        q = generateSelectClause(queryInfo, select);
                        if (countPages)
                            generateCount(queryInfo, null);
                    }
                }
            }
        }

        // If we don't already know from generating the JPQL, find out how many
        // parameters the JPQL takes and which parameters are named parameters.
        if (query != null || queryInfo.paramNames != null) {
            int initialParamCount = queryInfo.paramCount;
            Parameter[] params = method.getParameters();
            List<Integer> paramPositions = null;
            Class<?> paramType;
            boolean hasParamAnnotation = false;
            for (int i = 0; i < params.length && !SPECIAL_PARAM_TYPES.contains(paramType = params[i].getType()); i++) {
                Param param = params[i].getAnnotation(Param.class);
                hasParamAnnotation |= param != null;
                String paramName = param == null ? null : param.value();
                if (param == null && queryInfo.jpql != null && params[i].isNamePresent()) {
                    String name = params[i].getName();
                    if (paramPositions == null)
                        paramPositions = getParameterPositions(queryInfo.jpql);
                    for (int p = 0; p < paramPositions.size() && paramName == null; p++) {
                        int pos = paramPositions.get(p); // position at which the named parameter name must appear
                        int next = pos + name.length(); // the next character must not be alphanumeric for the name to be a match
                        if (queryInfo.jpql.regionMatches(paramPositions.get(p), name, 0, name.length())
                            && (next >= queryInfo.jpql.length() || !Character.isLetterOrDigit(queryInfo.jpql.charAt(next)))) {
                            paramName = name;
                            paramPositions.remove(p);
                        }
                    }
                }
                if (paramName != null) {
                    if (queryInfo.paramNames == null)
                        queryInfo.paramNames = new ArrayList<>();
                    if (entityInfo.idClassAttributeAccessors != null && paramType.equals(entityInfo.idType))
                        // TODO is this correct to do when @Query has a named parameter with type of the IdClass?
                        // It seems like the JPQL would not be consistent.
                        for (int p = 1, numIdClassParams = entityInfo.idClassAttributeAccessors.size(); p <= numIdClassParams; p++) {
                            queryInfo.paramNames.add(new StringBuilder(paramName).append('_').append(p).toString());
                            if (p > 1) {
                                queryInfo.paramCount++;
                                queryInfo.paramAddedCount++;
                            }
                        }
                    else
                        queryInfo.paramNames.add(paramName);
                }
                queryInfo.paramCount++;

                if (initialParamCount != 0)
                    throw new MappingException("Cannot mix positional and named parameters on repository method " +
                                               method.getDeclaringClass().getName() + '.' + method.getName()); // TODO NLS

                int numParamNames = queryInfo.paramNames == null ? 0 : queryInfo.paramNames.size();
                if (numParamNames > 0 && numParamNames != queryInfo.paramCount)
                    if (hasParamAnnotation) {
                        throw new MappingException("Cannot mix positional and named parameters on repository method " +
                                                   method.getDeclaringClass().getName() + '.' + method.getName()); // TODO NLS
                    } else { // we might have mistaken a literal value for a named parameter
                        queryInfo.paramNames = null;
                        queryInfo.paramCount -= queryInfo.paramAddedCount;
                        queryInfo.paramAddedCount = 0;
                    }
            }
        }

        // The @OrderBy annotation from Jakarta Data provides sort criteria statically
        if (orderBy.length > 0) {
            queryInfo.type = queryInfo.type == null ? QueryInfo.Type.FIND : queryInfo.type;
            queryInfo.sorts = queryInfo.sorts == null ? new ArrayList<>(orderBy.length + 2) : queryInfo.sorts;
            if (q == null)
                if (queryInfo.jpql == null) {
                    q = generateSelectClause(queryInfo, select); // TODO can select ever be present here and not already handled by other code path?
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
            throw new MappingException("Repository method name " + method.getName() +
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
                else if (cause instanceof SQLIntegrityConstraintViolationException)
                    x = new EntityExistsException(original);
            }
            if (x == null) {
                if (original instanceof OptimisticLockException)
                    x = new OptimisticLockingFailureException(original);
                else if (original instanceof jakarta.persistence.EntityExistsException)
                    x = new EntityExistsException(original);
                else if (original instanceof NoResultException)
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
                x = new DataException(cause.getMessage(), original);
            else if (DataConnectionException.class.equals(cause.getClass()))
                x = new DataConnectionException(cause.getMessage(), original);
            else if (EmptyResultException.class.equals(cause.getClass()))
                x = new EmptyResultException(cause.getMessage(), original);
            else if (MappingException.class.equals(cause.getClass()))
                x = new MappingException(cause.getMessage(), original);
            else if (NonUniqueResultException.class.equals(cause.getClass()))
                x = new NonUniqueResultException(cause.getMessage(), original);
            else
                x = new MappingException(original);
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
     * Finds and updates entities (or records) in the database.
     * Entities that are not found are ignored.
     *
     * @param arg       the entity or record, or array or Iterable or Stream of entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the updated entities, using the return type that is required by the repository Update method signature.
     * @throws Exception if an error occurs.
     */
    private Object findAndUpdate(Object arg, QueryInfo queryInfo, EntityManager em) throws Exception {
        List<Object> results;

        if (queryInfo.entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object entity = findAndUpdateOne(Array.get(arg, i), queryInfo, em);
                if (entity != null) {
                    results.add(entity);
                }
            }
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            results = new ArrayList<>();
            if (arg instanceof Iterable) {
                for (Object e : ((Iterable<?>) arg)) {
                    Object entity = findAndUpdateOne(e, queryInfo, em);
                    if (entity != null) {
                        results.add(entity);
                    }
                }
            } else {
                results = new ArrayList<>(1);
                Object entity = findAndUpdateOne(arg, queryInfo, em);
                if (entity != null) {
                    results.add(entity);
                }
            }
        }
        em.flush();

        if (queryInfo.entityInfo.recordClass != null)
            for (int i = 0; i < results.size(); i++)
                results.set(i, queryInfo.entityInfo.toRecord(results.get(i)));

        Object returnValue;
        if (queryInfo.returnArrayType != null) {
            Object[] newArray = (Object[]) Array.newInstance(queryInfo.returnArrayType, results.size());
            returnValue = results.toArray(newArray);
        } else {
            Class<?> multiType = queryInfo.getMultipleResultType();
            if (multiType == null)
                returnValue = results.isEmpty() ? null : results.get(0); // TODO error if multiple results? Detect earlier?
            else if (multiType.isInstance(results))
                returnValue = results;
            else if (Stream.class.equals(multiType))
                returnValue = results.stream();
            else if (Iterable.class.isAssignableFrom(multiType))
                returnValue = toIterable(multiType, null, results);
            else if (Iterator.class.equals(multiType))
                returnValue = results.iterator();
            else
                throw new UnsupportedOperationException(multiType + " is an unsupported return type."); // TODO NLS
        }

        Class<?> returnType = queryInfo.method.getReturnType();
        if (Optional.class.equals(returnType)) {
            returnValue = returnValue == null ? Optional.empty() : Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (returnValue != null && !returnType.isInstance(returnValue)) {
            throw new MappingException("The " + returnType.getName() + " return type of the " +
                                       queryInfo.method.getName() + " method of the " +
                                       queryInfo.method.getDeclaringClass().getName() +
                                       " class is not a valid return type for a repository " +
                                       "@Update" + " method."); // TODO NLS
        }

        return returnValue;
    }

    /**
     * Finds an entity (or record) in the database, locks it for subsequent update,
     * and updates the entity found in the database to match the desired state of the supplied entity.
     *
     * @param e         the entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the entity that is written to the database. Null if not found.
     * @throws Exception if an error occurs.
     */
    private Object findAndUpdateOne(Object e, QueryInfo queryInfo, EntityManager em) throws Exception {
        Class<?> singleType = queryInfo.getSingleResultType();
        String jpql = queryInfo.jpql;
        EntityInfo entityInfo = queryInfo.entityInfo;

        int versionParamIndex = 2;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = entityInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = entityInfo.getAttribute(e, entityInfo.getAttributeName("id", true));
        if (id == null) {
            jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
            if (version != null)
                jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> query = em.createQuery(jpql, singleType); // TODO for records, use the entity class, not the record class
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        // id parameter(s)

        int p = 0;
        if (entityInfo.idClassAttributeAccessors != null) {
            throw new UnsupportedOperationException(); // TODO
        } else if (id != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + id.getClass().getSimpleName());
            query.setParameter(++p, id);
        }

        // version parameter

        if (entityInfo.versionAttributeName != null && version != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + version.getClass().getSimpleName());
            query.setParameter(++p, version);
        }

        List<?> results = query.getResultList();

        Object entity;
        if (results.isEmpty()) {
            entity = null;
        } else {
            entity = results.get(0);
            entity = em.merge(toEntity(e));
        }
        return entity;
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
     * Generates JQPL for deletion by id, for find-and-delete repository operations.
     */
    private String generateDeleteById(QueryInfo queryInfo) {
        EntityInfo entityInfo = queryInfo.entityInfo;
        String o = queryInfo.entityVar;
        StringBuilder q;
        if (entityInfo.idClassAttributeAccessors == null) {
            String idAttrName = entityInfo.attributeNames.get("id");
            q = new StringBuilder(24 + entityInfo.name.length() + o.length() * 2 + idAttrName.length()) //
                            .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o).append(" WHERE ") //
                            .append(o).append('.').append(idAttrName).append("=?1");
        } else {
            q = new StringBuilder(200) //
                            .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o).append(" WHERE ");
            int count = 0;
            for (String idClassAttrName : entityInfo.idClassAttributeAccessors.keySet()) {
                if (++count != 1)
                    q.append(" AND ");
                q.append(o).append('.').append(entityInfo.getAttributeName(idClassAttrName, true)).append("=?").append(count);
            }
        }
        return q.toString();
    }

    /**
     * Generates JPQL for deletion by entity id and version (if versioned).
     */
    private StringBuilder generateDeleteEntity(QueryInfo queryInfo) {
        EntityInfo entityInfo = queryInfo.entityInfo;
        String o = queryInfo.entityVar;

        StringBuilder q = new StringBuilder(100) //
                        .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);

        if (queryInfo.method.getParameterCount() == 0) {
            queryInfo.type = QueryInfo.Type.DELETE;
            queryInfo.hasWhere = false;
        } else {
            queryInfo.init(Delete.class, QueryInfo.Type.DELETE_WITH_ENTITY_PARAM);
            queryInfo.hasWhere = true;

            q.append(" WHERE (");

            String idName = entityInfo.getAttributeName("id", true);
            if (idName == null && entityInfo.idClassAttributeAccessors != null) {
                boolean first = true;
                for (String name : entityInfo.idClassAttributeAccessors.keySet()) {
                    if (first)
                        first = false;
                    else
                        q.append(" AND ");

                    name = entityInfo.attributeNames.get(name);
                    q.append(o).append('.').append(name).append("=?").append(++queryInfo.paramCount);
                }
            } else {
                q.append(o).append('.').append(idName).append("=?").append(++queryInfo.paramCount);
            }

            if (entityInfo.versionAttributeName != null)
                q.append(" AND ").append(o).append('.').append(entityInfo.versionAttributeName).append("=?").append(++queryInfo.paramCount);

            q.append(')');
        }

        return q;
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

        endBefore -= condition.length;

        boolean negated = endsWith("Not", methodName, start, endBefore);
        endBefore -= (negated ? 3 : 0);

        String function = null;
        boolean ignoreCase = false;
        boolean rounded = false;

        switch (methodName.charAt(endBefore - 1)) {
            case 'e':
                if (ignoreCase = endsWith("IgnoreCas", methodName, start, endBefore - 1)) {
                    function = "LOWER(";
                    endBefore -= 10;
                } else if (endsWith("WithMinut", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (MINUTE FROM ";
                    endBefore -= 10;
                } else if (endsWith("AbsoluteValu", methodName, start, endBefore - 1)) {
                    function = "ABS(";
                    endBefore -= 13;
                }
                break;
            case 'd':
                if (rounded = endsWith("Rounde", methodName, start, endBefore - 1)) {
                    function = "ROUND(";
                    endBefore -= 7;
                } else if (endsWith("WithSecon", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (SECOND FROM ";
                    endBefore -= 10;
                }
                break;
            case 'n':
                if (endsWith("RoundedDow", methodName, start, endBefore - 1)) {
                    function = "FLOOR(";
                    endBefore -= 11;
                }
                break;
            case 'p':
                if (endsWith("RoundedU", methodName, start, endBefore - 1)) {
                    function = "CEILING(";
                    endBefore -= 9;
                }
                break;
            case 'r':
                if (endsWith("WithYea", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (YEAR FROM ";
                    endBefore -= 8;
                } else if (endsWith("WithHou", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (HOUR FROM ";
                    endBefore -= 8;
                } else if (endsWith("WithQuarte", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (QUARTER FROM ";
                    endBefore -= 11;
                }
                break;
            case 't':
                if (endsWith("CharCoun", methodName, start, endBefore - 1)) {
                    function = "LENGTH(";
                    endBefore -= 9;
                } else if (endsWith("ElementCoun", methodName, start, endBefore - 1)) {
                    function = "SIZE(";
                    endBefore -= 12;
                }
                break;
            case 'y':
                if (endsWith("WithDa", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (DAY FROM ";
                    endBefore -= 7;
                }
                break;
            case 'h':
                if (endsWith("WithMont", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (MONTH FROM ";
                    endBefore -= 9;
                }
                break;
            case 'k':
                if (endsWith("WithWee", methodName, start, endBefore - 1)) {
                    function = "EXTRACT (WEEK FROM ";
                    endBefore -= 8;
                }
                break;
        }

        boolean trimmed = endsWith("Trimmed", methodName, start, endBefore);
        endBefore -= (trimmed ? 7 : 0);

        String attribute = methodName.substring(start, endBefore);

        if (attribute.length() == 0)
            throw new MappingException("Entity property name is missing."); // TODO possibly combine with unknown entity property name

        String name = queryInfo.entityInfo.getAttributeName(attribute, true);
        if (name == null) {
            if (attribute.length() == 3) {
                // TODO We might be able to remove special cases like this now that we have the entity parameter pattern
                // Special case for BasicRepository.deleteAll and BasicRepository.findAll
                int len = q.length(), where = q.lastIndexOf(" WHERE (");
                if (where + 8 == len)
                    q.delete(where, len); // Remove " WHERE " because there are no conditions
                queryInfo.hasWhere = false;
            } else if (queryInfo.entityInfo.idClassAttributeAccessors != null && attribute.equalsIgnoreCase("id")) {
                generateConditionsForIdClass(queryInfo, null, condition, ignoreCase, negated, q);
            }
            return;
        }

        StringBuilder attributeExpr = new StringBuilder();
        if (function != null)
            attributeExpr.append(function); // such as LOWER(  or  ROUND(
        if (trimmed)
            attributeExpr.append("TRIM(");

        String o = queryInfo.entityVar;
        attributeExpr.append(o).append('.').append(name);

        if (trimmed)
            attributeExpr.append(')');
        if (function != null)
            if (rounded)
                attributeExpr.append(", 0)"); // round to zero digits beyond the decimal
            else
                attributeExpr.append(')');

        if (negated) {
            Condition negatedCondition = condition.negate();
            if (negatedCondition != null) {
                condition = negatedCondition;
                negated = false;
            }
        }

        boolean isCollection = queryInfo.entityInfo.collectionElementTypes.containsKey(name);
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

            String name = queryInfo.entityInfo.getAttributeName(idClassAttr, true);
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
     * Generates JPQL based on method parameters.
     * Method parameters with annotations such as Assign, Add, Multiply, and Divide indicate an update method.
     * Method annotations such as Count, Delete, and Exists indicate the respective type of method if present.
     * Methods with a none of the above are considered find methods.
     * Find methods can have special type parameters (Pageable, Limit, Sort, Sort[]). Other methods cannot.
     *
     * @param queryInfo  query information
     * @param q          JPQL query to which to append the WHERE clause. Or null to create a new JPQL query.
     * @param methodAnno Count, Delete, Exists, or Update annotation on the method. Otherwise null.
     * @param isUpdate   True if known to be an update. False if known to not be an update. Null if unknown.
     */
    private StringBuilder generateFromParameters(QueryInfo queryInfo, StringBuilder q, Annotation methodAnno, Boolean isUpdate) {
        System.out.println("generateFromParameters for " + queryInfo + "\r\n  annotated with  " + methodAnno + " and starting from " + q);
        Parameter[] params = queryInfo.method.getParameters();
        String o = queryInfo.entityVar;

        int numAttributeParams = params.length;
        while (numAttributeParams > 0 && SPECIAL_PARAM_TYPES.contains(params[numAttributeParams - 1].getType()))
            numAttributeParams--;

        Annotation[] updateOperationAnnos = new Annotation[numAttributeParams];
        String[] updateOperationAttrs = new String[numAttributeParams];
        int numUpdateOps = 0;
        for (int i = 0; i < numAttributeParams; i++) {
            Parameter param = params[i];
            Set<Annotation> paramAnnos = new HashSet<>();

            Assign assign = param.getAnnotation(Assign.class);
            if (assign != null) {
                paramAnnos.add(updateOperationAnnos[i] = assign);
                updateOperationAttrs[i] = assign.value();
            }

            Add add = param.getAnnotation(Add.class);
            if (add != null) {
                paramAnnos.add(updateOperationAnnos[i] = add);
                updateOperationAttrs[i] = add.value();
            }

            Divide divide = param.getAnnotation(Divide.class);
            if (divide != null) {
                paramAnnos.add(updateOperationAnnos[i] = divide);
                updateOperationAttrs[i] = divide.value();
            }

            Multiply multiply = param.getAnnotation(Multiply.class);
            if (multiply != null) {
                paramAnnos.add(updateOperationAnnos[i] = multiply);
                updateOperationAttrs[i] = multiply.value();
            }

            SubtractFrom subtractFrom = param.getAnnotation(SubtractFrom.class);
            if (subtractFrom != null) {
                paramAnnos.add(updateOperationAnnos[i] = subtractFrom);
                updateOperationAttrs[i] = subtractFrom.value();
            }

            if (paramAnnos.size() > 1)
                throw new MappingException("The " + getName(param, i) + " parameter of the " +
                                           queryInfo.method.getName() + " method of the " +
                                           repositoryInterface.getName() + " repository has an invalid combination of annotations: " +
                                           paramAnnos + "."); // TODO NLS

            if (updateOperationAnnos[i] != null) {
                numUpdateOps++;
                if (isUpdate == null)
                    isUpdate = Boolean.TRUE;
                else if (Boolean.FALSE.equals(isUpdate))
                    throw new MappingException("The " + getName(param, i) + " parameter of the " +
                                               queryInfo.method.getName() + " method of the " +
                                               repositoryInterface.getName() + " repository must not be annotated with the " +
                                               updateOperationAnnos[i].annotationType().getName() +
                                               " annotation because the repository method is not an update method."); // TODO NLS
            }
        }

        System.out.println("  found Annos: " + Arrays.toString(updateOperationAnnos));

        if (isUpdate == null)
            isUpdate = Boolean.FALSE;
        else if (numUpdateOps == 0 && Boolean.TRUE.equals(isUpdate))
            throw new MappingException("Because the " + queryInfo.method.getName() + " method of the " +
                                       repositoryInterface.getName() + " repository is an update operation, it must have either " +
                                       "a single parameter that is the entity type or an iterable, stream, or array of entity, " +
                                       "Or it must have at least one parameter that is annotated with one of " +
                                       List.of(Assign.class, Add.class, Multiply.class, Divide.class) + "."); // TODO

        if (numAttributeParams < params.length && (methodAnno != null || Boolean.TRUE.equals(isUpdate)))
            throw new MappingException("The special parameter types " + SPECIAL_PARAM_TYPES +
                                       " must not be used on the " + queryInfo.method.getName() + " method of the " +
                                       repositoryInterface.getName() + " repository because the repository method is a " +
                                       (methodAnno == null ? "Update" : methodAnno.annotationType().getSimpleName()) + " operation."); // TODO NLS

        Boolean isNamePresent = null; // unknown

        // Write new JPQL, starting with UPDATE or SELECT
        if (q == null)
            if (Boolean.FALSE.equals(isUpdate)) {
                queryInfo.type = QueryInfo.Type.FIND;
                q = generateSelectClause(queryInfo, queryInfo.method.getAnnotation(Select.class));
            } else {
                queryInfo.type = QueryInfo.Type.UPDATE;
                q = new StringBuilder(250).append("UPDATE ").append(queryInfo.entityInfo.name).append(' ').append(o).append(" SET");

                boolean first = true;
                for (int i = 0; i < numAttributeParams; i++)
                    if (updateOperationAnnos[i] != null) {
                        String attribute = updateOperationAttrs[i];
                        if ("".equals(attribute)) {
                            if (isNamePresent == null)
                                isNamePresent = params[i].isNamePresent();
                            if (Boolean.TRUE.equals(isNamePresent))
                                attribute = params[i].getName();
                            else
                                throw new MappingException("You must specify an entity attribute name as the value of the " +
                                                           updateOperationAnnos[i].annotationType().getName() + " annotation on parameter " + (i + 1) +
                                                           " of the " + queryInfo.method.getName() + " method of the " +
                                                           repositoryInterface.getName() + " repository or compile the application" +
                                                           " with the -parameters compiler option that preserves the parameter names."); // TODO NLS
                        }
                        String name = queryInfo.entityInfo.getAttributeName(attribute, true);

                        if (name == null) {
                            // TODO
                            //if (Assign.class.equals(updateOperationAnnos[i].annotationType()))
                            //    generateUpdatesForIdClass(queryInfo, update, first, q);
                            //else
                            throw new MappingException("The " + updateOperationAnnos[i].annotationType().getName() +
                                                       " annotation cannot be used on the " + getName(params[i], i) +
                                                       " parameter of the " + queryInfo.method.getName() + " method of the " +
                                                       repositoryInterface.getName() +
                                                       " repository cannot be used on the Id of the entity when the Id is an IdClass."); // TODO NLS
                        } else {
                            q.append(first ? " " : ", ").append(o).append('.').append(name).append("=");
                            first = false;

                            boolean withFunction = false;
                            Class<?> annoClass = updateOperationAnnos[i].annotationType();
                            if (Assign.class.equals(annoClass)) {
                            } else if (Add.class.equals(annoClass)) {
                                if (withFunction = CharSequence.class.isAssignableFrom(queryInfo.entityInfo.attributeTypes.get(name)))
                                    q.append("CONCAT(").append(o).append('.').append(name).append(',');
                                else
                                    q.append(o).append('.').append(name).append('+');
                            } else if (SubtractFrom.class.equals(annoClass)) {
                                q.append(o).append('.').append(name).append('-');
                            } else if (Multiply.class.equals(annoClass)) {
                                q.append(o).append('.').append(name).append('*');
                            } else if (Divide.class.equals(annoClass)) {
                                q.append(o).append('.').append(name).append('/');
                            } else { // should be unreachable
                                throw new UnsupportedOperationException(updateOperationAnnos[i].annotationType().getName());
                            }

                            queryInfo.paramCount++;
                            q.append('?').append(i + 1);

                            if (withFunction)
                                q.append(')');
                        }
                    }
            }

        // append the WHERE clause
        for (int i = 0; i < numAttributeParams; i++) {
            if (updateOperationAnnos[i] == null) {
                q.append(queryInfo.hasWhere == false ? " WHERE (" : " AND ");
                queryInfo.hasWhere = true;

                String attribute;
                if (isNamePresent == null)
                    isNamePresent = params[i].isNamePresent();
                if (Boolean.TRUE.equals(isNamePresent))
                    attribute = params[i].getName();
                else // TODO annotation for specifying attribute name
                    throw new MappingException("You must specify an entity attribute name as the value of the " +
                                               "[not-implemented-yet] annotation on parameter " + (i + 1) +
                                               " of the " + queryInfo.method.getName() + " method of the " +
                                               repositoryInterface.getName() + " repository or compile the application" +
                                               " with the -parameters compiler option that preserves the parameter names."); // TODO NLS

                String name = queryInfo.entityInfo.getAttributeName(attribute, true);
                if (name == null) {
                    // TODO generateConditionsForIdClass(...); continue;
                    throw new UnsupportedOperationException("Queries with IdClass"); // TODO NLS
                }

                queryInfo.paramCount++;
                q.append(o).append('.').append(name).append("=?").append(i + 1);
            }
        }
        if (queryInfo.hasWhere)
            q.append(')');

        return q;
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

    /**
     * Generates the JPQL ORDER BY clause. This method is common between the OrderBy annotation and keyword.
     */
    private void generateOrderBy(QueryInfo queryInfo, StringBuilder q) {
        Class<?> multiType = queryInfo.getMultipleResultType();

        boolean needsKeysetQueries = KeysetAwarePage.class.equals(multiType)
                                     || KeysetAwareSlice.class.equals(multiType)
                                     || Iterator.class.equals(multiType);

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

    /**
     * Handles Methods with Entity Parameter, Query by Method Name, and Query by Parameters patterns.
     * // TODO add the first and third patterns one piece at a time
     *
     * @param queryInfo  query information to populate.
     * @param countPages whether to generate a count query (for Page.totalElements and Page.totalPages).
     * @return the generated query written to a StringBuilder.
     */
    private StringBuilder generateQueryFromMethod(QueryInfo queryInfo, boolean countPages) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        Class<?> entityClass = queryInfo.entityInfo.recordClass == null ? queryInfo.entityInfo.entityClass : queryInfo.entityInfo.recordClass;

        EntityInfo entityInfo = queryInfo.entityInfo;
        String methodName = queryInfo.method.getName();
        Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
        String o = queryInfo.entityVar;
        StringBuilder q = null;

        // Methods with Entity Parameter: save, insert
        if (methodName.startsWith("save")) {
            queryInfo.type = QueryInfo.Type.SAVE;
            if (paramTypes.length != 1)
                throw new UnsupportedOperationException("Repository " + "save" + " operations must have exactly 1 parameter," +
                                                        " which can be the entity or a collection or array of entities. The " + methodName +
                                                        " method has " + paramTypes.length + " parameters."); // TODO NLS
            queryInfo.entityParamType = paramTypes[0];
        } else if (methodName.startsWith("insert")) {
            queryInfo.type = QueryInfo.Type.INSERT;
            if (paramTypes.length != 1)
                throw new UnsupportedOperationException("Repository " + "insert" + " operations must have exactly 1 parameter," +
                                                        " which can be the entity or a collection or array of entities. The " + methodName +
                                                        " method has " + paramTypes.length + " parameters."); // TODO NLS
            queryInfo.entityParamType = paramTypes[0];
        }
        // Methods with Entity Parameter: delete, update
        if (queryInfo.type == null && paramTypes.length == 1) {
            if (entityClass.equals(paramTypes[0])) {
                queryInfo.entityParamType = paramTypes[0];
            } else if (paramTypes[0].isArray()) {
                if (entityClass.equals(paramTypes[0].getComponentType()))
                    queryInfo.entityParamType = paramTypes[0];
            } else if (Iterable.class.isAssignableFrom(paramTypes[0]) || Stream.class.isAssignableFrom(paramTypes[0])) {
                Type paramType = queryInfo.method.getGenericParameterTypes()[0];
                if (paramType instanceof ParameterizedType) {
                    Type[] typeVars = ((ParameterizedType) paramType).getActualTypeArguments();
                    Type typeVar = typeVars.length == 1 ? typeVars[0] : null;
                    if (entityClass.equals(typeVar)) {
                        queryInfo.entityParamType = paramTypes[0];
                    }
                }
            }

            if (queryInfo.type == null && queryInfo.entityParamType != null)
                if (methodName.startsWith("delete") || methodName.startsWith("remove"))
                    q = generateDeleteEntity(queryInfo);
                else if (methodName.startsWith("update"))
                    q = generateUpdateEntity(queryInfo);
                else
                    throw new UnsupportedOperationException("The " + methodName + " method of the " + repositoryInterface.getName() +
                                                            " repository interface must have a name that begins with one of " +
                                                            "(delete, insert, save, update)" +
                                                            " because the method's parameter accepts entity instances.");
        }

        int by = queryInfo.type == null ? methodName.indexOf("By") : -1;

        if (by >= 0) {
            // Query by method name

            if (methodName.startsWith("find")) {
                Select select = queryInfo.method.getAnnotation(Select.class);
                List<String> selections = select == null ? new ArrayList<>() : null;
                int c = by + 2;
                parseFindBy(queryInfo, methodName, by, selections);
                q = generateSelectClause(queryInfo, select, selections == null ? null : selections.toArray(new String[selections.size()]));

                int orderBy = methodName.indexOf("OrderBy", by + 2);
                if (orderBy > c || orderBy == -1 && methodName.length() > c) {
                    int where = q.length();
                    generateWhereClause(queryInfo, methodName, c, orderBy > 0 ? orderBy : methodName.length(), q);
                    if (countPages)
                        generateCount(queryInfo, q.substring(where));
                }
                if (orderBy >= c)
                    parseOrderBy(queryInfo, orderBy, q);
                queryInfo.type = QueryInfo.Type.FIND;
            } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
                int c = by + 2;
                boolean isFindAndDelete = queryInfo.isFindAndDelete();
                if (isFindAndDelete) {
                    if (queryInfo.type != null)
                        throw new UnsupportedOperationException("The " + queryInfo.method.getGenericReturnType() +
                                                                " return type is not supported for the " + methodName +
                                                                " repository method."); // TODO NLS
                    queryInfo.type = QueryInfo.Type.FIND_AND_DELETE;
                    parseDeleteBy(queryInfo, by);
                    Select select = null; // queryInfo.method.getAnnotation(Select.class); // TODO This would be limited by collision with update count/boolean
                    q = generateSelectClause(queryInfo, select);
                    queryInfo.jpqlDelete = generateDeleteById(queryInfo);
                } else { // DELETE
                    queryInfo.type = queryInfo.type == null ? QueryInfo.Type.DELETE : queryInfo.type;
                    q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
                }

                int orderBy = isFindAndDelete && by > 0 ? methodName.indexOf("OrderBy", by + 2) : -1;
                if (orderBy > c || orderBy == -1 && methodName.length() > c)
                    generateWhereClause(queryInfo, methodName, c, orderBy > 0 ? orderBy : methodName.length(), q);
                if (orderBy >= c)
                    parseOrderBy(queryInfo, orderBy, q);

                queryInfo.type = queryInfo.type == null ? QueryInfo.Type.DELETE : queryInfo.type;
            } else if (methodName.startsWith("update")) {
                q = generateUpdateClause(queryInfo, methodName, by + 2);
                queryInfo.type = QueryInfo.Type.UPDATE;
            } else if (methodName.startsWith("count")) {
                int c = by + 2;
                q = new StringBuilder(150).append("SELECT COUNT(").append(o).append(") FROM ").append(entityInfo.name).append(' ').append(o);
                if (methodName.length() > c)
                    generateWhereClause(queryInfo, methodName, c, methodName.length(), q);
                queryInfo.type = QueryInfo.Type.COUNT;
            } else if (methodName.startsWith("exists")) {
                int c = by + 2;
                String name = entityInfo.idClassAttributeAccessors == null ? "id" : entityInfo.idClassAttributeAccessors.firstKey();
                String attrName = entityInfo.getAttributeName(name, true);
                q = new StringBuilder(200).append("SELECT ").append(o).append('.').append(attrName) //
                                .append(" FROM ").append(entityInfo.name).append(' ').append(o);
                if (methodName.length() > c)
                    generateWhereClause(queryInfo, methodName, c, methodName.length(), q);
                queryInfo.type = QueryInfo.Type.EXISTS;
            }
        }

        if (queryInfo.type == null) {
            // Might be Query by Parameters

            if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
                boolean isFindAndDelete = queryInfo.isFindAndDelete();
                if (isFindAndDelete) {
                    if (queryInfo.type != null)
                        throw new UnsupportedOperationException("The " + queryInfo.method.getGenericReturnType() +
                                                                " return type is not supported for the " + methodName +
                                                                " repository method."); // TODO NLS
                    queryInfo.type = QueryInfo.Type.FIND_AND_DELETE;
                    parseDeleteBy(queryInfo, by);
                    Select select = null; // queryInfo.method.getAnnotation(Select.class); // TODO This would be limited by collision with update count/boolean
                    q = generateSelectClause(queryInfo, select);
                    queryInfo.jpqlDelete = generateDeleteById(queryInfo);
                } else { // DELETE
                    queryInfo.type = queryInfo.type == null ? QueryInfo.Type.DELETE : queryInfo.type;
                    q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
                }
                if (queryInfo.method.getParameterCount() > 0)
                    generateWhereClause(queryInfo, q);
                queryInfo.type = queryInfo.type == null ? QueryInfo.Type.DELETE : queryInfo.type;
            } else if (methodName.startsWith("count")) {
                q = new StringBuilder(150).append("SELECT COUNT(").append(o).append(") FROM ").append(entityInfo.name).append(' ').append(o);
                if (queryInfo.method.getParameterCount() > 0)
                    generateWhereClause(queryInfo, q);
                queryInfo.type = QueryInfo.Type.COUNT;
            } else if (methodName.startsWith("exists")) {
                String name = entityInfo.idClassAttributeAccessors == null ? "id" : entityInfo.idClassAttributeAccessors.firstKey();
                String attrName = entityInfo.getAttributeName(name, true);
                q = new StringBuilder(200).append("SELECT ").append(o).append('.').append(attrName) //
                                .append(" FROM ").append(entityInfo.name).append(' ').append(o);
                if (queryInfo.method.getParameterCount() > 0)
                    generateWhereClause(queryInfo, q);
                queryInfo.type = QueryInfo.Type.EXISTS;
            } else {
                Update update = queryInfo.method.getAnnotation(Update.class);
                q = generateFromParameters(queryInfo, null, update, update == null ? null : Boolean.TRUE);
                System.out.println("  returned " + q);
            }
        }

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, queryInfo.method.getName() + " is identified as a " + queryInfo.type + " method");

        return q;
    }

    /**
     * Generates the SELECT clause of the JPQL.
     *
     * @param queryInfo  query information
     * @param select     Select annotation if present on the method.
     * @param selections selections from find...By if present and there is no Select annotation.
     * @return the SELECT clause.
     */
    private StringBuilder generateSelectClause(QueryInfo queryInfo, Select select, String... selections) {
        StringBuilder q = new StringBuilder(200);
        String o = queryInfo.entityVar;
        EntityInfo entityInfo = queryInfo.entityInfo;

        boolean distinct = select != null && select.distinct();
        String function = select == null ? null : toFunctionName(select.function());
        String[] cols;
        if (select == null) {
            cols = selections;
        } else {
            selections = select.value();
            cols = new String[selections == null ? 0 : selections.length];
            for (int i = 0; i < cols.length; i++) {
                String name = entityInfo.getAttributeName(selections[i], true);
                cols[i] = name == null ? selections[i] : name;
            }
        }

        Class<?> singleType = queryInfo.getSingleResultType();

        if (singleType.isPrimitive())
            singleType = QueryInfo.wrapperClassIfPrimitive(singleType);

        q.append("SELECT ");

        if (cols == null || cols.length == 0) {
            if (singleType.isAssignableFrom(entityInfo.entityClass)
                || entityInfo.inheritance && entityInfo.entityClass.isAssignableFrom(singleType)) {
                // Whole entity
                q.append(distinct ? "DISTINCT " : "").append(o);
            } else {
                // Look for single entity attribute with the desired type:
                String singleAttributeName = null;
                for (Map.Entry<String, Class<?>> entry : entityInfo.attributeTypes.entrySet()) {
                    Class<?> collectionElementType = entityInfo.collectionElementTypes.get(entry.getKey());
                    Class<?> attributeType = collectionElementType == null ? entry.getValue() : collectionElementType;
                    if (attributeType.isPrimitive())
                        attributeType = QueryInfo.wrapperClassIfPrimitive(attributeType);
                    if (singleType.isAssignableFrom(attributeType)) {
                        singleAttributeName = entry.getKey();
                        q.append(distinct ? "DISTINCT " : "").append(o).append('.').append(singleAttributeName);
                        break;
                    }
                }

                if (singleAttributeName == null) {
                    // Construct new instance from IdClass, embeddable, or entity attributes.
                    // It would be preferable if the spec included the Select annotation to explicitly identify parameters, but if that doesn't happen
                    // TODO we could compare attribute types with known constructor to improve on guessing a correct order of parameters
                    q.append("NEW ").append(singleType.getName()).append('(');
                    List<String> relAttrNames;
                    boolean first = true;
                    if (entityInfo.idClassAttributeAccessors != null && singleType.equals(entityInfo.idType))
                        for (String idClassAttributeName : entityInfo.idClassAttributeAccessors.keySet()) {
                            String name = entityInfo.getAttributeName(idClassAttributeName, true);
                            generateSelectExpression(q, first, function, distinct, o, name);
                            first = false;
                        }
                    else if ((relAttrNames = entityInfo.relationAttributeNames.get(singleType)) != null)
                        for (String name : relAttrNames) {
                            generateSelectExpression(q, first, function, distinct, o, name);
                            first = false;
                        }
                    else if (entityInfo.recordClass == null)
                        for (String name : entityInfo.attributeTypes.keySet()) {
                            generateSelectExpression(q, first, function, distinct, o, name);
                            first = false;
                        }
                    else {
                        for (RecordComponent component : entityInfo.recordClass.getRecordComponents()) {
                            String name = component.getName();
                            generateSelectExpression(q, first, function, distinct, o, name);
                            first = false;
                        }
                    }
                    q.append(')');
                }
            }
        } else { // Individual columns are requested by @Select
            Class<?> entityType = entityInfo.getType();
            boolean selectAsColumns = singleType.isAssignableFrom(entityType)
                                      || singleType.isInterface() // NEW instance doesn't apply to interfaces
                                      || singleType.isPrimitive() // NEW instance should not be used on primitives
                                      || singleType.getName().startsWith("java") // NEW instance constructor is unlikely for non-user-defined classes
                                      || entityInfo.inheritance && entityType.isAssignableFrom(singleType);
            if (!selectAsColumns && cols.length == 1) {
                String singleAttributeName = cols[0];
                Class<?> attributeType = entityInfo.collectionElementTypes.get(singleAttributeName);
                if (attributeType == null)
                    attributeType = entityInfo.attributeTypes.get(singleAttributeName);
                selectAsColumns = attributeType != null && (Object.class.equals(attributeType) // JPA metamodel does not preserve the type if not an EmbeddableCollection
                                                            || singleType.isAssignableFrom(attributeType));
            }
            if (selectAsColumns) {
                // Specify columns without creating new instance
                for (int i = 0; i < cols.length; i++) {
                    generateSelectExpression(q, i == 0, function, distinct, o, cols[i]);
                }
            } else {
                // Construct new instance from defined columns
                q.append("NEW ").append(singleType.getName()).append('(');
                for (int i = 0; i < cols.length; i++) {
                    generateSelectExpression(q, i == 0, function, distinct, o, cols[i]);
                }
                q.append(')');
            }
        }

        q.append(" FROM ").append(entityInfo.name).append(' ').append(o);
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
            String name = queryInfo.entityInfo.getAttributeName(attribute, true);

            if (name == null) {
                if (op == '=') {
                    generateUpdatesForIdClass(queryInfo, first, q);
                } else {
                    String opName = op == '+' ? "Add" : op == '*' ? "Multiply" : "Divide";
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
     * Generates JPQL for updates of an entity by entity id and version (if versioned).
     */
    private StringBuilder generateUpdateEntity(QueryInfo queryInfo) {
        EntityInfo entityInfo = queryInfo.entityInfo;
        String o = queryInfo.entityVar;
        StringBuilder q;

        String idName = queryInfo.entityInfo.getAttributeName("id", true);
        if (idName == null && queryInfo.entityInfo.idClassAttributeAccessors != null) {
            // TODO support this similar to what generateDeleteEntity does
            throw new MappingException("Update operations cannot be used on entities with composite IDs."); // TODO NLS
        }

        Class<?> singleType = queryInfo.getSingleResultType();
        if (UPDATE_COUNT_TYPES.contains(singleType)) {
            queryInfo.init(Update.class, QueryInfo.Type.UPDATE_WITH_ENTITY_PARAM);
            queryInfo.hasWhere = true;

            q = new StringBuilder(100) //
                            .append("UPDATE ").append(entityInfo.name).append(' ').append(o) //
                            .append(" SET ");

            boolean first = true;
            for (String name : entityInfo.getAttributeNamesForEntityUpdate()) {
                if (first)
                    first = false;
                else
                    q.append(", ");

                q.append(o).append('.').append(name).append("=?").append(++queryInfo.paramCount);
            }

            q.append(" WHERE ").append(o).append('.').append(idName).append("=?").append(++queryInfo.paramCount);

            if (entityInfo.versionAttributeName != null)
                q.append(" AND ").append(o).append('.').append(entityInfo.versionAttributeName).append("=?").append(++queryInfo.paramCount);
        } else {
            // Update that returns an entity - perform a find operation first so that em.merge can be used
            queryInfo.init(Update.class, QueryInfo.Type.UPDATE_WITH_ENTITY_PARAM_AND_RESULT);
            queryInfo.hasWhere = true;

            q = new StringBuilder(100) //
                            .append("SELECT ").append(o).append(" FROM ").append(entityInfo.name).append(' ').append(o) //
                            .append(" WHERE ").append(o).append('.').append(idName).append("=?").append(++queryInfo.paramCount);

            if (entityInfo.versionAttributeName != null)
                q.append(" AND ").append(o).append('.').append(entityInfo.versionAttributeName).append("=?").append(++queryInfo.paramCount);
        }

        return q;
    }

    /**
     * Generates JPQL to assign the entity properties of which the IdClass consists.
     */
    private void generateUpdatesForIdClass(QueryInfo queryInfo, boolean firstOperation, StringBuilder q) {

        int count = 0;
        for (String idClassAttr : queryInfo.entityInfo.idClassAttributeAccessors.keySet()) {
            count++;
            String name = queryInfo.entityInfo.getAttributeName(idClassAttr, true);

            q.append(firstOperation ? " " : ", ").append(queryInfo.entityVar).append('.').append(name) //
                            .append("=?").append(++queryInfo.paramCount);
            if (count != 1)
                queryInfo.paramAddedCount++;

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
            Function functions[] = filter.fn();
            Compare comparison = filter.op();
            Compare negatedFrom = comparison.negated();
            boolean negated = negatedFrom != null;
            if (negated)
                comparison = negatedFrom;

            if (attribute.length() == 0)
                throw new MappingException("Entity property name is missing."); // TODO possibly combine with unknown entity property name

            boolean ignoreCase = false;
            StringBuilder attributeExpr = new StringBuilder();
            for (int f = functions.length - 1; f >= 0; f--) {
                if (functions[f] == Function.IgnoreCase)
                    ignoreCase = true;
                String functionCall = FUNCTION_CALLS.get(functions[f]);
                if (functionCall == null)
                    throw new UnsupportedOperationException(functions[f].name()); // should never occur
                attributeExpr.append(functionCall);
            }

            String name = queryInfo.entityInfo.getAttributeName(attribute, true);
            if (name == null) {
                generateConditionsForIdClass(queryInfo, filter, Condition.forIdClass(comparison), ignoreCase, negated, q);
                continue;
            }

            String o = queryInfo.entityVar;
            attributeExpr.append(o).append('.').append(name);

            for (int f = functions.length - 1; f >= 0; f--) {
                if (functions[f] == Function.Rounded)
                    attributeExpr.append(", 0)"); // round to zero digits beyond the decimal
                else
                    attributeExpr.append(')');
            }

            boolean isCollection = queryInfo.entityInfo.collectionElementTypes.containsKey(name);
            if (isCollection)
                verifyCollectionsSupported(name, ignoreCase, comparison);

            switch (comparison) {
                case Equal:
                    q.append(attributeExpr).append(negated ? Condition.NOT_EQUALS.operator : Condition.EQUALS.operator);
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
                    break;
                case GreaterThan:
                    q.append(attributeExpr).append(Condition.GREATER_THAN.operator);
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
                    break;
                case GreaterThanEqual:
                    q.append(attributeExpr).append(Condition.GREATER_THAN_EQUAL.operator);
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
                    break;
                case LessThan:
                    q.append(attributeExpr).append(Condition.LESS_THAN.operator);
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
                    break;
                case LessThanEqual:
                    q.append(attributeExpr).append(Condition.LESS_THAN_EQUAL.operator);
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
                    break;
                case StartsWith:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT(");
                    appendParamOrValue(q, queryInfo, filter, ignoreCase).append(", '%')");
                    break;
                case EndsWith:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                    appendParamOrValue(q, queryInfo, filter, ignoreCase).append(")");
                    break;
                case Like:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE ");
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
                    break;
                case Between:
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ");
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
                    break;
                case Contains:
                    if (isCollection) {
                        q.append(' ');
                        appendParamOrValue(q, queryInfo, filter, ignoreCase) //
                                        .append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
                    } else {
                        q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                        appendParamOrValue(q, queryInfo, filter, ignoreCase).append(", '%')");
                    }
                    break;
                case In:
                    if (ignoreCase)
                        throw new MappingException(new UnsupportedOperationException("Repository keyword IgnoreCase cannot be combined with the In keyword.")); // TODO
                    q.append(attributeExpr).append(negated ? " NOT " : "").append(Condition.IN.operator);
                    appendParamOrValue(q, queryInfo, filter, ignoreCase);
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

    /**
     * Generates the JPQL WHERE clause based on method parameters.
     *
     * @param queryInfo query information
     * @param q         JPQL query to which to append the WHERE clause.
     */
    private void generateWhereClause(QueryInfo queryInfo, StringBuilder q) {
        Parameter[] params = queryInfo.method.getParameters();

        // TODO reject special parameters on methods other than find with a meaningful error
        int numQueryConditions = params.length;
        while (numQueryConditions > 0 && SPECIAL_PARAM_TYPES.contains(params[numQueryConditions - 1].getType()))
            numQueryConditions--;

        if (numQueryConditions == 0)
            return; // all parameters have special parameter types

        if (!params[0].isNamePresent())
            throw new MappingException("To use the Queries by Parameter name pattern for repository methods, " +
                                       " you must compile the application with the -parameters compiler option " +
                                       " that preserves the parameter names."); // TODO NLS

        queryInfo.hasWhere = true;
        for (int i = 0; i < numQueryConditions; i++) {
            q.append(i == 0 ? " WHERE (" : " AND ");

            String name = queryInfo.entityInfo.getAttributeName(params[i].getName(), true);
            if (name == null) {
                // TODO generateConditionsForIdClass(...); continue;
                throw new UnsupportedOperationException("Query by Parameters with IdClass"); // TODO NLS
            }

            String o = queryInfo.entityVar;
            q.append(o).append('.').append(name).append("=?").append(++queryInfo.paramCount);
        }

        q.append(')');
    }

    /**
     * Return a name for the parameter, suitable for display in an NLS message.
     *
     * @param param parameter
     * @param index zero-based method index.
     * @return parameter name.
     */
    @Trivial
    private static final String getName(Parameter param, int index) {
        return param.isNamePresent() //
                        ? param.getName() //
                        : ("(" + (index + 1) + ")");
    }

    /**
     * Identifies possible positions of named parameters within the JPQL.
     *
     * @param jpql JPQL
     * @return possible positions of named parameters within the JPQL.
     */
    @Trivial
    private List<Integer> getParameterPositions(String jpql) {
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; (index = jpql.indexOf(':', index)) >= 0;)
            positions.add(++index);
        return positions;
    }

    /**
     * Request an instance of a resource of the specified type.
     *
     * @param type resource type.
     * @return instance of the resource. Never null.
     * @throws UnsupportedOperationException if the type of resource is not available.
     */
    private <T> T getResource(Class<T> type) {
        Deque<EntityManager> resources = defaultMethodResources.get();
        if (EntityManager.class.equals(type)) {
            EntityManager em = primaryEntityInfoFuture.join().persister.createEntityManager();
            if (resources == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, type + " accessed outside the scope of repository default method",
                             Arrays.toString(new Exception().getStackTrace()));
            } else {
                resources.add(em);
            }
            @SuppressWarnings("unchecked")
            T t = (T) em;
            return t;
        }
        throw new UnsupportedOperationException("The " + type.getName() + " type of resource is not available from the Jakarta Data provider."); // TODO NLS
    }

    /**
     * Inserts entities (or records) into the database.
     * An error is raised if any of the entities (or records) already exist in the database.
     *
     * @param arg       the entity or record, or array or Iterable or Stream of entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the updated entities, using the return type that is required by Insert method signature.
     * @throws Exception if an error occurs.
     */
    private Object insert(Object arg, QueryInfo queryInfo, EntityManager em) throws Exception {
        Class<?> singleType = queryInfo.getSingleResultType();
        boolean resultVoid = void.class.equals(singleType) || Void.class.equals(singleType);
        ArrayList<Object> results;

        if (queryInfo.entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = resultVoid ? null : new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                Object entity = toEntity(Array.get(arg, i));
                em.persist(entity);
                if (results != null)
                    results.add(entity);
            }
            em.flush();
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            if (arg instanceof Iterable) {
                results = resultVoid ? null : new ArrayList<>();
                for (Object e : ((Iterable<?>) arg)) {
                    Object entity = toEntity(e);
                    em.persist(entity);
                    if (results != null)
                        results.add(entity);
                }
                em.flush();
            } else {
                results = resultVoid ? null : new ArrayList<>(1);
                Object entity = toEntity(arg);
                em.persist(entity);
                em.flush();
                if (results != null)
                    results.add(entity);
            }
        }

        Object returnValue;
        if (resultVoid) {
            returnValue = null;
        } else {
            if (queryInfo.entityInfo.recordClass != null)
                for (int i = 0; i < results.size(); i++)
                    results.set(i, queryInfo.entityInfo.toRecord(results.get(i)));

            if (queryInfo.returnArrayType != null) {
                Object[] newArray = (Object[]) Array.newInstance(queryInfo.returnArrayType, results.size());
                returnValue = results.toArray(newArray);
            } else {
                Class<?> multiType = queryInfo.getMultipleResultType();
                if (multiType == null)
                    returnValue = results.isEmpty() ? null : results.get(0); // TODO error if multiple results? Detect earlier?
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = toIterable(multiType, null, results);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw new UnsupportedOperationException(multiType + " is an unsupported return type."); // TODO NLS
            }
        }

        Class<?> returnType = queryInfo.method.getReturnType();
        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw new MappingException("The " + returnType.getName() + " return type of the " +
                                       queryInfo.method.getName() + " method of the " +
                                       queryInfo.method.getDeclaringClass().getName() +
                                       " class is not a valid return type for a repository " +
                                       "@Insert" + " method."); // TODO NLS
        }

        return returnValue;
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        CompletableFuture<QueryInfo> queryInfoFuture = queries.get(method);
        boolean isDefaultMethod = false;

        if (queryInfoFuture == null)
            if (method.isDefault()) {
                isDefaultMethod = true;
            } else {
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

            if (isDefaultMethod) {
                Deque<EntityManager> resourceStack = defaultMethodResources.get();
                boolean added;
                if (added = (resourceStack == null))
                    defaultMethodResources.set(resourceStack = new LinkedList<>());
                else
                    resourceStack.add(null); // indicator of nested default method
                try {
                    Object returnValue = InvocationHandler.invokeDefault(proxy, method, args);
                    if (trace && tc.isEntryEnabled())
                        Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), returnValue);
                    return returnValue;
                } finally {
                    for (EntityManager em; (em = resourceStack.pollLast()) != null;)
                        if (em.isOpen())
                            try {
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(this, tc, "close " + em);
                                em.close();
                            } catch (Throwable x) {
                                FFDCFilter.processException(x, getClass().getName(), "1827", this);
                            }
                    if (added)
                        defaultMethodResources.remove();
                }
            }

            QueryInfo queryInfo = queryInfoFuture.join();
            EntityInfo entityInfo = queryInfo.entityInfo;

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, queryInfo.toString());

            if (queryInfo.validateParams)
                validator.validateParameters(proxy, method, args);

            LocalTransactionCoordinator suspendedLTC = null;
            EntityManager em = null;
            Object returnValue;
            Class<?> returnType = method.getReturnType();
            boolean failed = true;

            boolean requiresTransaction;
            switch (queryInfo.type) {
                case FIND:
                case COUNT:
                case EXISTS:
                case RESOURCE_ACCESS:
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
                    case SAVE: {
                        em = entityInfo.persister.createEntityManager();
                        returnValue = save(args[0], queryInfo, em);
                        break;
                    }
                    case INSERT: {
                        em = entityInfo.persister.createEntityManager();
                        returnValue = insert(args[0], queryInfo, em);
                        break;
                    }
                    case FIND:
                    case FIND_AND_DELETE: {
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

                        Class<?> multiType = queryInfo.getMultipleResultType();

                        if (pagination != null && Iterator.class.equals(multiType))
                            returnValue = new PaginatedIterator<>(queryInfo, pagination, args);
                        else if (KeysetAwareSlice.class.equals(multiType) || KeysetAwarePage.class.equals(multiType))
                            returnValue = new KeysetAwarePageImpl<>(queryInfo, limit == null ? pagination : toPageable(limit), args);
                        else if (Slice.class.equals(multiType) || Page.class.equals(multiType) || pagination != null && Streamable.class.equals(multiType))
                            returnValue = new PageImpl<>(queryInfo, limit == null ? pagination : toPageable(limit), args);
                        else {
                            em = entityInfo.persister.createEntityManager();

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                Tr.debug(this, tc, "createQuery", queryInfo.jpql, entityInfo.entityClass.getName());

                            final QueryInfo qi = queryInfo;
                            final EntityManager eMgr = em;
                            TypedQuery<?> query = eMgr.createQuery(qi.jpql, qi.entityInfo.entityClass);
                            queryInfo.setParameters(query, args);

                            if (queryInfo.type == QueryInfo.Type.FIND_AND_DELETE)
                                query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

                            int maxResults = limit != null ? limit.maxResults() //
                                            : pagination != null ? pagination.size() //
                                                            : queryInfo.maxResults;

                            int startAt = limit != null ? computeOffset(limit) //
                                            : pagination != null ? computeOffset(pagination) //
                                                            : 0;

                            if (maxResults > 0) {
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(tc, "limit max results to " + maxResults);
                                query.setMaxResults(maxResults);
                            }
                            if (startAt > 0) {
                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(tc, "start at (0-based) position " + startAt);
                                query.setFirstResult(startAt);
                            }

                            if (multiType != null && BaseStream.class.isAssignableFrom(multiType)) {
                                Stream<?> stream = query.getResultStream();
                                if (Stream.class.equals(multiType)) // TODO FIND_AND_DELETE from stream?
                                    returnValue = stream;
                                else if (IntStream.class.equals(multiType))
                                    returnValue = stream.mapToInt(RepositoryImpl::toInt);
                                else if (LongStream.class.equals(multiType))
                                    returnValue = stream.mapToLong(RepositoryImpl::toLong);
                                else if (DoubleStream.class.equals(multiType))
                                    returnValue = stream.mapToDouble(RepositoryImpl::toDouble);
                                else
                                    throw new UnsupportedOperationException("Stream type " + multiType.getName());
                            } else {
                                Class<?> singleType = queryInfo.getSingleResultType();

                                List<?> results = query.getResultList();

                                if (queryInfo.type == QueryInfo.Type.FIND_AND_DELETE)
                                    for (Object result : results)
                                        if (result == null) {
                                            throw new DataException("Unable to delete from the database when the query result includes a null value."); // TODO NLS
                                        } else if (entityInfo.entityClass.isInstance(result)) {
                                            em.remove(result);
                                        } else if (entityInfo.idClassAttributeAccessors != null) {
                                            jakarta.persistence.Query delete = em.createQuery(queryInfo.jpqlDelete);
                                            int numParams = 0;
                                            for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                                                Object value = accessor instanceof Method ? ((Method) accessor).invoke(result) : ((Field) accessor).get(result);
                                                if (trace && tc.isDebugEnabled())
                                                    Tr.debug(this, tc, queryInfo.jpqlDelete,
                                                             "set ?" + (numParams + 1) + ' ' + (value == null ? null : value.getClass().getSimpleName()));
                                                delete.setParameter(++numParams, value);
                                            }
                                            delete.executeUpdate();
                                        } else { // is return value the entity or id?
                                            Object value = result;
                                            if (entityInfo.entityClass.isInstance(result) ||
                                                entityInfo.recordClass != null && entityInfo.recordClass.isInstance(result)) {
                                                List<Member> accessors = entityInfo.attributeAccessors.get(entityInfo.attributeNames.get("id"));
                                                if (accessors == null || accessors.isEmpty())
                                                    throw new MappingException("Unable to find the id attribute on the " + entityInfo.name + " entity."); // TODO NLS
                                                for (Member accessor : accessors)
                                                    value = accessor instanceof Method ? ((Method) accessor).invoke(value) : ((Field) accessor).get(value);
                                            } else if (!entityInfo.idType.isInstance(value)) {
                                                value = to(entityInfo.idType, result, false);
                                                if (value == result) // unable to convert value
                                                    throw new MappingException("Results for find-and-delete repository queries must be the entity class (" +
                                                                               (entityInfo.recordClass == null ? entityInfo.entityClass : entityInfo.recordClass).getName() +
                                                                               ") or the id class (" + entityInfo.idType +
                                                                               "), not the " + result.getClass().getName() + " class."); // TODO NLS
                                            }

                                            jakarta.persistence.Query delete = em.createQuery(queryInfo.jpqlDelete);
                                            if (trace && tc.isDebugEnabled())
                                                Tr.debug(this, tc, queryInfo.jpqlDelete,
                                                         "set ?1 " + (value == null ? null : value.getClass().getSimpleName()));
                                            delete.setParameter(1, value);
                                            delete.executeUpdate();
                                        }

                                if (results.isEmpty() && queryInfo.getOptionalResultType() != null) {
                                    returnValue = null;
                                } else if (multiType == null && (entityInfo.entityClass).equals(singleType)) {
                                    returnValue = oneResult(results);
                                } else if (multiType != null && multiType.isInstance(results) && (results.isEmpty() || !(results.get(0) instanceof Object[]))) {
                                    returnValue = results;
                                } else if (multiType != null && Iterable.class.isAssignableFrom(multiType)) {
                                    returnValue = toIterable(multiType, singleType, results);
                                } else if (Iterator.class.equals(multiType)) {
                                    returnValue = results.iterator();
                                } else if (queryInfo.returnArrayType != null) {
                                    int size = results.size();
                                    Object firstResult = size == 0 ? null : results.get(0);
                                    if (firstResult != null && firstResult.getClass().isArray()) {
                                        if (size == 1) {
                                            Class<?> optionalType = queryInfo.getOptionalResultType();
                                            if (firstResult.getClass().equals(optionalType))
                                                returnValue = firstResult;
                                            else {
                                                int len = Array.getLength(firstResult);
                                                returnValue = Array.newInstance(queryInfo.returnArrayType, len);
                                                for (int i = 0; i < len; i++) {
                                                    Object element = Array.get(firstResult, i);
                                                    Array.set(returnValue, i, queryInfo.returnArrayType.isInstance(element) //
                                                                    ? element : to(queryInfo.returnArrayType, element, true));
                                                }
                                            }
                                        } else { // result is a list of multiple arrays
                                            returnValue = results;
                                        }
                                    } else {
                                        // TODO Size 0 should be an error when the selected attribute is an array.
                                        // The following makes sense when not selecting an array attribute, and instead
                                        // using array to represent multiple results returned.
                                        returnValue = Array.newInstance(queryInfo.returnArrayType, size);
                                        int i = 0;
                                        for (Object result : results)
                                            Array.set(returnValue, i++, result);
                                    }
                                } else if (results.isEmpty()) {
                                    throw new EmptyResultException("Query with return type of " + returnType.getName() +
                                                                   " returned no results. If this is expected, specify a return type of array, List, Optional, Page, Slice, or Stream for the repository method.");
                                } else { // single result of other type
                                    returnValue = oneResult(results);
                                    if (returnValue != null && !singleType.isAssignableFrom(returnValue.getClass())) {
                                        // TODO these conversions are not all safe
                                        if (double.class.equals(singleType) || Double.class.equals(singleType))
                                            returnValue = ((Number) returnValue).doubleValue();
                                        else if (float.class.equals(singleType) || Float.class.equals(singleType))
                                            returnValue = ((Number) returnValue).floatValue();
                                        else if (long.class.equals(singleType) || Long.class.equals(singleType))
                                            returnValue = ((Number) returnValue).longValue();
                                        else if (int.class.equals(singleType) || Integer.class.equals(singleType))
                                            returnValue = ((Number) returnValue).intValue();
                                        else if (short.class.equals(singleType) || Short.class.equals(singleType))
                                            returnValue = ((Number) returnValue).shortValue();
                                        else if (byte.class.equals(singleType) || Byte.class.equals(singleType))
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
                        em = entityInfo.persister.createEntityManager();

                        jakarta.persistence.Query update = em.createQuery(queryInfo.jpql);
                        queryInfo.setParameters(update, args);

                        int updateCount = update.executeUpdate();

                        returnValue = toReturnValue(updateCount, returnType, queryInfo);
                        break;
                    }
                    case DELETE_WITH_ENTITY_PARAM: {
                        em = entityInfo.persister.createEntityManager();

                        Object arg = args[0] instanceof Stream //
                                        ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) //
                                        : args[0];
                        int updateCount = 0;
                        int numExpected = 0;

                        if (arg instanceof Iterable) {
                            for (Object e : ((Iterable<?>) arg)) {
                                numExpected++;
                                updateCount += remove(e, queryInfo, em);
                            }
                        } else if (queryInfo.entityParamType.isArray()) {
                            numExpected = Array.getLength(arg);
                            for (int i = 0; i < numExpected; i++)
                                updateCount += remove(Array.get(arg, i), queryInfo, em);
                        } else {
                            numExpected = 1;
                            updateCount = remove(arg, queryInfo, em);
                        }

                        if (updateCount < numExpected) {
                            Class<?> singleType = queryInfo.getSingleResultType();
                            if (void.class.equals(singleType) || Void.class.equals(singleType))
                                throw new OptimisticLockingFailureException((numExpected - updateCount) + " of the " +
                                                                            numExpected + " entities were not found for deletion."); // TODO NLS
                        }

                        returnValue = toReturnValue(updateCount, returnType, queryInfo);
                        break;
                    }
                    case UPDATE_WITH_ENTITY_PARAM: {
                        em = entityInfo.persister.createEntityManager();

                        Object arg = args[0] instanceof Stream //
                                        ? ((Stream<?>) args[0]).sequential().collect(Collectors.toList()) //
                                        : args[0];
                        int updateCount = 0;

                        if (arg instanceof Iterable) {
                            for (Object e : ((Iterable<?>) arg))
                                updateCount += update(e, queryInfo, em);
                        } else if (queryInfo.entityParamType.isArray()) {
                            int length = Array.getLength(arg);
                            for (int i = 0; i < length; i++)
                                updateCount += update(Array.get(arg, i), queryInfo, em);
                        } else {
                            updateCount = update(arg, queryInfo, em);
                        }

                        returnValue = toReturnValue(updateCount, returnType, queryInfo);
                        break;
                    }
                    case UPDATE_WITH_ENTITY_PARAM_AND_RESULT: {
                        em = entityInfo.persister.createEntityManager();
                        returnValue = findAndUpdate(args[0], queryInfo, em);
                        break;
                    }
                    case COUNT: {
                        em = entityInfo.persister.createEntityManager();

                        TypedQuery<Long> query = em.createQuery(queryInfo.jpql, Long.class);
                        queryInfo.setParameters(query, args);

                        Long result = query.getSingleResult();

                        Class<?> type = queryInfo.getSingleResultType();

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
                        em = entityInfo.persister.createEntityManager();

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
                    case RESOURCE_ACCESS: {
                        returnValue = getResource(returnType);
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException(queryInfo.type.name());
                }

                if (queryInfo.validateResult)
                    validator.validateReturnValue(proxy, method, returnValue);

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
            if (!isDefaultMethod && x instanceof Exception)
                x = failure((Exception) x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), x);
            throw x;
        }
    }

    @Trivial
    private final Object oneResult(List<?> results) {
        int size = results.size();
        if (size == 1)
            return results.get(0);
        else if (size == 0)
            throw new EmptyResultException("Query returned no results. If this is expected, specify a return type of array, List, Optional, Page, Slice, or Stream for the repository method.");
        else
            throw new NonUniqueResultException("Found " + results.size() +
                                               " results. To limit to a single result, specify Limit.of(1) as a parameter or use the findFirstBy name pattern.");
    }

    /**
     * Parses and handles the text between delete___By of a repository method.
     * Currently this is only "First" or "First#".
     *
     * @param queryInfo partially complete query information to populate with a maxResults value for deleteFirst(#)By...
     * @param by        index of first occurrence of "By" in the method name. -1 if "By" is absent.
     */
    private void parseDeleteBy(QueryInfo queryInfo, int by) {
        String methodName = queryInfo.method.getName();
        if (methodName.regionMatches(6, "First", 0, 5)) {
            int endBefore = by == -1 ? methodName.length() : by;
            parseFirst(queryInfo, 11, endBefore);
        }
    }

    /**
     * Parses and handles the text between find___By of a repository method.
     * Currently this is only "First" or "First#" and entity property names to select.
     * "Distinct" is reserved for future use.
     * Entity property names can be included (delimited by "And" when there are multiple) to select only those results.
     *
     * @param queryInfo  partially complete query information to populate with a maxResults value for findFirst(#)By...
     * @param methodName the method name.
     * @param by         index of first occurrence of "By" in the method name. -1 if "By" is absent.
     * @param selections order list to which to add selections int the find...By. If null, do not look for selections.
     */
    private void parseFindBy(QueryInfo queryInfo, String methodName, int by, List<String> selections) {
        int start = 4;
        int endBefore = by == -1 ? methodName.length() : by;

        for (boolean first = methodName.regionMatches(start, "First", 0, 5), distinct = !first && methodName.regionMatches(start, "Distinct", 0, 8); //
                        first || distinct;)
            if (first) {
                start = parseFirst(queryInfo, start += 5, endBefore);
                first = false;
                distinct = methodName.regionMatches(start, "Distinct", 0, 8);
            } else if (distinct) {
                throw new DataException("The keyword Distinct is not supported on the " + queryInfo.method.getName() + " method."); // TODO NLS
            }

        if (selections != null) {
            List<String> notFound = new ArrayList<>();
            do {
                int and = methodName.indexOf("And", start);
                if (and == -1 || and > endBefore)
                    and = endBefore;

                if (start < and) {
                    String name = methodName.substring(start, and);
                    String attrName = queryInfo.entityInfo.getAttributeName(name, false);
                    if (attrName == null)
                        notFound.add(name);
                    else
                        selections.add(attrName);
                }

                start = and + 3;
            } while (start < endBefore);

            // Enforcement of missing names should only be done if the user is trying to specify
            // property selections vs including descriptive text in the method name.
            if (!selections.isEmpty() && !notFound.isEmpty())
                throw new MappingException("Entity class " + queryInfo.entityInfo.getType().getName() +
                                           " does not have properties named " + notFound +
                                           ". The following are valid property names for the entity: " +
                                           queryInfo.entityInfo.attributeTypes.keySet()); // TODO NLS
        }
    }

    /**
     * Parses the number (if any) following findFirst or deleteFirst.
     *
     * @param queryInfo partially complete query information to populate with a maxResults value for find/deleteFirst(#)By...
     * @param start     starting position after findFirst or deleteFirst
     * @param endBefore index of first occurrence of "By" in the method name, or otherwise the method name length.
     * @return next starting position after the find/deleteFirst(#).
     */
    private int parseFirst(QueryInfo queryInfo, int start, int endBefore) {
        String methodName = queryInfo.method.getName();
        int num = start == endBefore ? 1 : 0;
        if (num == 0)
            while (start < endBefore) {
                char ch = methodName.charAt(start);
                if (ch >= '0' && ch <= '9') {
                    if (num <= (Integer.MAX_VALUE - (ch - '0')) / 10)
                        num = num * 10 + (ch - '0');
                    else
                        throw new UnsupportedOperationException(methodName.substring(0, endBefore) + " exceeds Integer.MAX_VALUE (2147483647)."); // TODO
                    start++;
                } else {
                    if (num == 0)
                        num = 1;
                    break;
                }
            }
        if (num == 0)
            throw new DataException("The number of results to retrieve must not be 0 on the " + methodName + " method."); // TODO NLS
        else
            queryInfo.maxResults = num;

        return start;
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
     * Removes the entity (or record) from the database if its attributes match the database.
     *
     * @param e         the entity or record.
     * @param queryInfo query information that is prepopulated for deleteById.
     * @param em        the entity manager.
     * @return the number of entities deleted (1 or 0).
     * @throws Exception if an error occurs or if the repository method return type is void and
     *                       the entity (or correct version of the entity) was not found.
     */
    private int remove(Object e, QueryInfo queryInfo, EntityManager em) throws Exception {
        Class<?> entityClass = queryInfo.entityInfo.recordClass == null ? queryInfo.entityInfo.entityClass : queryInfo.entityInfo.recordClass;

        if (e == null)
            throw new NullPointerException("The entity parameter cannot have a null value."); // TODO NLS // required by spec

        if (!entityClass.isInstance(e))
            throw new DataException("The " + (e == null ? null : e.getClass().getName()) +
                                    " parameter does not match the " + entityClass.getName() +
                                    " entity type that is expected for this repository."); // TODO NLS

        EntityInfo entityInfo = queryInfo.entityInfo;
        String jpql = queryInfo.jpql;

        int versionParamIndex = (entityInfo.idClassAttributeAccessors == null ? 1 : entityInfo.idClassAttributeAccessors.size()) + 1;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = entityInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            id = entityInfo.getAttribute(e, entityInfo.getAttributeName("id", true));
            if (id == null) {
                jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
                if (version != null)
                    jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> delete = em.createQuery(jpql, entityInfo.entityClass);

        if (entityInfo.idClassAttributeAccessors == null) {
            int p = 1;
            if (id != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + id.getClass().getSimpleName());
                delete.setParameter(p++, id);
            }
            if (version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + version.getClass().getSimpleName());
                delete.setParameter(p, version);
            }
        } else {
            queryInfo.setParametersFromIdClassAndVersion(delete, e, version);
        }

        int numDeleted = delete.executeUpdate();

        if (numDeleted == 0) {
            Class<?> returnType = queryInfo.method.getReturnType();
            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                if (entityInfo.versionAttributeName == null)
                    throw new OptimisticLockingFailureException("Entity was not found."); // TODO NLS
                else
                    throw new OptimisticLockingFailureException("Version " + version + " of the entity was not found."); // TODO NLS
            }
        } else if (numDeleted > 1) {
            throw new DataException("Found " + numDeleted + " matching entities."); // ought to be unreachable
        }

        return numDeleted;
    }

    /**
     * Converts to the specified type, raising an error if the conversion cannot be made.
     *
     * @param type               type to convert to.
     * @param item               item to convert.
     * @param failIfNotConverted whether or not to fail if unable to convert the value.
     * @return new instance of the requested type.
     */
    private static final Object to(Class<?> type, Object item, boolean failIfNotConverted) {
        Object result = item;
        if (item == null) {
            if (type.isPrimitive())
                throw new NullPointerException(); // TODO NLS
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
        if (failIfNotConverted && result == item && item != null)
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

    /**
     * Converts a record to its generated entity equivalent,
     * or does nothing if not a record.
     *
     * @param o entity or a record that needs conversion to an entity.
     * @return entity.
     */
    @Trivial
    private static final Object toEntity(Object o) {
        Object entity = o;
        Class<?> oClass = o == null ? null : o.getClass();
        if (o != null && oClass.isRecord())
            try {
                final Object recordObj = o;
                entity = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                    Class<?> entityClass = oClass.getClassLoader().loadClass(oClass.getName() + "Entity");
                    Constructor<?> ctor = entityClass.getConstructor(oClass);
                    return ctor.newInstance(recordObj);
                });
            } catch (PrivilegedActionException x) {
                throw new MappingException("Unable to convert record " + oClass + " to generated entity class.", x.getCause()); // TODO NLS
            }
        if (entity != o && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "toEntity " + oClass.getName() + " --> " + entity.getClass().getName());
        return entity;
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

    /**
     * Convert the results list into an Iterable of the specified type.
     *
     * @param iterableType the desired type of Iterable.
     * @param elementType  the type of each element if a find operation. Can be NULL if a save operation.
     * @param results      results of a find or save operation.
     * @return results converted to an Iterable of the specified type.
     */
    @Trivial
    private static final Iterable<?> toIterable(Class<?> iterableType, Class<?> elementType, List<?> results) {
        if (Streamable.class.equals(iterableType))
            return new StreamableImpl<>(results);
        Collection<Object> list;
        if (iterableType.isInterface()) {
            if (iterableType.isAssignableFrom(ArrayList.class)) // covers Iterable, Collection, List
                list = new ArrayList<>(results.size());
            else if (iterableType.isAssignableFrom(ArrayDeque.class)) // covers Queue, Deque
                list = new ArrayDeque<>(results.size());
            else if (iterableType.isAssignableFrom(LinkedHashSet.class)) // covers Set
                list = new LinkedHashSet<>(results.size());
            else
                throw new UnsupportedOperationException(iterableType + " is an unsupported return type."); // TODO NLS
        } else {
            try {
                @SuppressWarnings("unchecked")
                Constructor<? extends Collection<Object>> c = (Constructor<? extends Collection<Object>>) iterableType.getConstructor();
                list = c.newInstance();
            } catch (NoSuchMethodException x) {
                throw new MappingException("The " + iterableType.getName() + " result type lacks a public zero parameter constructor.", x); // TODO NLS
            } catch (IllegalAccessException | InstantiationException x) {
                throw new MappingException("Unable to access the zero parameter constructor of the " + iterableType.getName() + " result type.", x); // TODO NLS
            } catch (InvocationTargetException x) {
                throw new MappingException("The constructor for the " + iterableType.getName() + " result type raised an error: " + x.getCause().getMessage(), x.getCause()); // TODO NLS
            }
        }
        if (results.size() == 1 && results.get(0) instanceof Object[]) {
            Object[] a = (Object[]) results.get(0);
            for (int i = 0; i < a.length; i++)
                list.add(elementType.isInstance(a[i]) ? a[i] : to(elementType, a[i], true));
        } else {
            list.addAll(results);
        }
        return list;
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
            result = CompletableFuture.completedFuture(toReturnValue(i, queryInfo.getSingleResultType(), null));
        else
            throw new UnsupportedOperationException("Return update count as " + returnType);

        return result;
    }

    /**
     * Saves entities (or records) to the database, which can involve an update or an insert,
     * depending on whether the entity already exists.
     *
     * @param arg       the entity or record, or array or Iterable or Stream of entity or record.
     * @param queryInfo query information.
     * @param em        the entity manager.
     * @return the updated entities, using the return type that is required by the repository Save method signature.
     * @throws Exception if an error occurs.
     */
    private Object save(Object arg, QueryInfo queryInfo, EntityManager em) throws Exception {
        Class<?> singleType = queryInfo.getSingleResultType();
        boolean resultVoid = void.class.equals(singleType) || Void.class.equals(singleType);
        List<Object> results;

        if (queryInfo.entityParamType.isArray()) {
            results = new ArrayList<>();
            int length = Array.getLength(arg);
            for (int i = 0; i < length; i++)
                results.add(em.merge(toEntity(Array.get(arg, i))));
            em.flush();
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            if (Iterable.class.isAssignableFrom(queryInfo.entityParamType)) {
                results = new ArrayList<>();
                for (Object e : ((Iterable<?>) arg))
                    results.add(em.merge(toEntity(e)));
                em.flush();
            } else {
                results = resultVoid ? null : new ArrayList<>(1);
                Object entity = em.merge(toEntity(arg));
                if (results != null)
                    results.add(entity);
                em.flush();
            }
        }

        Object returnValue;
        if (resultVoid) {
            returnValue = null;
        } else {
            if (queryInfo.entityInfo.recordClass != null)
                for (int i = 0; i < results.size(); i++)
                    results.set(i, queryInfo.entityInfo.toRecord(results.get(i)));

            if (queryInfo.returnArrayType != null) {
                Object[] newArray = (Object[]) Array.newInstance(queryInfo.returnArrayType, results.size());
                returnValue = results.toArray(newArray);
            } else {
                Class<?> multiType = queryInfo.getMultipleResultType();
                if (multiType == null)
                    returnValue = results.isEmpty() ? null : results.get(0); // TODO error if multiple results? Detect earlier?
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = toIterable(multiType, null, results);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw new UnsupportedOperationException(multiType + " is an unsupported return type."); // TODO NLS
            }
        }

        Class<?> returnType = queryInfo.method.getReturnType();
        if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw new MappingException("The " + returnType.getName() + " return type of the " +
                                       queryInfo.method.getName() + " method of the " +
                                       queryInfo.method.getDeclaringClass().getName() +
                                       " class is not a valid return type for a repository " +
                                       "@Save" + " method."); // TODO NLS
        }

        return returnValue;
    }

    /**
     * Updates the entity (or record) from the database if its attributes match the database.
     *
     * @param e         the entity or record.
     * @param queryInfo query information that is prepopulated for update by id (and possibly version).
     * @param em        the entity manager.
     * @return the number of entities updated (1 or 0).
     * @throws Exception if an error occurs.
     */
    private int update(Object e, QueryInfo queryInfo, EntityManager em) throws Exception {
        Class<?> entityClass = queryInfo.entityInfo.recordClass == null ? queryInfo.entityInfo.entityClass : queryInfo.entityInfo.recordClass;

        if (e == null)
            throw new NullPointerException("The entity parameter cannot have a null value."); // TODO NLS // required by spec

        if (!entityClass.isInstance(e))
            throw new DataException("The " + (e == null ? null : e.getClass().getName()) +
                                    " parameter does not match the " + entityClass.getName() +
                                    " entity type that is expected for this repository."); // TODO NLS

        String jpql = queryInfo.jpql;
        EntityInfo entityInfo = queryInfo.entityInfo;
        LinkedHashSet<String> attrsToUpdate = entityInfo.getAttributeNamesForEntityUpdate();

        int versionParamIndex = attrsToUpdate.size() + 2;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = entityInfo.getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = entityInfo.getAttribute(e, entityInfo.getAttributeName("id", true));
        if (id == null) {
            jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
            if (version != null)
                jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != queryInfo.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> update = em.createQuery(jpql, entityInfo.entityClass);

        // parameters for entity attributes to update:

        int p = 0;
        for (String attrName : entityInfo.getAttributeNamesForEntityUpdate())
            QueryInfo.setParameter(++p, update, e,
                                   entityInfo.attributeAccessors.get(attrName));

        // id parameter(s)

        if (entityInfo.idClassAttributeAccessors != null) {
            throw new UnsupportedOperationException(); // TODO
        } else if (id != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + id.getClass().getSimpleName());
            update.setParameter(++p, id);
        }

        // version parameter

        if (entityInfo.versionAttributeName != null && version != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "set ?" + (p + 1) + ' ' + version.getClass().getSimpleName());
            update.setParameter(++p, version);
        }

        int numUpdated = update.executeUpdate();

        if (numUpdated > 1)
            throw new DataException("Found " + numUpdated + " matching entities."); // ought to be unreachable
        return numUpdated;
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
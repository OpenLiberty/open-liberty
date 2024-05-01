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

import static jakarta.data.repository.By.ID;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.persistence.Query;

/**
 * Query information.
 */
public class QueryInfo {
    private static final TraceComponent tc = Tr.register(QueryInfo.class);

    public static enum Type {
        COUNT, DELETE, DELETE_WITH_ENTITY_PARAM, EXISTS, FIND, FIND_AND_DELETE, INSERT, SAVE, RESOURCE_ACCESS,
        UPDATE, UPDATE_WITH_ENTITY_PARAM, UPDATE_WITH_ENTITY_PARAM_AND_RESULT
    }

    /**
     * Return types for deleteBy that distinguish delete-only from find-and-delete.
     */
    private static final Set<Class<?>> RETURN_TYPES_FOR_DELETE_ONLY = Set.of(void.class, Void.class,
                                                                             boolean.class, Boolean.class,
                                                                             int.class, Integer.class,
                                                                             long.class, Long.class,
                                                                             Number.class);

    /**
     * Mapping of Java primitive class to wrapper class.
     */
    private static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = Map.of(boolean.class, Boolean.class,
                                                                          byte.class, Byte.class,
                                                                          char.class, Character.class,
                                                                          double.class, Double.class,
                                                                          float.class, Float.class,
                                                                          int.class, Integer.class,
                                                                          long.class, Long.class,
                                                                          short.class, Short.class,
                                                                          void.class, Void.class);

    /**
     * Information about the type of entity to which the query pertains.
     */
    EntityInfo entityInfo;

    /**
     * Type of the first parameter if a life cycle method, otherwise null.
     */
    final Class<?> entityParamType;

    /**
     * Entity identifier variable name if an identifier variable is used.
     * Otherwise "*". "o" is used as the default in generated queries.
     */
    String entityVar = "o";

    /**
     * Entity identifier variable name and . character if an identifier variable is used.
     * Otherwise the empty string. "o." is used as the default in generated queries.
     */
    String entityVar_ = "o.";

    /**
     * Indicates if the query has a WHERE clause.
     * This is accurate only for generated or partially provided queries.
     */
    boolean hasWhere;

    /**
     * JPQL for the query. Null if a save operation.
     */
    String jpql;

    /**
     * JPQL for a find query after a keyset. Otherwise null.
     */
    String jpqlAfterKeyset;

    /**
     * JPQL for a find query before a keyset. Otherwise null.
     */
    String jpqlBeforeKeyset;

    /**
     * For counting the total number of results across all pages.
     * Null if pagination is not used or only slices are used.
     */
    String jpqlCount;

    /**
     * For deleting an entry when using the find-and-delete pattern
     * where a delete query returns the deleted entity.
     */
    String jpqlDelete;

    /**
     * Value from findFirst#By, or 1 for findFirstBy, otherwise 0.
     */
    int maxResults;

    /**
     * Repository method to which this query information pertains.
     */
    public final Method method;

    /**
     * Number of parameters to the JPQL query.
     */
    int paramCount;

    /**
     * Difference between the number of parameters to the JPQL query and the expected number of
     * corresponding parameters on the repository method signature. If the entity has an IdClass
     * and the repository method queries on Id, it will have only a single parameter for the user
     * to input, whereas the JPQL will have additional parameters for each additional attribute
     * of the IdClass.
     */
    int paramAddedCount;

    /**
     * Names that are specified by the <code>Param</code> annotation for each query parameter.
     * An empty list is a marker that named parameters are present, but need to be populated into the list.
     * Population is deferred to ensure the order of the list matches the order of parameters in the method signature.
     * A null value indicates positional parameters (?1, ?2, ...) are used rather than named parameters
     * or there are no parameters at all.
     */
    List<String> paramNames;

    /**
     * Array element type if the repository method returns an array, such as,
     * <code>Product[] findByNameLike(String namePattern);</code>
     * or if its parameterized type is an array, such as,
     * <code>CompletableFuture&lt;Product[]&gt; findByNameLike(String namePattern);</code>
     * Otherwise null.
     */
    final Class<?> returnArrayType;

    /**
     * Return type of the repository method return value,
     * split into levels of depth for each type parameter and array component.
     * This is useful in cases such as
     * <code>&#64;Query(...) Optional&lt;Float&gt; priceOf(String productId)</code>
     * which resolves to { Optional.class, Float.class }
     * and
     * <code>CompletableFuture&lt;Stream&lt;Product&gt&gt; findByNameLike(String namePattern)</code>
     * which resolves to { CompletableFuture.class, Stream.class, Product.class }
     * and
     * <code>CompletionStage&lt;Product[]&gt; findByNameIgnoreCaseLike(String namePattern)</code>
     * which resolves to { CompletionStage.class, Product[].class, Product.class }
     */
    final List<Class<?>> returnTypeAtDepth;

    /**
     * Ordered list of Sort criteria, which can be defined statically via the OrderBy annotation or keyword,
     * or dynamically via PageRequest Sort parameters or Sort parameters to the repository method,
     * or a combination of both static and dynamic.
     * If the Query annotation is used, it will be unknown whether its value hard-codes Sort criteria,
     * in which case this field gets set to any additional sort criteria that is added statically or dynamically,
     * or lacking either of those, an empty list.
     * If none of the above, the value of this field is null, which can also mean it has not been initialized yet.
     */
    List<Sort<Object>> sorts;

    /**
     * Categorization of query type.
     */
    Type type;

    /**
     * Indicates whether or not to validate method parameters, if Jakarta Validation is available.
     */
    boolean validateParams;

    /**
     * Indicates whether or not to validate method results, if Jakarta Validation is available.
     */
    boolean validateResult;

    /**
     * Construct partially complete query information.
     */
    public QueryInfo(Method method, Class<?> entityParamType, Class<?> returnArrayType, List<Class<?>> returnTypeAtDepth) {
        this.method = method;
        this.entityParamType = entityParamType;
        this.returnArrayType = returnArrayType;
        this.returnTypeAtDepth = returnTypeAtDepth;
    }

    /**
     * Construct partially complete query information.
     */
    public QueryInfo(Method method, Type type) {
        this.method = method;
        this.entityParamType = null;
        this.returnArrayType = null;
        this.returnTypeAtDepth = null;
        this.type = type;
    }

    /**
     * Adds Sort criteria to the end of the tracked list of sort criteria.
     * For IdClass, adds all Id properties separately.
     *
     * @param ignoreCase if ordering is to be independent of case.
     * @param attribute  name of attribute (@OrderBy value or Sort property or parsed from OrderBy query-by-method).
     * @param descending if ordering is to be in descending order
     */
    @Trivial
    void addSort(boolean ignoreCase, String attribute, boolean descending) {
        Set<String> names = entityInfo.idClassAttributeAccessors != null && ID.equalsIgnoreCase(attribute) //
                        ? entityInfo.idClassAttributeAccessors.keySet() //
                        : Set.of(attribute);

        for (String name : names) {
            name = entityInfo.getAttributeName(name, true);

            sorts.add(ignoreCase ? //
                            descending ? //
                                            Sort.descIgnoreCase(name) : //
                                            Sort.ascIgnoreCase(name) : //
                            descending ? //
                                            Sort.desc(name) : //
                                            Sort.asc(name));
        }
    }

    /**
     * Temporary code to append the portion of the query language ql starting from startAt
     * where the entity identify variable is inserted before references to entity attributes.
     * This method does not cover all scenarios but should be sufficient for simulating.
     * TODO remove this method once we have Jakarta Persistence 3.2.
     *
     * @param ql        Jakarta Data Query Language
     * @param startAt   position in query language to start at.
     * @param endBefore position in query language before which to end.
     * @param q         simulated JPQL to which to append.
     * @return simulated JPQL.
     */
    private StringBuilder appendWithIdentifierName(String ql, int startAt, int endBefore, StringBuilder q) {
        boolean isLiteral = false;
        boolean isNamedParamOrEmbedded = false;
        for (int i = startAt; i < endBefore; i++) {
            char ch = ql.charAt(i);
            if (!isLiteral && (ch == ':' || ch == '.')) {
                q.append(ch);
                isNamedParamOrEmbedded = true;
            } else if (ch == '\'') {
                q.append(ch);
                if (isLiteral) {
                    if (i + 1 < endBefore && ql.charAt(i + 1) == '\'') {
                        // escaped ' within a literal
                        q.append('\'');
                        i++;
                    } else {
                        isLiteral = false;
                    }
                } else {
                    isLiteral = true;
                    isNamedParamOrEmbedded = false;
                }
            } else if (Character.isLetter(ch)) {
                if (isNamedParamOrEmbedded || isLiteral) {
                    q.append(ch);
                } else {
                    StringBuilder s = new StringBuilder();
                    s.append(ch);
                    for (int j = i + 1; j < endBefore; j++) {
                        ch = ql.charAt(j);
                        if (Character.isJavaIdentifierPart(ch))
                            s.append(ch);
                        else
                            break;
                    }
                    i += s.length();
                    String str = s.toString();
                    i--; // adjust for separate loop increment

                    if ("id".equalsIgnoreCase(str) && ql.regionMatches(true, i + 1, "(THIS)", 0, 6)) {
                        q.append(entityVar_).append(entityInfo.getAttributeName(By.ID, true));
                        i += 6;
                    } else if ("this".equalsIgnoreCase(str)) {
                        q.append(entityVar);
                    } else if (entityInfo.getAttributeName(str, false) == null) {
                        q.append(str);
                    } else {
                        q.append(entityVar_).append(str);
                    }
                }
            } else if (Character.isDigit(ch)) {
                q.append(ch);
            } else {
                q.append(ch);
                if (!isLiteral)
                    isNamedParamOrEmbedded = false;
            }
        }

        return q;
    }

    /**
     * Adds dynamically specified Sort criteria from the PageRequest to the end of an existing list, or
     * if the combined list Sort criteria doesn't already exist, this method creates it
     * starting with the Sort criteria of this QueryInfo.
     *
     * Obtains and processes sort criteria from pagination information.
     *
     * @param combined   existing list of sorts, or otherwise null.
     * @param additional list to add from.
     * @return the combined list that the sort criteria was added to.
     */
    @Trivial
    List<Sort<Object>> combineSorts(List<Sort<Object>> combined, Iterable<Sort<Object>> additional) {
        Iterator<Sort<Object>> addIt = additional.iterator();
        boolean hasIdClass = entityInfo.idClassAttributeAccessors != null;
        if (combined == null && addIt.hasNext())
            combined = sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
        while (addIt.hasNext()) {
            Sort<Object> sort = addIt.next();
            if (sort == null)
                throw new IllegalArgumentException("Sort: null");
            else if (hasIdClass && ID.equals(sort.property()))
                for (String name : entityInfo.idClassAttributeAccessors.keySet())
                    combined.add(entityInfo.getWithAttributeName(entityInfo.getAttributeName(name, true), sort));
            else
                combined.add(entityInfo.getWithAttributeName(sort.property(), sort));
        }
        return combined;
    }

    /**
     * Adds dynamically specified Sort criteria to the end of an existing list, or
     * if the combined list of Sort criteria doesn't already exist, this method creates it
     * starting with the Sort criteria of this QueryInfo.
     *
     * @param combined   existing list of sorts, or otherwise null.
     * @param additional list to add from.
     * @return the combined list that the sort criteria was added to.
     */
    @Trivial
    List<Sort<Object>> combineSorts(List<Sort<Object>> combined, @SuppressWarnings("unchecked") Sort<Object>... additional) {
        boolean hasIdClass = entityInfo.idClassAttributeAccessors != null;
        if (combined == null && additional.length > 0)
            combined = sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
        for (Sort<Object> sort : additional) {
            if (sort == null)
                throw new IllegalArgumentException("Sort: null");
            else if (hasIdClass && ID.equals(sort.property()))
                for (String name : entityInfo.idClassAttributeAccessors.keySet())
                    combined.add(entityInfo.getWithAttributeName(entityInfo.getAttributeName(name, true), sort));
            else
                combined.add(entityInfo.getWithAttributeName(sort.property(), sort));
        }
        return combined;
    }

    /**
     * Locate the entity information for the specified result class.
     *
     * @param entityType              single result type of a repository method, which is hopefully an entity class.
     * @param entityInfos             map of entity name to already-completed future for the entity information.
     * @param primaryEntityInfoFuture future for the repository's primary entity type if it has one, otherwise null.
     * @return entity information.
     * @throws MappingException if the entity information is not found.
     */
    @Trivial
    EntityInfo getEntityInfo(Class<?> entityType, Map<String, CompletableFuture<EntityInfo>> entityInfos,
                             CompletableFuture<EntityInfo> primaryEntityInfoFuture) {
        if (entityType != null) {
            CompletableFuture<EntityInfo> failedFuture = null;
            for (CompletableFuture<EntityInfo> future : entityInfos.values())
                if (future.isCompletedExceptionally()) {
                    failedFuture = future;
                } else {
                    EntityInfo info = future.join();
                    if (entityType.equals(info.entityClass))
                        return info;
                }
            if (failedFuture != null)
                failedFuture.join(); // cause error to be raised
        }
        if (primaryEntityInfoFuture == null)
            throw new MappingException("The " + method.getName() + " method of the " + method.getDeclaringClass().getName() +
                                       " repository does not specify an entity class. To correct this, have the repository interface" +
                                       " extend DataRepository or another built-in repository interface and supply the entity class" +
                                       " as the first type variable."); // TODO NLS

        if (!primaryEntityInfoFuture.isDone() && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "await completion of primary entity info", primaryEntityInfoFuture);

        return primaryEntityInfoFuture.join();
    }

    /**
     * Locate the entity information for the specified entity name.
     *
     * @param entityName  case sensitive entity name obtained from JDQL or JPQL.
     * @param entityInfos map of entity name to already-completed future for the entity information.
     * @return entity information.
     * @throws MappingException if the entity information is not found.
     */
    @Trivial
    EntityInfo getEntityInfo(String entityName, Map<String, CompletableFuture<EntityInfo>> entityInfos) {
        CompletableFuture<EntityInfo> future = entityInfos.get(entityName);
        if (future == null) {
            // Identify possible case mismatch
            for (String name : entityInfos.keySet())
                if (entityName.equalsIgnoreCase(name))
                    throw new MappingException("The " + method.getName() + " method of the " + method.getDeclaringClass().getName() +
                                               " repository specifies query language that requires a " + entityName +
                                               " entity that is not found but is a close match for the " + name +
                                               " entity. Review the query language to ensure the correct entity name is used."); // TODO NLS

            future = entityInfos.get(EntityInfo.FAILED);
            if (future == null)
                throw new MappingException("The " + method.getName() + " method of the " + method.getDeclaringClass().getName() +
                                           " repository specifies query language that requires a " + entityName +
                                           " entity that is not found. Check if " + entityName + " is the name of a valid entity." +
                                           " To enable the entity to be found, give the repository a life cycle method that is" +
                                           " annotated with one of " + "(Insert, Save, Update, Delete)" +
                                           " and supply the entity as its parameter or have the repository extend" +
                                           " DataRepository or another built-in repository interface with the entity class as the" +
                                           " first type variable."); // TODO NLS
        }
        return future.join();
    }

    /**
     * Obtains keyset cursor values for the specified entity.
     *
     * @param entity the entity.
     * @return keyset cursor values, ordering according to the sort criteria.
     */
    @Trivial
    Object[] getKeysetValues(Object entity) {
        if (!entityInfo.getType().isInstance(entity))
            throw new MappingException("Unable to obtain keyset values from the " +
                                       (entity == null ? null : entity.getClass().getName()) +
                                       " type query result. Queries that use keyset pagination must return results of the same type as the entity type, which is " +
                                       entityInfo.getType().getName() + "."); // TODO NLS
        ArrayList<Object> keyValues = new ArrayList<>();
        for (Sort<?> keyInfo : sorts)
            try {
                List<Member> accessors = entityInfo.attributeAccessors.get(keyInfo.property());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "getKeysetValues for " + entity, accessors);
                Object value = entity;
                for (Member accessor : accessors)
                    if (accessor instanceof Method)
                        value = ((Method) accessor).invoke(value);
                    else
                        value = ((Field) accessor).get(value);
                keyValues.add(value);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x instanceof InvocationTargetException ? x.getCause() : x);
            }
        return keyValues.toArray();
    }

    /**
     * @return returns the type of data structure that returns multiple results for this query.
     *         Null if the query return type limits to single results.
     */
    @Trivial
    Class<?> getMultipleResultType() {
        Class<?> type = null;
        int depth = returnTypeAtDepth.size();
        for (int d = 0; d < depth - 1 && type == null; d++) {
            type = returnTypeAtDepth.get(d);
            if (Optional.class.equals(type) || CompletionStage.class.equals(type) || CompletableFuture.class.equals(type))
                type = null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getMultipleResultType: " + (type == null ? null : type.getName()));
        return type;
    }

    /**
     * @return returns the type that is returned by the repository method as an Optional<Type>.
     *         Null if the repository method result type does not include Optional.
     */
    @Trivial
    Class<?> getOptionalResultType() {
        Class<?> type = null;
        int depth = returnTypeAtDepth.size();
        for (int d = 0; d < depth - 1; d++) {
            type = returnTypeAtDepth.get(d);
            if (Optional.class.equals(type)) {
                type = returnTypeAtDepth.get(d + 1);
                break;
            } else {
                type = null;
                if (!CompletionStage.class.equals(type) || !CompletableFuture.class.equals(type))
                    break;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getOptionalResultType: " + (type == null ? null : type.getName()));
        return type;
    }

    /**
     * @return returns the type of a single result obtained by the query.
     *         For example, a single result of a query that returns List<MyEntity> is of type MyEntity.
     */
    @Trivial
    Class<?> getSingleResultType() {
        Class<?> type = returnTypeAtDepth.get(returnTypeAtDepth.size() - 1);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getSingleResultType: " + (type == null ? null : type.getName()));
        return type;
    }

    /**
     * Identifies whether sort criteria can be dynamically supplied when invoking the query.
     *
     * @return true if it is possible to provide sort criteria dynamically, otherwise false.
     */
    @Trivial
    boolean hasDynamicSortCriteria() {
        boolean hasDynamicSort = false;
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = paramCount - paramAddedCount; i < paramTypes.length && !hasDynamicSort; i++)
            hasDynamicSort = PageRequest.class.equals(paramTypes[i])
                             || Order.class.equals(paramTypes[i])
                             || Sort[].class.equals(paramTypes[i])
                             || Sort.class.equals(paramTypes[i]);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "hasDynamicSortCriteria? " + hasDynamicSort);
        return hasDynamicSort;
    }

    /**
     * Determine if the index of the text ignoring case if it is the next non-whitespace characters.
     *
     * @parma text the text to match.
     * @param ql      query language.
     * @param startAt starting position in the query language string.
     * @return position of the text ignoring case if it is the next non-whitespace characters. Otherwise -1;
     */
    private static int indexOfAfterWhitespace(String text, String ql, int startAt) {
        int length = ql.length();
        while (startAt < length && Character.isWhitespace(ql.charAt(startAt)))
            startAt++;
        return ql.regionMatches(true, startAt, text, 0, 2) ? startAt : -1;
    }

    /**
     * Initialize this query information for the specified type of annotated repository operation.
     *
     * @param annoClass     Insert, Update, Save, or Delete annotation class.
     * @param operationType corresponding operation type.
     */
    void init(Class<? extends Annotation> annoClass, Type operationType) {
        type = operationType;
        if (entityParamType == null)
            throw new UnsupportedOperationException("Repository " + '@' + annoClass.getSimpleName() +
                                                    " operations must have exactly 1 parameter, which can be the entity" +
                                                    " or a collection or array of entities. The " + method.getDeclaringClass().getName() +
                                                    '.' + method.getName() + " method has " + method.getParameterCount() + " parameters."); // TODO NLS
    }

    /**
     * Initializes query information based on the Query annotation.
     *
     * @param ql                      Query.value() might be JPQL or JDQL
     * @param multiType               the type of data structure that returns multiple results for this query. Otherwise null.
     * @param entityInfos             map of entity name to entity information.
     * @param primaryEntityInfoFuture future for the repository's primary entity type if it has one, otherwise null.
     */
    void initForQuery(String ql, Class<?> multiType, Map<String, CompletableFuture<EntityInfo>> entityInfos,
                      CompletableFuture<EntityInfo> primaryEntityInfoFuture) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        boolean isCursoredPage = CursoredPage.class.equals(multiType);
        boolean countPages = isCursoredPage || Page.class.equals(multiType);

        int length = ql.length();
        int startAt = 0;
        char firstChar = ' ';
        for (; startAt < length && Character.isWhitespace(firstChar = ql.charAt(startAt)); startAt++);

        if (firstChar == 'D' || firstChar == 'd') { // DELETE FROM EntityName[ WHERE ...]
            // Temporarily simulate optional identifier names by inserting them.
            // TODO remove when switched to Jakarta Persistence 3.2.
            if (startAt + 12 < length
                && ql.regionMatches(true, startAt + 1, "ELETE", 0, 5)
                && Character.isWhitespace(ql.charAt(startAt + 6))) {
                type = Type.DELETE;
                jpql = ql;
                startAt += 7; // start of FROM
                for (; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                if (startAt + 6 < length
                    && ql.regionMatches(true, startAt, "FROM", 0, 4)
                    && Character.isWhitespace(ql.charAt(startAt + 4))) {
                    startAt += 5; // start of EntityName
                    for (; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                    StringBuilder entityName = new StringBuilder();
                    for (char ch; startAt < length && Character.isJavaIdentifierPart(ch = ql.charAt(startAt)); startAt++)
                        entityName.append(ch);
                    if (entityName.length() > 0) {
                        entityInfo = getEntityInfo(entityName.toString(), entityInfos);
                        // skip whitespace
                        for (; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                        if (startAt >= length) {
                            // Entity identifier variable is not present. Add it.
                            entityVar = "o";
                            entityVar_ = "o.";
                            jpql = new StringBuilder(entityName.length() + 14) //
                                            .append("DELETE FROM ").append(entityName).append(" o").toString();
                        } else if (startAt + 6 < length
                                   && ql.regionMatches(true, startAt, "WHERE", 0, 5)
                                   && !Character.isJavaIdentifierPart(ql.charAt(startAt + 5))) {
                            hasWhere = true;
                            entityVar = "o";
                            entityVar_ = "o.";
                            StringBuilder q = new StringBuilder(ql.length() * 3 / 2) //
                                            .append("DELETE FROM ").append(entityName).append(" o WHERE");
                            jpql = appendWithIdentifierName(ql, startAt + 5, ql.length(), q).toString();
                        }
                    }
                }
            }
        } else if (firstChar == 'U' || firstChar == 'u') { // UPDATE EntityName[ SET ... WHERE ...]
            // Temporarily simulate optional identifier names by inserting them.
            // TODO remove when switched to Jakarta Persistence 3.2.
            if (startAt + 13 < length
                && ql.regionMatches(true, startAt + 1, "PDATE", 0, 5)
                && Character.isWhitespace(ql.charAt(startAt + 6))) {
                type = Type.UPDATE;
                jpql = ql;
                startAt += 7; // start of EntityName
                for (; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                StringBuilder entityName = new StringBuilder();
                for (char ch; startAt < length && Character.isJavaIdentifierPart(ch = ql.charAt(startAt)); startAt++)
                    entityName.append(ch);
                if (entityName.length() > 0)
                    entityInfo = getEntityInfo(entityName.toString(), entityInfos);
                else if (entityInfo == null)
                    throw new MappingException("@Repository " + method.getDeclaringClass().getName() + " does not specify an entity class." + // TODO NLS
                                               " To correct this, have the repository interface extend DataRepository" +
                                               " or another built-in repository interface and supply the entity class as the first parameter.");
                if (startAt + 1 < length && entityName.length() > 0 && Character.isWhitespace(ql.charAt(startAt))) {
                    for (startAt++; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                    if (startAt + 4 < length
                        && ql.regionMatches(true, startAt, "SET", 0, 3)
                        && !Character.isJavaIdentifierPart(ql.charAt(startAt + 3))) {
                        entityVar = "o";
                        entityVar_ = "o.";
                        StringBuilder q = new StringBuilder(ql.length() * 3 / 2) //
                                        .append("UPDATE ").append(entityName).append(" o SET");
                        jpql = appendWithIdentifierName(ql, startAt + 3, ql.length(), q).toString();
                    }
                }
            }
        } else { // SELECT ... or FROM ... or WHERE ... or ORDER BY ...
            int select0 = -1, selectLen = 0; // starts after SELECT
            int from0 = -1, fromLen = 0; // starts after FROM
            int where0 = -1, whereLen = 0; // starts after WHERE
            int order0 = -1, orderLen = 0; // starts at ORDER BY

            if (length > startAt + 6
                && ql.regionMatches(true, startAt, "SELECT", 0, 6)
                && !Character.isJavaIdentifierPart(ql.charAt(startAt + 6))) {
                select0 = startAt += 6;
                // The end of the SELECT clause is a FROM, WHERE, GROUP BY, HAVING, or ORDER BY clause, or the end of the query
            }

            boolean isLiteral = false;
            boolean isNamedParamOrEmbedded = false;
            for (; startAt < length; startAt++) {
                char ch = ql.charAt(startAt);
                if (!isLiteral && (ch == ':' || ch == '.')) {
                    isNamedParamOrEmbedded = true;
                } else if (ch == '\'') {
                    if (isLiteral) {
                        if (startAt + 1 < length && ql.charAt(startAt + 1) == '\'')
                            startAt++; // escaped ' within a literal
                        else
                            isLiteral = false;
                    } else {
                        isLiteral = true;
                        isNamedParamOrEmbedded = false;
                    }
                } else if (Character.isLetter(ch)) {
                    if (!isNamedParamOrEmbedded && !isLiteral) {
                        int by;
                        if (from0 < 0 && where0 < 0 && length > startAt + 4
                            && ql.regionMatches(true, startAt, "FROM", 0, 4)
                            && !Character.isJavaIdentifierPart(ql.charAt(startAt + 4))) {
                            if (select0 >= 0 && selectLen == 0)
                                selectLen = startAt - select0;
                            from0 = startAt + 4;
                            startAt = from0 - 1; // -1 to allow for loop increment
                        } else if (length > startAt + 5
                                   && ql.regionMatches(true, startAt, "WHERE", 0, 5)
                                   && !Character.isJavaIdentifierPart(ql.charAt(startAt + 5))) {
                            if (select0 >= 0 && selectLen == 0)
                                selectLen = startAt - select0;
                            else if (from0 >= 0 && fromLen == 0)
                                fromLen = startAt - from0;
                            where0 = startAt + 5;
                            startAt = where0 - 1; // -1 to allow for loop increment
                            whereLen = 0;
                        } else if (length > startAt + 8
                                   && ql.regionMatches(true, startAt, "GROUP", 0, 5)
                                   && (by = indexOfAfterWhitespace("BY", ql, startAt + 5)) > 0) {
                            if (select0 >= 0 && selectLen == 0)
                                selectLen = startAt - select0;
                            else if (from0 >= 0 && fromLen == 0)
                                fromLen = startAt - from0;
                            else if (where0 >= 0 && whereLen == 0)
                                whereLen = startAt - where0;
                            startAt = by + 2 - 1; // -1 to allow for loop increment
                        } else if (length > startAt + 6
                                   && ql.regionMatches(true, startAt, "HAVING", 0, 6)
                                   && !Character.isJavaIdentifierPart(ql.charAt(startAt + 6))) {
                            if (select0 >= 0 && selectLen == 0)
                                selectLen = startAt - select0;
                            else if (from0 >= 0 && fromLen == 0)
                                fromLen = startAt - from0;
                            else if (where0 >= 0 && whereLen == 0)
                                whereLen = startAt - where0;
                            startAt += 6 - 1; // -1 to allow for loop increment
                        } else if (length > startAt + 8
                                   && ql.regionMatches(true, startAt, "ORDER", 0, 5)
                                   && (by = indexOfAfterWhitespace("BY", ql, startAt + 5)) > 0) {
                            if (select0 >= 0 && selectLen == 0)
                                selectLen = startAt - select0;
                            else if (from0 >= 0 && fromLen == 0)
                                fromLen = startAt - from0;
                            else if (where0 >= 0 && whereLen == 0)
                                whereLen = startAt - where0;
                            order0 = startAt; // include the ORDER BY unlike the other clauses
                            startAt = by + 2 - 1; // -1 to allow for loop increment
                        } else {
                            while (length > startAt + 1 && Character.isJavaIdentifierPart(ql.charAt(startAt + 1)))
                                startAt++;
                        }
                    }
                } else if (Character.isDigit(ch)) {
                } else if (!isLiteral) {
                    isNamedParamOrEmbedded = false;
                }
            }

            if (select0 >= 0 && selectLen == 0)
                selectLen = length - select0;
            else if (from0 >= 0 && fromLen == 0)
                fromLen = length - from0;
            else if (where0 >= 0 && whereLen == 0)
                whereLen = length - where0;
            else if (order0 >= 0 && orderLen == 0)
                orderLen = length - order0;

            type = Type.FIND;
            entityVar = "this";
            entityVar_ = "";
            hasWhere = whereLen > 0;

            // Locate the entity identifier variable (if present). Examples of FROM clause:
            // FROM EntityName
            // FROM EntityName e
            // FROM EntityName AS e
            for (startAt = from0; startAt < from0 + fromLen && Character.isWhitespace(ql.charAt(startAt)); startAt++);
            if (startAt < from0 + fromLen) {
                int entityName0 = startAt, entityNameLen = 0; // starts at EntityName
                for (; startAt < from0 + fromLen && Character.isJavaIdentifierPart(ql.charAt(startAt)); startAt++);
                if ((entityNameLen = startAt - entityName0) > 0) {
                    String entityName = ql.substring(entityName0, entityName0 + entityNameLen);
                    entityInfo = getEntityInfo(entityName, entityInfos);

                    for (; startAt < from0 + fromLen && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                    if (startAt < from0 + fromLen) {
                        int idVar0 = startAt, idVarLen = 0; // starts at the entity identifier variable
                        for (; startAt < from0 + fromLen && Character.isJavaIdentifierPart(ql.charAt(startAt)); startAt++);
                        if ((idVarLen = startAt - idVar0) > 0) {
                            if (idVarLen == 2
                                && (ql.charAt(idVar0) == 'A' || ql.charAt(idVar0) == 'a')
                                && (ql.charAt(idVar0 + 1) == 'S' || ql.charAt(idVar0 + 1) == 's')) {
                                // skip over the AS keyword
                                for (; startAt < from0 + fromLen && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                                idVar0 = startAt;
                                for (; startAt < from0 + fromLen && Character.isJavaIdentifierPart(ql.charAt(startAt)); startAt++);
                            }
                            if (startAt > idVar0) {
                                entityVar = ql.substring(idVar0, startAt);
                                entityVar_ = entityVar + '.';
                            }
                        }
                    }
                }
            }

            if (entityInfo == null)
                entityInfo = getEntityInfo(getSingleResultType(), entityInfos, primaryEntityInfoFuture);

            String entityName = entityInfo.name;

            if (trace && tc.isDebugEnabled()) {
                Tr.debug(tc, ql, "JDQL query parts", // does not include GROUP BY, HAVING, or address subqueries or other complex JPQL
                         "  SELECT [" + (selectLen > 0 ? ql.substring(select0, select0 + selectLen) : "") + "]",
                         "    FROM [" + (fromLen > 0 ? ql.substring(from0, from0 + fromLen) : "") + "]",
                         "   WHERE [" + (whereLen > 0 ? ql.substring(where0, where0 + whereLen) : "") + "]",
                         "  [" + (orderLen > 0 ? ql.substring(order0, order0 + orderLen) : "") + "]",
                         "  entity [" + entityName + "] [" + entityVar + "]");
            }

            // TODO remove this once we have JPA 3.2
            boolean lacksEntityVar;
            if (lacksEntityVar = "this".equals(entityVar)) {
                entityVar = "o";
                entityVar_ = "o.";
            }

            if (countPages) {
                // TODO count query cannot always be accurately inferred if Query value is JPQL
                StringBuilder c = new StringBuilder("SELECT COUNT(");
                if (lacksEntityVar
                    || selectLen <= 0
                    || ql.substring(select0, select0 + selectLen).indexOf(',') >= 0) // comma delimited multiple return values
                    c.append(entityVar);
                else // allows for COUNT(DISTINCT o.name)
                    appendWithIdentifierName(ql, select0, select0 + selectLen, c);

                c.append(") FROM");
                if (from0 >= 0 && !lacksEntityVar)
                    c.append(ql.substring(from0, from0 + fromLen));
                else
                    c.append(' ').append(entityName).append(' ').append(entityVar).append(' ');

                if (whereLen > 0) {
                    c.append("WHERE");
                    appendWithIdentifierName(ql, where0, where0 + whereLen, c);
                }

                jpqlCount = c.toString();
            }

            if (isCursoredPage) {
                if (order0 >= 0)
                    throw new UnsupportedOperationException("The " + ql + " query that is supplied to the " + method.getName() +
                                                            " method of the " + method.getDeclaringClass().getName() +
                                                            " repository cannot include an ORDER BY clause because" +
                                                            " the method returns a " + "CursoredPage" + ". Remove the ORDER BY" +
                                                            " clause and instead use the " + "OrderBy" +
                                                            " annotation to specify static sort criteria."); // TODO NLS

                if (where0 + whereLen != length)
                    throw new UnsupportedOperationException("The " + ql + " query that is supplied to the " + method.getName() +
                                                            " method of the " + method.getDeclaringClass().getName() +
                                                            " repository must end in a WHERE clause because" +
                                                            " the method returns a " + "CursoredPage" + ". There WHERE clause" +
                                                            " ends at position " + (where0 + whereLen) + " but the length of the" +
                                                            " query is " + length + "."); // TODO NLS

                // Enclose the WHERE clause in parenthesis so that conditions can be appended.
                boolean addSpace = ql.charAt(where0) != ' ';
                ql = new StringBuilder(ql.length() + 2) //
                                .append(ql.substring(0, where0)) //
                                .append(" (") //
                                .append(ql.substring(where0 + (addSpace ? 0 : 1), where0 + whereLen)) //
                                .append(")") //
                                .toString();
                whereLen += 2 + (addSpace ? 1 : 0);
            }

            StringBuilder q = new StringBuilder(ql.length() + (selectLen >= 0 ? 0 : 50) + (fromLen >= 0 ? 0 : 50) + 2);
            q.append("SELECT");
            if (selectLen > 0) {
                appendWithIdentifierName(ql, select0, select0 + selectLen, q);
                if (fromLen == 0 && whereLen == 0 && orderLen == 0)
                    q.append(' ');
            } else {
                q.append(' ').append(entityVar).append(' ');
            }

            q.append("FROM");
            if (fromLen > 0 && !lacksEntityVar)
                q.append(ql.substring(from0, from0 + fromLen));
            else
                q.append(' ').append(entityName).append(' ').append(entityVar).append(' ');

            if (whereLen > 0) {
                q.append("WHERE");
                appendWithIdentifierName(ql, where0, where0 + whereLen, q);
            }

            if (orderLen > 0) {
                appendWithIdentifierName(ql, order0, order0 + orderLen, q);
            }

            jpql = q.toString();
        }
    }

    /**
     * Determines whether a delete operation is find-and-delete (true) or delete only (false).
     * The determination is made based on the return type, with multiple and Optional results
     * indicating find-and-delete, and void or singular results that are boolean or a numeric
     * type compatible with an update count indicating delete only. Singular results that are
     * the entity type, record type, or id type other than the delete-only types indicate
     * find-and-delete.
     *
     * @return true if the return type is void or is the type of an update count.
     * @throws MappingException if the repository method return type is incompatible with both
     *                              delete-only and find-and-delete.
     */
    @Trivial
    boolean isFindAndDelete() {
        boolean isFindAndDelete = true;

        boolean isMultiple, isOptional;
        int d;
        Class<?> type = returnTypeAtDepth.get(d = 0);
        if (CompletionStage.class.equals(type) || CompletableFuture.class.equals(type))
            type = returnTypeAtDepth.get(++d);
        if (isOptional = Optional.class.equals(type))
            type = returnTypeAtDepth.get(++d);
        if (isMultiple = d < returnTypeAtDepth.size() - 1)
            type = returnTypeAtDepth.get(++d);

        isFindAndDelete = isOptional || isMultiple || !RETURN_TYPES_FOR_DELETE_ONLY.contains(type);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isFindAndDelete? " + isFindAndDelete + " isOptional? " + isOptional + " isMultiple? " + isMultiple +
                               " type: " + (type == null ? null : type.getName()));

        if (isFindAndDelete) {
            if (type != null
                && !type.equals(entityInfo.entityClass)
                && !type.equals(entityInfo.recordClass)
                && !type.equals(Object.class)
                && !wrapperClassIfPrimitive(type).equals(wrapperClassIfPrimitive(entityInfo.idType)))
                throw new MappingException("Results for find-and-delete repository queries must be the entity class (" +
                                           (entityInfo.recordClass == null ? entityInfo.entityClass : entityInfo.recordClass).getName() +
                                           ") or the id class (" + entityInfo.idType +
                                           "), not the " + type.getName() + " class."); // TODO NLS
        }

        return isFindAndDelete;
    }

    /**
     * Raises an error because the number of keyset keys does not match the number of sort parameters.
     *
     * @param keysetCursor keyset cursor
     */
    @Trivial
    private void keysetSizeMismatchError(PageRequest.Cursor keysetCursor) {
        List<String> keyTypes = new ArrayList<>();
        for (int i = 0; i < keysetCursor.size(); i++)
            keyTypes.add(keysetCursor.get(i) == null ? null : keysetCursor.get(i).getClass().getName());

        throw new MappingException("The keyset cursor with key types " + keyTypes +
                                   " cannot be used with sort criteria of " + sorts +
                                   " because they have different numbers of elements. The keyset size is " + keysetCursor.size() +
                                   " and the sort criteria size is " + sorts.size() + "."); // TODO NLS
    }

    /**
     * Sets query parameters from keyset values.
     *
     * @param query        the query
     * @param keysetCursor keyset values
     * @throws Exception if an error occurs
     */
    void setKeysetParameters(Query query, PageRequest.Cursor keysetCursor) throws Exception {
        int paramNum = paramCount; // set to position before the first keyset parameter
        if (paramNames == null) // positional parameters
            for (int i = 0; i < keysetCursor.size(); i++) {
                Object value = keysetCursor.get(i);
                if (entityInfo.idClassAttributeAccessors != null && entityInfo.idType.isInstance(value)) {
                    for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                        Object v = accessor instanceof Field ? ((Field) accessor).get(value) : ((Method) accessor).invoke(value);
                        if (++paramNum - paramCount > sorts.size())
                            keysetSizeMismatchError(keysetCursor);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set keyset parameter ?" + paramNum + ' ' + value.getClass().getName() + "-->" +
                                               (v == null ? null : v.getClass().getSimpleName()));
                        query.setParameter(paramNum, v);
                    }
                } else {
                    if (++paramNum - paramCount > sorts.size())
                        keysetSizeMismatchError(keysetCursor);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set keyset parameter ?" + paramNum + ' ' +
                                           (value == null ? null : value.getClass().getSimpleName()));
                    query.setParameter(paramNum, value);
                }
            }
        else // named parameters
            for (int i = 0; i < keysetCursor.size(); i++) {
                Object value = keysetCursor.get(i);
                if (entityInfo.idClassAttributeAccessors != null && entityInfo.idType.isInstance(value)) {
                    for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                        Object v = accessor instanceof Field ? ((Field) accessor).get(value) : ((Method) accessor).invoke(value);
                        if (++paramNum - paramCount > sorts.size())
                            keysetSizeMismatchError(keysetCursor);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set keyset parameter :keyset" + paramNum + ' ' + value.getClass().getName() + "-->" +
                                               (v == null ? null : v.getClass().getSimpleName()));
                        query.setParameter("keyset" + paramNum, v);
                    }
                } else {
                    if (++paramNum - paramCount > sorts.size())
                        keysetSizeMismatchError(keysetCursor);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set keyset parameter :keyset" + paramNum + ' ' +
                                           (value == null ? null : value.getClass().getSimpleName()));
                    query.setParameter("keyset" + paramNum, value);
                }
            }

        if (sorts.size() > paramNum - paramCount) // not enough keyset values
            keysetSizeMismatchError(keysetCursor);
    }

    /**
     * Sets the query parameter at the specified position to a value from the entity,
     * obtained via the accessor methods.
     *
     * @param p         parameter position.
     * @param query     the query.
     * @param entity    the entity.
     * @param accessors accessor methods to obtain the entity attribute value.
     * @throws Exception if an error occurs.
     */
    @Trivial
    static void setParameter(int p, Query query, Object entity, List<Member> accessors) throws Exception {
        Object v = entity;
        for (Member accessor : accessors)
            v = accessor instanceof Method ? ((Method) accessor).invoke(v) : ((Field) accessor).get(v);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "set ?" + p + ' ' + (v == null ? null : v.getClass().getSimpleName()));

        query.setParameter(p, v);
    }

    /**
     * Sets query parameters from repository method arguments.
     *
     * @param query the query
     * @param args  repository method arguments
     * @throws Exception if an error occurs
     */
    void setParameters(Query query, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        int methodParamForQueryCount = paramCount - paramAddedCount;
        if (args != null && args.length < methodParamForQueryCount)
            throw new MappingException("The " + method.getName() + " repository method has " + args.length +
                                       " parameters, but requires " + methodParamForQueryCount +
                                       " method parameters. The generated JPQL query is: " + jpql + "."); // TODO NLS

        int namedParamCount = paramNames == null ? 0 : paramNames.size();
        for (int i = 0, p = 0; i < methodParamForQueryCount; i++) {
            Object arg = args[i];

            if (arg == null || entityInfo.idClassAttributeAccessors == null || !entityInfo.idType.isInstance(arg)) {
                if (p < namedParamCount) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set :" + paramNames.get(p) + ' ' + (arg == null ? null : arg.getClass().getSimpleName()));
                    query.setParameter(paramNames.get(p++), arg);
                } else { // positional parameter
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set ?" + (p + 1) + ' ' + (arg == null ? null : arg.getClass().getSimpleName()));
                    query.setParameter(++p, arg);
                }
            } else { // split IdClass argument into parameters
                for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                    Object param = accessor instanceof Method ? ((Method) accessor).invoke(arg) : ((Field) accessor).get(arg);
                    if (p < namedParamCount) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set :" + paramNames.get(p) + ' ' + (param == null ? null : param.getClass().getSimpleName()));
                        query.setParameter(paramNames.get(p++), param);
                    } else { // positional parameter
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set ?" + (p + 1) + ' ' + (param == null ? null : param.getClass().getSimpleName()));
                        query.setParameter(++p, param);
                    }
                }
            }
        }
    }

    /**
     * Sets query parameters for DELETE_WITH_ENTITY_PARAM where the entity has an IdClass.
     *
     * @param query   the query
     * @param entity  the entity
     * @param version the version if versioned, otherwise null.
     * @throws Exception if an error occurs
     */
    void setParametersFromIdClassAndVersion(Query query, Object entity, Object version) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        int p = 0;
        for (String idClassAttr : entityInfo.idClassAttributeAccessors.keySet())
            setParameter(++p, query, entity,
                         entityInfo.attributeAccessors.get(entityInfo.getAttributeName(idClassAttr, true)));

        if (version != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "set ?" + (p + 1) + ' ' + version);
            query.setParameter(++p, version);
        }
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder b = new StringBuilder("QueryInfo@").append(Integer.toHexString(hashCode())) //
                        .append(' ').append(method.getReturnType().getSimpleName()).append(' ').append(method.getName());
        boolean first = true;
        for (Class<?> p : method.getParameterTypes()) {
            b.append(first ? "(" : ", ").append(p.getSimpleName());
            first = false;
        }
        b.append(first ? "() " : ") ");
        if (jpql != null)
            b.append(jpql);
        if (paramCount > 0) {
            b.append(" [").append(paramCount).append(paramNames == null ? " positional params" : " named params");
            if (paramAddedCount != 0)
                b.append(", ").append(paramCount - paramAddedCount).append(" method params");
            b.append(']');
        }
        return b.toString();
    }

    /**
     * Ensure that the annotations are valid together on the same repository method.
     *
     * @param delete  The Delete annotation if present, otherwise null.
     * @param insert  The Insert annotation if present, otherwise null.
     * @param update  The Update annotation if present, otherwise null.
     * @param save    The Save annotation if present, otherwise null.
     * @param find    The Find annotation if present, otherwise null.
     * @param query   The Query annotation if present, otherwise null.
     * @param orderBy array of OrderBy annotations if present, otherwise an empty array.
     * @param count   The Count annotation if present, otherwise null.
     * @param exists  The Exists annotation if present, otherwise null.
     * @return Count, Delete, Exists, Find, Insert, Query, Save, or Update annotation if present. Otherwise null.
     * @throws UnsupportedOperationException if the combination of annotations is not valid.
     */
    @Trivial
    Annotation validateAnnotationCombinations(Delete delete, Insert insert, Update update, Save save,
                                              Find find, jakarta.data.repository.Query query, OrderBy[] orderBy,
                                              Annotation count, Annotation exists) {
        int o = orderBy.length == 0 ? 0 : 1;

        // These can be paired with OrderBy:
        int f = find == null ? 0 : 1;
        int q = query == null ? 0 : 1;

        // These cannot be paired with OrderBy or with each other:
        int ius = (insert == null ? 0 : 1) +
                  (update == null ? 0 : 1) +
                  (save == null ? 0 : 1);

        int iusdce = ius +
                     (delete == null ? 0 : 1) +
                     (count == null ? 0 : 1) +
                     (exists == null ? 0 : 1);

        if (iusdce + f > 1 // more than one of (Insert, Update, Save, Delete, Count, Exists, Find)
            || iusdce + o > 1 // more than one of (Insert, Update, Save, Delete, Count, Exists, OrderBy)
            || iusdce + q > 1) { // one of (Insert, Update, Save, Delete, Count, Exists) with Query

            // Invalid combination of multiple annotations

            List<String> annoClassNames = new ArrayList<String>();
            for (Annotation anno : Arrays.asList(count, delete, exists, find, insert, query, save, update))
                if (anno != null)
                    annoClassNames.add(anno.annotationType().getName());
            if (orderBy.length > 0)
                annoClassNames.add(OrderBy.class.getName());

            throw new UnsupportedOperationException("The " + method.getDeclaringClass().getName() + '.' + method.getName() +
                                                    " repository method cannot be annotated with the following combination of annotations: " +
                                                    annoClassNames); // TODO NLS
        }

        return ius == 1 //
                        ? (insert != null ? insert : update != null ? update : save) //
                        : iusdce == 1 //
                                        ? (delete != null ? delete : count != null ? count : exists) //
                                        : (q == 1 ? query : f == 1 ? find : null);
    }

    /**
     * Copy of query information, but with updated JPQL and sort criteria.
     */
    QueryInfo withJPQL(String jpql, List<Sort<Object>> sorts) {
        QueryInfo q = new QueryInfo(method, entityParamType, returnArrayType, returnTypeAtDepth);
        q.entityInfo = entityInfo;
        q.entityVar = entityVar;
        q.entityVar_ = entityVar_;
        q.hasWhere = hasWhere;
        q.jpql = jpql;
        q.jpqlAfterKeyset = jpqlAfterKeyset;
        q.jpqlBeforeKeyset = jpqlBeforeKeyset;
        q.jpqlCount = jpqlCount;
        q.jpqlDelete = jpqlDelete; // TODO jpqlCount and jpqlDelete could potentially be combined because you will never need both at once
        q.maxResults = maxResults;
        q.paramCount = paramCount;
        q.paramAddedCount = paramAddedCount;
        q.paramNames = paramNames;
        q.sorts = sorts;
        q.type = type;
        q.validateParams = validateParams;
        q.validateParams = validateResult;
        return q;
    }

    /**
     * Returns the wrapper class if a primitive class, otherwise the same class.
     *
     * @param c class that is possibly a primitive class.
     * @return wrapper class for a primitive, otherwise the same class that was supplied as a parameter.
     */
    @Trivial
    static final Class<?> wrapperClassIfPrimitive(Class<?> c) {
        Class<?> w = WRAPPER_CLASSES.get(c);
        return w == null ? c : w;
    }
}

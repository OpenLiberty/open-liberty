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

import io.openliberty.data.repository.Count;
import io.openliberty.data.repository.Exists;
import io.openliberty.data.repository.Select;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.page.Pageable;
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
     * Entity variable name. "o" is used as the default in generated queries.
     */
    String entityVar = "o";

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
    final Method method;

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
     * or dynamically via Pageable Sort parameters or Sort parameters to the repository method,
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
        Set<String> names = entityInfo.idClassAttributeAccessors != null && "id".equalsIgnoreCase(attribute) //
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
     * Adds dynamically specified Sort criteria from the Pageable to the end of an existing list, or
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
                throw new DataException(new IllegalArgumentException("Sort: null"));
            else if (hasIdClass && sort.property().equalsIgnoreCase("id"))
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
                throw new DataException(new IllegalArgumentException("Sort: null"));
            else if (hasIdClass && sort.property().equalsIgnoreCase("id"))
                for (String name : entityInfo.idClassAttributeAccessors.keySet())
                    combined.add(entityInfo.getWithAttributeName(entityInfo.getAttributeName(name, true), sort));
            else
                combined.add(entityInfo.getWithAttributeName(sort.property(), sort));
        }
        return combined;
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
            hasDynamicSort = Pageable.class.equals(paramTypes[i])
                             || Order.class.equals(paramTypes[i])
                             || Sort[].class.equals(paramTypes[i])
                             || Sort.class.equals(paramTypes[i]);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "hasDynamicSortCriteria? " + hasDynamicSort);
        return hasDynamicSort;
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
     * @param queryJPQL      Query.value()
     * @param queryCountJPQL Query.count()
     * @param countPages     whether or not to obtain a count of pages.
     */
    void initForQuery(String queryJPQL, String queryCountJPQL, boolean countPages) {
        jpql = queryJPQL;

        String upper = jpql.toUpperCase();
        String upperTrimmed = upper.stripLeading();
        if (upperTrimmed.startsWith("SELECT")) {
            int order = upper.lastIndexOf("ORDER BY");
            type = Type.FIND;
            sorts = sorts == null ? new ArrayList<>() : sorts;
            jpqlCount = queryCountJPQL.length() > 0 ? queryCountJPQL : null;

            int selectIndex = upper.length() - upperTrimmed.length();
            int from = find("FROM", upper, selectIndex + 9);
            if (from > 0) {
                // TODO support for multiple entity types
                int entityName = find(entityInfo.name.toUpperCase(), upper, from + 5);
                if (entityName > 0)
                    entityVar = findEntityVariable(jpql, entityName + entityInfo.name.length() + 1);

                if (countPages && jpqlCount == null) {
                    // Attempt to infer from provided query
                    String s = jpql.substring(selectIndex + 6, from);
                    int comma = s.indexOf(',');
                    if (comma > 0)
                        s = s.substring(0, comma);
                    jpqlCount = new StringBuilder(jpql.length() + 7) //
                                    .append("SELECT COUNT(").append(s.trim()).append(") ") //
                                    .append(order > from ? jpql.substring(from, order) : jpql.substring(from)) //
                                    .toString();
                }
            }
        } else if (upperTrimmed.startsWith("UPDATE")) {
            type = Type.UPDATE;
        } else if (upperTrimmed.startsWith("DELETE")) {
            type = Type.DELETE;
        } else {
            throw new UnsupportedOperationException(jpql);
        }

        hasWhere = upperTrimmed.contains("WHERE");
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
    private void keysetSizeMismatchError(Pageable.Cursor keysetCursor) {
        List<String> keyTypes = new ArrayList<>();
        for (int i = 0; i < keysetCursor.size(); i++)
            keyTypes.add(keysetCursor.getKeysetElement(i) == null ? null : keysetCursor.getKeysetElement(i).getClass().getName());

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
    void setKeysetParameters(Query query, Pageable.Cursor keysetCursor) throws Exception {
        int paramNum = paramCount; // set to position before the first keyset parameter
        if (paramNames == null) // positional parameters
            for (int i = 0; i < keysetCursor.size(); i++) {
                Object value = keysetCursor.getKeysetElement(i);
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
                Object value = keysetCursor.getKeysetElement(i);
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
     * @param select  The Select annotation if present, otherwise null.
     * @return Count, Delete, Exists, Find, Insert, Query, Save, or Update annotation if present. Otherwise null.
     * @throws UnsupportedOperationException if the combination of annotations is not valid.
     */
    @Trivial
    Annotation validateAnnotationCombinations(Delete delete, Insert insert, Update update, Save save,
                                              Find find, jakarta.data.repository.Query query, OrderBy[] orderBy,
                                              Count count, Exists exists, Select select) {
        int o = orderBy.length == 0 ? 0 : 1;

        // These can be paired with OrderBy:
        int f = find == null ? 0 : 1;
        int q = query == null ? 0 : 1;
        int s = select == null ? 0 : 1;

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
            || iusdce + q + s > 1) { // one of (Insert, Update, Save, Delete, Count, Exists) with Query or Select, or both Query and Select

            // Invalid combination of multiple annotations

            List<String> annoClassNames = new ArrayList<String>();
            for (Annotation anno : Arrays.asList(count, delete, exists, find, insert, query, save, select, update))
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

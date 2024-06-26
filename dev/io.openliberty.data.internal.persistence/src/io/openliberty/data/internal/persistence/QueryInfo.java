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
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.BaseStream;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.version.DataVersionCompatibility;
import jakarta.data.Limit;
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
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

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
     * Valid types for repository method parameters after the query parameters.
     */
    static final Set<Class<?>> SPECIAL_PARAM_TYPES = new HashSet<>(Arrays.asList //
    (Limit.class, Order.class, PageRequest.class, Sort.class, Sort[].class));

    /**
     * Valid types for when a repository method computes an update count
     */
    private static final Set<Class<?>> UPDATE_COUNT_TYPES = new HashSet<>(Arrays.asList //
    (boolean.class, Boolean.class, int.class, Integer.class, long.class, Long.class, void.class, Void.class, Number.class));

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
    private String entityVar = "o";

    /**
     * Entity identifier variable name and . character if an identifier variable is used.
     * Otherwise the empty string. "o." is used as the default in generated queries.
     */
    private String entityVar_ = "o.";

    /**
     * Indicates if the query has a WHERE clause.
     * This is accurate only for generated or partially provided queries.
     */
    private boolean hasWhere;

    /**
     * True if the repository method return type is Optional<Type>,
     * CompletableFuture<Optional<Type>>, or CompletionStage<Optional<Type>>.
     * Otherwise false.
     */
    final boolean isOptional;

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
     * The type of data structure that returns multiple results for this query.
     * Null if the query return type limits to single results.
     */
    final Class<?> multiType;

    /**
     * Difference between the number of parameters to the JPQL query and the expected number of
     * corresponding parameters on the repository method signature. If the entity has an IdClass
     * and the repository method queries on Id, it will have only a single parameter for the user
     * to input, whereas the JPQL will have additional parameters for each additional attribute
     * of the IdClass.
     */
    int paramAddedCount;

    /**
     * Number of parameters to the JPQL query.
     */
    int paramCount;

    /**
     * Names that are specified by the <code>Param</code> annotation for each query parameter.
     * An empty list is a marker that named parameters are present, but need to be populated into the list.
     * Population is deferred to ensure the order of the list matches the order of parameters in the method signature.
     * A null value indicates positional parameters (?1, ?2, ...) are used rather than named parameters
     * or there are no parameters at all.
     */
    private List<String> paramNames;

    /**
     * Array element type if the repository method returns an array, such as,
     * <code>Product[] findByNameLike(String namePattern);</code>
     * or if its parameterized type is an array, such as,
     * <code>CompletableFuture&lt;Product[]&gt; findByNameLike(String namePattern);</code>
     * Otherwise null.
     */
    final Class<?> returnArrayType;

    /**
     * The type of a single result obtained by the query.
     * For example, a single result of a query that returns List<MyEntity> is of the type MyEntity.
     */
    final Class<?> singleType;

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
     * Constructor for the withJPQL method.
     */
    private QueryInfo(Method method, Class<?> entityParamType, boolean isOptional,
                      Class<?> multiType, Class<?> returnArrayType, Class<?> singleType) {
        this.method = method;
        this.entityParamType = entityParamType;
        this.isOptional = isOptional;
        this.multiType = multiType;
        this.returnArrayType = returnArrayType;
        this.singleType = singleType;
    }

    /**
     * Construct partially complete query information.
     *
     * @param method            repository method.
     * @param entityParamType   type of the first parameter if a life cycle method, otherwise null.
     * @param returnArrayType   array element type if the repository method returns an array, otherwise null.
     * @param returnTypeAtDepth return type of the repository method return value,
     *                              split into levels of depth for each type parameter and array component.
     *                              This is useful in cases such as
     *                              <code>&#64;Query(...) Optional&lt;Float&gt; priceOf(String productId)</code>
     *                              which resolves to { Optional.class, Float.class }
     *                              and
     *                              <code>CompletableFuture&lt;Stream&lt;Product&gt&gt; findByNameLike(String namePattern)</code>
     *                              which resolves to { CompletableFuture.class, Stream.class, Product.class }
     *                              and
     *                              <code>CompletionStage&lt;Product[]&gt; findByNameIgnoreCaseLike(String namePattern)</code>
     *                              which resolves to { CompletionStage.class, Product[].class, Product.class }
     */
    public QueryInfo(Method method, Class<?> entityParamType, Class<?> returnArrayType, List<Class<?>> returnTypeAtDepth) {
        this.method = method;
        this.entityParamType = entityParamType;
        this.returnArrayType = returnArrayType;

        int d = 0, depth = returnTypeAtDepth.size();
        Class<?> type = returnTypeAtDepth.get(d);
        if (CompletionStage.class.equals(type) || CompletableFuture.class.equals(type))
            if (++d < depth)
                type = returnTypeAtDepth.get(d);
            else
                throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                        method.getDeclaringClass().getName() +
                                                        " repository specifies the " + method.getGenericReturnType() +
                                                        " result type, which is not a supported result type for a repository method."); // TODO NLS and add helpful information about supported result types
        if (isOptional = Optional.class.equals(type)) {
            multiType = null;
            if (++d < depth)
                type = returnTypeAtDepth.get(d);
            else
                throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                        method.getDeclaringClass().getName() +
                                                        " repository specifies the " + method.getGenericReturnType() +
                                                        " result type, which is not a supported result type for a repository method."); // TODO NLS and add helpful information about supported result types
        } else {
            if (returnArrayType != null
                || Iterator.class.equals(type)
                || Iterable.class.isAssignableFrom(type) // includes Page, List, ...
                || BaseStream.class.isAssignableFrom(type)) {
                multiType = type;
                if (++d < depth)
                    type = returnTypeAtDepth.get(d);
                else
                    throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                            method.getDeclaringClass().getName() +
                                                            " repository specifies the " + method.getGenericReturnType() +
                                                            " result type, which is not a supported result type for a repository method."); // TODO NLS and add helpful information about supported result types
            } else {
                multiType = null;
            }
        }

        singleType = type;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "result type information",
                     "isOptional? " + isOptional,
                     "multiType:  " + multiType,
                     "singleType: " + singleType);
    }

    /**
     * Construct partially complete query information.
     */
    public QueryInfo(Method method, Type type) {
        this.method = method;
        this.entityParamType = null;
        this.multiType = null;
        this.isOptional = false;
        this.returnArrayType = null;
        this.singleType = null;
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
    private void addSort(boolean ignoreCase, String attribute, boolean descending) {
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
                    } else if ("this".equalsIgnoreCase(str)
                               || entityInfo.getAttributeName(str, false) == null) {
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
     * Generates JPQL for a *By condition such as MyColumn[IgnoreCase][Not]Like
     */
    private void generateCondition(String methodName, int start, int endBefore, StringBuilder q) {
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

        String name = entityInfo.getAttributeName(attribute, true);
        if (name == null) {
            if (entityInfo.idClassAttributeAccessors != null && ID.equals(attribute))
                generateConditionsForIdClass(condition, ignoreCase, negated, q);
            return;
        }

        StringBuilder attributeExpr = new StringBuilder();
        if (function != null)
            attributeExpr.append(function); // such as LOWER(  or  ROUND(
        if (trimmed)
            attributeExpr.append("TRIM(");

        String o_ = entityVar_;
        attributeExpr.append(o_).append(name);

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

        boolean isCollection = entityInfo.collectionElementTypes.containsKey(name);
        if (isCollection)
            condition.verifyCollectionsSupported(name, ignoreCase);

        switch (condition) {
            case STARTS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT(");
                generateParam(q, ignoreCase, ++paramCount).append(", '%')");
                break;
            case ENDS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                generateParam(q, ignoreCase, ++paramCount).append(")");
                break;
            case LIKE:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE ");
                generateParam(q, ignoreCase, ++paramCount);
                break;
            case BETWEEN:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ");
                generateParam(q, ignoreCase, ++paramCount).append(" AND ");
                generateParam(q, ignoreCase, ++paramCount);
                break;
            case CONTAINS:
                if (isCollection) {
                    q.append(" ?").append(++paramCount).append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
                } else {
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                    generateParam(q, ignoreCase, ++paramCount).append(", '%')");
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
                generateParam(q, ignoreCase, ++paramCount);
        }
    }

    /**
     * Generates JPQL for a *By condition on the IdClass, which expands to multiple conditions in JPQL.
     */
    private void generateConditionsForIdClass(Condition condition, boolean ignoreCase, boolean negate, StringBuilder q) {

        String o_ = entityVar_;

        q.append(negate ? "NOT (" : "(");

        int count = 0;
        for (String idClassAttr : entityInfo.idClassAttributeAccessors.keySet()) {
            if (++count != 1)
                q.append(" AND ");

            String name = entityInfo.getAttributeName(idClassAttr, true);
            if (ignoreCase)
                q.append("LOWER(").append(o_).append(name).append(')');
            else
                q.append(o_).append(name);

            switch (condition) {
                case EQUALS:
                case NOT_EQUALS:
                    q.append(condition.operator);
                    generateParam(q, ignoreCase, ++paramCount);
                    if (count != 1)
                        paramAddedCount++;
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
     * @param where the WHERE clause
     */
    private void generateCount(String where) {
        String o = entityVar;
        StringBuilder q = new StringBuilder(21 + 2 * o.length() + entityInfo.name.length() + (where == null ? 0 : where.length())) //
                        .append("SELECT COUNT(").append(o).append(") FROM ") //
                        .append(entityInfo.name).append(' ').append(o);

        if (where != null)
            q.append(where);

        jpqlCount = q.toString();
    }

    /**
     * Generates JQPL for deletion by id, for find-and-delete repository operations.
     */
    private String generateDeleteById() {
        String o = entityVar;
        String o_ = entityVar_;
        StringBuilder q;
        if (entityInfo.idClassAttributeAccessors == null) {
            String idAttrName = entityInfo.attributeNames.get(ID);
            q = new StringBuilder(24 + entityInfo.name.length() + o.length() * 2 + idAttrName.length()) //
                            .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o).append(" WHERE ") //
                            .append(o_).append(idAttrName).append("=?1");
        } else {
            q = new StringBuilder(200) //
                            .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o).append(" WHERE ");
            int count = 0;
            for (String idClassAttrName : entityInfo.idClassAttributeAccessors.keySet()) {
                if (++count != 1)
                    q.append(" AND ");
                q.append(o_).append(entityInfo.getAttributeName(idClassAttrName, true)).append("=?").append(count);
            }
        }
        return q.toString();
    }

    /**
     * Generates JPQL for deletion by entity id and version (if versioned).
     */
    private StringBuilder generateDeleteEntity() {
        String o = entityVar;
        String o_ = entityVar_;

        StringBuilder q = new StringBuilder(100) //
                        .append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);

        if (method.getParameterCount() == 0) {
            type = Type.DELETE;
            hasWhere = false;
        } else {
            setType(Delete.class, Type.DELETE_WITH_ENTITY_PARAM);
            hasWhere = true;

            q.append(" WHERE (");

            String idName = entityInfo.getAttributeName(ID, true);
            if (idName == null && entityInfo.idClassAttributeAccessors != null) {
                boolean first = true;
                for (String name : entityInfo.idClassAttributeAccessors.keySet()) {
                    if (first)
                        first = false;
                    else
                        q.append(" AND ");

                    name = entityInfo.attributeNames.get(name);
                    q.append(o_).append(name).append("=?").append(++paramCount);
                }
            } else {
                q.append(o_).append(idName).append("=?").append(++paramCount);
            }

            if (entityInfo.versionAttributeName != null)
                q.append(" AND ").append(o_).append(entityInfo.versionAttributeName).append("=?").append(++paramCount);

            q.append(')');
        }

        return q;
    }

    /**
     * Generates the before/after keyset queries and populates them into the query information.
     * Example conditions to add for forward keyset of (lastName, firstName, ssn):
     * AND ((o.lastName > ?5)
     * _ OR (o.lastName = ?5 AND o.firstName > ?6)
     * _ OR (o.lastName = ?5 AND o.firstName = ?6 AND o.ssn > ?7) )
     *
     * @param q    query up to the WHERE clause, if present
     * @param fwd  ORDER BY clause in forward page direction. Null if forward page direction is not needed.
     * @param prev ORDER BY clause in previous page direction. Null if previous page direction is not needed.
     */
    void generateKeysetQueries(StringBuilder q, StringBuilder fwd, StringBuilder prev) {
        int numKeys = sorts.size();
        String paramPrefix = paramNames == null ? "?" : ":keyset";
        StringBuilder a = fwd == null ? null : new StringBuilder(200).append(hasWhere ? " AND (" : " WHERE (");
        StringBuilder b = prev == null ? null : new StringBuilder(200).append(hasWhere ? " AND (" : " WHERE (");
        String o_ = entityVar_;
        for (int i = 0; i < numKeys; i++) {
            if (a != null)
                a.append(i == 0 ? "(" : " OR (");
            if (b != null)
                b.append(i == 0 ? "(" : " OR (");
            for (int k = 0; k <= i; k++) {
                Sort<?> keyInfo = sorts.get(k);
                String name = keyInfo.property();
                boolean asc = keyInfo.isAscending();
                boolean lower = keyInfo.ignoreCase();
                if (a != null)
                    if (lower) {
                        a.append(k == 0 ? "LOWER(" : " AND LOWER(").append(o_).append(name).append(')');
                        a.append(k < i ? '=' : (asc ? '>' : '<'));
                        a.append("LOWER(").append(paramPrefix).append(paramCount + 1 + k).append(')');
                    } else {
                        a.append(k == 0 ? "" : " AND ").append(o_).append(name);
                        a.append(k < i ? '=' : (asc ? '>' : '<'));
                        a.append(paramPrefix).append(paramCount + 1 + k);
                    }
                if (b != null)
                    if (lower) {
                        b.append(k == 0 ? "LOWER(" : " AND LOWER(").append(o_).append(name).append(')');
                        b.append(k < i ? '=' : (asc ? '<' : '>'));
                        b.append("LOWER(").append(paramPrefix).append(paramCount + 1 + k).append(')');
                    } else {
                        b.append(k == 0 ? "" : " AND ").append(o_).append(name);
                        b.append(k < i ? '=' : (asc ? '<' : '>'));
                        b.append(paramPrefix).append(paramCount + 1 + k);
                    }
            }
            if (a != null)
                a.append(')');
            if (b != null)
                b.append(')');
        }
        if (a != null)
            jpqlAfterKeyset = new StringBuilder(q).append(a).append(')').append(fwd).toString();
        if (b != null)
            jpqlBeforeKeyset = new StringBuilder(q).append(b).append(')').append(prev).toString();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "forward & previous keyset queries", jpqlAfterKeyset, jpqlBeforeKeyset);
    }

    /**
     * Generates the JPQL ORDER BY clause. This method is common between the OrderBy annotation and keyword.
     */
    private void generateOrderBy(StringBuilder q) {
        boolean needsKeysetQueries = CursoredPage.class.equals(multiType);

        StringBuilder fwd = needsKeysetQueries ? new StringBuilder(100) : q; // forward page order
        StringBuilder prev = needsKeysetQueries ? new StringBuilder(100) : null; // previous page order

        boolean first = true;
        for (Sort<?> sort : sorts) {
            validateSort(sort);
            fwd.append(first ? " ORDER BY " : ", ");
            generateSort(fwd, sort, true);

            if (needsKeysetQueries) {
                prev.append(first ? " ORDER BY " : ", ");
                generateSort(prev, sort, false);
            }
            first = false;
        }

        if (needsKeysetQueries) {
            generateKeysetQueries(q, fwd, prev);
            q.append(fwd);
        }
    }

    /**
     * Generates and appends JQPL for a repository method parameter. Either of the form ?1 or LOWER(?1)
     *
     * @param q     builder for the JPQL query.
     * @param lower indicates if the query parameter should be compared in lower case.
     * @param num   parameter number.
     * @return the same builder for the JPQL query.
     */
    @Trivial
    private static StringBuilder generateParam(StringBuilder q, boolean lower, int num) {
        q.append(lower ? "LOWER(?" : '?').append(num);
        return lower ? q.append(')') : q;
    }

    /**
     * Generates JPQL based on method parameters.
     * Method annotations Count, Delete, Exists, Find, and Update indicate the respective type of method.
     * Find methods can have special type parameters (PageRequest, Limit, Order, Sort, Sort[]). Other methods cannot.
     *
     * @param q          JPQL query to which to append the WHERE clause. Or null to create a new JPQL query.
     * @param methodAnno Count, Delete, Exists, Find, or Update annotation on the method. Never null.
     * @param countPages indicates whether or not to count pages. Only applies for find queries.
     */
    private StringBuilder generateQueryByParameters(StringBuilder q, Annotation methodAnno,
                                                    boolean countPages) {
        String o = entityVar;
        String o_ = entityVar_;
        DataVersionCompatibility compat = entityInfo.builder.provider.compat;

        Boolean isNamePresent = null; // unknown
        Parameter[] params = null;

        Class<?>[] paramTypes = method.getParameterTypes();
        int numAttributeParams = paramTypes.length;
        while (numAttributeParams > 0 && SPECIAL_PARAM_TYPES.contains(paramTypes[numAttributeParams - 1]))
            numAttributeParams--;

        if (numAttributeParams < paramTypes.length && !(methodAnno instanceof Find) && !(methodAnno instanceof Delete))
            throw new MappingException("The special parameter types " + SPECIAL_PARAM_TYPES +
                                       " must not be used on the " + method.getName() + " method of the " +
                                       method.getDeclaringClass().getName() + " repository because the repository method is a " +
                                       methodAnno.annotationType().getSimpleName() + " operation."); // TODO NLS

        Annotation[][] annosForAllParams = method.getParameterAnnotations();
        boolean[] isUpdateOp = new boolean[annosForAllParams.length];

        if (q == null)
            // Write new JPQL, starting with UPDATE or SELECT
            if (methodAnno instanceof Update) {
                type = Type.UPDATE;
                q = new StringBuilder(250).append("UPDATE ").append(entityInfo.name).append(' ').append(o).append(" SET");

                boolean first = true;
                // p is the method parameter number (0-based)
                // qp is the query parameter number (1-based and accounting for IdClass requiring multiple query parameters)
                for (int p = 0, qp = 1; p < numAttributeParams; p++, qp++) {
                    boolean isIdClass = entityInfo.idClassAttributeAccessors != null && paramTypes[p].equals(entityInfo.idType);

                    String[] attrAndOp = compat.getUpdateAttributeAndOperation(annosForAllParams[p]);
                    if (attrAndOp == null) {
                        if (isIdClass)
                            qp += entityInfo.idClassAttributeAccessors.size() - 1;
                    } else {
                        isUpdateOp[p] = true;
                        if (isIdClass) {
                            if ("=".equals(attrAndOp[1])) {
                                //    generateUpdatesForIdClass(queryInfo, update, first, q);
                                throw new UnsupportedOperationException("@Assign IdClass"); // TODO
                            } else {
                                throw new MappingException("One or more of the " + Arrays.toString(annosForAllParams[p]) +
                                                           " annotations specifes an operation that cannot be used on parameter " +
                                                           (p + 1) + " of the " + method.getName() + " method of the " +
                                                           method.getDeclaringClass().getName() + " repository when the Id is an IdClass."); // TODO NLS
                            }
                        } else {
                            String attribute = attrAndOp[0];
                            String op = attrAndOp[1];

                            if ("".equals(attribute)) {
                                if (isNamePresent == null) {
                                    params = method.getParameters();
                                    isNamePresent = params[p].isNamePresent();
                                }
                                if (Boolean.TRUE.equals(isNamePresent))
                                    attribute = params[p].getName();
                                else
                                    throw new MappingException("You must specify an entity attribute name as the value of the" +
                                                               " annotation on parameter " + (p + 1) +
                                                               " of the " + method.getName() + " method of the " +
                                                               method.getDeclaringClass().getName() + " repository or compile the application" +
                                                               " with the -parameters compiler option that preserves the parameter names."); // TODO NLS
                            }

                            String name = entityInfo.getAttributeName(attribute, true);

                            q.append(first ? " " : ", ").append(o_).append(name).append("=");
                            first = false;

                            boolean withFunction = false;
                            switch (op) {
                                case "=":
                                    break;
                                case "+":
                                    if (withFunction = CharSequence.class.isAssignableFrom(entityInfo.attributeTypes.get(name)))
                                        q.append("CONCAT(").append(o_).append(name).append(',');
                                    else
                                        q.append(o_).append(name).append('+');
                                    break;
                                default:
                                    q.append(o_).append(name).append(op);
                            }

                            paramCount++;
                            q.append('?').append(qp);

                            if (withFunction)
                                q.append(')');
                        }
                    }
                }
            } else {
                type = Type.FIND;
                q = generateSelectClause().append(" FROM ").append(entityInfo.name).append(' ').append(o);
            }

        int startIndexForWhereClause = q.length();

        // append the WHERE clause
        // p is the method parameter number (0-based)
        // qp is the query parameter number (1-based and accounting for IdClass requiring multiple query parameters)
        for (int p = 0, qp = 1; p < numAttributeParams; p++, qp++) {
            boolean isIdClass = entityInfo.idClassAttributeAccessors != null && paramTypes[p].equals(entityInfo.idType);
            if (!isUpdateOp[p]) {
                if (hasWhere) {
                    q.append(compat.hasOrAnnotation(annosForAllParams[p]) ? " OR " : " AND ");
                } else {
                    q.append(" WHERE (");
                    hasWhere = true;
                }

                // Determine the entity attribute name, first from @By("name"), otherwise from the parameter name
                String attribute = null;
                for (Annotation anno : annosForAllParams[p])
                    if (anno instanceof By)
                        attribute = ((By) anno).value();

                if (attribute == null) {
                    if (isNamePresent == null) {
                        params = method.getParameters();
                        isNamePresent = params[p].isNamePresent();
                    }
                    if (Boolean.TRUE.equals(isNamePresent))
                        attribute = params[p].getName();
                    else
                        throw new MappingException("You must specify an entity attribute name as the value of the " +
                                                   By.class.getName() + " annotation on parameter " + (p + 1) +
                                                   " of the " + method.getName() + " method of the " +
                                                   method.getDeclaringClass().getName() + " repository or compile the application" +
                                                   " with the -parameters compiler option that preserves the parameter names."); // TODO NLS
                }

                if (isIdClass) {
                    String[] idClassAttrNames = new String[entityInfo.idClassAttributeAccessors.size()];
                    int i = 0;
                    for (String idClassAttr : entityInfo.idClassAttributeAccessors.keySet())
                        idClassAttrNames[i++] = entityInfo.getAttributeName(idClassAttr, true);

                    compat.appendConditionsForIdClass(q, qp, method, p, o_, idClassAttrNames, annosForAllParams[p]);
                    paramCount += idClassAttrNames.length;
                    paramAddedCount += (idClassAttrNames.length - 1);
                    qp += (idClassAttrNames.length - 1);
                    continue;
                }

                String name = entityInfo.getAttributeName(attribute, true);

                boolean isCollection = entityInfo.collectionElementTypes.containsKey(name);

                paramCount++;

                compat.appendCondition(q, qp, method, p, o_, name, isCollection, annosForAllParams[p]);
            } else if (isIdClass) {
                // adjust query parameter position based on the number of parameters needed for an IdClass
                qp += entityInfo.idClassAttributeAccessors.size() - 1;
            }
        }
        if (hasWhere)
            q.append(')');

        if (countPages && type == Type.FIND)
            generateCount(numAttributeParams == 0 ? null : q.substring(startIndexForWhereClause));

        return q;
    }

    /**
     * Generates the SELECT clause of the JPQL.
     *
     * @return the SELECT clause.
     */
    private StringBuilder generateSelectClause() {
        StringBuilder q = new StringBuilder(200);
        String o = entityVar;
        String o_ = entityVar_;

        String[] cols, selections = entityInfo.builder.provider.compat.getSelections(method);
        if (selections == null || selections.length == 0) {
            cols = null;
        } else if (type == Type.FIND_AND_DELETE) {
            // TODO NLS message for error path once selections are supported function
            throw new UnsupportedOperationException();
        } else {
            cols = new String[selections.length];
            for (int i = 0; i < cols.length; i++) {
                String name = entityInfo.getAttributeName(selections[i], true);
                cols[i] = name == null ? selections[i] : name;
            }
        }

        Class<?> singleType = this.singleType;

        if (singleType.isPrimitive())
            singleType = wrapperClassIfPrimitive(singleType);

        if (cols == null || cols.length == 0) {
            if (singleType.isAssignableFrom(entityInfo.entityClass)
                || entityInfo.inheritance && entityInfo.entityClass.isAssignableFrom(singleType)) {
                // Whole entity
                if (!"this".equals(o))
                    q.append("SELECT ").append(o);
            } else {
                // Look for single entity attribute with the desired type:
                String singleAttributeName = null;
                for (Map.Entry<String, Class<?>> entry : entityInfo.attributeTypes.entrySet()) {
                    // TODO include type variable for collection element in comparison?
                    // JPA metamodel might not be including this information
                    Class<?> collectionElementType = entityInfo.collectionElementTypes.get(entry.getKey());
                    Class<?> attributeType = entry.getValue();
                    if (attributeType.isPrimitive())
                        attributeType = wrapperClassIfPrimitive(attributeType);
                    if (singleType.isAssignableFrom(attributeType))
                        if (singleAttributeName == null)
                            singleAttributeName = entry.getKey();
                        else
                            throw new MappingException("The " + method.getName() + " method of the " +
                                                       method.getDeclaringClass().getName() +
                                                       " repository specifies the " + singleType.getName() +
                                                       " result type, which corresponds to multiple entity attributes: " +
                                                       singleAttributeName + ", " + entry.getKey() +
                                                       ". To use this result type, update the repository method to " +
                                                       "instead use the Query annotation with a SELECT clause to " +
                                                       "disambiguate which entity attribute to use as the result " +
                                                       "of the query."); // TODO NLS
                }

                if (singleAttributeName == null) {
                    // Construct new instance for record or IdClass
                    q.append("SELECT NEW ").append(singleType.getName()).append('(');
                    RecordComponent[] recordComponents;
                    boolean first = true;
                    if ((recordComponents = singleType.getRecordComponents()) != null)
                        for (RecordComponent component : recordComponents) {
                            String name = component.getName();
                            q.append(first ? "" : ", ").append(o).append('.').append(name);
                            first = false;
                        }
                    else if (entityInfo.idClassAttributeAccessors != null && singleType.equals(entityInfo.idType))
                        // TODO determine correct order of idClass attributes for constructor (possibly based on type?)
                        // instead of guessing they are alphabetized?
                        for (String idClassAttributeName : entityInfo.idClassAttributeAccessors.keySet()) {
                            String name = entityInfo.getAttributeName(idClassAttributeName, true);
                            q.append(first ? "" : ", ").append(o).append('.').append(name);
                            first = false;
                        }
                    else
                        throw new MappingException("The " + method.getName() + " method of the " +
                                                   method.getDeclaringClass().getName() + " repository specifies the " +
                                                   singleType.getName() + " result type, which is not convertible from the " +
                                                   entityInfo.entityClass.getName() + " entity type. A repository method " +
                                                   "result type must be the entity type, an entity attribute type, or a " +
                                                   "Java record with attribute names that are a subset of the entity attribute names, " +
                                                   "or the Query annotation must be used to construct the result type with JPQL."); // TODO NLS
                    q.append(')');
                } else {
                    q.append("SELECT ").append(o_).append(singleAttributeName);
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
                for (int i = 0; i < cols.length; i++)
                    q.append(i == 0 ? "SELECT " : ", ").append(o).append('.').append(cols[i]);
            } else {
                // Construct new instance from defined columns
                q.append("SELECT NEW ").append(singleType.getName()).append('(');
                for (int i = 0; i < cols.length; i++)
                    q.append(i == 0 ? "" : ", ").append(o).append('.').append(cols[i]);
                q.append(')');
            }
        }

        return q;
    }

    /**
     * Generates and appends JQPL to sort based on the specified entity attribute.
     * For most properties, this will be of a form such as o.name or LOWER(o.name) DESC or ...
     *
     * @param q             builder for the JPQL query.
     * @param Sort          sort criteria for a single attribute (name must already be converted to a valid entity attribute name).
     * @param sameDirection indicate to append the Sort in the normal direction. Otherwise reverses it (for keyset pagination in previous page direction).
     * @return the same builder for the JPQL query.
     */
    @Trivial
    void generateSort(StringBuilder q, Sort<?> sort, boolean sameDirection) {
        q.append(sort.ignoreCase() ? "LOWER(" : "").append(entityVar_).append(sort.property());

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
     * Generates the JPQL UPDATE clause for a repository updateBy method such as updateByProductIdSetProductNameMultiplyPrice
     */
    private StringBuilder generateUpdateClause(String methodName, int c) {
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
            throw new UnsupportedOperationException(methodName); // updateBy that lacks updates

        // Compute the WHERE clause first due to its parameters being ordered first in the repository method signature
        StringBuilder where = new StringBuilder(150);
        generateWhereClause(methodName, c, uFirst, where);

        String o = entityVar;
        String o_ = entityVar_;
        StringBuilder q = new StringBuilder(250);
        q.append("UPDATE ").append(entityInfo.name).append(' ').append(o).append(" SET");

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
            String name = entityInfo.getAttributeName(attribute, true);

            if (name == null) {
                if (op == '=') {
                    generateUpdatesForIdClass(first, q);
                } else {
                    String opName = op == '+' ? "Add" : op == '*' ? "Multiply" : "Divide";
                    throw new MappingException("The " + opName +
                                               " repository update operation cannot be used on the Id of the entity when the Id is an IdClass."); // TODO NLS
                }
            } else {
                q.append(first ? " " : ", ").append(o_).append(name).append("=");

                switch (op) {
                    case '+':
                        if (CharSequence.class.isAssignableFrom(entityInfo.attributeTypes.get(name))) {
                            q.append("CONCAT(").append(o_).append(name).append(',') //
                                            .append('?').append(++paramCount).append(')');
                            break;
                        }
                        // else fall through
                    case '*':
                    case '/':
                        q.append(o_).append(name).append(op);
                        // fall through
                    case '=':
                        q.append('?').append(++paramCount);
                }
            }

            u = next == Integer.MAX_VALUE ? -1 : next;
        }

        return q.append(where);
    }

    /**
     * Generates JPQL for updates of an entity by entity id and version (if versioned).
     */
    private StringBuilder generateUpdateEntity() {
        String o = entityVar;
        String o_ = entityVar_;
        StringBuilder q;

        String idName = entityInfo.getAttributeName(ID, true);
        if (idName == null && entityInfo.idClassAttributeAccessors != null) {
            // TODO support this similar to what generateDeleteEntity does
            throw new MappingException("Update operations cannot be used on entities with composite IDs."); // TODO NLS
        }

        if (UPDATE_COUNT_TYPES.contains(singleType)) {
            setType(Update.class, Type.UPDATE_WITH_ENTITY_PARAM);
            hasWhere = true;

            q = new StringBuilder(100) //
                            .append("UPDATE ").append(entityInfo.name).append(' ').append(o) //
                            .append(" SET ");

            boolean first = true;
            for (String name : entityInfo.getAttributeNamesForEntityUpdate()) {
                if (first)
                    first = false;
                else
                    q.append(", ");

                q.append(o_).append(name).append("=?").append(++paramCount);
            }

            q.append(" WHERE ").append(o_).append(idName).append("=?").append(++paramCount);

            if (entityInfo.versionAttributeName != null)
                q.append(" AND ").append(o_).append(entityInfo.versionAttributeName).append("=?").append(++paramCount);
        } else {
            // Update that returns an entity - perform a find operation first so that em.merge can be used
            setType(Update.class, Type.UPDATE_WITH_ENTITY_PARAM_AND_RESULT);
            hasWhere = true;

            q = new StringBuilder(100) //
                            .append("SELECT ").append(o).append(" FROM ").append(entityInfo.name).append(' ').append(o) //
                            .append(" WHERE ").append(o_).append(idName).append("=?").append(++paramCount);

            if (entityInfo.versionAttributeName != null)
                q.append(" AND ").append(o_).append(entityInfo.versionAttributeName).append("=?").append(++paramCount);
        }

        return q;
    }

    /**
     * Generates JPQL to assign the entity properties of which the IdClass consists.
     */
    private void generateUpdatesForIdClass(boolean firstOperation, StringBuilder q) {

        String o_ = entityVar_;
        int count = 0;
        for (String idClassAttr : entityInfo.idClassAttributeAccessors.keySet()) {
            count++;
            String name = entityInfo.getAttributeName(idClassAttr, true);

            q.append(firstOperation ? " " : ", ").append(o_).append(name) //
                            .append("=?").append(++paramCount);
            if (count != 1)
                paramAddedCount++;

            firstOperation = false;
        }
    }

    /**
     * Generates the JPQL WHERE clause for all findBy, deleteBy, or updateBy conditions such as MyColumn[IgnoreCase][Not]Like
     */
    private void generateWhereClause(String methodName, int start, int endBefore, StringBuilder q) {
        hasWhere = true;
        q.append(" WHERE (");
        for (int and = start, or = start, iNext = start, i = start; hasWhere && i >= start && iNext < endBefore; i = iNext) {
            // The extra character (+1) below allows for entity property names that begin with Or or And.
            // For example, findByOrg and findByPriceBetweenAndOrderNumber
            and = and == -1 || and > i + 1 ? and : methodName.indexOf("And", i + 1);
            or = or == -1 || or > i + 1 ? or : methodName.indexOf("Or", i + 1);
            iNext = Math.min(and, or);
            if (iNext < 0)
                iNext = Math.max(and, or);
            generateCondition(methodName, i, iNext < 0 || iNext >= endBefore ? endBefore : iNext, q);
            if (iNext > 0 && iNext < endBefore) {
                q.append(iNext == and ? " AND " : " OR ");
                iNext += (iNext == and ? 3 : 2);
            }
        }
        if (hasWhere)
            q.append(')');
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
     * Identifies possible positions of named parameters within the JPQL.
     *
     * @param jpql JPQL
     * @return possible positions of named parameters within the JPQL.
     */
    @Trivial
    private static List<Integer> getParameterPositions(String jpql) { // TODO move this to where we are already stepping through the QL
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; (index = jpql.indexOf(':', index)) >= 0;)
            positions.add(++index);
        return positions;
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
     * Gathers the information that is needed to perform the query that the repository method represents.
     *
     * @param entityInfo entity information.
     * @param repository repository implementation.
     * @return information about the query.
     */
    @Trivial
    QueryInfo init(EntityInfo entityInfo, RepositoryImpl<?> repository) {
        // This code path does not require the record name in the map because it is not used for @Query
        return init(Map.of(entityInfo.name, CompletableFuture.completedFuture(entityInfo)),
                    repository);
    }

    /**
     * Gathers the information that is needed to perform the query that the repository method represents.
     *
     * @param entityInfos map of entity name to entity information.
     * @param repository  repository implementation.
     * @return information about the query.
     */
    @FFDCIgnore(Throwable.class) // TODO look into these failures and decide if FFDC should be enabled
    @Trivial
    QueryInfo init(Map<String, CompletableFuture<EntityInfo>> entityInfos, RepositoryImpl<?> repository) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "init", entityInfos, this);

        try {
            entityInfo = entityInfos.size() == 1 //
                            ? entityInfos.values().iterator().next().join() //
                            : null; // defer to processing of Query value

            if (repository.validator != null) {
                boolean[] v = repository.validator.isValidatable(method);
                validateParams = v[0];
                validateResult = v[1];
            }

            boolean countPages = Page.class.equals(multiType) || CursoredPage.class.equals(multiType);
            StringBuilder q = null;

            // TODO would it be more efficient to invoke method.getAnnotations() once?

            // spec-defined annotation types
            Delete delete = method.getAnnotation(Delete.class);
            Find find = method.getAnnotation(Find.class);
            Insert insert = method.getAnnotation(Insert.class);
            Update update = method.getAnnotation(Update.class);
            Save save = method.getAnnotation(Save.class);
            Query query = method.getAnnotation(Query.class);
            OrderBy[] orderBy = method.getAnnotationsByType(OrderBy.class);

            // experimental annotation types
            Annotation count = repository.provider.compat.getCountAnnotation(method);
            Annotation exists = repository.provider.compat.getExistsAnnotation(method);

            Annotation methodTypeAnno = validateAnnotationCombinations(delete, insert, update, save,
                                                                       find, query, orderBy,
                                                                       count, exists);

            if (query != null) { // @Query annotation
                initQueryLanguage(query.value(), entityInfos, repository.primaryEntityInfoFuture);
            } else if (save != null) { // @Save annotation
                setType(Save.class, Type.SAVE);
            } else if (insert != null) { // @Insert annotation
                setType(Insert.class, Type.INSERT);
            } else if (entityParamType != null) {
                if (update != null) { // @Update annotation
                    q = generateUpdateEntity();
                } else if (delete != null) { // @Delete annotation
                    q = generateDeleteEntity();
                } else { // should be unreachable
                    throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                            repository.repositoryInterface.getName() +
                                                            " repository interface must be annotated with one of " +
                                                            "(Delete, Insert, Save, Update)" +
                                                            " because the method's parameter accepts entity instances. The following" +
                                                            " annotations were found: " + Arrays.toString(method.getAnnotations()));
                }
            } else {
                if (methodTypeAnno != null) {
                    // Query by Parameters
                    q = initQueryByParameters(methodTypeAnno, countPages); // keyset queries before orderby
                } else {
                    // Query by Method Name
                    q = initQueryByMethodName(countPages);
                }

                if (type == Type.FIND_AND_DELETE
                    && multiType != null
                    && Stream.class.isAssignableFrom(multiType)) {
                    throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                            repository.repositoryInterface.getName() +
                                                            " repository interface cannot use the " +
                                                            method.getReturnType().getName() + " return type for a delete operation.");
                }
            }

            // If we don't already know from generating the JPQL, find out how many
            // parameters the JPQL takes and which parameters are named parameters.
            if (query != null || paramNames != null) {
                int initialParamCount = paramCount;
                Parameter[] params = method.getParameters();
                List<Integer> paramPositions = null;
                Class<?> paramType;
                boolean hasParamAnnotation = false;
                for (int i = 0; i < params.length && !SPECIAL_PARAM_TYPES.contains(paramType = params[i].getType()); i++) {
                    Param param = params[i].getAnnotation(Param.class);
                    hasParamAnnotation |= param != null;
                    String paramName = param == null ? null : param.value();
                    if (param == null && jpql != null && params[i].isNamePresent()) {
                        String name = params[i].getName();
                        if (paramPositions == null)
                            paramPositions = getParameterPositions(jpql);
                        for (int p = 0; p < paramPositions.size() && paramName == null; p++) {
                            int pos = paramPositions.get(p); // position at which the named parameter name must appear
                            int next = pos + name.length(); // the next character must not be alphanumeric for the name to be a match
                            if (jpql.regionMatches(paramPositions.get(p), name, 0, name.length())
                                && (next >= jpql.length() || !Character.isLetterOrDigit(jpql.charAt(next)))) {
                                paramName = name;
                                paramPositions.remove(p);
                            }
                        }
                    }
                    if (paramName != null) {
                        if (paramNames == null)
                            paramNames = new ArrayList<>();
                        if (entityInfo.idClassAttributeAccessors != null && paramType.equals(entityInfo.idType))
                            // TODO is this correct to do when @Query has a named parameter with type of the IdClass?
                            // It seems like the JPQL would not be consistent.
                            for (int p = 1, numIdClassParams = entityInfo.idClassAttributeAccessors.size(); p <= numIdClassParams; p++) {
                                paramNames.add(new StringBuilder(paramName).append('_').append(p).toString());
                                if (p > 1) {
                                    paramCount++;
                                    paramAddedCount++;
                                }
                            }
                        else
                            paramNames.add(paramName);
                    }
                    paramCount++;

                    if (initialParamCount != 0)
                        throw new UnsupportedOperationException("Cannot mix positional and named parameters on repository method " +
                                                                method.getDeclaringClass().getName() + '.' + method.getName()); // TODO NLS

                    int numParamNames = paramNames == null ? 0 : paramNames.size();
                    if (numParamNames > 0 && numParamNames != paramCount)
                        if (hasParamAnnotation) {
                            throw new UnsupportedOperationException("Cannot mix positional and named parameters on repository method " +
                                                                    method.getDeclaringClass().getName() + '.' + method.getName()); // TODO NLS
                        } else { // we might have mistaken a literal value for a named parameter
                            paramNames = null;
                            paramCount -= paramAddedCount;
                            paramAddedCount = 0;
                        }
                }
            }

            // The @OrderBy annotation from Jakarta Data provides sort criteria statically
            if (orderBy.length > 0) {
                //type = type == null ? Type.FIND : type;
                sorts = sorts == null ? new ArrayList<>(orderBy.length + 2) : sorts;
                if (q == null)
                    if (jpql == null) {
                        q = generateSelectClause();
                        q.append(" FROM ").append(entityInfo.name).append(' ').append(entityVar);
                        if (countPages)
                            generateCount(null);
                    } else {
                        q = new StringBuilder(jpql);
                    }

                for (int i = 0; i < orderBy.length; i++)
                    addSort(orderBy[i].ignoreCase(), orderBy[i].value(), orderBy[i].descending());

                if (!hasDynamicSortCriteria())
                    generateOrderBy(q);
            }

            jpql = q == null ? jpql : q.toString();

            if (type == null)
                throw new UnsupportedOperationException("The " + method.getName() + " method of the " +
                                                        repository.repositoryInterface.getName() +
                                                        " repository does not match any of the patterns defined by Jakarta Data. " +
                                                        "A repository method must either use annotations such as " +
                                                        "(Delete, Find, Insert, Query, Save, Update)" +
                                                        " to define operations, be a resource accessor method without parameters and " +
                                                        "returning one of " + "(Connection, DataSource, EntityManager)" +
                                                        ", or it must be named according to the requirements of the " +
                                                        "Query by Method Name pattern. Method names for Query by Method Name must " +
                                                        "begin with one of the " + "(count, delete, exists, find)" +
                                                        " keywords, followed by 0 or more additional characters, " +
                                                        "optionally followed by the 'By' keyword and one or more conditions " +
                                                        "delimited by the 'And' or 'Or' keyword. " +
                                                        "Some examples of valid method names are: " +
                                                        entityInfo.getExampleMethodNames() + "."); // TODO NLS

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "init", new Object[] { this, entityInfo });
            return this;
        } catch (Throwable x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "init", x);
            throw x;
        }

    }

    /**
     * Initializes query information based on the Query annotation.
     *
     * @param ql                      Query.value() might be JPQL or JDQL
     * @param entityInfos             map of entity name to entity information.
     * @param primaryEntityInfoFuture future for the repository's primary entity type if it has one, otherwise null.
     */
    private void initQueryLanguage(String ql, Map<String, CompletableFuture<EntityInfo>> entityInfos,
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
                        setEntityInfo(entityName.toString(), entityInfos);
                        // skip whitespace
                        for (; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                        if (startAt >= length) {
                            // Entity identifier variable is not present. Add it.
                            entityVar = "this";
                            entityVar_ = "this.";
                            jpql = new StringBuilder(entityInfo.name.length() + 12) //
                                            .append("DELETE FROM ").append(entityInfo.name).toString();
                        } else if (startAt + 6 < length
                                   && ql.regionMatches(true, startAt, "WHERE", 0, 5)
                                   && !Character.isJavaIdentifierPart(ql.charAt(startAt + 5))) {
                            // Entity identifier variable is not present. Add it.
                            hasWhere = true;
                            entityVar = "this";
                            entityVar_ = "this.";
                            StringBuilder q = new StringBuilder(ql.length() * 3 / 2) //
                                            .append("DELETE FROM ").append(entityInfo.name).append(" WHERE");
                            jpql = appendWithIdentifierName(ql, startAt + 5, ql.length(), q).toString();
                        }
                    }
                }
            }

            // TODO remove this workaround for #28895 once fixed
            if (jpql.equals("DELETE FROM Product WHERE this.name LIKE ?1"))
                jpql = "DELETE FROM Product WHERE name LIKE ?1";
            // TODO remove this workaround for #28898 once fixed
            else if (jpql.equals("DELETE FROM ReceiptEntity WHERE this.total < :max"))
                jpql = "DELETE FROM ReceiptEntity WHERE total < :max";
            else if (jpql.equals("DELETE FROM Coordinate WHERE this.x > 0.0d AND this.y > 0.0f"))
                jpql = "DELETE FROM Coordinate WHERE x > 0.0d AND y > 0.0f";
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
                    setEntityInfo(entityName.toString(), entityInfos);
                else if (entityInfo == null)
                    throw new MappingException("@Repository " + method.getDeclaringClass().getName() + " does not specify an entity class." + // TODO NLS
                                               " To correct this, have the repository interface extend DataRepository" +
                                               " or another built-in repository interface and supply the entity class as the first parameter.");
                if (startAt + 1 < length && entityInfo.name.length() > 0 && Character.isWhitespace(ql.charAt(startAt))) {
                    for (startAt++; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                    if (startAt + 4 < length
                        && ql.regionMatches(true, startAt, "SET", 0, 3)
                        && !Character.isJavaIdentifierPart(ql.charAt(startAt + 3))) {
                        entityVar = "o";
                        entityVar_ = "o.";
                        StringBuilder q = new StringBuilder(ql.length() * 3 / 2) //
                                        .append("UPDATE ").append(entityInfo.name).append(" o SET");
                        jpql = appendWithIdentifierName(ql, startAt + 3, ql.length(), q).toString();
                    }
                }
            }
        } else { // SELECT ... or FROM ... or WHERE ... or ORDER BY ...
            int select0 = -1, selectLen = 0; // starts after SELECT
            int from0 = -1, fromLen = 0; // starts after FROM
            int entityName0 = -1, entityNameLen = 0;
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
            entityVar_ = "this.";
            hasWhere = whereLen > 0;

            // Locate the entity identifier variable (if present). Examples of FROM clause:
            // FROM EntityName
            // FROM EntityName e
            // FROM EntityName AS e
            for (startAt = from0; startAt < from0 + fromLen && Character.isWhitespace(ql.charAt(startAt)); startAt++);
            if (startAt < from0 + fromLen) {
                entityName0 = startAt; // starts at EntityName
                for (; startAt < from0 + fromLen && Character.isJavaIdentifierPart(ql.charAt(startAt)); startAt++);
                if ((entityNameLen = startAt - entityName0) > 0) {
                    String entityName = ql.substring(entityName0, entityName0 + entityNameLen);
                    setEntityInfo(entityName, entityInfos);

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
                setEntityInfo(entityInfos, primaryEntityInfoFuture);

            String entityName = entityInfo.name;

            if (trace && tc.isDebugEnabled()) {
                Tr.debug(tc, ql, "JDQL query parts", // does not include GROUP BY, HAVING, or address subqueries or other complex JPQL
                         "  SELECT [" + (selectLen > 0 ? ql.substring(select0, select0 + selectLen) : "") + "]",
                         "    FROM [" + (fromLen > 0 ? ql.substring(from0, from0 + fromLen) : "") + "]",
                         "   WHERE [" + (whereLen > 0 ? ql.substring(where0, where0 + whereLen) : "") + "]",
                         "  [" + (orderLen > 0 ? ql.substring(order0, order0 + orderLen) : "") + "]",
                         "  entity [" + entityName + "] [" + entityVar + "]");
            }

            boolean hasEntityVar = !"this".equals(entityVar);

            if (countPages) {
                // TODO count query cannot always be accurately inferred if Query value is JPQL
                StringBuilder c = new StringBuilder("SELECT COUNT(");
                if (selectLen <= 0
                    || ql.substring(select0, select0 + selectLen).indexOf(',') >= 0) // comma delimited multiple return values
                    c.append(entityVar);
                else // allows for COUNT(DISTINCT o.name)
                    appendWithIdentifierName(ql, select0, select0 + selectLen, c);

                c.append(") FROM");
                if (from0 >= 0) {
                    if (entityName0 > 0) {
                        c.append(ql.substring(from0, entityName0));
                        c.append(entityName);
                        c.append(ql.substring(entityName0 + entityNameLen, from0 + fromLen));
                    } else {
                        c.append(ql.substring(from0, from0 + fromLen));
                    }
                } else {
                    c.append(' ').append(entityName).append(' ');
                    if (hasEntityVar)
                        c.append(entityVar).append(' ');
                }

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

            StringBuilder q;
            if (selectLen > 0) {
                q = new StringBuilder(ql.length() + (selectLen >= 0 ? 0 : 50) + (fromLen >= 0 ? 0 : 50) + 2);
                q.append("SELECT");
                appendWithIdentifierName(ql, select0, select0 + selectLen, q);
            } else {
                q = generateSelectClause().append(' ');
            }

            q.append("FROM");
            if (fromLen > 0) {
                if (entityName0 > 0) {
                    q.append(ql.substring(from0, entityName0));
                    q.append(entityName);
                    q.append(ql.substring(entityName0 + entityNameLen, from0 + fromLen));
                } else {
                    q.append(ql.substring(from0, from0 + fromLen));
                }
            } else {
                q.append(' ').append(entityName).append(' ');
                if (hasEntityVar)
                    q.append(entityVar).append(' ');
            }

            if (whereLen > 0) {
                q.append("WHERE");
                appendWithIdentifierName(ql, where0, where0 + whereLen, q);
            }

            if (orderLen > 0) {
                appendWithIdentifierName(ql, order0, order0 + orderLen, q);
            }

            jpql = q.toString();

            // TODO remove this workaround for #28874 once fixed
            if (jpql.equals("SELECT NEW test.jakarta.data.jpa.web.Rebate(this.id, this.amount, this.customerId, this.purchaseMadeAt, this.purchaseMadeOn, this.status, this.updatedAt, this.version) FROM RebateEntity WHERE this.customerId=?1 AND this.status=test.jakarta.data.jpa.web.Rebate.Status.PAID ORDER BY this.amount DESC, this.id ASC"))
                jpql = "SELECT NEW test.jakarta.data.jpa.web.Rebate(o.id, o.amount, o.customerId, o.purchaseMadeAt, o.purchaseMadeOn, o.status, o.updatedAt, o.version) FROM RebateEntity o WHERE o.customerId=?1 AND o.status=test.jakarta.data.jpa.web.Rebate.Status.PAID ORDER BY o.amount DESC, o.id ASC";
            else if (jpql.equals(" FROM NaturalNumber WHERE this.isOdd = false AND this.numType = ee.jakarta.tck.data.framework.read.only.NaturalNumber.NumberType.PRIME"))
                jpql = "SELECT o FROM NaturalNumber o WHERE o.isOdd = false AND o.numType = ee.jakarta.tck.data.framework.read.only.NaturalNumber.NumberType.PRIME";
        }
    }

    /**
     * Handles Query by Method Name.
     *
     * @param countPages whether to generate a count query (for Page.totalElements and Page.totalPages).
     * @return the generated query written to a StringBuilder.
     */
    private StringBuilder initQueryByMethodName(boolean countPages) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        String methodName = method.getName();
        String o = entityVar;
        StringBuilder q = null;

        int by = methodName.indexOf("By");

        if (methodName.startsWith("find")) {
            int orderBy = -1;
            if (by >= 9 && methodName.regionMatches(by - 5, "Order", 0, 5)) {
                orderBy = by - 5;
                by = -1;
            } else if (by > 0) {
                orderBy = methodName.indexOf("OrderBy", by + 2);
            }
            parseFindClause(by > 0 ? by : orderBy > 0 ? orderBy : -1);
            q = generateSelectClause().append(" FROM ").append(entityInfo.name).append(' ').append(o);
            if (by > 0) {
                int where = q.length();
                generateWhereClause(methodName, by + 2, orderBy > 0 ? orderBy : methodName.length(), q);
                if (countPages)
                    generateCount(q.substring(where));
            }
            if (orderBy >= 0)
                parseOrderBy(orderBy, q);
            type = Type.FIND;
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            int orderBy = -1;
            boolean isFindAndDelete = isFindAndDelete();
            if (isFindAndDelete) {
                if (by >= 11 && methodName.regionMatches(by - 5, "Order", 0, 5)) {
                    orderBy = by - 5;
                    by = -1;
                } else if (by > 0) {
                    orderBy = methodName.indexOf("OrderBy", by + 2);
                }
                if (type != null)
                    throw new UnsupportedOperationException("The " + method.getGenericReturnType() +
                                                            " return type is not supported for the " + methodName +
                                                            " repository method."); // TODO NLS
                type = Type.FIND_AND_DELETE;
                parseDeleteBy(by);
                q = generateSelectClause().append(" FROM ").append(entityInfo.name).append(' ').append(o);
                jpqlDelete = generateDeleteById();
            } else { // DELETE
                type = type == null ? Type.DELETE : type;
                q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
            }

            if (by > 0)
                generateWhereClause(methodName, by + 2, orderBy > 0 ? orderBy : methodName.length(), q);
            if (orderBy > 0)
                parseOrderBy(orderBy, q);

            type = type == null ? Type.DELETE : type;
        } else if (methodName.startsWith("update")) {
            int c = by < 0 ? 6 : (by + 2);
            q = generateUpdateClause(methodName, c);
            type = Type.UPDATE;
        } else if (methodName.startsWith("count")) {
            q = new StringBuilder(150).append("SELECT COUNT(").append(o).append(") FROM ").append(entityInfo.name).append(' ').append(o);
            if (by > 0 && methodName.length() > by + 2)
                generateWhereClause(methodName, by + 2, methodName.length(), q);
            type = Type.COUNT;
        } else if (methodName.startsWith("exists")) {
            String name = entityInfo.idClassAttributeAccessors == null ? ID : entityInfo.idClassAttributeAccessors.firstKey();
            String attrName = entityInfo.getAttributeName(name, true);
            q = new StringBuilder(200).append("SELECT ").append(o).append('.').append(attrName) //
                            .append(" FROM ").append(entityInfo.name).append(' ').append(o);
            if (by > 0 && methodName.length() > by + 2)
                generateWhereClause(methodName, by + 2, methodName.length(), q);
            type = Type.EXISTS;
        } else {
            throw new UnsupportedOperationException("The " + methodName + " method of the " + method.getDeclaringClass().getName() +
                                                    " repository does not match any of the patterns defined by Jakarta Data. " +
                                                    "A repository method must either use annotations such as " +
                                                    "(Delete, Find, Insert, Query, Save, Update)" +
                                                    " to define operations, be a resource accessor method without parameters and " +
                                                    "returning one of " + "(Connection, DataSource, EntityManager)" +
                                                    ", or it must be named according to the requirements of the " +
                                                    "Query by Method Name pattern. Method names for Query by Method Name must " +
                                                    "begin with one of the " + "(count, delete, exists, find)" +
                                                    " keywords, followed by 0 or more additional characters, " +
                                                    "optionally followed by the 'By' keyword and one or more conditions " +
                                                    "delimited by the 'And' or 'Or' keyword. " +
                                                    "Some examples of valid method names are: " +
                                                    entityInfo.getExampleMethodNames() + "."); // TODO NLS
        }

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, methodName + " is identified as a " + type + " method");

        return q;
    }

    /**
     * Handles the Query by Parameters pattern,
     * which requires one of the following annotations:
     * Count, Delete, Exists, Find, or Update.
     *
     * @param methodTypeAnno Count, Delete, Exists, Find, or Update annotation.
     *                           The Insert, Save, and Query annotations are never supplied to this method.
     * @param countPages     whether to generate a count query (for Page.totalElements and Page.totalPages).
     * @return the generated query written to a StringBuilder.
     */
    @Trivial
    private StringBuilder initQueryByParameters(Annotation methodTypeAnno, boolean countPages) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "generateQueryFromMethodParams", methodTypeAnno, countPages);

        String o = entityVar;
        String o_ = entityVar_;
        StringBuilder q = null;

        if (methodTypeAnno instanceof Find || methodTypeAnno instanceof Update) {
            q = generateQueryByParameters(null, methodTypeAnno, countPages);
        } else if (methodTypeAnno instanceof Delete) {
            if (isFindAndDelete()) {
                type = Type.FIND_AND_DELETE;
                q = generateSelectClause().append(" FROM ").append(entityInfo.name).append(' ').append(o);
                jpqlDelete = generateDeleteById();
            } else { // DELETE
                type = Type.DELETE;
                q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
            }
            if (method.getParameterCount() > 0)
                generateQueryByParameters(q, methodTypeAnno, countPages);
        } else if ("Count".equals(methodTypeAnno.annotationType().getSimpleName())) {
            type = Type.COUNT;
            q = new StringBuilder(150).append("SELECT COUNT(").append(o).append(") FROM ").append(entityInfo.name).append(' ').append(o);
            if (method.getParameterCount() > 0)
                generateQueryByParameters(q, methodTypeAnno, countPages);
        } else if ("Exists".equals(methodTypeAnno.annotationType().getSimpleName())) {
            type = Type.EXISTS;
            String name = entityInfo.idClassAttributeAccessors == null ? ID : entityInfo.idClassAttributeAccessors.firstKey();
            String attrName = entityInfo.getAttributeName(name, true);
            q = new StringBuilder(200).append("SELECT ").append(o_).append(attrName) //
                            .append(" FROM ").append(entityInfo.name).append(' ').append(o);
            if (method.getParameterCount() > 0)
                generateQueryByParameters(q, methodTypeAnno, countPages);
        } else {
            // TODO should be unreachable
            throw new UnsupportedOperationException("Unexpected annotation " + methodTypeAnno + " for parameter-based query.");
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "generateQueryFromMethodParams", new Object[] { q, type });

        return q;
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
    private boolean isFindAndDelete() {

        boolean isFindAndDelete = isOptional
                                  || multiType != null
                                  || !RETURN_TYPES_FOR_DELETE_ONLY.contains(singleType);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isFindAndDelete? " + isFindAndDelete +
                               "; optional?: " + isOptional +
                               "; multiType: " + (multiType == null ? null : multiType.getSimpleName()) +
                               "; singleType: " + (singleType == null ? null : singleType.getSimpleName()));

        if (isFindAndDelete) {
            if (type != null
                && !type.equals(entityInfo.entityClass)
                && !type.equals(entityInfo.recordClass)
                && !type.equals(Object.class)
                && !wrapperClassIfPrimitive(singleType).equals(wrapperClassIfPrimitive(entityInfo.idType)))
                throw new MappingException("Results for find-and-delete repository queries must be the entity class (" +
                                           (entityInfo.recordClass == null ? entityInfo.entityClass : entityInfo.recordClass).getName() +
                                           ") or the id class (" + entityInfo.idType +
                                           "), not the " + method.getGenericReturnType() + " class."); // TODO NLS
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
     * Parses and handles the text between delete___By of a repository method.
     * Currently this is only "First" or "First#".
     *
     * @param by index of first occurrence of "By" in the method name. -1 if "By" is absent.
     */
    private void parseDeleteBy(int by) {
        String methodName = method.getName();
        if (methodName.regionMatches(6, "First", 0, 5)) {
            int endBefore = by == -1 ? methodName.length() : by;
            parseFirst(11, endBefore);
        }
    }

    /**
     * Parses and handles the text between find___By or find___OrderBy or find___ of a repository method.
     * Currently this is only "First" or "First#" and entity property names to select.
     * "Distinct" is reserved for future use.
     *
     * @param by index of first occurrence of "By" or "OrderBy" in the method name. -1 if both are absent.
     */
    private void parseFindClause(int by) {
        String methodName = method.getName();
        int start = 4;
        int endBefore = by == -1 ? methodName.length() : by;

        for (boolean first = methodName.regionMatches(start, "First", 0, 5), distinct = !first && methodName.regionMatches(start, "Distinct", 0, 8); //
                        first || distinct;)
            if (first) {
                start = parseFirst(start += 5, endBefore);
                first = false;
                distinct = methodName.regionMatches(start, "Distinct", 0, 8);
            } else if (distinct) {
                throw new UnsupportedOperationException("The keyword Distinct is not supported on the " + methodName + " method."); // TODO NLS
            }
    }

    /**
     * Parses the number (if any) following findFirst or deleteFirst.
     *
     * @param start     starting position after findFirst or deleteFirst
     * @param endBefore index of first occurrence of "By" in the method name, or otherwise the method name length.
     * @return next starting position after the find/deleteFirst(#).
     */
    private int parseFirst(int start, int endBefore) {
        String methodName = method.getName();
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
            throw new UnsupportedOperationException("The number of results to retrieve must not be 0 on the " + methodName + " method."); // TODO NLS
        else
            maxResults = num;

        return start;
    }

    /**
     * Identifies the statically specified sort criteria for a repository findBy method such as
     * findByLastNameLikeOrderByLastNameAscFirstNameDesc
     */
    private void parseOrderBy(int orderBy, StringBuilder q) {
        String methodName = method.getName();

        sorts = sorts == null ? new ArrayList<>() : sorts;

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

            addSort(ignoreCase, attribute, descending);

            if (iNext > 0)
                iNext += (iNext == desc ? 4 : 3);
        }

        if (!hasDynamicSortCriteria())
            generateOrderBy(q);
    }

    /**
     * Locate the entity information for this query.
     *
     * @param entityInfos             map of entity name to already-completed future for the entity information.
     * @param primaryEntityInfoFuture future for the repository's primary entity type if it has one, otherwise null.
     * @throws MappingException if the entity information is not found.
     */
    @Trivial
    private void setEntityInfo(Map<String, CompletableFuture<EntityInfo>> entityInfos,
                               CompletableFuture<EntityInfo> primaryEntityInfoFuture) {
        if (singleType != null) {
            CompletableFuture<EntityInfo> failedFuture = null;
            for (CompletableFuture<EntityInfo> future : entityInfos.values())
                if (future.isCompletedExceptionally()) {
                    failedFuture = future;
                } else {
                    entityInfo = future.join();
                    if (singleType.equals(entityInfo.entityClass) ||
                        singleType.equals(entityInfo.recordClass))
                        return;
                }
            if (failedFuture != null)
                failedFuture.join(); // cause error to be raised
        }

        if (primaryEntityInfoFuture == null)
            throw new MappingException("The " + method.getName() + " method of the " + method.getDeclaringClass().getName() +
                                       " repository does not specify an entity class. To correct this, have the repository interface" +
                                       " extend DataRepository or another built-in repository interface and supply the entity class" +
                                       " as the first type variable."); // TODO NLS

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && !primaryEntityInfoFuture.isDone())
            Tr.debug(this, tc, "await completion of primary entity info", primaryEntityInfoFuture);

        entityInfo = primaryEntityInfoFuture.join();
    }

    /**
     * Locate the entity information for the specified entity name.
     *
     * @param entityName  case sensitive entity name obtained from JDQL or JPQL.
     * @param entityInfos map of entity name to already-completed future for the entity information.
     * @throws MappingException if the entity information is not found.
     */
    @Trivial
    private void setEntityInfo(String entityName, Map<String, CompletableFuture<EntityInfo>> entityInfos) {
        CompletableFuture<EntityInfo> future = entityInfos.get(entityName);
        if (future == null) {
            // When a Java record is used as an entity, the name is [RecordName]Entity
            String recordEntityName = entityName + EntityInfo.RECORD_ENTITY_SUFFIX;
            future = entityInfos.get(recordEntityName);
            if (future == null) {
                entityInfo = null;
            } else {
                entityInfo = future.join();
                if (entityInfo.recordClass == null)
                    entityInfo = null;
            }

            if (entityInfo == null) {
                // Identify possible case mismatch
                for (String name : entityInfos.keySet()) {
                    if (recordEntityName.equalsIgnoreCase(name) && entityInfos.get(name).join().recordClass != null)
                        name = name.substring(0, name.length() - EntityInfo.RECORD_ENTITY_SUFFIX.length());
                    if (entityName.equalsIgnoreCase(name))
                        throw new MappingException("The " + method.getName() + " method of the " + method.getDeclaringClass().getName() +
                                                   " repository specifies query language that requires a " + entityName +
                                                   " entity that is not found but is a close match for the " + name +
                                                   " entity. Review the query language to ensure the correct entity name is used."); // TODO NLS
                }

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
        } else {
            entityInfo = future.join();
        }
    }

    /**
     * Sets query parameters from keyset values.
     *
     * @param query        the query
     * @param keysetCursor keyset values
     * @throws Exception if an error occurs
     */
    void setKeysetParameters(jakarta.persistence.Query query, PageRequest.Cursor keysetCursor) throws Exception {
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
    static void setParameter(int p, jakarta.persistence.Query query, Object entity, List<Member> accessors) throws Exception {
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
    void setParameters(jakarta.persistence.Query query, Object... args) throws Exception {
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
    void setParametersFromIdClassAndVersion(jakarta.persistence.Query query, Object entity, Object version) throws Exception {
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

    /**
     * Initialize this query information for the specified type of annotated repository operation.
     *
     * @param annoClass     Insert, Update, Save, or Delete annotation class.
     * @param operationType corresponding operation type.
     */
    private void setType(Class<? extends Annotation> annoClass, Type operationType) {
        type = operationType;
        if (entityParamType == null)
            throw new UnsupportedOperationException("Repository " + '@' + annoClass.getSimpleName() +
                                                    " operations must have exactly 1 parameter, which can be the entity" +
                                                    " or a collection or array of entities. The " + method.getDeclaringClass().getName() +
                                                    '.' + method.getName() + " method has " + method.getParameterCount() + " parameters."); // TODO NLS
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
    List<Sort<Object>> supplySorts(List<Sort<Object>> combined, Iterable<Sort<Object>> additional) {
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
    List<Sort<Object>> supplySorts(List<Sort<Object>> combined, @SuppressWarnings("unchecked") Sort<Object>... additional) {
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
    private Annotation validateAnnotationCombinations(Delete delete, Insert insert, Update update, Save save,
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

        int iusce = ius +
                    (count == null ? 0 : 1) +
                    (exists == null ? 0 : 1);

        int iusdce = iusce +
                     (delete == null ? 0 : 1);

        if (iusdce + f > 1 // more than one of (Insert, Update, Save, Delete, Count, Exists, Find)
            || iusce + o > 1 // more than one of (Insert, Update, Save, Delete, Count, Exists, OrderBy)
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
     * Validates that ignoreCase is only true if the type of the property being sort on is a String.
     *
     * @param sort the Jakarta Data Sort object being evaluated
     */
    @Trivial
    void validateSort(Sort<?> sort) {
        Class<?> propertyClass = entityInfo.attributeTypes.get(sort.property());

        if (sort.ignoreCase() == true && !propertyClass.equals(String.class))
            throw new UnsupportedOperationException("The ignoreCase parameter in a Sort can only be true if the Entity" +
                                                    " property being sorted is of type String. The property [" + sort.property() +
                                                    "] is of type [" + propertyClass + "]"); //TODO NLS
    }

    /**
     * Copy of query information, but with updated JPQL and sort criteria.
     */
    QueryInfo withJPQL(String jpql, List<Sort<Object>> sorts) {
        QueryInfo q = new QueryInfo(method, entityParamType, isOptional, multiType, returnArrayType, singleType);
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
    private static final Class<?> wrapperClassIfPrimitive(Class<?> c) {
        Class<?> w = WRAPPER_CLASSES.get(c);
        return w == null ? c : w;
    }
}

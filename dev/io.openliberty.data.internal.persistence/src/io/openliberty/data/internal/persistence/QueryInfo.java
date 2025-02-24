/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
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

import static io.openliberty.data.internal.persistence.Util.SORT_PARAM_TYPES;
import static io.openliberty.data.internal.persistence.Util.lifeCycleReturnTypes;
import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;
import static jakarta.data.repository.By.ID;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
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
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

/**
 * Query information.
 */
public class QueryInfo {
    private static final TraceComponent tc = Tr.register(QueryInfo.class);

    public static enum Type {
        COUNT(false), // query method count
        EXISTS(false), // query method exists
        FIND(false), // query method find/@Find/@Query(SELECT/FROM/WHERE)
        FIND_AND_DELETE(true), // query method delete/@Delete with entity result
        INSERT(true), // life cycle @Insert
        LC_DELETE(true), // life cycle @Delete
        LC_UPDATE(true), // life cycle @Update
        LC_UPDATE_RET_ENTITY(true), // life cycle @Update with entity result
        QM_DELETE(true), // query method delete/@Delete/@Query(DELETE)
        QM_UPDATE(true), // query method update/@Update/@Query(UPDATE)
        RESOURCE_ACCESS(false), // resource accessor method
        SAVE(true); // life cycle @Save

        final boolean requiresTransaction;

        private Type(boolean requiresTransaction) {
            this.requiresTransaction = requiresTransaction;
        }
    }

    /**
     * Placeholder that indicates the entity class needs to be determined
     * based on the content of the Query value.
     */
    public static final Class<?> ENTITY_TBD = Query.class;

    /**
     * Indicates the repository method has no Sort, Sort[], or Order parameters
     * for dynamic sort criteria and also does not define any static sort criteria.
     */
    private static final int[] NONE = new int[0];

    /**
     * Indicates the repository method has no Sort, Sort[], or Order parameters
     * for dynamic sort criteria, but has a Query annotation that might define
     * static sort criteria.
     */
    private static final int[] NONE_QUERY_LANGUAGE_ONLY = new int[0];

    /**
     * Indicates the repository method has no Sort, Sort[], or Order parameters
     * for dynamic sort criteria, but supplies static sort criteria via the
     * OrderBy annotation or keyword.
     */
    private static final int[] NONE_STATIC_SORT_ONLY = new int[0];

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
     * Otherwise "this". "o" is used as the default in generated queries.
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
     * JPQL for a find query after a cursor. Otherwise null.
     */
    String jpqlAfterCursor;

    /**
     * JPQL for a find query before a cursor. Otherwise null.
     */
    String jpqlBeforeCursor;

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
     * Number of parameters to the JPQL query.
     */
    int jpqlParamCount;

    /**
     * Names of named parameters in query language, ordered according to the
     * position in which each appears as a repository method parameter.
     * Repository method parameters identify the name with the
     * <code>Param</code> annotation if present, or otherwise by the
     * name of the parameter (if the -parameters compiler option is enabled).
     * The empty set value is used when the field has not been initialized yet
     * or the query has no parameters or has positional parameters (?1, ?2, ...)
     * rather than named parameters.
     */
    private Set<String> jpqlParamNames = Collections.emptySet();

    /**
     * Value from findFirst#By, or 1 for findFirstBy, otherwise 0.
     */
    int maxResults;

    /**
     * Repository method to which this query information pertains.
     */
    public final Method method;

    /**
     * The type of data structure that returns multiple results for this query.
     * Null if the query return type limits to single results.
     */
    final Class<?> multiType;

    /**
     * The interface that is annotated with @Repository.
     */
    final Class<?> repositoryInterface;

    /**
     * Array element type if the repository method returns an array, such as,
     * <code>Product[] findByNameLike(String namePattern);</code>
     * or if its parameterized type is an array, such as,
     * <code>CompletableFuture&lt;Product[]&gt; findByNameLike(String namePattern);</code>
     * Otherwise null.
     */
    final Class<?> returnArrayType;

    /**
     * The type of a single result obtained by the query. For example,
     * A query that returns List<MyEntity> has singleType MyEntity.
     * A query that returns List<ArrayList<String>> has singleType ArrayList<String>.
     * A query that returns Optional<String[]> has singleType String[].
     */
    final Class<?> singleType;

    /**
     * Element type of singleType when singleType is an array or collection.
     * Null if singleType is not an array or collection. For example,
     * A query that returns List<MyEntity> has singleTypeElementType null.
     * A query that returns List<ArrayList<String>> has singleTypeElementType String.
     * A query that returns Optional<String[]> has singleTypeElementType String.
     */
    final Class<?> singleTypeElementType;

    /**
     * Positions of Sort, Sort[], and Order parameters.
     * When there are no parameters specifying sort criteria dynamically,
     * then the value is one of:
     * NONE
     * NONE_QUERY_LANGUAGE_ONLY
     * NONE_STATIC_SORT_ONLY
     */
    private int[] sortPositions = NONE;

    /**
     * Ordered list of Sort criteria, which can be defined
     * statically via the OrderBy annotation or keyword, or
     * dynamically via Sort parameters to the repository method,
     * or a combination of both static and dynamic.
     * If the Query annotation is used, it will be unknown whether it
     * hard-codes Sort criteria, in which case this field gets set to
     * any additional sort criteria that is added statically or dynamically,
     * or lacking either of those, an empty list.
     * If none of the above, the value of this field is null,
     * which can also mean it has not been initialized yet.
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
     *
     * @param repositoryInterface interface annotated with @Repository.
     * @param method              repository method.
     * @param entityParamType     type of the first parameter if a life cycle method, otherwise null.
     * @param returnArrayType     array element type if the repository method returns an array, otherwise null.
     * @param returnTypeAtDepth   return type of the repository method return value,
     *                                split into levels of depth for each type parameter and array component.
     *                                This is useful in cases such as
     *                                <code>&#64;Query(...) Optional&lt;Float&gt; priceOf(String productId)</code>
     *                                which resolves to { Optional.class, Float.class }
     *                                and
     *                                <code>CompletableFuture&lt;Stream&lt;Product&gt&gt; findByNameLike(String namePattern)</code>
     *                                which resolves to { CompletableFuture.class, Stream.class, Product.class }
     *                                and
     *                                <code>CompletionStage&lt;Product[]&gt; findByNameIgnoreCaseLike(String namePattern)</code>
     *                                which resolves to { CompletionStage.class, Product[].class, Product.class }
     */
    @Trivial
    public QueryInfo(Class<?> repositoryInterface,
                     Method method,
                     Class<?> entityParamType,
                     Class<?> returnArrayType,
                     List<Class<?>> returnTypeAtDepth) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled()) {
            StringBuilder b = new StringBuilder(200) //
                            .append(method.getGenericReturnType().getTypeName()).append(' ') //
                            .append(repositoryInterface.getName()).append('.') //
                            .append(method.getName());
            boolean first = true;
            for (java.lang.reflect.Type p : method.getGenericParameterTypes()) {
                b.append(first ? "(" : ", ").append(p.getTypeName());
                first = false;
            }
            b.append(first ? "()" : ")");
            Tr.entry(this, tc, "<init>", b.toString(), entityParamType, returnArrayType, returnTypeAtDepth);
        }

        this.repositoryInterface = repositoryInterface;
        this.method = method;
        this.entityParamType = entityParamType;
        this.returnArrayType = returnArrayType;

        int d = 0, depth = returnTypeAtDepth.size();
        Class<?> type = returnTypeAtDepth.get(d);
        if (CompletionStage.class.equals(type) || CompletableFuture.class.equals(type))
            if (++d < depth)
                type = returnTypeAtDepth.get(d);
            else
                // TODO add helpful information about supported result types
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1004.general.rtrn.err",
                          method.getGenericReturnType().getTypeName(),
                          method.getName(),
                          repositoryInterface.getName());
        if (isOptional = Optional.class.equals(type)) {
            multiType = null;
            if (++d < depth)
                type = returnTypeAtDepth.get(d);
            else
                // TODO add helpful information about supported result types
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1004.general.rtrn.err",
                          method.getGenericReturnType().getTypeName(),
                          method.getName(),
                          repositoryInterface.getName());
        } else {
            if (returnArrayType != null
                || Iterator.class.equals(type)
                || Iterable.class.isAssignableFrom(type) // includes Page, List, ...
                || BaseStream.class.isAssignableFrom(type)) {
                multiType = type;
                if (++d < depth)
                    type = returnTypeAtDepth.get(d);
                else
                    // TODO add helpful information about supported result types
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1004.general.rtrn.err",
                              method.getGenericReturnType().getTypeName(),
                              method.getName(),
                              repositoryInterface.getName());
            } else {
                multiType = null;
            }
        }

        singleType = type;

        if ((singleType.isArray() || Iterable.class.isAssignableFrom(singleType)) &&
            ++d < depth)
            singleTypeElementType = returnTypeAtDepth.get(d);
        else
            singleTypeElementType = null;

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>", new Object[] { this,
                                                       "result isOptional? " + isOptional,
                                                       "result multiType:  " + multiType,
                                                       "result singleType: " + singleType,
                                                       "          element: " + singleTypeElementType });
    }

    /**
     * Construct partially complete query information.
     */
    public QueryInfo(Class<?> repositoryInterface, Method method, Type type) {
        this.method = method;
        this.entityParamType = null;
        this.multiType = null;
        this.isOptional = false;
        this.repositoryInterface = repositoryInterface;
        this.returnArrayType = null;
        this.singleType = null;
        this.singleTypeElementType = null;
        this.type = type;
    }

    /**
     * Construct a copy of a source QueryInfo, but with different JPQL and sorts.
     *
     * @param source QueryInfo from which to copy.
     * @param jpql   JPQL to use instead of the JPQL from source.
     * @param sorts  Sorts to use instead of the sorts from source.
     */
    private QueryInfo(QueryInfo source, String jpql, List<Sort<Object>> sorts) {
        entityInfo = source.entityInfo;
        entityParamType = source.entityParamType;
        entityVar = source.entityVar;
        entityVar_ = source.entityVar_;
        hasWhere = source.hasWhere;
        isOptional = source.isOptional;
        this.jpql = jpql;
        jpqlAfterCursor = source.jpqlAfterCursor;
        jpqlBeforeCursor = source.jpqlBeforeCursor;
        jpqlCount = source.jpqlCount;
        jpqlDelete = source.jpqlDelete;
        jpqlParamCount = source.jpqlParamCount;
        jpqlParamNames = source.jpqlParamNames;
        maxResults = source.maxResults;
        method = source.method;
        multiType = source.multiType;
        repositoryInterface = source.repositoryInterface;
        returnArrayType = source.returnArrayType;
        singleType = source.singleType;
        singleTypeElementType = source.singleTypeElementType;
        this.sorts = sorts;
        type = source.type;
        validateParams = source.validateParams;
    }

    /**
     * Adds Sort criteria to the end of the tracked list of sort criteria.
     *
     * @param ignoreCase if ordering is to be independent of case.
     * @param attribute  name of attribute (@OrderBy value, Sort property,
     *                       or parsed from OrderBy method name keyword).
     * @param descending if ordering is to be in descending order
     */
    @Trivial
    private void addSort(boolean ignoreCase, String attribute, boolean descending) {
        if (entityInfo.idClassAttributeAccessors == null ||
            !ID.equalsIgnoreCase(attribute)) {
            String name = getAttributeName(attribute, true);

            sorts.add(ignoreCase ? //
                            descending ? //
                                            Sort.descIgnoreCase(name) : //
                                            Sort.ascIgnoreCase(name) : //
                            descending ? //
                                            Sort.desc(name) : //
                                            Sort.asc(name));
        } else {
            // Expand ID(THIS) for composite IdClass into separate attributes
            for (String name : entityInfo.idClassAttributeAccessors.keySet())
                addSort(ignoreCase, name, descending);
        }
    }

    /**
     * Appends the attribute name if it is a function ending with a ')' character.
     * Otherwise appends the attribute name prefixed by the entity identifier
     * variable.
     *
     * @param attrName attribute name, which might be represented as a function
     *                     such as id(o) or version(o)
     * @param q        string builder to which to append
     */
    @Trivial
    private final void appendAttributeName(String attrName, StringBuilder q) {
        if (attrName.charAt(attrName.length() - 1) != ')')
            q.append(entityVar_);
        q.append(attrName);
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
     * @param o_        entity identifier variable followed by the . character.
     * @param q         simulated JPQL to which to append.
     * @return simulated JPQL.
     */
    private StringBuilder appendWithIdentifierName(String ql, int startAt, int endBefore, String o_, StringBuilder q) {
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
            } else if (Character.isJavaIdentifierStart(ch)) {
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
                        String name = getAttributeName(By.ID, true);
                        appendAttributeName(name, q);
                        i += 6;
                    } else if ("this".equalsIgnoreCase(str)
                               || getAttributeName(str, false) == null) {
                        q.append(str);
                    } else {
                        q.append(o_).append(str);
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
     * Compute the zero-based offset to use as a starting point for a Limit range.
     *
     * @param limit limit that was specified by the application.
     * @return offset value.
     * @throws IllegalArgumentException if the starting point for the limited range
     *                                      is not positive or would overflow
     *                                      Integer.MAX_VALUE.
     */
    int computeOffset(Limit range) {
        long startIndex = range.startAt() - 1;
        if (startIndex <= Integer.MAX_VALUE)
            // The Limit constructor disallows values less than 1.
            return (int) startIndex;
        else
            throw exc(IllegalArgumentException.class,
                      "CWWKD1073.offset.exceeds.max",
                      startIndex + 1,
                      range,
                      method.getName(),
                      repositoryInterface.getName(),
                      "Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
    }

    /**
     * Compute the zero-based offset for the start of a page.
     *
     * @param pagination requested pagination.
     * @return offset for the specified page.
     * @throws IllegalArgumentException if the offset exceeds Integer.MAX_VALUE or
     *                                      the PageRequest requests cursor-based
     *                                      pagination.
     */
    int computeOffset(PageRequest pagination) {
        if (pagination.mode() != PageRequest.Mode.OFFSET)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1035.incompat.page.mode",
                      pagination.mode(),
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericReturnType().getTypeName(),
                      CursoredPage.class.getSimpleName());

        int maxPageSize = pagination.size();
        long pageIndex = pagination.page() - 1; // zero-based
        if (Integer.MAX_VALUE / maxPageSize >= pageIndex)
            return (int) (pageIndex * maxPageSize);
        else
            throw exc(IllegalArgumentException.class,
                      "CWWKD1043.offset.exceeds.max",
                      BigInteger.valueOf(pageIndex) //
                                      .multiply(BigInteger.valueOf(maxPageSize)) //
                                      .toString(),
                      pagination,
                      method.getName(),
                      repositoryInterface.getName(),
                      "Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
    }

    /**
     * Converts a value to the type that is required by the repository method
     * return type.
     *
     * @param value              value to convert.
     * @param toType             type to convert to.
     * @param failIfNotConverted whether or not to fail if unable to convert.
     * @return converted value.
     */
    @FFDCIgnore(ArithmeticException.class) // reported to user as chained exception
    @Trivial
    private Object convert(Object value, Class<?> toType, boolean failIfNotConverted) {
        if (value == null) {
            if (toType.isPrimitive())
                throw exc(MappingException.class,
                          "CWWKD1046.result.convert.err",
                          null,
                          method.getName(),
                          repositoryInterface.getName(),
                          method.getGenericReturnType().getTypeName());
            else
                return null;
        }

        Class<?> fromType = value.getClass();
        Exception cause = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "convert " + loggableAppend(fromType.getSimpleName(),
                                                           " (", value, ")")
                               + " to " + toType.getSimpleName());

        if (value instanceof Number &&
            (Util.PRIMITIVE_NUMERIC_TYPES.contains(toType) ||
             Number.class.isAssignableFrom(toType))) {
            // Conversion from one numeric type to another
            try {
                if (BigDecimal.class.equals(fromType)) {
                    BigDecimal v = (BigDecimal) value;
                    if (long.class.equals(toType) ||
                        Long.class.equals(toType)) {
                        return v.longValueExact();
                    } else if (int.class.equals(toType) ||
                               Integer.class.equals(toType)) {
                        return v.intValueExact();
                    } else if (short.class.equals(toType) ||
                               Short.class.equals(toType)) {
                        return v.shortValueExact();
                    } else if (byte.class.equals(toType) ||
                               Byte.class.equals(toType)) {
                        return v.byteValueExact();
                    } else if (BigInteger.class.equals(toType)) {
                        return v.toBigIntegerExact();
                    }
                } else if (BigInteger.class.equals(fromType)) {
                    BigInteger v = (BigInteger) value;
                    if (long.class.equals(toType) ||
                        Long.class.equals(toType)) {
                        return v.longValueExact();
                    } else if (int.class.equals(toType) ||
                               Integer.class.equals(toType)) {
                        return v.intValueExact();
                    } else if (short.class.equals(toType) ||
                               Short.class.equals(toType)) {
                        return v.shortValueExact();
                    } else if (byte.class.equals(toType) ||
                               Byte.class.equals(toType)) {
                        return v.byteValueExact();
                    } else if (BigDecimal.class.equals(toType)) {
                        return new BigDecimal(v);
                    }
                } else if (double.class.equals(fromType) ||
                           Double.class.equals(fromType)) {
                    Double v = (Double) value;
                    if (double.class.equals(toType))
                        return v;
                    else if (BigDecimal.class.equals(toType))
                        return BigDecimal.valueOf(v);
                } else if (float.class.equals(fromType) ||
                           Float.class.equals(fromType)) {
                    Float v = (Float) value;
                    if (float.class.equals(toType))
                        return v;
                    else if (double.class.equals(toType))
                        return v.doubleValue();
                    else if (BigDecimal.class.equals(toType))
                        return BigDecimal.valueOf(v);
                } else {
                    Number n = (Number) value;
                    long v = n.longValue();
                    if (long.class.equals(toType) ||
                        Long.class.equals(toType)) {
                        return v;
                    } else if (int.class.equals(toType) ||
                               Integer.class.equals(toType)) {
                        if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
                            return n.intValue();
                        else
                            convertFail(n, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    } else if (short.class.equals(toType) ||
                               Short.class.equals(toType)) {
                        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE)
                            return n.shortValue();
                        else
                            convertFail(n, Short.MIN_VALUE, Short.MAX_VALUE);
                    } else if (byte.class.equals(toType) ||
                               Byte.class.equals(toType)) {
                        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE)
                            return n.byteValue();
                        else
                            convertFail(n, Byte.MIN_VALUE, Byte.MAX_VALUE);
                    } else if (BigInteger.class.equals(toType)) {
                        return BigInteger.valueOf(v);
                    } else if (BigDecimal.class.equals(toType)) {
                        return BigDecimal.valueOf(v);
                    } else if (double.class.equals(toType) ||
                               Double.class.equals(toType)) {
                        if (Integer.class.equals(fromType) ||
                            Short.class.equals(fromType) ||
                            Byte.class.equals(fromType) ||
                            v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
                            return n.doubleValue();
                    } else if (float.class.equals(toType) ||
                               Float.class.equals(toType)) {
                        if (Short.class.equals(fromType) ||
                            Byte.class.equals(fromType) ||
                            v >= Short.MIN_VALUE && v <= Short.MAX_VALUE)
                            return n.floatValue();
                    }
                }
            } catch (ArithmeticException x) {
                cause = x;
            }
        } else if (value instanceof CharSequence &&
                   (Util.PRIMITIVE_NUMERIC_TYPES.contains(toType) ||
                    Number.class.isAssignableFrom(toType))) {
            // Conversion from text to numeric value
            if (int.class.equals(toType) || Integer.class.equals(toType))
                return Integer.parseInt(value.toString());
            else if (long.class.equals(toType) || Long.class.equals(toType))
                return Long.parseLong(value.toString());
            else if (short.class.equals(toType) || Short.class.equals(toType))
                return Short.parseShort(value.toString());
            else if (byte.class.equals(toType) || Byte.class.equals(toType))
                return Byte.parseByte(value.toString());
            else if (double.class.equals(toType) || Double.class.equals(toType))
                return Double.parseDouble(value.toString());
            else if (float.class.equals(toType) || Float.class.equals(toType))
                return Float.parseFloat(value.toString());
            else if (BigDecimal.class.equals(toType))
                return new BigDecimal(value.toString());
            else if (BigInteger.class.equals(toType))
                return new BigInteger(value.toString());
        } else if (String.class.equals(toType) ||
                   CharSequence.class.equals(toType)) {
            // Conversion to text
            return value.toString();
        } else if (char.class.equals(toType) ||
                   Character.class.equals(toType)) {
            // Conversion from length 1 or 0 text to single/optional character
            if (value instanceof CharSequence) {
                CharSequence chars = (CharSequence) value;
                if (chars.length() == 1)
                    return chars.charAt(0);
                else if (chars.isEmpty() && Character.class.equals(toType))
                    return null;
            }
        } else if (boolean.class.equals(toType) ||
                   Boolean.class.equals(toType)) {
            if (value instanceof Boolean)
                return value;
            else if (value instanceof CharSequence) {
                // conversion from true/false text to boolean
                String str = ((CharSequence) value).toString();
                if ("true".equalsIgnoreCase(str))
                    return true;
                else if ("false".equalsIgnoreCase(str))
                    return false;
            }
        } else if (value instanceof List &&
                   Iterable.class.isAssignableFrom(toType)) {
            return convertToIterable((List<?>) value,
                                     toType,
                                     singleTypeElementType,
                                     null);
        }

        if (failIfNotConverted) {
            MappingException x;
            x = exc(MappingException.class,
                    "CWWKD1046.result.convert.err",
                    loggableAppend(fromType.getName(), " (", value, ")"),
                    method.getName(),
                    repositoryInterface.getName(),
                    method.getGenericReturnType().getTypeName());
            if (cause != null)
                x = (MappingException) x.initCause(cause);
            throw x;
        } else {
            return value;
        }
    }

    /**
     * Raises an error for a type conversion failure due to a value being outside of
     * the specified range.
     *
     * @param queryInfo query information for the repository method.
     * @param value     the value that fails to convert.
     * @param min       minimum value for range.
     * @param max       maximum value for range.
     * @throws MappingException for the type conversion failure.
     */
    @Trivial
    private void convertFail(Number value, long min, long max) {
        throw exc(MappingException.class,
                  "CWWKD1047.result.out.of.range",
                  loggableAppend(value.getClass().getName(), " (", value, ")"),
                  method.getName(),
                  repositoryInterface.getName(),
                  method.getGenericReturnType().getTypeName(),
                  min,
                  max);
    }

    /**
     * Convert the results list into an Iterable of the specified type.
     *
     * @param results      results of a find or save operation.
     * @param iterableType the desired type of Iterable.
     * @param elementType  the type of each element, or null.
     *                         Always null if not a find operation.
     * @param query        the query if available.
     *                         Always null if not a find operation.
     * @return results converted to an Iterable of the specified type.
     */
    @Trivial
    private final Iterable<?> convertToIterable(List<?> results,
                                                Class<?> iterableType,
                                                Class<?> elementType,
                                                jakarta.persistence.Query query) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "convertToIterable",
                     loggable(results),
                     "to " + iterableType.getName(),
                     elementType == null ? "of ?" : ("of " + elementType.getName()),
                     query);

        Collection<Object> list;
        if (iterableType.isInterface()) {
            if (iterableType.isAssignableFrom(ArrayList.class))
                // covers Iterable, Collection, List
                list = new ArrayList<>(results.size());
            else if (iterableType.isAssignableFrom(ArrayDeque.class))
                // covers Queue, Deque
                list = new ArrayDeque<>(results.size());
            else if (iterableType.isAssignableFrom(LinkedHashSet.class))
                // covers Set
                list = new LinkedHashSet<>(results.size());
            else
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1046.result.convert.err",
                          List.class.getName(),
                          method.getName(),
                          repositoryInterface.getName(),
                          method.getGenericReturnType().getTypeName());
        } else {
            try {
                @SuppressWarnings("unchecked")
                Constructor<? extends Collection<Object>> c = //
                                (Constructor<? extends Collection<Object>>) //
                                iterableType.getConstructor();
                list = c.newInstance();
            } catch (IllegalAccessException | InstantiationException | //
                            NoSuchMethodException x) {
                throw (UnsupportedOperationException) exc //
                (UnsupportedOperationException.class,
                 "CWWKD1057.no.args.constr.inacc",
                 method.getGenericReturnType().getTypeName(),
                 method.getName(),
                 repositoryInterface.getName(),
                 iterableType.getName()) //
                                 .initCause(x);
            } catch (InvocationTargetException x) {
                throw (DataException) exc //
                (DataException.class,
                 "CWWKD1058.no.args.constr.err",
                 method.getGenericReturnType().getTypeName(),
                 method.getName(),
                 repositoryInterface.getName(),
                 iterableType.getName(),
                 x.getMessage()) //
                                 .initCause(x);
            }
        }
        if (results.size() == 1 && results.get(0) instanceof Object[]) {
            Object[] a = (Object[]) results.get(0);
            for (int i = 0; i < a.length; i++) {
                Object element = a[i];
                if (elementType != null && !elementType.isInstance(element))
                    element = convert(element, elementType, true);
                list.add(element);
            }
        } else {
            for (Object element : results) {
                if (elementType != null && !elementType.isInstance(element)) {
                    Object converted = convert(element, elementType, false);
                    // EclipseLink returns wrong values when selecting
                    // ElementCollection attributes instead of rejecting it as
                    // unsupported. Raise an error instead.
                    if (converted == element)
                        throw excIncompatibleQueryResult(results, query);
                }
                list.add(element);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "convertToIterable", loggable(list));
        return list;
    }

    /**
     * Execute a repository count query.
     *
     * @param em   entity manager.
     * @param args method parameters.
     * @return the count, converted to a type that is compatible with the
     *         method signature.
     * @throws Exception        if an error occurs.
     * @throws MappingException if the count cannot be converted to the
     *                              requested type.
     */
    @Trivial
    Object count(EntityManager em, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "count",
                     // em and method args have already been logged if loggable
                     "to be returned as " + singleType.getName());

        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        setParameters(query, args);

        Long count = query.getSingleResult();

        Object returnValue;
        if (long.class.equals(singleType) ||
            Long.class.equals(singleType) ||
            singleType.isAssignableFrom(Long.class)) {
            returnValue = count;
        } else {
            if (int.class.equals(singleType) || Integer.class.equals(singleType))
                if (count > Integer.MAX_VALUE)
                    throw exc(MappingException.class,
                              "CWWKD1048.result.exceeds.max",
                              count,
                              method.getName(),
                              repositoryInterface.getName(),
                              method.getGenericReturnType().getTypeName(),
                              "Integer.MAX_VALUE (" + Integer.MAX_VALUE + ')');
                else
                    returnValue = count.intValue();
            else if (short.class.equals(singleType) || Short.class.equals(singleType))
                if (count > Short.MAX_VALUE)
                    throw exc(MappingException.class,
                              "CWWKD1048.result.exceeds.max",
                              count,
                              method.getName(),
                              repositoryInterface.getName(),
                              method.getGenericReturnType().getTypeName(),
                              "Short.MAX_VALUE (" + Short.MAX_VALUE + ')');
                else
                    returnValue = count.shortValue();
            else if (byte.class.equals(singleType) || Byte.class.equals(singleType))
                if (count > Byte.MAX_VALUE)
                    throw exc(MappingException.class,
                              "CWWKD1048.result.exceeds.max",
                              count,
                              method.getName(),
                              repositoryInterface.getName(),
                              method.getGenericReturnType().getTypeName(),
                              "Byte.MAX_VALUE (" + Byte.MAX_VALUE + ')');
                else
                    returnValue = count.byteValue();
            else if (BigInteger.class.equals(singleType))
                returnValue = BigInteger.valueOf(count);
            else if (BigDecimal.class.equals(singleType))
                returnValue = BigDecimal.valueOf(count);
            else
                throw exc(MappingException.class,
                          "CWWKD1049.count.convert.err",
                          count,
                          method.getName(),
                          repositoryInterface.getName(),
                          method.getGenericReturnType().getTypeName());
        }

        Class<?> returnType = method.getReturnType();
        if (isOptional) {
            returnValue = Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) ||
                   CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (multiType != null) {
            throw exc(MappingException.class,
                      "CWWKD1049.count.convert.err",
                      count,
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericReturnType().getTypeName());
        }

        if (trace && tc.isEntryEnabled())
            if (count == returnValue)
                Tr.exit(this, tc, "count", returnValue);
            else
                Tr.exit(this, tc, "count", count + " converted to " + returnValue);
        return returnValue;
    }

    /**
     * Raises an error because the number of cursor elements does not match the
     * number of sort parameters.
     *
     * @param cursor cursor
     */
    @Trivial
    private void cursorSizeMismatchError(PageRequest.Cursor cursor) {
        List<String> keyTypes = new ArrayList<>();
        for (int i = 0; i < cursor.size(); i++)
            keyTypes.add(cursor.get(i) == null ? null : cursor.get(i).getClass().getName());

        throw exc(IllegalArgumentException.class,
                  "CWWKD1036.cursor.size.mismatch",
                  cursor.size(),
                  method.getName(),
                  repositoryInterface.getName(),
                  sorts.size(),
                  loggable(cursor.elements()),
                  sorts);
    }

    /**
     * Deletes entities that were found by a find-and-delete operation.
     *
     * @param results entities or record entities to delete from the database.
     * @param em      entity manager.
     * @throws Exception if an error occurs.
     */
    @Trivial
    private void delete(List<?> results, EntityManager em) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "delete", loggable(results));

        for (Object result : results)
            if (result == null) {
                throw exc(DataException.class,
                          "CWWKD1046.result.convert.err",
                          null,
                          method.getName(),
                          repositoryInterface.getName(),
                          method.getGenericReturnType().getTypeName());
            } else if (entityInfo.entityClass.isInstance(result)) {
                em.remove(result);
            } else if (entityInfo.idClassAttributeAccessors != null) {
                jakarta.persistence.Query delete = em.createQuery(jpqlDelete);
                int numParams = 0;
                for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                    Object value = accessor instanceof Method //
                                    ? ((Method) accessor).invoke(result) //
                                    : ((Field) accessor).get(result);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, jpqlDelete,
                                 "set ?" + (numParams + 1) + ' ' + loggable(value));
                    delete.setParameter(++numParams, value);
                }
                delete.executeUpdate();
            } else { // is return value the entity or id?
                Object value = result;
                if (entityInfo.entityClass.isInstance(result) ||
                    (entityInfo.recordClass != null &&
                     entityInfo.recordClass.isInstance(result))) {
                    List<Member> accessors = entityInfo.attributeAccessors //
                                    .get(entityInfo.attributeNames.get(ID));
                    if (accessors == null || accessors.isEmpty())
                        throw exc(MappingException.class,
                                  "CWWKD1025.missing.id.attr",
                                  entityInfo.getType().getName(),
                                  method.getName(),
                                  repositoryInterface);
                    for (Member accessor : accessors)
                        value = accessor instanceof Method //
                                        ? ((Method) accessor).invoke(value) //
                                        : ((Field) accessor).get(value);
                } else if (!entityInfo.idType.isInstance(value)) {
                    value = convert(result, entityInfo.idType, false);
                    if (value == result)
                        throw exc(MappingException.class,
                                  "CWWKD1006.delete.rtrn.err",
                                  method.getGenericReturnType().getTypeName(),
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  entityInfo.getType().getName(),
                                  entityInfo.idType);
                }

                jakarta.persistence.Query delete = em.createQuery(jpqlDelete);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, jpqlDelete,
                             "set ?1 " + loggable(value));
                delete.setParameter(1, value);
                delete.executeUpdate();
            }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "delete");
    }

    /**
     * Deletes entities (or records) from the database.
     * An error is raised if any of the entities (or records) are not found
     * in the database.
     *
     * @param arg the entity or record, or array/Iterable/Stream of entity or record
     * @param em  the entity manager
     * @return the deleted entity count, boolean indicator of any deletion, or void
     *         return type that is required by the Delete method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial
    Object delete(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "delete", loggable(arg));

        int updateCount = 0;
        int numExpected = 0;

        if (arg instanceof Iterable) {
            for (Object e : ((Iterable<?>) arg)) {
                numExpected++;
                updateCount += deleteOne(e, em);
            }
        } else if (entityParamType.isArray()) {
            numExpected = Array.getLength(arg);
            for (int i = 0; i < numExpected; i++)
                updateCount += deleteOne(Array.get(arg, i), em);
        } else {
            numExpected = 1;
            updateCount = deleteOne(arg, em);
        }

        if (numExpected == 0)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1092.lifecycle.arg.empty",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericParameterTypes()[0].getTypeName());

        if (updateCount < numExpected)
            if (numExpected == 1)
                throw exc(OptimisticLockingFailureException.class,
                          "CWWKD1051.single.opt.lock.exc",
                          method.getName(),
                          repositoryInterface.getName(),
                          entityInfo.entityClass.getName(),
                          Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
            else
                throw exc(OptimisticLockingFailureException.class,
                          "CWWKD1052.multi.opt.lock.exc",
                          method.getName(),
                          repositoryInterface.getName(),
                          numExpected - updateCount,
                          numExpected,
                          entityInfo.entityClass.getName(),
                          Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);

        Object returnValue = toReturnValue(updateCount, method.getReturnType());

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "delete", loggable(returnValue));
        return returnValue;
    }

    /**
     * Removes the entity (or record) from the database if its attributes
     * match the database.
     *
     * @param e  the entity or record.
     * @param em the entity manager.
     * @return the number of entities deleted (1 or 0).
     * @throws Exception if an error occurs or the repository method return type is
     *                       void and the entity (or correct version of the entity)
     *                       was not found.
     */
    private int deleteOne(Object e, EntityManager em) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "deleteOne", loggable(e));

        Class<?> entityClass = entityInfo.getType();

        if (e == null)
            throw exc(NullPointerException.class,
                      "CWWKD1015.null.entity.param",
                      method.getName(),
                      repositoryInterface.getName());

        if (!entityClass.isInstance(e))
            throw exc(IllegalArgumentException.class,
                      "CWWKD1016.incompat.entity.param",
                      method.getName(),
                      repositoryInterface.getName(),
                      entityClass.getName(),
                      e.getClass().getName());

        String jpql = this.jpql;

        int versionParamIndex = (entityInfo.idClassAttributeAccessors == null //
                        ? 1 //
                        : entityInfo.idClassAttributeAccessors.size()) + 1;
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        String idAttributeName = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            idAttributeName = getAttributeName(ID, true);
            id = getAttribute(e, idAttributeName);
            if (id == null) {
                jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
                if (version != null)
                    jpql = jpql.replace("=?" + versionParamIndex,
                                        "=?" + (versionParamIndex - 1));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()
            && jpql != this.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> delete = em.createQuery(jpql, entityInfo.entityClass);

        if (entityInfo.idClassAttributeAccessors == null) {
            int p = 1;
            if (id != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + loggable(id));
                delete.setParameter(p++, id);
            }
            if (version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + loggable(version));
                delete.setParameter(p, version);
            }
        } else {
            setParametersFromIdClassAndVersion(1, delete, e, version);
        }

        int numDeleted = delete.executeUpdate();

        if (numDeleted == 0) {
            Class<?> returnType = method.getReturnType();
            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                if (idAttributeName == null)
                    idAttributeName = ID;
                List<String> entityProps = new ArrayList<>(2);
                if (id != null)
                    entityProps.add(loggableAppend(idAttributeName,
                                                   "=", id));
                if (entityInfo.versionAttributeName != null && version != null)
                    entityProps.add(loggableAppend(entityInfo.versionAttributeName,
                                                   "=", version));
                throw exc(OptimisticLockingFailureException.class,
                          "CWWKD1050.opt.lock.exc",
                          method.getName(),
                          repositoryInterface.getName(),
                          e.getClass().getName(),
                          entityProps,
                          Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
            }
        } else if (numDeleted > 1) {
            // ought to be unreachable
            throw new DataException("Found " + numDeleted + " matching entities.");
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "deleteOne", numDeleted);
        return numDeleted;
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
     * Create a new EmptyResultException.
     *
     * @return the EmptyResultException.
     */
    @Trivial
    private EmptyResultException excEmptyResult() {
        return exc(EmptyResultException.class,
                   "CWWKD1053.empty.result",
                   method.getGenericReturnType().getTypeName(),
                   method.getName(),
                   repositoryInterface.getName(),
                   List.of(List.class.getSimpleName(),
                           Optional.class.getSimpleName(),
                           Page.class.getSimpleName(),
                           CursoredPage.class.getSimpleName(),
                           Stream.class.getSimpleName()));
    }

    /**
     * Constructs the MappingException for the error where the repository method
     * defines extra named parameters that are not used by the JDQL or JPQL query.
     *
     * @return MappingException.
     */
    @Trivial
    private MappingException excExtraMethodArgNamedParams(Set<String> extras,
                                                          Set<String> qlRequired) {
        String firstExtraParam = null;
        StringBuilder extraParamNames = new StringBuilder();
        for (String name : extras) {
            if (firstExtraParam == null)
                firstExtraParam = name;
            else
                extraParamNames.append(", ");
            extraParamNames.append(name);
        }

        boolean isFirst = true;
        StringBuilder qlParamNames = new StringBuilder();
        for (String name : qlRequired) {
            if (!isFirst)
                qlParamNames.append(", ");
            qlParamNames.append(':').append(name);
            isFirst = false;
        }

        if (qlRequired.isEmpty())
            return exc(MappingException.class,
                       "CWWKD1086.named.params.unused",
                       method.getName(),
                       repositoryInterface.getName(),
                       extraParamNames,
                       method.getAnnotation(Query.class).value(),
                       ':' + firstExtraParam);
        else
            return exc(MappingException.class,
                       "CWWKD1085.extra.method.params",
                       method.getName(),
                       repositoryInterface.getName(),
                       extraParamNames,
                       qlParamNames,
                       method.getAnnotation(Query.class).value());
    }

    /**
     * Create a new UnsupportedOperationException for a conflicting Limit or
     * PageRequest parameter.
     *
     * @param param   method parameter that is an instance of Limit or PageRequest.
     * @param limit   other Limit parameter value. Otherwise null.
     * @param pageReq other PageRequest parameter value. Otherwise null.
     * @param method  repository method
     * @return UnsupportedOperationException
     */
    @Trivial
    private UnsupportedOperationException excIncompatible(Object param,
                                                          Limit limit,
                                                          PageRequest pageReq,
                                                          Method method) {
        Class<?> type = param instanceof Limit ? Limit.class : PageRequest.class;

        if (limit == null && pageReq == null)
            // conflicts with First keyword
            return exc(UnsupportedOperationException.class,
                       "CWWKD1099.first.keyword.incompat",
                       method.getName(),
                       repositoryInterface.getName(),
                       type.getSimpleName());
        else if (param instanceof Limit ? limit != null : pageReq != null)
            // conflicts with another parameter of the same time
            return exc(UnsupportedOperationException.class,
                       "CWWKD1017.dup.special.param",
                       method.getName(),
                       repositoryInterface.getName(),
                       type.getSimpleName());
        else
            // conflict between Limit and PageRequest parameters
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1018.confl.special.param",
                      method.getName(),
                      repositoryInterface.getName(),
                      Limit.class.getSimpleName(),
                      PageRequest.class.getSimpleName());
    }

    /**
     * Constructs an UnsupportedOperationException for an error where the
     * repository method return type does not match the query results.
     * On reason this might happen is when EclipseLink returns wrong values
     * when selecting ElementCollection attributes instead of rejecting
     * it as unsupported.
     *
     * @param results list of at least 1 result.
     * @param query   jakarta.persistence.Query, a String, or null.
     * @return UnsupportedOperationException.
     */
    @Trivial
    private UnsupportedOperationException excIncompatibleQueryResult(List<?> results,
                                                                     Object query) {
        String r = results.getClass().getName() +
                   "<" + results.get(0).getClass().getName() + ">";

        if (query == null)
            return exc(UnsupportedOperationException.class,
                       "CWWKD1102.incompat.query.result",
                       method.getName(),
                       repositoryInterface.getName(),
                       method.getGenericReturnType().getTypeName(),
                       r);
        else
            return exc(UnsupportedOperationException.class,
                       "CWWKD1103.incompat.query.result",
                       method.getName(),
                       repositoryInterface.getName(),
                       method.getGenericReturnType().getTypeName(),
                       query instanceof String ? query : query.getClass().getName(),
                       r);
    }

    /**
     * Check if the cause of the lacking named parameter is a mispositioned
     * special parameter. If so, raises UnsupportedOperationException.
     *
     * Otherwise, constructs a MappingException for the error where one or more
     * of the named parameters required by a JDQL or JPQL query are not specified
     * by the method parameters
     *
     * @param lacking query named parameters for which no method parameters were
     *                    found.
     * @return MappingException for a missing named parameter.
     * @throws UnsupportedOperationException if there is a mispositioned special
     *                                           parameter.
     */
    @Trivial
    private MappingException excLackingMethodArgNamedParams(Set<String> lacking) {

        validateParameterPositions();

        String first = null;
        StringBuilder all = new StringBuilder();
        for (String name : lacking) {
            if (first == null)
                first = name;
            else
                all.append(", ");
            all.append(':').append(name);
        }

        return exc(MappingException.class,
                   "CWWKD1084.missing.named.params",
                   method.getName(),
                   repositoryInterface.getName(),
                   all,
                   method.getAnnotation(Query.class).value(),
                   "@Param(\"" + first + "\")",
                   "String " + first);
    }

    /**
     * Constructs the UnsupportedOperationException for the error where a repository
     * method intermixed named and positional parameters for a query.
     *
     * @return UnsupportedOperationException.
     */
    @Trivial
    private UnsupportedOperationException excMixedQLParamTypes(int methodNPCount) {
        String firstNamedParam = null;
        StringBuilder allNamedParams = new StringBuilder().append('(');
        for (String name : jpqlParamNames) {
            if (firstNamedParam == null)
                firstNamedParam = name;
            else
                allNamedParams.append(", ");
            allNamedParams.append(':').append(name);
        }
        allNamedParams.append(')');

        Class<?> firstNamedParamType = String.class;
        for (Parameter p : method.getParameters()) {
            Param param = p.getAnnotation(Param.class);
            if (param == null //
                            ? p.isNamePresent() && firstNamedParam.equals(p.getName()) //
                            : firstNamedParam.equals(param.value()))
                firstNamedParamType = p.getType();
            break;
        }

        return exc(UnsupportedOperationException.class,
                   "CWWKD1019.mixed.positional.named",
                   method.getName(),
                   repositoryInterface.getName(),
                   jpqlParamCount - methodNPCount,
                   methodNPCount,
                   allNamedParams,
                   method.getAnnotation(Query.class).value(),
                   ':' + firstNamedParam,
                   "@Param(\"" + firstNamedParam + "\")",
                   firstNamedParamType.getSimpleName() + ' ' + firstNamedParam);
    }

    /**
     * Create a new NonUniqueResultException.
     *
     * @param numResults number of results.
     * @return the NonUniqueResultException.
     */
    @Trivial
    private NonUniqueResultException excNonUniqueResult(int numResults) {
        throw exc(NonUniqueResultException.class,
                  "CWWKD1054.non.unique.result",
                  method.getName(),
                  repositoryInterface.getName(),
                  method.getGenericReturnType().getTypeName(),
                  numResults,
                  List.of(// In a future release: @Find @First findByX(...)
                          "findFirstByX(...)",
                          "findByX(..., Limit.of(1))"));
    }

    /**
     * Constructs the UnsupportedOperationException for the general error where
     * a repository method is unrecognized and log the error.
     *
     * @return UnsupportedOperationException.
     */
    @Trivial
    private UnsupportedOperationException excUnsupportedMethod() {
        return exc(UnsupportedOperationException.class,
                   "CWWKD1011.unknown.method.pattern",
                   method.getName(),
                   repositoryInterface.getName(),
                   List.of("Delete", "Find", "Insert",
                           "Query", "Save", "Update"),
                   List.of("Connection", "DataSource", "EntityManager"),
                   List.of("count", "delete", "exists", "find"),
                   entityInfo.getExampleMethodNames());
    }

    /**
     * Execute JPQL for a repository delete or update query.
     *
     * @param em   entity manager.
     * @param args method parameters.
     * @return void, boolean, int, or long value representing a count of
     *         matching entities, or a CompletableFuture for the value,
     *         whichever is compatible with the method signature.
     * @throws Exception if an error occurs.
     *
     *
     * @param em
     * @param args
     * @return
     * @throws Exception
     */
    @Trivial // em and method args have already been logged if loggable
    Object execute(EntityManager em, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "execute", type); // DELETE or UPDATE

        jakarta.persistence.Query update = em.createQuery(jpql);
        setParameters(update, args);

        int updateCount = update.executeUpdate();

        Object returnValue = toReturnValue(updateCount, method.getReturnType());

        if (trace && tc.isEntryEnabled())
            if (returnValue instanceof CompletableFuture)
                Tr.exit(this, tc, "execute", returnValue + ": " +
                                             ((CompletableFuture<?>) returnValue) //
                                                             .getNow(null)
                                             + " (" + updateCount + ')');
            else
                Tr.exit(this, tc, "execute", returnValue + " (" + updateCount + ')');
        return returnValue;
    }

    /**
     * Execute a repository exists query.
     *
     * @param em   entity manager.
     * @param args method parameters.
     * @return boolean value or CompletableFuture for a boolean value,
     *         whichever is compatible with the method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial // em and method args have already been logged if loggable
    Object exists(EntityManager em, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "exists");

        jakarta.persistence.Query query = em.createQuery(jpql);
        query.setMaxResults(1);
        setParameters(query, args);

        List<?> results = query.getResultList();
        boolean found = !results.isEmpty();

        Class<?> returnType = method.getReturnType();
        Object returnVal = CompletableFuture.class.equals(returnType) ||
                           CompletionStage.class.equals(returnType) //
                                           ? CompletableFuture.completedFuture(found) //
                                           : found;

        // There is no need to check if the return value is compatible
        // because that was done when initializing the query info for EXISTS

        if (trace && tc.isEntryEnabled())
            if (returnVal instanceof CompletableFuture)
                Tr.exit(this, tc, "exists", returnVal + ": " + found +
                                            " (" + results.size() + ')');
            else
                Tr.exit(this, tc, "exists", returnVal + " (" + results.size() + ')');
        return returnVal;
    }

    /**
     * Execute a repository find query, and possibly also a delete operation
     * if find-and-delete.
     *
     * @param em   entity manager.
     * @param args method parameters.
     * @return results, after wrapping in an Optional or CompletionStage if required
     *         by the repository method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial // em and method args have already been logged if loggable
    Object find(EntityManager em, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "find", type);

        Limit limit = null;
        int max = maxResults;
        PageRequest pageReq = null;
        List<Sort<Object>> sortList = null;

        // The first method parameters are used as query parameters.
        // Beyond that, they can have other purposes such as
        // pagination and sorting.
        for (int i = jpqlParamCount; i < (args == null ? 0 : args.length); i++) {
            Object param = args[i];
            if (param instanceof Limit) {
                if (max == 0 && limit == null && pageReq == null)
                    max = (limit = (Limit) param).maxResults();
                else
                    throw excIncompatible(param, limit, pageReq, method);
            } else if (param instanceof Order) {
                @SuppressWarnings("unchecked")
                Iterable<Sort<Object>> order = (Iterable<Sort<Object>>) param;
                sortList = supplySorts(sortList, order);
            } else if (param instanceof PageRequest) {
                if (max == 0 && pageReq == null && limit == null)
                    max = (pageReq = (PageRequest) param).size();
                else
                    throw excIncompatible(param, limit, pageReq, method);
            } else if (param instanceof Sort) {
                @SuppressWarnings("unchecked")
                List<Sort<Object>> newList = supplySorts(sortList, (Sort<Object>) param);
                sortList = newList;
            } else if (param instanceof Sort[]) {
                @SuppressWarnings("unchecked")
                List<Sort<Object>> newList = supplySorts(sortList, (Sort<Object>[]) param);
                sortList = newList;
            } else if (param == null) {
                // ignore null for empty Sort...
                boolean isSort = false;
                for (int s = 0; s < sortPositions.length; s++)
                    isSort |= sortPositions[s] == i;
                if (!isSort)
                    // BasicRepository.findAll requires NullPointerException
                    throw exc(NullPointerException.class,
                              "CWWKD1087.null.param",
                              method.getParameterTypes()[i].getName(),
                              method.getName(),
                              repositoryInterface.getName());
            } else {
                validateParameterPositions();

                throw exc(DataException.class,
                          "CWWKD1023.extra.param",
                          method.getName(),
                          repositoryInterface.getName(),
                          jpqlParamCount,
                          method.getParameterTypes()[i].getName(),
                          jpql);
            }
        }

        if (sortList == null && sortPositions.length > 0)
            sortList = sorts;

        QueryInfo queryInfo = this;
        if (sortList == null || sortList.isEmpty()) {
            if (pageReq != null)
                requireOrderedPagination(args);
        } else {
            boolean forward = pageReq == null ||
                              pageReq.mode() != PageRequest.Mode.CURSOR_PREVIOUS;
            StringBuilder q = new StringBuilder(jpql);
            StringBuilder order = null; // ORDER BY clause based on Sorts
            for (Sort<?> sort : sortList) {
                validateSort(sort);
                order = order == null //
                                ? new StringBuilder(100).append(" ORDER BY ") //
                                : order.append(", ");
                generateSort(order, sort, forward);
            }

            if (pageReq == null ||
                pageReq.mode() == PageRequest.Mode.OFFSET) {
                // offset pagination can be a starting point for cursor pagination
                String jpqlOrdered = q.append(order).toString();
                queryInfo = new QueryInfo(this, jpqlOrdered, sortList);
            } else { // CURSOR_NEXT or CURSOR_PREVIOUS
                queryInfo = new QueryInfo(this, null, sortList);
                queryInfo.generateCursorQueries(q,
                                                forward ? order : null,
                                                forward ? null : order);
            }
        }

        Object returnValue = queryInfo.find(limit, max, pageReq, sortList, em, args);

        if (isOptional) {
            returnValue = returnValue == null
                          || returnValue instanceof Collection &&
                             ((Collection<?>) returnValue).isEmpty()
                          || returnValue instanceof Page
                             && !((Page<?>) returnValue).hasContent() //
                                             ? Optional.empty() //
                                             : Optional.of(returnValue);
        }

        Class<?> returnType = method.getReturnType();
        if (CompletableFuture.class.equals(returnType) ||
            CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "find", loggable(returnValue));
        return returnValue;
    }

    /**
     * Execute a repository find query, and possibly also a delete operation
     * if find-and-delete.
     *
     * @param limit    Limit, if specified as a repository method parameter
     * @param max      maximum number of results to return
     * @param pageReq  PageRequest, if specified as a repository method parameter
     * @param sortList combined list of Sorts
     * @param em       entity manager.
     * @param args     method parameters.
     * @return results, before wrapping in an Optional or CompletionStage.
     * @throws Exception if an error occurs.
     */
    @Trivial // method args have already been logged if loggable
    private Object find(Limit limit,
                        int max,
                        PageRequest pageReq,
                        List<Sort<Object>> sortList,
                        EntityManager em,
                        Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "find",
                     "Limit: " + limit,
                     "max results: " + max,
                     "PageRequest: " + pageReq,
                     "Sorts: " + sortList);

        Object returnValue;

        if (CursoredPage.class.equals(multiType)) {
            returnValue = new CursoredPageImpl<>(this, pageReq, args);
        } else if (Page.class.equals(multiType)) {
            PageRequest req = limit == null ? pageReq : toPageRequest(limit);
            returnValue = new PageImpl<>(this, req, args);
        } else if (pageReq != null &&
                   !PageRequest.Mode.OFFSET.equals(pageReq.mode())) {
            throw exc(IllegalArgumentException.class,
                      "CWWKD1035.incompat.page.mode",
                      pageReq.mode(),
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericReturnType().getTypeName(),
                      CursoredPage.class.getSimpleName());
        } else {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "createQuery",
                         jpql,
                         entityInfo.entityClass.getName());

            jakarta.persistence.Query query = em.createQuery(jpql);
            setParameters(query, args);

            if (type == Type.FIND_AND_DELETE)
                query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

            int startAt = limit != null //
                            ? computeOffset(limit) //
                            : pageReq != null //
                                            ? computeOffset(pageReq) //
                                            : 0;

            if (max > 0) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "limit max results to " + max);
                query.setMaxResults(max);
            }
            if (startAt > 0) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, "start at (0-based) position " + startAt);
                query.setFirstResult(startAt);
            }

            if (multiType != null && BaseStream.class.isAssignableFrom(multiType)) {
                Stream<?> stream = query.getResultStream();
                if (Stream.class.equals(multiType))
                    returnValue = stream;
                else if (IntStream.class.equals(multiType))
                    returnValue = stream.mapToInt(this::toInt);
                else if (LongStream.class.equals(multiType))
                    returnValue = stream.mapToLong(this::toLong);
                else if (DoubleStream.class.equals(multiType))
                    returnValue = stream.mapToDouble(this::toDouble);
                else
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1046.result.convert.err",
                              List.class.getName(),
                              method.getName(),
                              repositoryInterface.getName(),
                              method.getGenericReturnType().getTypeName());
            } else {
                List<?> results = query.getResultList();

                if (trace) {
                    Tr.debug(this, tc, "result list type: " +
                                       (results == null ? null : results.getClass().toGenericString()));
                    if (results != null && !results.isEmpty()) {
                        Object r0 = results.get(0);
                        Tr.debug(this, tc, "type of first result: " +
                                           (r0 == null ? null : r0.getClass().toGenericString()));
                    }
                }

                if (type == Type.FIND_AND_DELETE)
                    delete(results, em);

                if (results.isEmpty() && isOptional) {
                    returnValue = null;
                } else if (multiType == null && entityInfo.entityClass.equals(singleType)) {
                    returnValue = oneResult(results);
                } else if (multiType != null &&
                           multiType.isInstance(results) &&
                           (results.isEmpty() || singleType.isInstance(results.get(0)) &&
                                                 !(results.get(0) instanceof Object[]))) {
                    returnValue = results;
                } else if (multiType != null && Iterable.class.isAssignableFrom(multiType)) {
                    returnValue = convertToIterable(results,
                                                    multiType,
                                                    singleType,
                                                    query);
                } else if (Iterator.class.equals(multiType)) {
                    returnValue = results.iterator();
                } else if (returnArrayType != null) {
                    int size = results.size();
                    Object firstNonNullResult = null;
                    for (Object result : results)
                        if (result != null) {
                            firstNonNullResult = result;
                            break;
                        }
                    if (firstNonNullResult == null
                        || type == Type.FIND_AND_DELETE
                        || returnArrayType != Object.class &&
                           returnArrayType.isInstance(firstNonNullResult)
                        || returnArrayType.isPrimitive() &&
                           Util.isWrapperClassFor(returnArrayType,
                                                  firstNonNullResult.getClass())) {
                        returnValue = Array.newInstance(returnArrayType, size);
                        int i = 0;
                        for (Object result : results)
                            Array.set(returnValue, i++, result);
                    } else if (firstNonNullResult.getClass().isArray()) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "convert " + firstNonNullResult.getClass().getName() +
                                               " to " + returnArrayType.getName());
                        if (returnArrayType.isArray()) {
                            // convert List<Object[]> to array of array
                            returnValue = Array.newInstance(returnArrayType, size);
                            int i = 0;
                            for (Object result : results)
                                if (result == null) {
                                    Array.set(returnValue, i++, result);
                                } else {
                                    // Object[] needs conversion to returnArrayType
                                    Class<?> subarrayType = returnArrayType.getComponentType();
                                    int len = Array.getLength(result);
                                    Object subarray = Array.newInstance(subarrayType, len);
                                    for (int j = 0; j < len; j++) {
                                        Object element = Array.get(result, j);
                                        if (!subarrayType.isInstance(element))
                                            element = convert(element, subarrayType, true);
                                        Array.set(subarray, j, element);
                                    }
                                    Array.set(returnValue, i++, subarray);
                                }
                        } else if (size == 1) {
                            // convert size 1 List<Object[]> to array
                            if (isOptional &&
                                firstNonNullResult.getClass().equals(singleType))
                                returnValue = firstNonNullResult;
                            else {
                                int len = Array.getLength(firstNonNullResult);
                                returnValue = Array.newInstance(returnArrayType, len);
                                for (int i = 0; i < len; i++) {
                                    Object element = Array.get(firstNonNullResult, i);
                                    if (!returnArrayType.isInstance(element))
                                        element = convert(element, returnArrayType, true);
                                    Array.set(returnValue, i, element);
                                }
                            }
                        } else {
                            // List<Object[]> with multiple Object[] elements
                            // cannot convert to a one dimensional array
                            throw excNonUniqueResult(size);
                        }
                    } else {
                        throw exc(MappingException.class,
                                  "CWWKD1046.result.convert.err",
                                  loggableAppend(firstNonNullResult.getClass().getName(),
                                                 " (", firstNonNullResult, ")"),
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  method.getGenericReturnType().getTypeName());
                    }
                } else if (results.isEmpty()) {
                    throw excEmptyResult();
                } else { // single result of other type
                    if (Iterable.class.isAssignableFrom(singleType) &&
                        !(results.get(0) instanceof Iterable))
                        // workaround for EclipseLink wrongly returning
                        // ElementCollection as separate individual elements
                        // as shown in #30575
                        returnValue = results;
                    else
                        returnValue = oneResult(results);
                    if (returnValue != null &&
                        !singleType.isAssignableFrom(returnValue.getClass()))
                        returnValue = convert(returnValue, singleType, true);
                }
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "find", loggable(returnValue));
        return returnValue;
    }

    /**
     * Finds and updates entities (or records) in the database.
     *
     * @param arg the entity or record, or array/Iterable/Stream of entity or record
     * @param em  the entity manager.
     * @return the updated entities, using the return type that is required by the
     *         repository Update method signature.
     * @throws OptimisticLockingFailureException if an entity is not found in the database.
     * @throws Exception                         if an error occurs.
     */
    Object findAndUpdate(Object arg, EntityManager em) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findAndUpdate", loggable(arg));

        List<Object> results;

        boolean hasSingularEntityParam = false;
        if (entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = new ArrayList<>(length);
            for (int i = 0; i < length; i++)
                results.add(findAndUpdateOne(Array.get(arg, i), em));
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                            : arg;

            results = new ArrayList<>();
            if (arg instanceof Iterable) {
                for (Object e : ((Iterable<?>) arg))
                    results.add(findAndUpdateOne(e, em));
            } else {
                hasSingularEntityParam = true;
                results = new ArrayList<>(1);
                results.add(findAndUpdateOne(arg, em));
            }
        }

        if (results.isEmpty())
            throw exc(IllegalArgumentException.class,
                      "CWWKD1092.lifecycle.arg.empty",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericParameterTypes()[0].getTypeName());

        em.flush();

        Class<?> returnType = method.getReturnType();
        if (void.class.equals(returnType) || Void.class.equals(returnType))
            return null;

        if (entityInfo.recordClass != null)
            for (int i = 0; i < results.size(); i++)
                results.set(i, entityInfo.toRecord(results.get(i)));

        Object returnValue;
        if (returnArrayType != null) {
            Object[] newArray = (Object[]) Array.newInstance(returnArrayType, results.size());
            returnValue = results.toArray(newArray);
        } else {
            if (multiType == null)
                if (results.size() == 1)
                    returnValue = results.get(0);
                else if (results.isEmpty())
                    returnValue = null;
                else
                    throw excNonUniqueResult(results.size());
            else if (multiType.isInstance(results))
                returnValue = results;
            else if (Stream.class.equals(multiType))
                returnValue = results.stream();
            else if (Iterable.class.isAssignableFrom(multiType))
                returnValue = convertToIterable(results, multiType, null, null);
            else if (Iterator.class.equals(multiType))
                returnValue = results.iterator();
            else
                throw exc(MappingException.class,
                          "CWWKD1003.rtrn.err",
                          method.getGenericReturnType().getTypeName(),
                          method.getName(),
                          repositoryInterface.getName(),
                          "Update",
                          lifeCycleReturnTypes(results.get(0).getClass().getSimpleName(),
                                               hasSingularEntityParam,
                                               false));
        }

        if (Optional.class.equals(returnType)) {
            returnValue = returnValue == null ? Optional.empty() : Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue); // useful for @Asynchronous
        } else if (returnValue != null && !returnType.isInstance(returnValue)) {
            throw exc(MappingException.class,
                      "CWWKD1003.rtrn.err",
                      method.getGenericReturnType().getTypeName(),
                      method.getName(),
                      repositoryInterface.getName(),
                      "Update",
                      lifeCycleReturnTypes(results.get(0).getClass().getSimpleName(),
                                           hasSingularEntityParam,
                                           false));
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "findAndUpdate", loggable(returnValue));
        return returnValue;
    }

    /**
     * Finds an entity (or record) in the database, locks it for subsequent update,
     * and updates the entity found in the database to match the desired state
     * of the supplied entity.
     *
     * @param e  the entity or record.
     * @param em the entity manager.
     * @return the entity that is written to the database. Never null.
     * @throws OptimisticLockingException if the entity is not found.
     * @throws Exception                  if an error occurs.
     */
    private Object findAndUpdateOne(Object e, EntityManager em) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findAndUpdateOne", loggable(e));

        String jpql = this.jpql;

        int versionParamIndex = entityInfo.idClassAttributeAccessors == null //
                        ? 2 //
                        : (entityInfo.idClassAttributeAccessors.size() + 1);
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        String idAttributeName = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            idAttributeName = entityInfo.attributeNames.get(ID);
            id = getAttribute(e, idAttributeName);
            if (id == null) {
                jpql = jpql.replace("=?" + (versionParamIndex - 1),
                                    " IS NULL");
                if (version != null)
                    jpql = jpql.replace("=?" + versionParamIndex,
                                        "=?" + (versionParamIndex - 1));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != this.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        Class<?> entityClass = singleType.equals(entityInfo.recordClass) //
                        ? entityInfo.entityClass //
                        : singleType;

        TypedQuery<?> query = em.createQuery(jpql, entityClass);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        if (entityInfo.idClassAttributeAccessors == null) {
            int p = 1;
            if (id != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + loggable(id));
                query.setParameter(p++, id);
            }
            if (version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + loggable(version));
                query.setParameter(p, version);
            }
        } else {
            setParametersFromIdClassAndVersion(1, query, e, version);
        }

        List<?> results = query.getResultList();

        if (results.isEmpty()) {
            List<String> entityProps = new ArrayList<>(2);
            if (id != null)
                entityProps.add(loggableAppend(idAttributeName, "=", id));
            if (entityInfo.versionAttributeName != null && version != null)
                entityProps.add(loggableAppend(entityInfo.versionAttributeName,
                                               "=", version));
            throw exc(OptimisticLockingFailureException.class,
                      "CWWKD1050.opt.lock.exc",
                      method.getName(),
                      repositoryInterface.getName(),
                      e.getClass().getName(),
                      entityProps,
                      Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
        }

        Object returnValue = em.merge(toEntity(e));

        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findAndUpdateOne", loggable(returnValue));
        return returnValue;
    }

    /**
     * Locate the names of named parameters after the specified point in the query
     * and populate them into the paramNames list.
     *
     * @param ql      query language
     * @param startAt starting position in the query language
     */
    @Trivial
    private LinkedHashSet<String> findNamedParameters(String ql, int startAt) {
        LinkedHashSet<String> qlParamNames = new LinkedHashSet<>();

        int length = ql.length();
        boolean isLiteral = false;
        StringBuilder paramName = null;
        for (; startAt < length; startAt++) {
            char ch = ql.charAt(startAt);
            if (!isLiteral && ch == ':') {
                paramName = new StringBuilder(30);
            } else if (ch == '\'') {
                if (isLiteral) {
                    if (startAt + 1 < length && ql.charAt(startAt + 1) == '\'')
                        startAt++; // escaped ' within a literal
                    else
                        isLiteral = false;
                } else {
                    isLiteral = true;
                    if (paramName != null) {
                        qlParamNames.add(paramName.toString());
                        paramName = null;
                    }
                }
            } else if (Character.isJavaIdentifierStart(ch)) {
                if (paramName != null) {
                    paramName.append(ch);
                    while (length > startAt + 1 && Character //
                                    .isJavaIdentifierPart(ch = ql.charAt(startAt + 1))) {
                        paramName.append(ch);
                        startAt++;
                    }
                }
            } else if (paramName != null) {
                qlParamNames.add(paramName.toString());
                paramName = null;
            }
        }

        if (paramName != null)
            qlParamNames.add(paramName.toString());

        return qlParamNames;
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
            throw excUnsupportedMethod();

        String name = getAttributeName(attribute, true);

        StringBuilder attributeExpr = new StringBuilder();
        if (function != null)
            attributeExpr.append(function); // such as LOWER(  or  ROUND(
        if (trimmed)
            attributeExpr.append("TRIM(");

        appendAttributeName(name, attributeExpr);

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
                generateParam(q, ignoreCase, ++jpqlParamCount).append(", '%')");
                break;
            case ENDS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                generateParam(q, ignoreCase, ++jpqlParamCount).append(")");
                break;
            case LIKE:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE ");
                generateParam(q, ignoreCase, ++jpqlParamCount);
                break;
            case BETWEEN:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ");
                generateParam(q, ignoreCase, ++jpqlParamCount).append(" AND ");
                generateParam(q, ignoreCase, ++jpqlParamCount);
                break;
            case CONTAINS:
                if (isCollection) {
                    q.append(" ?").append(++jpqlParamCount).append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
                } else {
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ");
                    generateParam(q, ignoreCase, ++jpqlParamCount).append(", '%')");
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
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1074.qbmn.incompat.keywords",
                              method.getName(),
                              repositoryInterface.getName(),
                              "IgnoreCase",
                              "In");
            default:
                q.append(attributeExpr).append(negated ? " NOT " : "").append(condition.operator);
                generateParam(q, ignoreCase, ++jpqlParamCount);
        }
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
     * Generates the queries for before/after a cursor and populates them into the
     * query information.
     * Example conditions to add for cursor next of (lastName, firstName, ssn):
     * AND ((o.lastName > ?5)
     * _ OR (o.lastName = ?5 AND o.firstName > ?6)
     * _ OR (o.lastName = ?5 AND o.firstName = ?6 AND o.ssn > ?7) )
     *
     * @param q    query up to the WHERE clause, if present
     * @param fwd  ORDER BY clause in forward page direction.
     *                 Null if forward page direction is not needed.
     * @param prev ORDER BY clause in previous page direction.
     *                 Null if previous page direction is not needed.
     */
    private void generateCursorQueries(StringBuilder q,
                                       StringBuilder fwd,
                                       StringBuilder prev) {
        int numSorts = sorts.size();
        String paramPrefix = jpqlParamNames.isEmpty() ? "?" : ":cursor";
        StringBuilder a = fwd == null ? null : new StringBuilder(200).append(hasWhere ? " AND (" : " WHERE (");
        StringBuilder b = prev == null ? null : new StringBuilder(200).append(hasWhere ? " AND (" : " WHERE (");
        for (int i = 0; i < numSorts; i++) {
            if (a != null)
                a.append(i == 0 ? "(" : " OR (");
            if (b != null)
                b.append(i == 0 ? "(" : " OR (");
            for (int s = 0; s <= i; s++) {
                Sort<?> sort = sorts.get(s);
                String name = sort.property();
                boolean asc = sort.isAscending();
                boolean lower = sort.ignoreCase();
                if (a != null)
                    if (lower) {
                        a.append(s == 0 ? "LOWER(" : " AND LOWER(");
                        appendAttributeName(name, a);
                        a.append(')');
                        a.append(s < i ? '=' : (asc ? '>' : '<'));
                        a.append("LOWER(").append(paramPrefix).append(jpqlParamCount + 1 + s).append(')');
                    } else {
                        a.append(s == 0 ? "" : " AND ");
                        appendAttributeName(name, a);
                        a.append(s < i ? '=' : (asc ? '>' : '<'));
                        a.append(paramPrefix).append(jpqlParamCount + 1 + s);
                    }
                if (b != null)
                    if (lower) {
                        b.append(s == 0 ? "LOWER(" : " AND LOWER(");
                        appendAttributeName(name, b);
                        b.append(')');
                        b.append(s < i ? '=' : (asc ? '<' : '>'));
                        b.append("LOWER(").append(paramPrefix).append(jpqlParamCount + 1 + s).append(')');
                    } else {
                        b.append(s == 0 ? "" : " AND ");
                        appendAttributeName(name, b);
                        b.append(s < i ? '=' : (asc ? '<' : '>'));
                        b.append(paramPrefix).append(jpqlParamCount + 1 + s);
                    }
            }
            if (a != null)
                a.append(')');
            if (b != null)
                b.append(')');
        }
        if (a != null)
            jpqlAfterCursor = new StringBuilder(q).append(a).append(')').append(fwd).toString();
        if (b != null)
            jpqlBeforeCursor = new StringBuilder(q).append(b).append(')').append(prev).toString();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "forward & previous cursor queries", jpqlAfterCursor, jpqlBeforeCursor);
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
                q.append(o_).append(getAttributeName(idClassAttrName, true)).append("=?").append(count);
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
            type = Type.QM_DELETE;
            hasWhere = false;
        } else {
            setType(Delete.class, Type.LC_DELETE);
            hasWhere = true;

            q.append(" WHERE (");

            String idName = entityInfo.attributeNames.get(ID);
            if (idName == null && entityInfo.idClassAttributeAccessors != null) {
                // IdClass cannot be a single query parameter because there is
                // no way to obtain an IdClass object from an entity instance.
                boolean first = true;
                for (String name : entityInfo.idClassAttributeAccessors.keySet()) {
                    if (first)
                        first = false;
                    else
                        q.append(" AND ");

                    name = entityInfo.attributeNames.get(name);
                    q.append(o_).append(name).append("=?").append(++jpqlParamCount);
                }
            } else {
                q.append(o_).append(idName).append("=?").append(++jpqlParamCount);
            }

            if (entityInfo.versionAttributeName != null)
                q.append(" AND ").append(o_).append(entityInfo.versionAttributeName).append("=?").append(++jpqlParamCount);

            q.append(')');
        }

        return q;
    }

    /**
     * Generates the JPQL ORDER BY clause. This method is common between the OrderBy annotation and keyword.
     */
    private void generateOrderBy(StringBuilder q) {
        boolean needsCursorQueries = CursoredPage.class.equals(multiType);

        StringBuilder fwd = needsCursorQueries ? new StringBuilder(100) : q; // forward page order
        StringBuilder prev = needsCursorQueries ? new StringBuilder(100) : null; // previous page order

        boolean first = true;
        for (Sort<?> sort : sorts) {
            validateSort(sort);
            fwd.append(first ? " ORDER BY " : ", ");
            generateSort(fwd, sort, true);

            if (needsCursorQueries) {
                prev.append(first ? " ORDER BY " : ", ");
                generateSort(prev, sort, false);
            }
            first = false;
        }

        if (needsCursorQueries) {
            generateCursorQueries(q, fwd, prev);
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
    @Trivial
    private StringBuilder generateQueryByParameters(StringBuilder q,
                                                    Annotation methodAnno,
                                                    boolean countPages) {
        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "generateQueryByParameters",
                     q,
                     methodAnno == null ? null : methodAnno.annotationType().getSimpleName(),
                     countPages);

        String o = entityVar;
        String o_ = entityVar_;
        DataVersionCompatibility compat = entityInfo.builder.provider.compat;

        boolean isFindOrDelete = methodAnno instanceof Find ||
                                 methodAnno instanceof Delete;
        Boolean isNamePresent = null; // unknown
        Parameter[] params = null;

        Set<Class<?>> specParamTypes = compat.specialParamTypes();
        Class<?>[] paramTypes = method.getParameterTypes();
        int numAttributeParams = paramTypes.length;
        while (numAttributeParams > 0 &&
               specParamTypes.contains(paramTypes[numAttributeParams - 1])) {
            numAttributeParams--;
            if (!isFindOrDelete) // special parameter is not allowed
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1020.invalid.param.type",
                          method.getName(),
                          repositoryInterface.getName(),
                          paramTypes[numAttributeParams - 1].getSimpleName(),
                          methodAnno.annotationType().getSimpleName());
        }

        Annotation[][] annosForAllParams = method.getParameterAnnotations();
        boolean[] isUpdateOp = new boolean[annosForAllParams.length];

        if (q == null)
            // Write new JPQL, starting with UPDATE or SELECT
            if (methodAnno instanceof Update) {
                type = Type.QM_UPDATE;
                q = new StringBuilder(250).append("UPDATE ").append(entityInfo.name).append(' ').append(o).append(" SET");

                boolean first = true;
                // p is the method parameter number (0-based)
                // qp is the query parameter number (1-based)
                for (int p = 0, qp = 1; p < numAttributeParams; p++, qp++) {
                    String[] attrAndOp = compat.getUpdateAttributeAndOperation(annosForAllParams[p]);
                    if (attrAndOp != null) {
                        isUpdateOp[p] = true;
                        if (entityInfo.idClassAttributeAccessors != null &&
                            paramTypes[p].equals(entityInfo.idType) &&
                            !"=".equals(attrAndOp[1])) {
                            // IdClass values cannot support operations other than
                            // assignment.
                            // Unreachable in version 1.0 and uncertain what
                            // will be added to the spec. Deferring NLS message
                            // until then.
                            throw new MappingException("One or more of the " +
                                                       Arrays.toString(annosForAllParams[p]) +
                                                       " annotations specifes an operation" +
                                                       " that cannot be used on parameter " +
                                                       (p + 1) + " of the " + method.getName() +
                                                       " method of the " +
                                                       repositoryInterface.getName() +
                                                       " repository when the Id is an IdClass.");
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
                                    throw exc(MappingException.class,
                                              "CWWKD1027.anno.missing.attr.name",
                                              p + 1,
                                              method.getName(),
                                              repositoryInterface.getName(),
                                              "Assign");
                            }

                            String name = getAttributeName(attribute, true);

                            q.append(first ? " " : ", ");
                            appendAttributeName(name, q);
                            q.append("=");
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

                            jpqlParamCount++;
                            q.append('?').append(qp);

                            if (withFunction)
                                q.append(')');
                        }
                    }
                }

                if (first)
                    // No parameters are annotated to indicate update.
                    // Raise an error that considers this an invalid life cycle
                    // operation attempt. In the future, we could raise an error
                    // that also mentions the possibility of invalid parameter-based
                    // update operation.
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1009.lifecycle.param.err",
                              method.getName(),
                              repositoryInterface.getName(),
                              method.getParameterCount(),
                              Update.class.getSimpleName());
            } else {
                type = Type.FIND;
                q = generateSelectClause().append(" FROM ").append(entityInfo.name).append(' ').append(o);
            }

        int startIndexForWhereClause = q.length();

        // append the WHERE clause
        // p is the method parameter number (0-based)
        // qp is the query parameter number (1-based)
        for (int p = 0, qp = 1; p < numAttributeParams; p++, qp++) {
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
                        switch (type) {
                            case FIND:
                            case FIND_AND_DELETE:
                                validateParameterPositions();
                                String specParams = type == Type.FIND //
                                                ? compat.specialParamsForFind() //
                                                : compat.specialParamsForFindAndDelete();
                                throw exc(UnsupportedOperationException.class,
                                          "CWWKD1012.fd.missing.param.anno",
                                          p + 1,
                                          method.getName(),
                                          repositoryInterface.getName(),
                                          specParams);
                            case QM_DELETE:
                            case COUNT:
                            case EXISTS:
                                throw exc(UnsupportedOperationException.class,
                                          "CWWKD1013.cde.missing.param.anno",
                                          p + 1,
                                          method.getName(),
                                          repositoryInterface.getName());
                            case QM_UPDATE:
                                throw exc(UnsupportedOperationException.class,
                                          "CWWKD1014.upd.missing.param.anno",
                                          p + 1,
                                          method.getName(),
                                          repositoryInterface.getName(),
                                          List.of("By", "Assign"));
                            default: // should be unreachable
                                throw new IllegalStateException(type.name());
                        }
                }

                String name = getAttributeName(attribute, true);

                boolean isCollection = entityInfo.collectionElementTypes.containsKey(name);

                jpqlParamCount++;

                compat.appendCondition(q, qp, method, p, o_, name, isCollection, annosForAllParams[p]);
            }
        }
        if (hasWhere)
            q.append(')');

        if (countPages && type == Type.FIND)
            generateCount(numAttributeParams == 0 ? null : q.substring(startIndexForWhereClause));

        if (type == Type.FIND ||
            type == Type.FIND_AND_DELETE)
            initDynamicSortPositions(paramTypes);

        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "generateQueryByParameters", q);
        return q;
    }

    /**
     * Generates the SELECT clause of the JPQL.
     *
     * @return the SELECT clause.
     */
    @FFDCIgnore(RuntimeException.class) // caught to switch to better error
    private StringBuilder generateSelectClause() {
        StringBuilder q = new StringBuilder(200);
        String o = entityVar;
        String o_ = entityVar_;

        String[] cols, selections = entityInfo.builder.provider.compat.getSelections(method);
        if (selections == null || selections.length == 0) {
            cols = null;
        } else if (type == Type.FIND_AND_DELETE) {
            // Unreachable in version 1.0 and uncertain what will be added to
            // the spec regarding selections. Deferring NLS message until then.
            throw new UnsupportedOperationException //
            ("The " + method.getName() + " method of the " +
             repositoryInterface.getName() + " repository has a " +
             method.getGenericReturnType().getTypeName() + " return type and" +
             " specifies to return the " + selections + " entity attributes," +
             " but delete operations can only return void, a deletion count," +
             " a boolean deletion indicator, or the removed entities.");
        } else {
            cols = new String[selections.length];
            for (int i = 0; i < cols.length; i++) {
                String name = getAttributeName(selections[i], true);
                cols[i] = name == null ? selections[i] : name;
            }
        }

        Class<?> singleType = this.singleType;

        if (singleType.isPrimitive())
            singleType = Util.wrapperClassIfPrimitive(singleType);

        if (type == Type.FIND_AND_DELETE &&
            !(singleType.isAssignableFrom(Util.wrapperClassIfPrimitive(entityInfo.idType)) ||
              singleType.isAssignableFrom(entityInfo.getType()))) {
            throw exc(MappingException.class,
                      "CWWKD1006.delete.rtrn.err",
                      method.getGenericReturnType().getTypeName(),
                      method.getName(),
                      repositoryInterface.getName(),
                      entityInfo.getType().getName(),
                      entityInfo.idType.getName());
        }

        if (cols == null || cols.length == 0) {
            if (singleType.isAssignableFrom(entityInfo.entityClass)
                || entityInfo.inheritance && entityInfo.entityClass.isAssignableFrom(singleType)) {
                // Whole entity
                if (!"this".equals(o))
                    q.append("SELECT ").append(o);
            } else if (entityInfo.idClassAttributeAccessors != null &&
                       singleType.equals(entityInfo.idType)) {
                // IdClass
                // TODO remove once #29073 is fixed
                // The following guess of alphabetic order is not valid in most cases, but this
                // whole code block will be removed before GA, so there is no reason to correct it.
                q.append("SELECT NEW ").append(singleType.getName()).append('(');
                boolean first = true;
                for (String idClassAttributeName : entityInfo.idClassAttributeAccessors.keySet()) {
                    String name = getAttributeName(idClassAttributeName, true);
                    q.append(first ? "" : ", ").append(o_).append(name);
                    first = false;
                }
                q.append(')');
                // TODO enable this once #29073 is fixed
                // q.append("SELECT ID(").append(entityVar).append(')');
            } else {
                // Is the result type a record or a single attribute?
                RecordComponent[] recordComponents = singleType.getRecordComponents();
                if (recordComponents == null) {
                    // Look for single entity attribute with the desired type:
                    String singleAttributeName = null;
                    for (Map.Entry<String, Class<?>> entry : entityInfo.attributeTypes.entrySet()) {
                        Class<?> attributeType = entry.getValue();
                        if (attributeType.isPrimitive())
                            attributeType = Util.wrapperClassIfPrimitive(attributeType);
                        if (singleType.isAssignableFrom(attributeType))
                            if (singleAttributeName == null)
                                singleAttributeName = entry.getKey();
                            else
                                throw exc(MappingException.class,
                                          "CWWKD1008.ambig.rtrn.err",
                                          method.getGenericReturnType().getTypeName(),
                                          method.getName(),
                                          repositoryInterface.getName(),
                                          List.of(singleAttributeName, entry.getKey()));
                    }

                    if (singleAttributeName == null)
                        throw exc(MappingException.class,
                                  "CWWKD1005.find.rtrn.err",
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  method.getGenericReturnType().getTypeName(),
                                  entityInfo.entityClass.getName(),
                                  List.of("List", "Optional",
                                          "Page", "CursoredPage",
                                          "Stream"));

                    else
                        q.append("SELECT ").append(o_).append(singleAttributeName);
                } else {
                    // Construct new instance for record
                    q.append("SELECT NEW ").append(singleType.getName()).append('(');

                    String[] names = new String[recordComponents.length];
                    for (int i = 0; i < recordComponents.length; i++) {
                        // 1.1 TODO first check for Select annotation on record component
                        names[i] = recordComponents[i].getName();
                    }

                    try {
                        boolean first = true;
                        for (String name : names) {
                            name = getAttributeName(name, true);
                            q.append(first ? "" : ", ");
                            appendAttributeName(name, q);
                            first = false;
                        }
                    } catch (RuntimeException x) {
                        // Raise a more precise error that relates to using records
                        // for a subset of entity attributes
                        MappingException mx;
                        mx = exc(MappingException.class,
                                 "CWWKD1101.attr.subset.mismatch",
                                 method.getGenericReturnType().getTypeName(),
                                 method.getName(),
                                 repositoryInterface.getName(),
                                 singleType.getName(),
                                 Arrays.toString(names),
                                 entityInfo.getType().getName(),
                                 entityInfo.getAttributeNames());
                        throw (MappingException) mx.initCause(x);
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
                    q.append(i == 0 ? "SELECT " : ", ");
                    appendAttributeName(cols[i], q);
                }
            } else {
                // Construct new instance from defined columns
                q.append("SELECT NEW ").append(singleType.getName()).append('(');
                for (int i = 0; i < cols.length; i++) {
                    if (i > 0)
                        q.append(", ");
                    appendAttributeName(cols[i], q);
                }
                q.append(')');
            }
        }

        return q;
    }

    /**
     * Generates and appends JQPL to sort based on the specified entity attribute.
     * For most attributes, this will be of a form such as o.name or LOWER(o.name) DESC or ...
     *
     * @param q             builder for the JPQL query.
     * @param Sort          sort criteria for a single attribute (name must already
     *                          be converted to a valid entity attribute name).
     * @param sameDirection indicate to append the Sort in the normal direction.
     *                          Otherwise reverses it (for cursor pagination in the
     *                          previous page direction).
     * @return the same builder for the JPQL query.
     */
    @Trivial
    private void generateSort(StringBuilder q, Sort<?> sort, boolean sameDirection) {
        String propName = sort.property();
        if (sort.ignoreCase())
            q.append("LOWER(");

        appendAttributeName(propName, q);

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
            String name = getAttributeName(attribute, true);

            q.append(first ? " " : ", ").append(o_).append(name).append("=");

            switch (op) {
                case '+':
                    if (CharSequence.class.isAssignableFrom(entityInfo.attributeTypes.get(name))) {
                        q.append("CONCAT(").append(o_).append(name).append(',') //
                                        .append('?').append(++jpqlParamCount).append(')');
                        break;
                    }
                    // else fall through
                case '*':
                case '/':
                    q.append(o_).append(name).append(op);
                    // fall through
                case '=':
                    q.append('?').append(++jpqlParamCount);
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

        if (entityInfo.attributeNamesForEntityUpdate != null &&
            Util.UPDATE_COUNT_TYPES.contains(singleType)) {
            setType(Update.class, Type.LC_UPDATE);

            q = new StringBuilder(100) //
                            .append("UPDATE ").append(entityInfo.name) //
                            .append(' ').append(o) //
                            .append(" SET ");

            boolean first = true;
            for (String name : entityInfo.attributeNamesForEntityUpdate) {
                if (first)
                    first = false;
                else
                    q.append(", ");

                q.append(o_).append(name).append("=?").append(++jpqlParamCount);
            }
        } else {
            // Update that returns an entity. And also used when an entity
            // has relation attributes that require using em.merge.
            // Perform a find operation first so that em.merge can be used.
            setType(Update.class, Type.LC_UPDATE_RET_ENTITY);

            q = new StringBuilder(100) //
                            .append("SELECT ").append(o) //
                            .append(" FROM ").append(entityInfo.name) //
                            .append(' ').append(o);
        }

        hasWhere = true;

        q.append(" WHERE (");

        String idName = entityInfo.attributeNames.get(ID);
        if (idName == null && entityInfo.idClassAttributeAccessors != null) {
            // IdClass cannot be a single query parameter because there is
            // no way to obtain an IdClass object from an entity instance.
            boolean first = true;
            for (String name : entityInfo.idClassAttributeAccessors.keySet()) {
                if (first)
                    first = false;
                else
                    q.append(" AND ");

                name = entityInfo.attributeNames.get(name);
                q.append(o_).append(name).append("=?").append(++jpqlParamCount);
            }
        } else {
            q.append(o_).append(idName).append("=?").append(++jpqlParamCount);
        }

        if (entityInfo.versionAttributeName != null)
            q.append(" AND ").append(o_).append(entityInfo.versionAttributeName) //
                            .append("=?").append(++jpqlParamCount);

        q.append(')');

        return q;
    }

    /**
     * Generates the JPQL WHERE clause for all find/delete/count/exists/By
     * conditions such as MyColumn[IgnoreCase][Not]Like
     */
    private void generateWhereClause(String methodName,
                                     int start,
                                     int endBefore,
                                     StringBuilder q) {
        hasWhere = true;
        q.append(" WHERE (");
        for (int and = start, or = start, iNext = start, i = start; //
                        hasWhere && i >= start && iNext < endBefore; //
                        i = iNext) {
            // The extra character (+1) below allows for entity attribute names
            // that begin with Or or And. For example,
            // findByOrg and findByPriceBetweenAndOrderNumber
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
     * Obtains the value of an entity attribute.
     *
     * @param entity        the entity from which to obtain the value.
     * @param attributeName name of the entity attribute.
     * @return the value of the attribute.
     */
    @Trivial
    private Object getAttribute(Object entity, String attributeName) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getAttribute", loggable(entity), attributeName);

        List<Member> accessors = entityInfo.attributeAccessors.get(attributeName);
        if (accessors == null)
            throw new IllegalArgumentException(attributeName); // should never occur

        Object value = entity;
        for (Member accessor : accessors) {
            Class<?> type = accessor.getDeclaringClass();
            if (type.isInstance(value)) {
                if (accessor instanceof Method)
                    value = ((Method) accessor).invoke(value);
                else // Field
                    value = ((Field) accessor).get(value);
            } else {
                throw exc(MappingException.class,
                          "CWWKD1059.attr.cast.err",
                          method.getName(),
                          repositoryInterface.getName(),
                          attributeName,
                          loggableAppend(entity.getClass().getName(),
                                         " (" + entity + ")"),
                          accessor.getName(),
                          type.getName(),
                          loggableAppend(value.getClass().getName(),
                                         " (" + value + ")"));
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getAttribute", loggable(value));
        return value;
    }

    @Trivial
    private String getAttributeName(String name, boolean failIfNotFound) {
        String attributeName;
        int len = name.length();
        if (len > 6 && name.charAt(len - 1) == ')') {
            // id(this) and version(this) can be supplied in Sort parameters, but the
            // query might use an entity identification variable instead of "this".
            if (name.regionMatches(true, len - 6, "(this", 0, 5))
                if (len == 8 && name.regionMatches(true, 0, "id", 0, 2) &&
                    entityInfo.idClassAttributeAccessors == null) {
                    // id(this)
                    attributeName = entityInfo.attributeNames.get(By.ID);
                    if (attributeName == null && failIfNotFound)
                        throw exc(UnsupportedOperationException.class,
                                  "CWWKD1093.fn.not.applicable",
                                  name,
                                  entityInfo.getType().getName(),
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  "@Id");
                } else if (len == 13 && name.regionMatches(true, 0, "version", 0, 7)) {
                    // version(this)
                    if (entityInfo.versionAttributeName == null && failIfNotFound)
                        throw exc(UnsupportedOperationException.class,
                                  "CWWKD1093.fn.not.applicable",
                                  name,
                                  entityInfo.getType().getName(),
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  "@Version");
                    else
                        attributeName = entityInfo.versionAttributeName;
                } else {
                    // id(this) with IdClass, or other function with (this):
                    // switch this to entity variable // TODO should we do this for other functions?
                    attributeName = new StringBuilder(len - 4 + entityVar.length()) //
                                    .append(name.substring(0, len - 5)) //
                                    .append(entityVar) //
                                    .append(')') //
                                    .toString();
                }
            else
                throw exc(MappingException.class,
                          "CWWKD1010.unknown.entity.attr",
                          name,
                          entityInfo.getType().getName(),
                          method.getName(),
                          repositoryInterface.getName(),
                          entityInfo.attributeTypes.keySet());
        } else {
            String lowerName = name.toLowerCase();
            attributeName = entityInfo.attributeNames.get(lowerName);
            if (attributeName == null)
                if (name.length() == 0) {
                    throw exc(MappingException.class,
                              "CWWKD1024.missing.entity.attr",
                              method.getName(),
                              repositoryInterface.getName(),
                              entityInfo.getType().getName(),
                              entityInfo.attributeTypes.keySet());
                } else {
                    // tolerate possible mixture of . and _ separators:
                    lowerName = lowerName.replace('.', '_');
                    attributeName = entityInfo.attributeNames.get(lowerName);
                    if (attributeName == null) {
                        // tolerate possible mixture of . and _ separators with lack of separators:
                        lowerName = lowerName.replace("_", "");
                        attributeName = entityInfo.attributeNames.get(lowerName);
                        if (attributeName == null && failIfNotFound) {
                            if (Util.hasOperationAnno(method))
                                throw exc(MappingException.class,
                                          "CWWKD1010.unknown.entity.attr",
                                          name,
                                          entityInfo.getType().getName(),
                                          method.getName(),
                                          repositoryInterface.getName(),
                                          entityInfo.attributeTypes.keySet());
                            else
                                throw exc(MappingException.class,
                                          "CWWKD1091.method.name.parse.err",
                                          name,
                                          entityInfo.getType().getName(),
                                          method.getName(),
                                          repositoryInterface.getName(),
                                          Util.OP_ANNOS,
                                          entityInfo.attributeTypes.keySet());
                        }
                    }
                }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getAttributeName " + name + ": " + attributeName);
        return attributeName;
    }

    /**
     * Obtains cursor values for the specified entity.
     *
     * @param entity the entity.
     * @return cursor values, ordering according to the sort criteria.
     */
    @Trivial
    Object[] getCursorValues(Object entity) {
        ArrayList<Object> cursorValues = new ArrayList<>();
        for (Sort<?> sort : sorts)
            try {
                List<Member> accessors = entityInfo.attributeAccessors.get(sort.property());
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "getCursorValues for " + loggable(entity),
                             accessors);
                Object value = entity;
                // TODO is it possible for accessors to be null here?
                // Could sort.property() being VERSION(THIS) or some invalid name cause this?
                for (Member accessor : accessors)
                    if (accessor instanceof Method)
                        value = ((Method) accessor).invoke(value);
                    else
                        value = ((Field) accessor).get(value);
                cursorValues.add(value);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new DataException(x instanceof InvocationTargetException ? x.getCause() : x);
            }
        return cursorValues.toArray();
    }

    /**
     * Returns the entity information for this query,
     * if known at the point when this method is invoked.
     *
     * @return the entity information, if known.
     */
    @Trivial
    public final EntityInfo getEntityInfo() {
        return entityInfo;
    }

    /**
     * Creates a Sort instance with the corresponding entity attribute name
     * or returns the existing instance if it already matches.
     *
     * @param name name provided by the user to sort by.
     * @param sort the Sort to add.
     * @return a Sort instance with the corresponding entity attribute name.
     */
    @Trivial
    private <T> Sort<T> getWithAttributeName(String name, Sort<T> sort) {
        name = getAttributeName(name, true);
        if (name == sort.property())
            return sort;
        else
            return sort.isAscending() //
                            ? sort.ignoreCase() ? Sort.ascIgnoreCase(name) : Sort.asc(name) //
                            : sort.ignoreCase() ? Sort.descIgnoreCase(name) : Sort.desc(name);
    }

    /**
     * Determine if the index of the text ignoring case if it is the next non-whitespace characters.
     *
     * @parma text the text to match.
     * @param ql      query language.
     * @param startAt starting position in the query language string.
     * @return position of the text ignoring case if it is the next non-whitespace characters. Otherwise -1;
     */
    @Trivial
    private static int indexOfAfterWhitespace(String text, String ql, int startAt) {
        int length = ql.length();
        while (startAt < length && Character.isWhitespace(ql.charAt(startAt)))
            startAt++;
        return ql.regionMatches(true, startAt, text, 0, 2) ? startAt : -1;
    }

    /**
     * Infer the selection value to use for a COUNT query.
     * Typically, the best we can do is to use the entity identifier variable,
     * For example, COUNT(o). This works for:
     *
     * <pre>
     * SELECT o ...
     * SELECT o.col1 ...
     * SELECT o.col1, o.col2 ...
     * SELECT NEW org.example.ClassName(o.col1, o.col2) ...
     * </pre>
     *
     * It should be noted we cannot use COUNT(o.col1) because it does not count
     * null values, making it inconsistent with the number of values returned by
     * SELECT o.col1.
     *
     * One place where COUNT(o) does not work is when selecting DISTINCT values.
     *
     * <pre>
     * SELECT DISTINCT o.col1 ...
     * </pre>
     *
     * In this case, the total number of matching entities would be incorrect.
     * Instead, we want the number of distinct values. This will work well if there
     * are no NULL values. But when there are NULL values, COUNT will omit then,
     * but the SELECT DISTINCT query will includes one NULL value in its results,
     * making the COUNT one less than it should be. It is unclear what to do
     * about this, except to point out that Jakarta Data is only required to
     * support JDQL and can choose to support as much of JPQL as it wishes, so here
     * we have the limitation that we are only supporting JPQL DISTINCT for
     * Page.totalElements and Page.totalPages when the results have no NULL values.
     *
     * @param ql        the query.
     * @param select0   position after SELECT, if present.
     * @param selectLen length of the text in the SELECT clause after SELECT,
     *                      if present, otherwise -1.
     * @return selection to use for a SELECT COUNT( {selection} ) query.
     */
    private String inferCountFromSelect(String ql, int select0, int selectLen) {
        // Look for DISTINCT in the selections
        for (int i = select0; i < select0 + selectLen - 9; i++) {
            char ch = ql.charAt(i);
            if ((ch == 'D' || ch == 'd') &&
                (i == select0 || !Character.isJavaIdentifierPart(ql.charAt(i - 1))) &&
                !Character.isJavaIdentifierPart(ql.charAt(i - 1)) &&
                ql.regionMatches(true, i + 1, "ISTINCT", 0, 7))
                return ql.substring(select0, select0 + selectLen);
        }

        return entityVar;
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
    @FFDCIgnore(Throwable.class) // report invalid repository methods as errors instead
    @Trivial
    QueryInfo init(Map<String, CompletableFuture<EntityInfo>> entityInfos, RepositoryImpl<?> repository) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "init", entityInfos, this);

        try {
            DataVersionCompatibility compat = repository.provider.compat;

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

            // spec-defined annotation types
            Delete delete = method.getAnnotation(Delete.class);
            Find find = method.getAnnotation(Find.class);
            Insert insert = method.getAnnotation(Insert.class);
            Update update = method.getAnnotation(Update.class);
            Save save = method.getAnnotation(Save.class);
            Query query = method.getAnnotation(Query.class);
            OrderBy[] orderBy = method.getAnnotationsByType(OrderBy.class);

            // experimental annotation types
            Annotation count = compat.getCountAnnotation(method);
            Annotation exists = compat.getExistsAnnotation(method);

            Annotation methodTypeAnno = //
                            validateAnnotationCombinations(delete, insert, update, save,
                                                           find, query, orderBy,
                                                           count, exists);

            if (query != null) { // @Query annotation
                initQueryLanguage(query.value(),
                                  entityInfos,
                                  repository.primaryEntityInfoFuture,
                                  compat);
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
                    q = initQueryByParameters(methodTypeAnno, countPages);
                } else {
                    // Query by Method Name
                    q = initQueryByMethodName(countPages);
                }

                if (type == Type.FIND_AND_DELETE
                    && multiType != null
                    && Stream.class.isAssignableFrom(multiType)) {
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1006.delete.rtrn.err",
                              method.getGenericReturnType().getTypeName(),
                              method.getName(),
                              repositoryInterface.getName(),
                              entityInfo.getType().getName(),
                              entityInfo.idType.getName());
                }
            }

            // The @OrderBy annotation from Jakarta Data provides sort criteria statically
            if (orderBy.length > 0) {
                // disallow on incompatible operations
                if (type != Type.FIND && type != Type.FIND_AND_DELETE)
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1096.orderby.incompat",
                              method.getName(),
                              repositoryInterface.getName());

                if (sorts != null) // also has an OrderBy keyword
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1090.orderby.conflict",
                              method.getName(),
                              repositoryInterface.getName());

                sorts = new ArrayList<>(orderBy.length);
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

                if (sortPositions.length == 0) {
                    sortPositions = NONE_STATIC_SORT_ONLY;
                    generateOrderBy(q);
                }
            }

            // Default to ascending by ID when the repository method that uses
            // pagination does not provide a way to indicate sort criteria.
            if (type == Type.FIND &&
                query == null && // do not change the user's Query value
                sortPositions.length == 0 &&
                (sorts == null || sorts.isEmpty()) &&
                (Page.class.equals(multiType) ||
                 CursoredPage.class.equals(multiType))) {

                sortPositions = NONE_STATIC_SORT_ONLY;
                String idAttr = entityInfo.attributeNames.get(ID);
                sorts = List.of(Sort.asc(idAttr));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "default sorting of " + sorts);
                generateOrderBy(q);
            }

            jpql = q == null ? jpql : q.toString();

            validate();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "init", new Object[] { this, entityInfo });
            return this;
        } catch (Throwable x) {
            String message = x.getMessage();
            if (message == null || !message.startsWith("CWWKD1"))
                Tr.error(tc, "CWWKD1000.repo.general.err",
                         method.getName(),
                         repositoryInterface.getName(),
                         x);
            // else the error was already logged
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "init", x);
            throw x;
        }

    }

    /**
     * Adds the specified 0-based index as a position where the repository method
     * provides sort criteria.
     *
     * @param index 0-based position to add.
     */
    @Trivial
    private void initDynamicSortPosition(int index) {
        if (sortPositions.length == 0) {
            sortPositions = new int[] { index };
        } else {
            // It is unusual, but supported, to have multiple parameters
            // provide sort criteria
            int[] previous = sortPositions;
            sortPositions = new int[previous.length + 1];
            System.arraycopy(previous, 0, sortPositions, 0, previous.length);
            sortPositions[sortPositions.length - 1] = index;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "found sort criteria position (1-based): " + (index + 1));
    }

    /**
     * Records positions (0-based) of all sort criteria in the method parameters
     * following the query parameters.
     *
     * @param paramTypes method parameter types.
     */
    @Trivial
    private void initDynamicSortPositions(Class<?>[] paramTypes) {
        for (int i = jpqlParamCount; i < paramTypes.length; i++)
            if (SORT_PARAM_TYPES.contains(paramTypes[i]))
                initDynamicSortPosition(i);
    }

    /**
     * Initializes query information based on the Query annotation.
     *
     * @param ql                      Query.value() might be JPQL or JDQL
     * @param entityInfos             map of entity name to entity information.
     * @param primaryEntityInfoFuture future for the repository's primary entity
     *                                    type if it has one, otherwise null.
     * @param compat                  isolates Jakarta Data version-dependent behavior
     */
    private void initQueryLanguage(String ql,
                                   Map<String, CompletableFuture<EntityInfo>> entityInfos,
                                   CompletableFuture<EntityInfo> primaryEntityInfoFuture,
                                   DataVersionCompatibility compat) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        boolean isCursoredPage = CursoredPage.class.equals(multiType);
        boolean countPages = isCursoredPage || Page.class.equals(multiType);

        // for collecting names of named parameters:
        LinkedHashSet<String> qlParamNames = new LinkedHashSet<>();

        int length = ql.length();
        int startAt = 0;
        char firstChar = ' ';
        for (; startAt < length && Character.isWhitespace(firstChar = ql.charAt(startAt)); startAt++);

        if (firstChar == 'D' || firstChar == 'd') { // DELETE FROM EntityName[ WHERE ...]
            if (startAt + 12 < length
                && ql.regionMatches(true, startAt + 1, "ELETE", 0, 5)
                && Character.isWhitespace(ql.charAt(startAt + 6))) {
                type = Type.QM_DELETE;
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
                    if (entityName.length() > 0)
                        setEntityInfo(entityName.toString(), entityInfos, ql);
                    else
                        throw exc(UnsupportedOperationException.class,
                                  "CWWKD1030.ql.lacks.entity",
                                  ql,
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  "DELETE",
                                  "DELETE FROM [entity_name] WHERE [conditional_expression]");

                    // skip whitespace
                    for (; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                    if (startAt >= length) {
                        // Entity identifier variable is not present.
                        entityVar = "this";
                        entityVar_ = "";
                        if (entityInfo.recordClass != null)
                            // Switch from record name to entity name
                            jpql = new StringBuilder(entityInfo.name.length() + 18) //
                                            .append("DELETE FROM ") //
                                            .append(entityInfo.name) //
                                            .toString();
                    } else if (startAt + 6 < length
                               && ql.regionMatches(true, startAt, "WHERE", 0, 5)
                               && !Character.isJavaIdentifierPart(ql.charAt(startAt + 5))) {
                        // Entity identifier variable is not present.
                        hasWhere = true;
                        entityVar = "this";
                        entityVar_ = "";

                        if (entityInfo.recordClass != null)
                            // Switch from record name to entity name
                            jpql = new StringBuilder(ql.length() + 6) //
                                            .append("DELETE FROM ") //
                                            .append(entityInfo.name) //
                                            .append(" WHERE") //
                                            .append(ql.substring(startAt + 5, ql.length())) //
                                            .toString();
                    }
                }
            }

            qlParamNames = findNamedParameters(ql, startAt);

            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, ql, "DELETE query",
                         "  " + jpql,
                         "  entity [" + entityInfo.name + "] [" + entityVar + "]",
                         "  :named " + qlParamNames);
        } else if (firstChar == 'U' || firstChar == 'u') { // UPDATE EntityName[ SET ... WHERE ...]
            if (startAt + 13 < length
                && ql.regionMatches(true, startAt + 1, "PDATE", 0, 5)
                && Character.isWhitespace(ql.charAt(startAt + 6))) {
                type = Type.QM_UPDATE;
                jpql = ql;
                startAt += 7; // start of EntityName
                for (; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                StringBuilder entityName = new StringBuilder();
                for (char ch; startAt < length && Character.isJavaIdentifierPart(ch = ql.charAt(startAt)); startAt++)
                    entityName.append(ch);
                if (entityName.length() > 0)
                    setEntityInfo(entityName.toString(), entityInfos, ql);
                else
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1030.ql.lacks.entity",
                              ql,
                              method.getName(),
                              repositoryInterface.getName(),
                              "UPDATE",
                              "UPDATE [entity_name] SET [update_items] WHERE [conditional_expression]");
                if (startAt + 1 < length && entityInfo.name.length() > 0 && Character.isWhitespace(ql.charAt(startAt))) {
                    for (startAt++; startAt < length && Character.isWhitespace(ql.charAt(startAt)); startAt++);
                    if (startAt + 4 < length
                        && ql.regionMatches(true, startAt, "SET", 0, 3)
                        && !Character.isJavaIdentifierPart(ql.charAt(startAt + 3))) {
                        entityVar = "this";
                        entityVar_ = "";

                        if (entityName.length() != entityInfo.name.length() || entityName.indexOf(entityInfo.name) != 0)
                            jpql = new StringBuilder(ql.length() * 3 / 2) //
                                            .append("UPDATE ").append(entityInfo.name).append(" SET") //
                                            .append(jpql.substring(startAt + 3, ql.length())) //
                                            .toString();
                    }
                }
            }

            qlParamNames = findNamedParameters(ql, startAt);

            if (trace && tc.isDebugEnabled())
                Tr.debug(tc, ql, "UPDATE query",
                         "  " + jpql,
                         "  entity [" + entityInfo.name + "] [" + entityVar + "]",
                         "  :named " + qlParamNames);
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

            boolean isEmbedded = false;
            boolean isLiteral = false;
            StringBuilder paramName = null;
            for (; startAt < length; startAt++) {
                char ch = ql.charAt(startAt);
                if (!isLiteral && (ch == ':' || ch == '.')) {
                    if (ch == ':')
                        paramName = new StringBuilder(30);
                    else
                        isEmbedded = true;
                } else if (ch == '\'') {
                    if (isLiteral) {
                        if (startAt + 1 < length && ql.charAt(startAt + 1) == '\'')
                            startAt++; // escaped ' within a literal
                        else
                            isLiteral = false;
                    } else {
                        isLiteral = true;
                        if (isEmbedded) {
                            isEmbedded = false;
                        } else if (paramName != null) {
                            qlParamNames.add(paramName.toString());
                            paramName = null;
                        }
                    }
                } else if (Character.isJavaIdentifierStart(ch)) {
                    if (paramName != null) {
                        paramName.append(ch);
                        while (length > startAt + 1 && Character //
                                        .isJavaIdentifierPart(ch = ql.charAt(startAt + 1))) {
                            paramName.append(ch);
                            startAt++;
                        }
                    } else if (!isEmbedded && !isLiteral) {
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
                    if (paramName != null)
                        paramName.append(ch);
                } else if (!isLiteral) {
                    if (isEmbedded) {
                        isEmbedded = false;
                    } else if (paramName != null) {
                        qlParamNames.add(paramName.toString());
                        paramName = null;
                    }
                }
            }
            if (paramName != null) {
                qlParamNames.add(paramName.toString());
                paramName = null;
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
                entityName0 = startAt; // starts at EntityName
                for (; startAt < from0 + fromLen && Character.isJavaIdentifierPart(ql.charAt(startAt)); startAt++);
                if ((entityNameLen = startAt - entityName0) > 0) {
                    String entityName = ql.substring(entityName0, entityName0 + entityNameLen);
                    setEntityInfo(entityName, entityInfos, ql);

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
                } else {
                    String queryType = selectLen > 0 ? "SELECT" : "FROM";
                    String example = (selectLen > 0 ? "SELECT [select_list] " : "") +
                                     "FROM [entity_name] WHERE [conditional_expression]";
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1030.ql.lacks.entity",
                              ql,
                              method.getName(),
                              repositoryInterface.getName(),
                              queryType,
                              example);
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
                         "  entity [" + entityName + "] [" + entityVar + "]",
                         "  :named " + qlParamNames);
            }

            boolean hasEntityVar = entityVar_.length() > 0;

            // TODO remove this workaround for #30351 once fixed and run the _fat_jpa bucket to verify
            boolean insertEntityVar = entityVar_.length() == 0 && !entityInfo.relationAttributeNames.isEmpty();
            if (insertEntityVar)
                entityVar_ = entityVar + ".";

            if (countPages) {
                // TODO count query cannot always be accurately inferred if Query value is JPQL
                StringBuilder c = new StringBuilder("SELECT COUNT(");
                c.append(inferCountFromSelect(ql, select0, selectLen));

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

                if (whereLen > 0)
                    if (insertEntityVar) {
                        c.append("WHERE");
                        appendWithIdentifierName(ql, where0, where0 + whereLen, entityVar_, c);
                    } else
                        c.append("WHERE").append(ql.substring(where0, where0 + whereLen));

                jpqlCount = c.toString();

                if (trace && tc.isDebugEnabled())
                    Tr.debug(tc, ql, "count query: " + jpqlCount);
            }

            if (isCursoredPage) {
                if (order0 >= 0)
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1033.ql.orderby.disallowed",
                              method.getName(),
                              repositoryInterface.getName(),
                              CursoredPage.class.getSimpleName(),
                              OrderBy.class.getSimpleName(),
                              ql);

                if (whereLen > 0) {
                    if (where0 + whereLen != length)
                        throw exc(UnsupportedOperationException.class,
                                  "CWWKD1034.ql.where.required",
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  CursoredPage.class.getSimpleName(),
                                  where0 + whereLen,
                                  length,
                                  ql);

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
            }

            StringBuilder q;
            if (selectLen > 0) {
                q = new StringBuilder(ql.length() + (selectLen >= 0 ? 0 : 50) + (fromLen >= 0 ? 0 : 50) + 2);
                String selection = ql.substring(select0, select0 + selectLen);
                if (insertEntityVar) {
                    q.append("SELECT");
                    appendWithIdentifierName(ql, select0, select0 + selectLen, entityVar_, q);
                } else {
                    q.append("SELECT").append(selection);
                }
                if (fromLen == 0 && whereLen == 0 && orderLen == 0 &&
                    !Character.isWhitespace(q.charAt(q.length() - 1)))
                    q.append(' ');
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

            if (whereLen > 0)
                // TODO once fixed, test #30351 by adding: && !"this.".equalsIgnoreCase(entityVar_)
                // and running DataJPATestServlet.testCountQueryWithFromAndWhereClausesOnly
                if (insertEntityVar) {
                    q.append("WHERE");
                    appendWithIdentifierName(ql, where0, where0 + whereLen, entityVar_, q);
                } else {
                    q.append("WHERE").append(ql.substring(where0, where0 + whereLen));
                }

            if (orderLen > 0)
                if (insertEntityVar)
                    appendWithIdentifierName(ql, order0, order0 + orderLen, entityVar_, q);
                else
                    q.append(ql.substring(order0, order0 + orderLen));

            jpql = q.toString();
        }

        // Find out how many parameters the method supplies to the query
        // and which of those parameters are named parameters.
        int qlParamNameCount = qlParamNames.size();
        boolean hasExtraParam = false;
        Parameter[] params = method.getParameters();
        Set<Class<?>> specParamTypes = compat.specialParamTypes();
        for (int i = 0; i < params.length &&
                        !specParamTypes.contains(params[i].getType()); //
                        jpqlParamCount = ++i) {
            Param param = params[i].getAnnotation(Param.class);
            String paramName = null;
            if (param != null) {
                // @Param annotation
                paramName = param.value();
            } else if (qlParamNameCount > 0 && params[i].isNamePresent()) {
                // name of parameter (if using -parameters)
                paramName = params[i].getName();
            }
            if (paramName != null) {
                if (jpqlParamNames.isEmpty())
                    jpqlParamNames = new LinkedHashSet<>();
                boolean isDuplicate = !jpqlParamNames.add(paramName);
                if (qlParamNames.contains(paramName)) {
                    if (isDuplicate) // duplicate of a valid name
                        throw exc(MappingException.class,
                                  "CWWKD1083.dup.method.param",
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  paramName,
                                  "@Param(\"" + paramName + "\")",
                                  params[i].getType().getSimpleName() + ' ' + paramName);
                } else {
                    hasExtraParam = true;
                }
            }
        }

        sortPositions = NONE_QUERY_LANGUAGE_ONLY;
        for (int i = jpqlParamCount; i < params.length; i++)
            if (SORT_PARAM_TYPES.contains(params[i].getType()))
                initDynamicSortPosition(i);

        int paramNamesCount = jpqlParamNames.size();
        if (hasExtraParam || qlParamNameCount != paramNamesCount) {
            // Does the method supply all named parameters that the query needs?
            LinkedHashSet<String> lacking = new LinkedHashSet<>(qlParamNames);
            lacking.removeAll(jpqlParamNames);
            if (!lacking.isEmpty())
                throw excLackingMethodArgNamedParams(lacking);

            // Does the method supply any named parameters not needed by the query?
            Set<String> extras = new LinkedHashSet<>(jpqlParamNames);
            extras.removeAll(qlParamNames);
            if (!extras.isEmpty())
                throw excExtraMethodArgNamedParams(extras, qlParamNames);
        }

        // Does the method supply a mixture of named and positional parameters?
        if (paramNamesCount > 0 && paramNamesCount < jpqlParamCount)
            throw excMixedQLParamTypes(paramNamesCount);
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

            initDynamicSortPositions(method.getParameterTypes());
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
                type = Type.FIND_AND_DELETE;
                q = generateSelectClause().append(" FROM ").append(entityInfo.name).append(' ').append(o);
                jpqlDelete = generateDeleteById();
            } else { // DELETE
                type = Type.QM_DELETE;
                q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name).append(' ').append(o);
            }

            if (by > 0)
                generateWhereClause(methodName, by + 2, orderBy > 0 ? orderBy : methodName.length(), q);

            initDynamicSortPositions(method.getParameterTypes());
            if (orderBy > 0)
                parseOrderBy(orderBy, q);

            type = type == null ? Type.QM_DELETE : type;
        } else if (methodName.startsWith("update")) {
            int c = by < 0 ? 6 : (by + 2);
            q = generateUpdateClause(methodName, c);
            type = Type.QM_UPDATE;
        } else if (methodName.startsWith("count")) {
            q = new StringBuilder(150).append("SELECT COUNT(").append(o).append(") FROM ").append(entityInfo.name).append(' ').append(o);
            if (by > 0 && methodName.length() > by + 2)
                generateWhereClause(methodName, by + 2, methodName.length(), q);
            type = Type.COUNT;
        } else if (methodName.startsWith("exists")) {
            q = new StringBuilder(200) //
                            .append("SELECT ID(").append(o).append(") FROM ") //
                            .append(entityInfo.name).append(' ').append(o);
            if (by > 0 && methodName.length() > by + 2)
                generateWhereClause(methodName, by + 2, methodName.length(), q);
            type = Type.EXISTS;
            validateReturnForExists();
        } else {
            throw excUnsupportedMethod();
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
            Tr.entry(this, tc, "initQueryByParameters",
                     methodTypeAnno == null ? null : methodTypeAnno.annotationType().getSimpleName(),
                     countPages);

        String o = entityVar;
        StringBuilder q = null;

        if (methodTypeAnno instanceof Find || methodTypeAnno instanceof Update) {
            q = generateQueryByParameters(null, methodTypeAnno, countPages);
        } else if (methodTypeAnno instanceof Delete) {
            if (isFindAndDelete()) {
                type = Type.FIND_AND_DELETE;
                q = generateSelectClause().append(" FROM ").append(entityInfo.name).append(' ').append(o);
                jpqlDelete = generateDeleteById();
            } else { // DELETE
                type = Type.QM_DELETE;
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
            validateReturnForExists();
            q = new StringBuilder(200) //
                            .append("SELECT ID(").append(o).append(") FROM ") //
                            .append(entityInfo.name).append(' ').append(o);
            if (method.getParameterCount() > 0)
                generateQueryByParameters(q, methodTypeAnno, countPages);
        } else {
            // unreachable
            throw new IllegalArgumentException(methodTypeAnno.toString());
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "initQueryByParameters", new Object[] { q, type });

        return q;
    }

    /**
     * Inserts entities (or records) into the database.
     * An error is raised if any of the entities (or records) already exist
     * in the database.
     *
     * @param arg the entity or record, or array/Iterable/Stream of entity or record
     * @param em  the entity manager
     * @return the inserted entities, using the return type that is required by the
     *         Insert method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial
    Object insert(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "insert", loggable(arg));

        boolean resultVoid = void.class.equals(singleType) ||
                             Void.class.equals(singleType);
        ArrayList<Object> results;

        boolean hasSingularEntityParam = false;
        int entityCount = 0;
        if (entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = resultVoid ? null : new ArrayList<>(length);
            for (; entityCount < length; entityCount++) {
                Object entity = toEntity(Array.get(arg, entityCount));
                em.persist(entity);
                if (results != null)
                    results.add(entity);
            }
            em.flush();
        } else if (arg instanceof Iterable) {
            results = resultVoid ? null : new ArrayList<>();
            for (Object e : ((Iterable<?>) arg)) {
                entityCount++;
                Object entity = toEntity(e);
                em.persist(entity);
                if (results != null)
                    results.add(entity);
            }
            em.flush();
        } else {
            entityCount = 1;
            hasSingularEntityParam = true;
            results = resultVoid ? null : new ArrayList<>(1);
            Object entity = toEntity(arg);
            em.persist(entity);
            em.flush();
            if (results != null)
                results.add(entity);
        }

        if (entityCount == 0)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1092.lifecycle.arg.empty",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericParameterTypes()[0].getTypeName());

        Class<?> returnType = method.getReturnType();
        Object returnValue;
        if (resultVoid) {
            returnValue = null;
        } else {
            if (entityInfo.recordClass != null)
                for (int i = 0; i < results.size(); i++)
                    results.set(i, entityInfo.toRecord(results.get(i)));

            if (returnArrayType != null) {
                Object[] newArray = (Object[]) Array.newInstance(returnArrayType,
                                                                 results.size());
                returnValue = results.toArray(newArray);
            } else {
                if (multiType == null)
                    if (results.size() == 1)
                        returnValue = results.get(0);
                    else if (results.isEmpty())
                        returnValue = null;
                    else
                        throw exc(ClassCastException.class,
                                  "CWWKD1094.return.mismatch",
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  method.getGenericReturnType().getTypeName(),
                                  results.size(),
                                  "@Insert",
                                  lifeCycleReturnTypes(entityInfo.getType().getName(),
                                                       hasSingularEntityParam,
                                                       false));
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = convertToIterable(results, multiType, null, null);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw exc(MappingException.class,
                              "CWWKD1003.rtrn.err",
                              method.getGenericReturnType().getTypeName(),
                              method.getName(),
                              repositoryInterface.getName(),
                              "Insert",
                              lifeCycleReturnTypes(results.get(0).getClass().getSimpleName(),
                                                   hasSingularEntityParam,
                                                   false));
            }
        }

        if (CompletableFuture.class.equals(returnType) ||
            CompletionStage.class.equals(returnType)) {
            // useful for @Asynchronous
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw exc(MappingException.class,
                      "CWWKD1003.rtrn.err",
                      method.getGenericReturnType().getTypeName(),
                      method.getName(),
                      repositoryInterface.getName(),
                      "Insert",
                      lifeCycleReturnTypes(results.get(0).getClass().getSimpleName(),
                                           hasSingularEntityParam,
                                           false));
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "insert", loggable(returnValue));
        return returnValue;
    }

    /**
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     */
    @Trivial
    public void introspect(PrintWriter writer, String indent) {
        writer.println(indent + "QueryInfo@" + Integer.toHexString(hashCode()));
        indent = indent + "  ";
        writer.println(indent + "entity: " + entityInfo);
        writer.println(indent + "repository: " + repositoryInterface.getName());

        // method signature information
        java.lang.reflect.Type[] paramTypes = method.getGenericParameterTypes();
        Annotation[][] paramAnnos = method.getParameterAnnotations();
        Parameter[] params = method.getParameters();
        for (Annotation anno : method.getAnnotations())
            writer.println(indent + anno);
        writer.println(indent + method.getGenericReturnType().getTypeName() + ' ' +
                       method.getName() + (paramTypes.length == 0 ? "()" : "("));
        for (int i = 0; i < paramTypes.length; i++) {
            for (Annotation paramAnno : paramAnnos[i])
                writer.println(indent + "  " + paramAnno);
            writer.println(indent + "  " + paramTypes[i].getTypeName() + ' ' +
                           params[i].getName() +
                           (i == paramTypes.length - 1 ? ')' : ','));
        }

        writer.println(indent + "return array type: " +
                       (returnArrayType == null ? null : returnArrayType.getName()));
        writer.println(indent + "multiple result type: " +
                       (multiType == null ? null : multiType.getName()));
        writer.println(indent + "single result type: " +
                       (singleType == null ? null : singleType.getName()));
        writer.println(indent + "collection or array element type of single result type: " +
                       (singleTypeElementType == null ? null : singleTypeElementType.getName()));
        writer.println(indent + "result is Optional? " + isOptional);

        writer.println(indent + "entity identifier variable: " + entityVar +
                       " [" + entityVar_ + "]");

        writer.println(indent + "hasWhere? " + hasWhere);
        writer.println(indent + "type: " + type);
        writer.println(indent + "life cycle method entity parameter type: " +
                       (entityParamType == null ? null : entityParamType.getName()));

        final String jpqlIndent = indent + "      ";
        writer.print(indent + "JPQL: ");
        Util.printlnIndented(jpql, writer, jpqlIndent);
        writer.print(indent + "JPQL for afterCursor: ");
        Util.printlnIndented(jpqlAfterCursor, writer, jpqlIndent);
        writer.print(indent + "JPQL for jpqlBeforeCursor: ");
        Util.printlnIndented(jpqlBeforeCursor, writer, jpqlIndent);
        writer.print(indent + "JPQL count query: ");
        Util.printlnIndented(jpqlCount, writer, jpqlIndent);
        writer.print(indent + "JPQL delete query: ");
        Util.printlnIndented(jpqlDelete, writer, jpqlIndent);

        writer.println(indent + "JPQL parameter count: " + jpqlParamCount);
        writer.println(indent + "JPQL parameter names: " + jpqlParamNames);

        writer.println(indent + "maximum results: " + maxResults);
        writer.println(indent + "sorts: " + sorts);
        writer.print(indent + "positions of sort-related method parameters: ");
        if (sortPositions.length == 0)
            if (sortPositions == NONE)
                writer.println("no sort parameters and no static sort");
            else if (sortPositions == NONE_QUERY_LANGUAGE_ONLY)
                writer.println("no sort parameters, but has @Query");
            else if (sortPositions == NONE_STATIC_SORT_ONLY)
                writer.println("no sort parameters, but has OrderBy");
            else
                writer.println();
        else
            writer.println(Arrays.toString(sortPositions));

        writer.println(indent + "validate method parameters? " + validateParams);
        writer.println(indent + "validate method result? " + validateResult);
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
    @SuppressWarnings("unlikely-arg-type")
    @Trivial
    private boolean isFindAndDelete() {

        boolean isFindAndDelete = isOptional
                                  || multiType != null
                                  || !Util.RETURN_TYPES_FOR_DELETE_ONLY.contains(singleType);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isFindAndDelete? " + isFindAndDelete +
                               "; optional?: " + isOptional +
                               "; multiType: " + (multiType == null ? null : multiType.getSimpleName()) +
                               "; singleType: " + (singleType == null ? null : singleType.getSimpleName()));

        if (isFindAndDelete)
            if (type != null
                && !type.equals(entityInfo.entityClass)
                && !type.equals(entityInfo.recordClass)
                && !type.equals(Object.class)
                && !Util.wrapperClassIfPrimitive(singleType) //
                                .equals(Util.wrapperClassIfPrimitive(entityInfo.idType))) {
                throw exc(MappingException.class,
                          "CWWKD1006.delete.rtrn.err",
                          method.getGenericReturnType().getTypeName(),
                          method.getName(),
                          repositoryInterface.getName(),
                          entityInfo.getType().getName(),
                          entityInfo.idType.getName());
            }

        return isFindAndDelete;
    }

    /**
     * Prepare a value, which might include customer data, for logging.
     * If the repository class/package/method is not considered loggable
     * then return a copy of the value for logging where customer data
     * is replaced with a placeholder.
     *
     * @param value value.
     * @return loggable value.
     */
    @Trivial
    final Object loggable(Object value) {
        return entityInfo.builder.provider.loggable(repositoryInterface,
                                                    method,
                                                    value);
    }

    /**
     * Appends a suffix if the repository class/package/method is considered
     * loggable. Otherwise returns only the prefix.
     *
     * @param prefix         first part of value to always include.
     * @param possibleSuffix suffix to only include if logValues allows.
     * @return loggable value.
     */
    @Trivial
    private final String loggableAppend(String prefix, Object... possibleSuffix) {
        return entityInfo.builder.provider.loggableAppend(repositoryInterface,
                                                          method,
                                                          prefix,
                                                          possibleSuffix);
    }

    /**
     * Raise an error because the PageRequest is missing.
     *
     * @throws IllegalArgumentException      if the user supplied a null PageRequest
     * @throws UnsupportedOperationException if the repository method signature
     *                                           lacks a parameter for supplying a
     *                                           PageRequest
     */
    void missingPageRequest() {
        Class<?>[] paramTypes = method.getParameterTypes();

        // Check parameter positions after those used for query parameters
        boolean signatureHasPageReq = false;
        for (int i = jpqlParamCount; i < paramTypes.length; i++)
            signatureHasPageReq |= PageRequest.class.equals(paramTypes[i]);

        if (signatureHasPageReq)
            // NullPointerException is required by BasicRepository.findAll
            throw new NullPointerException("PageRequest: null");
        else
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1041.rtrn.mismatch.pagereq",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericReturnType().getTypeName());
    }

    /**
     * Requires a single result.
     *
     * @param results list of results that is expected to have exactly 1 result.
     * @return the single result.
     * @throws EmptyResultException     if the list is empty.
     * @throws NonUniqueResultException if the list has more than 1 result.
     */
    @Trivial
    private final Object oneResult(List<?> results) {
        int size = results.size();
        if (size == 1)
            return results.get(0);
        else if (size == 0)
            throw excEmptyResult();
        else
            throw excNonUniqueResult(results.size());
    }

    /**
     * Parses and handles the text after the find keyword of the find clause,
     * such as find___By or find___OrderBy or find___.
     * Currently the only keyword supported within this portion of the find clause
     * is First, which can be optionally followed by a number.
     * "Distinct" is reserved for future use.
     * Other characters in the clause are ignored.
     *
     * @param by index of first occurrence of "By" or "OrderBy" in the method name.
     *               -1 if both are absent.
     */
    private void parseFindClause(int by) {
        String methodName = method.getName();
        int start = 4;
        int endBefore = by == -1 ? methodName.length() : by;

        for (boolean first = methodName.regionMatches(start, "First", 0, 5),
                        distinct = !first && methodName //
                                        .regionMatches(start, "Distinct", 0, 8); //
                        first || distinct;)
            if (first) {
                start = parseFirst(start += 5, endBefore);
                first = false;
                distinct = methodName.regionMatches(start, "Distinct", 0, 8);
            } else if (distinct) {
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1056.unsupported.keyword",
                          method.getName(),
                          repositoryInterface.getName(),
                          "Distinct");
            }
    }

    /**
     * Parses the number (if any) following findFirst.
     *
     * @param start     starting position after findFirst.
     * @param endBefore index of first occurrence of "By" in the method name,
     *                      or otherwise the method name length.
     * @return next starting position after the findFirst(#).
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
                        throw exc(UnsupportedOperationException.class,
                                  "CWWKD1028.first.exceeds.max",
                                  methodName,
                                  repositoryInterface.getName(),
                                  methodName.substring(0, endBefore),
                                  "Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
                    start++;
                } else {
                    if (num == 0)
                        num = 1;
                    break;
                }
            }
        if (num == 0)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1029.first.neg.or.zero",
                      methodName,
                      repositoryInterface.getName(),
                      0);
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

        if (sortPositions == NONE && !sorts.isEmpty()) {
            sortPositions = NONE_STATIC_SORT_ONLY;
            generateOrderBy(q);
        }
    }

    /**
     * Pagination is only possible if results are ordered.
     *
     * If the repository method has parameters to supply an order and these are
     * empty or null, assume the user made a mistake and raise an error.
     *
     * @param args parameters to the repository method.
     * @throws IllegalArgumentException if a Sort[] or Order parameter is empty.
     * @throws NullPointerException     if a Sort or Order parameter is null.
     */
    @Trivial
    private void requireOrderedPagination(Object[] args) {
        if (sortPositions.length > 0) {
            Class<?>[] paramTypes = method.getParameterTypes();
            for (int s = 0; s < sortPositions.length; s++) {
                int p = sortPositions[s];
                if (Order.class.equals(paramTypes[p]) ||
                    Sort.class.equals(paramTypes[p]))
                    if (args[p] == null)
                        // BasicRepository.findAll(PageRequest, Order) requires
                        // NullPointerException when Order is null.
                        throw exc(NullPointerException.class,
                                  "CWWKD1087.null.param",
                                  paramTypes[p].getName(),
                                  method.getName(),
                                  repositoryInterface.getName());
                    else
                        throw exc(IllegalArgumentException.class,
                                  "CWWKD1088.empty.sorts",
                                  paramTypes[p].getName(),
                                  method.getName(),
                                  repositoryInterface.getName());
                else if (Sort[].class.equals(paramTypes[p]))
                    throw exc(IllegalArgumentException.class,
                              "CWWKD1088.empty.sorts",
                              Sort.class.getName() + "[]",
                              method.getName(),
                              repositoryInterface.getName());
            }
        }

        if (sortPositions == NONE)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1089.unordered.pagination",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericReturnType().getTypeName());
    }

    /**
     * Saves entities (or records) to the database, which can involve an update
     * or an insert, depending on whether the entity already exists.
     *
     * @param arg the entity or record, or array/Iterable/Stream of entity or record
     * @param em  the entity manager.
     * @return the updated entities, using the return type that is required by the
     *         repository Save method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial // avoid logging customer data
    Object save(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "save", loggable(arg));

        boolean resultVoid = void.class.equals(singleType) ||
                             Void.class.equals(singleType);
        List<Object> results;

        boolean hasSingularEntityParam = false;
        int entityCount = 0;
        if (entityParamType.isArray()) {
            results = new ArrayList<>();
            int length = Array.getLength(arg);
            for (; entityCount < length; entityCount++)
                results.add(em.merge(toEntity(Array.get(arg, entityCount))));
            em.flush();
        } else if (Iterable.class.isAssignableFrom(entityParamType)) {
            results = new ArrayList<>();
            for (Object e : ((Iterable<?>) arg)) {
                entityCount++;
                results.add(em.merge(toEntity(e)));
            }
            em.flush();
        } else {
            entityCount = 1;
            hasSingularEntityParam = true;
            results = resultVoid ? null : new ArrayList<>(1);
            Object entity = em.merge(toEntity(arg));
            if (results != null)
                results.add(entity);
            em.flush();
        }

        if (entityCount == 0)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1092.lifecycle.arg.empty",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericParameterTypes()[0].getTypeName());

        Class<?> returnType = method.getReturnType();
        Object returnValue;
        if (resultVoid) {
            returnValue = null;
        } else {
            if (entityInfo.recordClass != null)
                for (int i = 0; i < results.size(); i++)
                    results.set(i, entityInfo.toRecord(results.get(i)));

            if (returnArrayType != null) {
                Object[] newArray = (Object[]) Array.newInstance(returnArrayType, results.size());
                returnValue = results.toArray(newArray);
            } else {
                if (multiType == null)
                    if (results.size() == 1)
                        returnValue = results.get(0);
                    else if (results.isEmpty())
                        returnValue = null;
                    else
                        throw exc(ClassCastException.class,
                                  "CWWKD1094.return.mismatch",
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  method.getGenericReturnType().getTypeName(),
                                  results.size(),
                                  "@Save",
                                  lifeCycleReturnTypes(entityInfo.getType().getName(),
                                                       hasSingularEntityParam,
                                                       false));
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = convertToIterable(results, multiType, null, null);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw exc(MappingException.class,
                              "CWWKD1003.rtrn.err",
                              method.getGenericReturnType().getTypeName(),
                              method.getName(),
                              repositoryInterface.getName(),
                              "Save",
                              lifeCycleReturnTypes(results.get(0).getClass().getSimpleName(),
                                                   hasSingularEntityParam,
                                                   false));
            }
        }

        if (CompletableFuture.class.equals(returnType) ||
            CompletionStage.class.equals(returnType)) {
            // useful for @Asynchronous
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw exc(MappingException.class,
                      "CWWKD1003.rtrn.err",
                      method.getGenericReturnType().getTypeName(),
                      method.getName(),
                      repositoryInterface.getName(),
                      "Save",
                      lifeCycleReturnTypes(results.get(0).getClass().getSimpleName(),
                                           hasSingularEntityParam,
                                           false));
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "save", loggable(returnValue));
        return returnValue;
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
            throw exc(MappingException.class,
                      "CWWKD1001.no.primary.entity",
                      method.getName(),
                      repositoryInterface.getName(),
                      "DataRepository<EntityClass, EntityIdClass>");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && !primaryEntityInfoFuture.isDone())
            Tr.debug(this, tc, "await completion of primary entity info", primaryEntityInfoFuture);

        entityInfo = primaryEntityInfoFuture.join();
    }

    /**
     * Locate the entity information for the specified entity name.
     *
     * @param entityName  case sensitive entity name obtained from JDQL or JPQL.
     * @param entityInfos map of entity name to already-completed future for the entity information.
     * @param ql          query language.
     * @throws MappingException if the entity information is not found.
     */
    @Trivial
    private void setEntityInfo(String entityName,
                               Map<String, CompletableFuture<EntityInfo>> entityInfos,
                               String ql) {
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
                        throw exc(MappingException.class,
                                  "CWWKD1031.ql.similar.entity",
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  entityName,
                                  name,
                                  ql);
                }

                future = entityInfos.get(EntityInfo.FAILED);
                if (future == null)
                    throw exc(MappingException.class,
                              "CWWKD1032.ql.unknown.entity",
                              method.getName(),
                              repositoryInterface.getName(),
                              entityName,
                              List.of("Insert", "Save", "Update", "Delete"),
                              ql);
            }
        } else {
            entityInfo = future.join();
        }
    }

    /**
     * Sets the query parameter at the specified position to a value from the entity,
     * obtained via the accessor methods.
     *
     * @param p        parameter position.
     * @param query    the query.
     * @param entity   the entity.
     * @param attrName entity attribute name.
     * @throws Exception if an error occurs.
     */
    @Trivial
    private void setParameter(int p,
                              jakarta.persistence.Query query,
                              Object entity,
                              String attrName) throws Exception {
        Object v = entity;
        for (Member accessor : entityInfo.attributeAccessors.get(attrName))
            v = accessor instanceof Method ? ((Method) accessor).invoke(v) : ((Field) accessor).get(v);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "set ?" + p + ' ' + loggable(v));

        query.setParameter(p, v);
    }

    /**
     * Sets query parameters from repository method arguments.
     *
     * @param query the query
     * @param args  repository method arguments
     * @throws Exception if an error occurs
     */
    @Trivial // avoid logging customer data
    void setParameters(jakarta.persistence.Query query, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Iterator<String> namedParams = jpqlParamNames.iterator();
        for (int i = 0, p = 0; i < jpqlParamCount; i++) {
            Object arg = args[i];

            if (namedParams.hasNext()) {
                String paramName = namedParams.next();
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "set :" + paramName + ' ' + loggable(arg));
                query.setParameter(paramName, arg);
                p++;
            } else { // positional parameter
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "set ?" + (p + 1) + ' ' + loggable(arg));
                query.setParameter(++p, arg);
            }
        }
    }

    /**
     * Sets query parameters from cursor element values.
     *
     * @param query  the query
     * @param cursor the cursor
     * @throws Exception if an error occurs
     */
    void setParametersFromCursor(jakarta.persistence.Query query, PageRequest.Cursor cursor) throws Exception {
        int paramNum = jpqlParamCount; // position before that of first cursor element
        if (jpqlParamNames.isEmpty()) // positional parameters
            for (int i = 0; i < cursor.size(); i++) {
                Object value = cursor.get(i);
                if (entityInfo.idClassAttributeAccessors != null &&
                    entityInfo.idType.isInstance(value)) {
                    // Expand ID(THIS) for composite IdClass into separate attributes
                    for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                        Object v = accessor instanceof Field //
                                        ? ((Field) accessor).get(value) //
                                        : ((Method) accessor).invoke(value);
                        if (++paramNum - jpqlParamCount > sorts.size())
                            cursorSizeMismatchError(cursor);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set [cursor] ?" + paramNum + ' ' +
                                               loggable(value) + "-->" +
                                               loggable(v));
                        query.setParameter(paramNum, v);
                    }
                } else {
                    if (++paramNum - jpqlParamCount > sorts.size())
                        cursorSizeMismatchError(cursor);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set [cursor] ?" + paramNum + ' ' +
                                           loggable(value));
                    query.setParameter(paramNum, value);
                }
            }
        else // named parameters
            for (int i = 0; i < cursor.size(); i++) {
                Object value = cursor.get(i);
                if (entityInfo.idClassAttributeAccessors != null &&
                    entityInfo.idType.isInstance(value)) {
                    // Expand ID(THIS) for composite IdClass into separate attributes
                    for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                        Object v = accessor instanceof Field //
                                        ? ((Field) accessor).get(value) //
                                        : ((Method) accessor).invoke(value);
                        if (++paramNum - jpqlParamCount > sorts.size())
                            cursorSizeMismatchError(cursor);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set [cursor] :cursor" + paramNum +
                                               ' ' + value.getClass().getName() + "-->" +
                                               (v == null ? null : v.getClass().getSimpleName()));
                        query.setParameter("cursor" + paramNum, v);
                    }
                } else {
                    if (++paramNum - jpqlParamCount > sorts.size())
                        cursorSizeMismatchError(cursor);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set [cursor] :cursor" + paramNum + ' ' +
                                           (value == null ? null : value.getClass().getSimpleName()));
                    query.setParameter("cursor" + paramNum, value);
                }
            }

        if (sorts.size() > paramNum - jpqlParamCount) // not enough cursor elements
            cursorSizeMismatchError(cursor);
    }

    /**
     * Sets query parameters for DELETE_WITH_ENTITY_PARAM where the entity has an IdClass.
     *
     * @param startingParamIndex index of first parameter to set.
     * @param query              the query
     * @param entity             the entity
     * @param version            the version if versioned, otherwise null.
     * @throws Exception if an error occurs
     */
    @Trivial // avoid tracing customer data
    private void setParametersFromIdClassAndVersion(int startingParamIndex,
                                                    jakarta.persistence.Query query,
                                                    Object entity,
                                                    Object version) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        int p = startingParamIndex;
        for (String idClassAttr : entityInfo.idClassAttributeAccessors.keySet())
            setParameter(p++, query, entity, getAttributeName(idClassAttr, true));

        if (version != null) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "set ?" + p + ' ' + version);
            query.setParameter(p++, version);
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
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1009.lifecycle.param.err",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getParameterCount(),
                      annoClass.getSimpleName());
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
    private List<Sort<Object>> supplySorts(List<Sort<Object>> combined,
                                           Iterable<Sort<Object>> additional) {
        Iterator<Sort<Object>> addIt = additional.iterator();
        boolean hasIdClass = entityInfo.idClassAttributeAccessors != null;
        if (combined == null && addIt.hasNext())
            combined = sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
        while (addIt.hasNext()) {
            Sort<Object> sort = addIt.next();
            if (sort == null)
                throw new IllegalArgumentException("Sort: null");
            // IdClass is split up so that it can be possible to create a cursor
            // that corresponds to sort criteria
            else if (hasIdClass && ID.equalsIgnoreCase(sort.property()))
                for (String name : entityInfo.idClassAttributeAccessors.keySet())
                    combined.add(getWithAttributeName(getAttributeName(name, true), sort));
            else
                combined.add(getWithAttributeName(sort.property(), sort));
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
    private List<Sort<Object>> supplySorts(List<Sort<Object>> combined,
                                           @SuppressWarnings("unchecked") Sort<Object>... additional) {
        boolean hasIdClass = entityInfo.idClassAttributeAccessors != null;
        if (combined == null && additional.length > 0)
            combined = sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
        for (Sort<Object> sort : additional) {
            if (sort == null)
                throw new IllegalArgumentException("Sort: null");
            // IdClass is split up so that it can be possible to create a cursor
            // that corresponds to sort criteria
            else if (hasIdClass && ID.equalsIgnoreCase(sort.property()))
                for (String name : entityInfo.idClassAttributeAccessors.keySet())
                    combined.add(getWithAttributeName(getAttributeName(name, true), sort));
            else
                combined.add(getWithAttributeName(sort.property(), sort));
        }
        return combined;
    }

    /**
     * Functional interface that can be supplied to stream.mapToDouble.
     *
     * @param o object to convert.
     * @return double value.
     */
    @Trivial
    private final double toDouble(Object o) {
        return (Double) convert(o, double.class, true);
    }

    /**
     * Converts a record to its generated entity equivalent,
     * or does nothing if not a record.
     *
     * @param o a record that needs conversion to an entity,
     *              or an entity that is already an entity and does not
     *              need conversion.
     * @return entity.
     * @throws NullPointerException if the record is null, with a CWWKD1015 message
     *                                  that is appropriate for life cycle operations
     */
    @Trivial
    private final Object toEntity(Object o) {
        if (o == null)
            throw exc(NullPointerException.class,
                      "CWWKD1015.null.entity.param",
                      method.getName(),
                      repositoryInterface.getName());

        Object entity = o;
        Class<?> oClass = o.getClass();
        if (oClass.isRecord())
            try {
                Class<?> entityClass = oClass.getClassLoader() //
                                .loadClass(oClass.getName() + "Entity");
                Constructor<?> ctor = entityClass.getConstructor(oClass);
                entity = ctor.newInstance(o);
            } catch (ClassNotFoundException | IllegalAccessException | //
                            InstantiationException | InvocationTargetException | //
                            NoSuchMethodException | SecurityException x) {
                Throwable targetx = x instanceof InvocationTargetException //
                                ? x.getCause() //
                                : x;
                IllegalArgumentException iax = exc(IllegalArgumentException.class,
                                                   "CWWKD1070.record.convert.err",
                                                   loggableAppend(oClass.getName(),
                                                                  " (" + o + ')'),
                                                   method.getName(),
                                                   repositoryInterface.getName(),
                                                   targetx.getMessage());
                throw (IllegalArgumentException) iax.initCause(x);
            }
        if (entity != o &&
            TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "toEntity " + loggable(o),
                     oClass.getName() + " --> " + entity.getClass().getName());
        return entity;
    }

    /**
     * Functional interface that can be supplied to stream.mapToInt.
     *
     * @param o object to convert.
     * @return int value.
     */
    @Trivial
    private final int toInt(Object o) {
        return (Integer) convert(o, int.class, true);
    }

    /**
     * Functional interface that can be supplied to stream.mapToLong.
     *
     * @param o object to convert.
     * @return long value.
     */
    @Trivial
    private final long toLong(Object o) {
        return (Long) convert(o, long.class, true);
    }

    /**
     * Converts a Limit to a PageRequest if possible.
     * Some tests are relying on this. Consider if we should allow this
     * pattern where a Limit can used in place of PageRequest if its
     * starting result is 1.
     *
     * @param limit Limit.
     * @return PageRequest.
     * @throws IllegalArgumentException if the Limit is a range with a
     *                                      starting point above 1.
     */
    private final PageRequest toPageRequest(Limit limit) {
        if (limit.startAt() != 1L)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1041.rtrn.mismatch.pagereq",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericReturnType().getTypeName());

        return PageRequest.ofSize(limit.maxResults());
    }

    /**
     * Converts an update count to the requested return type.
     *
     * @param i          update count value.
     * @param returnType requested return type.
     * @param queryInfo  query information, which must have type DELETE or UPDATE.
     * @return converted value.
     */
    private final Object toReturnValue(int i, Class<?> returnType) {
        Object result;
        if (int.class.equals(returnType) || Integer.class.equals(returnType) ||
            Number.class.equals(returnType))
            result = i;
        else if (long.class.equals(returnType) || Long.class.equals(returnType))
            result = Long.valueOf(i);
        else if (boolean.class.equals(returnType) || Boolean.class.equals(returnType))
            result = i != 0;
        else if (void.class.equals(returnType) || Void.class.equals(returnType))
            result = null;
        else if (CompletableFuture.class.equals(returnType) ||
                 CompletionStage.class.equals(returnType))
            result = CompletableFuture.completedFuture(toReturnValue(i, singleType));
        else
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1007.updel.rtrn.err",
                      method.getGenericReturnType().getTypeName(),
                      method.getName(),
                      repositoryInterface.getName(),
                      type == Type.QM_DELETE ? "Delete" : "Update");
        return result;
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder b = new StringBuilder("QueryInfo@") //
                        .append(Integer.toHexString(hashCode())).append(' ') //
                        .append(method.getGenericReturnType().getTypeName()).append(' ') //
                        .append(method.getName());
        boolean first = true;
        for (Class<?> p : method.getParameterTypes()) {
            b.append(first ? "(" : ", ").append(p.getSimpleName());
            first = false;
        }
        b.append(first ? "() " : ") ");
        if (jpql != null)
            b.append(jpql);
        if (jpqlParamCount > 0)
            b.append(" [").append(jpqlParamCount).append(jpqlParamNames.isEmpty() ? //
                            " positional params]" : //
                            " named params]");
        return b.toString();
    }

    /**
     * Updates entities (or records) in the database.
     * An error is raised if any of the entities (or records) are not found
     * in the database.
     *
     * @param arg the entity or record, or array/Iterable/Stream of entity or record
     * @param em  the entity manager
     * @return count of matching entities, boolean indicator of whether any matched,
     *         or void return type that is required by the Update method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial
    Object update(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().collect(Collectors.toList()) //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "update", loggable(arg));

        int updateCount = 0;
        int numExpected = 0;

        if (arg instanceof Iterable) {
            for (Object e : ((Iterable<?>) arg)) {
                numExpected++;
                updateCount += updateOne(e, em);
            }
        } else if (entityParamType.isArray()) {
            numExpected = Array.getLength(arg);
            for (int i = 0; i < numExpected; i++)
                updateCount += updateOne(Array.get(arg, i), em);
        } else {
            numExpected = 1;
            updateCount = updateOne(arg, em);
        }

        if (numExpected == 0)
            throw exc(IllegalArgumentException.class,
                      "CWWKD1092.lifecycle.arg.empty",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getGenericParameterTypes()[0].getTypeName());

        if (updateCount < numExpected)
            if (numExpected == 1)
                throw exc(OptimisticLockingFailureException.class,
                          "CWWKD1051.single.opt.lock.exc",
                          method.getName(),
                          repositoryInterface.getName(),
                          entityInfo.entityClass.getName(),
                          Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);
            else
                throw exc(OptimisticLockingFailureException.class,
                          "CWWKD1052.multi.opt.lock.exc",
                          method.getName(),
                          repositoryInterface.getName(),
                          numExpected - updateCount,
                          numExpected,
                          entityInfo.entityClass.getName(),
                          Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES);

        Object returnValue = toReturnValue(updateCount, method.getReturnType());

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "update", loggable(returnValue));
        return returnValue;
    }

    /**
     * Updates the entity (or record) from the database if its attributes match
     * the database.
     *
     * @param e  the entity or record.
     * @param em the entity manager.
     * @return the number of entities updated (1 or 0).
     * @throws Exception if an error occurs.
     */
    @Trivial
    private int updateOne(Object e, EntityManager em) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "updateOne", loggable(e));

        Class<?> entityClass = entityInfo.getType();

        if (e == null)
            throw exc(NullPointerException.class,
                      "CWWKD1015.null.entity.param",
                      method.getName(),
                      repositoryInterface.getName());

        if (!entityClass.isInstance(e))
            throw exc(IllegalArgumentException.class,
                      "CWWKD1016.incompat.entity.param",
                      method.getName(),
                      repositoryInterface.getName(),
                      entityClass.getName(),
                      e.getClass().getName());

        String jpql = this.jpql;
        Set<String> attrsToUpdate = entityInfo.attributeNamesForEntityUpdate;

        int versionParamIndex = entityInfo.idClassAttributeAccessors == null //
                        ? (attrsToUpdate.size() + 2) //
                        : (attrsToUpdate.size() +
                           entityInfo.idClassAttributeAccessors.size() + 1);
        Object version = null;
        if (entityInfo.versionAttributeName != null) {
            version = getAttribute(e, entityInfo.versionAttributeName);
            if (version == null)
                jpql = jpql.replace("=?" + versionParamIndex, " IS NULL");
        }

        Object id = null;
        String idAttributeName = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            idAttributeName = entityInfo.attributeNames.get(ID);
            id = getAttribute(e, idAttributeName);
            if (id == null) {
                jpql = jpql.replace("=?" + (versionParamIndex - 1), " IS NULL");
                if (version != null)
                    jpql = jpql.replace("=?" + versionParamIndex, "=?" + (versionParamIndex - 1));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != this.jpql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> update = em.createQuery(jpql, entityInfo.entityClass);

        // parameters for entity attributes to update:
        int p = 1;
        for (String attrName : attrsToUpdate)
            setParameter(p++, update, e, attrName);

        // id and version parameters
        if (entityInfo.idClassAttributeAccessors == null) {
            if (id != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + loggable(id));
                update.setParameter(p++, id);
            }

            if (entityInfo.versionAttributeName != null && version != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "set ?" + p + ' ' + loggable(version));
                update.setParameter(p++, version);
            }
        } else { // has IdClass
            setParametersFromIdClassAndVersion(p, update, e, version);
        }

        int numUpdated = update.executeUpdate();

        if (numUpdated > 1) // ought to be unreachable
            throw new DataException("Found " + numUpdated + " matching entities.");

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "updateOne", numUpdated);
        return numUpdated;
    }

    /**
     * Validate this instance. This is invoked at the end of initialization.
     */
    @Trivial
    private void validate() {
        if (type == null)
            throw excUnsupportedMethod();

        int methodParamCount = method.getParameterCount();
        if (jpql != null &&
            methodParamCount < jpqlParamCount &&
            type != Type.LC_DELETE &&
            type != Type.LC_UPDATE &&
            type != Type.LC_UPDATE_RET_ENTITY)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1021.insufficient.params",
                      method.getName(),
                      repositoryInterface.getName(),
                      methodParamCount,
                      jpqlParamCount,
                      jpql);

        if (jpql != null &&
            jpqlParamCount < methodParamCount &&
            type != Type.FIND &&
            type != Type.FIND_AND_DELETE) {

            if (type == Type.QM_DELETE) {
                Class<?>[] paramTypes = method.getParameterTypes();
                for (int i = jpqlParamCount; i < methodParamCount; i++)
                    if (Util.SORT_PARAM_TYPES.contains(paramTypes[i]) ||
                        Limit.class.equals(paramTypes[i]))
                        throw exc(UnsupportedOperationException.class,
                                  "CWWKD1097.param.incompat",
                                  method.getName(),
                                  repositoryInterface.getName(),
                                  paramTypes[i].getSimpleName());
            }

            throw exc(UnsupportedOperationException.class,
                      "CWWKD1022.too.many.params",
                      method.getName(),
                      repositoryInterface.getName(),
                      jpqlParamCount,
                      methodParamCount,
                      jpql);
        }

        if (type == Type.FIND &&
            CursoredPage.class.equals(multiType)) {

            if (!singleType.equals(entityInfo.getType()))
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1037.cursor.rtrn.mismatch",
                          singleType.getSimpleName(),
                          method.getName(),
                          repositoryInterface.getName(),
                          entityInfo.getType().getName(),
                          method.getGenericReturnType().getTypeName());

            if (sortPositions == NONE_QUERY_LANGUAGE_ONLY && sorts == null)
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1100.cursor.requires.sort",
                          method.getName(),
                          repositoryInterface.getName(),
                          method.getGenericReturnType().getTypeName(),
                          "Order, Sort, Sort[]");
        }
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

            throw exc(UnsupportedOperationException.class,
                      "CWWKD1002.method.annos.err",
                      method.getName(),
                      repositoryInterface.getName(),
                      annoClassNames);
        }

        return ius == 1 //
                        ? (insert != null ? insert : update != null ? update : save) //
                        : iusdce == 1 //
                                        ? (delete != null ? delete : count != null ? count : exists) //
                                        : (q == 1 ? query : f == 1 ? find : null);
    }

    /**
     * Confirm that special parameters are positioned after all other parameters.
     *
     * @throws UnupportedOperationException if a special parameter is ahead of
     *                                          a query parameter.
     */
    @Trivial
    private void validateParameterPositions() {
        DataVersionCompatibility compat = entityInfo.builder.provider.compat;

        Class<?>[] paramTypes = method.getParameterTypes();
        Set<Class<?>> specParamTypes = compat.specialParamTypes();
        int specParamIndex = Integer.MAX_VALUE, otherParamIndex = -1;
        for (int i = 0; i < paramTypes.length; i++)
            if (specParamTypes.contains(paramTypes[i]))
                specParamIndex = i < specParamIndex ? i : specParamIndex;
            else
                otherParamIndex = i;

        if (specParamIndex < otherParamIndex)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1098.spec.param.position.err",
                      method.getName(),
                      repositoryInterface.getName(),
                      paramTypes[specParamIndex].getName(),
                      compat.specialParamsForFind());
    }

    /**
     * Validates that the return type is valid for an exists method.
     */
    @Trivial
    private void validateReturnForExists() {
        if ((!boolean.class.equals(singleType) &&
             !Boolean.class.equals(singleType))
            ||
            (multiType != null &&
             !CompletableFuture.class.equals(multiType) &&
             !CompletionStage.class.equals(multiType)))

            throw exc(UnsupportedOperationException.class,
                      "CWWKD1003.rtrn.err",
                      method.getGenericReturnType().getTypeName(),
                      method.getName(),
                      repositoryInterface.getName(),
                      "exists",
                      "boolean, Boolean");
    }

    /**
     * Validates that ignoreCase is only true if the type of the attribute being
     * sorted on is a String.
     *
     * @param sort the Jakarta Data Sort object being evaluated
     */
    @Trivial
    private void validateSort(Sort<?> sort) {
        String propName = sort.property();
        if (propName.charAt(propName.length() - 1) == ')') {
            // skip for version(o) and id(o), the latter of which which could be a composite value
        } else {
            Class<?> propertyClass = entityInfo.attributeTypes.get(propName);

            if (sort.ignoreCase() //
                && !CharSequence.class.isAssignableFrom(propertyClass)
                && !char.class.equals(propertyClass)
                && !Character.class.equals(propertyClass))
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1026.ignore.case.not.text",
                          propName,
                          entityInfo.getType().getName(),
                          sort,
                          propertyClass.getName(),
                          method.getName(),
                          repositoryInterface.getName());
        }
    }
}

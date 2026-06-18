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

import static io.openliberty.data.internal.AttributeConstraint.IgnoreCase;
import static io.openliberty.data.internal.AttributeConstraint.Not;
import static io.openliberty.data.internal.QueryType.COUNT;
import static io.openliberty.data.internal.QueryType.EXISTS;
import static io.openliberty.data.internal.QueryType.FIND;
import static io.openliberty.data.internal.QueryType.FIND_AND_DELETE;
import static io.openliberty.data.internal.QueryType.LC_DELETE;
import static io.openliberty.data.internal.QueryType.LC_UPDATE;
import static io.openliberty.data.internal.QueryType.LC_UPDATE_MERGE;
import static io.openliberty.data.internal.QueryType.NATIVE;
import static io.openliberty.data.internal.QueryType.QM_DELETE;
import static io.openliberty.data.internal.QueryType.QM_UPDATE;
import static io.openliberty.data.internal.Util.SORT_PARAM_TYPES;
import static io.openliberty.data.internal.cdi.DataExtension.exc;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.cdi.RepositoryProducer;
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
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Update;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

/**
 * Query information.
 */
public abstract class QueryInfo {
    private static final TraceComponent tc = Tr.register(QueryInfo.class);

    /**
     * Placeholder that indicates the entity class needs to be determined
     * based on the content of the Query value.
     */
    public static final Class<?> ENTITY_TBD = Query.class;

    /**
     * Placeholder to indicate there are no repository method parameters
     * for which processing must be deferred until a value is available
     * because they are Constraint-typed.
     */
    private static final Map<Integer, Object> NO_CONSTRAINTS_DEFERRED = //
                    Collections.emptyMap();

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
     * Error condition returned by inspectMethodParam indicating that an annotation
     * of the method parameter conflicts with the constraint type of the method
     * parameter.
     */
    protected static final int PARAM_ANNO_CONFLICTS_WITH_CONSTRAINT = -1;

    /**
     * Error condition returned by inspectMethodParam indicating that two or more
     * annotations on the method parameter conflict with each other.
     */
    protected static final int PARAM_ANNOS_CONFLICT = -2;

    /**
     * The implicit entity identifier variable defined by Jakarta Persistence.
     */
    private static final String THIS = "this";

    /**
     * Information about the type of entity to which the query pertains.
     */
    protected EntityInfo entityInfo;

    /**
     * Type of the first parameter if a life cycle method, otherwise null.
     */
    protected final Class<?> entityParamType;

    /**
     * Entity identifier variable name if an identifier variable is used.
     * Otherwise "this". "o" is used as the default in generated queries.
     */
    private String entityVar = THIS;

    /**
     * Entity identifier variable name and . character if an identifier variable is used.
     * Otherwise the empty string. "o." is used as the default in generated queries.
     */
    protected String entityVar_ = THIS + '.';

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
     * JPQL for a find query after a cursor. Otherwise null.
     */
    String jpqlAfterCursor;

    /**
     * JPQL for a find query before a cursor. Otherwise null.
     */
    String jpqlBeforeCursor;

    /**
     * For counting the total number of results across all pages.
     * If less than Util.MIN_COUNT_QUERY_LENGTH characters long, indicates a
     * query keyword that prevents computation of a count.
     * Null if pagination is not used or if pagination without totals is used.
     */
    String jpqlCount;

    /**
     * For deleting an entry when using the find-and-delete pattern
     * where a delete query returns the deleted entity.
     */
    String jpqlDelete;

    /**
     * Names of named parameters in query language, ordered according to the
     * position in which each appears as a repository method parameter.
     * Repository method parameters identify the name with the
     * <code>Param</code> annotation if present, or otherwise by the
     * name of the parameter (if the -parameters compiler option is enabled).
     * This set also includes names of named parameters that are used in
     * generated restrictions, such as those added for cursor pagination.
     * The empty set value is used when the field has not been initialized yet
     * or the query has no parameters or has positional parameters (?1, ?2, ...)
     * rather than named parameters.
     */
    Set<String> jpqlParamNames = Collections.emptySet();

    /**
     * Value from the First annotation, or findFirst#By, or 1 for findFirstBy,
     * otherwise 0.
     */
    int maxResults;

    /**
     * Repository method to which this query information pertains.
     */
    public final Method method;

    /**
     * Repository method annotation indicating the type of method.
     * Null if the repository method is not annotated Delete, Find, Insert, Query, ...
     */
    protected Annotation methodTypeAnno;

    /**
     * The type of data structure that returns multiple results for this query.
     * Null if the query return type limits to single results.
     */
    final Class<?> multiType;

    /**
     * Producer for the repository bean.
     */
    final RepositoryProducer<?> producer;

    /**
     * The query, typically in the JPQL query language.
     * SQL if the repository method annotated NativeQuery.
     * Null if a save operation.
     */
    protected String ql;

    /**
     * Number of parameters to the JPQL or SQL query. This count does not
     * include parameters that are generated for cursor pagination because
     * a repository method that supports cursor pagination can run with
     * or without a cursor. Other generated parameters are included.
     * Be careful when using this count. It changes as parameters are
     * found and/or generated.
     */
    int qlParamCount;

    /**
     * The interface that is annotated with @Repository.
     */
    final Class<?> repositoryInterface;

    /**
     * Starting position in the JPQL for added restrictions.
     * -1 indicates to use the end of the JPQL query.
     */
    int restrictAt = -1;

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
    protected final Class<?> singleType;

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
    int[] sortPositions = NONE;

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
     * Index of the first repository method argument that is a special parameter.
     * If there are no special parameters, then the value is set to the total
     * number of method arguments.
     */
    protected int specialParamsStartAt;

    /**
     * Categorization of query type.
     */
    protected QueryType type;

    /**
     * Indicates whether or not to validate method parameters, if Jakarta Validation is available.
     */
    boolean validateParams;

    /**
     * Indicates whether or not to validate method results, if Jakarta Validation is available.
     */
    boolean validateResult;

    /**
     * Used by the version-specific query info classes to construct partially
     * complete query information.
     *
     * @param repositoryProducer    producer of the repository bean instance.
     * @param repositoryInterface   interface annotated with @Repository.
     * @param method                repository method.
     * @param methodType            type of repository method, if known in advance.
     * @param methodTypeAnno        mutually exclusive repository method annotation
     *                                  (Find/Delete/...) if known in advance.
     * @param entityParamType       type of the first parameter if a life cycle method,
     *                                  otherwise null.
     * @param isOptional            indicates if the return type is an Optional.
     * @param returnArrayType       array element type if the repository method returns
     *                                  an array, otherwise null.
     * @param multiType             Data structure type that allows multiple
     *                                  results for this query. Null if the query
     *                                  return type limits to single results.
     * @param singleType            Type of a single result obtained by the query.
     * @param singleTypeElementType Element type of singleType when singleType is an
     *                                  array or collection. Otherwise null.
     */
    @Trivial
    protected QueryInfo(RepositoryProducer<?> repositoryProducer,
                        Class<?> repositoryInterface,
                        Method method,
                        QueryType methodType,
                        Annotation methodTypeAnno,
                        Class<?> entityParamType,
                        boolean isOptional,
                        Class<?> returnArrayType,
                        Class<?> multiType,
                        Class<?> singleType,
                        Class<?> singleTypeElementType) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled()) {
            StringBuilder b = new StringBuilder(200) //
                            .append(method.getGenericReturnType().getTypeName()) //
                            .append(' ').append(repositoryInterface.getName()) //
                            .append('.').append(method.getName());
            boolean first = true;
            for (java.lang.reflect.Type p : method.getGenericParameterTypes()) {
                b.append(first ? "(" : ", ").append(p.getTypeName());
                first = false;
            }
            b.append(first ? "()" : ")");
            Tr.entry(this, tc, "<init>",
                     b.toString(),
                     "life cycle entity: " + entityParamType,
                     "result isOptional? " + isOptional,
                     "result multiType:  " + multiType,
                     "result singleType: " + singleType,
                     "          element: " + singleTypeElementType,
                     "return array type: " + returnArrayType,
                     "type if known:     " + methodType,
                     "anno if known:     " + methodTypeAnno);
        }

        this.producer = repositoryProducer;
        this.repositoryInterface = repositoryInterface;
        this.method = method;
        this.type = methodType;
        this.methodTypeAnno = methodTypeAnno;
        this.entityParamType = entityParamType;
        this.isOptional = isOptional;
        this.returnArrayType = returnArrayType;
        this.multiType = multiType;
        this.singleType = singleType;
        this.singleTypeElementType = singleTypeElementType;
        this.specialParamsStartAt = method.getParameterCount(); // assume none unless found

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "<init>", this);
    }

    /**
     * Adds Sort criteria to the end of the tracked list of sort criteria.
     *
     * @param orderBy OrderBy annotation from a repository method.
     */
    @Trivial
    private void addSort(OrderBy orderBy) {
        if (entityInfo.idClassAttributeAccessors == null ||
            !ID.equalsIgnoreCase(orderBy.value())) {
            String expression = getAttributeName(orderBy.value(), true);
            sorts.add(createSort(expression, orderBy));
        } else {
            // Expand ID(THIS) for composite IdClass into separate attributes
            for (String name : entityInfo.idClassAttributeAccessors.keySet()) {
                name = getAttributeName(name, true);
                sorts.add(createSort(name, orderBy));
            }
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
     * Append a constraint such as o.myAttribute < ?1 to the JPQL query.
     *
     * @param q                 JPQL query to which to append.
     * @param o_                entity identifier variable.
     * @param attrName          entity attribute name.
     * @param constraint        type of constraint to apply to the entity attribute.
     * @param prevNumJPQLParams count of JQPL query parameters required for repository
     *                              method parameters up to, but not including, the
     *                              repository method parameter for the constraint
     *                              being appended.
     * @param isCollection      whether the entity attribute is a collection.
     * @param annos             method parameter annotations.
     * @return the updated JPQL query.
     */
    protected abstract StringBuilder appendConstraint(StringBuilder q,
                                                      String o_,
                                                      String attrName,
                                                      AttributeConstraint constraint,
                                                      int prevNumJPQLParams,
                                                      boolean isCollection,
                                                      Annotation[] annos);

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
            throw Fail.pageModeIncompatible(this, pagination);

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
                throw Fail.resultConversion(this, null, null);
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
                            throw Fail.outOfRange(this, n, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    } else if (short.class.equals(toType) ||
                               Short.class.equals(toType)) {
                        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE)
                            return n.shortValue();
                        else
                            throw Fail.outOfRange(this, n, Short.MIN_VALUE, Short.MAX_VALUE);
                    } else if (byte.class.equals(toType) ||
                               Byte.class.equals(toType)) {
                        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE)
                            return n.byteValue();
                        else
                            throw Fail.outOfRange(this, n, Byte.MIN_VALUE, Byte.MAX_VALUE);
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
            String resultInfo = loggableAppend(fromType.getName(), " (", value, ")");
            throw Fail.resultConversion(this, resultInfo, cause);
        } else {
            return value;
        }
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
                throw Fail.resultConversion(this, List.class.getName(), null);
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
                        throw Fail.resultIncompatible(this, results, query);
                }
                list.add(element);
            }
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "convertToIterable", loggable(list));
        return list;
    }

    /**
     * Construct a copy of this QueryInfo, but with different JPQL and
     * possibly different sorts.
     *
     * @param source        QueryInfo from which to copy.
     * @param constraints   map of method parameter index (0-based) to deferred
     *                          Constraint at the position. Empty if none.
     * @param restriction   Restriction value that was supplied to the repository method.
     *                          Otherwise null.
     * @param jpqlParams    Map to be populated with JPQL parameter names and values
     *                          for Constraints and Restrictions. Map keys are the
     *                          named parameter name or positional parameter index.
     *                          Map values are obtained from the Constraints or
     *                          Restrictions. The first positional parameter index
     *                          starts at qlParamCount, which is updated by this
     *                          method when JPQL parameters for repository method
     *                          special parameters are added.
     * @param pageReq       PageRequest, if supplied to the repository method.
     * @param sortsOverride If present, sorts to use instead of the sorts from source.
     *                          A value is supplied when the repostiory method has
     *                          Order or Sort parameters. Otherwise null.
     * @return the new query information.
     */
    private QueryInfo copy(Map<Integer, Object> constraints,
                           Object restriction,
                           Map<Object, Object> jpqlParams,
                           PageRequest pageReq,
                           List<Sort<Object>> sortsOverride) {
        DataVersionCompatibility compat = entityInfo.factory.provider.compat;

        QueryInfo info = compat.createQueryInfo(producer, //
                                                repositoryInterface, //
                                                method, //
                                                type, //
                                                methodTypeAnno, //
                                                entityParamType, //
                                                isOptional, //
                                                multiType, //
                                                returnArrayType, //
                                                singleType, //
                                                singleTypeElementType);
        info.entityInfo = entityInfo;
        info.entityVar = entityVar;
        info.entityVar_ = entityVar_;
        info.maxResults = maxResults;
        info.sorts = sortsOverride == null ? sorts : sortsOverride;
        info.validateParams = validateParams;

        StringBuilder q;

        if (constraints.isEmpty()) {
            info.hasWhere = hasWhere;
            info.jpqlAfterCursor = jpqlAfterCursor;
            info.jpqlBeforeCursor = jpqlBeforeCursor;
            info.jpqlCount = jpqlCount;
            info.jpqlDelete = jpqlDelete;
            info.qlParamCount = qlParamCount;
            info.jpqlParamNames = jpqlParamNames.isEmpty() //
                            ? jpqlParamNames //
                            : new LinkedHashSet<>(jpqlParamNames);
            info.restrictAt = restrictAt;
            info.specialParamsStartAt = specialParamsStartAt;

            if (restriction == null) {
                // no Constraints deferred or Restriction
                q = new StringBuilder(ql);
            } else {
                // has Restriction, but no Constraints deferred
                int len = ql.length();
                q = new StringBuilder(len + 200);
                if (info.restrictAt >= 0 && info.restrictAt < len)
                    q.append(ql.substring(0, info.restrictAt));
                else
                    q.append(ql).append(' ');

                q.append(info.hasWhere ? "AND " : "WHERE ");
                info.hasWhere = true;

                info.qlParamCount = generateRestrictions(q,
                                                         restriction,
                                                         info.qlParamCount,
                                                         info.jpqlParamNames,
                                                         jpqlParams);

                if (info.restrictAt >= 0 && info.restrictAt < len) {
                    int newPosition = q.length();
                    q.append(' ').append(ql.substring(info.restrictAt));
                    info.restrictAt = newPosition;
                }
            }
        } else {
            // Constraints were deferred until execution
            // Generate new JPQL for Query by Parameters

            boolean countPages = Page.class.equals(multiType) ||
                                 CursoredPage.class.equals(multiType);

            q = info.initQueryByParameters(countPages, constraints, jpqlParams);

            if (restriction != null) {
                q.append(info.hasWhere ? " AND " : " WHERE ");
                info.hasWhere = true;
                info.qlParamCount = generateRestrictions(q,
                                                         restriction,
                                                         info.qlParamCount,
                                                         info.jpqlParamNames,
                                                         jpqlParams);
            }

            // If there are no overrides from Order/Sort parameters, keep the
            // static Sorts from the source QueryInfo.
            if (sortsOverride == null)
                sortsOverride = sorts;
        }

        boolean forward = pageReq == null ||
                          pageReq.mode() != PageRequest.Mode.CURSOR_PREVIOUS;
        StringBuilder order = null; // ORDER BY clause based on Sorts
        if (sortsOverride != null)
            for (Sort<?> sort : sortsOverride) {
                info.validateSort(sort);
                order = order == null //
                                ? new StringBuilder(100).append(" ORDER BY ") //
                                : order.append(", ");
                info.generateSort(order, sort, forward);
            }

        if (pageReq == null ||
            pageReq.mode() == PageRequest.Mode.OFFSET) {
            // offset pagination can be a starting point for cursor pagination
            if (order != null) {
                info.restrictAt = q.length() + 1;
                q.append(order);
            }
            info.ql = q.toString();
        } else { // CURSOR_NEXT or CURSOR_PREVIOUS
            info.ql = null;
            info.generateCursorQueries(q,
                                       forward ? order : null,
                                       forward ? null : order);
        }

        return info;
    }

    /**
     * Execute a repository count query.
     *
     * @param entityHandler the EntityAgent or EntityManager
     * @param args          method parameters
     * @return the count, converted to a type that is compatible with the
     *         method signature.
     * @throws Exception        if an error occurs.
     * @throws MappingException if the count cannot be converted to the
     *                              requested type.
     */
    @Trivial
    Object count(AutoCloseable entityHandler, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "count",
                     // em and method args have already been logged if loggable
                     "to be returned as " + singleType.getName());

        @SuppressWarnings("unchecked")
        TypedQuery<Long> query = (TypedQuery<Long>) createQuery(entityHandler,
                                                                Long.class,
                                                                args);

        Long count = query.getSingleResult();

        Object returnValue;
        if (long.class.equals(singleType) ||
            Long.class.equals(singleType) ||
            singleType.isAssignableFrom(Long.class)) {
            returnValue = count;
        } else {
            if (int.class.equals(singleType) || Integer.class.equals(singleType))
                if (count > Integer.MAX_VALUE)
                    throw Fail.countExceedsMax(this, count, Integer.class);
                else
                    returnValue = count.intValue();
            else if (short.class.equals(singleType) || Short.class.equals(singleType))
                if (count > Short.MAX_VALUE)
                    throw Fail.countExceedsMax(this, count, Short.class);
                else
                    returnValue = count.shortValue();
            else if (byte.class.equals(singleType) || Byte.class.equals(singleType))
                if (count > Byte.MAX_VALUE)
                    throw Fail.countExceedsMax(this, count, Byte.class);
                else
                    returnValue = count.byteValue();
            else if (BigInteger.class.equals(singleType))
                returnValue = BigInteger.valueOf(count);
            else if (BigDecimal.class.equals(singleType))
                returnValue = BigDecimal.valueOf(count);
            else
                throw Fail.countConversion(this, count);
        }

        Class<?> returnType = method.getReturnType();
        if (isOptional) {
            returnValue = Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) ||
                   CompletionStage.class.equals(returnType)) {
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (multiType != null) {
            throw Fail.countConversion(this, count);
        }

        if (trace && tc.isEntryEnabled())
            if (count == returnValue)
                Tr.exit(this, tc, "count", returnValue);
            else
                Tr.exit(this, tc, "count", count + " converted to " + returnValue);
        return returnValue;
    }

    /**
     * Creates and prepares a query, including setting parameters based on the
     * repository method arguments and any Constraints and Restrictions.
     *
     * @param entityHandler    the EntityAgent or EntityManager
     * @param typeOfTypedQuery TypedQuery type. Null to avoid using TypedQuery
     *                             (for UPDATE or DELETE).
     * @param args             method parameters.
     * @return the query, ready to execute.
     */
    @Trivial // avoid tracing repository method args
    private jakarta.persistence.Query createQuery(AutoCloseable entityHandler,
                                                  Class<?> typeOfTypedQuery,
                                                  Object[] args) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        DataVersionCompatibility compat = producer.compat();
        Object restriction = null;

        for (int i = specialParamsStartAt; i < (args == null ? 0 : args.length); i++) {
            Object param = args[i];
            if (compat.isRestriction(param)) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             "found restriction " + param.getClass().getName(),
                             loggable(param));
                if (restriction == null)
                    restriction = param;
                else
                    throw Fail.duplicateSpecialParam(this, "Restriction");
            } else if (param == null) {
                throw Fail.nullMethodParameter(this, i);
            } else {
                throw Fail.extraMethodParam(this, i);
            }
        }

        Map<Integer, Object> deferredConstraints;
        if (args == null || args.length == 0 ||
            methodTypeAnno == null || methodTypeAnno instanceof Query)
            deferredConstraints = NO_CONSTRAINTS_DEFERRED;
        else
            deferredConstraints = getDeferredConstraints(restriction != null, args);
        boolean requiresNewQuery = restriction != null ||
                                   !deferredConstraints.isEmpty();

        // Map of named parameter name or positional parameter index to value
        // for values corresponding to repository method special parameters.
        // The first positional parameter index to add starts at qlParamCount,
        // which is updated as entries for additional JPQL parameters are added.
        Map<Object, Object> addedJPQLParams = null;

        QueryInfo queryInfo = requiresNewQuery //
                        ? copy(deferredConstraints, //
                               restriction, //
                               addedJPQLParams = new LinkedHashMap<>(), //
                               null, //
                               null) //
                        : this;

        jakarta.persistence.Query query = typeOfTypedQuery == null //
                        ? ehCreateStatement(entityHandler, queryInfo.ql) //
                        : ehCreateTypedQuery(entityHandler,
                                             queryInfo.ql,
                                             typeOfTypedQuery);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "created query " + query);

        setParameters(query, args, deferredConstraints, addedJPQLParams);
        return query;
    }

    /**
     * Creates a new Sort instance equivalent to the behavior of the given
     * OrderBy annotation.
     *
     * @param expression usually an entity attribute name but might be
     *                       another type of expression instead.
     * @param orderBy    annotation found on a repository method.
     * @return the new Sort instance.
     */
    protected abstract <T> Sort<T> createSort(String expression, OrderBy orderBy);

    /**
     * Creates a new Sort instance with the corresponding entity attribute name
     * or expression, with all other fields copied from the given Sort instance.
     *
     * @param expression usually an entity attribute name but might be
     *                       another type of expression instead.
     * @param sort       the Sort from which to copy.
     * @return an otherwise identical Sort instance, but with the corresponding
     *         entity attribute name or expression.
     */
    protected abstract <T> Sort<T> createSort(String expression, Sort<T> sort);

    /**
     * Deletes entities that were found by a find-and-delete operation.
     *
     * @param results       entities or record entities to delete from the database
     * @param entityHandler EntityAgent or EntityManager
     * @throws Exception if an error occurs.
     */
    @Trivial
    private void delete(List<?> results, AutoCloseable entityHandler) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "delete", loggable(results));

        for (Object result : results)
            if (result == null) {
                throw Fail.resultConversion(this, null, null);
            } else if (entityInfo.entityClass.isInstance(result)) {
                ehDelete(entityHandler, result);
            } else if (entityInfo.idClassAttributeAccessors != null) {
                jakarta.persistence.Query delete = ehCreateStatement(entityHandler,
                                                                     jpqlDelete);
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
                        throw Fail.returnTypeInvalidForDelete(this);
                }

                jakarta.persistence.Query delete = ehCreateStatement(entityHandler,
                                                                     jpqlDelete);
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
     * @param arg           the entity or record, or array/Iterable/Stream of entity or record
     * @param entityHandler EntityAgent or EntityManager
     * @return the deleted entity count, boolean indicator of any deletion, or void
     *         return type that is required by the Delete method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial
    Object delete(Object arg, AutoCloseable entityHandler) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "delete", loggable(arg));

        int updateCount = 0;
        int numExpected = 0;

        if (arg instanceof Iterable) {
            for (Object e : ((Iterable<?>) arg)) {
                numExpected++;
                updateCount += deleteOne(e, entityHandler);
            }
        } else if (entityParamType.isArray()) {
            numExpected = Array.getLength(arg);
            for (int i = 0; i < numExpected; i++)
                updateCount += deleteOne(Array.get(arg, i), entityHandler);
        } else {
            numExpected = 1;
            updateCount = deleteOne(arg, entityHandler);
        }

        if (numExpected == 0)
            throw Fail.emptyLifeCycleParam(this);

        if (updateCount < numExpected)
            throw Fail.optimisticLockConflict(this, updateCount, numExpected);

        Object returnValue = toReturnValue(updateCount, method.getReturnType());

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "delete", loggable(returnValue));
        return returnValue;
    }

    /**
     * Removes the entity (or record) from the database if its attributes
     * match the database.
     *
     * @param e             the entity or record.
     * @param entityHandler the EntityAgent or EntityManager
     * @return the number of entities deleted (1 or 0).
     * @throws Exception if an error occurs or the repository method return type is
     *                       void and the entity (or correct version of the entity)
     *                       was not found.
     */
    private int deleteOne(Object e, AutoCloseable entityHandler) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "deleteOne", loggable(e));

        if (!entityInfo.getType().isInstance(e))
            throw Fail.entityMismatch(this, e);

        String jpql = ql;

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
            && jpql != this.ql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        jakarta.persistence.Query delete = ehCreateStatement(entityHandler, jpql);

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
                throw Fail.entityNotFound(this, e, idAttributeName, id, version);
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
     * Detaches entities from the persistence context.
     *
     * @param arg the entity or array/Iterable/Stream of entity
     * @param em  the entity manager
     * @throws Exception if an error occurs.
     */
    @Trivial
    Void detach(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "detach", loggable(arg));

        int count = 0;
        if (arg instanceof Iterable) {
            for (Object entity : ((Iterable<?>) arg)) {
                em.detach(entityNotNull(entity));
                count++;
            }
        } else if (entityParamType.isArray()) {
            int length = Array.getLength(arg);
            for (; count < length; count++)
                em.detach(entityNotNull(Array.get(arg, count)));
        } else {
            em.detach(entityNotNull(arg));
            count++;
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "detach");
        return null;
    }

    /**
     * Delegates to the EntityAgent or EntityManager to create a Jakarta
     * Persistence Native Query that peforms a SQL SELECT operation or other
     * native SQL query that returns query results.
     *
     * @param entityHandler EntityAgent or EntityManager
     * @return the query
     */
    protected abstract jakarta.persistence.Query //
                    ehCreateNativeQuery(AutoCloseable entityHandler);

    /**
     * Delegates to the EntityAgent or EntityManager to create a Jakarta
     * Persistence Native Query that peforms a SQL DELETE, INSERT, UPDATE,
     * or other native SQL statement that is not return query results.
     *
     * @param entityHandler EntityAgent or EntityManager
     * @return the statement/query
     */
    protected abstract jakarta.persistence.Query //
                    ehCreateNativeStatement(AutoCloseable entityHandler);

    /**
     * Delegates to the EntityAgent or EntityManager to create a
     * Jakarta Persistence Query that peforms a DELETE or UPDATE.
     *
     * @param entityHandler EntityAgent or EntityManager
     * @param jpql          a JPQL DELETE or UPDATE statement
     * @return the query, ready to execute
     */
    protected abstract jakarta.persistence.Query //
                    ehCreateStatement(AutoCloseable entityHandler,
                                      String jpql);

    /**
     * Delegates to the EntityAgent or EntityManager to create a TypedQuery.
     *
     * @param <T>           result type of the query
     * @param entityHandler EntityAgent or EntityManager
     * @param jpql          the query represented as JPQL
     * @param resultType    the result type of the query
     * @return the query, ready to execute
     */
    protected abstract <T> TypedQuery<T> //
                    ehCreateTypedQuery(AutoCloseable entityHandler,
                                       String jpql,
                                       Class<?> resultType);

    /**
     * Delegates to the EntityAgent or EntityManager to delete or remove
     * an entity.
     *
     * @param entityHandler EntityAgent or EntityManager
     * @param entity        the entity to remove
     */
    protected abstract void ehDelete(AutoCloseable entityHandler, Object entity);

    /**
     * Delegates to the EntityAgent or EntityManager to insert or persist
     * an entity.
     *
     * @param entityHandler EntityAgent or EntityManager
     * @param entity        the entity to insert
     */
    protected abstract void ehInsert(AutoCloseable entityHandler, Object entity);

    /**
     * Delegates to the EntityAgent or EntityManager to update or merge
     * an entity.
     *
     * @param entityHandler EntityAgent or EntityManager
     * @param entity        the entity to update
     */
    protected abstract Object ehUpdate(AutoCloseable entityHandler, Object entity);

    /**
     * Delegates to the EntityAgent or EntityManager to upsert or merge
     * an entity.
     *
     * @param entityHandler EntityAgent or EntityManager
     * @param entity        the entity to update or insert
     */
    @Trivial
    protected abstract Object ehUpsert(AutoCloseable entityHandler, Object entity);

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
     * Convenience method that raises an error if the given entity is null
     * and otherwise returns the entity.
     *
     * @param entity entity instance that might be null.
     * @return the non-null entity.
     * @throws NullPointerException if the given entity is null.
     */
    @Trivial
    private Object entityNotNull(Object entity) {
        if (entity == null)
            throw Fail.entityNull(this);
        return entity;
    }

    /**
     * Execute JPQL for a repository delete or update query.
     *
     * @param entityHandler the EntityManager or EntityAgent
     * @param args          method parameters
     * @return void, boolean, int, or long value representing a count of
     *         matching entities, or a CompletableFuture for the value,
     *         whichever is compatible with the method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial // entityHandler and method args have already been logged if loggable
    Object execute(AutoCloseable entityHandler, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "execute", type); // DELETE or UPDATE

        jakarta.persistence.Query update = createQuery(entityHandler, null, args);

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
     * @param entityHandler the EntityAgent or EntityManager
     * @param args          method parameters
     * @return boolean value or CompletableFuture for a boolean value,
     *         whichever is compatible with the method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial // entityHandler and method args have already been logged if loggable
    Object exists(AutoCloseable entityHandler, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "exists");

        jakarta.persistence.Query query = createQuery(entityHandler,
                                                      Object.class,
                                                      args);
        query.setMaxResults(1);

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
     * @param eh       EntityAgent or EntityManager, both of which are EntityHandler
     * @param txStatus transaction status.
     * @param args     method parameters.
     * @return results, after wrapping in an Optional or CompletionStage if required
     *         by the repository method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial // eh, txStatus, and method args have already been logged if loggable
    Object find(AutoCloseable eh, int txStatus, Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "find", type);

        QueryCustomization qc = QueryCustomization.from(this, args);

        Map<Integer, Object> deferredConstraints;
        if (args != null && args.length > 0 &&
            (methodTypeAnno instanceof Find ||
             methodTypeAnno instanceof Delete))
            deferredConstraints = getDeferredConstraints(qc.restriction() != null,
                                                         args);
        else // Query, JakartaQuery, NativeQuery, or Query-by-Method-Name
            deferredConstraints = NO_CONSTRAINTS_DEFERRED;

        boolean requiresNewQuery = qc.restriction() != null ||
                                   !deferredConstraints.isEmpty();

        if (qc.sorts() == null || qc.sorts().isEmpty()) {
            if (qc.pageRequest() != null)
                requireOrderedPagination(args);
        } else {
            requiresNewQuery = true;
        }

        // Map of named parameter name or positional parameter index to value
        // for values corresponding to repository method special parameters.
        // The first positional parameter index to add starts at qlParamCount,
        // which is updated as entries for additional JPQL parameters are added.
        Map<Object, Object> addedJPQLParams = null;

        QueryInfo queryInfo = requiresNewQuery //
                        ? copy(deferredConstraints, //
                               qc.restriction(), //
                               addedJPQLParams = new LinkedHashMap<>(), //
                               qc.pageRequest(), //
                               qc.sorts()) //
                        : this;

        Object returnValue = queryInfo.find(eh,
                                            qc,
                                            txStatus,
                                            args,
                                            deferredConstraints,
                                            addedJPQLParams);

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
     * @param entityHandler       EntityAgent or EntityManager
     * @param qc                  Query customization from special parameters and
     *                                First and OrderBy annotations/keywords
     * @param txStatus            transaction status.
     * @param args                method parameters.
     * @param deferredConstraints map of method parameter index to non-Literal
     *                                Constraints that are supplied at execution time.
     * @param addedJPQLParams     map of JPQL parameter names/indices and values that
     *                                are added due to repository special parameters.
     *                                Null indicates none are added.
     * @return results, before wrapping in an Optional or CompletionStage.
     * @throws Exception if an error occurs.
     */
    @Trivial // em, txStatus, and method args have already been logged if loggable
    private Object find(AutoCloseable entityHandler,
                        QueryCustomization qc,
                        int txStatus,
                        Object[] args,
                        Map<Integer, Object> deferredConstraints,
                        Map<Object, Object> addedJPQLParams) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "find",
                     "non-literal Constraints at: " + deferredConstraints.keySet(),
                     "added JPQL params: " + (addedJPQLParams == null //
                                     ? null //
                                     : addedJPQLParams.keySet()));

        Limit limit = qc.limit();
        int max = qc.maxResults();
        PageRequest pageReq = qc.pageRequest();
        Object returnValue;

        if (CursoredPage.class.equals(multiType)) {
            returnValue = new CursoredPageImpl<>(//
                            this, //
                            entityHandler, //
                            pageReq, //
                            args, //
                            deferredConstraints, //
                            addedJPQLParams);
        } else if (Page.class.equals(multiType)) {
            PageRequest req = limit == null ? pageReq : toPageRequest(limit);
            returnValue = new PageImpl<>(//
                            this, //
                            entityHandler, //
                            req, //
                            args, //
                            deferredConstraints, //
                            addedJPQLParams);
        } else if (pageReq != null &&
                   !PageRequest.Mode.OFFSET.equals(pageReq.mode())) {
            throw Fail.pageModeIncompatible(this, pageReq);
        } else {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "createQuery",
                         ql,
                         entityInfo.entityClass.getName());

            TypedQuery<?> query = ehCreateTypedQuery(entityHandler,
                                                     ql,
                                                     Object.class);
            setParameters(query, args, deferredConstraints, addedJPQLParams);

            if (type == FIND_AND_DELETE)
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

            returnValue = getQueryResults(entityHandler, qc, query, txStatus);
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "find", loggable(returnValue));
        return returnValue;
    }

    /**
     * Finds and updates entities (or records) in the database.
     *
     * @param arg           the entity or record, or array/Iterable/Stream
     *                          of entity or record
     * @param entityHandler the EntityAgent or EntityManager
     * @return the updated entities, using the return type that is required by the
     *         repository Update method signature.
     * @throws OptimisticLockingFailureException if an entity is not found in the database.
     * @throws Exception                         if an error occurs.
     */
    Object findAndUpdate(Object arg, AutoCloseable entityHandler) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findAndUpdate", loggable(arg));

        List<Object> results;

        boolean hasSingularEntityParam = false;
        if (entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = new ArrayList<>(length);
            for (int i = 0; i < length; i++)
                results.add(findAndUpdateOne(Array.get(arg, i), entityHandler));
        } else {
            arg = arg instanceof Stream //
                            ? ((Stream<?>) arg).sequential().toList() //
                            : arg;

            results = new ArrayList<>();
            if (arg instanceof Iterable) {
                for (Object e : ((Iterable<?>) arg))
                    results.add(findAndUpdateOne(e, entityHandler));
            } else {
                hasSingularEntityParam = true;
                results = new ArrayList<>(1);
                results.add(findAndUpdateOne(arg, entityHandler));
            }
        }

        if (!results.isEmpty() && entityHandler instanceof EntityManager em) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "flush");
            em.flush();
        }

        Object returnValue;
        Class<?> returnType = method.getReturnType();
        if (boolean.class.equals(singleType) || Boolean.class.equals(singleType)) {
            returnValue = !results.isEmpty();
        } else if (Util.PRIMITIVE_NUMERIC_TYPES.contains(singleType) ||
                   Number.class.isAssignableFrom(singleType)) {
            returnValue = convert(results.size(), singleType, true);
        } else if (results.isEmpty()) {
            throw Fail.emptyLifeCycleParam(this);
        } else if (void.class.equals(returnType) || Void.class.equals(returnType)) {
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
                        throw Fail.nonUniqueResult(this, results.size());
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = convertToIterable(results, multiType, null, null);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw Fail.returnTypeInvalid(this, "Update", hasSingularEntityParam,
                                                 null, results.get(0).getClass());
            }
        }

        if (Optional.class.equals(returnType)) {
            returnValue = returnValue == null //
                            ? Optional.empty() //
                            : Optional.of(returnValue);
        } else if (CompletableFuture.class.equals(returnType) ||
                   CompletionStage.class.equals(returnType)) {
            // useful for @Asynchronous
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (returnValue != null &&
                   !Util.wrapperClassIfPrimitive(returnType) //
                                   .isAssignableFrom(returnValue.getClass())) {
            throw Fail.returnTypeInvalid(this, "Update", hasSingularEntityParam,
                                         null, results.get(0).getClass());
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
     * @param e             the entity or record
     * @param entityHandler the EntityAgent or Entitymanager
     * @return the entity that is written to the database. Never null.
     * @throws OptimisticLockingException if the entity is not found.
     * @throws Exception                  if an error occurs.
     */
    private Object findAndUpdateOne(Object e, AutoCloseable entityHandler) //
                    throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "findAndUpdateOne", loggable(e));

        String jpql = ql;

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

        if (TraceComponent.isAnyTracingEnabled() && jpql != this.ql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id or version", jpql);

        TypedQuery<?> query = ehCreateTypedQuery(entityHandler, jpql, Object.class);
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

        if (results.isEmpty())
            throw Fail.entityNotFound(this, e, idAttributeName, id, version);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "found", loggable(results.get(0)));

        e = toEntity(e);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "merge", loggable(e));

        Object returnValue = ehUpdate(entityHandler, e);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "findAndUpdateOne", loggable(returnValue));
        return returnValue;
    }

    /**
     * Generates JPQL for a *By constraint such as MyColumn[IgnoreCase][Not]Like
     *
     * @param methodName name of the repository method.
     * @param start      position (0-based) within the method name to start at.
     * @param endBefore  position (0-based) before which to end because it no longer
     *                       corresponds to the entity attribute constraint.
     * @param q          partial completed JPQL query to which to append.
     */
    private void generateConstraint(String methodName,
                                    int start,
                                    int endBefore,
                                    StringBuilder q) {
        int length = endBefore - start;

        AttributeConstraint constraint = AttributeConstraint.Equal;
        switch (methodName.charAt(endBefore - 1)) {
            case 'n': // GreaterThan | LessThan | In | Between
                if (length > 2) {
                    char ch = methodName.charAt(endBefore - 2);
                    if (ch == 'a') { // GreaterThan | LessThan
                        if (endsWith("GreaterTh", methodName, start, endBefore - 2))
                            constraint = AttributeConstraint.GreaterThan;
                        else if (endsWith("LessTh", methodName, start, endBefore - 2))
                            constraint = AttributeConstraint.LessThan;
                    } else if (ch == 'I') { // In
                        constraint = AttributeConstraint.In;
                    } else if (ch == 'e' &&
                               endsWith("Betwe", methodName, start, endBefore - 2)) {
                        constraint = AttributeConstraint.Between;
                    }
                }
                break;
            case 'l': // GreaterThanEqual | LessThanEqual | Null
                if (length > 4) {
                    char ch = methodName.charAt(endBefore - 2);
                    if (ch == 'a') { // GreaterThanEqual | LessThanEqual
                        if (endsWith("GreaterThanEqu", methodName, start, endBefore - 2))
                            constraint = AttributeConstraint.GreaterThanEqual;
                        else if (endsWith("LessThanEqu", methodName, start, endBefore - 2))
                            constraint = AttributeConstraint.LessThanEqual;
                    } else if (ch == 'l' &&
                               methodName.charAt(endBefore - 3) == 'u' &&
                               methodName.charAt(endBefore - 4) == 'N') {
                        constraint = AttributeConstraint.Null;
                    }
                }
                break;
            case 'e': // Like, True, False
                if (length > 4) {
                    char ch = methodName.charAt(endBefore - 4);
                    if (ch == 'L') {
                        if (methodName.charAt(endBefore - 3) == 'i' &&
                            methodName.charAt(endBefore - 2) == 'k')
                            constraint = AttributeConstraint.Like;
                    } else if (ch == 'T') {
                        if (methodName.charAt(endBefore - 3) == 'r' &&
                            methodName.charAt(endBefore - 2) == 'u')
                            constraint = AttributeConstraint.True;
                    } else if (endsWith("Fals", methodName, start, endBefore - 1)) {
                        constraint = AttributeConstraint.False;
                    }
                }
                break;
            case 'h': // StartsWith | EndsWith
                if (length > 8) {
                    char ch = methodName.charAt(endBefore - 8);
                    if (ch == 'E') {
                        if (endsWith("ndsWit", methodName, start, endBefore - 1))
                            constraint = AttributeConstraint.EndsWith;
                    } else if (endBefore > 10 && ch == 'a' &&
                               endsWith("StartsWit", methodName, start, endBefore - 1)) {
                        constraint = AttributeConstraint.StartsWith;
                    }
                }
                break;
            case 's': // Contains
                if (endsWith("Contain", methodName, start, endBefore - 1))
                    constraint = AttributeConstraint.Contains;
                break;
            case 'y': // Empty
                if (endsWith("Empt", methodName, start, endBefore - 1))
                    constraint = AttributeConstraint.Empty;
        }

        endBefore -= constraint.lengthWithinMethodName();

        boolean negated = endsWith(Not.name(), methodName, start, endBefore);
        endBefore -= (negated ? 3 : 0);

        boolean ignoreCase = endsWith(IgnoreCase.name(), methodName, start, endBefore);
        endBefore -= (ignoreCase ? 10 : 0);

        String attribute = methodName.substring(start, endBefore);

        if (attribute.length() == 0)
            throw Fail.unsupportedMethod(this);

        String name = getAttributeName(attribute, true);

        StringBuilder attributeExpr = new StringBuilder();
        if (ignoreCase)
            attributeExpr.append("LOWER(");

        appendAttributeName(name, attributeExpr);

        if (ignoreCase)
            attributeExpr.append(')');

        if (negated) {
            constraint = constraint.negate();
            negated = constraint.isNegative();
        }

        boolean isCollection = entityInfo.collectionElementTypes.containsKey(name);
        if (isCollection && (ignoreCase || !constraint.supportsCollections()))
            throw exc(MappingException.class,
                      "CWWKD1110.incompat.with.collection",
                      method.getName(),
                      repositoryInterface.getName(),
                      ignoreCase ? IgnoreCase.name() : constraint.name(),
                      name,
                      entityInfo.getType().getName(),
                      Util.constraintsThatSupportCollections());

        switch (constraint) {
            case Equal:
            case GreaterThan:
            case GreaterThanEqual:
            case LessThan:
            case LessThanEqual:
            case Not:
                q.append(attributeExpr).append(constraint.operator());
                generateParam(q, ignoreCase, ++qlParamCount);
                break;
            case StartsWith:
            case NotStartsWith:
                q.append(attributeExpr) //
                                .append(negated ? " NOT " : " ") //
                                .append("LIKE CONCAT(");
                generateParam(q, ignoreCase, ++qlParamCount).append(", '%')");
                break;
            case EndsWith:
            case NotEndsWith:
                q.append(attributeExpr) //
                                .append(negated ? " NOT " : " ") //
                                .append("LIKE CONCAT('%', ");
                generateParam(q, ignoreCase, ++qlParamCount).append(")");
                break;
            case Like:
            case NotLike:
                q.append(attributeExpr) //
                                .append(negated ? " NOT " : " ") //
                                .append("LIKE ");
                generateParam(q, ignoreCase, ++qlParamCount);
                break;
            case Between:
            case NotBetween:
                q.append(attributeExpr).append(constraint.operator());
                generateParam(q, ignoreCase, ++qlParamCount).append(" AND ");
                generateParam(q, ignoreCase, ++qlParamCount);
                break;
            case Contains:
            case NotContains:
                if (isCollection) {
                    q.append(" ?").append(++qlParamCount) //
                                    .append(negated ? " NOT " : " ") //
                                    .append("MEMBER OF ").append(attributeExpr);
                } else {
                    q.append(attributeExpr) //
                                    .append(negated ? " NOT " : " ") //
                                    .append("LIKE CONCAT('%', ");
                    generateParam(q, ignoreCase, ++qlParamCount).append(", '%')");
                }
                break;
            case In:
            case NotIn:
                if (ignoreCase)
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1074.qbmn.incompat.keywords",
                              method.getName(),
                              repositoryInterface.getName(),
                              IgnoreCase.name(),
                              constraint.name());
                q.append(attributeExpr).append(constraint.operator());
                generateParam(q, ignoreCase, ++qlParamCount);
                break;
            case Null:
            case NotNull:
            case True:
            case False:
                q.append(attributeExpr).append(constraint.operator());
                break;
            case Empty:
                q.append(attributeExpr).append(isCollection //
                                ? AttributeConstraint.Empty.operator() //
                                : AttributeConstraint.Null.operator());
                break;
            case NotEmpty:
                q.append(attributeExpr).append(isCollection //
                                ? AttributeConstraint.NotEmpty.operator() //
                                : AttributeConstraint.NotNull.operator());
                break;
            default:
                throw new UnsupportedOperationException(constraint.name());
        }
    }

    /**
     * Appends JPQL to the partially built query to represent a Constraint.
     *
     * @param q              partially built query to which to append JPQL
     *                           representing the Constraint.
     * @param constraint     the Constraint for which to generate JPQL.
     * @param jpqlParamCount number of named or positional parameters identified
     *                           up to this point for the JPQL.
     * @param jpqlParamNames names of named parameters in the partially built
     *                           query. Empty if the query uses positional
     *                           parameters or has none. If using named parameters,
     *                           this method should add any that are generated.
     * @param jpqlParams     list for this method to populate with the name of
     *                           named parameters or index of positional parameters,
     *                           mapped to value, for each value obtained from the
     *                           processed Restriction(s).
     * @return the new count of named or positional parameters, including any that
     *         were generated for the Constraint.
     */
    protected abstract int generateConstraint(StringBuilder q,
                                              Object constraint,
                                              int jpqlParamCount,
                                              Set<String> jpqlParamNames,
                                              Map<Object, Object> jpqlParams);

    /**
     * Generates a query to select the COUNT of all entities matching the
     * supplied WHERE condition(s), or all entities if no WHERE conditions.
     * Populates the jpqlCount of the query information with the result.
     *
     * @param where the WHERE clause
     */
    private void generateCount(String where) {
        String o = entityVar;
        StringBuilder q = new StringBuilder(21 + 2 * o.length() +
                                            entityInfo.name.length() +
                                            (where == null ? 0 : where.length())) //
                                                            .append("SELECT COUNT(").append(o).append(") FROM ") //
                                                            .append(entityInfo.name);
        if (o != THIS)
            q.append(' ').append(o);

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
        boolean positionalParams = jpqlParamNames.isEmpty();
        String[] paramNames = positionalParams ? null : new String[numSorts];
        StringBuilder a = fwd == null //
                        ? null //
                        : new StringBuilder(200).append(hasWhere ? " AND (" : " WHERE (");
        StringBuilder b = prev == null //
                        ? null //
                        : new StringBuilder(200).append(hasWhere ? " AND (" : " WHERE (");
        for (int i = 0; i < numSorts; i++) {
            if (!positionalParams)
                paramNames[i] = generateNamedParameterName("cursor",
                                                           qlParamCount + i + 1);
            if (a != null)
                a.append(i == 0 ? "(" : " OR (");
            if (b != null)
                b.append(i == 0 ? "(" : " OR (");
            for (int s = 0; s <= i; s++) {
                Sort<?> sort = sorts.get(s);
                boolean asc = sort.isAscending();
                boolean lower = sort.ignoreCase();
                String name = sort.property();
                if (a != null)
                    if (lower) {
                        a.append(s == 0 ? "LOWER(" : " AND LOWER(");
                        appendAttributeName(name, a);
                        a.append(')');
                        a.append(s < i ? '=' : (asc ? '>' : '<'));
                        a.append("LOWER(");
                        if (positionalParams)
                            a.append('?').append(qlParamCount + s + 1);
                        else
                            a.append(':').append(paramNames[s]);
                        a.append(')');
                    } else {
                        a.append(s == 0 ? "" : " AND ");
                        appendAttributeName(name, a);
                        a.append(s < i ? '=' : (asc ? '>' : '<'));
                        if (positionalParams)
                            a.append('?').append(qlParamCount + s + 1);
                        else
                            a.append(':').append(paramNames[s]);
                    }
                if (b != null)
                    if (lower) {
                        b.append(s == 0 ? "LOWER(" : " AND LOWER(");
                        appendAttributeName(name, b);
                        b.append(')');
                        b.append(s < i ? '=' : (asc ? '<' : '>'));
                        b.append("LOWER(");
                        if (positionalParams)
                            b.append('?').append(qlParamCount + s + 1);
                        else
                            b.append(':').append(paramNames[s]);
                        b.append(')');
                    } else {
                        b.append(s == 0 ? "" : " AND ");
                        appendAttributeName(name, b);
                        b.append(s < i ? '=' : (asc ? '<' : '>'));
                        if (positionalParams)
                            b.append('?').append(qlParamCount + s + 1);
                        else
                            b.append(':').append(paramNames[s]);
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
            q = new StringBuilder(24 + entityInfo.name.length() +
                                  o.length() * 2 +
                                  idAttrName.length()) //
                                                  .append("DELETE FROM ").append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            q.append(" WHERE ").append(o_).append(idAttrName).append("=?1");
        } else {
            q = new StringBuilder(200) //
                            .append("DELETE FROM ").append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            q.append(" WHERE ");
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
                        .append("DELETE FROM ").append(entityInfo.name);
        if (o != THIS)
            q.append(' ').append(o);

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
                q.append(o_).append(name).append("=?").append(++qlParamCount);
            }
        } else {
            q.append(o_).append(idName).append("=?").append(++qlParamCount);
        }

        if (entityInfo.versionAttributeName != null)
            q.append(" AND ").append(o_).append(entityInfo.versionAttributeName) //
                            .append("=?").append(++qlParamCount);

        q.append(')');

        return q;
    }

    /**
     * Generates the name of a named parameter with the given prefix and number
     * which is not already in use (as represented by jpqlParamNames). This method
     * ensures a unique name by appending the _ character after the number until
     * the name is found to be unique. For example, a prefix of {@code cursor}
     * and number of {@code 2} might result in generated parameter name
     * {@code :cursor2} or {@code :cursor2_} or {@code :cursor2__} or so forth
     * depending on whether the prior names are already used in the query.
     * This method updates the jpqlParamNames field to include the generated name,
     * but does not add to the qlParamCount.
     *
     * @param prefix text to include at the beginning of the generated name.
     * @param num    number to include after the prefix in the generated name.
     * @return the generated named parameter name.
     */
    @Trivial
    private String generateNamedParameterName(String prefix, int num) {
        String paramName = prefix + num;
        while (!jpqlParamNames.add(paramName))
            paramName += '_';
        return paramName;
    }

    /**
     * Generates the JPQL ORDER BY clause. This method is common between the
     * OrderBy annotation and keyword.
     *
     * @param q a JPQL query to which to add the ORDER BY clause.
     */
    private void generateOrderBy(StringBuilder q) {
        boolean needsCursorQueries = CursoredPage.class.equals(multiType);

        restrictAt = q.length() + 1;

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
     * Generates JPQL based on repository method parameters.
     * For each parameter, annotations on the parameter as well as
     * the parameter class determine a type of constraint or assignment
     * operation. Repository methods must be annotated with one of:
     * Count, Delete, Exists, Find, or Update.
     * Allowed special parameter types vary by method annotation.
     *
     * @param q           JPQL query to which to append a WHERE clause.
     *                        Or null in the case of Find/Update methods
     *                        to create a new JPQL query.
     * @param methodAnno  Count, Delete, Exists, Find, or Update. Never null.
     * @param countPages  indicates whether or not to count pages.
     *                        Only applies for find queries.
     * @param constraints map of method parameter index (0-based) to deferred
     *                        Constraint at the position. Empty if Constraint
     *                        values are not yet available or there are no
     *                        deferred Constraints.
     * @param jpqlParams  Map to be populated with JPQL parameter names and values
     *                        for Constraints and Restrictions. Map keys are the
     *                        named parameter name or positional parameter index.
     *                        Map values are obtained from the Constraints or
     *                        Restrictions. The first positional parameter index
     *                        starts at qlParamCount, which is updated by this
     *                        method when JPQL parameters for repository method
     *                        special parameters are added.
     */
    @Trivial
    private StringBuilder generateParamBasedQuery(StringBuilder q,
                                                  Annotation methodAnno,
                                                  boolean countPages,
                                                  Map<Integer, Object> constraints,
                                                  Map<Object, Object> jpqlParams) {
        boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "generateParamBasedQuery",
                     q,
                     methodAnno == null ? null : methodAnno.annotationType().getSimpleName(),
                     countPages,
                     constraints.keySet(),
                     jpqlParams == null ? null : jpqlParams.keySet());

        String o = entityVar;
        String o_ = entityVar_;
        DataVersionCompatibility compat = entityInfo.factory.provider.compat;

        Boolean isNamePresent = null; // unknown
        Parameter[] params = null;

        Set<Class<?>> specParamTypes = compat.specialParamTypes();
        Class<?>[] paramTypes = method.getParameterTypes();
        int numConstraints = paramTypes.length;
        while (numConstraints > 0 &&
               specParamTypes.contains(paramTypes[numConstraints - 1]))
            if (!compat.isSpecialParamValid(paramTypes[--numConstraints], type))
                throw Fail.methodParamInvalid(this, paramTypes[numConstraints], methodAnno);

        Annotation[][] annosForAllParams = method.getParameterAnnotations();

        // Arrays to be populated per repository method parameter
        String[] attrNames = new String[numConstraints];
        AttributeConstraint[] attrConstraints = //
                        new AttributeConstraint[numConstraints];
        char[] updateOps = new char[numConstraints];
        int[] numPreviousJPQLParams = new int[numConstraints + 1];
        StringBuilder[] constraintJPQL = new StringBuilder[numConstraints];

        // p is the repository method parameter number (0-based)
        // qp is the JPQL query parameter number (1-based)
        int numJPQLParams = 0;
        for (int p = 0; p < numConstraints; p++) {
            numPreviousJPQLParams[p] = numJPQLParams;

            Object constraint = constraints.get(p);
            if (constraint == null) {
                numJPQLParams = inspectMethodParam(p,
                                                   paramTypes[p],
                                                   annosForAllParams[p],
                                                   attrNames,
                                                   attrConstraints,
                                                   updateOps,
                                                   numJPQLParams);
                if (numJPQLParams < 0)
                    Fail.methodParamAnnoConflict(this, numJPQLParams, p,
                                                 paramTypes[p], annosForAllParams[p]);
            } else {
                constraintJPQL[p] = new StringBuilder(50);
                numJPQLParams = generateConstraint(constraintJPQL[p],
                                                   constraint,
                                                   numJPQLParams,
                                                   jpqlParamNames,
                                                   jpqlParams);
            }

            // Determine the entity attribute name, first from @By or an assignment
            // annotation.
            String name = attrNames[p];
            if (name == null) {
                for (Annotation anno : annosForAllParams[p])
                    if (anno instanceof By)
                        name = ((By) anno).value();
            }
            // Otherwise determine the entity attribute name from the parameter name,
            if (name == null ||
                name.length() == 0 && attrNames[p] != null) { // TODO 1.2 allow the empty string on assign annos?
                if (isNamePresent == null) {
                    params = method.getParameters();
                    isNamePresent = params[p].isNamePresent();
                }
                if (Boolean.TRUE.equals(isNamePresent))
                    name = params[p].getName();
                else
                    throw Fail.methodParamLacksAnno(this, p + 1);
            }
            attrNames[p] = getAttributeName(name, true);
        }

        numPreviousJPQLParams[numConstraints] = numJPQLParams;

        // Write new JPQL, starting with SELECT or UPDATE
        if (q == null && type == FIND) { // SELECT
            q = generateSelectClause() //
                            .append(" FROM ") //
                            .append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
        } else if (q == null) { // UPDATE
            q = new StringBuilder(250).append("UPDATE ") //
                            .append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            q.append(" SET");

            boolean needsVersionUpdate = entityInfo.versionAttributeName != null;
            boolean first = true;
            // p is the repository method parameter position (0-based)
            for (int p = 0; p < numConstraints; p++) {
                char op = updateOps[p];
                if (op != Character.MIN_VALUE) {
                    if (op != '=' &&
                        entityInfo.idClassAttributeAccessors != null &&
                        paramTypes[p].equals(entityInfo.idType)) {
                        // TODO 1.1
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
                        String name = attrNames[p];

                        if (needsVersionUpdate &&
                            name.equals(entityInfo.versionAttributeName))
                            needsVersionUpdate = false;

                        q.append(first ? " " : ", ");
                        appendAttributeName(name, q);
                        q.append("=");
                        first = false;

                        boolean withFunction = false;
                        switch (op) {
                            case '=':
                                break;
                            case '+':
                                Class<?> attrType = entityInfo.attributeTypes.get(name);
                                withFunction = CharSequence.class.isAssignableFrom(attrType);
                                if (withFunction)
                                    q.append("CONCAT(").append(o_).append(name).append(',');
                                else
                                    q.append(o_).append(name).append('+');
                                break;
                            default:
                                q.append(o_).append(name).append(op);
                        }

                        qlParamCount++;
                        q.append('?').append(numPreviousJPQLParams[p] + 1);

                        if (withFunction)
                            q.append(')');
                    }
                }
            }

            if (needsVersionUpdate) {
                Class<?> versionType = entityInfo.attributeTypes //
                                .get(entityInfo.versionAttributeName);

                q.append(first ? " " : ", ");
                appendAttributeName(entityInfo.versionAttributeName, q);
                q.append("=");
                if (LocalDateTime.class.equals(versionType) ||
                    Instant.class.equals(versionType)) {
                    q.append("LOCAL DATETIME");
                } else {
                    appendAttributeName(entityInfo.versionAttributeName, q);
                    q.append(" + 1");
                }
                first = false;
            }

            if (first) // No parameters are annotated to indicate update.
                throw Fail.lifeCycleMethodParamCount(this, Update.class);
        }

        int startIndexForWhereClause = q.length();

        // append the WHERE clause
        // p is the repository method parameter position (0-based)
        for (int p = 0; p < numConstraints; p++) {
            if (attrConstraints[p] != null || constraintJPQL[p] != null) {
                if (hasWhere) {
                    q.append(" AND ");
                } else {
                    q.append(" WHERE (");
                    hasWhere = true;
                }

                String name = attrNames[p];

                boolean isCollection = entityInfo.collectionElementTypes //
                                .containsKey(name);

                qlParamCount += numPreviousJPQLParams[p + 1] - numPreviousJPQLParams[p];

                if (constraintJPQL[p] == null) {
                    appendConstraint(q,
                                     o_,
                                     name,
                                     attrConstraints[p],
                                     numPreviousJPQLParams[p],
                                     isCollection,
                                     annosForAllParams[p]);
                } else {
                    if (name.charAt(name.length() - 1) != ')')
                        q.append(o_);

                    q.append(name).append(constraintJPQL[p]);

                    // TODO @IgnoreCase on Constraint with expression
                }
            }
        }
        if (hasWhere)
            q.append(')');

        if (countPages && type == FIND)
            generateCount(numConstraints == 0 ? null : q.substring(startIndexForWhereClause));

        specialParamsStartAt = locateSpecialParameters(paramTypes);

        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "generateParamBasedQuery", q);
        return q;
    }

    /**
     * Appends JPQL to the partially built query to implement a Restriction
     * parameter of a repository method.
     *
     * @param q              partially built query ending with the WHERE clause.
     * @param restriction    value of Restriction parameter. Otherwise null.
     * @param jpqlParamCount number of named or positional parameters in the
     *                           partially built query.
     * @param jpqlParamNames names of named parameters in the partially bulit
     *                           query. Empty if the query uses positional
     *                           parameeters or has none. If using named parameters,
     *                           this method should add any that are generated for
     *                           the restriction part of the query.
     * @param jpqlParams     list for this method to populate with the name of
     *                           named parameters or index of positional parameters,
     *                           mapped to value, for each value obtained from the
     *                           processed Restriction(s).
     * @return the new count of named or positional parameters, including any that
     *         were generated for the Restriction(s).
     */
    protected abstract int generateRestrictions(StringBuilder q,
                                                Object restriction,
                                                int jpqlParamCount,
                                                Set<String> jpqlParamNames,
                                                Map<Object, Object> jpqlParams);

    /**
     * Generates the SELECT clause of the JPQL.
     *
     * @return the SELECT clause.
     */
    @FFDCIgnore(RuntimeException.class) // caught to switch to better error
    private StringBuilder generateSelectClause() {
        StringBuilder q = new StringBuilder(200);
        String o = entityVar;

        String[] cols, selections = entityInfo.factory.provider.compat.getSelections(method);
        if (selections.length == 0) {
            cols = null;
        } else if (type == FIND_AND_DELETE) {
            // Unreachable in version 1.0 and uncertain what will be added to
            // the spec regarding selections. Deferring NLS message until then.
            throw new UnsupportedOperationException //
            ("The " + method.getName() + " method of the " +
             repositoryInterface.getName() + " repository has a " +
             method.getGenericReturnType().getTypeName() + " return type and" +
             " specifies to return the " +
             Arrays.toString(selections) + " entity attributes," +
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

        if (type == FIND_AND_DELETE &&
            !(singleType.isAssignableFrom(Util.wrapperClassIfPrimitive(entityInfo.idType)) ||
              singleType.isAssignableFrom(entityInfo.getType()))) {
            throw Fail.returnTypeInvalidForDelete(this);
        }

        if (cols == null || cols.length == 0) {
            if (singleType.isAssignableFrom(entityInfo.entityClass)
                || entityInfo.inheritance && entityInfo.entityClass.isAssignableFrom(singleType)) {
                // Whole entity
                // Omission of the optional SELECT clause means "SELECT this" per
                // the Jakarta Persistence spec. Given that, a SELECT clause ends up
                // being required if the entity identification variable is not "this"
                if (o != THIS)
                    q.append("SELECT ").append(o);
            } else if (entityInfo.idClassAttributeAccessors != null &&
                       singleType.equals(entityInfo.idType)) {
                // IdClass
                q.append("SELECT ID(").append(entityVar).append(')');
            } else {
                // Is the result type a record?
                RecordComponent[] recordComponents = singleType.getRecordComponents();
                if (recordComponents == null) {
                    // not a record, not an entity, and app did not use @Select
                    throw Fail.returnTypeInvalidForFind(this);
                } else {
                    // Construct new instance for record
                    q.append("SELECT NEW ").append(singleType.getName()).append('(');

                    String[] names = new String[recordComponents.length];
                    for (int i = 0; i < recordComponents.length; i++) {
                        String[] select = entityInfo.factory.provider.compat //
                                        .getSelections(recordComponents[i]);
                        if (select == null || select.length == 0)
                            names[i] = recordComponents[i].getName();
                        else if (select.length == 1)
                            names[i] = select[0];
                        else
                            throw new UnsupportedOperationException("@Select " + Arrays.toString(select)); // 1.1 TODO
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
                        throw Fail.selectedAttributesMismatch(this, names, x);
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
     * Generates and appends JPQL to sort based on the specified entity attribute.
     * For most attributes, this will be of a form such as o.name or LOWER(o.name) DESC or ...
     *
     * @param q             builder for the JPQL query.
     * @param sort          sort criteria for a single attribute (name must already
     *                          be converted to a valid entity attribute name).
     * @param sameDirection indicate to append the Sort in the normal direction.
     *                          Otherwise reverses it (for cursor pagination in the
     *                          previous page direction).
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

        String nullOrdering = getNullOrdering(sort, sameDirection);
        if (nullOrdering != null)
            q.append(" NULLS ").append(nullOrdering);
    }

    /**
     * Generates JPQL for updates of an entity by entity id and version (if versioned).
     */
    private StringBuilder generateUpdateEntity() {
        String o = entityVar;
        String o_ = entityVar_;
        StringBuilder q;

        if (type == LC_UPDATE) {
            q = new StringBuilder(100) //
                            .append("UPDATE ").append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            q.append(" SET ");

            boolean first = true;
            for (String name : entityInfo.attributeNamesForEntityUpdate) {
                if (first)
                    first = false;
                else
                    q.append(", ");

                q.append(o_).append(name).append("=?").append(++qlParamCount);
            }
        } else { // type == LC_UPDATE_MERGE
            // Update that returns an entity. And also used when an entity has a
            // version attribute or relation attribute that requires using em.merge.
            // Perform a find operation first so that em.merge can be used.

            q = new StringBuilder(100) //
                            .append("SELECT ").append(o) //
                            .append(" FROM ").append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
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
                q.append(o_).append(name).append("=?").append(++qlParamCount);
            }
        } else {
            q.append(o_).append(idName).append("=?").append(++qlParamCount);
        }

        if (entityInfo.versionAttributeName != null)
            q.append(" AND ").append(o_).append(entityInfo.versionAttributeName) //
                            .append("=?").append(++qlParamCount);

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
            generateConstraint(methodName, i, iNext < 0 || iNext >= endBefore ? endBefore : iNext, q);
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
    private String getAttributeName(final String name, final boolean failIfNotFound) {
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
                        throw Fail.functionNotApplicable(this, name, "@Id");
                } else if (len == 13 && name.regionMatches(true, 0, "version", 0, 7)) {
                    // version(this)
                    if (entityInfo.versionAttributeName == null && failIfNotFound)
                        throw Fail.functionNotApplicable(this, name, "@Version");
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
                // allow functions, such as LENGTH(name)
                attributeName = name;
        } else if (len == 0) {
            throw Fail.entityAttributeNameMissing(this);
        } else {
            String lowerName = name.toLowerCase();
            attributeName = entityInfo.attributeNames.get(lowerName);
            if (attributeName == null) {
                // tolerate possible mixture of . and _ separators:
                lowerName = lowerName.replace('.', '_');
                attributeName = entityInfo.attributeNames.get(lowerName);
                if (attributeName == null) {
                    // tolerate possible mixture of . and _ separators with lack of separators:
                    lowerName = lowerName.replace("_", "");
                    attributeName = entityInfo.attributeNames.get(lowerName);
                    if (attributeName == null) {
                        boolean nameCharsOnly = true;
                        for (int i = 0; nameCharsOnly && i < lowerName.length(); i++)
                            nameCharsOnly &= Character //
                                            .isJavaIdentifierPart(lowerName.charAt(i));
                        if (!nameCharsOnly)
                            // allow functions, such as: length * width
                            attributeName = name;

                        if (nameCharsOnly && failIfNotFound)
                            throw Fail.unknownEntityAttribute(this, name);
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getAttributeName " + name + ": " + attributeName);
        return attributeName;
    }

    /**
     * Identify Constraint-typed repository method parameters for which
     * processing is deferred. Constraints that operate on non-literal
     * expressions are always deferred until the expression instance is
     * available.
     *
     * @param alwaysDefer  indicates that processing of every Constraint-typed
     *                         method parameter is always deferred.
     * @param methodParams repository method parameters.
     * @return map of method parameter index (0-based) to Constraint instance
     *         at that position. The empty map indicates none.
     */
    protected abstract Map<Integer, Object> getDeferredConstraints(boolean alwaysDefer,
                                                                   Object[] methodParams);

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
     * Looks for mutually exclusive annotations (Delete, Find, Query, ...)
     * on the repository method, validating that at most one is present and
     * returning the annotation that is found. Otherwise null.
     *
     * @return annotation if present on the method.
     */
    @Trivial
    private Annotation getMutuallyExclusiveMethodAnno() {
        Annotation methodAnno = null;
        List<String> conflicts = new LinkedList<String>();
        DataVersionCompatibility compat = producer.compat();

        BiFunction<Annotation, Annotation, Annotation> inspect = (anno, previous) -> {
            if (anno == null) {
                return previous;
            } else if (previous == null) {
                return anno;
            } else { // conflict
                if (conflicts.isEmpty())
                    conflicts.add(previous.annotationType().getSimpleName());
                conflicts.add(anno.annotationType().getSimpleName());
                return previous;
            }
        };

        for (Class<? extends Annotation> annoClass : compat.lifeCycleAnnoTypes(null))
            methodAnno = inspect.apply(method.getAnnotation(annoClass), methodAnno);

        methodAnno = inspect.apply(compat.getCountAnnotation(method), methodAnno);
        methodAnno = inspect.apply(compat.getExistsAnnotation(method), methodAnno);

        Integer first = compat.getFirstAnnotationValue(method);
        if (first != null) {
            if (first < 1) {
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1029.first.neg.or.zero",
                          method.getName(),
                          repositoryInterface.getName(),
                          first);
            } else if (methodAnno == null) {
                maxResults = first;
            } else {
                conflicts.add(methodAnno.annotationType().getSimpleName());
                conflicts.add("First");
            }
        }

        OrderBy[] orderBy = method.getAnnotationsByType(OrderBy.class);
        if (orderBy.length > 0 &&
            methodAnno != null &&
            !(methodAnno instanceof Delete))
            conflicts.add(OrderBy.class.getName());

        for (Class<? extends Annotation> annoClass : compat.queryLanguageAnnoTypes())
            methodAnno = inspect.apply(method.getAnnotation(annoClass), methodAnno);

        methodAnno = inspect.apply(method.getAnnotation(Find.class), methodAnno);

        if (!conflicts.isEmpty())
            // Invalid combination of multiple annotations
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1002.method.annos.err",
                      method.getName(),
                      repositoryInterface.getName(),
                      conflicts);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getMutuallyExclusiveMethodAnno: " +
                               (methodAnno == null ? null : methodAnno.annotationType()));
        return methodAnno;
    }

    /**
     * Returns Sort.nullOrdering as a String if FIRST or LAST, otherwise null.
     *
     * @param sort          sort criteria.
     * @param sameDirection indicates to interpret the Sort in the normal direction.
     *                          Otherwise reverses it (for cursor pagination in the
     *                          previous page direction).
     * @return "FIRST", "LAST", or null.
     */
    protected abstract String getNullOrdering(Sort<?> sort, boolean sameDirection);

    /**
     * Value from the Query annotation of Jakarta Data or from the JakartaQuery
     * annotatioh of Jakarta Persistence that supplies JPQL to a repository method.
     *
     * @return JPQL or JCQL value of a query annotation. Null if the repository
     *         method does not have a query annotation.
     */
    protected abstract String getQueryAnnoValue();

    /**
     * Obtain the query results and convert the type as needed. This method does
     * not include packaging results into an Optional or completion stage.
     *
     * @param entityHandler EntityAgent or EntityManager (both are EntityHandler)
     * @param qc            special parameters combined with equivalent method
     *                          annotations
     * @param query         the query
     * @param txStatus      transaction status
     * @return successful results of the query
     * @throws exception if unsuccessful
     */
    private Object getQueryResults(AutoCloseable entityHandler,
                                   QueryCustomization qc,
                                   jakarta.persistence.Query query,
                                   int txStatus) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        Object returnValue;
        if (multiType != null && BaseStream.class.isAssignableFrom(multiType)) {
            Stream<?> stream;
            // TODO 1.1 getResultStream can be used for stateful repositories
            //if (txStatus == Status.STATUS_NO_TRANSACTION)
            stream = query.getResultList().stream();
            //else
            //    stream = query.getResultStream();
            if (Stream.class.equals(multiType))
                returnValue = stream;
            else if (IntStream.class.equals(multiType))
                returnValue = stream.mapToInt(this::toInt);
            else if (LongStream.class.equals(multiType))
                returnValue = stream.mapToLong(this::toLong);
            else if (DoubleStream.class.equals(multiType))
                returnValue = stream.mapToDouble(this::toDouble);
            else
                throw Fail.resultConversion(this, List.class.getName(), null);
        } else {
            List<?> results = query.getResultList();

            if (trace) {
                Tr.debug(this, tc,
                         "result list type: " + (results == null ? null //
                                         : results.getClass().toGenericString()));
                if (results != null && !results.isEmpty()) {
                    Object r0 = results.get(0);
                    Tr.debug(this, tc,
                             "type of first result: " + (r0 == null ? null //
                                             : r0.getClass().toGenericString()));
                }
            }

            if (type == FIND_AND_DELETE)
                delete(results, entityHandler);

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
                    || type == FIND_AND_DELETE
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
                        Tr.debug(this, tc, "convert " +
                                           firstNonNullResult.getClass().getName() +
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
                                Class<?> subarrayType = //
                                                returnArrayType.getComponentType();
                                int len = Array.getLength(result);
                                Object subarray = Array.newInstance(subarrayType,
                                                                    len);
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
                        throw Fail.nonUniqueResult(this, size);
                    }
                } else {
                    String resultInfo = loggableAppend(firstNonNullResult //
                                    .getClass().getName(),
                                                       " (", firstNonNullResult, ")");
                    throw Fail.resultConversion(this, resultInfo, null);
                }
            } else if (results.isEmpty()) {
                throw Fail.emptyResult(this);
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
        return returnValue;
    }

    /**
     * Identifies the repository method type based on life cycle annotations.
     * For example (Insert, Update, Save, Delete, Detach, Merge, ...).
     */
    protected abstract void identifyType();

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
     * Gathers the information that is needed to perform the query that the
     * repository method represents.
     *
     * @param entityInfos map of entity name to entity information.
     * @param repository  repository implementation.
     * @return information about the query.
     */
    @FFDCIgnore(Throwable.class) // report invalid repository methods as errors instead
    @Trivial
    QueryInfo init(Map<String, CompletableFuture<EntityInfo>> entityInfos,
                   RepositoryImpl<?> repository) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "init", entityInfos, this);

        try {
            DataVersionCompatibility compat = repository.provider.compat;

            methodTypeAnno = getMutuallyExclusiveMethodAnno();

            entityInfo = entityInfos.size() == 1 //
                            ? entityInfos.values().iterator().next().join() //
                            : null; // defer to processing of Query value

            if (repository.validator != null) {
                boolean[] v = repository.validator.isValidatable(method);
                validateParams = v[0];
                validateResult = v[1];
            }

            boolean countPages = Page.class.equals(multiType) ||
                                 CursoredPage.class.equals(multiType);
            StringBuilder q = null;
            boolean validateNumberOfMethodArgs = true;

            String queryAnnoValue = getQueryAnnoValue();

            if (type == null && queryAnnoValue == null)
                identifyType();

            if (type == NATIVE) { // native query language method
                initNativeQueryLanguage(queryAnnoValue,
                                        compat);
            } else if (queryAnnoValue != null) { // query language method
                initQueryLanguage(queryAnnoValue,
                                  entityInfos,
                                  repository.primaryEntityInfoFuture,
                                  compat);
            } else if (type != null && type.isLifeCycleMethod) {
                if (type == LC_DELETE)
                    q = generateDeleteEntity();
                else if (type == LC_UPDATE || type == LC_UPDATE_MERGE)
                    q = generateUpdateEntity();
                // other life cycle methods don't use JPQL
            } else {
                if (methodTypeAnno == null) {
                    // Query by Method Name
                    q = initQueryByMethodName(countPages);
                } else {
                    // Query by Parameters
                    q = initQueryByParameters(countPages,
                                              NO_CONSTRAINTS_DEFERRED,
                                              null);

                    // Only validate if Constraint parameters correspond one-to-one
                    // with JPQL parameters.
                    validateNumberOfMethodArgs = qlParamCount == specialParamsStartAt;
                }

                if (type == FIND_AND_DELETE
                    && multiType != null
                    && Stream.class.isAssignableFrom(multiType)) {
                    throw Fail.returnTypeInvalidForDelete(this);
                }
            }

            // The @OrderBy annotation from Jakarta Data provides sort criteria statically
            OrderBy[] orderBy = method.getAnnotationsByType(OrderBy.class);
            if (orderBy.length > 0) {
                if (type != FIND && type != FIND_AND_DELETE || sorts != null)
                    throw Fail.orderByAnnoIncompat(this);

                sorts = new ArrayList<>(orderBy.length);
                if (q == null)
                    if (ql == null) {
                        q = generateSelectClause();
                        q.append(" FROM ").append(entityInfo.name);
                        if (entityVar != THIS)
                            q.append(' ').append(entityVar);
                        if (countPages)
                            generateCount(null);
                    } else {
                        q = new StringBuilder(ql);
                    }

                for (int i = 0; i < orderBy.length; i++)
                    addSort(orderBy[i]);

                if (sortPositions.length == 0) {
                    sortPositions = NONE_STATIC_SORT_ONLY;
                    generateOrderBy(q);
                }
            }

            // Default to ascending by ID when the repository method that uses
            // pagination does not provide a way to indicate sort criteria.
            if (type == FIND &&
                queryAnnoValue == null && // do not change the user's Query value
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

            ql = q == null ? ql : q.toString();

            validate(validateNumberOfMethodArgs);

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
     * Initializes query information based on the NativeQuery annotation.
     *
     * @param sql    NativeQuery.value() might be SQL or something else
     * @param compat isolates Jakarta Data version-dependent behavior
     */
    private void initNativeQueryLanguage(String sql,
                                         DataVersionCompatibility compat) {
        Parameter[] params = method.getParameters();
        Set<Class<?>> specialParamTypes = compat.specialParamTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> paramType = params[i].getType();
            if (specialParamTypes.contains(paramType)) {
                if (i < specialParamsStartAt)
                    specialParamsStartAt = i;
                // Reject all special parameters on native queries until we
                // TODO determine which, if any, can be supported
                if (!Limit.class.equals(paramType))
                    throw new UnsupportedOperationException //
                    ("The " + method.getName() + " method of the " +
                     repositoryInterface.getName() + " repository cannot have a " +
                     paramType.getSimpleName() + " parameter because the method is " +
                     "annotated " + "@NativeQuery" + "."); // TODO NLS
            } else if (i > specialParamsStartAt) {
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1098.spec.param.position.err",
                          method.getName(),
                          repositoryInterface.getName(),
                          params[specialParamsStartAt].getName(),
                          Util.names(specialParamTypes));
            }
        }

        qlParamCount = specialParamsStartAt;

        ql = sql;
    }

    /**
     * Initializes query information based on the Query or JakartaQuery annotation.
     *
     * @param jpql                    Query.value() might be JPQL or JDQL
     * @param entityInfos             map of entity name to entity information.
     * @param primaryEntityInfoFuture future for the repository's primary entity
     *                                    type if it has one, otherwise null.
     * @param compat                  isolates Jakarta Data version-dependent behavior
     */
    private void initQueryLanguage(String jpql,
                                   Map<String, CompletableFuture<EntityInfo>> entityInfos,
                                   CompletableFuture<EntityInfo> primaryEntityInfoFuture,
                                   DataVersionCompatibility compat) {
        // Find out how many parameters the method supplies to the query
        // versus which method parameters are special parameters.
        boolean addsToWHERE = false;
        Parameter[] params = method.getParameters();
        Set<Class<?>> specialParamTypes = compat.specialParamTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> paramType = params[i].getType();
            if (specialParamTypes.contains(paramType)) {
                if (i < specialParamsStartAt)
                    specialParamsStartAt = i;
                if ("jakarta.data.restrict.Restriction".equals(paramType.getName())) // TODO 1.1
                    if (addsToWHERE)
                        throw Fail.duplicateSpecialParam(this, "Restriction");
                    else
                        addsToWHERE = true;
            } else if (i > specialParamsStartAt) {
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1098.spec.param.position.err",
                          method.getName(),
                          repositoryInterface.getName(),
                          params[specialParamsStartAt].getName(),
                          Util.names(specialParamTypes));
            }
        }

        qlParamCount = specialParamsStartAt;

        // for collecting names of named parameters:
        LinkedHashSet<String> qlParamNames = new LinkedHashSet<>();

        // indices at which the query needs to be modified, along with the
        // type of modification needed
        TreeMap<Integer, QueryEdit> modifyAt = null;

        int length = jpql.length();
        int startAt = 0;
        char firstChar = ' ';
        while (startAt < length &&
               Character.isWhitespace(firstChar = jpql.charAt(startAt)))
            startAt++;

        if ((firstChar == 'D' || firstChar == 'd') &&
            startAt + 12 < length &&
            jpql.regionMatches(true, startAt + 1, "ELETE", 0, 5) &&
            Character.isWhitespace(jpql.charAt(startAt + 6))) {

            type = QM_DELETE; // DELETE FROM EntityName[ WHERE ...]

            startAt += 7; // start of FROM

            modifyAt = parseQuery(jpql,
                                  startAt,
                                  false,
                                  addsToWHERE,
                                  entityInfos,
                                  qlParamNames);
        } else if ((firstChar == 'U' || firstChar == 'u') &&
                   startAt + 13 < length &&
                   jpql.regionMatches(true, startAt + 1, "PDATE", 0, 5) &&
                   Character.isWhitespace(jpql.charAt(startAt + 6))) {

            type = QM_UPDATE; // UPDATE EntityName SET ...[ WHERE ...]

            int entityNameStartAt = startAt += 7;
            for (; startAt < length &&
                   Character.isWhitespace(jpql.charAt(startAt)); startAt++);
            StringBuilder entityName = new StringBuilder();
            for (char ch; startAt < length &&
                          Character.isJavaIdentifierPart(ch = jpql.charAt(startAt)); startAt++)
                entityName.append(ch);
            if (entityName.length() > 0)
                setEntityInfo(entityName.toString(), entityInfos, jpql);
            else
                throw Fail.queryLacksEntityName(this, jpql, "UPDATE");

            entityVar = parseIdentificationVariable(startAt, length, jpql);
            entityVar_ = entityVar + '.';

            modifyAt = parseQuery(jpql,
                                  startAt,
                                  false,
                                  addsToWHERE,
                                  entityInfos,
                                  qlParamNames);

            if (entityInfo == null || entityInfo.recordClass != null)
                modifyAt.put(entityNameStartAt, QueryEdit.REPLACE_RECORD_ENTITY);
        }

        if (type == null) {
            type = FIND; // SELECT ... or FROM ... or WHERE ... or ORDER BY ...

            int select0 = -1;
            if (length > startAt + 6
                && jpql.regionMatches(true, startAt, "SELECT", 0, 6)
                && !Character.isJavaIdentifierPart(jpql.charAt(startAt + 6))) {
                select0 = startAt += 6;
                // The end of the SELECT clause is a FROM, WHERE, GROUP BY, HAVING,
                // or ORDER BY clause, or the end of the query
            }

            addsToWHERE |= CursoredPage.class.equals(multiType);
            modifyAt = parseQuery(jpql,
                                  startAt,
                                  select0 >= 0,
                                  addsToWHERE,
                                  entityInfos,
                                  qlParamNames);
        }

        if (entityInfo == null)
            setEntityInfo(entityInfos, primaryEntityInfoFuture);

        if (modifyAt.isEmpty())
            ql = jpql;
        else
            ql = replaceQuery(jpql, modifyAt);

        // Validation of method parameters vs parameters in the query
        int qlParamNameCount = qlParamNames.size();
        boolean hasExtraParam = false;
        for (int i = 0; i < specialParamsStartAt; i++) {
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
                        throw Fail.namedParamConflict(this, paramName, params[i]);
                } else {
                    hasExtraParam = true;
                }
            }
        }

        sortPositions = NONE_QUERY_LANGUAGE_ONLY;
        for (int i = specialParamsStartAt; i < params.length; i++)
            if (SORT_PARAM_TYPES.contains(params[i].getType()))
                initDynamicSortPosition(i);

        int paramNamesCount = jpqlParamNames.size();
        if (hasExtraParam || qlParamNameCount != paramNamesCount) {
            // Does the method supply all named parameters that the query needs?
            LinkedHashSet<String> lacking = new LinkedHashSet<>(qlParamNames);
            lacking.removeAll(jpqlParamNames);
            if (!lacking.isEmpty())
                throw Fail.methodLacksNamedParams(this, lacking);

            // Does the method supply any named parameters not needed by the query?
            Set<String> extras = new LinkedHashSet<>(jpqlParamNames);
            extras.removeAll(qlParamNames);
            if (!extras.isEmpty())
                throw Fail.unusedNamedParamsOnMethod(this, extras, qlParamNames);
        }

        // Does the method supply a mixture of named and positional parameters?
        if (paramNamesCount > 0 && paramNamesCount < qlParamCount)
            throw Fail.mixedQLParamTypes(this, paramNamesCount);
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
        Class<?>[] paramTypes = method.getParameterTypes();
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
            q = generateSelectClause().append(" FROM ").append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            if (by > 0) {
                int where = q.length();
                generateWhereClause(methodName, by + 2, orderBy > 0 ? orderBy : methodName.length(), q);
                if (countPages)
                    generateCount(q.substring(where));
            }

            type = FIND;
            specialParamsStartAt = locateSpecialParameters(paramTypes);

            if (orderBy >= 0)
                parseOrderBy(orderBy, q);

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
                type = FIND_AND_DELETE;
                q = generateSelectClause().append(" FROM ").append(entityInfo.name);
                if (o != THIS)
                    q.append(' ').append(o);
                jpqlDelete = generateDeleteById();
            } else { // DELETE
                type = QM_DELETE;
                q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name);
                if (o != THIS)
                    q.append(' ').append(o);
            }

            if (by > 0)
                generateWhereClause(methodName, by + 2, orderBy > 0 ? orderBy : methodName.length(), q);

            type = type == null ? QM_DELETE : type;
            specialParamsStartAt = locateSpecialParameters(paramTypes);

            if (orderBy > 0)
                parseOrderBy(orderBy, q);
        } else if (methodName.startsWith("count")) {
            q = new StringBuilder(150) //
                            .append("SELECT COUNT(").append(o).append(") FROM ") //
                            .append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            if (by > 0 && methodName.length() > by + 2)
                generateWhereClause(methodName, by + 2, methodName.length(), q);
            type = COUNT;
            specialParamsStartAt = locateSpecialParameters(paramTypes);
        } else if (methodName.startsWith("exists")) {
            q = new StringBuilder(200) //
                            .append("SELECT ID(").append(o).append(") FROM ") //
                            .append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            if (by > 0 && methodName.length() > by + 2)
                generateWhereClause(methodName, by + 2, methodName.length(), q);
            type = EXISTS;
            specialParamsStartAt = locateSpecialParameters(paramTypes);
            validateReturnForExists();
        } else {
            throw Fail.unsupportedMethod(this);
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
     * @param countPages  whether to generate a count query (for Page.totalElements
     *                        and Page.totalPages).
     * @param constraints map of method parameter index (0-based) to deferred
     *                        Constraint at the position. Null indicates
     *                        Constraint values are not yet avaiable or there
     *                        are no deferred Constraints.
     * @param jpqlParams  Map to be populated with JPQL parameter names and values
     *                        for Constraints and Restrictions. Map keys are the
     *                        named parameter name or positional parameter index.
     *                        Map values are obtained from the Constraints or
     *                        Restrictions. The first positional parameter index
     *                        starts at qlParamCount, which is updated by this
     *                        method when JPQL parameters for repository method
     *                        special parameters are added.
     * @return the generated query written to a StringBuilder.
     */
    @Trivial
    private StringBuilder initQueryByParameters(boolean countPages,
                                                Map<Integer, Object> constraints,
                                                Map<Object, Object> jpqlParams) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "initQueryByParameters",
                     methodTypeAnno == null ? null : methodTypeAnno.annotationType().getSimpleName(),
                     countPages,
                     constraints.keySet(),
                     jpqlParams == null ? null : jpqlParams.keySet());

        String o = entityVar;
        StringBuilder q = null;

        if (methodTypeAnno instanceof Find) {
            type = FIND;
            q = generateParamBasedQuery(null, methodTypeAnno, countPages, constraints, jpqlParams);
        } else if (methodTypeAnno instanceof Update) {
            type = QM_UPDATE;
            q = generateParamBasedQuery(null, methodTypeAnno, countPages, constraints, jpqlParams);
        } else if (methodTypeAnno instanceof Delete) {
            if (isFindAndDelete()) {
                type = FIND_AND_DELETE;
                q = generateSelectClause().append(" FROM ").append(entityInfo.name);
                if (o != THIS)
                    q.append(' ').append(o);
                jpqlDelete = generateDeleteById();
            } else { // DELETE
                type = QM_DELETE;
                q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name);
                if (o != THIS)
                    q.append(' ').append(o);
            }
            if (method.getParameterCount() > 0)
                generateParamBasedQuery(q, methodTypeAnno, countPages, constraints, jpqlParams);
        } else if ("Count".equals(methodTypeAnno.annotationType().getSimpleName())) {
            type = COUNT;
            q = new StringBuilder(150).append("SELECT COUNT(").append(o).append(") FROM ").append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            if (method.getParameterCount() > 0)
                generateParamBasedQuery(q, methodTypeAnno, countPages, constraints, jpqlParams);
        } else if ("Exists".equals(methodTypeAnno.annotationType().getSimpleName())) {
            type = EXISTS;
            validateReturnForExists();
            q = new StringBuilder(200) //
                            .append("SELECT ID(").append(o).append(") FROM ") //
                            .append(entityInfo.name);
            if (o != THIS)
                q.append(' ').append(o);
            if (method.getParameterCount() > 0)
                generateParamBasedQuery(q, methodTypeAnno, countPages, constraints, jpqlParams);
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
     * @param arg           the entity or record, or array/Iterable/Stream
     *                          of entity or record
     * @param entityHandler the EntityAgent or EntityManager
     * @return the inserted entities, using the return type that is required by the
     *         Insert method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial
    Object insert(Object arg, AutoCloseable entityHandler) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
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
                ehInsert(entityHandler, entity); // TODO entityAgent.insertMultiple?
                if (results != null)
                    results.add(entity);
            }
        } else if (arg instanceof Iterable) {
            results = resultVoid ? null : new ArrayList<>();
            for (Object e : ((Iterable<?>) arg)) {
                entityCount++;
                Object entity = toEntity(e);
                ehInsert(entityHandler, entity);
                if (results != null)
                    results.add(entity);
            }
        } else {
            entityCount = 1;
            hasSingularEntityParam = true;
            results = resultVoid ? null : new ArrayList<>(1);
            Object entity = toEntity(arg);
            ehInsert(entityHandler, entity);
            if (results != null)
                results.add(entity);
        }

        if (entityCount == 0)
            throw Fail.emptyLifeCycleParam(this);

        if (entityHandler instanceof EntityManager em) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "flush");
            em.flush();
        }

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
                        throw Fail.resultSizeMismatch(this, "@Insert", results.size(),
                                                      hasSingularEntityParam);
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = convertToIterable(results, multiType, null, null);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw Fail.returnTypeInvalid(this, "Insert", hasSingularEntityParam,
                                                 null, results.get(0).getClass());
            }
        }

        if (CompletableFuture.class.equals(returnType) ||
            CompletionStage.class.equals(returnType)) {
            // useful for @Asynchronous
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw Fail.returnTypeInvalid(this, "Insert", hasSingularEntityParam,
                                         null, results.get(0).getClass());
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "insert", loggable(returnValue));
        return returnValue;
    }

    /**
     * Inspects the type and annotations of a method parameter to a parameter-based
     * Find/Delete/Update method to determine its meaning. Based on the meaning,
     * updates one or more of (attrNames, constraints, updateOps) at position p.
     *
     * @param p                 repository method parameter index (0-based).
     * @param paramType         class of the repository method parameter at index p.
     *                              When generating the query upfront, this is from
     *                              the repository method signature. When generating
     *                              the query at invocation time and a Constraint
     *                              subtype is supplied, this is the class of the
     *                              supplied instance.
     * @param paramAnnos        annotations on the repository method parameter at
     *                              index p.
     * @param attrNames         the implementer can update this at position p to
     *                              supply the entity attribute name from the value
     *                              of an assignment annotation.
     * @param constraints       the implementer can update this at position p to
     *                              supply the constraint type indicated by the
     *                              Is annotation or by a Constraint-typed method
     *                              parameter.
     * @param updateOps         the implementer can update this at position p to
     *                              supply the update operation indicated by an
     *                              assignment annotation.
     * @param prevNumJPQLParams count of JQPL query parameters required for
     *                              repository method parameters up to, but not
     *                              including, the current repository method
     *                              parameter being inspected.
     * @return count of JPQL query parameters required for repository method
     *         parameters up to and including the current one. Otherwise returns
     *         an error code: PARAM_ANNO_CONFLICTS_WITH_CONSTRAINT or
     *         PARAM_ANNOS_CONFLICT.
     */
    protected abstract int inspectMethodParam(int p,
                                              Class<?> paramType,
                                              Annotation[] paramAnnos,
                                              String[] attrNames,
                                              AttributeConstraint[] constraints,
                                              char[] updateOps,
                                              int prevNumJPQLParams);

    /**
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     * @param future future for this QueryInfo.
     */
    @FFDCIgnore(Throwable.class)
    @Trivial
    public void introspect(PrintWriter writer,
                           String indent,
                           CompletableFuture<QueryInfo> future) {
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

        writer.println(indent + "method annotation: " +
                       methodTypeAnno);
        writer.println(indent + "first special parameter at index " +
                       specialParamsStartAt + " (0-based)");

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

        final String QL = type == QueryType.NATIVE ? "SQL" : "JPQL";
        final String qlIndent = indent + "      ";
        writer.print(indent + QL + ": ");
        Util.printlnIndented(ql, writer, qlIndent);
        writer.print(indent + "JPQL for afterCursor: ");
        Util.printlnIndented(jpqlAfterCursor, writer, qlIndent);
        writer.print(indent + "JPQL for jpqlBeforeCursor: ");
        Util.printlnIndented(jpqlBeforeCursor, writer, qlIndent);
        writer.print(indent + "JPQL count query: ");
        Util.printlnIndented(jpqlCount, writer, qlIndent);
        writer.print(indent + "JPQL delete query: ");
        Util.printlnIndented(jpqlDelete, writer, qlIndent);

        writer.println(indent + QL + " parameter count: " + qlParamCount);
        writer.println(indent + "JPQL parameter names: " + jpqlParamNames);

        writer.println(indent + "maximum results: " + maxResults);
        writer.println(indent + "restrictions can be added at: " + restrictAt);
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

        if (future != null) {
            writer.print(indent + "state: ");
            if (future.isCancelled())
                writer.println("cancelled");
            else if (future.isDone())
                try {
                    future.join();
                    writer.println("completed");
                } catch (Throwable x) {
                    writer.println("failed");
                    Util.printStackTrace(x, writer, indent + "  ", null);
                }
            else
                writer.println("not completed");
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
    private boolean isFindAndDelete() {

        boolean isFindAndDelete = isOptional
                                  || multiType != null
                                  || !Util.RETURN_TYPES_FOR_UPDATE_COUNT.contains(singleType);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "isFindAndDelete? " + isFindAndDelete +
                               "; optional?: " + isOptional +
                               "; multiType: " + (multiType == null ? null : multiType.getSimpleName()) +
                               "; singleType: " + (singleType == null ? null : singleType.getSimpleName()));

        if (isFindAndDelete &&
            singleType != null &&
            !singleType.equals(entityInfo.entityClass) &&
            !singleType.equals(entityInfo.recordClass) &&
            !singleType.equals(Object.class) &&
            !Util.wrapperClassIfPrimitive(singleType) //
                            .equals(Util.wrapperClassIfPrimitive(entityInfo.idType)))
            throw Fail.returnTypeInvalidForDelete(this);

        return isFindAndDelete;
    }

    /**
     * Locates the repository method special parameters. These are positioned
     * after the query parameters. Records positions (0-based) of all sort criteria
     * if valid for the type of repository method.
     *
     * @param paramTypes method parameter types.
     * @return 0-based index at which which the first special parameter occurs in
     *         the method parameters. If no special parameters, then the number of
     *         method parameters.
     */
    private int locateSpecialParameters(Class<?>[] paramTypes) {
        DataVersionCompatibility compat = entityInfo.factory.provider.compat;
        Set<Class<?>> specialParamTypes = compat.specialParamTypes();
        int specialParamsStartAt = paramTypes.length; // not found yet

        for (int i = 0; i < paramTypes.length; i++) {
            if (i < specialParamsStartAt &&
                specialParamTypes.contains(paramTypes[i]))
                specialParamsStartAt = i;

            if (i >= specialParamsStartAt)
                if (compat.isSpecialParamValid(paramTypes[i], type) &&
                    (i == specialParamsStartAt ||
                     specialParamTypes.contains(paramTypes[i]))) {

                    if (SORT_PARAM_TYPES.contains(paramTypes[i]))
                        initDynamicSortPosition(i);
                } else if (type == QM_DELETE &&
                           compat.isSpecialParamValid(paramTypes[i], FIND_AND_DELETE)) {
                    throw exc(UnsupportedOperationException.class,
                              "CWWKD1097.param.incompat",
                              method.getName(),
                              repositoryInterface.getName(),
                              paramTypes[i].getSimpleName());
                } else {
                    throw Fail.specialParamIncompatible(this, paramTypes[i]);
                }
        }

        return specialParamsStartAt;
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
        return entityInfo.factory.provider.loggable(repositoryInterface,
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
    final String loggableAppend(String prefix, Object... possibleSuffix) {
        return entityInfo.factory.provider.loggableAppend(repositoryInterface,
                                                          method,
                                                          prefix,
                                                          possibleSuffix);
    }

    /**
     * Makes entities be managed by a persistence context.
     *
     * @param arg the entity or array/Iterable/Stream of entity
     * @param em  the entity manager
     * @return the managed entities, using the return type that is required by the
     *         Merge method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial
    Object merge(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "merge", loggable(arg));

        boolean resultVoid = void.class.equals(singleType) ||
                             Void.class.equals(singleType);
        ArrayList<Object> results;

        boolean hasSingularEntityParam = false;
        int count = 0;
        if (entityParamType.isArray()) {
            int length = Array.getLength(arg);
            results = resultVoid ? null : new ArrayList<>(length);
            for (; count < length; count++) {
                Object merged = em.merge(entityNotNull(Array.get(arg, count)));
                if (results != null)
                    results.add(merged);
            }
        } else if (arg instanceof Iterable) {
            results = resultVoid ? null : new ArrayList<>();
            for (Object e : ((Iterable<?>) arg)) {
                count++;
                Object merged = em.merge(entityNotNull(e));
                if (results != null)
                    results.add(merged);
            }
        } else {
            count = 1;
            hasSingularEntityParam = true;
            results = resultVoid ? null : new ArrayList<>(1);
            Object merged = em.merge(entityNotNull(arg));
            if (results != null)
                results.add(merged);
        }

        if (count == 0)
            throw Fail.emptyLifeCycleParam(this);

        Class<?> returnType = method.getReturnType();
        Object returnValue;
        if (resultVoid) {
            returnValue = null;
        } else {
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
                        throw Fail.resultSizeMismatch(this, "@Merge", results.size(),
                                                      hasSingularEntityParam);
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = convertToIterable(results, multiType, null, null);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw Fail.returnTypeInvalid(this, "Merge", hasSingularEntityParam,
                                                 null, results.get(0).getClass());
            }
        }

        if (CompletableFuture.class.equals(returnType) ||
            CompletionStage.class.equals(returnType)) {
            // useful for @Asynchronous
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw Fail.returnTypeInvalid(this, "Merge", hasSingularEntityParam,
                                         null, results.get(0).getClass());
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "merge", loggable(returnValue));
        return returnValue;
    }

    /**
     * Run a native query and return the result(s) if any.
     *
     * @param entityHandler EntityAgent or EntityManager (both are EntityHandler)
     * @param txStatus      transaction status
     * @param args          method parameters
     * @return results, after wrapping in an Optional or CompletionStage if
     *         required by the repository method signature
     * @throws Exception if an error occurs
     */
    @Trivial // eh, txStatus, and method args have already been logged if loggable
    Object nativeQuery(AutoCloseable entityHandler,
                       int txStatus,
                       Object... args) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "nativeQuery");

        QueryCustomization qc = QueryCustomization.from(this, args);
        PageRequest pageReq = qc.pageRequest();
        Object returnValue;

        if (CursoredPage.class.equals(multiType)) {
            throw new UnsupportedOperationException(); // TODO
        } else if (Page.class.equals(multiType)) {
            throw new UnsupportedOperationException(); // TODO
        } else if (pageReq != null &&
                   !PageRequest.Mode.OFFSET.equals(pageReq.mode())) {
            throw Fail.pageModeIncompatible(this, pageReq);
        } else {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "nativeQuery",
                         ql,
                         entityInfo.entityClass.getName());

            // TODO how do you know if it is a query or update?
            // The following generalization is not precise enough:
            int max = qc.maxResults();
            boolean mightHaveUpdateCount = max == 0 && // not specified
                                           multiType == null &&
                                           Util.RETURN_TYPES_FOR_UPDATE_COUNT //
                                                           .contains(singleType)
                                           &&
                                           !ql.trim().regionMatches(true, 0, "SELECT", 0, 6);

            jakarta.persistence.Query query;
            if (mightHaveUpdateCount) {
                query = ehCreateNativeStatement(entityHandler);

                setParameters(query, args, Collections.emptyMap(), null);

                returnValue = toReturnValue(query.executeUpdate(),
                                            singleType);
            } else {
                query = ehCreateNativeQuery(entityHandler);

                Limit limit = qc.limit();
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

                setParameters(query, args, Collections.emptyMap(), null);

                returnValue = getQueryResults(entityHandler, qc, query, txStatus);
            }
        }

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
            Tr.exit(this, tc, "nativeQuery", loggable(returnValue));
        return returnValue;
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
            throw Fail.emptyResult(this);
        else
            throw Fail.nonUniqueResult(this, results.size());
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
        int i = start;
        int num = start == endBefore ? 1 : 0;
        if (num == 0)
            while (i < endBefore) {
                char ch = methodName.charAt(i);
                if (ch >= '0' && ch <= '9') {
                    if (num <= (Integer.MAX_VALUE - (ch - '0')) / 10)
                        num = num * 10 + (ch - '0');
                    else
                        throw exc(UnsupportedOperationException.class,
                                  "CWWKD1028.first.exceeds.max",
                                  methodName,
                                  repositoryInterface.getName(),
                                  methodName.substring(start, endBefore),
                                  "Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
                    i++;
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
        else if (maxResults == 0)
            maxResults = num;
        else
            // TODO 1.1 NLS
            throw new UnsupportedOperationException("The " + methodName +
                                                    " method of the " +
                                                    repositoryInterface.getName() +
                                                    " repository interface cannot" +
                                                    " be annotated with the First" +
                                                    " annotation because its method" +
                                                    " name contains the First keyword.");

        return i;
    }

    /**
     * Finds the identification variable if any that follows
     * FROM EntityName
     *
     * @param startAt   position at which to start.
     * @param endBefore position before which to end.
     * @param ql        query language string.
     * @return identification variable name, if any. Otherwise this.
     */
    private String parseIdentificationVariable(int startAt,
                                               int endBefore,
                                               String ql) {
        String entityVar = "this";
        for (; startAt < endBefore &&
               Character.isWhitespace(ql.charAt(startAt)); startAt++);
        if (startAt < endBefore) {
            int idVar0 = startAt, idVarLen = 0; // starts at the entity identifier variable
            for (; startAt < endBefore &&
                   Character.isJavaIdentifierPart(ql.charAt(startAt)); startAt++);
            if ((idVarLen = startAt - idVar0) > 0) {
                if (idVarLen == 2
                    && (ql.charAt(idVar0) == 'A' || ql.charAt(idVar0) == 'a')
                    && (ql.charAt(idVar0 + 1) == 'S' || ql.charAt(idVar0 + 1) == 's')) {
                    // skip over the AS keyword
                    for (; startAt < endBefore &&
                           Character.isWhitespace(ql.charAt(startAt)); startAt++);
                    idVar0 = startAt;
                    for (; startAt < endBefore &&
                           Character.isJavaIdentifierPart(ql.charAt(startAt)); startAt++);
                }

                if (startAt > idVar0) {
                    String s = ql.substring(idVar0, startAt);
                    if (!Util.QL_KEYWORDS_AFTER_ENTITY_NAME.contains(s.toUpperCase()))
                        entityVar = s;
                }
            }
        }

        return entityVar;
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
            if (ignoreCase = endsWith(IgnoreCase.name(), methodName, i, endBefore))
                endBefore -= 10;

            String attribute = methodName.substring(i, endBefore);

            if (attribute.length() == 0) {
                // Error handling for missing attribute name due to Asc or Desc
                // appearing within an attribute name that is used in the OrderBy
                String lowerOrderBy = methodName.substring(orderBy + 7).toLowerCase();
                for (String lowerAttrName : entityInfo.attributeNames.keySet()) {
                    String keyword = lowerAttrName.contains("asc") ? "Asc" //
                                    : lowerAttrName.contains("desc") ? "Desc" //
                                                    : null;
                    if (keyword != null && lowerOrderBy.contains(lowerAttrName))
                        throw exc(MappingException.class,
                                  "CWWKD1105.keyword.in.orderby",
                                  methodName,
                                  repositoryInterface.getName(),
                                  entityInfo.attributeNames.get(lowerAttrName),
                                  entityInfo.getType().getName(),
                                  keyword);
                }
            }

            String name = getAttributeName(attribute, true);
            sorts.add(new Sort<>(name, !descending, ignoreCase));

            if (iNext > 0)
                iNext += (iNext == desc ? 4 : 3);
        }

        if (sortPositions == NONE && !sorts.isEmpty()) {
            sortPositions = NONE_STATIC_SORT_ONLY;
            generateOrderBy(q);
        }
    }

    /**
     * Locate the starting index of FROM keywords past the specified point.
     * There can be multiple due to subqueries and UNION/INTERSECT.
     * Locate the names of named parameters after the specified point in the query
     * and populate them into the paramNames list.
     *
     * This method relies on the query type being one of: FIND, QM_DELETE, QM_UPDATE
     *
     * @param ql                 query language
     * @param startAt            starting position in the query language
     * @param startsWithSelect   indicates whether or not a find query begins
     *                               with SELECT. False if a DELETE or UPDATE.
     * @param encloseWhereClause indicates if the WHERE clause must be enclosed in
     *                               parentheses so that conditions can be added
     *                               to it (for cursor pagination or Restriction).
     * @param entityInfos        map of entity name to entity information.
     * @param qlParamNames       list to populate with the names of named parameters.
     * @return indices at which the query needs to be modified, along with the type
     *         of modification needed
     */
    private TreeMap<Integer, QueryEdit> //
                    parseQuery(String ql,
                               final int startAt,
                               boolean startsWithSelect,
                               boolean encloseWhereClause,
                               Map<String, CompletableFuture<EntityInfo>> entityInfos,
                               LinkedHashSet<String> qlParamNames) {
        TreeMap<Integer, QueryEdit> modifyAt = new TreeMap<>();

        int length = ql.length();
        boolean hasTopLevelSelectClause = startsWithSelect;
        boolean isCursoredPage = CursoredPage.class.equals(multiType);
        boolean countPages = type == FIND &&
                             (isCursoredPage || Page.class.equals(multiType));
        int countReplacesFirstSelectAt = hasTopLevelSelectClause && countPages //
                        ? startAt // position after SELECT
                        : -1; // SELECT clause is not present
        int countReplacesFirstSelectEndingAt = -1;
        int numTopLevelFromClauses = 0;
        boolean initEntityVar = type != QM_UPDATE;
        boolean insertRecordConstructors = type == FIND &&
                                           producer.compat().atLeast(1, 1) &&
                                           singleType.isRecord();
        int insertConstructorBeginAt = hasTopLevelSelectClause && insertRecordConstructors //
                        ? parseSelectForConstructor(ql, startAt, modifyAt) //
                        : -1;
        // Conversion to a record requires at least 2 constructor args. Per the
        // Jakarta Data spec, "when the select list contains only one path expression,
        // the query directly returns the values of the path expression."
        int numPossibleConstructorArgs = insertConstructorBeginAt == -1 ? 0 : 1;

        Integer addFromAt = type == FIND //
                        ? null // unknown, check for FROM at depth 0 in query
                        : -1; // never, it's a DELETE or UPDATE so it always has FROM
        int encloseWhereBeginAt = -1;
        int encloseWhereEndAt = -1;
        int depth = 0; // depth of parentheses, to ignore EXTRACT(* FROM *) and subqueries
        boolean isLiteral = false;
        StringBuilder paramName = null;

        for (int i = startAt; i < length; i++) {
            char ch = ql.charAt(i);
            if (!isLiteral && ch == ':') {
                paramName = new StringBuilder(30);
            } else if (!isLiteral && ch == '(') {
                depth++;
            } else if (!isLiteral && ch == ')') {
                depth = depth > 0 ? depth - 1 : 0;
                if (i < encloseWhereEndAt)
                    encloseWhereEndAt = i;
            } else if (ch == '\'') {
                if (isLiteral) {
                    if (i + 1 < length && ql.charAt(i + 1) == '\'')
                        i++; // escaped ' within a literal
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
                if (paramName == null) {
                    if (i + 4 < length &&
                        !Character.isJavaIdentifierPart(ql.charAt(i + 4)) &&
                        ql.regionMatches(true, i, "FROM", 0, 4)) {

                        if (depth == 0) { // avoids EXTRACT(YEAR FROM d)
                            numTopLevelFromClauses++;
                            if (addFromAt == null) {
                                addFromAt = -1;
                            }
                            if (hasTopLevelSelectClause &&
                                countReplacesFirstSelectEndingAt < 0) {
                                countReplacesFirstSelectEndingAt = i;
                            }
                            if (numPossibleConstructorArgs > 0) {
                                if (numPossibleConstructorArgs == 1)
                                    modifyAt.remove(insertConstructorBeginAt);
                                else
                                    modifyAt.put(i - 1,
                                                 QueryEdit.ADD_CONSTRUCTOR_END);
                                numPossibleConstructorArgs = 0;
                            }
                        }

                        i += 4;
                        if (entityInfo == null || entityInfo.recordClass != null)
                            modifyAt.put(i + 1, QueryEdit.REPLACE_RECORD_ENTITY);

                        if (depth == 0 && initEntityVar) {
                            // determine the entity identification variable
                            while (i < length && Character.isWhitespace(ql.charAt(i)))
                                i++;
                            StringBuilder entityName = new StringBuilder();
                            for (char c; i < length && //
                                         Character.isJavaIdentifierPart(c = ql.charAt(i)); //
                                            i++)
                                entityName.append(c);
                            if (entityName.length() > 0)
                                setEntityInfo(entityName.toString(), entityInfos, ql);
                            else if (type != FIND) // a DELETE query
                                throw Fail.queryLacksEntityName(this, ql, "DELETE");

                            entityVar = parseIdentificationVariable(i, length, ql);
                            entityVar_ = entityVar + '.';
                            initEntityVar = false;
                        }
                        i--; // balances loop increment when already positioned correctly
                    } else if (depth == 0) {
                        boolean isSelect = false, isWhere = false, isOrder = false;
                        int l; // keyword length
                        if (i + (l = 5) < length &&
                            !Character.isJavaIdentifierPart(ql.charAt(i + l)) &&
                            ((isWhere = ql.regionMatches(true, i, "WHERE", 0, l)) ||
                             (isOrder = ql.regionMatches(true, i, "ORDER", 0, l)) ||
                             ql.regionMatches(true, i, "GROUP", 0, l) ||
                             ql.regionMatches(true, i, "UNION", 0, l))
                            ||
                            (i + (l = 6) < length &&
                             !Character.isJavaIdentifierPart(ql.charAt(i + l)) &&
                             ((isSelect = ql.regionMatches(true, i, "SELECT", 0, l)) ||
                              ql.regionMatches(true, i, "HAVING", 0, l) ||
                              ql.regionMatches(true, i, "EXCEPT", 0, l)))
                            ||
                            (i + (l = 9) < length &&
                             !Character.isJavaIdentifierPart(ql.charAt(i + l)) &&
                             ql.regionMatches(true, i, "INTERSECT", 0, l))) {

                            if (isOrder)
                                restrictAt = i;
                            if (isCursoredPage && !isSelect && !isWhere && !isOrder)
                                // ORDER BY and SELECT positioned after WHERE are
                                // also incompatible but are handled better elsewhere
                                throw Fail.queryIncompatipleWithCursor(this, ql, i, l);
                            if (hasTopLevelSelectClause &&
                                countReplacesFirstSelectEndingAt < 0) {
                                countReplacesFirstSelectEndingAt = i;
                            }
                            if (numPossibleConstructorArgs > 0) {
                                if (numPossibleConstructorArgs == 1)
                                    modifyAt.remove(insertConstructorBeginAt);
                                else
                                    modifyAt.put(i - 1, // avoid possible collision with ADD_FROM
                                                 QueryEdit.ADD_CONSTRUCTOR_END);
                                numPossibleConstructorArgs = 0;
                            }
                            if (encloseWhereBeginAt > 0) {
                                if (isCursoredPage)
                                    throw Fail.cursorQueryIncompat(this, ql, i, isOrder);
                                int p = i - 1;
                                while (p > 0 && Character.isWhitespace(ql.charAt(p)))
                                    p--;
                                if (encloseWhereEndAt != p) {
                                    modifyAt.put(encloseWhereBeginAt,
                                                 QueryEdit.ADD_PARENTHESIS_BEGIN);
                                    modifyAt.put(p + 1,
                                                 QueryEdit.ADD_PARENTHESIS_END);
                                } // else it is already enclosed in parentheses
                                encloseWhereBeginAt = -1;
                                encloseWhereEndAt = -1;
                            }
                            if (addFromAt == null)
                                addFromAt = isSelect ? 0 : i;
                            i += l;
                            if (isWhere) {
                                hasWhere = true;
                                if (encloseWhereClause) {
                                    while (i < length && Character.isWhitespace(ql.charAt(i)))
                                        i++;
                                    if (i < length) {
                                        encloseWhereBeginAt = i;
                                        encloseWhereEndAt = ql.charAt(i) == '(' //
                                                        ? length // adjusts for next )
                                                        : -1;
                                    }
                                }
                            } else if (isSelect) {
                                hasTopLevelSelectClause = true;

                                if (insertRecordConstructors) {
                                    insertConstructorBeginAt = hasTopLevelSelectClause && insertRecordConstructors //
                                                    ? parseSelectForConstructor(ql, i, modifyAt) //
                                                    : -1;
                                    numPossibleConstructorArgs = //
                                                    insertConstructorBeginAt == -1 ? 0 : 1;
                                }

                                if (countReplacesFirstSelectAt < 0)
                                    countReplacesFirstSelectAt = i;
                            } else if (isOrder) {
                                if (countPages)
                                    modifyAt.put(-i, // avoid possible collision
                                                 QueryEdit.OMIT_ORDER_IN_COUNT);
                            } else {
                                if (jpqlCount == null)
                                    // indicates that the keyword prevents computing a count
                                    jpqlCount = ql.substring(i - l, i);
                            }
                            i--; // balances loop increment when already positioned correctly
                        }
                    }
                } else {
                    paramName.append(ch);
                    while (length > i + 1 && Character //
                                    .isJavaIdentifierPart(ch = ql.charAt(i + 1))) {
                        paramName.append(ch);
                        i++;
                    }
                }
            } else {
                if (depth == 0 && !isLiteral && ch == ',' && numPossibleConstructorArgs > 0)
                    numPossibleConstructorArgs++;
                if (paramName != null) {
                    qlParamNames.add(paramName.toString());
                    paramName = null;
                }
            }
        }

        if (initEntityVar) {
            entityVar = THIS;
            entityVar_ = THIS + ".";
        }

        if (paramName != null)
            qlParamNames.add(paramName.toString());

        if (countPages && countReplacesFirstSelectAt >= 0) {
            modifyAt.put(countReplacesFirstSelectAt,
                         QueryEdit.REPLACE_SELECT_IN_COUNT_BEGIN);
            if (countReplacesFirstSelectEndingAt < 0)
                countReplacesFirstSelectEndingAt = length;
            modifyAt.put(-countReplacesFirstSelectEndingAt, // avoid possible collision
                         QueryEdit.REPLACE_SELECT_IN_COUNT_END);
        }

        if (type == FIND && !hasTopLevelSelectClause)
            modifyAt.put(QueryEdit.BEFORE_QUERY,
                         QueryEdit.ADD_SELECT_IF_NEEDED);

        if (addFromAt == null)
            if (startsWithSelect)
                addFromAt = length;
            else if (startAt < length && ql.charAt(startAt) == '(')
                addFromAt = -1; // not a JDQL query
            else
                addFromAt = 0;

        if (addFromAt != -1)
            modifyAt.put(addFromAt,
                         QueryEdit.ADD_FROM);

        if (numPossibleConstructorArgs > 0) {
            if (numPossibleConstructorArgs == 1)
                modifyAt.remove(insertConstructorBeginAt);
            else
                modifyAt.put(length - 1, // avoid possible collision with ADD_FROM
                             QueryEdit.ADD_CONSTRUCTOR_END);
        }

        if (encloseWhereBeginAt > 0) {
            int p = length - 1;
            while (p > 0 && Character.isWhitespace(ql.charAt(p)))
                p--;
            if (encloseWhereEndAt != p) {
                modifyAt.put(encloseWhereBeginAt,
                             QueryEdit.ADD_PARENTHESIS_BEGIN);
                modifyAt.put(p + 1,
                             QueryEdit.ADD_PARENTHESIS_END);
            }
        }

        return modifyAt;
    }

    /**
     * Inspects the beginning of a SELECT clause to determine if constructor
     * syntax (NEW) is present. If not present, add an instruction to insert it.
     *
     * @param ql       the query.
     * @param i        position in the query after SELECT.
     * @param modifyAt indices at which to perform modifications.
     * @return position in the query at which to insert constructor syntax.
     *         -1 if the constructor syntax should not be inserted.
     */
    @Trivial
    private int parseSelectForConstructor(String ql,
                                          int i,
                                          Map<Integer, QueryEdit> modifyAt) {
        int insertConstructorBeginAt = -1;
        int length = ql.length();

        while (i < length && Character.isWhitespace(ql.charAt(i)))
            i++;

        if (i + 3 < length &&
            !Character.isJavaIdentifierPart(ql.charAt(i + 3)) &&
            ql.regionMatches(true, i, "NEW", 0, 3)) {
            // already has constructor syntax
            i += 3;
        } else {
            modifyAt.put(i, QueryEdit.ADD_CONSTRUCTOR_BEGIN);
            insertConstructorBeginAt = i;
        }

        return insertConstructorBeginAt;
    }

    /**
     * Adds entities to the persistence context to be inserted in to the database.
     *
     * @param arg the entity or array/Iterable/Stream of entity
     * @param em  the entity manager
     * @throws Exception if an error occurs.
     */
    @Trivial
    Void persist(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "persist", loggable(arg));

        int count = 0;
        if (entityParamType.isArray()) {
            int length = Array.getLength(arg);
            for (; count < length; count++)
                em.persist(entityNotNull(Array.get(arg, count)));
        } else if (arg instanceof Iterable) {
            for (Object entity : ((Iterable<?>) arg)) {
                em.persist(entityNotNull(entity));
                count++;
            }
        } else {
            em.persist(entityNotNull(arg));
            count = 1;
        }

        if (count == 0)
            throw Fail.emptyLifeCycleParam(this);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "persist", count);
        return null;
    }

    /**
     * Refreshes the state of managed entities from the database.
     *
     * @param arg the entity or array/Iterable/Stream of entity
     * @param em  the entity manager
     * @throws Exception if an error occurs.
     */
    @Trivial
    Void refresh(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "refresh", loggable(arg));

        int count = 0;
        if (arg instanceof Iterable) {
            for (Object entity : ((Iterable<?>) arg)) {
                em.refresh(entityNotNull(entity));
                count++;
            }
        } else if (entityParamType.isArray()) {
            int length = Array.getLength(arg);
            for (; count < length; count++)
                em.refresh(entityNotNull(Array.get(arg, count)));
        } else {
            em.refresh(entityNotNull(arg));
            count++;
        }

        if (count == 0)
            throw Fail.emptyLifeCycleParam(this);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "refresh");
        return null;
    }

    /**
     * Requests the removal of entities from the database.
     * Removal might be delayed by the persistence context.
     *
     * @param arg the entity or array/Iterable/Stream of entities.
     * @param em  the entity manager
     * @throws Exception if an error occurs.
     */
    @Trivial
    Void remove(Object arg, EntityManager em) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "remove", loggable(arg));

        int removalsRequested = 0;

        if (arg instanceof Iterable) {
            for (Object e : ((Iterable<?>) arg)) {
                removalsRequested++;
                em.remove(entityNotNull(e));
            }
        } else if (entityParamType.isArray()) {
            removalsRequested = Array.getLength(arg);
            for (int i = 0; i < removalsRequested; i++)
                em.remove(entityNotNull(Array.get(arg, i)));
        } else {
            removalsRequested = 1;
            em.remove(entityNotNull(arg));
        }

        if (removalsRequested == 0)
            throw Fail.emptyLifeCycleParam(this);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "remove");
        return null;
    }

    /**
     * Replaces the given query with one that includes the requested modifications.
     *
     * @param ql       the query.
     * @param modifyAt indices at which to perform modifications.
     * @return a query that contains the requested modifications.
     */
    private String replaceQuery(String ql,
                                TreeMap<Integer, QueryEdit> modifyAt) {

        final String recordName = entityInfo.recordClass == null //
                        ? null //
                        : entityInfo.recordClass.getSimpleName();
        final int rLen = recordName == null ? 0 : recordName.length();
        final int qlLen = ql.length();

        // for editing the main query
        StringBuilder q = new StringBuilder(10 * modifyAt.size() +
                                            (modifyAt.firstKey() <= 0 ? 100 : 0) +
                                            qlLen);
        int qStartAt = 0; // index into the original query (ql)

        // for generating the count query
        StringBuilder c = jpqlCount == null &&
                          (Page.class.equals(multiType) ||
                           CursoredPage.class.equals(multiType)) //
                                           ? new StringBuilder(qlLen + 50) //
                                           : null;
        int cStartAt = 0; // index into the original query (ql)
        int cEndAt = qlLen;
        int cSelectClauseEndAt = -1;

        for (Entry<Integer, QueryEdit> mod : modifyAt.entrySet()) {
            int m = mod.getKey();
            switch (mod.getValue()) {
                case REPLACE_SELECT_IN_COUNT_END:
                    cSelectClauseEndAt = -m; // position at end of SELECT clause
                    break;
                case REPLACE_SELECT_IN_COUNT_BEGIN:
                    if (c != null) {
                        c.append(ql.substring(cStartAt, cStartAt = m)); // SELECT
                        c.append(" COUNT(");
                        int selectItemsLength = cSelectClauseEndAt - cStartAt;
                        c.append(inferCountFromSelect(ql,
                                                      cStartAt,
                                                      selectItemsLength));
                        c.append(") ");
                        cStartAt = cSelectClauseEndAt;
                    }
                    break;
                case OMIT_ORDER_IN_COUNT:
                    if (c != null)
                        cEndAt = -m - 5; // start of ORDER BY
                    break;
                case ADD_SELECT_IF_NEEDED:
                    // generateSelectClause determines if a SELECT clause is needed
                    q.append(generateSelectClause()).append(' ');

                    if (c != null)
                        c.append("SELECT COUNT(").append(entityVar).append(") ");
                    break;
                case ADD_CONSTRUCTOR_BEGIN:
                    q.append(ql.substring(qStartAt, qStartAt = m));
                    if (!Character.isWhitespace(ql.charAt(m - 1)))
                        q.append(' ');
                    q.append("NEW ").append(singleType.getName()).append('(');
                    break;
                case ADD_CONSTRUCTOR_END:
                    q.append(ql.substring(qStartAt, qStartAt = m));
                    char next = ql.charAt(m);
                    qStartAt = ++m;
                    if (Character.isWhitespace(next))
                        q.append(')').append(next);
                    else
                        q.append(next).append(") ");
                    break;
                case ADD_FROM:
                    q.append(ql.substring(qStartAt, qStartAt = cStartAt = m));
                    if (m > 0 && !Character.isWhitespace(ql.charAt(m - 1)))
                        q.append(' ');

                    q.append("FROM ").append(entityInfo.name);
                    if (c != null)
                        c.append("FROM ").append(entityInfo.name);

                    if (entityVar != THIS) {
                        q.append(' ').append(entityVar);
                        if (c != null)
                            c.append(' ').append(entityVar);
                    }

                    if (m < qlLen && !Character.isWhitespace(ql.charAt(m))) {
                        q.append(' ');
                        if (c != null)
                            c.append(' ');
                    }
                    break;
                case ADD_PARENTHESIS_BEGIN:
                    q.append(ql.substring(qStartAt, qStartAt = m));
                    if (m < qlLen && ql.charAt(m) == ' ') {
                        qStartAt = ++m;
                        q.append(' ');
                    }
                    q.append('(');
                    break;
                case ADD_PARENTHESIS_END:
                    q.append(ql.substring(qStartAt, (qStartAt = m) - 1));
                    char last = ql.charAt(m - 1);
                    if (Character.isWhitespace(last))
                        q.append(')').append(last);
                    else
                        q.append(last).append(") ");
                    break;
                case REPLACE_RECORD_ENTITY:
                    if (rLen > 0) { // has a record entity to replace

                        q.append(ql.substring(qStartAt, m));
                        if (c != null)
                            c.append(ql.substring(cStartAt, cEndAt < m ? cEndAt : m));

                        for (char ch; m < qlLen &&
                                      !Character.isJavaIdentifierPart(ch = ql.charAt(m)); //
                                        m++) {
                            q.append(ch);
                            if (c != null && m < cEndAt)
                                c.append(ch);
                        }

                        if ((m + rLen == qlLen // exactly long enough to have RecordName
                             || m + rLen < qlLen // more than long enough and next char must delimit
                                && !Character.isJavaIdentifierPart(ql.charAt(m + rLen)))
                            && ql.regionMatches(false, m, recordName, 0, rLen)) {
                            m += rLen;
                            q.append(entityInfo.name);
                            if (c != null && m <= cEndAt)
                                c.append(entityInfo.name);
                        }

                        qStartAt = m;
                        cStartAt = m < cEndAt ? m : cEndAt;
                    }
                    break;
                default:
                    throw new IllegalArgumentException(mod.getValue().toString());
            }
        }

        if (qStartAt < qlLen)
            q.append(ql.substring(qStartAt));

        if (c != null) {
            if (cStartAt < cEndAt)
                c.append(ql.substring(cStartAt, cEndAt));

            jpqlCount = c.toString();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, ql, "count query: " + jpqlCount);
        }

        return q.toString();
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
                    Sort.class.equals(paramTypes[p]) ||
                    Sort[].class.equals(paramTypes[p])) {
                    String paramTypeName = Sort[].class.equals(paramTypes[p]) //
                                    ? (Sort.class.getName() + "[]") //
                                    : paramTypes[p].getName();
                    if (args[p] == null)
                        // BasicRepository.findAll(PageRequest, Order) requires
                        // NullPointerException when Order is null.
                        throw Fail.nullMethodParameter(this, p);
                    else
                        throw exc(IllegalArgumentException.class,
                                  "CWWKD1088.empty.sorts",
                                  paramTypeName,
                                  method.getName(),
                                  repositoryInterface.getName());
                }
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
     * @param arg           the entity or record, or array/Iterable/Stream
     *                          of entity or record
     * @param entityHandler the EntityAgent or EntityManager
     * @return the updated entities, using the return type that is required by the
     *         repository Save method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial // avoid logging customer data
    Object save(Object arg, AutoCloseable entityHandler) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
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
                // workaround is not possible when multiple entities
                results.add(ehUpsert(entityHandler,
                                     toEntity(Array.get(arg, entityCount))));
        } else if (Iterable.class.isAssignableFrom(entityParamType)) {
            results = new ArrayList<>();
            for (Object e : ((Iterable<?>) arg)) {
                entityCount++;
                // workaround is not possible when multiple entities
                results.add(ehUpsert(entityHandler,
                                     toEntity(e)));
            }
        } else {
            entityCount = 1;
            hasSingularEntityParam = true;
            results = resultVoid ? null : new ArrayList<>(1);
            Object entity = ehUpsert(entityHandler,
                                     toEntity(arg));
            if (results != null)
                results.add(entity);
        }

        if (entityCount == 0)
            throw Fail.emptyLifeCycleParam(this);

        if (entityHandler instanceof EntityManager em) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "flush");
            em.flush();
        }

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
                        throw Fail.resultSizeMismatch(this, "@Save", results.size(),
                                                      hasSingularEntityParam);
                else if (multiType.isInstance(results))
                    returnValue = results;
                else if (Stream.class.equals(multiType))
                    returnValue = results.stream();
                else if (Iterable.class.isAssignableFrom(multiType))
                    returnValue = convertToIterable(results, multiType, null, null);
                else if (Iterator.class.equals(multiType))
                    returnValue = results.iterator();
                else
                    throw Fail.returnTypeInvalid(this, "Save", hasSingularEntityParam,
                                                 null, results.get(0).getClass());
            }
        }

        if (CompletableFuture.class.equals(returnType) ||
            CompletionStage.class.equals(returnType)) {
            // useful for @Asynchronous
            returnValue = CompletableFuture.completedFuture(returnValue);
        } else if (!resultVoid && !returnType.isInstance(returnValue)) {
            throw Fail.returnTypeInvalid(this, "Save", hasSingularEntityParam,
                                         null, results.get(0).getClass());
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
                              Util.lifeCycleAnnoNames(producer),
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
     * @param query               the query
     * @param args                repository method arguments
     * @param deferredConstraints map of method parameter index to non-Literal
     *                                Constraints that are supplied at execution time.
     * @param addedJPQLParams     map of JPQL parameter names/indices and values
     *                                for repository method special parameters.
     */
    @Trivial // avoid logging customer data
    void setParameters(jakarta.persistence.Query query,
                       Object[] args,
                       Map<Integer, Object> deferredConstraints,
                       Map<Object, Object> addedJPQLParams) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        final int numArgs = args == null ? 0 : args.length;

        if (trace && tc.isDebugEnabled()) {
            Object addedLoggable = loggable(addedJPQLParams);
            Tr.debug(this, tc, "setParameters",
                     numArgs + " method args",
                     "first special param at 0-based index " + specialParamsStartAt,
                     jpqlParamNames,
                     addedLoggable == addedJPQLParams //
                                     ? addedLoggable //
                                     : addedJPQLParams.keySet());
        }

        if (jpqlParamNames.isEmpty()) { // positional parameters
            int paramNum = 1;
            for (int a = 0; a < specialParamsStartAt; a++) {
                Object value;
                while (addedJPQLParams != null &&
                       (value = addedJPQLParams.getOrDefault(paramNum, NONE)) != NONE) {
                    // Positional parameter generated at execution time from an
                    // Expression within a Restriction or Constraint
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "[X] set ?" + paramNum + ' ' + loggable(value));
                    query.setParameter(paramNum++, value);
                }
                if (deferredConstraints.containsKey(a))
                    // Handled above: Constraint with non-Literal Expression
                    // supplied at execution time
                    continue;
                value = args[a];
                Object[] constraintValues = toConstraintValues(value);
                if (constraintValues == null) { // Normal positional parameter
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "[M] set ?" + paramNum + ' ' + loggable(value));
                    query.setParameter(paramNum++, value);
                } else { // Literal Expression from a Constraint
                    for (Object cvalue : constraintValues) {
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "[L] set ?" + paramNum + ' ' + loggable(cvalue));
                        query.setParameter(paramNum++, cvalue);
                    }
                }
            }
            // Additional generated positional parameters (might be for cursor pagination)
            for (Object value; addedJPQLParams != null &&
                               (value = addedJPQLParams.getOrDefault(paramNum, NONE)) != NONE;) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "[a] set ?" + paramNum + ' ' + loggable(value));
                query.setParameter(paramNum++, value);
            }
        } else { // named parameters
            // Named parameters are only available when the repository uses a
            // query annotation to supply the query directly in query language.
            // In this case, Constraint typed parameters will not be allowed.
            Iterator<String> paramNames = jpqlParamNames.iterator();
            for (int a = 0; a < specialParamsStartAt; a++) {
                if (!paramNames.hasNext())
                    throw Fail.extraMethodParams(this, a + 1, specialParamsStartAt + 1);
                String paramName = paramNames.next();
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "[m] set :" + paramName + ' ' + loggable(args[a]));
                query.setParameter(paramName, args[a]);
            }
            // Additional generated positional parameters (might be for cursor pagination)
            if (addedJPQLParams != null)
                for (Entry<Object, Object> entry : addedJPQLParams.entrySet()) {
                    String paramName = (String) entry.getKey();
                    Object value = entry.getValue();
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "[a] set :" + paramName + ' ' + loggable(value));
                    query.setParameter(paramName, value);
                }
        }
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
    protected void setType(Class<? extends Annotation> annoClass,
                           QueryType operationType) {
        type = operationType;
        if (entityParamType == null) {
            int paramCount = method.getParameterCount();
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1009.lifecycle.param.err",
                      method.getName(),
                      repositoryInterface.getName(),
                      paramCount == 1 ? method.getGenericParameterTypes()[0].getTypeName() //
                                      : paramCount,
                      annoClass.getSimpleName());
        }
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
    List<Sort<Object>> supplySorts(List<Sort<Object>> combined,
                                   Iterable<Sort<Object>> additional) {
        Iterator<Sort<Object>> addIt = additional.iterator();
        boolean hasIdClass = entityInfo.idClassAttributeAccessors != null;
        if (combined == null && addIt.hasNext())
            combined = sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
        while (addIt.hasNext()) {
            Sort<Object> sort = addIt.next();
            if (sort == null) {
                throw new IllegalArgumentException("Sort: null");
            } else if (hasIdClass && ID.equalsIgnoreCase(sort.property())) {
                // IdClass is split up so that it can be possible to create a cursor
                // that corresponds to sort criteria
                for (String name : entityInfo.idClassAttributeAccessors.keySet()) {
                    name = getAttributeName(name, true);
                    sort = name == sort.property() ? sort : createSort(name, sort);
                    combined.add(sort);
                }
            } else {
                String name = getAttributeName(sort.property(), true);
                sort = name == sort.property() ? sort : createSort(name, sort);
                combined.add(sort);
            }
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
    List<Sort<Object>> supplySorts(List<Sort<Object>> combined,
                                   @SuppressWarnings("unchecked") Sort<Object>... additional) {
        boolean hasIdClass = entityInfo.idClassAttributeAccessors != null;
        if (combined == null && additional.length > 0)
            combined = sorts == null ? new ArrayList<>() : new ArrayList<>(sorts);
        for (Sort<Object> sort : additional) {
            if (sort == null) {
                throw new IllegalArgumentException("Sort: null");
            } else if (hasIdClass && ID.equalsIgnoreCase(sort.property())) {
                // IdClass is split up so that it can be possible to create a cursor
                // that corresponds to sort criteria
                for (String name : entityInfo.idClassAttributeAccessors.keySet()) {
                    name = getAttributeName(name, true);
                    sort = name == sort.property() ? sort : createSort(name, sort);
                    combined.add(sort);
                }
            } else {
                String name = getAttributeName(sort.property(), true);
                sort = name == sort.property() ? sort : createSort(name, sort);
                combined.add(sort);
            }
        }
        return combined;
    }

    /**
     * Temporary method that obtains the literal value(s) from a constraint if the
     * supplied value is a constraint for a literal expression.
     * TODO 1.1 come up with better approach
     *
     * @param constraintOrValue a jakarta.data.constraint.Constraint subtype or a
     *                              literal value.
     * @return array of literal values obtained from the constraint.
     *         Null if not a constraint.
     */
    protected abstract Object[] toConstraintValues(Object constraintOrValue);

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
            throw Fail.entityNull(this);

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
                      type == QM_DELETE ? "Delete" : "Update");
        return result;
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder b = new StringBuilder(getClass().getSimpleName()).append('@') //
                        .append(Integer.toHexString(hashCode())).append(' ') //
                        .append(method.getGenericReturnType().getTypeName()).append(' ') //
                        .append(method.getName());
        boolean first = true;
        for (Class<?> p : method.getParameterTypes()) {
            b.append(first ? "(" : ", ").append(p.getSimpleName());
            first = false;
        }
        b.append(first ? "() " : ") ");
        if (ql != null)
            b.append(ql);
        if (qlParamCount > 0)
            b.append(" [").append(qlParamCount).append(jpqlParamNames.isEmpty() ? //
                            " positional params]" : //
                            " named params]");
        return b.toString();
    }

    /**
     * Updates entities (or records) in the database.
     * An error is raised if any of the entities (or records) are not found
     * in the database.
     *
     * @param arg           the entity or record, or array/Iterable/Stream
     *                          of entity or record
     * @param entityHandler the EntityAgent or EntityManager
     * @return count of matching entities, boolean indicator of whether any matched,
     *         or void return type that is required by the Update method signature.
     * @throws Exception if an error occurs.
     */
    @Trivial
    Object update(Object arg, AutoCloseable entityHandler) throws Exception {
        arg = arg instanceof Stream //
                        ? ((Stream<?>) arg).sequential().toList() //
                        : arg;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "update", loggable(arg));

        int updateCount = 0;
        int numExpected = 0;

        if (arg instanceof Iterable) {
            for (Object e : ((Iterable<?>) arg)) {
                numExpected++;
                updateCount += updateOne(e, entityHandler);
            }
        } else if (entityParamType.isArray()) {
            numExpected = Array.getLength(arg);
            for (int i = 0; i < numExpected; i++)
                updateCount += updateOne(Array.get(arg, i), entityHandler);
        } else {
            numExpected = 1;
            updateCount = updateOne(arg, entityHandler);
        }

        if (entityHandler instanceof EntityManager em) {
            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc, "flush");
            em.flush();
        }

        if (numExpected == 0)
            throw Fail.emptyLifeCycleParam(this);

        if (updateCount < numExpected)
            throw Fail.optimisticLockConflict(this, updateCount, numExpected);

        Object returnValue = toReturnValue(updateCount, method.getReturnType());

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "update", loggable(returnValue));
        return returnValue;
    }

    /**
     * Updates the entity (or record) from the database if its attributes match
     * the database.
     *
     * @param e             the entity or record
     * @param entityHandler the EntityAgent or EntityManager
     * @return the number of entities updated (1 or 0).
     * @throws Exception if an error occurs.
     */
    @Trivial
    private int updateOne(Object e, AutoCloseable entityHandler) throws Exception {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "updateOne", loggable(e));

        if (!entityInfo.getType().isInstance(e))
            throw Fail.entityMismatch(this, e);

        String jpql = this.ql;
        Set<String> attrsToUpdate = entityInfo.attributeNamesForEntityUpdate;

        Object id = null;
        String idAttributeName = null;
        if (entityInfo.idClassAttributeAccessors == null) {
            idAttributeName = entityInfo.attributeNames.get(ID);
            id = getAttribute(e, idAttributeName);
            if (id == null) {
                int idParamIndex = entityInfo.idClassAttributeAccessors == null //
                                ? (attrsToUpdate.size() + 1) //
                                : (attrsToUpdate.size() +
                                   entityInfo.idClassAttributeAccessors.size());

                jpql = jpql.replace("=?" + (idParamIndex - 1), " IS NULL");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && jpql != this.ql)
            Tr.debug(this, tc, "JPQL adjusted for NULL id", jpql);

        jakarta.persistence.Query update = ehCreateStatement(entityHandler, jpql);

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
        } else { // has IdClass
            setParametersFromIdClassAndVersion(p, update, e, null);
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
     *
     * @param validateNumberOfMethodArgs indicates whether to validate the
     *                                       number of repository method arguments
     *                                       versus the number of JPQL parameters.
     */
    @Trivial
    private void validate(boolean validateNumberOfMethodArgs) {
        if (type == null)
            throw Fail.unsupportedMethod(this);

        if (validateNumberOfMethodArgs &&
            ql != null &&
            method.getParameterCount() < qlParamCount &&
            type != LC_DELETE &&
            type != LC_UPDATE &&
            type != LC_UPDATE_MERGE)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1021.insufficient.params",
                      method.getName(),
                      repositoryInterface.getName(),
                      method.getParameterCount(),
                      qlParamCount,
                      ql);

        if (type == FIND &&
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

            throw Fail.returnTypeInvalid(this, "exists", false, "boolean, Boolean", null);
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

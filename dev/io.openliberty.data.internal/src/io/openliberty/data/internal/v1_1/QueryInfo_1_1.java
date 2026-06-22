/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.data.internal.v1_1;

import static io.openliberty.data.internal.QueryType.DETACH;
import static io.openliberty.data.internal.QueryType.INSERT;
import static io.openliberty.data.internal.QueryType.LC_DELETE;
import static io.openliberty.data.internal.QueryType.LC_UPDATE;
import static io.openliberty.data.internal.QueryType.LC_UPDATE_MERGE;
import static io.openliberty.data.internal.QueryType.MERGE;
import static io.openliberty.data.internal.QueryType.NATIVE;
import static io.openliberty.data.internal.QueryType.PERSIST;
import static io.openliberty.data.internal.QueryType.REFRESH;
import static io.openliberty.data.internal.QueryType.REMOVE;
import static io.openliberty.data.internal.QueryType.SAVE;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.AttributeConstraint;
import io.openliberty.data.internal.QueryInfo;
import io.openliberty.data.internal.QueryType;
import io.openliberty.data.internal.Util;
import io.openliberty.data.internal.cdi.RepositoryProducer;
import io.openliberty.data.repository.IgnoreCase;
import io.openliberty.data.repository.function.AbsoluteValue;
import io.openliberty.data.repository.function.CharCount;
import io.openliberty.data.repository.function.ElementCount;
import io.openliberty.data.repository.function.Extract;
import io.openliberty.data.repository.function.Rounded;
import io.openliberty.data.repository.function.Trimmed;
import io.openliberty.data.repository.update.Add;
import io.openliberty.data.repository.update.Assign;
import io.openliberty.data.repository.update.Divide;
import io.openliberty.data.repository.update.Multiply;
import io.openliberty.data.repository.update.SubtractFrom;
import jakarta.data.Sort;
import jakarta.data.Sort.Nulls;
import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Between;
import jakarta.data.constraint.Constraint;
import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.In;
import jakarta.data.constraint.LessThan;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotBetween;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.constraint.NotIn;
import jakarta.data.constraint.NotLike;
import jakarta.data.constraint.NotNull;
import jakarta.data.constraint.Null;
import jakarta.data.exceptions.DataException;
import jakarta.data.expression.Expression;
import jakarta.data.expression.NavigableExpression;
import jakarta.data.expression.TemporalExpression;
import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.NavigableAttribute;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Is;
import jakarta.data.repository.JakartaQuery;
import jakarta.data.repository.NativeQuery;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.QueryOptions;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;
import jakarta.data.repository.stateful.Detach;
import jakarta.data.repository.stateful.Merge;
import jakarta.data.repository.stateful.Persist;
import jakarta.data.repository.stateful.Refresh;
import jakarta.data.repository.stateful.Remove;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.CompositeRestriction;
import jakarta.data.spi.expression.function.CurrentDate;
import jakarta.data.spi.expression.function.CurrentDateTime;
import jakarta.data.spi.expression.function.CurrentTime;
import jakarta.data.spi.expression.function.FunctionExpression;
import jakarta.data.spi.expression.function.NumericCast;
import jakarta.data.spi.expression.function.NumericFunctionExpression;
import jakarta.data.spi.expression.function.NumericOperatorExpression;
import jakarta.data.spi.expression.function.TextFunctionExpression;
import jakarta.data.spi.expression.literal.Literal;
import jakarta.data.spi.expression.path.NavigablePath;
import jakarta.data.spi.expression.path.Path;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.QueryHint;
import jakarta.persistence.TypedQuery;

/**
 * QueryInfo implementation for Jakarta Data 1.1.
 */
public class QueryInfo_1_1 extends QueryInfo {
    private static final TraceComponent tc = Tr.register(QueryInfo_1_1.class);

    private static final String FUNCTION_ANNO_PACKAGE = Rounded.class.getPackageName();

    private static final Map<String, String> FUNCTION_CALLS = new HashMap<>();
    static {
        FUNCTION_CALLS.put(AbsoluteValue.class.getSimpleName(), "ABS(");
        FUNCTION_CALLS.put(CharCount.class.getSimpleName(), "LENGTH(");
        FUNCTION_CALLS.put(ElementCount.class.getSimpleName(), "SIZE(");
        FUNCTION_CALLS.put(Rounded.Direction.DOWN.name(), "FLOOR(");
        FUNCTION_CALLS.put(Rounded.Direction.NEAREST.name(), "ROUND(");
        FUNCTION_CALLS.put(Rounded.Direction.UP.name(), "CEILING(");
        FUNCTION_CALLS.put(Trimmed.class.getSimpleName(), "TRIM(");
        FUNCTION_CALLS.put(Extract.Field.DAY.name(), "EXTRACT (DAY FROM ");
        FUNCTION_CALLS.put(Extract.Field.HOUR.name(), "EXTRACT (HOUR FROM ");
        FUNCTION_CALLS.put(Extract.Field.MINUTE.name(), "EXTRACT (MINUTE FROM ");
        FUNCTION_CALLS.put(Extract.Field.MONTH.name(), "EXTRACT (MONTH FROM ");
        FUNCTION_CALLS.put(Extract.Field.QUARTER.name(), "EXTRACT (QUARTER FROM ");
        FUNCTION_CALLS.put(Extract.Field.SECOND.name(), "EXTRACT (SECOND FROM ");
        FUNCTION_CALLS.put(Extract.Field.WEEK.name(), "EXTRACT (WEEK FROM ");
        FUNCTION_CALLS.put(Extract.Field.YEAR.name(), "EXTRACT (YEAR FROM ");
    }

    /**
     * Empty size 0 array that indicates no Constraint values.
     */
    private static final Object[] NO_VALUES = new Object[0];

    /**
     * Construct partially complete query information.
     *
     * @param repositoryProducer    producer of the repository bean instance.
     * @param repositoryInterface   interface annotated with @Repository.
     * @param method                repository method.
     * @param methodType            type of repository method, if known in advance.
     * @param methodTypeAnno        mutually exclusive repository method annotation
     *                                  (Find/Delete/...) if known in advance,
     *                                  in which case methodType must be supplied.
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
    QueryInfo_1_1(RepositoryProducer<?> repositoryProducer,
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
        super(repositoryProducer, //
              repositoryInterface, //
              method, //
              methodType, //
              methodTypeAnno, //
              entityParamType, //
              isOptional, //
              multiType, //
              returnArrayType, //
              singleType, //
              singleTypeElementType);
    }

    /**
     * Generate the name of a named parameter that supplies a value that is
     * represented as an Expression.
     *
     * @param jpqlParamCount parameter number to include in the generated name.
     * @param jpqlParamNames list of named parameter names to which to add the
     *                           generated name, which must not already be in the list.
     * @return
     */
    @Trivial
    private String addExpressionParam(int jpqlParamCount, Set<String> jpqlParamNames) {
        String paramName = "xpr" + jpqlParamCount;
        while (!jpqlParamNames.add(paramName))
            paramName += '_';
        return paramName;
    }

    @Override
    @Trivial
    protected StringBuilder appendConstraint(StringBuilder q,
                                             String o_,
                                             String attrName,
                                             AttributeConstraint constraint,
                                             int prevNumJPQLParams,
                                             boolean isCollection,
                                             Annotation[] annos) {
        StringBuilder attributeExpr = new StringBuilder();

        List<Annotation> functionAnnos = new ArrayList<>();
        boolean ignoreCase = false;
        for (int a = annos.length - 1; a >= 0; a--) {
            if (annos[a] instanceof IgnoreCase) {
                ignoreCase = true;
            } else {
                String annoPackage = annos[a].annotationType().getPackageName();
                if (FUNCTION_ANNO_PACKAGE.equals(annoPackage)) {
                    functionAnnos.add(annos[a]);
                    String functionType = annos[a] instanceof Extract //
                                    ? ((Extract) annos[a]).value().name() //
                                    : annos[a] instanceof Rounded //
                                                    ? ((Rounded) annos[a]).value().name() //
                                                    : annos[a].annotationType().getSimpleName();
                    String functionCall = FUNCTION_CALLS.get(functionType);
                    attributeExpr.append(functionCall);
                }
            }
        }

        boolean negated = constraint.isNegative();
        AttributeConstraint baseConstraint = negated //
                        ? constraint.negate() //
                        : constraint;

        if (ignoreCase)
            attributeExpr.append("LOWER(");

        if (attrName.charAt(attrName.length() - 1) != ')')
            attributeExpr.append(o_);

        attributeExpr.append(attrName);

        if (ignoreCase)
            attributeExpr.append(')');

        for (Annotation anno : functionAnnos) {
            if (anno instanceof Rounded && ((Rounded) anno).value() == Rounded.Direction.NEAREST)
                attributeExpr.append(", 0)"); // round to zero digits beyond the decimal
            else
                attributeExpr.append(')');
        }

        if (isCollection)
            if (ignoreCase ||
                baseConstraint != AttributeConstraint.Equal) // TODO also have an operation for collection containing?
                throw new UnsupportedOperationException("The " + constraint.constraintName() +
                                                        " constraint that is applied to entity attribute " +
                                                        attrName +
                                                        " is not supported for collection attributes."); // TODO NLS (future)

        switch (baseConstraint) {
            case Equal:
            case GreaterThan:
            case GreaterThanEqual:
            case LessThan:
            case LessThanEqual:
                q.append(attributeExpr).append(constraint.operator());
                appendParam(q, ignoreCase, prevNumJPQLParams + 1);
                break;
            case Between:
                q.append(attributeExpr).append(constraint.operator());
                appendParam(q, ignoreCase, prevNumJPQLParams + 1);
                q.append(" AND ");
                appendParam(q, ignoreCase, prevNumJPQLParams + 2);
                break;
            case In:
                if (ignoreCase)
                    throw new UnsupportedOperationException(); // should be unreachable
                q.append(attributeExpr).append(constraint.operator());
                appendParam(q, ignoreCase, prevNumJPQLParams + 1);
                break;
            // TODO 1.1: escape characters and custom wildcards
            case Like:
                q.append(attributeExpr).append(constraint.operator());
                appendParam(q, ignoreCase, prevNumJPQLParams + 1);
                break;
            case LikeEscaped:
                q.append(attributeExpr).append(constraint.operator());
                appendParam(q, ignoreCase, prevNumJPQLParams + 1);
                q.append(" ESCAPE ");
                appendParam(q, false, prevNumJPQLParams + 2);
                break;
            case Null:
                q.append(attributeExpr).append(constraint.operator());
                break;
            case Contains:
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" LIKE ('%' || ");
                appendParam(q, ignoreCase, prevNumJPQLParams + 1).append(" || '%')");
                break;
            case EndsWith:
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" LIKE ('%' || ");
                appendParam(q, ignoreCase, prevNumJPQLParams + 1).append(')');
                break;
            case StartsWith:
                q.append(attributeExpr) //
                                .append(negated ? " NOT" : "") //
                                .append(" LIKE (");
                appendParam(q, ignoreCase, prevNumJPQLParams + 1).append(" || '%')");
                break;
            // TODO operation for collection containing?
            //case ???:
            //    q.append(" ?").append(qp) //
            //                    .append(negated ? " NOT" : "") //
            //                    .append(" MEMBER OF ").append(attributeExpr);
            //    break;
            default:
                throw new UnsupportedOperationException(constraint.constraintName());
        }

        return q;
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

    @Override
    @Trivial
    protected <T> Sort<T> createSort(String expression, OrderBy orderBy) {
        return new Sort<T>( //
                        expression, //
                        !orderBy.descending(), //
                        orderBy.ignoreCase(), //
                        orderBy.nullOrdering());
    }

    @Override
    @Trivial
    protected <T> Sort<T> createSort(String expression, Sort<T> sort) {
        return new Sort<T>( //
                        expression, //
                        sort.isAscending(), //
                        sort.ignoreCase(), //
                        sort.nullOrdering());
    }

    @Override
    protected jakarta.persistence.Query //
                    ehCreateNativeQuery(AutoCloseable entityHandler) {
        // If the repository method return type is the entity class or multiple
        // entities, consider the entity class to be the result type. Otherwise
        // the result could be a count or single entity attribute.
        Class<?> resultClass = singleType != null &&
                               entityInfo.entityClass.isAssignableFrom(singleType) //
                                               ? entityInfo.entityClass //
                                               : entityInfo.isHibernate //
                                                               ? Object.class //
                                                               : null;

        // TODO Persistence 4.0 API
        //if (entityHandler instanceof EntityHandler handler) ...

        jakarta.persistence.Query query;
        if (entityHandler instanceof EntityManager em) {
            if (resultClass == null)
                query = em.createNativeQuery(ql);
            else
                query = em.createNativeQuery(ql, resultClass);
        } else {
            try {
                query = (jakarta.persistence.Query) entityHandler.getClass() //
                                .getMethod("createNativeQuery",
                                           String.class,
                                           Class.class) //
                                .invoke(entityHandler,
                                        ql,
                                        resultClass);
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new RuntimeException(x); // should be impossible
            } catch (InvocationTargetException x) {
                if (x.getCause() instanceof RuntimeException rx)
                    throw rx;
                throw new DataException(x.getCause());
            }
        }

        QueryOptions options = method.getAnnotation(QueryOptions.class);
        if (options != null)
            try {
                setReadOptions(options, query, entityHandler);
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new RuntimeException(x); // should be impossible
            } catch (InvocationTargetException x) {
                if (x.getCause() instanceof RuntimeException rx)
                    throw rx;
                throw new DataException(x.getCause());
            }

        return query;
    }

    @Override
    protected jakarta.persistence.Query //
                    ehCreateNativeStatement(AutoCloseable entityHandler) {
        jakarta.persistence.Query query;

        QueryOptions options = method.getAnnotation(QueryOptions.class);

        // TODO Persistence 4.0 API
        //if (entityHandler instanceof EntityHandler handler) ...
        //    handler.createNativeStatement(ql)

        if (entityHandler instanceof EntityManager em) {
            query = em.createNativeQuery(ql);
        } else {
            try {
                query = (jakarta.persistence.Query) entityHandler.getClass() //
                                .getMethod("createNativeMutationQuery", String.class) //
                                .invoke(entityHandler, ql);
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new RuntimeException(x); // should be impossible
            } catch (InvocationTargetException x) {
                if (x.getCause() instanceof RuntimeException rx)
                    throw rx;
                throw new DataException(x.getCause());
            }
        }

        if (options != null)
            setWriteOptions(options, query);

        return query;
    }

    @Override
    @Trivial
    protected jakarta.persistence.Query ehCreateStatement(AutoCloseable entityHandler,
                                                          String jpql) {
        QueryOptions options = type.supportsQueryOptions //
                        ? method.getAnnotation(QueryOptions.class) //
                        : null;

        jakarta.persistence.Query query;
        // TODO Persistence 4.0 API
        //query = entityHandler instanceof EntityHandler handler //
        //                ? handler.createStatement(jpql) //
        //                : ((EntityManager) entityHandler).createQuery(jpql);
        try {
            query = (jakarta.persistence.Query) entityHandler.getClass() //
                            .getMethod("createQuery", String.class) //
                            .invoke(entityHandler, jpql);
            if (options != null)
                setWriteOptions(options, query);
            return query;
        } catch (IllegalAccessException | NoSuchMethodException x) {
            throw new RuntimeException(x); // should be impossible
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException rx)
                throw rx;
            throw new DataException(x.getCause());
        }
    }

    @Override
    @Trivial
    protected <T> TypedQuery<T> ehCreateTypedQuery(AutoCloseable entityHandler,
                                                   String jpql,
                                                   Class<?> resultType) {
        QueryOptions options = type.supportsQueryOptions //
                        ? method.getAnnotation(QueryOptions.class) //
                        : null;

        // TODO Persistence 4.0 API
        //TypedQuery<T> query = entityHandler instanceof EntityHandler handler //
        //                ? handler.createQuery(jpql, resultType) //
        //                : ((EntityManager) entityHandler) //
        //                        .createQuery(jpql, resultType)
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) entityHandler.getClass() //
                            .getMethod("createQuery", String.class, Class.class) //
                            .invoke(entityHandler, jpql, resultType);
            if (options != null)
                setReadOptions(options, query, entityHandler);
            return query;
        } catch (IllegalAccessException | NoSuchMethodException x) {
            throw new RuntimeException(x); // should be impossible
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException rx)
                throw rx;
            throw new DataException(x.getCause());
        }
    }

    @FFDCIgnore(InvocationTargetException.class)
    @Override
    @Trivial
    protected void ehDelete(AutoCloseable entityHandler, Object entity) {
        // TODO Persistence 4.0 API
        // return entityHandler instanceof EntityAgent agent //
        //                ? agent.delete(entity) //
        //                : ((EntityManager) entityHandler).remove(entity);

        if (entityHandler instanceof EntityManager manager)
            manager.remove(entity);
        else
            try {
                entityHandler.getClass() //
                                .getMethod("delete", Object.class) //
                                .invoke(entityHandler, entity);
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new RuntimeException(x); // should be impossible
            } catch (InvocationTargetException x) {
                if (x.getCause() instanceof RuntimeException rx)
                    throw rx;
                throw new DataException(x.getCause());
            }
        // TODO deleteMultiple
    }

    @FFDCIgnore(InvocationTargetException.class)
    @Override
    @Trivial
    protected void ehInsert(AutoCloseable entityHandler, Object entity) {
        // TODO Persistence 4.0 API
        // return entityHandler instanceof EntityAgent agent //
        //                ? agent.insert(entity) //
        //                : ((EntityManager) entityHandler).persist(entity);

        if (entityHandler instanceof EntityManager manager)
            manager.persist(entity);
        else
            try {
                entityHandler.getClass() //
                                .getMethod("insert", Object.class) //
                                .invoke(entityHandler, entity);
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new RuntimeException(x); // should be impossible
            } catch (InvocationTargetException x) {
                if (x.getCause() instanceof RuntimeException rx)
                    throw rx;
                throw new DataException(x.getCause());
            }
        // TODO insertMultiple
    }

    @FFDCIgnore(InvocationTargetException.class)
    @Override
    @Trivial
    protected Object ehUpdate(AutoCloseable entityHandler, Object entity) {
        // TODO Persistence 4.0 API
        // return entityHandler instanceof EntityAgent agent //
        //                ? agent.update(entity) //
        //                : ((EntityManager) entityHandler).merge(entity);

        Object updated;
        if (entityHandler instanceof EntityManager manager)
            updated = manager.merge(entity);
        else
            try {
                entityHandler.getClass() //
                                .getMethod("update", Object.class) //
                                .invoke(entityHandler, entity);
                updated = entity;
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new RuntimeException(x); // should be impossible
            } catch (InvocationTargetException x) {
                if (x.getCause() instanceof RuntimeException rx)
                    throw rx;
                throw new DataException(x.getCause());
            }
        // TODO updateMultiple
        return updated;
    }

    @FFDCIgnore(InvocationTargetException.class)
    @Override
    @Trivial
    protected Object ehUpsert(AutoCloseable entityHandler, Object entity) {
        // TODO Persistence 4.0 API
        // return entityHandler instanceof EntityAgent agent //
        //                ? agent.upsert(entity) //
        //                : ((EntityManager) entityHandler).merge(entity);

        Object upserted;
        if (entityHandler instanceof EntityManager manager)
            upserted = manager.merge(entity);
        else
            try {
                entityHandler.getClass() //
                                .getMethod("upsert", Object.class) //
                                .invoke(entityHandler, entity);
                upserted = entity;
            } catch (IllegalAccessException | NoSuchMethodException x) {
                throw new RuntimeException(x); // should be impossible
            } catch (InvocationTargetException x) {
                if (x.getCause() instanceof RuntimeException rx)
                    throw rx;
                throw new DataException(x.getCause());
            }
        // TODO upsertMultiple
        return upserted;
    }

    /**
     * Appends JPQL to the partially built query to represent a Constraint.
     *
     * @param q              partially built query to which to append JPQL
     *                           representing the Constraint.
     * @param entityVar_     entity identifier variable name and . character.
     * @param constraint     the Constraint for which to generate JPQL.
     * @param jpqlParamCount number of named or positional parameters identified
     *                           up to this point for the JPQL.
     * @param jpqlParamNames names of named parameters in the partially built
     *                           query. Empty if the query uses positional
     *                           parameeters or has none. If using named parameters,
     *                           this method should add any that are generated.
     * @param jpqlParams     list for this method to populate with the name of
     *                           named parameters or index of positional parameters,
     *                           mapped to value, for each value obtained from the
     *                           processed Restriction(s).
     * @return the new count of named or positional parameters, including any that
     *         were generated for the Constraint.
     */
    @Override
    // TODO @Trivial // avoid tracing values found in Expression.toString()
    protected int generateConstraint(StringBuilder q,
                                     Object constraint,
                                     int jpqlParamCount,
                                     Set<String> jpqlParamNames,
                                     Map<Object, Object> jpqlParams) {

        Expression<?, ?> exp1 = null;
        Expression<?, ?> exp2 = null;
        List<Expression<?, ?>> exps = null;
        AttributeConstraint c = null;

        if (constraint instanceof AtLeast l) {
            c = AttributeConstraint.GreaterThanEqual;
            exp1 = l.bound();
        } else if (constraint instanceof AtMost m) {
            c = AttributeConstraint.LessThanEqual;
            exp1 = m.bound();
        } else if (constraint instanceof Between b) {
            c = AttributeConstraint.Between;
            exp1 = b.lowerBound();
            exp2 = b.upperBound();
        } else if (constraint instanceof GreaterThan g) {
            c = AttributeConstraint.GreaterThan;
            exp1 = g.bound();
        } else if (constraint instanceof EqualTo e) {
            c = AttributeConstraint.Equal;
            exp1 = e.expression();
        } else if (constraint instanceof In i) {
            c = AttributeConstraint.In;
            exps = i.expressions();
        } else if (constraint instanceof LessThan l) {
            c = AttributeConstraint.LessThan;
            exp1 = l.bound();
        } else if (constraint instanceof Like l) {
            c = AttributeConstraint.LikeEscaped;
            exp1 = l.pattern();
            exp2 = Literal.of(l.escape());
        } else if (constraint instanceof NotBetween nb) {
            c = AttributeConstraint.NotBetween;
            exp1 = nb.lowerBound();
            exp2 = nb.upperBound();
        } else if (constraint instanceof NotEqualTo ne) {
            c = AttributeConstraint.Not;
            exp1 = ne.expression();
        } else if (constraint instanceof NotIn ni) {
            c = AttributeConstraint.NotIn;
            exps = ni.expressions();
        } else if (constraint instanceof NotLike nl) {
            c = AttributeConstraint.NotLikeEscaped;
            exp1 = nl.pattern();
            exp2 = Literal.of(nl.escape());
        } else if (constraint instanceof NotNull) {
            c = AttributeConstraint.NotNull;
        } else if (constraint instanceof Null) {
            c = AttributeConstraint.Null;
        } else {
            throw new IllegalArgumentException("Constraint: " +
                                               constraint.getClass().getName());
        }

        q.append(c.operator());

        if (exp1 != null) {
            jpqlParamCount = generateExpression(q,
                                                entityVar_,
                                                exp1,
                                                jpqlParamCount,
                                                jpqlParamNames,
                                                jpqlParams);

            if (exp2 != null) {
                if (c == AttributeConstraint.LikeEscaped ||
                    c == AttributeConstraint.NotLikeEscaped)
                    q.append(" ESCAPE "); // [NOT] LIKE ?1 ESCAPE ?2
                else if (c == AttributeConstraint.Between ||
                         c == AttributeConstraint.NotBetween)
                    q.append(" AND "); // [NOT] BETWEEN ?1 AND ?2
                else
                    throw new IllegalArgumentException("Constraint: " +
                                                       constraint.getClass().getName());

                jpqlParamCount = generateExpression(q,
                                                    entityVar_,
                                                    exp2,
                                                    jpqlParamCount,
                                                    jpqlParamNames,
                                                    jpqlParams);
            }
        } else if (exps != null) { // IN or NOT IN
            q.append('(');
            for (int i = 0; i < exps.size(); i++) {
                if (i != 0)
                    q.append(", ");

                jpqlParamCount = generateExpression(q,
                                                    entityVar_,
                                                    exps.get(i),
                                                    jpqlParamCount,
                                                    jpqlParamNames,
                                                    jpqlParams);
            }
            q.append(')');
        }

        return jpqlParamCount;
    }

    /**
     * Appends JPQL to the partially built query to represent an Expression
     * parameter of a Constraint or Restriction.
     *
     * @param q              partially built query ending with the WHERE clause.
     * @param entityVar_     entity identifier variable name and . character.
     * @param expression     the Expression for which to generate JPQL.
     * @param jpqlParamCount number of named or positional parameters in the
     *                           partially built query.
     * @param jpqlParamNames names of named parameters in the partially bulit
     *                           query. Empty if the query uses positional
     *                           parameeters or has none. If using named parameters,
     *                           this method should add any that are generated.
     * @param xprParams      list for this method to populate with the name of
     *                           named parameters or index of positional parameters,
     *                           mapped to value, for values (if any) obtained from
     *                           the Expression.
     * @return the new count of named or positional parameters, including any that
     *         were generated for the Expression.
     */
    @Trivial // avoid tracing values found in Expression.toString()
    private int generateExpression(StringBuilder q,
                                   String entityVar_,
                                   Expression<?, ?> expression,
                                   int jpqlParamCount,
                                   Set<String> jpqlParamNames,
                                   Map<Object, Object> xprParams) {
        if (expression instanceof Attribute<?> attr) {
            q.append(entityVar_).append(attr.name());
        } else if (expression instanceof Literal<?> literal) {
            jpqlParamCount++;
            boolean positionalParams = jpqlParamNames.isEmpty();
            if (positionalParams) {
                q.append('?').append(jpqlParamCount);
                xprParams.put(jpqlParamCount, literal.value());
            } else {
                String paramName = addExpressionParam(jpqlParamCount, jpqlParamNames);
                q.append(':').append(paramName);
                xprParams.put(paramName, literal.value());
            }
        } else if (expression instanceof Path path) {
            // put most distant attribute on the top of the stack
            ArrayList<Attribute<?>> attrStack = new ArrayList<>();
            for (NavigableExpression<?, ?> nav = path.expression(); nav != null;) {
                if (nav instanceof NavigablePath<?, ?, ?> npath) {
                    attrStack.add(npath.attribute());
                    nav = npath.expression();
                } else if (nav instanceof NavigableAttribute<?, ?> attr) {
                    attrStack.add(attr);
                    nav = null;
                } else {
                    throw new IllegalArgumentException(nav.getClass().getName());
                }
            }
            // append attributes from most distant (top of stack) to least distant:
            q.append(entityVar_);
            while (!attrStack.isEmpty())
                q.append(attrStack.remove(attrStack.size() - 1).name()).append('.');
            q.append(path.attribute().name());
        } else if (expression instanceof FunctionExpression<?, ?> fn) {
            String name = fn.name();
            List<? extends Expression<?, ?>> args = fn.arguments();
            // before first argument:
            switch (name) {
                case NumericFunctionExpression.ABS:
                case NumericFunctionExpression.LENGTH:
                case TextFunctionExpression.LEFT:
                case TextFunctionExpression.RIGHT:
                case TextFunctionExpression.LOWER:
                case TextFunctionExpression.UPPER:
                    q.append(name.toUpperCase()).append('(');
                    break;
                case TextFunctionExpression.CONCAT:
                    q.append('(');
                    break;
                case NumericFunctionExpression.NEG:
                    q.append('-');
                    break;
                default:
                    throw new IllegalArgumentException("Function: " + name);
            }
            // first argument:
            jpqlParamCount = generateExpression(q,
                                                entityVar_,
                                                args.get(0),
                                                jpqlParamCount,
                                                jpqlParamNames,
                                                xprParams);
            // between first and second arguments:
            switch (name) {
                case TextFunctionExpression.CONCAT:
                    q.append(" || ");
                    break;
                case TextFunctionExpression.LEFT:
                case TextFunctionExpression.RIGHT:
                    q.append(", ");
                    break;
            }
            // second argument:
            switch (name) {
                case TextFunctionExpression.CONCAT:
                case TextFunctionExpression.LEFT:
                case TextFunctionExpression.RIGHT:
                    jpqlParamCount = generateExpression(q,
                                                        entityVar_,
                                                        args.get(1),
                                                        jpqlParamCount,
                                                        jpqlParamNames,
                                                        xprParams);
                    break;
            }
            // after last argument:
            switch (name) {
                case NumericFunctionExpression.ABS:
                case NumericFunctionExpression.LENGTH:
                case TextFunctionExpression.CONCAT:
                case TextFunctionExpression.LEFT:
                case TextFunctionExpression.RIGHT:
                case TextFunctionExpression.LOWER:
                case TextFunctionExpression.UPPER:
                    q.append(')');
                    break;
            }
        } else if (expression instanceof NumericCast<?, ?> cast) {
            String typeName = cast.type().getSimpleName();
            q.append("CAST (");
            jpqlParamCount = generateExpression(q,
                                                entityVar_,
                                                cast.expression(),
                                                jpqlParamCount,
                                                jpqlParamNames,
                                                xprParams);
            q.append(" AS ").append(typeName).append(')');
        } else if (expression instanceof NumericOperatorExpression<?, ?> op) {
            q.append('(');
            jpqlParamCount = generateExpression(q,
                                                entityVar_,
                                                op.left(),
                                                jpqlParamCount,
                                                jpqlParamNames,
                                                xprParams);
            q.append(switch (op.operator()) {
                case PLUS -> " + ";
                case MINUS -> " - ";
                case TIMES -> " * ";
                case DIVIDE -> " / ";
            });
            jpqlParamCount = generateExpression(q,
                                                entityVar_,
                                                op.right(),
                                                jpqlParamCount,
                                                jpqlParamNames,
                                                xprParams);
            q.append(')');
        } else if (expression instanceof TemporalExpression<?, ?> temporal) {
            if (temporal instanceof CurrentDate)
                q.append("LOCAL DATE");
            else if (temporal instanceof CurrentDateTime)
                q.append("LOCAL DATETIME");
            else if (temporal instanceof CurrentTime)
                q.append("LOCAL TIME");
            else
                throw new IllegalArgumentException("Expression: " +
                                                   expression.getClass().getName());
        } else {
            throw new IllegalArgumentException("Expression: " +
                                               expression.getClass().getName());
        }
        return jpqlParamCount;
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
     *                           parameters or has none. If using named parameters,
     *                           this method should add any that are generated for
     *                           the restriction part of the query.
     * @param qrParams       initially empty list for this method to populate
     *                           with the name of named parameters or index of
     *                           positional parameters, mapped to value, for each
     *                           value obtained from the processed Restriction(s).
     * @return the new count of named or positional parameters, including any that
     *         were generated for the Restriction(s).
     */
    @Override
    // TODO @Trivial // avoid tracing values found in Restriction.toString()
    public int generateRestrictions(StringBuilder q,
                                    Object restriction,
                                    int jpqlParamCount,
                                    Set<String> jpqlParamNames,
                                    Map<Object, Object> qrParams) {

        if (restriction instanceof BasicRestriction<?, ?> r) {
            jpqlParamCount = generateExpression(q,
                                                entityVar_,
                                                r.expression(),
                                                jpqlParamCount,
                                                jpqlParamNames,
                                                qrParams);

            jpqlParamCount = generateConstraint(q,
                                                r.constraint(),
                                                jpqlParamCount,
                                                jpqlParamNames,
                                                qrParams);
        } else if (restriction instanceof CompositeRestriction<?> r) {
            q.append(r.isNegated() ? "NOT (" : "(");
            boolean all = r.type() == CompositeRestriction.Type.ALL;
            List<?> rr = r.restrictions();
            int count = rr.size();
            if (count == 0)
                q.append(all ? "TRUE = TRUE" : "FALSE <> FALSE");
            else // one or more
                for (int i = 0; i < count; i++) {
                    if (i > 0)
                        q.append(all ? " AND " : " OR ");

                    jpqlParamCount = generateRestrictions(q,
                                                          rr.get(i),
                                                          jpqlParamCount,
                                                          jpqlParamNames,
                                                          qrParams);
                }
            q.append(')');
        } else {
            throw new IllegalArgumentException("Unsupported Restriction type: " +
                                               restriction.getClass().getName());
        }

        return jpqlParamCount;
    }

    @Override
    @Trivial // to avoid tracing values supplied to repository methods
    public Map<Integer, Object> getDeferredConstraints(boolean alwaysDefer,
                                                       Object[] methodParams) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "getDeferredConstraints",
                     alwaysDefer,
                     Stream.of(methodParams) //
                                     .map(o -> o == null ? null : o.getClass().getName()) //
                                     .toList());

        Map<Integer, Object> deferred = null;

        for (int i = 0; i < specialParamsStartAt; i++)
            if (methodParams[i] instanceof Constraint c &&
                (alwaysDefer || hasNonLiteralExpression(c))) {
                if (deferred == null)
                    deferred = new HashMap<>();
                deferred.put(i, c);
            }

        if (deferred == null)
            deferred = Collections.emptyMap();

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "getDeferredConstraints", deferred.keySet());
        return deferred;
    }

    @Override
    @Trivial
    protected String getNullOrdering(Sort<?> sort, boolean sameDirection) {
        switch (sort.nullOrdering()) {
            case FIRST:
                return sameDirection ? Nulls.FIRST.name() : Nulls.LAST.name();
            case LAST:
                return sameDirection ? Nulls.LAST.name() : Nulls.FIRST.name();
            case UNSPECIFIED:
                return null;
            default:
                throw new IllegalStateException("Sort.nullOrdering: " +
                                                sort.nullOrdering());
        }
    }

    @Override
    @Trivial
    protected String getQueryAnnoValue() {
        if (methodTypeAnno instanceof Query query) {
            return query.value();
        } else if (methodTypeAnno instanceof JakartaQuery query) {
            return query.value();
        } else if (methodTypeAnno instanceof NativeQuery query) {
            type = NATIVE;
            return query.value();
        } else {
            return null;
        }
    }

    /**
     * Determine if the constraint applies to one or more values that are
     * expressions other than literal expressions.
     *
     * @param constraint instance of Constraint supplied to a repository method.
     * @return true if the constraint applies to any non-literal expressions.
     */
    @Trivial
    private boolean hasNonLiteralExpression(Constraint<?> constraint) {
        boolean allLiteral = false;
        if (constraint instanceof AtLeast c)
            allLiteral = c instanceof Literal;
        else if (constraint instanceof AtMost c)
            allLiteral = c.bound() instanceof Literal;
        else if (constraint instanceof Between c)
            allLiteral = c.lowerBound() instanceof Literal &&
                         c.upperBound() instanceof Literal;
        else if (constraint instanceof EqualTo c)
            allLiteral = c.expression() instanceof Literal;
        else if (constraint instanceof GreaterThan c)
            allLiteral = c.bound() instanceof Literal;
        else if (constraint instanceof In c)
            allLiteral = c.expressions().stream().allMatch(e -> e instanceof Literal);
        else if (constraint instanceof LessThan c)
            allLiteral = c.bound() instanceof Literal;
        else if (constraint instanceof Like c)
            allLiteral = c.pattern() instanceof Literal;
        else if (constraint instanceof NotBetween c)
            allLiteral = c.lowerBound() instanceof Literal &&
                         c.upperBound() instanceof Literal;
        else if (constraint instanceof NotEqualTo c)
            allLiteral = c.expression() instanceof Literal;
        else if (constraint instanceof NotIn c)
            allLiteral = c.expressions().stream().allMatch(e -> e instanceof Literal);
        else if (constraint instanceof NotLike c)
            allLiteral = c.pattern() instanceof Literal;
        else if (constraint instanceof NotNull)
            allLiteral = true;
        else if (constraint instanceof Null)
            allLiteral = true;
        else
            throw new UnsupportedOperationException("Constraint: " +
                                                    constraint.getClass().getName());
        return !allLiteral;
    }

    @Override
    @Trivial
    protected void identifyType() {
        if (entityParamType != null && methodTypeAnno instanceof Delete)
            setType(Delete.class, LC_DELETE);
        else if (entityParamType != null && methodTypeAnno instanceof Update)
            setType(Update.class,
                    entityInfo.attributeNamesForEntityUpdate != null &&
                                  Util.UPDATE_COUNT_TYPES.contains(singleType) //
                                                  ? LC_UPDATE //
                                                  : LC_UPDATE_MERGE);
        else if (methodTypeAnno instanceof Insert)
            setType(Insert.class, INSERT);
        else if (methodTypeAnno instanceof Save)
            setType(Save.class, SAVE);
        else if (methodTypeAnno instanceof Detach)
            setType(Detach.class, DETACH);
        else if (methodTypeAnno instanceof Merge)
            setType(Merge.class, MERGE);
        else if (methodTypeAnno instanceof Persist)
            setType(Persist.class, PERSIST);
        else if (methodTypeAnno instanceof Refresh)
            setType(Refresh.class, REFRESH);
        else if (methodTypeAnno instanceof Remove)
            setType(Remove.class, REMOVE);
    }

    @Override
    public int inspectMethodParam(int p,
                                  Class<?> paramType,
                                  Annotation[] paramAnnos,
                                  String[] attrNames,
                                  AttributeConstraint[] constraints,
                                  char[] updateOps,
                                  int prevNumJPQLParams) {
        int numJPQLParams = prevNumJPQLParams;

        for (Annotation anno : paramAnnos)
            if (anno instanceof Is) {
                constraints[p] = toAttributeConstraint(((Is) anno).value(), paramType);
            } else if (anno instanceof Assign) {
                attrNames[p] = ((Assign) anno).value();
                updateOps[p] = '=';
                numJPQLParams++;
            } else if (anno instanceof Add) {
                attrNames[p] = ((Add) anno).value();
                updateOps[p] = '+';
                numJPQLParams++;
            } else if (anno instanceof Multiply) {
                attrNames[p] = ((Multiply) anno).value();
                updateOps[p] = '*';
                numJPQLParams++;
            } else if (anno instanceof Divide) {
                attrNames[p] = ((Divide) anno).value();
                updateOps[p] = '/';
                numJPQLParams++;
            } else if (anno instanceof SubtractFrom) {
                attrNames[p] = ((SubtractFrom) anno).value();
                updateOps[p] = '-';
                numJPQLParams++;
            }

        if (constraints[p] == null && Constraint.class.isAssignableFrom(paramType)) {
            constraints[p] = toAttributeConstraint(null, paramType);
        }

        if (numJPQLParams == prevNumJPQLParams) {
            if (constraints[p] == null)
                constraints[p] = AttributeConstraint.Equal;

            // no annotation indicating a constraint or update
            numJPQLParams += constraints[p].numMethodParams();
        } else if (numJPQLParams - prevNumJPQLParams > 1) {
            // TODO possibly allow a redundant Constraint that matches the Is annotation.
            numJPQLParams = PARAM_ANNOS_CONFLICT;
        } else if (false) { // TODO 1.1 check if paramType is a Constraint
            numJPQLParams = PARAM_ANNO_CONFLICTS_WITH_CONSTRAINT;
        }

        return numJPQLParams;
    }

    /**
     * Configures the query options that are intended for JPQL find/select queries.
     *
     * @param options       configurable query options
     * @param query         the query upon which to configure the options
     * @param entityHandler EntityAgent or EntityManager
     */
    private <T> void setReadOptions(QueryOptions options,
                                    jakarta.persistence.Query query,
                                    AutoCloseable entityHandler) //
                    throws // TODO remove once using Persistence 4.0 API
                    IllegalAccessException, //
                    InvocationTargetException, //
                    NoSuchMethodException {
        // QueryOptions specified via Hints:
        for (QueryHint hint : options.hints())
            query.setHint(hint.name(),
                          hint);
        if (options.entityGraph().length() > 0) {
            // TODO Persistence 4.0: entityHandler.getEntityGraph(options.entityGraph());
            EntityGraph<?> loadGraph = (EntityGraph<?>) entityHandler.getClass() //
                            .getMethod("getEntityGraph", String.class) //
                            .invoke(entityHandler, options.entityGraph());
            query.setHint("jakarta.persistence.loadgraph",
                          loadGraph);
        }

        query.setHint("jakarta.persistence.lock.scope",
                      options.lockScope());

        // QueryOptions specified via dedicated API methods:
        if (!entityInfo.isHibernate) {
            // TODO enable for Hibernate once its NullPointerException is fixed
            query.setCacheStoreMode(options.cacheStoreMode());
            query.setCacheRetrieveMode(options.cacheRetrieveMode());
        }
        // TODO Persistence 4.0 directly delegate to setQueryFlushMode
        switch (options.flush()) {
            case DEFAULT:
                break;
            case FLUSH:
                query.setFlushMode(FlushModeType.AUTO);
                break;
            case NO_FLUSH:
                // TODO query.setQueryFlushMode(NO_FLUSH);
                throw new UnsupportedOperationException("QueryFlushMode.NO_FLUSH");
        }
        query.setLockMode(options.lockMode());
        // TODO the correct value is null, but Hibernate and EclipseLink do not handle it correctly
        query.setTimeout(options.timeout() == -1 ? 0 : options.timeout());
    }

    /**
     * Configures the query options that are intended for JPQL DELETE and UPDATE
     * statements.
     *
     * @param options   configurable query options
     * @param statement the jakarta.persistence.Statement upon which to configure
     *                      the options
     */
    private static void setWriteOptions(QueryOptions options,
                                        jakarta.persistence.Query statement) {
        // QueryOptions specified via Hints:
        for (QueryHint hint : options.hints())
            statement.setHint(hint.name(),
                              hint.value());

        // QueryOptions specified via dedicated API methods:
        // TODO Persistence 4.0 directly delegate to setQueryFlushMode
        switch (options.flush()) {
            case DEFAULT:
                break;
            case FLUSH:
                statement.setFlushMode(FlushModeType.AUTO);
                break;
            case NO_FLUSH:
                // TODO query.setQueryFlushMode(NO_FLUSH);
                throw new UnsupportedOperationException("QueryFlushMode.NO_FLUSH");
        }

        statement.setTimeout(options.timeout() == -1 ? null : options.timeout());
    }

    /**
     * Convert a constraint subtype to its AttributeConstraint representation.
     *
     * @param isAnnoConstraintType subtype of Constraint indicated by Is anno.
     *                                 Otherwise null.
     * @param methodParamType      repository method parameter type.
     * @return AttributeConstraint representation.
     */
    private static AttributeConstraint toAttributeConstraint(Class<?> isAnnoConstraintType,
                                                             Class<?> methodParamType) {
        Class<?> type = isAnnoConstraintType == null ||
                        Constraint.class.isAssignableFrom(methodParamType) //
                                        ? methodParamType //
                                        : isAnnoConstraintType;

        if (isAnnoConstraintType != null && type != isAnnoConstraintType)
            ; // TODO 1.1 error for collisions

        AttributeConstraint constraint;
        if (AtLeast.class.equals(type))
            constraint = AttributeConstraint.GreaterThanEqual;
        else if (AtMost.class.equals(type))
            constraint = AttributeConstraint.LessThanEqual;
        else if (Between.class.equals(type))
            constraint = AttributeConstraint.Between;
        else if (EqualTo.class.equals(type))
            constraint = AttributeConstraint.Equal;
        else if (GreaterThan.class.equals(type))
            constraint = AttributeConstraint.GreaterThan;
        else if (In.class.equals(type))
            constraint = AttributeConstraint.In;
        else if (LessThan.class.equals(type))
            constraint = AttributeConstraint.LessThan;
        else if (Like.class.equals(type))
            constraint = Like.class.equals(methodParamType) //
                            ? AttributeConstraint.LikeEscaped //
                            : AttributeConstraint.Like;
        else if (NotBetween.class.equals(type))
            constraint = AttributeConstraint.NotBetween;
        else if (NotEqualTo.class.equals(type))
            constraint = AttributeConstraint.Not;
        else if (NotIn.class.equals(type))
            constraint = AttributeConstraint.NotIn;
        else if (NotLike.class.equals(type))
            constraint = NotLike.class.equals(methodParamType) //
                            ? AttributeConstraint.NotLikeEscaped //
                            : AttributeConstraint.NotLike;
        else if (NotNull.class.equals(type))
            constraint = AttributeConstraint.NotNull;
        else if (Null.class.equals(type))
            constraint = AttributeConstraint.Null;
        else
            // TODO 1.1 if isAnnoConstraintType == null handle generic Constraint else
            throw new UnsupportedOperationException("Constraint: " + type.getName()); // TODO NLS

        // TODO 1.1: errors for types the Is annotation cannot support

        return constraint;
    }

    @Override
    @Trivial // avoid logging customer data
    public Object[] toConstraintValues(Object constraintOrValue) {
        // TODO 1.1 this is not the correct implementation (doesn't account for
        // other types of expressions than literals) and is only here temporarily
        // so that we can complete remove some experimental code elsewhere without
        // breaking tests.
        boolean isList = false;
        Object[] values;
        if (constraintOrValue instanceof AtLeast c)
            values = new Object[] { c.bound() };
        else if (constraintOrValue instanceof AtMost c)
            values = new Object[] { c.bound() };
        else if (constraintOrValue instanceof Between c)
            values = new Object[] { c.lowerBound(), c.upperBound() };
        else if (constraintOrValue instanceof EqualTo c)
            values = new Object[] { c.expression() };
        else if (constraintOrValue instanceof GreaterThan c)
            values = new Object[] { c.bound() };
        else if (isList = constraintOrValue instanceof In)
            values = ((In) constraintOrValue).expressions().toArray();
        else if (constraintOrValue instanceof LessThan c)
            values = new Object[] { c.bound() };
        else if (constraintOrValue instanceof Like c)
            values = new Object[] { c.pattern(), c.escape() };
        else if (constraintOrValue instanceof NotBetween c)
            values = new Object[] { c.lowerBound(), c.upperBound() };
        else if (constraintOrValue instanceof NotEqualTo c)
            values = new Object[] { c.expression() };
        else if (isList = constraintOrValue instanceof NotIn)
            values = ((NotIn) constraintOrValue).expressions().toArray();
        else if (constraintOrValue instanceof NotLike c)
            values = new Object[] { c.pattern(), c.escape() };
        else if (constraintOrValue instanceof NotNull ||
                 constraintOrValue instanceof Null)
            values = NO_VALUES;
        else if (constraintOrValue instanceof Constraint)
            throw new UnsupportedOperationException("Constraint: " +
                                                    constraintOrValue.getClass().getName());
        else
            return null;

        for (int i = 0; i < values.length; i++)
            if (values[i] instanceof Literal literal)
                values[i] = literal.value();
            else if (values[i] instanceof Character)
                ; // the escape character for Like and NotLike
            else
                // non-Literal constraint - should be unreachable
                throw new UnsupportedOperationException(values[i].getClass().getName());

        if (isList)
            values = new Object[] { List.of(values) };

        return values;
    }

}
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Publisher;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

import jakarta.data.DataException;
import jakarta.data.Delete;
import jakarta.data.Inheritance;
import jakarta.data.Page;
import jakarta.data.Result;
import jakarta.data.Select;
import jakarta.data.Select.Aggregate;
import jakarta.data.Update;
import jakarta.data.Where;
import jakarta.data.repository.Limit;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Sort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R, E> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private static final Set<Class<?>> SPECIAL_PARAM_TYPES = new HashSet<>(Arrays.asList //
    (Collector.class, Consumer.class, Limit.class, Pageable.class, Sort.class, Sort[].class));

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
        StringBuilder q = null;

        // TODO would it be more efficient to invoke method.getAnnotations() once?

        // @Query annotation
        Query query = queryInfo.method.getAnnotation(Query.class);
        queryInfo.jpql = query == null ? null : query.value();

        if (queryInfo.jpql == null) {
            // @Delete/@Update/@Where/@OrderBy annotations
            Update update = queryInfo.method.getAnnotation(Update.class);
            Where where = queryInfo.method.getAnnotation(Where.class);
            if (update == null) {
                if (queryInfo.method.getAnnotation(Delete.class) == null) {
                    if (where != null) {
                        queryInfo.type = QueryInfo.Type.SELECT;
                        generateSelect(queryInfo, q = new StringBuilder(200));
                        q.append(" WHERE ").append(where.value());
                        queryInfo.jpql = q.toString();
                    }
                } else {
                    queryInfo.type = QueryInfo.Type.DELETE;
                    q = new StringBuilder(200).append("DELETE FROM ").append(entityInfo.name).append(" o");
                    if (where != null)
                        q.append(" WHERE ").append(where.value());
                    queryInfo.jpql = q.toString();
                }
            } else {
                queryInfo.type = QueryInfo.Type.UPDATE;
                q = new StringBuilder(200).append("UPDATE ").append(entityInfo.name).append(" o SET ").append(update.value());
                if (where != null)
                    q.append(" WHERE ").append(where.value());
                queryInfo.jpql = q.toString();
            }

            if (queryInfo.jpql == null && queryInfo.method.getName().startsWith("save")) {
                queryInfo.type = QueryInfo.Type.MERGE;
                Class<?>[] paramTypes = queryInfo.method.getParameterTypes();
                if (paramTypes.length == 0)
                    throw new UnsupportedOperationException(queryInfo.method.getName() + " without any parameters");
                queryInfo.saveParamType = paramTypes[0];
            } else if (queryInfo.jpql == null) {

                // Repository method name pattern queries
                queryInfo.jpql = generateRepositoryQuery(queryInfo);

                // @Select annotation only
                if (queryInfo.jpql == null) {
                    Select select = queryInfo.method.getAnnotation(Select.class);
                    if (select != null) {
                        queryInfo.type = QueryInfo.Type.SELECT;
                        generateSelect(queryInfo, q = new StringBuilder(100));
                        queryInfo.jpql = q.toString();
                    }
                }
            }
        }

        // The Sorts parameter is from JNoSQL and might get added to Jakarta Data.
        // The @OrderBy annotation from Jakarta Data defines the same information annotatively.
        OrderBy[] orderBy = queryInfo.method.getAnnotationsByType(OrderBy.class);
        if (orderBy.length > 0) {
            queryInfo.type = queryInfo.type == null ? QueryInfo.Type.SELECT : queryInfo.type;
            if (q == null)
                if (queryInfo.jpql == null)
                    generateSelect(queryInfo, q = new StringBuilder(200));
                else
                    q = new StringBuilder(queryInfo.jpql);

            for (int i = 0; i < orderBy.length; i++) {
                q.append(i == 0 ? " ORDER BY o." : ", o.").append(orderBy[i].value());
                if (orderBy[i].descending())
                    q.append(" DESC");
            }
        }

        queryInfo.jpql = q == null ? queryInfo.jpql : q.toString();

        if (queryInfo.type == null) {
            String upper = queryInfo.jpql.toUpperCase();
            if (upper.startsWith("SELECT"))
                queryInfo.type = QueryInfo.Type.SELECT;
            else if (upper.startsWith("UPDATE"))
                queryInfo.type = QueryInfo.Type.UPDATE;
            else if (upper.startsWith("DELETE"))
                queryInfo.type = QueryInfo.Type.DELETE;
            else
                throw new UnsupportedOperationException(queryInfo.jpql);
        }

        // If we don't already know from generating the JPQL, find out how many
        // parameters the JPQL takes and which parameters are named parameters.
        if (queryInfo.jpql != null && queryInfo.paramCount < 0) {
            Parameter[] params = queryInfo.method.getParameters();
            for (int i = 0; i < params.length && !SPECIAL_PARAM_TYPES.contains(params[i].getType()); i++) {
                List<String> paramNames = i == 0 ? (queryInfo.paramNames = new ArrayList<>()) : queryInfo.paramNames;
                Param param = params[i].getAnnotation(Param.class);
                paramNames.add(param == null ? null : param.value());
            }
            queryInfo.paramCount = queryInfo.paramNames.size();
        }

        return queryInfo;
    }

    private String generateRepositoryQuery(QueryInfo queryInfo) {
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
            int orderBy = methodName.indexOf("OrderBy", c);
            generateSelect(queryInfo, q = new StringBuilder(200));
            if (orderBy > c || orderBy == -1 && methodName.length() > c) {
                String s = orderBy > 0 ? methodName.substring(c, orderBy) : methodName.substring(c);
                generateRepositoryQueryConditions(queryInfo, s, q);
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
                generateRepositoryQueryConditions(queryInfo, methodName.substring(c), q);
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
                generateRepositoryQueryConditions(queryInfo, methodName.substring(c), q);
            queryInfo.type = QueryInfo.Type.COUNT;
        } else if (methodName.startsWith("exists")) {
            int by = methodName.indexOf("By", 6);
            int c = by < 0 ? 6 : by + 2;
            q = new StringBuilder(200).append("SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END FROM ").append(queryInfo.entityInfo.name).append(" o");
            if (methodName.length() > c)
                generateRepositoryQueryConditions(queryInfo, methodName.substring(c), q);
            queryInfo.type = QueryInfo.Type.EXISTS;
        }

        return q == null ? null : q.toString();
    }

    /**
     * Generates JPQL for a findBy or deleteBy condition such as MyColumn[Not?]Like
     */
    private void generateRepositoryQueryCondition(QueryInfo queryInfo, String expression, StringBuilder q) {
        int length = expression.length();

        Condition condition = Condition.EQUALS;
        switch (expression.charAt(length - 1)) {
            case 'n': // GreaterThan | LessThan | In | Between
                if (length > Condition.IN.length) {
                    char ch = expression.charAt(length - 2);
                    if (ch == 'a') { // GreaterThan | LessThan
                        if (expression.endsWith("GreaterThan"))
                            condition = Condition.GREATER_THAN;
                        else if (expression.endsWith("LessThan"))
                            condition = Condition.LESS_THAN;
                    } else if (ch == 'I') { // In
                        condition = Condition.IN;
                    } else if (expression.endsWith("Between")) {
                        condition = Condition.BETWEEN;
                    }
                }
                break;
            case 'l': // GreaterThanEqual | LessThanEqual
                if (length > Condition.LESS_THAN_EQUAL.length && expression.charAt(length - 4) == 'q')
                    if (expression.endsWith("GreaterThanEqual"))
                        condition = Condition.GREATER_THAN_EQUAL;
                    else if (expression.endsWith("LessThanEqual"))
                        condition = Condition.LESS_THAN_EQUAL;
                break;
            case 'e': // Like
                if (expression.endsWith("Like"))
                    condition = Condition.LIKE;
                break;
            case 'h': // StartsWith | EndsWith
                if (expression.endsWith("StartsWith"))
                    condition = Condition.STARTS_WITH;
                else if (expression.endsWith("EndsWith"))
                    condition = Condition.ENDS_WITH;
                break;
            case 's': // Contains
                if (expression.endsWith("Contains"))
                    condition = Condition.CONTAINS;
        }

        boolean negated = length > condition.length + 3 //
                          && expression.charAt(length - condition.length - 3) == 'N'
                          && expression.charAt(length - condition.length - 2) == 'o'
                          && expression.charAt(length - condition.length - 1) == 't';

        String attribute = expression.substring(0, length - condition.length - (negated ? 3 : 0));

        if (negated) {
            Condition negatedCondition = condition.negate();
            if (negatedCondition != null) {
                condition = negatedCondition;
                negated = false;
            }
        }

        boolean upper = false;
        boolean lower = false;
        if (attribute.length() > 5 && ((upper = attribute.startsWith("Upper")) || (lower = attribute.startsWith("Lower"))))
            attribute = attribute.substring(5);

        String name = queryInfo.entityInfo.getAttributeName(attribute);
        if (name == null) {
            // Special case for CrudRepository.deleteAll and CrudRepository.findAll
            int len = q.length(), where = q.lastIndexOf(" WHERE ");
            if (where + 7 == len)
                q.delete(where, len); // Remove " WHERE " because there are no conditions
            return;
        }

        StringBuilder a = new StringBuilder();
        if (upper)
            a.append("UPPER(o.").append(name).append(')');
        else if (lower)
            a.append("LOWER(o.").append(name).append(')');
        else
            a.append("o.").append(name);

        String attributeExpr = a.toString();

        switch (condition) {
            case STARTS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT(?").append(++queryInfo.paramCount).append(", '%')");
                break;
            case ENDS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ?").append(++queryInfo.paramCount).append(")");
                break;
            case LIKE:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE ?").append(++queryInfo.paramCount);
                break;
            case BETWEEN:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ?").append(++queryInfo.paramCount).append(" AND ?").append(++queryInfo.paramCount);
                break;
            case CONTAINS:
                if (queryInfo.entityInfo.collectionAttributeNames.contains(name))
                    q.append(" ?").append(++queryInfo.paramCount).append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
                else
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ?").append(++queryInfo.paramCount).append(", '%')");
                break;
            default:
                q.append(attributeExpr).append(negated ? " NOT " : "").append(condition.operator).append('?').append(++queryInfo.paramCount);
        }
    }

    /**
     * Generates the JPQL WHERE clause for all findBy, deleteBy, or updateBy conditions such as MyColumn[Not?]Like
     */
    private void generateRepositoryQueryConditions(QueryInfo queryInfo, String conditions, StringBuilder q) {
        queryInfo.paramCount = 0;
        q.append(" WHERE ");
        for (int and = 0, or = 0, iNext, i = 0; i >= 0; i = iNext) {
            and = and == -1 || and > i ? and : conditions.indexOf("And", i);
            or = or == -1 || or > i ? or : conditions.indexOf("Or", i);
            iNext = Math.min(and, or);
            if (iNext < 0)
                iNext = Math.max(and, or);
            String condition = iNext < 0 ? conditions.substring(i) : conditions.substring(i, iNext);
            generateRepositoryQueryCondition(queryInfo, condition, q);
            if (iNext > 0) {
                q.append(iNext == and ? " AND " : " OR ");
                iNext += (iNext == and ? 3 : 2);
            }
        }
    }

    /**
     * Generates the JPQL ORDER BY clause for a repository findBy method such as findByLastNameLikeOrderByLastNameOrderByFirstName
     */
    private void generateRepositoryQueryOrderBy(QueryInfo queryInfo, int orderBy, StringBuilder q) {
        String methodName = queryInfo.method.getName();
        q.append(" ORDER BY ");
        do {
            int i = orderBy + 7;
            orderBy = methodName.indexOf("OrderBy", i);
            int stopAt = orderBy == -1 ? methodName.length() : orderBy;
            boolean desc = false;
            if (methodName.charAt(stopAt - 1) == 'c' && methodName.charAt(stopAt - 2) == 's')
                if (methodName.charAt(stopAt - 3) == 'A') {
                    stopAt -= 3;
                } else if (methodName.charAt(stopAt - 3) == 'e' && methodName.charAt(stopAt - 4) == 'D') {
                    stopAt -= 4;
                    desc = true;
                }

            String attribute = methodName.substring(i, stopAt);
            String name = queryInfo.entityInfo.getAttributeName(attribute);
            q.append("o.").append(name == null ? attribute : name);

            if (desc)
                q.append(" DESC");
            if (orderBy > 0)
                q.append(", ");
        } while (orderBy > 0);
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
        generateRepositoryQueryConditions(queryInfo, methodName.substring(c, uFirst), where);

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

    private void generateSelect(QueryInfo queryInfo, StringBuilder q) {
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
                    return repositoryInterface.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
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

                        // Jakarta Data allows the method parameter positions after those used as query parameters
                        // to be used for purposes such as pagination and sorting.
                        // Collector is added here for experimentation.
                        StringBuilder q = null;
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
                            else if (param instanceof Sort) {
                                Sort sort = (Sort) param;
                                q = q == null ? new StringBuilder(queryInfo.jpql).append(" ORDER BY o.") : q.append(", o.");
                                q.append(sort.getProperty());
                                if (sort.isDescending())
                                    q.append(" DESC");
                            } else if (param instanceof Sort[]) {
                                Sort[] sorts = (Sort[]) param;;
                                for (int s = 0; s < sorts.length; s++)
                                    if (sorts[s] == null)
                                        throw new NullPointerException("Sort: null");
                                    else {
                                        q = q == null ? new StringBuilder(queryInfo.jpql).append(" ORDER BY o.") : q.append(", o.");
                                        q.append(sorts[s].getProperty());
                                        if (sorts[s].isDescending())
                                            q.append(" DESC");
                                    }
                            }
                        }

                        if (q != null)
                            queryInfo = queryInfo.withJPQL(q.toString());

                        boolean asyncCompatibleResultForPagination = pagination != null &&
                                                                     (void.class.equals(returnType) || CompletableFuture.class.equals(returnType)
                                                                      || CompletionStage.class.equals(returnType));

                        if (asyncCompatibleResultForPagination && collector != null)
                            return runAndCollect(queryInfo, pagination, collector, args);
                        else if (asyncCompatibleResultForPagination && consumer != null)
                            return runWithConsumer(queryInfo, pagination, consumer, args);
                        else if (pagination != null && Iterator.class.equals(returnType))
                            return new PaginatedIterator<E>(queryInfo, pagination, args);
                        else if (Page.class.equals(returnType))
                            return new PageImpl<E>(queryInfo, pagination, args); // TODO Limit with Page as return type
                        else if (Publisher.class.equals(returnType))
                            return new PublisherImpl<E>(queryInfo, provider.executor, limit, pagination, args);

                        em = queryInfo.entityInfo.persister.createEntityManager();

                        TypedQuery<?> query = em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
                        queryInfo.setParameters(query, args);

                        long maxResults = limit != null ? limit.maxResults() //
                                        : pagination != null ? pagination.getSize() //
                                                        : queryInfo.maxResults;

                        long startAt = limit != null ? limit.startAt() - 1 //
                                        : pagination != null ? (pagination.getPage() - 1) * maxResults //
                                                        : 0;
                        // TODO possible overflow with both of these.
                        if (maxResults > 0)
                            query.setMaxResults((int) maxResults);
                        if (startAt > 0)
                            query.setFirstResult((int) startAt);

                        List<?> results = query.getResultList();
                        Class<?> type = queryInfo.returnTypeParam != null && (Optional.class.equals(returnType)
                                                                              || CompletableFuture.class.equals(returnType)
                                                                              || CompletionStage.class.equals(returnType)) //
                                                                                              ? queryInfo.returnTypeParam //
                                                                                              : returnType;
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, results.size() + " results to be returned as " + type.getName());

                        if (collector != null) {
                            // Collector is more useful on the other path, when combined with pagination
                            Object r = collector.supplier().get();
                            BiConsumer<Object, Object> accumulator = collector.accumulator();
                            for (Object item : results)
                                accumulator.accept(r, item);
                            returnValue = collector.finisher().apply(r);
                        } else if (consumer != null) {
                            for (Object result : results)
                                consumer.accept(result);
                            returnValue = null;
                        } else if (queryInfo.entityInfo.type.equals(type)) {
                            returnValue = results.isEmpty() ? null : results.get(0);
                        } else if (type.isInstance(results)) {
                            returnValue = results;
                        } else if (queryInfo.returnArrayType != null) {
                            Object r = Array.newInstance(queryInfo.returnArrayType, results.size());
                            int i = 0;
                            for (Object o : results)
                                Array.set(r, i++, o);
                            returnValue = r;
                        } else if (Collection.class.isAssignableFrom(type)) {
                            try {
                                @SuppressWarnings("unchecked")
                                Constructor<? extends Collection<Object>> c = (Constructor<? extends Collection<Object>>) type.getConstructor();
                                Collection<Object> list = c.newInstance();
                                list.addAll(results);
                                returnValue = list;
                            } catch (NoSuchMethodException x) {
                                throw new UnsupportedOperationException(type + " lacks public zero parameter constructor.");
                            }
                        } else if (Stream.class.equals(type)) {
                            Stream.Builder<Object> builder = Stream.builder();
                            for (Object result : results)
                                builder.accept(result);
                            returnValue = builder.build();
                        } else if (IntStream.class.equals(type)) {
                            IntStream.Builder builder = IntStream.builder();
                            for (Object result : results)
                                builder.accept((Integer) result);
                            returnValue = builder.build();
                        } else if (LongStream.class.equals(type)) {
                            LongStream.Builder builder = LongStream.builder();
                            for (Object result : results)
                                builder.accept((Long) result);
                            returnValue = builder.build();
                        } else if (DoubleStream.class.equals(type)) {
                            DoubleStream.Builder builder = DoubleStream.builder();
                            for (Object result : results)
                                builder.accept((Double) result);
                            returnValue = builder.build();
                        } else if (results.isEmpty()) {
                            returnValue = null;
                        } else if (results.size() == 1) {
                            // single result
                            returnValue = results.get(0);
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
                        } else { // TODO convert other return types?
                            returnValue = results;
                        }

                        if (Optional.class.equals(returnType)) {
                            returnValue = results.isEmpty() ? Optional.empty() : Optional.of(returnValue);
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
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + repositoryInterface.getSimpleName() + '.' + method.getName(), x);
            throw x;
        }
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
            long num = first + 5 == length ? 1 : 0;
            if (num == 0)
                for (int c = first + 5; c < length; c++) {
                    char ch = s.charAt(c);
                    if (ch >= '0' && ch <= '9')
                        num = num * 10 + (ch - '0');
                    else {
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
            long maxPageSize;
            do {
                // TODO possible overflow with both of these.
                maxPageSize = pagination.getSize();
                query.setFirstResult((int) ((pagination.getPage() - 1) * maxPageSize));
                query.setMaxResults((int) maxPageSize);
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
            long maxPageSize;
            do {
                // TODO possible overflow with both of these.
                maxPageSize = pagination.getSize();
                query.setFirstResult((int) ((pagination.getPage() - 1) * maxPageSize));
                query.setMaxResults((int) maxPageSize);
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
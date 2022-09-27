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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

import jakarta.data.Delete;
import jakarta.data.Inheritance;
import jakarta.data.Limit;
import jakarta.data.OrderBy;
import jakarta.data.Paginated;
import jakarta.data.Pagination;
import jakarta.data.Param;
import jakarta.data.Query;
import jakarta.data.Result;
import jakarta.data.Select;
import jakarta.data.Select.Aggregate;
import jakarta.data.Sort;
import jakarta.data.SortType;
import jakarta.data.Sorts;
import jakarta.data.Update;
import jakarta.data.Where;
import jakarta.data.repository.CrudRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

public class RepositoryImpl<R, E> implements InvocationHandler {
    private static final TraceComponent tc = Tr.register(RepositoryImpl.class);

    private final PersistenceDataProvider provider;
    final Map<Method, CompletableFuture<QueryInfo>> queries = new HashMap<>();
    private final Class<R> repositoryInterface;

    public RepositoryImpl(PersistenceDataProvider provider, Class<R> repositoryInterface, Class<E> defaultEntityClass) {
        this.provider = provider;
        this.repositoryInterface = repositoryInterface;
        boolean inheritance = defaultEntityClass.getAnnotation(Inheritance.class) != null ||
                              defaultEntityClass.getAnnotation(jakarta.persistence.Inheritance.class) != null;

        CompletableFuture<EntityInfo> defaultEntityInfoFuture = provider.futureEntityInfo(defaultEntityClass);

        for (Method method : repositoryInterface.getMethods()) {
            Class<?> returnType = method.getReturnType();
            Class<?> returnArrayType = returnType.getComponentType();

            Class<?> entityClass = returnArrayType == null ? returnType : returnArrayType;
            if (!inheritance || !defaultEntityClass.isAssignableFrom(entityClass)) // TODO allow other entity types from model
                entityClass = defaultEntityClass;

            CompletableFuture<EntityInfo> entityInfoFuture = entityClass.equals(defaultEntityClass) ? defaultEntityInfoFuture : provider.futureEntityInfo(entityClass);

            queries.put(method, entityInfoFuture.thenCombine(CompletableFuture.completedFuture(method), this::createQueryInfo));
        }
    }

    private QueryInfo createQueryInfo(EntityInfo entityInfo, Method method) {

        StringBuilder q = null;
        Class<?> returnType = method.getReturnType();
        Class<?> returnArrayType = returnType.getComponentType();
        Class<?> saveParamType = null;
        QueryInfo.Type type = null;

        // TODO would it be more efficient to invoke method.getAnnotations() once?

        // @Query annotation
        Query query = method.getAnnotation(Query.class);
        String jpql = query == null ? null : query.value();

        // Repository built-in methods // TODO ideally these should not need special handling. A user should be able to copy/paste them onto a custom repository interface
        if (jpql == null && CrudRepository.class.equals(method.getDeclaringClass()))
            jpql = getBuiltInRepositoryQuery(entityInfo, method.getName(), method.getParameterTypes());

        if (jpql == null) {
            // @Delete/@Update/@Where/@OrderBy annotations
            Update update = method.getAnnotation(Update.class);
            Where where = method.getAnnotation(Where.class);
            if (update == null) {
                if (method.getAnnotation(Delete.class) == null) {
                    if (where != null) {
                        type = QueryInfo.Type.SELECT;
                        generateSelect(entityInfo, q = new StringBuilder(200), method);
                        q.append(" WHERE ").append(where.value());
                        jpql = q.toString();
                    }
                } else {
                    type = QueryInfo.Type.DELETE;
                    q = new StringBuilder(200).append("DELETE FROM ").append(entityInfo.name).append(" o");
                    if (where != null)
                        q.append(" WHERE ").append(where.value());
                    jpql = q.toString();
                }
            } else {
                type = QueryInfo.Type.UPDATE;
                q = new StringBuilder(200).append("UPDATE ").append(entityInfo.name).append(" o SET ").append(update.value());
                if (where != null)
                    q.append(" WHERE ").append(where.value());
                jpql = q.toString();
            }

            if (jpql == null && method.getName().startsWith("save")) {
                type = QueryInfo.Type.MERGE;
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 0)
                    throw new UnsupportedOperationException(method.getName() + " without any parameters");
                saveParamType = paramTypes[0];
            } else if (jpql == null) {

                // Repository method name pattern queries
                jpql = generateRepositoryQuery(entityInfo, method);

                // @Select annotation only
                if (jpql == null) {
                    Select select = method.getAnnotation(Select.class);
                    if (select != null) {
                        type = QueryInfo.Type.SELECT;
                        generateSelect(entityInfo, q = new StringBuilder(100), method);
                        jpql = q.toString();
                    }
                }
            }
        }

        // The Sorts parameter is from JNoSQL and might get added to Jakarta Data.
        // The @OrderBy annotation from Jakarta Data defines the same information annotatively.
        OrderBy[] orderBy = method.getAnnotationsByType(OrderBy.class);
        if (orderBy.length > 0) {
            type = type == null ? QueryInfo.Type.SELECT : type;
            if (q == null)
                if (jpql == null)
                    generateSelect(entityInfo, q = new StringBuilder(200), method);
                else
                    q = new StringBuilder(jpql);

            for (int i = 0; i < orderBy.length; i++) {
                q.append(i == 0 ? " ORDER BY o." : ", o.").append(orderBy[i].value());
                if (orderBy[i].descending())
                    q.append(" DESC");
            }
        }

        jpql = q == null ? jpql : q.toString();

        return new QueryInfo(type, jpql, entityInfo, saveParamType, returnArrayType);
    }

    private String generateRepositoryQuery(EntityInfo entityInfo, Method method) {
        String methodName = method.getName();
        StringBuilder q;
        if (methodName.startsWith("findBy")) {
            int orderBy = methodName.indexOf("OrderBy");
            generateSelect(entityInfo, q = new StringBuilder(200), method);
            if (orderBy > 6 || orderBy == -1 && methodName.length() > 6) {
                String s = orderBy > 0 ? methodName.substring(6, orderBy) : methodName.substring(6);
                generateRepositoryQueryConditions(entityInfo, s, q);
            }
            if (orderBy >= 6)
                generateRepositoryQueryOrderBy(entityInfo, methodName, orderBy, q);
        } else if (methodName.startsWith("deleteBy")) {
            q = new StringBuilder(150).append("DELETE FROM ").append(entityInfo.name).append(" o");
            if (methodName.length() > 8)
                generateRepositoryQueryConditions(entityInfo, methodName.substring(8), q);
        } else if (methodName.startsWith("updateBy")) {
            q = generateRepositoryUpdateQuery(entityInfo, methodName);
        } else {
            return null;
        }

        System.out.println("Generated query for Repository method " + methodName);
        System.out.println("  " + q);
        return q.toString();
    }

    /**
     * Generates JPQL for a findBy or deleteBy condition such as MyColumn[Not?]Like
     */
    private int generateRepositoryQueryCondition(EntityInfo entityInfo, String expression, StringBuilder q, int paramCount) {
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

        StringBuilder a = new StringBuilder();
        if (upper)
            a.append("UPPER(o.");
        else if (lower)
            a.append("LOWER(o.");
        else
            a.append("o.");

        String name = entityInfo.getAttributeName(attribute);
        a.append(name = name == null ? attribute : name);

        if (upper || lower)
            a.append(")");

        String attributeExpr = a.toString();

        switch (condition) {
            case STARTS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT(?").append(++paramCount).append(", '%')");
                break;
            case ENDS_WITH:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ?").append(++paramCount).append(")");
                break;
            case LIKE:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE ?").append(++paramCount);
                break;
            case BETWEEN:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ?").append(++paramCount).append(" AND ?").append(++paramCount);
                break;
            case CONTAINS:
                if (entityInfo.collectionAttributeNames.contains(name))
                    q.append(" ?").append(++paramCount).append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
                else
                    q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ?").append(++paramCount).append(", '%')");
                break;
            default:
                q.append(attributeExpr).append(negated ? " NOT " : "").append(condition.operator).append('?').append(++paramCount);
        }

        return paramCount;
    }

    /**
     * Generates the JPQL WHERE clause for all findBy, deleteBy, or updateBy conditions such as MyColumn[Not?]Like
     */
    private int generateRepositoryQueryConditions(EntityInfo entityInfo, String conditions, StringBuilder q) {
        int paramCount = 0;
        q.append(" WHERE ");
        for (int and = 0, or = 0, iNext, i = 0; i >= 0; i = iNext) {
            and = and == -1 || and > i ? and : conditions.indexOf("And", i);
            or = or == -1 || or > i ? or : conditions.indexOf("Or", i);
            iNext = Math.min(and, or);
            if (iNext < 0)
                iNext = Math.max(and, or);
            String condition = iNext < 0 ? conditions.substring(i) : conditions.substring(i, iNext);
            paramCount = generateRepositoryQueryCondition(entityInfo, condition, q, paramCount);
            if (iNext > 0) {
                q.append(iNext == and ? " AND " : " OR ");
                iNext += (iNext == and ? 3 : 2);
            }
        }
        return paramCount;
    }

    /**
     * Generates the JPQL ORDER BY clause for a repository findBy method such as findByLastNameLikeOrderByLastNameOrderByFirstName
     */
    private void generateRepositoryQueryOrderBy(EntityInfo entityInfo, String methodName, int orderBy, StringBuilder q) {
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
            String name = entityInfo.getAttributeName(attribute);
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
    private StringBuilder generateRepositoryUpdateQuery(EntityInfo entityInfo, String methodName) {
        int set = methodName.indexOf("Set", 8);
        int add = methodName.indexOf("Add", 8);
        int mul = methodName.indexOf("Multiply", 8);
        int div = methodName.indexOf("Divide", 8);
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
        int paramCount = generateRepositoryQueryConditions(entityInfo, methodName.substring(8, uFirst), where);

        StringBuilder q = new StringBuilder(250);
        q.append("UPDATE ").append(entityInfo.name).append(" o SET");

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
            String name = entityInfo.getAttributeName(attribute);
            q.append(first ? " o." : ", o.").append(name == null ? attribute : name).append("=");

            if (op != null)
                q.append("o.").append(name == null ? attribute : name).append(op);
            q.append('?').append(++paramCount);

            u = next == Integer.MAX_VALUE ? -1 : next;
        }

        return q.append(where);
    }

    private void generateSelect(EntityInfo entityInfo, StringBuilder q, Method method) {
        // TODO entityClass now includes inheritance subtypes and much of the following was already computed.
        Result result = method.getAnnotation(Result.class);
        Select select = method.getAnnotation(Select.class);
        Class<?> type = result == null ? null : result.value();
        String[] cols = select == null ? null : select.value();
        boolean distinct = select != null && select.distinct();
        String function = select == null ? null : toFunctionName(select.function());

        if (type == null) {
            Class<?> returnType = method.getReturnType();
            if (!Iterable.class.isAssignableFrom(returnType)) {
                Class<?> arrayType = returnType.getComponentType();
                returnType = arrayType == null ? returnType : arrayType;
                if (!returnType.isPrimitive()
                    && !returnType.isInterface()
                    && !returnType.isAssignableFrom(entityInfo.type)
                    && !returnType.getName().startsWith("java"))
                    type = returnType;
            }
        }

        q.append("SELECT ");

        if (type == null ||
            entityInfo.inheritance && entityInfo.type.isAssignableFrom(type))
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
                for (String name : entityInfo.attributeNames.values()) {
                    generateSelectExpression(q, first, function, distinct, name);
                    first = false;
                }
            else
                for (int i = 0; i < cols.length; i++) {
                    String name = entityInfo.getAttributeName(cols[i]);
                    generateSelectExpression(q, i == 0, function, distinct, name == null ? cols[i] : name);
                }
            q.append(')');
        }
        q.append(" FROM ").append(entityInfo.name).append(" o");
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

    private String getBuiltInRepositoryQuery(EntityInfo e, String methodName, Class<?>[] paramTypes) {
        if (paramTypes.length == 0) {
            if ("count".equals(methodName))
                return "SELECT COUNT(o) FROM " + e.name + " o";
        } else if (paramTypes.length == 1) {
            if ("save".equals(methodName))
                return null; // default handling covers this
            if (Iterable.class.equals(paramTypes[0])) {
                if ("findById".equals(methodName))
                    return "SELECT o FROM " + e.name + " o WHERE o." + e.keyName + " IN ?1";
                else if ("deleteById".equals(methodName))
                    return "DELETE FROM " + e.name + " o WHERE o." + e.keyName + " IN ?1";
            } else {
                if ("findById".equals(methodName))
                    return "SELECT o FROM " + e.name + " o WHERE o." + e.keyName + "=?1";
                else if ("existsById".equals(methodName))
                    return "SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END FROM " + e.name + " o WHERE o." + e.keyName + "=?1";
                else if ("deleteById".equals(methodName))
                    return "DELETE FROM " + e.name + " o WHERE o." + e.keyName + "=?1";
            }
        }
        throw new UnsupportedOperationException("Repository method " + methodName + " with parameters " + Arrays.toString(paramTypes));
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
            Tr.entry(this, tc, "invoke " + method.getName(), args);
        try {
            QueryInfo queryInfo = queryInfoFuture.join();

            int paramCount;
            Collector<Object, Object, Object> collector = null;
            Consumer<Object> consumer = null;
            Pagination pagination = null;
            Sorts sorts = null;

            // Jakarta NoSQL allows the last 3 parameter positions to be used for Pagination, Sorts, and Consumer
            // Collector is added here for experimentation.
            for (paramCount = args == null ? 0 : args.length; paramCount > 0 && paramCount > args.length - 3;) {
                Object param = args[--paramCount];
                if (param instanceof Collector)
                    collector = (Collector<Object, Object, Object>) param;
                else if (param instanceof Consumer)
                    consumer = (Consumer<Object>) param;
                else if (param instanceof Pagination)
                    pagination = (Pagination) param;
                else if (param instanceof Sort)
                    sorts = Sorts.sorts().add((Sort) param);
                else if (param instanceof Sorts)
                    sorts = (Sorts) param;
                else {
                    paramCount++;
                    break;
                }
            }

            if (sorts != null) {
                boolean first = true;
                StringBuilder q = queryInfo.jpql == null ? new StringBuilder(200) : new StringBuilder(queryInfo.jpql);
                if (queryInfo.jpql == null)
                    generateSelect(queryInfo.entityInfo, q, method);
                for (Sort sort : sorts.getSorts()) {
                    q.append(first ? " ORDER BY o." : ", o.").append(sort.getName());
                    if (sort.getType() == SortType.DESC)
                        q.append(" DESC");
                    first = false;
                }
                queryInfo = new QueryInfo(queryInfo.type, q.toString(), queryInfo.entityInfo, queryInfo.saveParamType, queryInfo.returnArrayType);
            }

            // The Pagination parameter is from JNoSQL.
            // The @Paginated annotation is not - I just wanted to experiment with how it could work
            // if defined annotatively, which turns out to be possible, but not as flexible.
            if (pagination == null) {
                Paginated paginated = method.getAnnotation(Paginated.class);
                if (paginated != null)
                    pagination = Pagination.page(1).size(paginated.value());
            }

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

            LocalTransactionCoordinator suspendedLTC = null;
            EntityManager em = null;
            Object returnValue;
            Class<?> returnType = method.getReturnType();
            boolean failed = true;
            try {
                if (requiresTransaction) {
                    suspendedLTC = provider.localTranCurrent.suspend();
                    provider.tranMgr.begin();
                }

                em = queryInfo.entityInfo.persister.createEntityManager();

                switch (queryInfo.type) {
                    case MERGE:
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
                    case SELECT:
                        TypedQuery<?> query = em.createQuery(queryInfo.jpql, queryInfo.entityInfo.type);
                        if (args != null) {
                            Parameter[] params = method.getParameters();
                            for (int i = 0; i < paramCount; i++) {
                                Param param = params[i].getAnnotation(Param.class);
                                if (param == null)
                                    query.setParameter(i + 1, args[i]);
                                else // named parameter
                                    query.setParameter(param.value(), args[i]);
                            }
                        }

                        if (pagination == null) {
                            Limit limit = method.getAnnotation(Limit.class);
                            if (limit != null)
                                query.setMaxResults(limit.value());
                        } else {
                            // TODO possible overflow with both of these. And what is the difference between getPageSize/getLimit?
                            query.setFirstResult((int) pagination.getSkip());
                            query.setMaxResults((int) pagination.getPageSize());
                        }

                        List<?> results = query.getResultList();

                        if (collector != null) {
                            // Collector is more useful on the other path, when combined with pagination
                            Object r = collector.supplier().get();
                            BiConsumer<Object, Object> accumulator = collector.accumulator();
                            for (Object item : results)
                                accumulator.accept(r, item);
                            returnValue = collector.finisher().apply(r);
                            if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType))
                                returnValue = CompletableFuture.completedFuture(returnValue);
                        } else if (consumer != null) {
                            for (Object result : results)
                                consumer.accept(result);
                            return CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType) //
                                            ? CompletableFuture.completedFuture(null) //
                                            : null;
                        } else if (queryInfo.entityInfo.type.equals(returnType)) {
                            returnValue = results.isEmpty() ? null : results.get(0);
                        } else if (returnType.isInstance(results)) {
                            returnValue = results;
                        } else if (queryInfo.returnArrayType != null) {
                            Object r = Array.newInstance(queryInfo.returnArrayType, results.size());
                            int i = 0;
                            for (Object o : results)
                                Array.set(r, i++, o);
                            returnValue = r;
                        } else if (Optional.class.equals(returnType)) {
                            returnValue = results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
                        } else if (Collection.class.isAssignableFrom(returnType)) {
                            try {
                                @SuppressWarnings("unchecked")
                                Constructor<? extends Collection<Object>> c = (Constructor<? extends Collection<Object>>) returnType.getConstructor();
                                Collection<Object> list = c.newInstance();
                                list.addAll(results);
                                returnValue = list;
                            } catch (NoSuchMethodException x) {
                                throw new UnsupportedOperationException(returnType + " lacks public zero parameter constructor.");
                            }
                        } else if (Stream.class.isAssignableFrom(returnType)) {
                            Stream.Builder<Object> builder = Stream.builder();
                            for (Object result : results)
                                builder.accept(result);
                            returnValue = builder.build();
                        } else if (IntStream.class.isAssignableFrom(returnType)) {
                            IntStream.Builder builder = IntStream.builder();
                            for (Object result : results)
                                builder.accept((Integer) result);
                            returnValue = builder.build();
                        } else if (LongStream.class.isAssignableFrom(returnType)) {
                            LongStream.Builder builder = LongStream.builder();
                            for (Object result : results)
                                builder.accept((Long) result);
                            returnValue = builder.build();
                        } else if (DoubleStream.class.isAssignableFrom(returnType)) {
                            DoubleStream.Builder builder = DoubleStream.builder();
                            for (Object result : results)
                                builder.accept((Double) result);
                            returnValue = builder.build();
                        } else if (CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType)) {
                            Limit limit = method.getAnnotation(Limit.class);
                            if (limit != null && limit.value() == 1) // single result
                                returnValue = CompletableFuture.completedFuture(results.isEmpty() ? null : results.get(0));
                            else // multiple
                                returnValue = CompletableFuture.completedFuture(results);
                        } else if (results.isEmpty()) {
                            returnValue = null;
                        } else if (results.size() == 1) {
                            // single result
                            returnValue = results.get(0);
                            if (returnValue != null && !returnType.isAssignableFrom(returnValue.getClass())) {
                                // TODO these conversions are not all safe
                                if (double.class.equals(returnType) || Double.class.equals(returnType))
                                    returnValue = ((Number) returnValue).doubleValue();
                                else if (float.class.equals(returnType) || Float.class.equals(returnType))
                                    returnValue = ((Number) returnValue).floatValue();
                                else if (long.class.equals(returnType) || Long.class.equals(returnType))
                                    returnValue = ((Number) returnValue).longValue();
                                else if (int.class.equals(returnType) || Integer.class.equals(returnType))
                                    returnValue = ((Number) returnValue).intValue();
                                else if (short.class.equals(returnType) || Short.class.equals(returnType))
                                    returnValue = ((Number) returnValue).shortValue();
                                else if (byte.class.equals(returnType) || Byte.class.equals(returnType))
                                    returnValue = ((Number) returnValue).byteValue();
                            }
                        } else { // TODO convert other return types?
                            returnValue = results;
                        }
                        break;
                    case UPDATE:
                    case DELETE:
                        jakarta.persistence.Query update = em.createQuery(queryInfo.jpql);
                        if (args != null) {
                            Parameter[] params = method.getParameters();
                            for (int i = 0; i < args.length; i++) {
                                Param param = params[i].getAnnotation(Param.class);
                                if (param == null)
                                    update.setParameter(i + 1, args[i]);
                                else // named parameter
                                    update.setParameter(param.value(), args[i]);
                            }
                        }

                        int updateCount = update.executeUpdate();

                        returnValue = toReturnValue(updateCount, returnType);
                        break;
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
                Tr.exit(this, tc, "invoke " + method.getName(), returnValue);
            return returnValue;
        } catch (Throwable x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + method.getName(), x);
            throw x;
        }
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

    private static final Object toReturnValue(int i, Class<?> returnType) {
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
            result = CompletableFuture.completedFuture(Long.valueOf(i)); // default for completion stages
        else
            throw new UnsupportedOperationException("Return update count as " + returnType);

        return result;
    }
}
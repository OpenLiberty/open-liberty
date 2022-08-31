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
package io.openliberty.data.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

import jakarta.enterprise.inject.spi.Bean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;

import io.openliberty.data.Data;
import io.openliberty.data.Delete;
import io.openliberty.data.Inheritance;
import io.openliberty.data.Limit;
import io.openliberty.data.OrderBy;
import io.openliberty.data.Page;
import io.openliberty.data.Paginated;
import io.openliberty.data.Pagination;
import io.openliberty.data.Param;
import io.openliberty.data.Query;
import io.openliberty.data.Repository;
import io.openliberty.data.Result;
import io.openliberty.data.Select;
import io.openliberty.data.Select.Aggregate;
import io.openliberty.data.Sort;
import io.openliberty.data.SortType;
import io.openliberty.data.Sorts;
import io.openliberty.data.Update;
import io.openliberty.data.Where;

public class QueryHandler<T> implements InvocationHandler {
    private static enum Condition {
        BETWEEN(null, 7),
        CONTAINS(null, 8),
        ENDS_WITH(null, 8),
        EQUALS("=", 0),
        GREATER_THAN(">", 11),
        GREATER_THAN_EQUAL(">=", 16),
        IN(" IN ", 2),
        LESS_THAN("<", 8),
        LESS_THAN_EQUAL("<=", 13),
        LIKE(null, 4),
        NOT_EQUALS("<>", 3),
        STARTS_WITH(null, 10);

        final int length;
        final String operator;

        Condition(String operator, int length) {
            this.operator = operator;
            this.length = length;
        }

        Condition negate() {
            switch (this) {
                case EQUALS:
                    return NOT_EQUALS;
                case GREATER_THAN:
                    return LESS_THAN_EQUAL;
                case GREATER_THAN_EQUAL:
                    return LESS_THAN;
                case LESS_THAN:
                    return GREATER_THAN_EQUAL;
                case LESS_THAN_EQUAL:
                    return GREATER_THAN;
                case NOT_EQUALS:
                    return EQUALS;
                default:
                    return null;
            }
        }
    }

    private static enum QueryType {
        DELETE, MERGE, SELECT, UPDATE
    }

    private final Class<T> beanClass;
    private final Data data;
    private final Class<?> defaultEntityClass; // repository methods can return subclasses in the case of @Inheritance
    private final boolean inheritance;
    private final DataPersistence persistence;

    @SuppressWarnings("unchecked")
    public QueryHandler(Bean<T> bean, Class<?> entityClass) {
        beanClass = (Class<T>) bean.getBeanClass();
        data = beanClass.getAnnotation(Data.class);
        defaultEntityClass = entityClass;
        inheritance = entityClass.getAnnotation(Inheritance.class) != null ||
                      entityClass.getAnnotation(jakarta.persistence.Inheritance.class) != null;

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        persistence = bc.getService(bc.getServiceReference(DataPersistence.class));
    }

    private String getBuiltInRepositoryQuery(EntityInfo e, String methodName, Object[] args, Class<?>[] paramTypes) {
        if (args == null) {
            if ("count".equals(methodName))
                return "SELECT COUNT(o) FROM " + e.name + " o";
        } else if (args.length == 1) {
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
    private int generateRepositoryQueryCondition(Class<?> entityClass, String expression, StringBuilder q, int paramCount) {
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

        String name = persistence.getAttributeName(attribute, entityClass, data.provider());
        a.append(name == null ? attribute : name);

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
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("LIKE CONCAT('%', ?").append(++paramCount).append(", '%')");
                break;
            case BETWEEN:
                q.append(attributeExpr).append(negated ? " NOT " : " ").append("BETWEEN ?").append(++paramCount).append(" AND ?").append(++paramCount);
                break;
            case CONTAINS:
                q.append(" ?").append(++paramCount).append(negated ? " NOT " : " ").append("MEMBER OF ").append(attributeExpr);
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
            paramCount = generateRepositoryQueryCondition(entityInfo.type, condition, q, paramCount);
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
            String name = persistence.getAttributeName(attribute, entityInfo.type, data.provider());
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
            String name = persistence.getAttributeName(attribute, entityInfo.type, data.provider());
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
            inheritance && entityInfo.type.isAssignableFrom(type))
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
                for (String name : persistence.getAttributeNames(entityInfo.type, data.provider())) {
                    generateSelectExpression(q, first, function, distinct, name);
                    first = false;
                }
            else
                for (int i = 0; i < cols.length; i++) {
                    String name = persistence.getAttributeName(cols[i], entityInfo.type, data.provider());
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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // TODO a real implementation would pre-compute and cache most of what this method does instead of repeating it every time
        String methodName = method.getName();
        Class<?>[] paramTypes = method.getParameterTypes();
        int paramCount = paramTypes.length;

        if (args == null) {
            if ("hashCode".equals(methodName))
                return System.identityHashCode(proxy);
            else if ("toString".equals(methodName))
                return beanClass.getName() + "[QueryHandler]@" + Integer.toHexString(System.identityHashCode(proxy));
        } else if (args.length == 1) {
            if ("equals".equals(methodName))
                return proxy == args[0];
        }

        System.out.println("Handler invoke " + method);

        Object returnValue;
        Class<?> returnType = method.getReturnType();
        Class<?> returnArrayType = returnType.getComponentType();

        Result resultAnno = method.getAnnotation(Result.class);
        Class<?> resultType = resultAnno == null ? null : resultAnno.value();

        Class<?> entityClass = resultType == null //
                        ? returnArrayType == null ? returnType : returnArrayType // computed from return type
                        : resultType;
        if (!inheritance || !defaultEntityClass.isAssignableFrom(entityClass)) // TODO allow other entity types from model
            entityClass = defaultEntityClass;

        EntityInfo entityInfo = persistence.getEntityInfo(data.provider(), entityClass);

        QueryType queryType;
        boolean requiresTransaction;
        Collector<Object, Object, Object> collector = null;
        Consumer<Object> consumer = null;
        Pagination pagination = null;
        Sorts sorts = null;

        // @Query annotation
        Query fullQuery = method.getAnnotation(Query.class);
        String jpql = fullQuery == null ? null : fullQuery.value();

        // Repository built-in methods
        if (jpql == null && Repository.class.equals(method.getDeclaringClass()))
            jpql = getBuiltInRepositoryQuery(entityInfo, methodName, args, paramTypes);

        // @Delete/@Update/@Where/@OrderBy annotations
        if (jpql == null) {
            Update update = method.getAnnotation(Update.class);
            Where where = method.getAnnotation(Where.class);
            if (update == null) {
                if (method.getAnnotation(Delete.class) == null) {
                    if (where != null) {
                        StringBuilder q = new StringBuilder(200);
                        generateSelect(entityInfo, q, method);
                        q.append(" WHERE ").append(where.value());
                        jpql = q.toString();
                    }
                } else {
                    StringBuilder q = new StringBuilder(200);
                    q.append("DELETE FROM ").append(entityInfo.name).append(" o");
                    if (where != null)
                        q.append(" WHERE ").append(where.value());
                    jpql = q.toString();
                }
            } else {
                StringBuilder q = new StringBuilder(200);
                q.append("UPDATE ").append(entityInfo.name).append(" o SET ").append(update.value());
                if (where != null)
                    q.append(" WHERE ").append(where.value());
                jpql = q.toString();
            }
        }

        // Repository method name pattern queries
        if (jpql == null)
            jpql = generateRepositoryQuery(entityInfo, method);

        // @Select annotation only
        if (jpql == null) {
            Select select = method.getAnnotation(Select.class);
            if (select != null) {
                StringBuilder q = new StringBuilder(100);
                generateSelect(entityInfo, q, method);
                jpql = q.toString();
            }
        }

        // Jakarta NoSQL allows the last 3 parameter positions to be used for Pagination, Sorts, and Consumer
        // Collector is added here for experimentation.
        for (paramCount = paramTypes.length; paramCount > 0 && paramCount > paramTypes.length - 3;) {
            Class<?> type = paramTypes[paramCount - 1];
            if (Collector.class.equals(type))
                collector = (Collector<Object, Object, Object>) args[--paramCount];
            else if (Consumer.class.equals(type))
                consumer = (Consumer<Object>) args[--paramCount];
            else if (Pagination.class.equals(type))
                pagination = (Pagination) args[--paramCount];
            else if (Sort.class.equals(type))
                sorts = Sorts.sorts().add((Sort) args[--paramCount]);
            else if (Sorts.class.equals(type))
                sorts = (Sorts) args[--paramCount];
            else
                break;
        }

        // The Sorts parameter is from JNoSQL
        // The @OrderBy annotation is not. It's an experiment with defining the same information annotatively.
        OrderBy[] orderBy = method.getAnnotationsByType(OrderBy.class);
        if (orderBy.length > 0) {
            StringBuilder q = jpql == null ? new StringBuilder(200) : new StringBuilder(jpql);
            if (jpql == null)
                generateSelect(entityInfo, q, method);
            for (int i = 0; i < orderBy.length; i++) {
                q.append(i == 0 ? " ORDER BY o." : ", o.").append(orderBy[i].value());
                if (orderBy[i].descending())
                    q.append(" DESC");
            }
            jpql = q.toString();
        }

        if (sorts != null) {
            boolean first = true;
            StringBuilder q = jpql == null ? new StringBuilder(200) : new StringBuilder(jpql);
            if (jpql == null)
                generateSelect(entityInfo, q, method);
            for (Sort sort : sorts.getSorts()) {
                q.append(first ? " ORDER BY o." : ", o.").append(sort.getName());
                if (sort.getType() == SortType.DESC)
                    q.append(" DESC");
                first = false;
            }
            jpql = q.toString();
        }

        // The Pagination parameter is from JNoSQL.
        // The @Paginated annotation is not - I just wanted to experiment with how it could work
        // if defined annotatively, which turns out to be possible, but not as flexible.
        if (pagination == null) {
            Paginated paginated = method.getAnnotation(Paginated.class);
            if (paginated != null)
                pagination = Pagination.page(1).size(paginated.value());
        }

        boolean asyncCompatibleResultForPagination = pagination != null &&
                                                     (void.class.equals(returnType) || CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType));

        if (asyncCompatibleResultForPagination && collector != null)
            return runAndCollect(jpql, pagination, collector, entityInfo, method, paramCount, args, returnType);
        else if (asyncCompatibleResultForPagination && consumer != null)
            return runWithConsumer(jpql, pagination, consumer, entityInfo, method, paramCount, args, returnType);
        else if (pagination != null && Iterator.class.equals(returnType))
            return new PaginatedIterator<T>(jpql, pagination, entityInfo, method, paramCount, args);
        else if (Page.class.equals(returnType))
            return new PageImpl<T>(jpql, pagination, entityInfo, method, paramCount, args);
        else if (Publisher.class.equals(returnType))
            return new PublisherImpl<T>(jpql, persistence.executor, entityInfo, method, paramCount, args);

        // TODO Actual implementation is lacking so we are cheating by
        // temporarily sending in the JPQL directly:
        if (jpql == null) {
            queryType = QueryType.MERGE;
            requiresTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus();
        } else {
            String q = jpql.toUpperCase();
            if (q.startsWith("SELECT")) {
                queryType = QueryType.SELECT;
                requiresTransaction = false;
            } else if (q.startsWith("UPDATE")) {
                queryType = QueryType.UPDATE;
                requiresTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus();
            } else if (q.startsWith("DELETE")) {
                queryType = QueryType.DELETE;
                requiresTransaction = Status.STATUS_NO_TRANSACTION == persistence.tranMgr.getStatus();
            } else {
                throw new UnsupportedOperationException(jpql);
            }
        }

        LocalTransactionCoordinator suspendedLTC = null;
        EntityManager em = null;
        boolean failed = true;
        try {
            if (requiresTransaction) {
                suspendedLTC = persistence.localTranCurrent.suspend();
                persistence.tranMgr.begin();
            }

            em = entityInfo.persister.createEntityManager();

            switch (queryType) {
                case MERGE:
                    if (Iterable.class.isAssignableFrom(paramTypes[0])) {
                        ArrayList<Object> results = new ArrayList<>();
                        for (Object e : ((Iterable<?>) args[0]))
                            results.add(em.merge(e));
                        em.flush();
                        returnValue = results;
                    } else if (paramTypes[0].isArray()) {
                        ArrayList<Object> results = new ArrayList<>();
                        Object a = args[0];
                        int length = Array.getLength(a);
                        for (int i = 0; i < length; i++)
                            results.add(em.merge(Array.get(a, i)));
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
                    TypedQuery<?> query = em.createQuery(jpql, entityClass);
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
                    } else if (entityClass.equals(returnType)) {
                        returnValue = results.isEmpty() ? null : results.get(0);
                    } else if (returnType.isInstance(results)) {
                        returnValue = results;
                    } else if (returnArrayType != null) {
                        Object r = Array.newInstance(returnArrayType, results.size());
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
                    jakarta.persistence.Query update = em.createQuery(jpql);
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

                    returnValue = toReturnValue(updateCount, resultType, returnType);
                    break;
                default:
                    throw new UnsupportedOperationException(queryType.name());
            }

            failed = false;
        } finally {
            if (em != null)
                em.close();

            if (requiresTransaction) {
                try {
                    int status = persistence.tranMgr.getStatus();
                    if (status == Status.STATUS_MARKED_ROLLBACK || failed)
                        persistence.tranMgr.rollback();
                    else if (status != Status.STATUS_NO_TRANSACTION)
                        persistence.tranMgr.commit();
                } finally {
                    if (suspendedLTC != null)
                        persistence.localTranCurrent.resume(suspendedLTC);
                }
            } else {
                if (failed && Status.STATUS_ACTIVE == persistence.tranMgr.getStatus())
                    persistence.tranMgr.setRollbackOnly();
            }
        }
        return returnValue;
    }

    /**
     * This is an experiment with allowing a collector to perform reduction
     * on the contents of each page as the page is read in. This avoids having
     * all pages loaded at once and gives the application a completion stage
     * with a final result that can be awaited, or to which dependent stages
     * can be added to run once the final result is ready.
     *
     * @param jpql
     * @param pagination
     * @param collector
     * @param entityInfo
     * @param method
     * @param paramCount
     * @param args
     * @param returnType
     * @return completion stage that is already completed if only being used to
     *         supply the result to an Asynchronous method. If the database supports
     *         asynchronous patterns, it could be a not-yet-completed completion stage
     *         that is controlled by the database's async support.
     */
    private CompletableFuture<Object> runAndCollect(String jpql, Pagination pagination,
                                                    Collector<Object, Object, Object> collector, EntityInfo entityInfo,
                                                    Method method, int numParams, Object[] args, Class<?> returnType) {

        Object r = collector.supplier().get();
        BiConsumer<Object, Object> accumulator = collector.accumulator();

        // TODO it would be possible to process multiple pages in parallel if we wanted to and if the collector supports it
        EntityManager em = entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, entityInfo.type);
            if (args != null) {
                Parameter[] params = method.getParameters();
                for (int i = 0; i < numParams; i++) {
                    Param param = params[i].getAnnotation(Param.class);
                    if (param == null)
                        query.setParameter(i + 1, args[i]);
                    else // named parameter
                        query.setParameter(param.value(), args[i]);
                }
            }

            List<T> page;
            int maxResults;
            do {
                // TODO possible overflow with both of these. And what is the difference between getPageSize/getLimit?
                query.setFirstResult((int) pagination.getSkip());
                query.setMaxResults(maxResults = (int) pagination.getPageSize());
                pagination = pagination.next();

                page = query.getResultList();

                for (Object item : page)
                    accumulator.accept(r, item);

                System.out.println("Processed page with " + page.size() + " results");
            } while (pagination != null && page.size() == maxResults);
        } finally {
            em.close();
        }

        return void.class.equals(returnType) ? null : CompletableFuture.completedFuture(collector.finisher().apply(r));
    }

    /**
     * Copies the Jakarta NoSQL pattern of invoking a Consumer with the value of each result.
     *
     * @param jpql
     * @param pagination
     * @param consumer
     * @param entityInfo
     * @param method
     * @param paramCount
     * @param args
     * @param returnType
     * @return completion stage that is already completed if only being used to
     *         run as an Asynchronous method. If the database supports
     *         asynchronous patterns, it could be a not-yet-completed completion stage
     *         that is controlled by the database's async support.
     */
    private CompletableFuture<Void> runWithConsumer(String jpql, Pagination pagination, Consumer<Object> consumer, EntityInfo entityInfo,
                                                    Method method, int numParams, Object[] args, Class<?> returnType) {

        // TODO it would be possible to process multiple pages in parallel if we wanted to and if the consumer supports it
        EntityManager em = entityInfo.persister.createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            TypedQuery<T> query = (TypedQuery<T>) em.createQuery(jpql, entityInfo.type);
            if (args != null) {
                Parameter[] params = method.getParameters();
                for (int i = 0; i < numParams; i++) {
                    Param param = params[i].getAnnotation(Param.class);
                    if (param == null)
                        query.setParameter(i + 1, args[i]);
                    else // named parameter
                        query.setParameter(param.value(), args[i]);
                }
            }

            List<T> page;
            int maxResults;
            do {
                // TODO possible overflow with both of these. And what is the difference between getPageSize/getLimit?
                query.setFirstResult((int) pagination.getSkip());
                query.setMaxResults(maxResults = (int) pagination.getPageSize());
                pagination = pagination.next();

                page = query.getResultList();

                for (Object item : page)
                    consumer.accept(item);

                System.out.println("Processed page with " + page.size() + " results");
            } while (pagination != null && page.size() == maxResults);
        } finally {
            em.close();
        }

        return void.class.equals(returnType) ? null : CompletableFuture.completedFuture(null);
    }

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

    private static final Object toReturnValue(int i, Class<?> resultType, Class<?> returnType) {
        if (resultType == null)
            resultType = returnType;

        boolean returnsCompletionStage = CompletableFuture.class.equals(returnType) || CompletionStage.class.equals(returnType);

        Object result;
        if (int.class.equals(resultType) || Integer.class.equals(resultType) || Number.class.equals(resultType))
            result = i;
        else if (long.class.equals(resultType) || Long.class.equals(resultType))
            result = Long.valueOf(i);
        else if (boolean.class.equals(resultType) || Boolean.class.equals(resultType))
            result = i != 0;
        else if (void.class.equals(resultType) || Void.class.equals(resultType))
            result = null;
        else if (returnsCompletionStage && resultType.equals(returnType))
            result = Long.valueOf(i); // default for completion stages that do not specify @Result
        else
            throw new UnsupportedOperationException("Return update count as " + returnType);

        if (returnsCompletionStage)
            result = CompletableFuture.completedFuture(result);

        return result;
    }
}
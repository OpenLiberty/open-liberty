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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Status;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.LocalTransaction.LocalTransactionCoordinator;
import com.ibm.wsspi.persistence.PersistenceServiceUnit;

import io.openliberty.data.Data;
import io.openliberty.data.Delete;
import io.openliberty.data.Param;
import io.openliberty.data.Query;
import io.openliberty.data.Repository;
import io.openliberty.data.Select;
import io.openliberty.data.Update;
import io.openliberty.data.Where;

public class QueryHandler<T> implements InvocationHandler {
    private static enum Condition {
        BETWEEN(null, 7),
        EQUALS("=", 0),
        GREATER_THAN(">", 11),
        GREATER_THAN_EQUAL(">=", 16),
        IN(" IN ", 2),
        LESS_THAN("<", 8),
        LESS_THAN_EQUAL("<=", 13),
        LIKE(null, 4),
        NOT_EQUALS("<>", 3);

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

    private final Map<String, String> attributeNames = new LinkedHashMap<>();
    private final Class<T> beanClass;
    private final Data data;
    private final Class<?> entityClass;
    private final Set<Class<?>> entityClassesAvailable; // TODO is this information needed?
    private final String entityName;
    private final String keyAttribute;
    private final DataPersistence persistence;
    private final PersistenceServiceUnit punit;

    @SuppressWarnings("unchecked")
    public QueryHandler(Bean<T> bean, Class<?> entityClass, String keyAttribute) {
        beanClass = (Class<T>) bean.getBeanClass();
        data = beanClass.getAnnotation(Data.class);
        this.entityClass = entityClass;
        this.entityName = entityClass.getSimpleName();
        this.keyAttribute = keyAttribute;

        BundleContext bc = FrameworkUtil.getBundle(DataPersistence.class).getBundleContext();
        persistence = bc.getService(bc.getServiceReference(DataPersistence.class));

        Entry<PersistenceServiceUnit, Set<Class<?>>> persistenceInfo = //
                        persistence.getPersistenceInfo(data.provider(), beanClass.getClassLoader());
        if (persistenceInfo == null)
            throw new RuntimeException("Persistence layer unavailable for " + data);
        punit = persistenceInfo.getKey();
        entityClassesAvailable = persistenceInfo.getValue();

        // TODO replace this ugly code that maps from Java field or setter attribute name to database column name.
        EntityManager em = punit.createEntityManager();
        try {
            Class<?> Session = em.getClass().getClassLoader().loadClass("org.eclipse.persistence.sessions.Session");
            Object session = em.unwrap(Session);
            Object classDescriptor = session.getClass().getMethod("getDescriptor", Class.class).invoke(session, entityClass);
            Vector<?> databaseMappings = (Vector<?>) classDescriptor.getClass().getMethod("getMappings").invoke(classDescriptor);
            for (Object mapping : databaseMappings) {
                String attributeName = (String) mapping.getClass().getMethod("getAttributeName").invoke(mapping);
                Object databaseField = mapping.getClass().getMethod("getField").invoke(mapping);
                String columnName = (String) databaseField.getClass().getMethod("getName").invoke(databaseField);
                System.out.println("Attribute: " + attributeName + "; Column: " + columnName);
                attributeNames.put(attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1), attributeName);
            }
            System.out.println(attributeNames);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        } finally {
            em.close();
        }

    }

    private String getBuiltInRepositoryQuery(String methodName, Object[] args, Class<?>[] paramTypes) {
        if (args == null) {
            if ("count".equals(methodName))
                return "SELECT COUNT(o) FROM " + entityName + " o";
        } else if (args.length == 1) {
            if ("save".equals(methodName))
                return null; // default handling covers this
            if (Iterable.class.equals(paramTypes[0])) {
                if ("findById".equals(methodName))
                    return "SELECT o FROM " + entityName + " o WHERE o." + keyAttribute + " IN ?1";
                else if ("deleteById".equals(methodName))
                    return "DELETE FROM " + entityName + " o WHERE o." + keyAttribute + " IN ?1";
            } else {
                if ("findById".equals(methodName))
                    return "SELECT o FROM " + entityName + " o WHERE o." + keyAttribute + "=?1";
                else if ("existsById".equals(methodName))
                    return "SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END FROM " + entityName + " o WHERE o." + keyAttribute + "=?1";
                else if ("deleteById".equals(methodName))
                    return "DELETE FROM " + entityName + " o WHERE o." + keyAttribute + "=?1";
            }
        }
        throw new UnsupportedOperationException("Repository method " + methodName + " with parameters " + Arrays.toString(paramTypes));
    }

    private String generateRepositoryQuery(Method method) {
        String methodName = method.getName();
        int start = methodName.startsWith("findBy") ? 6 //
                        : methodName.startsWith("deleteBy") ? 8 //
                                        : -1;
        if (start > 0) {
            StringBuilder q = new StringBuilder(200);
            if (start == 6) { // findBy
                generateSelect(q, method);
            } else {
                q.append("DELETE FROM ").append(entityName).append(" o");
            }
            q.append(" WHERE ");

            int orderBy = methodName.indexOf("OrderBy");
            String s = orderBy > 0 ? methodName.substring(start, orderBy) : methodName.substring(start);
            for (int paramCount = 0, and = 0, or = 0, iNext, i = 0; i >= 0; i = iNext) {
                and = and == -1 || and > i ? and : s.indexOf("And", i);
                or = or == -1 || or > i ? or : s.indexOf("Or", i);
                iNext = Math.min(and, or);
                if (iNext < 0)
                    iNext = Math.max(and, or);
                String condition = iNext < 0 ? s.substring(i) : s.substring(i, iNext);
                paramCount = generateRepositoryQueryCondition(condition, q, paramCount);
                if (iNext > 0) {
                    q.append(iNext == and ? " AND " : " OR ");
                    iNext += (iNext == and ? 3 : 2);
                }
            }

            if (orderBy > 0) {
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
                    String name = attributeNames.get(attribute);
                    q.append("o.").append(name == null ? attribute : name);

                    if (desc)
                        q.append(" DESC");
                    if (orderBy > 0)
                        q.append(", ");
                } while (orderBy > 0);
            }

            System.out.println("Generated query for Repository method " + methodName);
            System.out.println("  " + q);
            return q.toString();
        }
        return null;
    }

    /**
     * Generates JPQL for a findBy or deleteBy condition such as MyColumn[Not?]Like
     */
    private int generateRepositoryQueryCondition(String expression, StringBuilder q, int paramCount) {
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

        String name = attributeNames.get(attribute);
        q.append("o.").append(name == null ? attribute : name);

        if (negated)
            q.append(" NOT");

        switch (condition) {
            case LIKE:
                q.append(" LIKE CONCAT('%', ?").append(++paramCount).append(", '%')");
                break;
            case BETWEEN:
                q.append(" BETWEEN ?").append(++paramCount).append(" AND ?").append(++paramCount);
                break;
            default:
                q.append(condition.operator).append('?').append(++paramCount);
        }

        return paramCount;
    }

    private void generateSelect(StringBuilder q, Method method) {
        Select select = method.getAnnotation(Select.class);
        Class<?> type = select == null ? null : select.type();
        String[] cols = select == null ? null : select.value();
        if (type == null || Select.AutoDetect.class.equals(type)) {
            Class<?> returnType = method.getReturnType();
            if (!Iterable.class.isAssignableFrom(returnType)) {
                Class<?> arrayType = returnType.getComponentType();
                returnType = arrayType == null ? returnType : arrayType;
                if (!returnType.isPrimitive()
                    && !returnType.isAssignableFrom(entityClass)
                    && !returnType.getName().startsWith("java"))
                    type = returnType;
            }
        }
        if (type == null || Select.AutoDetect.class.equals(type))
            if (cols == null || cols.length == 0) {
                q.append("SELECT o FROM ");
            } else {
                q.append("SELECT");
                for (int i = 0; i < cols.length; i++)
                    q.append(i == 0 ? " o." : ", o.").append(cols[i]);
                q.append(" FROM ");
            }
        else {
            q.append("SELECT NEW ").append(type.getName());
            boolean first = true;
            if (cols == null || cols.length == 0)
                for (String name : attributeNames.values()) {
                    q.append(first ? "(o." : ", o.").append(name);
                    first = false;
                }
            else
                for (int i = 0; i < cols.length; i++)
                    q.append(i == 0 ? "(o." : ", o.").append(cols[i]);
            q.append(") FROM ");
        }
        q.append(entityName).append(" o");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

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

        Class<?> returnType = method.getReturnType();
        Object returnValue;
        QueryType queryType;
        boolean requiresTransaction;

        // @Query annotation
        Query dataQuery = method.getAnnotation(Query.class);
        String jpql = dataQuery == null ? null : dataQuery.value();

        // Repository built-in methods
        if (jpql == null && Repository.class.equals(method.getDeclaringClass()))
            jpql = getBuiltInRepositoryQuery(methodName, args, method.getParameterTypes());

        // @Delete/@Update/@Where annotations
        if (jpql == null) {
            Update update = method.getAnnotation(Update.class);
            Where where = method.getAnnotation(Where.class);
            if (update == null) {
                if (method.getAnnotation(Delete.class) == null) {
                    if (where != null) {
                        StringBuilder q = new StringBuilder(200);
                        generateSelect(q, method);
                        q.append(" WHERE ").append(where.value());
                        jpql = q.toString();
                    }
                } else {
                    StringBuilder q = new StringBuilder(200);
                    q.append("DELETE FROM ").append(entityName).append(" o");
                    if (where != null)
                        q.append(" WHERE ").append(where.value());
                    jpql = q.toString();
                }
            } else {
                StringBuilder q = new StringBuilder(200);
                q.append("UPDATE ").append(entityName).append(" o SET ").append(update.value());
                if (where != null)
                    q.append(" WHERE ").append(where.value());
                jpql = q.toString();
            }
        }

        // Repository method name pattern queries
        if (jpql == null)
            jpql = generateRepositoryQuery(method);

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

            em = punit.createEntityManager();

            switch (queryType) {
                case MERGE:
                    if (entityClassesAvailable.contains(args[0].getClass()) ||
                        entityClassesAvailable.contains(method.getParameterTypes()[0])) {
                        returnValue = em.merge(args[0]);
                        em.flush();
                        returnValue = returnType.isInstance(returnValue) ? returnValue : null;
                    } else if (Iterable.class.isAssignableFrom(method.getParameterTypes()[0])) {
                        ArrayList<Object> results = new ArrayList<>();
                        for (Object e : ((Iterable<?>) args[0]))
                            results.add(em.merge(e));
                        em.flush();
                        returnValue = returnType.isInstance(results) ? results : null;
                    } else {
                        throw new UnsupportedOperationException(method.toString());
                    }
                    break;
                case SELECT:
                    Class<?> returnArrayType = returnType.getComponentType();
                    Class<?> resultType;
                    if (returnArrayType == null)
                        if (Iterable.class.isAssignableFrom(returnType) ||
                            Optional.class.equals(returnType))
                            resultType = entityClass;
                        else
                            resultType = returnType;
                    else
                        resultType = returnArrayType;

                    TypedQuery<?> query = em.createQuery(jpql, resultType);
                    if (args != null) {
                        Parameter[] params = method.getParameters();
                        for (int i = 0; i < args.length; i++) {
                            Param param = params[i].getAnnotation(Param.class);
                            if (param == null)
                                query.setParameter(i + 1, args[i]);
                            else // named parameter
                                query.setParameter(param.value(), args[i]);
                        }
                    }

                    List<?> results = query.getResultList();

                    if (resultType.equals(returnType))
                        returnValue = results.isEmpty() ? null : results.iterator().next();
                    else if (returnType.isInstance(results))
                        returnValue = results;
                    else if (returnArrayType != null) {
                        Object r = Array.newInstance(returnArrayType, results.size());
                        int i = 0;
                        for (Object o : results)
                            Array.set(r, i++, o);
                        returnValue = r;
                    } else if (Optional.class.equals(returnType))
                        returnValue = results.isEmpty() ? Optional.empty() : Optional.of(results.iterator().next());
                    else if (List.class.isAssignableFrom(returnType))
                        try {
                            @SuppressWarnings("unchecked")
                            Constructor<? extends List<Object>> c = (Constructor<? extends List<Object>>) returnType.getConstructor();
                            List<Object> list = c.newInstance();
                            list.addAll(results);
                            returnValue = list;
                        } catch (NoSuchMethodException x) {
                            throw new UnsupportedOperationException(returnType + " lacks public zero parameter constructor.");
                        }
                    else // TODO convert other return types, such as arrays
                        throw new UnsupportedOperationException(methodName + " with return type " + returnType);
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

                    returnValue = toReturnValue(updateCount, returnType);
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

    private static final Object toReturnValue(int i, Class<?> returnType) {
        if (int.class.equals(returnType) || Integer.class.equals(returnType) || Number.class.equals(returnType))
            return i;
        else if (long.class.equals(returnType) || Long.class.equals(returnType))
            return Long.valueOf(i);
        else if (boolean.class.equals(returnType) || Boolean.class.equals(returnType))
            return i != 0;
        else if (void.class.equals(returnType) || Void.class.equals(returnType))
            return null;
        else
            throw new UnsupportedOperationException("Return update count as " + returnType);
    }
}
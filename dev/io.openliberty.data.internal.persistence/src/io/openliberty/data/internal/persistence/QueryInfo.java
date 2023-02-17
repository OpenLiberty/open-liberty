/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.Pageable;
import jakarta.data.repository.Sort;
import jakarta.persistence.Query;

/**
 */
class QueryInfo {
    private final TraceComponent tc = Tr.register(QueryInfo.class);

    static enum Type {
        COUNT, DELETE, EXISTS, MERGE, SELECT, UPDATE
    }

    /**
     * Information about the type of entity to which the query pertains.
     */
    EntityInfo entityInfo;

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
     * Keyset consisting of key names and sort direction.
     */
    List<Sort> keyset;

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
     * Indicates that parameters are supplied to the repository method
     * as entity or Iterable of entity and need conversion to entity id
     * or list of entity id.
     * This is currently only used for delete(entity) and delete(Iterable of entities).
     */
    boolean paramsNeedConversionToId;

    /**
     * Array element type if the repository method returns an array, such as,
     * <code>Product[] findByNameLike(String namePattern);</code>
     * or if its parameterized type is an array, such as,
     * <code>CompletableFuture&lt;Product[]&gt; findByNameLike(String namePattern);</code>
     * Otherwise null.
     */
    final Class<?> returnArrayType;

    /**
     * Type parameter of the repository method return value.
     * Null if the return type is not parameterized or is generic.
     * This is useful in cases such as
     * <code>&#64;Query(...) Optional&lt;Float&gt; priceOf(String productId)</code>
     * and
     * <code>CompletableFuture&lt;Stream&lt;Product&gt&gt; findByNameLike(String namePattern)</code>
     */
    final Class<?> returnTypeParam;

    /**
     * Type of the first parameter to a save operation. Null if not a save operation.
     */
    Class<?> saveParamType;

    /**
     * Categorization of query type.
     */
    Type type;

    /**
     * Construct partially complete query information.
     */
    QueryInfo(Method method, Class<?> returnArrayType, Class<?> returnTypeParam) {
        this.method = method;
        this.returnArrayType = returnArrayType;
        this.returnTypeParam = returnTypeParam;
    }

    /**
     * Obtains keyset cursor values for the specified entity.
     *
     * @param entity the entity.
     * @return keyset cursor values, ordering according to the sort criteria.
     */
    @Trivial
    Object[] getKeysetValues(Object entity) {
        ArrayList<Object> keyValues = new ArrayList<>();
        for (Sort keyInfo : keyset)
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
                throw new DataException(x.getCause());
            }
        return keyValues.toArray();
    }

    /**
     * Sets query parameters from keyset values.
     *
     * @param query        the query
     * @param keysetCursor keyset values
     * @throws Exception if an error occurs
     */
    void setKeysetParameters(Query query, Pageable.Cursor keysetCursor) throws Exception {
        int paramNum = paramCount + 1; // set to position of first keyset parameter
        if (paramNames == null) // positional parameters
            for (int i = 0; i < keysetCursor.size(); i++) {
                Object value = keysetCursor.getKeysetElement(i);
                if (entityInfo.idClass != null && entityInfo.idClass.isInstance(value)) {
                    for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                        Object v = accessor instanceof Field ? ((Field) accessor).get(value) : ((Method) accessor).invoke(value);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set keyset parameter ?" + paramNum + ' ' + value.getClass().getName() + "-->" +
                                               (v == null ? null : v.getClass().getSimpleName()));
                        query.setParameter(paramNum++, v);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set keyset parameter ?" + paramNum + ' ' +
                                           (value == null ? null : value.getClass().getSimpleName()));
                    query.setParameter(paramNum++, value);
                }
            }
        else // named parameters
            for (int i = 0; i < keysetCursor.size(); i++) {
                Object value = keysetCursor.getKeysetElement(i);
                if (entityInfo.idClass != null && entityInfo.idClass.isInstance(value)) {
                    for (Member accessor : entityInfo.idClassAttributeAccessors.values()) {
                        Object v = accessor instanceof Field ? ((Field) accessor).get(value) : ((Method) accessor).invoke(value);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "set keyset parameter :keyset" + paramNum + ' ' + value.getClass().getName() + "-->" +
                                               (v == null ? null : v.getClass().getSimpleName()));
                        query.setParameter(paramNum++, v);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "set keyset parameter :keyset" + paramNum + ' ' +
                                           (value == null ? null : value.getClass().getSimpleName()));
                    query.setParameter("keyset" + (paramNum++), value);
                }
            }
        // TODO detect if user provides a wrong-sized keyset? Or let JPA error surface?
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

        if (entityInfo.idClass == null || !paramsNeedConversionToId) {
            int namedParamCount = paramNames == null ? 0 : paramNames.size();
            for (int i = 0, p = 0; i < methodParamForQueryCount; i++) {
                Object arg = paramsNeedConversionToId ? //
                                toEntityId(args[i]) : //
                                args[i];

                if (arg == null || entityInfo.idClass == null || !entityInfo.idClass.isInstance(arg)) {
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
        } else { // Special case: CrudRepository.delete(entity) where entity has IdClass
            Object arg = args == null || args.length == 0 ? null : args[0];
            if (arg == null || !entityInfo.type.isAssignableFrom(arg.getClass()))
                throw new DataException("The " + (arg == null ? null : arg.getClass().getName()) +
                                        " parameter does not match the " + entityInfo.type.getClass().getName() +
                                        " entity type that is expected for this repository.");
            int p = 0;
            for (String idClassAttr : entityInfo.idClassAttributeAccessors.keySet()) {
                List<Member> accessors = entityInfo.attributeAccessors.get(entityInfo.getAttributeName(idClassAttr));
                Object param = arg;
                for (Member accessor : accessors)
                    if (accessor instanceof Method)
                        param = ((Method) accessor).invoke(param);
                    else
                        param = ((Field) accessor).get(param);
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "set ?" + (p + 1) + ' ' + (param == null ? null : param.getClass().getSimpleName()));
                query.setParameter(++p, param);
            }
        }
    }

    /**
     * Converts a repository method parameter that is an entity or iterable of entities
     * into an entity id or list of entity ids.
     *
     * @param value value of the repository method parameter.
     * @return entity id or list of entity ids.
     */
    private Object toEntityId(Object value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<Member> keyAccessors = entityInfo.attributeAccessors.get(entityInfo.getAttributeName("id"));

        if (value instanceof Iterable) {
            List<Object> list = new ArrayList<>();
            for (Object v : (Iterable<?>) value) {
                for (Member keyAccessor : keyAccessors) {
                    Class<?> type = keyAccessor.getDeclaringClass();
                    if (type.isInstance(v)) {
                        if (keyAccessor instanceof Method)
                            v = ((Method) keyAccessor).invoke(v);
                        else // Field
                            v = ((Field) keyAccessor).get(v);
                    } else {
                        throw new MappingException("Value of type " + v.getClass().getName() + " is incompatible with attribute type " + type.getName()); // TODO NLS
                    }
                }
                list.add(v);
            }
            value = list;
        } else { // single value
            for (Member keyAccessor : keyAccessors) {
                Class<?> type = keyAccessor.getDeclaringClass();
                if (type.isInstance(value)) {
                    if (keyAccessor instanceof Method)
                        value = ((Method) keyAccessor).invoke(value);
                    else // Field
                        value = ((Field) keyAccessor).get(value);
                } else {
                    throw new MappingException("Value of type " + value.getClass().getName() + " is incompatible with attribute type " + type.getName()); // TODO NLS
                }
            }
        }

        return value;
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
     * Copy of query information, but with updated JPQL.
     */
    QueryInfo withJPQL(String jpql) {
        QueryInfo q = new QueryInfo(method, returnArrayType, returnTypeParam);
        q.entityInfo = entityInfo;
        q.hasWhere = hasWhere;
        q.jpql = jpql;
        q.jpqlAfterKeyset = jpqlAfterKeyset;
        q.jpqlBeforeKeyset = jpqlBeforeKeyset;
        q.jpqlCount = jpqlCount;
        q.keyset = keyset;
        q.maxResults = maxResults;
        q.paramCount = paramCount;
        q.paramAddedCount = paramAddedCount;
        q.paramNames = paramNames;
        q.paramsNeedConversionToId = paramsNeedConversionToId;
        q.saveParamType = saveParamType;
        q.type = type;
        return q;
    }
}

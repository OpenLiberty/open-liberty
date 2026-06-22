/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal;

import static io.openliberty.data.internal.QueryType.FIND;
import static io.openliberty.data.internal.QueryType.FIND_AND_DELETE;
import static io.openliberty.data.internal.cdi.DataExtension.exc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.ibm.websphere.ras.annotation.Sensitive;

import jakarta.data.Limit;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.MappingException;
import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Param;

/**
 * This class consists of methods that raise exceptions.
 */
public class Fail {

    /**
     * Raises MappingException for a result that cannot be converted to the
     * return type of the repository count method.
     *
     * @param info  query information for the repository method.
     * @param count the count of matching items obtained by the query.
     * @throws the MappingException.
     */
    static MappingException countConversion(QueryInfo info, Long count) {
        throw exc(MappingException.class,
                  "CWWKD1049.count.convert.err",
                  count,
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericReturnType().getTypeName());
    }

    /**
     * Raises a new MappingException when the total count of results exceeds
     * a maximum threshold.
     *
     * @param info  query information for the repository method.
     * @param count long count.
     * @param type  Integer.class, Short.class, or Byte.class.
     * @throws MappingException
     */
    static MappingException countExceedsMax(QueryInfo info,
                                            long count,
                                            Class<? extends Number> type) {
        String max = type.getSimpleName() + ".MAX_VALUE (";
        if (Integer.class.equals(type))
            max += Integer.MAX_VALUE;
        else if (Short.class.equals(type))
            max += Short.MAX_VALUE;
        else if (Byte.class.equals(type))
            max += Byte.MAX_VALUE;
        max += ')';

        throw exc(MappingException.class,
                  "CWWKD1048.result.exceeds.max",
                  count,
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericReturnType().getTypeName(),
                  max);
    }

    /**
     * Raises a new UnsupportedOperationException for a JPQL query that is
     * not compatible with cursor pagination.
     *
     * @param info              query information for the repository method.
     * @param ql                the query.
     * @param endOfWhereClause  position at which the WHERE clause ends.
     * @param endsAtOrderClause indicates if this error is being raised because an
     *                              ORDER BY clause was found in the query.
     * @throws UnsupportedOperationException
     */
    static UnsupportedOperationException cursorQueryIncompat(QueryInfo info,
                                                             String ql,
                                                             int endOfWhereClause,
                                                             boolean endsAtOrderClause) {

        if (endsAtOrderClause)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1033.ql.orderby.disallowed",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      CursoredPage.class.getSimpleName(),
                      OrderBy.class.getSimpleName(),
                      ql);
        else
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1034.ql.req.end.in.where",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      CursoredPage.class.getSimpleName(),
                      endOfWhereClause,
                      ql.length(),
                      ql);
    }

    /**
     * Raises IllegalStateException because the repository bean was disposed.
     *
     * @param impl   repository implementation
     * @param proxy  proxy instance upon which the repository method is invoked
     * @param method repository method that the application invoked
     * @throws IllegalStateException
     */
    static IllegalStateException disposed(RepositoryImpl<?> impl,
                                          Object proxy,
                                          Method method) {
        throw exc(IllegalStateException.class,
                  "CWWKD1076.repo.disposed",
                  method.getName(),
                  impl.repositoryInterface.getName(),
                  new StringBuilder("RepositoryImpl@") //
                                  .append(Integer.toHexString(impl.hashCode())) //
                                  .append("/(proxy)@") //
                                  .append(Integer.toHexString(System.identityHashCode(proxy))));
    }

    /**
     * Raises a new UnsupportedOperationException for a repository method that
     * has two or more of the same special parameter type.
     *
     * @param info           query information for the repository method.
     * @param paramClassName simple class name of the duplicate parameter.
     * @throws UnsupportedOperationException
     */
    static UnsupportedOperationException duplicateSpecialParam(QueryInfo info,
                                                               String paramClassName) {
        throw exc(UnsupportedOperationException.class,
                  "CWWKD1017.dup.special.param",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  paramClassName);
    }

    /**
     * Raise IllegalArgumentException because the application supplied an empty
     * value as the life cycle method parameter.
     *
     * @param info query information for the repository method.
     * @throws the IllegalArgumentException.
     */
    static IllegalArgumentException emptyLifeCycleParam(QueryInfo info) {
        throw exc(IllegalArgumentException.class,
                  "CWWKD1092.lifecycle.arg.empty",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericParameterTypes()[0].getTypeName());
    }

    /**
     * Raise a new EmptyResultException.
     *
     * @param info query information for the repository method.
     * @throws the EmptyResultException.
     */
    static EmptyResultException emptyResult(QueryInfo info) {
        throw exc(EmptyResultException.class,
                  "CWWKD1053.empty.result",
                  info.method.getGenericReturnType().getTypeName(),
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  List.of(List.class.getSimpleName(),
                          Optional.class.getSimpleName(),
                          Page.class.getSimpleName(),
                          CursoredPage.class.getSimpleName(),
                          Stream.class.getSimpleName()));
    }

    /**
     * Raises a new MappingException when an entity attribute name is not indicated
     * for a constraint, sort, or other place in the Jakarta Data API that requires
     * an entity attribute name.
     *
     * @param info  query information for the repository method.
     * @param count long count.
     * @param type  Integer.class, Short.class, or Byte.class.
     * @throws MappingException
     */
    static MappingException entityAttributeNameMissing(QueryInfo info) {
        throw exc(MappingException.class,
                  "CWWKD1024.missing.entity.attr",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.entityInfo.getType().getName(),
                  info.entityInfo.attributeTypes.keySet());
    }

    /**
     * Raise IllegalArgumentException because the supplied entity does not
     * match the expected type, or NullPointerException if the supplied entity
     * is null.
     *
     * @param info   query information for the repository method.
     * @param entity the supplied entity.
     * @throws IllegalArgumentException.
     * @throws NullPointerException.
     */
    static EmptyResultException entityMismatch(QueryInfo info,
                                               @Sensitive Object entity) {
        if (entity == null)
            throw Fail.entityNull(info);

        Class<?> expectedEntityClass = info.entityInfo.getType();

        throw exc(IllegalArgumentException.class,
                  "CWWKD1016.incompat.entity.param",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  expectedEntityClass.getName(),
                  entity.getClass().getName());
    }

    /**
     * Raises OptimisticLockingFailureException because no matching entity
     * was found.
     *
     * @param info            query information for the repository method.
     * @param entity          the entity.
     * @param idAttributeName name of the entity's id attribute.
     * @param id              value of the entity's id attribute.
     * @param version         value of the entity's version attribute. Null if none.
     * @throws the OptimisticLockingFailureException.
     */
    static OptimisticLockingFailureException entityNotFound(QueryInfo info,
                                                            @Sensitive Object entity,
                                                            String idAttributeName,
                                                            Object id,
                                                            Object version) {
        List<String> entityProps = new ArrayList<>(2);

        if (id != null)
            entityProps.add(info.loggableAppend(idAttributeName,
                                                "=",
                                                id));

        if (info.entityInfo.versionAttributeName != null && version != null)
            entityProps.add(info.loggableAppend(info.entityInfo.versionAttributeName,
                                                "=",
                                                version));

        throw exc(OptimisticLockingFailureException.class,
                  "CWWKD1050.opt.lock.exc",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  entity.getClass().getName(),
                  entityProps,
                  Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES_STATELESS);
    }

    /**
     * Raise a NullPointerException because the supplied entity is null.
     *
     * @param info query information for the repository method.
     * @throws the NullPointerException.
     */
    static NullPointerException entityNull(QueryInfo info) {
        throw exc(NullPointerException.class,
                  "CWWKD1015.null.entity.param",
                  info.method.getName(),
                  info.repositoryInterface.getName());
    }

    /**
     * Raises a new MappingException when a function that is supplied to a
     * repository method does not apply to the respective entity because it
     * lacks an entity attribute of the required type.
     *
     * @param info           query information for the repository method.
     * @param functionName   name of the function being attempted.
     * @param entityAnnoName name of an entity annotation, such as @Id or @Version.
     * @throws MappingException
     */
    static MappingException functionNotApplicable(QueryInfo info,
                                                  String functionName,
                                                  String entityAnnoName) {
        throw exc(MappingException.class,
                  "CWWKD1093.fn.not.applicable",
                  functionName,
                  info.entityInfo.getType().getName(),
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  entityAnnoName);
    }

    /**
     * Raise a new MappingException for the error where the repository method has an
     * extra parameter that does not apply to the query conditions and is not a
     * special parameter.
     *
     * @param info  query information for the repository method.
     * @param index index (0-based) of the repository method parameter.
     * @throws the MappingException.
     */
    static MappingException extraMethodParam(QueryInfo info, int index) {
        validateParameterPositions(info);

        throw exc(MappingException.class,
                  "CWWKD1023.extra.param",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.specialParamsStartAt,
                  info.method.getParameterTypes()[index].getName(),
                  info.ql);
    }

    /**
     * Raise a new MappingException for the error where the repository method has an
     * extra parameter that does not apply to the query conditions and is not a
     * special parameter.
     *
     * @param info        query information for the repository method.
     * @param numRequired number of parameters required by the query conditions.
     * @param numFound    number of parameters found on the repository method.
     * @throws the MappingException.
     */
    static MappingException extraMethodParams(QueryInfo info,
                                              int numRequired,
                                              int numFound) {
        throw exc(UnsupportedOperationException.class,
                  "CWWKD1022.too.many.params",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  numRequired,
                  numFound,
                  info.ql);
    }

    /**
     * Raises UnsupportedOperationException for a repository method that is not
     * valid as a life cycle method because it has no method parameters or more
     * than 1 method parameter.
     *
     * @param info     query information for the repository method.
     * @param anooType Java class of the of repository method annotation.
     * @throws UnsupportedOperationException.
     */
    static MappingException //
                    lifeCycleMethodParamCount(QueryInfo info,
                                              Class<? extends Annotation> annoType) {
        throw exc(UnsupportedOperationException.class,
                  "CWWKD1009.lifecycle.param.err",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getParameterCount(),
                  annoType.getSimpleName());
    }

    /**
     * Check if the cause of the lacking named parameter is a mispositioned
     * special parameter. If so, raises UnsupportedOperationException.
     *
     * Otherwise, raises a MappingException for the error where one or more
     * of the named parameters required by a JPQL query are not specified
     * by the method parameters
     *
     * @param info    query information for the repository method.
     * @param lacking query named parameters for which no method parameters were
     *                    found.
     * @throws MappingException              for a missing named parameter.
     * @throws UnsupportedOperationException if there is a mispositioned special
     *                                           parameter.
     */
    static RuntimeException methodLacksNamedParams(QueryInfo info,
                                                   Set<String> lacking) {

        validateParameterPositions(info);

        String first = null;
        StringBuilder all = new StringBuilder();
        for (String name : lacking) {
            if (first == null)
                first = name;
            else
                all.append(", ");
            all.append(':').append(name);
        }

        throw exc(MappingException.class,
                  "CWWKD1084.missing.named.params",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  all,
                  info.getQueryAnnoValue(),
                  "@Param(\"" + first + "\")",
                  "String " + first);
    }

    /**
     * Create a new UnsupportedOperationException for repository method parameter
     * annotations that conflict with each other or with the type of the repository
     * method parameter.
     *
     * @param info       query information for the repository method.
     * @param errorCode  constant from DataVersionCompatibility that classifies
     *                       the error.
     * @param paramIndex index (0-based) of repository method parameter.
     * @param paramType  Java class of the repository method parameter.
     * @param paramAnnos annotations on the repository method parameter.
     * @throws the UnsupportedOperationException
     */
    static UnsupportedOperationException methodParamAnnoConflict(QueryInfo info,
                                                                 int errorCode,
                                                                 int paramIndex,
                                                                 Class<?> paramType,
                                                                 Annotation[] paramAnnos) {
        if (errorCode == QueryInfo.PARAM_ANNO_CONFLICTS_WITH_CONSTRAINT)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1117.anno.constraint.conflict",
                      paramIndex + 1, // switch to 1-based
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      Arrays.toString(paramAnnos),
                      paramType.getClass().getName());
        else if (errorCode == QueryInfo.PARAM_ANNOS_CONFLICT)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1118.param.anno.conflict",
                      paramIndex + 1, // switch to 1-based
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      Arrays.toString(paramAnnos));
        else // internal error
            throw new IllegalArgumentException("errorCode: " + errorCode);
    }

    /**
     * Create a new UnsupportedOperationException for conflicting Limit or
     * PageRequest parameters supplied to a repository method. The conflict
     * might be with each other, with another of the same type, or with a
     * First keyword in the method name.
     *
     * @param info    query information for the repository method.
     * @param param   method parameter that is an instance of Limit or PageRequest.
     * @param limit   other Limit parameter value. Otherwise null.
     * @param pageReq other PageRequest parameter value. Otherwise null.
     * @throws the UnsupportedOperationException
     */
    static UnsupportedOperationException methodParamIncompat(QueryInfo info,
                                                             Object param,
                                                             Limit limit,
                                                             PageRequest pageReq) {
        Class<?> type = param instanceof Limit ? Limit.class : PageRequest.class;

        if (limit == null && pageReq == null)
            // conflicts with First keyword
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1099.first.keyword.incompat",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      type.getSimpleName());
        else if (param instanceof Limit ? limit != null : pageReq != null)
            // conflicts with another parameter of the same type
            throw Fail.duplicateSpecialParam(info, type.getSimpleName());
        else
            // conflict between Limit and PageRequest parameters
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1018.confl.special.param",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      Limit.class.getSimpleName(),
                      PageRequest.class.getSimpleName());
    }

    /**
     * Create a new UnsupportedOperationException for a repository method
     * parameter that is not valid on a repository method that has the given
     * repository method annotation.
     *
     * @param info       query information for the repository method.
     * @param paramType  Java class of the repository method parameter.
     * @param methodAnno repository method annotation, such as Update or Delete.
     * @throws the UnsupportedOperationException
     */
    static UnsupportedOperationException methodParamInvalid(QueryInfo info,
                                                            Class<?> paramType,
                                                            Annotation methodAnno) {
        throw exc(UnsupportedOperationException.class,
                  "CWWKD1020.invalid.param.type",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  paramType.getSimpleName(),
                  methodAnno.annotationType().getSimpleName());
    }

    /**
     * Constructs the MappingException or UnsupportedOperationException for
     * the error where a repository method parameter lacks an annotation that
     * identifies the corresponding entity attribute name.
     *
     * @param info query information for the repository method.
     * @param p    position (1-based) of the repository method parameter.
     * @throws MappingException or UnsupportedOperationException.
     */
    static RuntimeException methodParamLacksAnno(QueryInfo info, int p) {
        DataVersionCompatibility compat = info.producer.compat();

        switch (info.type) {
            case FIND:
            case FIND_AND_DELETE:
                validateParameterPositions(info);
                String specParams = info.type == QueryType.FIND //
                                ? compat.specialParamsForFind() //
                                : compat.specialParamsForFindAndDelete();
                throw exc(MappingException.class,
                          "CWWKD1012.fd.missing.param.anno",
                          p,
                          info.method.getName(),
                          info.repositoryInterface.getName(),
                          specParams);
            case QM_DELETE:
            case COUNT:
            case EXISTS:
                throw exc(MappingException.class,
                          "CWWKD1013.cde.missing.param.anno",
                          p,
                          info.method.getName(),
                          info.repositoryInterface.getName());
            case QM_UPDATE:
                throw exc(UnsupportedOperationException.class,
                          "CWWKD1014.upd.missing.param.anno",
                          info.method.getName(),
                          info.repositoryInterface.getName(),
                          info.method.getParameterCount(),
                          p,
                          compat.paramAnnosForUpdate());
            default: // should be unreachable
                throw new IllegalStateException(info.type.name());
        }
    }

    /**
     * Raise an error because the PageRequest is missing.
     *
     * @param info query information for the repository method.
     * @throws IllegalArgumentException      if the user supplied a null PageRequest
     * @throws UnsupportedOperationException if the repository method signature
     *                                           lacks a parameter for supplying a
     *                                           PageRequest
     */
    static RuntimeException missingPageRequest(QueryInfo info) {
        Class<?>[] paramTypes = info.method.getParameterTypes();

        // Check parameter positions after those used for query parameters
        boolean signatureHasPageReq = false;
        for (int i = 0; i < paramTypes.length; i++)
            signatureHasPageReq |= PageRequest.class.equals(paramTypes[i]);

        if (signatureHasPageReq)
            // NullPointerException is required by BasicRepository.findAll
            throw exc(NullPointerException.class,
                      "CWWKD1087.null.param",
                      PageRequest.class.getName(),
                      info.method.getName(),
                      info.repositoryInterface.getName());
        else
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1041.rtrn.mismatch.pagereq",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      info.method.getGenericReturnType().getTypeName());
    }

    /**
     * Raises UnsupportedOperationException for the error where a repository
     * method intermixed named and positional parameters for a query.
     *
     * @param info          query information for the repository method.
     * @param methodNPCount count of repository method parameters that indicate
     *                          names and values of JPQL named parameters.
     * @throws the UnsupportedOperationException.
     */
    static UnsupportedOperationException mixedQLParamTypes(QueryInfo info,
                                                           int methodNPCount) {
        String firstNamedParam = null;
        StringBuilder allNamedParams = new StringBuilder().append('(');
        for (String name : info.qlParamNames) {
            if (firstNamedParam == null)
                firstNamedParam = name;
            else
                allNamedParams.append(", ");
            allNamedParams.append(':').append(name);
        }
        allNamedParams.append(')');

        Class<?> firstNamedParamType = String.class;
        for (Parameter p : info.method.getParameters()) {
            Param param = p.getAnnotation(Param.class);
            if (param == null //
                            ? p.isNamePresent() &&
                              firstNamedParam.equals(p.getName()) //
                            : firstNamedParam.equals(param.value()))
                firstNamedParamType = p.getType();
            break;
        }

        throw exc(UnsupportedOperationException.class,
                  "CWWKD1019.mixed.positional.named",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.qlParamCount - methodNPCount,
                  methodNPCount,
                  allNamedParams,
                  info.getQueryAnnoValue(),
                  ':' + firstNamedParam,
                  "@Param(\"" + firstNamedParam + "\")",
                  firstNamedParamType.getSimpleName() + ' ' + firstNamedParam);
    }

    /**
     * Raises MappingException for a repository method that has two method
     * parameters that apply to the same JPQL named parameter.
     *
     * @param info           query information for the repository method.
     * @param namedParamName name of the JPQL named parameter.
     * @param methodParam    the method parameter.
     * @throws MappingException.
     */
    static MappingException namedParamConflict(QueryInfo info,
                                               String namedParamName,
                                               Parameter methodParam) {
        throw exc(MappingException.class,
                  "CWWKD1083.dup.method.param",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  namedParamName,
                  "@Param(\"" + namedParamName + "\")",
                  methodParam.getType().getSimpleName() + ' ' + namedParamName);
    }

    /**
     * Raise a new NonUniqueResultException.
     *
     * @param info       query information for the repository method.
     * @param numResults number of results.
     * @throws the NonUniqueResultException.
     */
    static NonUniqueResultException nonUniqueResult(QueryInfo info,
                                                    int numResults) {

        String entityName = info.entityInfo.getType().getSimpleName();
        String returnType = "Optional<" + entityName + ">";

        List<String> recommendations = info.producer.compat().atLeast(1, 1) //
                        ? List.of("@Find @First @OrderBy(...) " +
                                  returnType + " get(...)",
                                  returnType + " findFirstByX(...)") //
                        : List.of(returnType + " findFirstByX(...)",
                                  returnType + " findByX(..., Limit.of(1))");

        throw exc(NonUniqueResultException.class,
                  "CWWKD1054.non.unique.result",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericReturnType().getTypeName(),
                  numResults,
                  recommendations);
    }

    /**
     * Raises NullPointerException because a null value was supplied to a
     * repository method.
     *
     * @param info  query information for the repository method.
     * @param index index of repository method parameter that is null.
     * @throws the NullPointerException.
     */
    static NullPointerException nullMethodParameter(QueryInfo info, int index) {
        Class<?> paramType = info.method.getParameterTypes()[index];
        Class<?> arrayType = paramType.componentType();
        String paramTypeName = arrayType == null //
                        ? paramType.getName() //
                        : arrayType.getName() + "[]";
        throw exc(NullPointerException.class,
                  "CWWKD1087.null.param",
                  paramTypeName,
                  info.method.getName(),
                  info.repositoryInterface.getName());
    }

    /**
     * Raises OptimisticLockingFailureException because less entities than
     * expected were updated.
     *
     * @param info        query information for the repository method.
     * @param updateCount the observed update count.
     * @param numExpected the expected update count.
     * @throws the OptimisticLockingFailureException.
     */
    static OptimisticLockingFailureException optimisticLockConflict(QueryInfo info,
                                                                    int updateCount,
                                                                    int numExpected) {
        if (numExpected == 1)
            throw exc(OptimisticLockingFailureException.class,
                      "CWWKD1051.single.opt.lock.exc",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      info.entityInfo.entityClass.getName(),
                      Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES_STATELESS);
        else
            throw exc(OptimisticLockingFailureException.class,
                      "CWWKD1052.multi.opt.lock.exc",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      numExpected - updateCount,
                      numExpected,
                      info.entityInfo.entityClass.getName(),
                      Util.LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES_STATELESS);
    }

    /**
     * Raises UnsupportedOperationException because having an OrderBy annotation
     * on the repository method is incompatible with also using the OrderBy keyword
     * and with repository methods that are not find operations.
     *
     * @param info query information for the repository method.
     * @throws the UnsupportedOperationException.
     */
    static UnsupportedOperationException orderByAnnoIncompat(QueryInfo info) {
        // disallow on incompatible operations
        if (info.type != FIND && info.type != FIND_AND_DELETE)
            // TODO need appropriate error for (type == NATIVE) where the
            // @OrderBy is not allowed on a NativeQuery even if it is a
            // find operation
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1096.orderby.incompat",
                      info.method.getName(),
                      info.repositoryInterface.getName());

        if (info.sorts != null) // also has an OrderBy keyword
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1090.orderby.conflict",
                      info.method.getName(),
                      info.repositoryInterface.getName());

        throw new IllegalStateException(); // unreachable
    }

    /**
     * Raises an error for a type conversion failure due to a value being outside of
     * the specified range.
     *
     * @param info  query information for the repository method.
     * @param value the value that fails to convert.
     * @param min   minimum value for range.
     * @param max   maximum value for range.
     * @throws MappingException for the type conversion failure.
     */
    static MappingException outOfRange(QueryInfo info,
                                       @Sensitive Number value,
                                       long min,
                                       long max) {
        throw exc(MappingException.class,
                  "CWWKD1047.result.out.of.range",
                  info.loggableAppend(value.getClass().getName(), " (", value, ")"),
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericReturnType().getTypeName(),
                  min,
                  max);
    }

    /**
     * Raises IllegalArgumentException because the given PageRequest has a
     * mode that is incompatible with the return type of the repository method.
     *
     * @param info    query information for the repository method.
     * @param pageReq the page request.
     * @throws IllegalArgumentException.
     */
    static IllegalArgumentException pageModeIncompatible(QueryInfo info,
                                                         PageRequest pageReq) {
        throw exc(IllegalArgumentException.class,
                  "CWWKD1035.incompat.page.mode",
                  pageReq.mode(),
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericReturnType().getTypeName(),
                  CursoredPage.class.getSimpleName());
    }

    /**
     * Raises UnsupportedOperationException because the query is incompatible with
     * cursor-based pagination.
     *
     * @param info    query information for the repository method.
     * @param ql      a query written in query language, usually JPQL
     *                    or the JCQL subset of JPQL.
     * @param startAt starting index within query of the portion that
     *                    is incompatible with cursor pagination.
     * @param length  length of the incompatible portion of the query.
     * @throws the UnsupportedOperationException.
     */
    static UnsupportedOperationException queryIncompatipleWithCursor(QueryInfo info,
                                                                     String ql,
                                                                     int startAt,
                                                                     int length) {
        throw exc(UnsupportedOperationException.class,
                  "CWWKD1120.cursor.keyword.mismatch",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  ql.substring(startAt, startAt + length),
                  ql);
    }

    /**
     * Raises UnsupportedOperationException because a DELETE or UPDATE query
     * lacks the name of the entity upon which which the query operates.
     *
     * @param info      query information for the repository method.
     * @param ql        a query written in query language, usually JPQL
     *                      or the JCQL subset of JPQL.
     * @param queryType DELETE or UPDATE.
     * @throws the UnsupportedOperationException.
     */
    static UnsupportedOperationException queryLacksEntityName(QueryInfo info,
                                                              String ql,
                                                              String queryType) {
        String exampleQuery = "DELETE".equals(queryType) //
                        ? "DELETE FROM [entity_name] WHERE [conditional_expression]" //
                        : "UPDATE [entity_name] SET [update_items] WHERE [conditional_expression]";

        throw exc(UnsupportedOperationException.class,
                  "CWWKD1030.ql.lacks.entity",
                  ql,
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  queryType,
                  exampleQuery);
    }

    /**
     * Raises MappingException for a result that cannot be converted to the
     * return type of the repository method.
     *
     * @param info       query information for the repository method.
     * @param resultType name of the result class or null for a null result.
     * @param cause      a cause exception. Null if none.
     * @throws the MappingException.
     */
    static MappingException resultConversion(QueryInfo info,
                                             String resultType,
                                             Throwable cause) {
        MappingException x = exc(MappingException.class,
                                 "CWWKD1046.result.convert.err",
                                 resultType,
                                 info.method.getName(),
                                 info.repositoryInterface.getName(),
                                 info.method.getGenericReturnType().getTypeName());
        if (cause != null)
            x = (MappingException) x.initCause(cause);
        throw x;
    }

    /**
     * Raises an UnsupportedOperationException for an error where the
     * repository method return type does not match the query results.
     * One reason this might happen is when EclipseLink returns wrong values
     * when selecting ElementCollection attributes instead of rejecting
     * it as unsupported.
     *
     * @param info    query information for the repository method.
     * @param results list of at least 1 result.
     * @param query   jakarta.persistence.Query, a String, or null.
     * @throws UnsupportedOperationException.
     */
    static UnsupportedOperationException resultIncompatible(QueryInfo info,
                                                            @Sensitive List<?> results,
                                                            Object query) {
        String r = results.getClass().getName() +
                   "<" + results.get(0).getClass().getName() + ">";

        if (query == null)
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1102.incompat.query.result",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      info.method.getGenericReturnType().getTypeName(),
                      r);
        else
            throw exc(UnsupportedOperationException.class,
                      "CWWKD1103.incompat.query.result",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      info.method.getGenericReturnType().getTypeName(),
                      query instanceof String ? query : query.getClass().getName(),
                      r);
    }

    /**
     * Raises ClassCastException for a repository method return type that does
     * not match the number of entities returned.
     *
     * @param info                   query information for the repository method.
     * @param lifeCycleMethodAnno    type of life cycle method. For example, @Insert.
     * @param hasSingularEntityParam indicates if the method's entity parameter is
     *                                   singular (one entity) vs multiple.
     * @throws the ClassCastException.
     */
    static ClassCastException resultSizeMismatch(QueryInfo info,
                                                 String lifeCycleMethodAnno,
                                                 int numResults,
                                                 boolean hasSingularEntityParam) {
        throw exc(ClassCastException.class,
                  "CWWKD1094.return.mismatch",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericReturnType().getTypeName(),
                  numResults,
                  lifeCycleMethodAnno,
                  Util.lifeCycleReturnTypes(info.entityInfo.getType().getName(),
                                            hasSingularEntityParam,
                                            false));
    }

    /**
     * Raises MappingException for a return type that is not valid for the
     * repository method.
     *
     * @param repositoryInterface the repository interface.
     * @param method              the repository method.
     * @throws the MappingException.
     */
    public static MappingException returnTypeInvalid(Class<?> repositoryInterface,
                                                     Method method) {
        // TODO add helpful information about supported result types
        throw exc(UnsupportedOperationException.class,
                  "CWWKD1004.general.rtrn.err",
                  method.getGenericReturnType().getTypeName(),
                  method.getName(),
                  repositoryInterface.getName());
    }

    /**
     * Raises UnsupportedOperationException for a return type that is not valid
     * for the repository method.
     *
     * @param info                   query information for the repository method.
     * @param lifeCycleMethodType    type of life cycle method. For example, Insert.
     * @param hasSingularEntityParam indicates if the method's entity parameter is
     *                                   singular (one entity) vs multiple.
     * @param validReturnTypes       valid return types, if always the same.
     *                                   Otherwise null.
     * @param resultClass            resultClass from which to infer valid return
     *                                   types. Otherwise null.
     * @throws the UnsupportedOperationException.
     */
    static UnsupportedOperationException returnTypeInvalid(QueryInfo info,
                                                           String lifeCycleMethodType,
                                                           boolean hasSingularEntityParam,
                                                           String validReturnTypes,
                                                           Class<?> resultClass) {
        if (validReturnTypes == null)
            validReturnTypes = Util.lifeCycleReturnTypes(resultClass.getSimpleName(),
                                                         hasSingularEntityParam,
                                                         false).toString();

        throw exc(UnsupportedOperationException.class,
                  "CWWKD1003.rtrn.err",
                  info.method.getGenericReturnType().getTypeName(),
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  lifeCycleMethodType,
                  validReturnTypes);
    }

    /**
     * Raises MappingException for a return type that is not valid for a
     * repository delete method.
     *
     * @param info query information for the repository method.
     * @throws the MappingException.
     */
    static MappingException returnTypeInvalidForDelete(QueryInfo info) {
        throw exc(MappingException.class,
                  "CWWKD1006.delete.rtrn.err",
                  info.method.getGenericReturnType().getTypeName(),
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.entityInfo.getType().getName(),
                  info.entityInfo.idType);
    }

    /**
     * Raises MappingException for a return type that is not valid for a
     * repository method that performs a SELECT query/find operation.
     *
     * @param info query information for the repository method.
     * @throws the MappingException.
     */
    static MappingException returnTypeInvalidForFind(QueryInfo info) {
        throw exc(MappingException.class,
                  "CWWKD1005.find.rtrn.err",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  info.method.getGenericReturnType().getTypeName(),
                  info.entityInfo.entityClass.getName(),
                  List.of("List",
                          "Optional",
                          "Page",
                          "CursoredPage",
                          "Stream"));
    }

    /**
     * Raises MappingException for an attempt to retrieve a subset of entity
     * attributes, where at least one member of the requested subset is not
     * an entity attribute.
     *
     * @param info  query information for the repository method.
     * @param names requested entity attribute names, at least one of which
     *                  is not valid.
     * @param cause a cause exception to chain.
     * @throws the MappingException.
     */
    static MappingException selectedAttributesMismatch(QueryInfo info,
                                                       String[] names,
                                                       Throwable cause) {
        // Raise a more precise error that relates to using records
        // for a subset of entity attributes
        MappingException x = exc(MappingException.class,
                                 "CWWKD1101.attr.subset.mismatch",
                                 info.method.getGenericReturnType().getTypeName(),
                                 info.method.getName(),
                                 info.repositoryInterface.getName(),
                                 info.singleType.getName(),
                                 Arrays.toString(names),
                                 info.entityInfo.getType().getName(),
                                 info.entityInfo.getAttributeNames());
        throw (MappingException) x.initCause(cause);
    }

    /**
     * Raise a new UnsupportedOperationException for the error where a repository
     * method parameter is of a special parameter type that is incompatible with
     * the type of repository method.
     *
     * @param info      query information for the repository method.
     * @param paramType the type of special parameter that does not belong on
     *                      the repository method.
     * @throws UnsupportedOperationException.
     */
    static UnsupportedOperationException specialParamIncompatible(QueryInfo info,
                                                                  Class<?> paramType) {
        Fail.validateParameterPositions(info);

        throw exc(UnsupportedOperationException.class,
                  "CWWKD1020.invalid.param.type",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  paramType.getSimpleName(),
                  info.type.operationName);
    }

    /**
     * Raises MappingException because there is no known entity attribute
     * with the given name.
     *
     * @param info query information for the repository method.
     * @param name name that does not match the name of an entity attribute.
     * @throws the MappingException.
     */
    static MappingException unknownEntityAttribute(QueryInfo info, String name) {
        if (Util.hasOperationAnno(info.method, info.producer))
            throw exc(MappingException.class,
                      "CWWKD1010.unknown.entity.attr",
                      name,
                      info.entityInfo.getType().getName(),
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      info.entityInfo.attributeTypes.keySet());
        else
            throw exc(MappingException.class,
                      "CWWKD1091.method.name.parse.err",
                      name,
                      info.entityInfo.getType().getName(),
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      Util.operationAnnoNames(info.producer),
                      info.entityInfo.attributeTypes.keySet());
    }

    /**
     * Raises UnsupportedOperationException for the general error where
     * a repository method is unrecognized.
     *
     * @param info query information for the repository method.
     * @throws the UnsupportedOperationException.
     */
    static UnsupportedOperationException unsupportedMethod(QueryInfo info) {
        throw exc(UnsupportedOperationException.class,
                  "CWWKD1011.unknown.method.pattern",
                  info.method.getName(),
                  info.repositoryInterface.getName(),
                  Util.operationAnnoNames(info.producer),
                  Util.resourceAccessorTypeNames(info.producer),
                  Util.methodNamePrefixes(info.producer),
                  info.entityInfo.getExampleMethodNames());
    }

    /**
     * Raises MappingException for the error where the repository method
     * defines extra named parameters that are not used by the JPQL query.
     *
     * @throws MappingException.
     */
    static MappingException unusedNamedParamsOnMethod(QueryInfo info,
                                                      Set<String> extras,
                                                      Set<String> qlRequired) {
        String firstExtraParam = null;
        StringBuilder extraParamNames = new StringBuilder();
        for (String name : extras)
            if (name.length() > 0) {
                if (firstExtraParam == null)
                    firstExtraParam = name;
                else
                    extraParamNames.append(", ");
                extraParamNames.append(name);
            }

        if (firstExtraParam == null && !extras.isEmpty())
            // @Param("") with empty String is not valid
            throw exc(MappingException.class,
                      "CWWKD1104.empty.anno.value",
                      Param.class.getSimpleName(),
                      info.method.getName(),
                      info.repositoryInterface.getName());

        boolean isFirst = true;
        StringBuilder qlParamNames = new StringBuilder();
        for (String name : qlRequired) {
            if (!isFirst)
                qlParamNames.append(", ");
            qlParamNames.append(':').append(name);
            isFirst = false;
        }

        if (qlRequired.isEmpty())
            throw exc(MappingException.class,
                      "CWWKD1086.named.params.unused",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      extraParamNames,
                      info.getQueryAnnoValue(),
                      ':' + firstExtraParam);
        else
            throw exc(MappingException.class,
                      "CWWKD1085.extra.method.params",
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      extraParamNames,
                      qlParamNames,
                      info.getQueryAnnoValue());
    }

    /**
     * Confirm that special parameters are positioned after all other parameters.
     *
     * @throws UnupportedOperationException if a special parameter is ahead of
     *                                          a query parameter.
     */
    private static void validateParameterPositions(QueryInfo info) {
        DataVersionCompatibility compat = info.entityInfo.factory.provider.compat;

        Class<?>[] paramTypes = info.method.getParameterTypes();
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
                      info.method.getName(),
                      info.repositoryInterface.getName(),
                      paramTypes[specParamIndex].getName(),
                      compat.specialParamsForFind());
    }

}

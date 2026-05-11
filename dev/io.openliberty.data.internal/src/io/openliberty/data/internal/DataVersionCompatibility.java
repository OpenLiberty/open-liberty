/*******************************************************************************
 * Copyright (c) 2024,2026 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.cdi.RepositoryProducer;
import jakarta.data.repository.Find;

/**
 * Interface for version-dependent capability, available as an OSGi service.
 */
public interface DataVersionCompatibility {
    /**
     * Size 0 array indicating no Select annotations are present.
     */
    final String[] NO_SELECTIONS = new String[0];

    /**
     * Construct an instance that handles capability for the given Jakarta Data
     * specification version, by converting to _ the . character that separates
     * the major and minor versions in specVersion, and afterword invoking the
     * no argument constructor:
     * io.openliberty.data.internal.v{major_minor}.Data_{major_minor}
     *
     * @param specVersion value of getPackage().getSpecificationVersion()
     *                        for a Jakarta Data class.
     * @return the instance.
     */
    static DataVersionCompatibility of(String specVersion) {
        if (specVersion == null)
            specVersion = "1.0";
        String major_minor = specVersion.replace('.', '_');
        String implClassName = "io.openliberty.data.internal.v" + major_minor + //
                               ".Data_" + major_minor;
        try {
            @SuppressWarnings("unchecked")
            Class<DataVersionCompatibility> implClass = //
                            (Class<DataVersionCompatibility>) //
                            DataVersionCompatibility.class.getClassLoader() //
                                            .loadClass(implClassName);
            return implClass.getConstructor().newInstance();
        } catch (ClassNotFoundException | //
                        IllegalAccessException | //
                        InstantiationException | //
                        NoSuchMethodException x) {
            throw new RuntimeException(x); // internal error
        } catch (InvocationTargetException x) {
            throw new RuntimeException(x.getCause()); // internal error
        }
    }

    /**
     * Indicates whether the enabled version of Jakarta Data is at the requested
     * level or higher.
     *
     * @param major major version of Jakarta Data specification. Must be >= 1.
     * @param minor minor version of Jakarta Data specification. Must be >= 0.
     * @return true if at the requested level of Jakarta Data or higher,
     *         otherwise false.
     */
    boolean atLeast(int major, int minor);

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
    public QueryInfo createQueryInfo(RepositoryProducer<?> repositoryProducer,
                                     Class<?> repositoryInterface,
                                     Method method,
                                     QueryType methodType,
                                     Annotation methodTypeAnno,
                                     Class<?> entityParamType,
                                     boolean isOptional,
                                     Class<?> returnArrayType,
                                     Class<?> multiType,
                                     Class<?> singleType,
                                     Class<?> singleTypeElementType);

    /**
     * Obtains the Count annotation if present on the method. Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return Count annotation if present, otherwise null.
     */
    Annotation getCountAnnotation(Method method);

    /**
     * Obtains the entity class from the Find annotation value, if present.
     *
     * @param find Find annotation.
     * @return entity class if the Find annotation value is present. Otherwise void.class.
     */
    Class<?> getEntityClass(Find find);

    /**
     * Obtains the Exists annotation if present on the method. Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return Exists annotation if present, otherwise null.
     */
    Annotation getExistsAnnotation(Method method);

    /**
     * Obtains the value of the First annotation if present on the method.
     * Otherwise null.
     *
     * @param method repository method. Must not be null.
     * @return First annotation value if present, otherwise null.
     */
    Integer getFirstAnnotationValue(Method method);

    /**
     * Obtains the values of Select annotations if present on the method
     * or record component. The order for values is the same as the order in
     * which the annotations are listed. Otherwise a size 0 array.
     *
     * @param element repository method or record component. Must not be null.
     * @return values of the Select annotations indicating the columns to select,
     *         otherwise a size 0 array to indicate no Select annotation is present.
     */
    String[] getSelections(AnnotatedElement element);

    /**
     * Determines if the special parameter value is a Restriction.
     *
     * @param param possible special parameter value.
     * @return true if the value is a Restriction. False otherwise.
     */
    boolean isRestriction(Object param);

    /**
     * Determines if the special parameter type is valid for the type of
     * repository method.
     *
     * @param paramType type of special parameter.
     * @param queryType type of repository method.
     * @return true if valid. False if not valid.
     */
    boolean isSpecialParamValid(Class<?> paramType,
                                QueryType queryType);

    /**
     * Returns the repository method annotations that accept JPQL
     * (such as Query).
     *
     * @return the annotation classes.
     */
    Set<Class<? extends Annotation>> jpqlQueryAnnoTypes();

    /**
     * Returns the repository method annotations that represent life cycle
     * operations (such as Delete and Insert) for either a stateful or
     * stateless repository, depending on the parameter.
     *
     * @param stateful true for a stateful repository; false for stateless;
     *                     null for both.
     * @return the annotation classes.
     */
    Collection<Class<? extends Annotation>> lifeCycleAnnoTypes(Boolean stateful);

    /**
     * Returns the repository method annotations that represent operations
     * (such as Find and Delete, but not OrderBy) for either a stateful or
     * stateless repository, depending on the parameter. This method is
     * intended for error reporting and is not written to be efficient.
     *
     * @param method   the repository method if experimental annotation types
     *                     should be included. Otherwise null.
     * @param stateful true for a stateful repository; false for stateless;
     *                     null for both.
     * @return the annotation classes.
     */
    @Trivial
    default Collection<Class<? extends Annotation>> operationAnnoTypes(Method method,
                                                                       Boolean stateful) {
        TreeMap<String, Class<? extends Annotation>> sorted = new TreeMap<>();

        sorted.put(Find.class.getName(), Find.class);

        for (Class<? extends Annotation> annoClass : jpqlQueryAnnoTypes())
            sorted.put(annoClass.getSimpleName(), annoClass);

        for (Class<? extends Annotation> annoClass : lifeCycleAnnoTypes(stateful))
            sorted.put(annoClass.getSimpleName(), annoClass);

        if (method != null) {
            Annotation anno;
            if ((anno = getCountAnnotation(method)) != null)
                sorted.put(anno.annotationType().getName(), anno.annotationType());

            if ((anno = getExistsAnnotation(method)) != null)
                sorted.put(anno.annotationType().getName(), anno.annotationType());
        }

        return sorted.values();
    }

    /**
     * Returns the names of annotations that are valid on the parameters of a
     * parameter-based update method.
     *
     * @return the annotation names.
     */
    String paramAnnosForUpdate();

    /**
     * Returns the name of the Liberty feature that provides Jakarta Persistence.
     * For example, persistence-3.2.
     *
     * @return the name of the Liberty feature that provides Jakarta Persistence.
     */
    String persistenceFeatureName();

    /**
     * List of valid return types for resource accessor methods.
     *
     * @param stateful true for a stateful repository; false for stateless.
     * @return valid return types.
     */
    Set<Class<?>> resourceAccessorTypes(boolean stateful);

    /**
     * Returns the names of special parameter types that are valid for repository
     * find operations.
     *
     * @return names of valid special parameter types.
     */
    String specialParamsForFind();

    /**
     * Returns the names of special parameter types that are valid for repository
     * find-and-delete operations.
     *
     * @return names of valid special parameter types.
     */
    String specialParamsForFindAndDelete();

    /**
     * Returns the Jakarta Data defined parameter types with special meaning
     * that can be used on repository methods after the query parameters.
     *
     * @return the Jakarta Data defined special parameter types.
     */
    Set<Class<?>> specialParamTypes();
}
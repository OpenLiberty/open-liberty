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
package io.openliberty.data.internal.persistence.cdi;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.QueryInfo;
import io.openliberty.data.internal.persistence.RepositoryImpl;
import io.openliberty.data.internal.persistence.Util;
import jakarta.data.exceptions.DataException;
import jakarta.data.repository.Repository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

/**
 * Producer for repository implementation that is provided by the container/runtime.
 *
 * @param <R> repository interface.
 */
public class RepositoryProducer<R> implements Producer<R>, ProducerFactory<R>, BeanAttributes<R> {
    private final static TraceComponent tc = Tr.register(RepositoryProducer.class);

    private static final Set<Annotation> QUALIFIERS = Set.of(Any.Literal.INSTANCE, Default.Literal.INSTANCE);

    private final BeanManager beanMgr;
    private final Set<Type> beanTypes;
    private final FutureEMBuilder futureEMBuilder;
    private final DataExtension extension;
    private final Map<R, R> intercepted = new ConcurrentHashMap<>();
    private final Class<?> primaryEntityClass;
    private final DataProvider provider;
    private final Map<Class<?>, List<QueryInfo>> queriesPerEntityClass;
    private final Class<?> repositoryInterface;

    RepositoryProducer(Class<?> repositoryInterface, BeanManager beanMgr, DataProvider provider, DataExtension extension,
                       FutureEMBuilder futureEMBuilder, Class<?> primaryEntityClass, Map<Class<?>, List<QueryInfo>> queriesPerEntityClass) {
        this.beanMgr = beanMgr;
        this.beanTypes = Set.of(repositoryInterface);
        this.extension = extension;
        this.futureEMBuilder = futureEMBuilder;
        this.primaryEntityClass = primaryEntityClass;
        this.provider = provider;
        this.queriesPerEntityClass = queriesPerEntityClass;
        this.repositoryInterface = repositoryInterface;
        provider.producerCreated(futureEMBuilder.moduleName.getApplication(), this);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Trivial
    public <T> Producer<T> createProducer(Bean<T> bean) {
        return (Producer<T>) this;
    }

    @Override
    public void dispose(R repository) {
        R r = intercepted.remove(repository);
        if (r != null)
            repository = r;

        RepositoryImpl<?> handler = (RepositoryImpl<?>) Proxy.getInvocationHandler(repository);
        handler.beanDisposed();
    }

    @Override
    @Trivial
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public String getName() {
        return null;
    }

    @Override
    @Trivial
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    @Trivial
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    @Trivial
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    @Trivial
    public Set<Type> getTypes() {
        return beanTypes;
    }

    /**
     * Write information about this instance to the introspection file for
     * Jakarta Data.
     *
     * @param writer writes to the introspection file.
     * @param indent indentation for lines.
     * @return list of QueryInfo for the caller to log.
     */
    @Trivial
    public List<QueryInfo> introspect(PrintWriter writer, String indent) {
        List<QueryInfo> queryInfos = new ArrayList<>();

        writer.println(indent + "RepositoryProducer@" + Integer.toHexString(hashCode()));
        writer.println(indent + "  repository: " + repositoryInterface.getName());
        writer.println(indent + "  primary entity: " +
                       (primaryEntityClass == null ? null : primaryEntityClass.getName()));
        writer.println(indent + "  intercepted: " + intercepted);

        writer.println();
        writer.println(Util.toString(repositoryInterface, indent + "  "));

        queriesPerEntityClass.forEach((entityClass, queries) -> {
            writer.println();
            if (QueryInfo.ENTITY_TBD.equals(entityClass))
                writer.println(indent + "  Queries for entity to be determined:");
            else
                writer.println(indent + "  Queries for entity " + entityClass.getName() + ':');

            TreeMap<String, QueryInfo> sorted = new TreeMap<>();
            for (QueryInfo qi : queries)
                sorted.put(qi.method.toString(), qi);

            for (QueryInfo qi : sorted.values())
                writer.println(indent + "    " + qi.toString() //
                                .replace('\r', ' ') // print on single line
                                .replace('\n', ' '));

            queryInfos.addAll(queries);
        });

        writer.println();
        futureEMBuilder.introspect(writer, "  " + indent);

        return queryInfos;
    }

    @Override
    @Trivial
    public boolean isAlternative() {
        return false;
    }

    @FFDCIgnore(Throwable.class)
    @Override
    @Trivial
    public R produce(CreationalContext<R> cc) {
        @SuppressWarnings("unchecked")
        Class<R> repositoryInterface = (Class<R>) this.repositoryInterface;

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "produce", cc, repositoryInterface.getName());

        try {
            InterceptionFactory<R> interception = beanMgr.createInterceptionFactory(cc, repositoryInterface);

            boolean intercept = false;
            AnnotatedTypeConfigurator<R> configurator = interception.configure();
            for (Annotation anno : configurator.getAnnotated().getAnnotations())
                if (beanMgr.isInterceptorBinding(anno.annotationType())) {
                    intercept = true;
                    configurator.add(anno);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "add " + anno + " for " + configurator.getAnnotated().getJavaClass());
                }
            for (AnnotatedMethodConfigurator<? super R> method : configurator.methods())
                for (Annotation anno : method.getAnnotated().getAnnotations())
                    if (beanMgr.isInterceptorBinding(anno.annotationType())) {
                        intercept = true;
                        method.add(anno);
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "add " + anno + " for " + method.getAnnotated().getJavaMember());
                    }

            RepositoryImpl<?> handler = new RepositoryImpl<>(provider, extension, futureEMBuilder, //
                            repositoryInterface, primaryEntityClass, queriesPerEntityClass);

            R instance = repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                                         new Class<?>[] { repositoryInterface },
                                                                         handler));

            if (intercept) {
                R r = interception.createInterceptedInstance(instance);
                intercepted.put(r, instance);
                instance = r;
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "produce", instance.toString());
            return instance;
        } catch (Throwable x) {
            if (x instanceof DataException || x.getCause() instanceof DataException)
                ; // already logged the error
            else
                Tr.error(tc, "CWWKD1095.repo.err",
                         repositoryInterface.getName(),
                         repositoryInterface.getAnnotation(Repository.class),
                         primaryEntityClass == null ? null : primaryEntityClass.getName(),
                         x);
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "produce", x);
            throw x;
        }
    }
}
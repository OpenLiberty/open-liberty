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

import static io.openliberty.data.internal.persistence.cdi.DataExtension.exc;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.data.internal.persistence.DataProvider;
import io.openliberty.data.internal.persistence.EntityManagerBuilder;
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

    /**
     * Amount of time in seconds to wait for an EntityManagerBuilder to become
     * available.
     */
    private static final long INIT_TIMEOUT_SEC = TimeUnit.MINUTES.toSeconds(5);

    private static final Set<Annotation> QUALIFIERS = //
                    Set.of(Any.Literal.INSTANCE, Default.Literal.INSTANCE);

    private final BeanManager beanMgr;
    private final Set<Type> beanTypes;
    private final FutureEMBuilder futureEMBuilder;
    private final DataExtension extension;
    private final Map<R, R> intercepted = new ConcurrentHashMap<>();
    private final Class<?> primaryEntityClass;
    private final DataProvider provider;
    private final Map<Class<?>, List<QueryInfo>> queriesPerEntityClass;
    private final AtomicReference<RepositoryImpl<?>> repositoryImplRef = //
                    new AtomicReference<>(); // most recently created instance
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
        provider.producerCreated(futureEMBuilder.jeeName.getApplication(), this);
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
        repositoryImplRef.compareAndSet(handler, null);
        handler.beanDisposed();
    }

    /**
     * Error handling for a timeout that occurs when attempting to obtain an
     * EntityManagerBuilder.
     *
     * @param repositoryInterface the repository interface.
     * @param cause               the TimeoutException.
     * @return DataException with an appropriate error message.
     */
    @Trivial
    private DataException excTimedOut(Class<?> repositoryInterface,
                                      Throwable cause) {
        DataException x;
        if (CheckpointPhase.getPhase().restored()) {
            // No checkpoint in progress
            x = exc(DataException.class,
                    "CWWKD1106.init.timed.out",
                    repositoryInterface.getName(),
                    futureEMBuilder.dataStore,
                    futureEMBuilder.jeeName,
                    INIT_TIMEOUT_SEC);
        } else { // during checkpoint
            ComponentMetaData metadata = ComponentMetaDataAccessorImpl //
                            .getComponentMetaDataAccessor() //
                            .getComponentMetaData();
            J2EEName jeeName = metadata == null //
                            ? futureEMBuilder.jeeName //
                            : metadata.getJ2EEName();

            x = exc(DataException.class,
                    "CWWKD1107.init.timed.out.checkpoint",
                    jeeName,
                    repositoryInterface.getName());
        }
        x.initCause(cause);
        return x;
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
     * @param writer          writes to the introspection file.
     * @param indent          indentation for lines.
     * @param repositoryImpls list to populate with a RepositoryImpl that is produced
     *                            by this producer.
     * @return list of QueryInfo for the caller to log.
     */
    @Trivial
    public List<QueryInfo> introspect(PrintWriter writer,
                                      String indent,
                                      List<RepositoryImpl<?>> repositoryImpls) {
        RepositoryImpl<?> repositoryImpl = repositoryImplRef.get();
        if (repositoryImpl != null)
            repositoryImpls.add(repositoryImpl);

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

            EntityManagerBuilder builder = futureEMBuilder.get(INIT_TIMEOUT_SEC, //
                                                               TimeUnit.SECONDS);

            RepositoryImpl<?> handler = new RepositoryImpl<>(provider, extension, builder, //
                            repositoryInterface, primaryEntityClass, queriesPerEntityClass);

            R instance = repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                                         new Class<?>[] { repositoryInterface },
                                                                         handler));

            if (intercept) {
                R r = interception.createInterceptedInstance(instance);
                intercepted.put(r, instance);
                instance = r;
            }

            repositoryImplRef.set(handler);

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

            if (x instanceof TimeoutException) {
                DataException dx = excTimedOut(repositoryInterface, x);
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "produce", DataException.class);
                throw dx;
            } else {
                if (trace && tc.isEntryEnabled())
                    Tr.exit(this, tc, "produce", x);
                if (x instanceof RuntimeException)
                    throw (RuntimeException) x;
                else if (x instanceof Error)
                    throw (Error) x;
                else if (x instanceof ExecutionException)
                    throw toRuntimeException((ExecutionException) x);
                else // InterruptedException
                    throw new DataException(x);
            }
        }
    }

    /**
     * Converts an ExecutionException and its cause into a RuntimeException of the
     * same type as the cause exception, or DataException if the cause is not a
     * subtype of RuntimeException that has a constructor with arguments
     * (String, Throwable).
     *
     * @param xx ExecutionException with a cause indicating a failure that occurred
     *               during asynchronous initialization.
     * @return DataException with the same cause.
     */
    @FFDCIgnore(Throwable.class)
    @Trivial
    private RuntimeException toRuntimeException(ExecutionException xx) {
        RuntimeException x = null;
        Throwable cause = xx.getCause();
        if (cause instanceof RuntimeException)
            try {
                @SuppressWarnings("unchecked")
                Class<RuntimeException> cl = (Class<RuntimeException>) cause.getClass();
                // All known DataException subtypes and many other runtime exceptions
                // such as UnsupportedOperationException have a constructor of the form,
                Constructor<RuntimeException> cons = cl.getConstructor(String.class,
                                                                       Throwable.class);
                x = cons.newInstance(cause.getMessage(), cause);
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "no matching constructor for " +
                                       cause.getClass());
            }

        if (x == null)
            x = new DataException(cause.getMessage(), cause);

        x.setStackTrace(xx.getStackTrace());
        return x;
    }
}
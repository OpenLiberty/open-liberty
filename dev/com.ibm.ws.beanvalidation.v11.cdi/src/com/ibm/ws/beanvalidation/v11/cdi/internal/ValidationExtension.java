/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v11.cdi.internal;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.validation.BootstrapConfiguration;
import javax.validation.Configuration;
import javax.validation.Constraint;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.MethodType;

import org.apache.bval.cdi.BValAnnotatedType;
import org.apache.bval.cdi.BValBinding;
import org.apache.bval.cdi.BValInterceptor;
import org.apache.bval.cdi.ValidatorBean;
import org.apache.bval.cdi.ValidatorFactoryBean;
import org.apache.commons.lang3.Validate;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.BeanValidationExtensionHelper;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/**
 * This class is the CDI integration point for creating the Validator and Validator Factory objects
 * that as of Bean Validation 1.1 are handled by the Bean Validation implementation. Additionally,
 * support for method and constructor validation interceptors is provided. Most of the function is
 * reused from the Apache BValExtension, but the addBValBeans had to be overridden to allow for delaying
 * the creation of the Validator and ValidatorFactory objects.
 */
@Component(service = WebSphereCDIExtension.class,
           property = { "api.classes=javax.validation.Validator;javax.validation.ValidatorFactory;org.apache.bval.cdi.BValInterceptor;org.apache.bval.cdi.BValExtension" },
           immediate = true)
public class ValidationExtension extends ValidationExtensionService implements Extension, WebSphereCDIExtension {
    private static final TraceComponent tc = Tr.register(ValidationExtension.class);
    private static final Logger LOGGER = Logger.getLogger(ValidationExtension.class.getName());
    private static ValidationExtension instance;

    private static final AnnotatedTypeFilter DEFAULT_ANNOTATED_TYPE_FILTER = new AnnotatedTypeFilter() {

        @Override
        public boolean accept(AnnotatedType<?> annotatedType) {
            if (annotatedType.getJavaClass().getName().startsWith("org.apache.bval.")) {
                return false;
            }
            return true;
        }
    };

    private static AnnotatedTypeFilter annotatedTypeFilter = DEFAULT_ANNOTATED_TYPE_FILTER;

    public static void setAnnotatedTypeFilter(AnnotatedTypeFilter annotatedTypeFilter) {
        ValidationExtension.annotatedTypeFilter = Validate.notNull(annotatedTypeFilter);
    }

    private boolean validatorFound = Boolean.getBoolean("bval.in-container");
    private boolean validatorFactoryFound = Boolean.getBoolean("bval.in-container");

    private boolean validBean;
    private boolean validConstructors;
    private boolean validBusinessMethods;
    private boolean validGetterMethods;

    private Configuration<?> config = null;

    private Set<ExecutableType> globalExecutableTypes;
    private boolean isExecutableValidationEnabled;

    public ValidationExtension() {

    }

    private static void setInstance(ValidationExtension ve) {
        instance = ve;
    }

    static ValidationExtension getInstance() {
        return instance;
    }

    @Override
    protected void activate(ComponentContext cc) {
        super.activate(cc);
        setInstance(this);
    }

    public void initValidationExtension(ClassLoader appCl) {
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoader classLoader = null;

        try {
            ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);
            classLoader = instance().configureBvalClassloader(appCl);

            //Use customer classloader to handle multiple validation.xml being in the same ear.
            classLoader = BeanValidationExtensionHelper.newValidationClassLoader(classLoader);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(classLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called setClassLoader with oldClassLoader of " + oldClassLoader + " and newClassLoader of " + classLoader);
            }

            config = Validation.byDefaultProvider().configure();
            try {
                final BootstrapConfiguration bootstrap = config.getBootstrapConfiguration();
                globalExecutableTypes = Collections.unmodifiableSet(convertToRuntimeTypes(bootstrap.getDefaultValidatedExecutableTypes()));
                isExecutableValidationEnabled = bootstrap.isExecutableValidationEnabled();

                // TODO we never contain IMPLICIT or ALL
                validBean = globalExecutableTypes.contains(ExecutableType.IMPLICIT) || globalExecutableTypes.contains(ExecutableType.ALL);
                validConstructors = validBean || globalExecutableTypes.contains(ExecutableType.CONSTRUCTORS);
                validBusinessMethods = validBean || globalExecutableTypes.contains(ExecutableType.NON_GETTER_METHODS);
                validGetterMethods = globalExecutableTypes.contains(ExecutableType.ALL) || globalExecutableTypes.contains(ExecutableType.GETTER_METHODS);
            } catch (final Exception e) { // custom providers can throw an exception
                LOGGER.log(Level.SEVERE, e.getMessage(), e);

                globalExecutableTypes = Collections.emptySet();
                isExecutableValidationEnabled = false;
            }

        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null Configuration: " + e.getMessage());
            }
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                }
            }
            if (setClassLoader != null && setClassLoader.wasChanged) {
                ValidationExtensionService.instance().releaseLoader(classLoader);
            }
        }
    }

    // lazily to get a small luck to have CDI in place
    private ValidatorFactory ensureFactoryValidator(ClassLoader appCl) {
        config.addProperty("bval.before.cdi", "true");
        return BeanValidationExtensionHelper.validatorFactoryAccessorProxy(appCl);
    }

    private static Set<ExecutableType> convertToRuntimeTypes(final Set<ExecutableType> defaultValidatedExecutableTypes) {
        final Set<ExecutableType> types = EnumSet.noneOf(ExecutableType.class);
        for (final ExecutableType type : defaultValidatedExecutableTypes) {
            if (ExecutableType.NONE == type) {
                continue;
            }
            if (ExecutableType.ALL == type) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
                types.add(ExecutableType.GETTER_METHODS);
                break;
            }
            if (ExecutableType.IMPLICIT == type) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
            } else {
                types.add(type);
            }
        }
        return types;
    }

    public Set<ExecutableType> getGlobalExecutableTypes() {
        initValidationExtension(null);
        return globalExecutableTypes;
    }

    public void addBvalBinding(final @Observes BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addInterceptorBinding(BValBinding.class);
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(BValInterceptor.class));
    }

    public <A> void processAnnotatedType(final @Observes @WithAnnotations({ Valid.class, Constraint.class, ValidateOnExecution.class }) ProcessAnnotatedType<A> pat,
                                         final BeanManager beanManager) {
        // CDI can end up calling this method during applicationStarting which means that we don't
        // have any component/module metadata.  In order to properly find the correct validation.xml,
        // we need to have that metadata.  For this case, we simply ignore the call -- and the PAT
        // can be processed at runtime.
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            SetContextClassLoaderPrivileged setClassLoader = null;
            ClassLoader oldClassLoader = null;
            ClassLoader classLoader = null;
            try {
                classLoader = instance().configureBvalClassloader(pat.getAnnotatedType().getJavaClass().getClassLoader());
                ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);

                // set the thread context class loader to be used, must be reset in finally block
                setClassLoader = new SetContextClassLoaderPrivileged(tca);
                oldClassLoader = setClassLoader.execute(classLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Called setClassLoader with oldClassLoader of" + oldClassLoader + " and newClassLoader of " + classLoader);
                }

                internalProcessAnnotatedType(pat);
            } finally {
                if (setClassLoader != null) {
                    setClassLoader.execute(oldClassLoader);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                    }
                }
                if (setClassLoader != null && setClassLoader.wasChanged) {
                    instance().releaseLoader(classLoader);
                }
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "No CMD on thread - ignoring call");
        }

    }

    private <A> void internalProcessAnnotatedType(final @Observes @WithAnnotations({ Valid.class, Constraint.class, ValidateOnExecution.class }) ProcessAnnotatedType<A> pat) {
        initValidationExtension(pat.getAnnotatedType().getJavaClass().getClassLoader());
        if (!isExecutableValidationEnabled) {
            return;
        }

        final AnnotatedType<A> annotatedType = pat.getAnnotatedType();

        if (!annotatedTypeFilter.accept(annotatedType)) {
            return;
        }

        final Class<A> javaClass = annotatedType.getJavaClass();
        final int modifiers = javaClass.getModifiers();
        if (!javaClass.isInterface() && !Modifier.isFinal(modifiers) && !Modifier.isAbstract(modifiers)) {
            ValidatorFactory factory = ensureFactoryValidator(pat.getAnnotatedType().getJavaClass().getClassLoader());
            try {
                Validator validator = factory.getValidator();
                try {
                    final BeanDescriptor classConstraints = validator.getConstraintsForClass(javaClass);
                    if (annotatedType.isAnnotationPresent(ValidateOnExecution.class)
                        || hasValidationAnnotation(annotatedType.getMethods())
                        || hasValidationAnnotation(annotatedType.getConstructors())
                        || classConstraints != null
                           && (validBean && classConstraints.isBeanConstrained()
                               || validConstructors && !classConstraints.getConstrainedConstructors().isEmpty()
                               || validBusinessMethods && !classConstraints.getConstrainedMethods(MethodType.NON_GETTER).isEmpty()
                               || validGetterMethods && !classConstraints.getConstrainedMethods(MethodType.GETTER).isEmpty())) {
                        final BValAnnotatedType<A> bValAnnotatedType = new BValAnnotatedType<A>(annotatedType);
                        pat.setAnnotatedType(bValAnnotatedType);
                    }
                } catch (final NoClassDefFoundError ncdfe) {
                    // skip
                }
            } catch (final ValidationException ve) {
                LOGGER.log(Level.FINEST, ve.getMessage(), ve);
            } finally {
                factory.close();
            }
        }
    }

    private static <A> boolean hasValidationAnnotation(final Collection<? extends AnnotatedCallable<? super A>> methods) {
        for (final AnnotatedCallable<? super A> m : methods) {
            if (m.isAnnotationPresent(ValidateOnExecution.class)) {
                return true;
            }
        }
        return false;
    }

    public <A> void processBean(final @Observes ProcessBean<A> processBeanEvent) {
        if (validatorFound && validatorFactoryFound) {
            return;
        }

        final Bean<A> bean = processBeanEvent.getBean();
        if (ValidatorBean.class.isInstance(bean) || ValidatorFactoryBean.class.isInstance(bean)) {
            return;
        }

        final Set<Type> types = bean.getTypes();
        if (!validatorFound) {
            validatorFound = types.contains(Validator.class);
        }
        if (!validatorFactoryFound) {
            validatorFactoryFound = types.contains(ValidatorFactory.class);
        }
    }

    public void addBValBeans(final @Observes AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        initValidationExtension(null);

        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoader classLoader = null;
        try {
            classLoader = instance().configureBvalClassloader(null);
            ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(classLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called setClassLoader with oldClassLoader of" + oldClassLoader + " and newClassLoader of " + classLoader);
            }

            cdiIntegration(afterBeanDiscovery, beanManager);
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                }
            }
            if (setClassLoader != null && setClassLoader.wasChanged) {
                instance().releaseLoader(classLoader);
            }
        }
    }

    private void cdiIntegration(final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        try {
            config.addProperty("bval.before.cdi", "false");
        } catch (final Exception e) {
            // ignore
        }

        if (!validatorFactoryFound) {
            try {
                afterBeanDiscovery.addBean(new LibertyValidatorFactoryBean());
                validatorFactoryFound = true;
            } catch (final IllegalStateException e) {
                throw new ValidationException(e);
            }
        }
        if (!validatorFound) {
            try {
                afterBeanDiscovery.addBean(new LibertyValidatorBean());
                afterBeanDiscovery.addBean(new BValExtensionBean());
                validatorFound = true;
            } catch (final IllegalStateException e) {
                throw new ValidationException(e);
            }
        }
    }

    /**
     * Request that an instance of the specified type be provided by the container.
     *
     * @param clazz
     * @return the requested instance wrapped in a {@link Releasable}.
     */
    public static <T> Releasable<T> inject(final Class<T> clazz) {
        try {
            final BeanManager beanManager = CDI.current().getBeanManager();
            if (beanManager == null) {
                return null;
            }
            final AnnotatedType<T> annotatedType = beanManager.createAnnotatedType(clazz);
            final InjectionTarget<T> it = beanManager.createInjectionTarget(annotatedType);
            final CreationalContext<T> context = beanManager.createCreationalContext(null);
            final T instance = it.produce(context);
            it.inject(instance, context);
            it.postConstruct(instance);

            return new Releasable<T>(context, it, instance);
        } catch (final Exception e) {
            // no-op
        } catch (final NoClassDefFoundError error) {
            // no-op
        }
        return null;
    }

    public static BeanManager getBeanManager() {
        return CDI.current().getBeanManager();
    }

    /**
     * Represents an item that can be released from a {@link CreationalContext} at some point in the future.
     *
     * @param <T>
     */
    public static class Releasable<T> {
        private final CreationalContext<T> context;
        private final InjectionTarget<T> injectionTarget;
        private final T instance;

        private Releasable(final CreationalContext<T> context, final InjectionTarget<T> injectionTarget, final T instance) {
            this.context = context;
            this.injectionTarget = injectionTarget;
            this.instance = instance;
        }

        public void release() {
            try {
                injectionTarget.preDestroy(instance);
                injectionTarget.dispose(instance);
                context.release();
            } catch (final Exception e) {
                // no-op
            } catch (final NoClassDefFoundError e) {
                // no-op
            }
        }

        public T getInstance() {
            return instance;
        }
    }

    /**
     * Defines an item that can determine whether a given {@link AnnotatedType} will be processed
     * by the {@link ValidationExtension} for executable validation. May be statically applied before
     * container startup.
     *
     * @see ValidationExtension#setAnnotatedTypeFilter(AnnotatedTypeFilter)
     */
    public interface AnnotatedTypeFilter {
        boolean accept(AnnotatedType<?> annotatedType);
    }
}

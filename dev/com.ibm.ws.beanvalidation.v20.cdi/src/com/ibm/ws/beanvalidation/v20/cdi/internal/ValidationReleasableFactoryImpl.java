/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v20.cdi.internal;

import java.security.AccessController;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.BeanManager;
import javax.validation.BootstrapConfiguration;
import javax.validation.ClockProvider;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidatorFactory;
import javax.validation.valueextraction.ValueExtractor;

import org.hibernate.validator.cdi.internal.InjectingConstraintValidatorFactory;
import org.hibernate.validator.internal.engine.valueextraction.ValueExtractorDescriptor;
import org.hibernate.validator.internal.util.privilegedactions.GetInstancesFromServiceLoader;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.service.ValidationReleasable;
import com.ibm.ws.beanvalidation.service.ValidationReleasableFactory;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.kernel.service.util.PrivHelper;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * An implementation that is CDI aware.
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL,
           immediate = true,
           property = { "type=CDIValidationReleasableFactory" })
public class ValidationReleasableFactoryImpl implements ValidationReleasableFactory, ComponentMetaDataListener {

    private static final TraceComponent tc = Tr.register(ValidationReleasableFactoryImpl.class);
    private static final String REFERENCE_CDI_SERVICE = "cdiService";
    private static final String REFERENCE_MANAGED_OBJECT_SERVICE = "managedObjectService";

    private final AtomicServiceReference<CDIService> cdiService = new AtomicServiceReference<CDIService>(REFERENCE_CDI_SERVICE);
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef = new AtomicServiceReference<ManagedObjectService>(REFERENCE_MANAGED_OBJECT_SERVICE);
    private final Map<ComponentMetaData, BeanManager> beanManagers = new WeakHashMap<ComponentMetaData, BeanManager>();

    @Override
    public <T> ManagedObject<T> createValidationReleasable(Class<T> clazz) {
        return null;
    }

    @Override
    public ValidationReleasable<ConstraintValidatorFactory> createConstraintValidatorFactory() {
        return null;
    }

    @Override
    public ValidatorFactory injectValidatorFactoryResources(Configuration<?> config, ClassLoader appClassLoader) {
        if (getCurrentBeanManager() != null) {
            createManagedConstraintValidatorFactory(config, appClassLoader);
            createManagedMessageInterpolator(config, appClassLoader);
            createManagedTraversableResolver(config, appClassLoader);
            createManagedParameterNameProvider(config, appClassLoader);
            createManagedClockProvider(config, appClassLoader);
            addValueExtractorBeans(config, appClassLoader);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "managedObjectService is null, skipping creating CDI enhanced objects.");
        }
        return config.buildValidatorFactory();
    }

    private BeanManager getCurrentBeanManager() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        BeanManager beanMgr = beanManagers.get(cmd);
        if (beanMgr == null) {
            beanMgr = cdiService.getServiceWithException().getCurrentBeanManager();
            beanManagers.put(cmd, beanMgr);
        }
        return beanMgr;
    }

    @Activate
    protected void activate(ComponentContext cc) {
        cdiService.activate(cc);
        managedObjectServiceRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        cdiService.deactivate(cc);
        managedObjectServiceRef.deactivate(cc);
    }

    @Reference(name = REFERENCE_CDI_SERVICE, service = CDIService.class)
    protected void setCdiService(ServiceReference<CDIService> ref) {
        cdiService.setReference(ref);
    }

    protected void unsetCdiService(ServiceReference<CDIService> ref) {
        cdiService.unsetReference(ref);
    }

    @Reference(name = REFERENCE_MANAGED_OBJECT_SERVICE,
               service = ManagedObjectService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        managedObjectServiceRef.setReference(ref);
    }

    protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        managedObjectServiceRef.unsetReference(ref);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.metadata.ComponentMetaDataListener#componentMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void componentMetaDataCreated(MetaDataEvent<ComponentMetaData> event) {
        // no-op

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.metadata.ComponentMetaDataListener#componentMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void componentMetaDataDestroyed(MetaDataEvent<ComponentMetaData> event) {
        BeanManager beanManager = beanManagers.remove(event.getMetaData());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Removed bean manager from cache: ", beanManager);
    }

    private void createManagedMessageInterpolator(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String messageInterpolatorClassName = bootstrapConfiguration.getMessageInterpolatorClassName();
        MessageInterpolator mi = null;

        if (messageInterpolatorClassName == null) {
            mi = config.getDefaultMessageInterpolator();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends MessageInterpolator> messageInterpolatorClass = (Class<? extends MessageInterpolator>) loadClass(messageInterpolatorClassName, appClassLoader);
            mi = createManagedObject(messageInterpolatorClass);
        }

        if (mi != null) {
            config.messageInterpolator(mi);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Failed to create a CDI managed object for MessageInterpolator class(null means default) " + messageInterpolatorClassName);
        }
    }

    private void createManagedTraversableResolver(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String traversableResolverClassName = bootstrapConfiguration.getTraversableResolverClassName();
        TraversableResolver tr = null;

        if (traversableResolverClassName == null) {
            tr = config.getDefaultTraversableResolver();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends TraversableResolver> traversableResolverClass = (Class<? extends TraversableResolver>) loadClass(traversableResolverClassName, appClassLoader);
            tr = createManagedObject(traversableResolverClass);
        }

        if (tr != null) {
            config.traversableResolver(tr);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Failed to create a CDI managed object for TraversableResolver class(null means default) " + traversableResolverClassName);
        }
    }

    private void createManagedParameterNameProvider(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String parameterNameProviderClassName = bootstrapConfiguration.getParameterNameProviderClassName();
        ParameterNameProvider pnp = null;

        if (parameterNameProviderClassName == null) {
            pnp = config.getDefaultParameterNameProvider();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends ParameterNameProvider> parameterNameProviderClass = (Class<? extends ParameterNameProvider>) loadClass(parameterNameProviderClassName, appClassLoader);
            pnp = createManagedObject(parameterNameProviderClass);
        }

        if (pnp != null) {
            config.parameterNameProvider(pnp);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Failed to create a CDI managed object for ParameterNameProvider class(null means default) " + parameterNameProviderClassName);
        }
    }

    private void createManagedClockProvider(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        String clockProviderClassName = bootstrapConfiguration.getClockProviderClassName();
        ClockProvider clockProvider = null;

        if (clockProviderClassName == null) {
            clockProvider = config.getDefaultClockProvider();
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends ClockProvider> clockProviderClass = (Class<? extends ClockProvider>) loadClass(clockProviderClassName, appClassLoader);
            clockProvider = createManagedObject(clockProviderClass);
        }

        if (clockProvider != null) {
            config.clockProvider(clockProvider);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Failed to create a CDI managed object for ClockProvider class(null means default) " + clockProviderClassName);
        }
    }

    private void createManagedConstraintValidatorFactory(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration configSource = config.getBootstrapConfiguration();
        String constraintValidatorFactoryClassName = configSource.getConstraintValidatorFactoryClassName();
        ConstraintValidatorFactory cvf = null;

        if (constraintValidatorFactoryClassName == null) {
            // use default
//            ManagedObject<?> mangedObject = createManagedObject(new ReleasableConstraintValidatorFactory(this));
//            cvf = (ConstraintValidatorFactory) mangedObject.getObject();
            cvf = createManagedObject(InjectingConstraintValidatorFactory.class);
        } else {
            @SuppressWarnings("unchecked")
            Class<? extends ConstraintValidatorFactory> constraintValidatorFactoryClass = (Class<? extends ConstraintValidatorFactory>) loadClass(constraintValidatorFactoryClassName,
                                                                                                                                                  appClassLoader);

            cvf = createManagedObject(constraintValidatorFactoryClass);
        }

        if (cvf != null) {
            config.constraintValidatorFactory(cvf);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Failed to create a CDI managed object for ConstraintValidatorFactory class(null means default) " + constraintValidatorFactoryClassName);
        }
    }

    private void addValueExtractorBeans(Configuration<?> config, ClassLoader appClassLoader) {
        Map<ValueExtractorDescriptor.Key, ValueExtractorDescriptor> valueExtractorDescriptors = createValidationXmlValueExtractors(config,
                                                                                                                                   appClassLoader).stream().collect(Collectors.toMap(ValueExtractorDescriptor::getKey,
                                                                                                                                                                                     Function.identity()));

        for (ValueExtractorDescriptor serviceLoaderValueExtractorDescriptor : createServiceLoaderValueExtractors()) {
            valueExtractorDescriptors.putIfAbsent(serviceLoaderValueExtractorDescriptor.getKey(), serviceLoaderValueExtractorDescriptor);
        }

        for (ValueExtractorDescriptor valueExtractorDescriptor : valueExtractorDescriptors.values()) {
            config.addValueExtractor(valueExtractorDescriptor.getValueExtractor());
        }
    }

    private Set<ValueExtractorDescriptor> createValidationXmlValueExtractors(Configuration<?> config, ClassLoader appClassLoader) {
        BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
        Set<String> valueExtractorClassNames = bootstrapConfiguration.getValueExtractorClassNames();

        @SuppressWarnings("unchecked")
        Set<ValueExtractorDescriptor> valueExtractorDescriptors = valueExtractorClassNames.stream().map(fqcn -> createManagedObject((Class<? extends ValueExtractor<?>>) loadClass(fqcn,
                                                                                                                                                                                   appClassLoader))).map(valueExtractor -> new ValueExtractorDescriptor(valueExtractor)).collect(Collectors.toSet());

        return valueExtractorDescriptors;
    }

    @SuppressWarnings("rawtypes")
    private Set<ValueExtractorDescriptor> createServiceLoaderValueExtractors() {
        Set<ValueExtractorDescriptor> valueExtractorDescriptors = new HashSet<>();

        List<ValueExtractor> valueExtractors;

        if (System.getSecurityManager() != null) {
            valueExtractors = AccessController.doPrivileged(GetInstancesFromServiceLoader.action(PrivHelper.getContextClassLoader(), ValueExtractor.class));
        } else {
            valueExtractors = GetInstancesFromServiceLoader.action(PrivHelper.getContextClassLoader(), ValueExtractor.class).run();
        }

        for (ValueExtractor<?> valueExtractor : valueExtractors) {
            valueExtractorDescriptors.add(new ValueExtractorDescriptor(createManagedObject(valueExtractor)));
        }

        return valueExtractorDescriptors;
    }

    private <T> ManagedObjectFactory<T> getManagedBeanManagedObjectFactory(Class<T> clazz) {
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        ManagedObjectService managedObjectService = managedObjectServiceRef.getServiceWithException();
        try {
            ManagedObjectFactory<T> factory = managedObjectService.createManagedObjectFactory(mmd, clazz, true);
            if (factory.isManaged()) {
                return factory;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "ManagedObjectFactory for " + clazz.getName() + " was not managed.");
                return null;
            }
        } catch (ManagedObjectException e) {
            // ffdc
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Failed to create a ManagedObjectFactory for " + clazz.getName(), e);
            return null;
        }
    }

    private <T> T createManagedObject(T instance) {
        ModuleMetaData mmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getModuleMetaData();
        ManagedObjectService managedObjectService = managedObjectServiceRef.getServiceWithException();
        ManagedObject<T> mo;
        try {
            mo = managedObjectService.createManagedObject(mmd, instance);
        } catch (ManagedObjectException e) {
            // ffdc
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Failed to create a ManagedObject for an instance of " + instance.getClass().getName(), e);
            return null;
        }
        return mo.getObject();
    }

    private <T> T createManagedObject(Class<T> clazz) {
        // The mof handles calling produce, inject, and postConstruct.
        ManagedObjectFactory<T> mof = getManagedBeanManagedObjectFactory(clazz);
        if (mof == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "ManagedObjectFactory during createManagedObject() was null.");
            return null;
        }

        ManagedObject<T> mo;
        try {
            mo = mof.createManagedObject();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Failed to create a ManagedObject using a ManagedObjectFactory for class type " + mof.getManagedObjectClass(), e);
            return null;
        }
        return mo.getObject();
    }

    private Class<?> loadClass(String className, ClassLoader appClassLoader) {
        try {
            return Class.forName(className, true, appClassLoader);
        } catch (ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Class not found during CDI enablement of the ValidatorFactory.", e);
            return null;
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.BeanDescriptor;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.beanvalidation.config.ValidationConfigurationFactory;
import com.ibm.ws.beanvalidation.config.ValidationConfigurationInterface;
import com.ibm.ws.beanvalidation.service.BeanValidation;
import com.ibm.ws.beanvalidation.service.BeanValidationExtensionHelper;
import com.ibm.ws.beanvalidation.service.BeanValidationRuntimeVersion;
import com.ibm.ws.beanvalidation.service.BeanValidationUsingClassLoader;
import com.ibm.ws.beanvalidation.service.ConstrainedHelper;
import com.ibm.ws.beanvalidation.service.ValidatorFactoryBuilder;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * OSGi specific implementation of the BeanValidation container integration
 * service. <p>
 */
@Component(service = { ModuleMetaDataListener.class,
                       BeanValidationUsingClassLoader.class,
                       BeanValidation.class },
           immediate = true)
public class OSGiBeanValidationImpl extends AbstractBeanValidation implements ModuleMetaDataListener, BeanValidationUsingClassLoader {
    private static final TraceComponent tc = Tr.register(OSGiBeanValidationImpl.class);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static final String REFERENCE_VALIDATION_CONFIG_FACTORY = "validationConfigFactory";
    private static final String REFERENCE_CLASSLOADING_SERVICE = "classLoadingService";
    private static final String REFERENCE_VALIDATOR_FACTORY_BUILDER = "ValidatorFactoryBuilder";
    private static final String REFERENCE_CONSTRAINED_HELPER = "ConstrainedHelper";

    private MetaDataSlot ivModuleMetaDataSlot;

    private final AtomicServiceReference<ValidationConfigurationFactory> validationConfigFactorySR = new AtomicServiceReference<ValidationConfigurationFactory>(REFERENCE_VALIDATION_CONFIG_FACTORY);

    private final AtomicServiceReference<ClassLoadingService> classLoadingServiceSR = new AtomicServiceReference<ClassLoadingService>(REFERENCE_CLASSLOADING_SERVICE);

    private final AtomicServiceReference<ValidatorFactoryBuilder> validatorFactoryBuilderSR = new AtomicServiceReference<ValidatorFactoryBuilder>(REFERENCE_VALIDATOR_FACTORY_BUILDER);

    private final AtomicServiceReference<ConstrainedHelper> constrainedHelperSR = new AtomicServiceReference<ConstrainedHelper>(REFERENCE_CONSTRAINED_HELPER);

    private static final Version DEFAULT_VERSION = BeanValidationRuntimeVersion.VERSION_1_0;
    private Version runtimeVersion = DEFAULT_VERSION;

    private static final PrivilegedAction<ThreadContextAccessor> getThreadContextAccessorAction = new PrivilegedAction<ThreadContextAccessor>() {
        @Override
        public ThreadContextAccessor run() {
            return ThreadContextAccessor.getThreadContextAccessor();
        }
    };

    @Override
    public void registerValidatorFactory(ModuleMetaData mmd, ClassLoader cl, ValidatorFactory validatorFactory) {
        getValidatorFactory(mmd, cl, validatorFactory);
    }

    @Override
    @Trivial
    public ConstraintValidatorFactory getConstraintValidatorFactory(Configuration<?> config) {
        ValidationConfigurationFactory configFactory = validationConfigFactorySR.getServiceWithException();
        return configFactory.getConstraintValidatorFactoryOverride(config);
    }

    @Trivial
    @Override
    public ValidatorFactory getValidatorFactory(ModuleMetaData mmd) {
        return getValidatorFactory(mmd, null);
    }

    @Override
    public ValidatorFactory getValidatorFactory(ModuleMetaData mmd, ClassLoader loader) {
        return getValidatorFactory(mmd, loader, null);
    }

    private ValidatorFactory getValidatorFactory(ModuleMetaData mmd, ClassLoader loader, ValidatorFactory validatorFactoryToSave) {
        if (ivModuleMetaDataSlot == null) {
            throw new ValidationException("Validation not enabled for module " +
                                          mmd.getName() + "; MetaDataSlotService not active");
        }

        if (isBeanValidationVersion20()) {
            return getValidatorFactoryHVProvider(mmd, loader);
        } else {
            return getValidatorFactoryApacheProvider(mmd, loader, validatorFactoryToSave);
        }
    }

    private ValidatorFactory getValidatorFactoryHVProvider(ModuleMetaData mmd, ClassLoader loader) {

        BeanValidationMetaData beanValMetaData = (BeanValidationMetaData) mmd.getMetaData(ivModuleMetaDataSlot);
        if (beanValMetaData == null) {
            throw new ValidationException("Validation not enabled for module " + mmd.getName());
        }

        ValidatorFactory vf = beanValMetaData.getValidatorFactory();

        if (vf == null) {
            synchronized (beanValMetaData) {
                vf = beanValMetaData.getValidatorFactory();
                if (vf == null) {
                    ClassLoader tmpClassLoader;
                    if (loader != null) {
                        tmpClassLoader = loader;
                    } else {
                        tmpClassLoader = beanValMetaData.getModuleClassLoader();
                    }
                    ValidatorFactoryBuilder validatorFactoryBuilder = validatorFactoryBuilderSR.getServiceWithException();
                    vf = validatorFactoryBuilder.buildValidatorFactory(tmpClassLoader, beanValMetaData.getModuleUri());
                    beanValMetaData.setValidatorFactory(vf);
                    mmd.setMetaData(ivModuleMetaDataSlot, beanValMetaData);
                }
            }
        }
        if (vf == null) {
            throw new ValidationException("Validation not enabled for module " + mmd.getName());
        }
        return vf;

    }

    private ValidatorFactory getValidatorFactoryApacheProvider(ModuleMetaData mmd, ClassLoader loader, ValidatorFactory validatorFactoryToSave) {
        OSGiBeanValidationScopeData scopeData = (OSGiBeanValidationScopeData) mmd.getMetaData(ivModuleMetaDataSlot);
        if (scopeData == null) {
            throw new ValidationException("Validation not enabled for module " + mmd.getName());
        }

        ValidatorFactory vf = scopeData.ivValidatorFactory;

        if (vf == null) {
            synchronized (scopeData) {
                vf = scopeData.ivValidatorFactory;
                if (vf == null) {
                    // It's possible that the requesting component is doing so after the app
                    // has been destroyed (i.e. moduleMetaDataDestroyed already called). If so,
                    // indicate by throwing an exception if the version>=11. For compatibility,
                    // leave the v10 case as is.
                    if (scopeData.configuratorReleased && isBeanValidationVersion11OrGreater()) {
                        throw new ValidationException("the module is stopped, so either the ValidatorFactory has " +
                                                      "already been destroyed or it was never created");
                    }

                    ClassLoaderTuple tuple = null;
                    if (loader != null && !classLoadingServiceSR.getServiceWithException().isThreadContextClassLoader(loader)) {
                        tuple = ClassLoaderTuple.of(createTCCL(loader), true);
                        loader = tuple.classLoader;
                    }
                    ClassLoader origLoader = scopeData.setClassLoader(loader);

                    boolean createSuccessful = false;
                    ValidationConfigurationInterface bvalConfigurator = null;
                    try {
                        ValidationConfig config = null;
                        try {
                            config = scopeData.ivModuleContainer.adapt(ValidationConfig.class);
                        } catch (UnableToAdaptException e) {
                            throw new ValidationException(e);
                        }

                        /*
                         * In bval-1.0, if validation.xml wasn't found we would go down the code
                         * path of creating the ValidatorFactory directly using Validation.buildDefaultValidatorFactory.
                         * Doing such directly still allowed the bval provider to try to find
                         * validation.xml itself (even though the container determined there was
                         * not one in the correct location). The provider looks for any META-INF/validation.xml
                         * on the classpath, so due to an applications class loading structure
                         * the provider could potentially find the xml from other modules that it isn't
                         * supposed to. As such, keeping the existing behavior for bval-1.0, but correcting
                         * it for bval-1.1 to go down the path to tell the provider to ignore xml by
                         * calling Validation.byDefaultProvider().configure().ignoreXmlConfiguration().
                         */
                        boolean bVal11OrHigher = isBeanValidationVersion11OrGreater();
                        // we can look in the app classpath (i.e. beyond just the module) for bval-1.0.  we can also
                        // look in the app classpath for bval-1.1, but only when the user has configured:
                        //     -Dcom.ibm.ws.beanvalidation.allowMultipleConfigsPerApp=false
                        boolean canLookInAppClassPath = !bVal11OrHigher || !BeanValidationExtensionHelper.IS_VALIDATION_CLASSLOADING_ENABLED;
                        if (config == null && canLookInAppClassPath) {
                            vf = ValidatorFactoryAccessor.getValidatorFactory(loader, bVal11OrHigher, validatorFactoryToSave != null);
                        } else {
                            // use the config factory service and allow it to create the correct
                            // type of ValidationConfigurator
                            ValidationConfigurationFactory configFactory = validationConfigFactorySR.getServiceWithException();
                            bvalConfigurator = configFactory.createValidationConfiguration(scopeData, config);

                            // If validatorFactoryToSave is not null, then a ValidatorFactory was already created
                            // by the CDI bval extension and we'll save that one in the scopeData instead of trying
                            // to create a new one.
                            vf = ValidatorFactoryAccessor.getValidatorFactory(bvalConfigurator, validatorFactoryToSave != null);
                        }

                        // if CDI configured a vf first, we always use this - but the preceding code was
                        // still necessary to complete the configuration
                        if (validatorFactoryToSave != null) {
                            vf = validatorFactoryToSave;
                        }

                        scopeData.ivValidatorFactory = vf;
                        scopeData.configurator = bvalConfigurator;
                        createSuccessful = true;

                    } finally {
                        releaseLoader(tuple);
                        scopeData.setClassLoader(origLoader);

                        // It's possible that the VF creation failed but we were able to initialize
                        // some state in the configurator. Release those resources if they exist.
                        if (!createSuccessful && bvalConfigurator != null) {
                            bvalConfigurator.release(null);
                        }
                    }
                }
            }
        }

        return vf;
    }

    @Trivial
    @Override
    public Validator getValidator(ComponentMetaData cmd) {
        return getValidator(cmd.getModuleMetaData(), priv.getContextClassLoader());
    }

    @Override
    public Validator getValidator(ModuleMetaData mmd, ClassLoader loader) {
        ValidatorFactory vfactory = getValidatorFactory(mmd, loader);
        Validator validator = vfactory.getValidator();

        return validator;
    }

    private ClassLoader createTCCL(ClassLoader parentCL) {
        return classLoadingServiceSR.getServiceWithException().createThreadContextClassLoader(parentCL);
    }

    @Override
    public void releaseLoader(ClassLoaderTuple tuple) {
        if (tuple != null && tuple.wasCreatedViaClassLoadingService) {
            classLoadingServiceSR.getServiceWithException().destroyThreadContextClassLoader(tuple.classLoader);
        }
    }

    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {
        if (isBeanValidationVersion20()) {
            moduleMetaDataCreatedHVProvider(event);
        } else {
            moduleMetaDataCreatedApacheProvider(event);
        }
    }

    private void moduleMetaDataCreatedHVProvider(MetaDataEvent<ModuleMetaData> event) {
        ModuleMetaData mmd = event.getMetaData();
        Container container = event.getContainer();

        MetaDataSlot mmdSlot = ivModuleMetaDataSlot;
        if (mmdSlot == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "MetaDataSlotService not active... skipping start module action",
                         mmd.getName());
            return;
        }

        BeanValidationMetaData beanValMetaData = (BeanValidationMetaData) mmd.getMetaData(ivModuleMetaDataSlot);

        if (beanValMetaData == null) {
            ModuleInfo moduleInfo = getModuleInfo(container);
            beanValMetaData = new BeanValidationMetaData(moduleInfo.getClassLoader(), moduleInfo.getURI());
            mmd.setMetaData(mmdSlot, beanValMetaData);
        }
    }

    private void moduleMetaDataCreatedApacheProvider(MetaDataEvent<ModuleMetaData> event) {
        ValidationConfig validationConfig = null;
        ModuleMetaData mmd = event.getMetaData();
        Container container = event.getContainer();

        try {
            validationConfig = container.adapt(ValidationConfig.class);

            if (validationConfig != null) {
                URL validationXmlUrl = container.getEntry(validationConfig.getDeploymentDescriptorPath()).getResource();
                //this.validationXmlUrl = validationXmlUrl;
                moduleValidationXMLs.put(mmd, validationXmlUrl);
                //Set<URL> urlSet = new HashSet<URL>();
                //urlSet.add(validationXmlUrl);
                //validationXmlEnum = java.util.Collections.enumeration(urlSet);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "validation.xml found at: " + validationXmlUrl.getPath());
                }
            }
        } catch (UnableToAdaptException e) {
            // This will occur if the validation.xml is invalid - log FFDC and continue module startup.
            // Ideally, we'd fail the app start here by throwing a ValidationException, but the CTS expects
            // the app to start and then fail at runtime.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find a valid validation.xml - will continue to start module: " + event.getMetaData().getJ2EEName());
            }
        }

        MetaDataSlot mmdSlot = ivModuleMetaDataSlot;
        if (mmdSlot == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "MetaDataSlotService not active... skipping start module action",
                         mmd.getName());
            return;
        }

        OSGiBeanValidationScopeData scopeData = (OSGiBeanValidationScopeData) mmd.getMetaData(ivModuleMetaDataSlot);
        if (scopeData == null) {

            scopeData = new OSGiBeanValidationScopeData(container);

            // store the module container in slot
            mmd.setMetaData(mmdSlot, scopeData);
        }

    }

    private void moduleMetaDataDestroyedHVProvider(MetaDataEvent<ModuleMetaData> event) {
        //Make sure vf.close() is called for bval 2.0 to prevent classloader leaks.
        ModuleMetaData mmd = event.getMetaData();
        MetaDataSlot mmdSlot = ivModuleMetaDataSlot;

        if (mmdSlot == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "MetaDataSlotService not active... skipping stop module action",
                         mmd.getName());
            return;
        }

        BeanValidationMetaData beanValMetaData = (BeanValidationMetaData) mmd.getMetaData(ivModuleMetaDataSlot);
        if (beanValMetaData != null) {
            ValidatorFactoryBuilder validatorFactoryBuilder = validatorFactoryBuilderSR.getServiceWithException();
            validatorFactoryBuilder.closeValidatorFactory(beanValMetaData.getValidatorFactory());
            beanValMetaData.close();
        }
    }

    private void moduleMetaDataDestroyedApacheProvider(MetaDataEvent<ModuleMetaData> event) {
        ModuleMetaData mmd = event.getMetaData();
        moduleValidationXMLs.remove(mmd);
        MetaDataSlot mmdSlot = ivModuleMetaDataSlot;

        if (mmdSlot == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "MetaDataSlotService not active... skipping stop module action",
                         mmd.getName());
            return;
        }

        OSGiBeanValidationScopeData scopeData = (OSGiBeanValidationScopeData) mmd.getMetaData(mmdSlot);
        if (scopeData != null) {
            synchronized (scopeData) {
                // release the configuration
                ValidationConfigurationInterface configurator = scopeData.configurator;
                if (configurator != null) {
                    configurator.release(scopeData.ivValidatorFactory);
                }

                scopeData.classloader = null;
                scopeData.configuratorReleased = true;
                if (isBeanValidationVersion11OrGreater()) {
                    scopeData.configurator = null;
                    scopeData.ivValidatorFactory = null;
                }
            }
        }

        cleanBvalCache();
    }

    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {

        if (isBeanValidationVersion20()) {
            moduleMetaDataDestroyedHVProvider(event);
        } else {
            moduleMetaDataDestroyedApacheProvider(event);
        }
    }

    @Activate
    protected void activate(ComponentContext cc) {
        setInstance(this);
        classLoadingServiceSR.activate(cc);
        validationConfigFactorySR.activate(cc);
        validatorFactoryBuilderSR.activate(cc);
        constrainedHelperSR.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        setInstance(null);
        classLoadingServiceSR.deactivate(cc);
        validationConfigFactorySR.deactivate(cc);
        validatorFactoryBuilderSR.deactivate(cc);
        constrainedHelperSR.deactivate(cc);
    }

    @Reference
    protected void setMetaDataSlotService(MetaDataSlotService slotService) {
        ivModuleMetaDataSlot = slotService.reserveMetaDataSlot(ModuleMetaData.class);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setMetaDataSlotService : mmd slot=" + ivModuleMetaDataSlot);
    }

    protected void unsetMetaDataSlotService(MetaDataSlotService slotService) {
        ivModuleMetaDataSlot = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unsetMetaDataSlotService");
    }

    @Reference(service = BeanValidationRuntimeVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRuntimeVersion(ServiceReference<BeanValidationRuntimeVersion> ref) {
        runtimeVersion = Version.parseVersion((String) ref.getProperty(BeanValidationRuntimeVersion.VERSION));
    }

    protected void unsetRuntimeVersion(ServiceReference<BeanValidationRuntimeVersion> ref) {
        runtimeVersion = DEFAULT_VERSION;
    }

    @Reference(name = REFERENCE_CLASSLOADING_SERVICE,
               service = ClassLoadingService.class)
    protected void setClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.setReference(ref);
    }

    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingServiceSR.unsetReference(ref);
    }

    @Reference(name = REFERENCE_CONSTRAINED_HELPER,
               service = ConstrainedHelper.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setConstrainedHelper(ServiceReference<ConstrainedHelper> ref) {
        constrainedHelperSR.setReference(ref);
    }

    protected void unsetConstrainedHelper(ServiceReference<ConstrainedHelper> ref) {
        constrainedHelperSR.unsetReference(ref);
    }

    @Reference(name = REFERENCE_VALIDATOR_FACTORY_BUILDER,
               service = ValidatorFactoryBuilder.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setValidatorFactoryBuilder(ServiceReference<ValidatorFactoryBuilder> ref) {
        validatorFactoryBuilderSR.setReference(ref);
    }

    protected void unsetValidatorFactoryBuilder(ServiceReference<ValidatorFactoryBuilder> ref) {
        validatorFactoryBuilderSR.unsetReference(ref);
    }

    @Reference(name = REFERENCE_VALIDATION_CONFIG_FACTORY,
               service = ValidationConfigurationFactory.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setValidationConfigFactory(ServiceReference<ValidationConfigurationFactory> factoryRef) {
        validationConfigFactorySR.setReference(factoryRef);
    }

    protected void unsetValidationConfigFactory(ServiceReference<ValidationConfigurationFactory> factoryRef) {
        validationConfigFactorySR.unsetReference(factoryRef);
    }

    private boolean isBeanValidationVersion11OrGreater() {
        return runtimeVersion.compareTo(BeanValidationRuntimeVersion.VERSION_1_1) >= 0;
    }

    private boolean isBeanValidationVersion10() {
        return runtimeVersion.compareTo(BeanValidationRuntimeVersion.VERSION_1_0) == 0;
    }

    private boolean isBeanValidationVersion11() {
        return runtimeVersion.compareTo(BeanValidationRuntimeVersion.VERSION_1_1) == 0;
    }

    private boolean isBeanValidationVersion20() {
        return runtimeVersion.compareTo(BeanValidationRuntimeVersion.VERSION_2_0) == 0;
    }

    @Override
    public ClassLoaderTuple configureBvalClassloader(ClassLoader cl) {
        if (cl == null) {
            cl = priv.getContextClassLoader();
        }
        if (cl != null) {
            ClassLoadingService classLoadingService = classLoadingServiceSR.getServiceWithException();
            if (classLoadingService.isThreadContextClassLoader(cl)) {
                return ClassLoaderTuple.of(cl, false);
            } else if (classLoadingService.isAppClassLoader(cl)) {
                return ClassLoaderTuple.of(createTCCL(cl), true);
            }
        }
        return ClassLoaderTuple.of(createTCCL(AbstractBeanValidation.class.getClassLoader()), true);
    }

    /**
     * Prevent the Apache bval code from leaking memory/application classloaders in its cached data due to
     * configuration updates.
     */
    private void cleanBvalCache() {
        if (isBeanValidationVersion11() && !FrameworkState.isStopping()) {
            ClassLoader classLoader = null;
            SetContextClassLoaderPrivileged setClassLoader = null;
            ClassLoader oldClassLoader = null;
            boolean wasTcclCreated = false;
            ClassLoaderTuple tuple = null;
            try {
                // Get a classloader that has the bean validation 1.1 API.
                classLoader = validationConfigFactorySR.getServiceWithException().getClass().getClassLoader();

                if (classLoader != null && !classLoadingServiceSR.getServiceWithException().isThreadContextClassLoader(classLoader)) {
                    tuple = ClassLoaderTuple.of(createTCCL(classLoader), true);
                    classLoader = tuple.classLoader;
                    wasTcclCreated = true;
                }

                ThreadContextAccessor tca = System.getSecurityManager() == null ? ThreadContextAccessor.getThreadContextAccessor() : AccessController.doPrivileged(getThreadContextAccessorAction);

                // set the thread context class loader to be used, must be reset in finally block
                setClassLoader = new SetContextClassLoaderPrivileged(tca);
                oldClassLoader = setClassLoader.execute(classLoader);

                if (classLoader == null) {
                    classLoader = oldClassLoader;
                }

                Class<?> clazz = classLoader.loadClass("org.apache.bval.jsr.ConstraintAnnotationAttributes");
                Field methodByNameAndClass = clazz.getDeclaredField("METHOD_BY_NAME_AND_CLASS");

                priv.setAccessible(methodByNameAndClass, true);

                Map<?, ?> methodMap = (Map<?, ?>) methodByNameAndClass.get(null);
                methodMap.clear();

                clazz = classLoader.loadClass("org.apache.bval.util.PropertyAccess");
                methodByNameAndClass = clazz.getDeclaredField("PROPERTY_DESCRIPTORS");

                priv.setAccessible(methodByNameAndClass, true);

                methodMap = (Map<?, ?>) methodByNameAndClass.get(null);
                methodMap.clear();
            } catch (Exception e) {
                //ffdc
            } finally {
                if (setClassLoader != null) {
                    setClassLoader.execute(oldClassLoader);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                    }
                }
                releaseLoader(tuple);
            }
        }
    }

    private ExtendedModuleInfo getModuleInfo(Container container) throws ValidationException {
        ExtendedModuleInfo moduleInfo = null;

        try {
            NonPersistentCache cache = container.adapt(NonPersistentCache.class);
            moduleInfo = (ExtendedModuleInfo) cache.getFromCache(ModuleInfo.class);
        } catch (UnableToAdaptException e) {
            throw new ValidationException(e);
        }
        return moduleInfo;
    }

    @Override
    public boolean isMethodConstrained(Method method) throws ValidationException {
        // Bean Validation 1.0 doesn't support method constraints.
        if (isBeanValidationVersion10()) {
            return false;
        }

        ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        AbstractMetaData beanValMetaData = (AbstractMetaData) componentMetaData.getModuleMetaData().getMetaData(ivModuleMetaDataSlot);

        if (beanValMetaData == null) {
            throw new ValidationException("Validation not enabled for module " + componentMetaData.getModuleMetaData().getName());
        }

        // Check for cached results in the module metadata slot.
        Boolean isMethodConstrained = beanValMetaData.isExecutableConstrained(method.toString());
        if (isMethodConstrained != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isExecutableConstrained cache hit on method " + method.toString() + " :  " + isMethodConstrained);
            }
            return isMethodConstrained;
        }

        Validator validator = getValidator(componentMetaData);
        ConstrainedHelper constrainedHelper = constrainedHelperSR.getServiceWithException();
        Class<?> declaringClass = method.getDeclaringClass();
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(declaringClass);
        isMethodConstrained = constrainedHelper.isMethodConstrained(method, beanDescriptor, declaringClass.getClassLoader(), beanValMetaData.getModuleUri());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isMethodConstrained calculated method " + method.toString() + " :  " + isMethodConstrained);
        }
        beanValMetaData.addExecutableToConstrainedCache(method.toString(), isMethodConstrained);
        return isMethodConstrained;
    }

    @Override
    public boolean isConstructorConstrained(Constructor<?> constructor) {
        // Bean Validation 1.0 doesn't support constructor constraints.
        if (isBeanValidationVersion10()) {
            return false;
        }

        ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        AbstractMetaData beanValMetaData = (AbstractMetaData) componentMetaData.getModuleMetaData().getMetaData(ivModuleMetaDataSlot);

        if (beanValMetaData == null) {
            throw new ValidationException("Validation not enabled for module " + componentMetaData.getModuleMetaData().getName());
        }

        // Check for cached results in the module metadata slot.
        Boolean isConstructorConstrained = beanValMetaData.isExecutableConstrained(constructor.toString());
        if (isConstructorConstrained != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isExecutableConstrained cache hit on constructor " + constructor.toString() + " :  " + isConstructorConstrained);
            }
            return isConstructorConstrained;
        }

        Validator validator = getValidator(componentMetaData);
        ConstrainedHelper constrainedHelper = constrainedHelperSR.getServiceWithException();
        Class<?> declaringClass = constructor.getDeclaringClass();
        BeanDescriptor beanDescriptor = validator.getConstraintsForClass(declaringClass);
        isConstructorConstrained = constrainedHelper.isConstructorConstrained(constructor, beanDescriptor, declaringClass.getClassLoader(), beanValMetaData.getModuleUri());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isConstructorConstrained calculated method " + constructor.toString() + " :  " + isConstructorConstrained);
        }
        beanValMetaData.addExecutableToConstrainedCache(constructor.toString(), isConstructorConstrained);
        return isConstructorConstrained;
    }
}

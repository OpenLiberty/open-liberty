/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;

import javax.naming.spi.ObjectFactory;

import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.factory.MBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.OverrideReferenceFactory;

/**
 * The interface for the InjectionEngine.
 *
 * This class defines the methods for registering, processing and injecting
 * meta data found.
 */
public interface InjectionEngine
{
    /**
     * Registers an implementation of an InjectionProcessor to collection injection meta data for both
     * single and plural variations of an annotation.
     *
     * @param processor The processor extending InjectionProcessor to be registered with the injection engine.
     * @param annotation The Annotation class the InjectionProcessor will process. Though the
     *            processor may process both the single and plural variations of the processor, only the single type
     *            is needed.
     * @return Class Returns class if one was already registered with the specified annotation, otherwise null
     *         will be returned.
     * @throws InjectionException
     */
    public <A extends Annotation, AS extends Annotation> void registerInjectionProcessor
                    (Class<? extends InjectionProcessor<A, AS>> processor,
                     Class<A> annotation)
                                    throws InjectionException;

    /**
     * Registers an implementation of an InjectionProcessorProvider.
     *
     * @param provider the provider
     * @throws InjectionException
     */
    // F50163
    public void registerInjectionProcessorProvider(InjectionProcessorProvider<?, ?> provider)
                    throws InjectionException;

    /**
     * Unregisters an InjectionProcessorProvider.
     *
     * @param provider the provider
     * @throws InjectionException if the provider was not registered
     */
    // F50163
    public void unregisterInjectionProcessorProvider(InjectionProcessorProvider<?, ?> provider)
                    throws InjectionException;

    /**
     * Collect and bind injection meta data for all the processors registered with the injection engine. The
     * parameter injectionTargetMap will be returned with the injection targets that will be injected.
     *
     * @param injectionTargetMap The map of cookies which is populated by
     * @param compNSConfig The specific container implementation of ComponentNameSpaceConfiguration.
     * @throws InjectionException
     */
    public void processInjectionMetaData(HashMap<Class<?>, InjectionTarget[]> injectionTargetMap,
                                         ComponentNameSpaceConfiguration compNSConfig)
                    throws InjectionException;

    /**
     * Injects the target into the object.
     *
     * @param objectToInject
     * @param injectionTarget
     * @throws InjectionException
     */
    public void inject(Object objectToInject,
                       InjectionTarget injectionTarget)
                    throws InjectionException;

    /**
     * Injects the object identified by the injection binding associated with
     * the specified target into the target instance identified by the
     * specified target context. <p>
     *
     * @param objectToInject the object that is the target of the injection.
     * @param target injection target metadata, including information identifying
     *            the specific field or method as well as how to
     *            obtain the object to inject.
     * @param targetContext provides access to context data associated with
     *            the target of the injection (e.g. EJBContext).
     *            May be null if no context to be provided by the
     *            container.
     * @throws InjectionException if an error occurs locating the object to
     *             inject or actually injecting it into the target
     *             instance.
     */
    // F49213.1
    public void inject(Object objectToInject,
                       InjectionTarget target,
                       InjectionTargetContext targetContext)
                    throws InjectionException;

    /**
     * Performs injection in the client process for federated client modules.
     *
     * @param compNSConfig the minimal namespace configuration that contains at
     *            least the following data:
     *            <ul>
     *            <li>getInjectionClasses - a list containing the main class
     *            <li>getJavaColonContext - a Context pointing to a javaNameSpace using
     *            ComponentNameSpaceLocation.SERVER.
     *            <li>getClassLoader
     *            <li>isClientContainer
     */
    public void injectClient(ComponentNameSpaceConfiguration compNSConfig)
                    throws InjectionException; // F732-32443

    //492391.2
    /**
     * This method will register an instance of an InjectionMetaDataListener with the current
     * engine instance.
     */
    public void registerInjectionMetaDataListener(InjectionMetaDataListener metaDataListener);

    // RTC96791
    /**
     * This method will unregister an instance of an InjectionMetaDataListener with the current
     * engine instance.
     */
    public void unregisterInjectionMetaDataListener(InjectionMetaDataListener metaDataListener);

    /**
     * Provides a mechanism to register an ObjectFactory to handle additional
     * data types for a processor that has already been provided. <p>
     *
     * This method is equivalent to calling {@link #registerObjectFactory(Class, Class, Class, boolean, Set, boolean)} with
     * a null allowedAttributes argument and a true refAddrNeeded argument.
     *
     * @param annotation the type of annotation processor the factory is to
     *            be registered with.
     * @param type the data type for which the factory is to be used.
     * @param objectFactory the actual object factory class.
     * @param allowOverride true to allow an override binding to global name space.
     *
     * @throws InjectionException if an injection processor has not been
     *             registered for the specified annotation.
     * @throws IllegalArgumentException if any of the parameters are null.
     **/
    // F623-841
    public void registerObjectFactory(Class<? extends Annotation> annotation,
                                      Class<?> type,
                                      Class<? extends ObjectFactory> objectFactory,
                                      boolean allowOverride) // F623-841.1
    throws InjectionException;

    /**
     * Provides a mechanism to register an ObjectFactory to handle additional
     * data types for a processor that has already been provided. <p>
     *
     * The most common usage is for the @Resource annotation processor.
     * By registering additional ObjectFactories, the @Resource annotation may
     * be used to inject more data types than are specified in the core
     * Java EE specification. <p>
     *
     * For those processors that support adding additional ObjectFactories,
     * the registered object factory will be invoked to create an instance
     * of the object when the required object is of the data type specified. <p>
     *
     * Note, if a binding is found in XML for this specified data type, then
     * the binding will be considered an override, and will be used instead
     * to lookup the object in naming. <p>
     *
     * The name, type, and description attributes of the Resource annotation
     * are always allowed. If the allowOverride boolean is specified in, then
     * the lookup and mappedName attributes are allowed. All other attributes
     * are disallowed unless they are specified in allowedAttributes. <p>
     *
     * @param annotation the type of annotation processor the factory is to
     *            be registered with.
     * @param type the data type for which the factory is to be used.
     * @param objectFactory the actual object factory class.
     * @param allowOverride true to allow an override binding to global name space.
     * @param allowedAttributes the attributes of the Resource annotation that are
     *            allowed to have non-default values, or null to allow
     *            all attributes
     * @param refAddrNeeded true if a processor-specific RefAddr is needed.
     *
     * @throws InjectionException if an injection processor has not been
     *             registered for the specified annotation.
     * @throws IllegalArgumentException if any of the parameters other than
     *             allowedAttributes are null.
     *
     * @see com.ibm.wsspi.injectionengine.factory.ResourceInfoRefAddr
     **/
    public void registerObjectFactory(Class<? extends Annotation> annotation,
                                      Class<?> type,
                                      Class<? extends ObjectFactory> objectFactory,
                                      boolean allowOverride,
                                      Set<String> allowedAttributes, // d675976
                                      boolean refAddrNeeded) // F48603
    throws InjectionException;

    /**
     * Provides a mechanism to register an ObjectFactory to handle additional
     * data types for a processor that has already been provided. <p>
     *
     * The most common usage is for the @Resource annotation processor.
     * By registering additional ObjectFactories, the @Resource annotation may
     * be used to inject more data types than are specified in the core
     * Java EE specification. <p>
     *
     * For those processors that support adding additional ObjectFactories,
     * the registered object factory will be invoked to create an instance
     * of the object when the required object is of the data type specified. <p>
     *
     * Note, if a binding is found in XML for this specified data type, then
     * the binding will be considered an override, and will be used instead
     * to lookup the object in naming. <p>
     *
     * The name, type, and description attributes of the Resource annotation
     * are always allowed. If the allowOverride boolean is specified in, then
     * the lookup and mappedName attributes are allowed. All other attributes
     * are disallowed unless they are specified in allowedAttributes. <p>
     *
     * @param info the ObjectFactory info
     * @throws InjectionException
     *
     * @throws InjectionException if an injection processor has not been
     *             registered for the annotation.
     *
     * @see com.ibm.wsspi.injectionengine.factory.ResourceInfoRefAddr
     */
    // F50163
    public void registerObjectFactoryInfo(ObjectFactoryInfo info)
                    throws InjectionException;

    /**
     * Unregisters an ObjectFactoryInfo.
     *
     * @param provider the provider
     * @throws InjectionException if the info was not registered
     */
    // F50163
    public void unregisterObjectFactoryInfo(ObjectFactoryInfo info)
                    throws InjectionException;

    /**
     * Provides a mechanism to override the algorithm used to identify
     * the target of a reference defined for the application component's
     * environment (java:comp/env). <p>
     *
     * For those processors that support override reference factories,
     * the registered reference factory will be invoked to create a naming
     * Reference that will be bound into the java:comp/env name space and
     * used to obtain an instance of the object that represents the target
     * of the specified component reference. <p>
     *
     * If the registered override reference factory does not wish to override
     * the component reference, it should return null; and normal processing
     * will be performed as if the override reference factory did not exist. <p>
     *
     * If multiple override reference factories are registered for the same
     * annotations, then they will be called in the order that they were
     * registered, until one of them returns a non-null result. <p>
     *
     * The implementation of OverrideRefereceFactory must be thread safe. <p>
     *
     * @param annotation the type of annotation processor the factory is to
     *            be registered with.
     * @param factory thread safe instance of an override reference factory.
     *
     * @throws InjectionException if an injection processor has not been
     *             registered for the specified annotation.
     * @throws IllegalArgumentException if any of the parameters are null.
     **/
    // F1339-9050
    public <A extends Annotation> void registerOverrideReferenceFactory(Class<A> annotation,
                                                                        OverrideReferenceFactory<A> factory)
                    throws InjectionException;

    /**
     * Provides a mechanism to override the default managed bean reference
     * factory for controlling managed bean auto-link behavior. <p>
     *
     * Returns the current default managed bean reference factory. <p>
     *
     * @param mbLinkRefFactory new managed bean reference factory class.
     *
     * @return the current default managed bean reference factory.
     **/
    // d698540.1
    public MBLinkReferenceFactory registerManagedBeanReferenceFactory(MBLinkReferenceFactory mbLinkRefFactory);

    /**
     * Registers a resource factory builder for a resource definition type.
     *
     * @param type the resource definition type
     * @param builder the resource factory builder for the resource definition
     *
     * @throws IllegalStateException if a resource factory builder has already been registered for the type.
     */
    public void registerResourceFactoryBuilder(String type, ResourceFactoryBuilder builder);

    /**
     * Unregisters a resource factory builder for a resource definition type and
     * returns the currently registered resource factory builder. Null is returned
     * if a factory has not been registered for the type.
     *
     * @param type the resource definition type
     */
    public ResourceFactoryBuilder unregisterResourceFactoryBuilder(String type);

    // F743-17630
    /**
     * Creates a new <code>ReferenceContext</code> instance.
     *
     * A new <code>ReferenceContet</code> is always created. The method does not
     * check the <code>ModuleMetaData</code> for the existence of a shared
     * <code>ReferenceContext</code>, nor does it add the <code>ReferenceContext</code>
     * to the <code>ModuleMetaData</code> after its been created.
     */
    public ReferenceContext createReferenceContext();

    /**
     * Creates a new <code>ReferenceContext</code> instance that may be eligible
     * for deferred non-java:comp processing as soon as the containing module or
     * application is started, if supported by the runtime environment.
     *
     * @see #getCommonReferenceContext
     */
    public ReferenceContext createReferenceContext(MetaData cmd);

    // F743-17630 F743-17630CodRv
    /**
     * Gets a <code>ReferenceContext</code> to be shared by all components in a
     * module. If a <code>ReferenceContext</code> already exists, then it is
     * returned. Otherwise, one is created and associated with the module.
     *
     * @param mmd the primary metadata for a module (e.g., as returned by {@link com.ibm.ws.runtime.deploy.DeployedModule#getMetaData})
     */
    public ReferenceContext getCommonReferenceContext(ModuleMetaData mmd); // F48603.5
}

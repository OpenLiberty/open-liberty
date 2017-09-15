/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.faces;

import javax.enterprise.context.spi.CreationalContext;
import javax.faces.application.ApplicationFactory;
import javax.faces.component.visit.VisitContextFactory;
import javax.faces.context.ExceptionHandlerFactory;
import javax.faces.context.ExternalContextFactory;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.context.FlashFactory;
import javax.faces.context.PartialViewContextFactory;
import javax.faces.flow.FlowHandlerFactory;
import javax.faces.lifecycle.ClientWindowFactory;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.render.RenderKitFactory;
import javax.faces.view.ViewDeclarationLanguageFactory;
import javax.faces.view.facelets.FaceletCacheFactory;
import javax.faces.view.facelets.TagHandlerDelegateFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * see Javadoc of <a href="http://java.sun.com/javaee/javaserverfaces/1.2/docs/api/index.html">JSF Specification</a>
 */
public final class FactoryFinder
{
    public static final String APPLICATION_FACTORY = "javax.faces.application.ApplicationFactory";
    public static final String EXCEPTION_HANDLER_FACTORY = "javax.faces.context.ExceptionHandlerFactory";
    public static final String EXTERNAL_CONTEXT_FACTORY = "javax.faces.context.ExternalContextFactory";
    public static final String FACES_CONTEXT_FACTORY = "javax.faces.context.FacesContextFactory";
    public static final String LIFECYCLE_FACTORY = "javax.faces.lifecycle.LifecycleFactory";
    public static final String PARTIAL_VIEW_CONTEXT_FACTORY = "javax.faces.context.PartialViewContextFactory";
    public static final String RENDER_KIT_FACTORY = "javax.faces.render.RenderKitFactory";
    public static final String TAG_HANDLER_DELEGATE_FACTORY = "javax.faces.view.facelets.TagHandlerDelegateFactory";
    public static final String VIEW_DECLARATION_LANGUAGE_FACTORY = "javax.faces.view.ViewDeclarationLanguageFactory";
    public static final String VISIT_CONTEXT_FACTORY = "javax.faces.component.visit.VisitContextFactory";
    public static final String FACELET_CACHE_FACTORY = "javax.faces.view.facelets.FaceletCacheFactory";
    public static final String FLASH_FACTORY = "javax.faces.context.FlashFactory";
    public static final String FLOW_HANDLER_FACTORY = "javax.faces.flow.FlowHandlerFactory";
    public static final String CLIENT_WINDOW_FACTORY = "javax.faces.lifecycle.ClientWindowFactory";

    /**
     * used as a monitor for itself and _factories. Maps in this map are used as monitors for themselves and the
     * corresponding maps in _factories.
     */
    private static Map<ClassLoader, Map<String, List<String>>> registeredFactoryNames
            = new HashMap<ClassLoader, Map<String, List<String>>>();

    /**
     * Maps from classLoader to another map, the container (i.e. Tomcat) will create a class loader for each web app
     * that it controls (typically anyway) and that class loader is used as the key.
     * 
     * The secondary map maps the factory name (i.e. FactoryFinder.APPLICATION_FACTORY) to actual instances that are
     * created via getFactory. The instances will be of the class specified in the setFactory method for the factory
     * name, i.e. FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY, MyFactory.class).
     */
    private static Map<ClassLoader, Map<String, Object>> factories
            = new HashMap<ClassLoader, Map<String, Object>>();

    private static final Set<String> VALID_FACTORY_NAMES = new HashSet<String>();
    private static final Map<String, Class<?>> ABSTRACT_FACTORY_CLASSES = new HashMap<String, Class<?>>();
    private static final ClassLoader MYFACES_CLASSLOADER;
    
    private static final String INJECTION_PROVIDER_INSTANCE = "oam.spi.INJECTION_PROVIDER_KEY";
    private static final String INJECTED_BEAN_STORAGE_KEY = "org.apache.myfaces.spi.BEAN_ENTRY_STORAGE";
    private static final String BEAN_ENTRY_CLASS_NAME = "org.apache.myfaces.cdi.dependent.BeanEntry";

    private static final Logger LOGGER = Logger.getLogger(FactoryFinder.class.getName());

    static
    {
        VALID_FACTORY_NAMES.add(APPLICATION_FACTORY);
        VALID_FACTORY_NAMES.add(EXCEPTION_HANDLER_FACTORY);
        VALID_FACTORY_NAMES.add(EXTERNAL_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(FACES_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(LIFECYCLE_FACTORY);
        VALID_FACTORY_NAMES.add(PARTIAL_VIEW_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(RENDER_KIT_FACTORY);
        VALID_FACTORY_NAMES.add(TAG_HANDLER_DELEGATE_FACTORY);
        VALID_FACTORY_NAMES.add(VIEW_DECLARATION_LANGUAGE_FACTORY);
        VALID_FACTORY_NAMES.add(VISIT_CONTEXT_FACTORY);
        VALID_FACTORY_NAMES.add(FACELET_CACHE_FACTORY);
        VALID_FACTORY_NAMES.add(FLASH_FACTORY);
        VALID_FACTORY_NAMES.add(FLOW_HANDLER_FACTORY);
        VALID_FACTORY_NAMES.add(CLIENT_WINDOW_FACTORY);
        
        ABSTRACT_FACTORY_CLASSES.put(APPLICATION_FACTORY, ApplicationFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(EXCEPTION_HANDLER_FACTORY, ExceptionHandlerFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(EXTERNAL_CONTEXT_FACTORY, ExternalContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FACES_CONTEXT_FACTORY, FacesContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(LIFECYCLE_FACTORY, LifecycleFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(PARTIAL_VIEW_CONTEXT_FACTORY, PartialViewContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(RENDER_KIT_FACTORY, RenderKitFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(TAG_HANDLER_DELEGATE_FACTORY, TagHandlerDelegateFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(VIEW_DECLARATION_LANGUAGE_FACTORY, ViewDeclarationLanguageFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(VISIT_CONTEXT_FACTORY, VisitContextFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FACELET_CACHE_FACTORY, FaceletCacheFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FLASH_FACTORY, FlashFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(FLOW_HANDLER_FACTORY, FlowHandlerFactory.class);
        ABSTRACT_FACTORY_CLASSES.put(CLIENT_WINDOW_FACTORY, ClientWindowFactory.class);
        try
        {
            ClassLoader classLoader;
            if (System.getSecurityManager() != null)
            {
                classLoader = (ClassLoader) AccessController.doPrivileged(new java.security.PrivilegedExceptionAction()
                {
                    public Object run()
                    {
                        return FactoryFinder.class.getClassLoader();
                    }
                });
            }
            else
            {
                classLoader = FactoryFinder.class.getClassLoader();
            }

            if (classLoader == null)
            {
                throw new FacesException("jsf api class loader cannot be identified", null);
            }
            MYFACES_CLASSLOADER = classLoader;
        }
        catch (Exception e)
        {
            throw new FacesException("jsf api class loader cannot be identified", e);
        }
    }

    // ~ Start FactoryFinderProvider Support
    
    private static Object factoryFinderProviderFactoryInstance;
    
    private static volatile boolean initialized = false;
    
    private static void initializeFactoryFinderProviderFactory()
    {
        if (!initialized)
        {
            factoryFinderProviderFactoryInstance = _FactoryFinderProviderFactory.getInstance();
            initialized = true;
        }
    }

    // ~ End FactoryFinderProvider Support

    // avoid instantiation
    FactoryFinder()
    {
    }

    /**
     * <p>
     * Create (if necessary) and return a per-web-application instance of the appropriate implementation class for the
     * specified JavaServer Faces factory class, based on the discovery algorithm described in the class description.
     * </p>
     * 
     * <p>
     * The standard factories and wrappers in JSF all implement the interface {@link FacesWrapper}. If the returned
     * <code>Object</code> is an implementation of one of the standard factories, it must be legal to cast it to an
     * instance of <code>FacesWrapper</code> and call {@link FacesWrapper#getWrapped()} on the instance.
     * </p>
     * 
     * @param factoryName
     *            Fully qualified name of the JavaServer Faces factory for which an implementation instance is requested
     * 
     * @return A per-web-application instance of the appropriate implementation class for the specified JavaServer Faces
     *         factory class
     * 
     * @throws FacesException
     *             if the web application class loader cannot be identified
     * @throws FacesException
     *             if an instance of the configured factory implementation class cannot be loaded
     * @throws FacesException
     *             if an instance of the configured factory implementation class cannot be instantiated
     * @throws IllegalArgumentException
     *             if <code>factoryname</code> does not identify a standard JavaServer Faces factory name
     * @throws IllegalStateException
     *             if there is no configured factory implementation class for the specified factory name
     * @throws NullPointerException
     *             if <code>factoryname</code> is null
     */
    public static Object getFactory(String factoryName) throws FacesException
    {
        if (factoryName == null)
        {
            throw new NullPointerException("factoryName may not be null");
        }
        
        initializeFactoryFinderProviderFactory();
        
        if (factoryFinderProviderFactoryInstance == null)
        {
            // Do the typical stuff
            return _getFactory(factoryName);
        }
        else
        {
            try
            {
                //Obtain the FactoryFinderProvider instance for this context.
                Object ffp = _FactoryFinderProviderFactory
                        .FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD
                        .invoke(factoryFinderProviderFactoryInstance, null);
                
                //Call getFactory method and pass the params
                return _FactoryFinderProviderFactory
                        .FACTORY_FINDER_PROVIDER_GET_FACTORY_METHOD.invoke(ffp, factoryName);
            }
            catch (InvocationTargetException e)
            {
                Throwable targetException = e.getCause();
                if (targetException instanceof NullPointerException)
                {
                    throw (NullPointerException) targetException;
                }
                else if (targetException instanceof FacesException)
                {
                    throw (FacesException) targetException;
                }
                else if (targetException instanceof IllegalArgumentException)
                {
                    throw (IllegalArgumentException) targetException;
                }
                else if (targetException instanceof IllegalStateException)
                {
                    throw (IllegalStateException) targetException;
                }
                else if (targetException == null)
                {
                    throw new FacesException(e);
                }
                else
                {
                    throw new FacesException(targetException);
                }
            }
            catch (Exception e)
            {
                //No Op
                throw new FacesException(e);
            }
        }
    }

    private static Object _getFactory(String factoryName) throws FacesException
    {
        ClassLoader classLoader = getClassLoader();

        // This code must be synchronized because this could cause a problem when
        // using update feature each time of myfaces (org.apache.myfaces.CONFIG_REFRESH_PERIOD)
        // In this moment, a concurrency problem could happen
        Map<String, List<String>> factoryClassNames = null;
        Map<String, Object> factoryMap = null;

        synchronized (registeredFactoryNames)
        {
            factoryClassNames = registeredFactoryNames.get(classLoader);

            if (factoryClassNames == null)
            {
                String message
                        = "No Factories configured for this Application. This happens if the faces-initialization "
                        + "does not work at all - make sure that you properly include all configuration "
                        + "settings necessary for a basic faces application "
                        + "and that all the necessary libs are included. Also check the logging output of your "
                        + "web application and your container for any exceptions!"
                        + "\nIf you did that and find nothing, the mistake might be due to the fact "
                        + "that you use some special web-containers which "
                        + "do not support registering context-listeners via TLD files and "
                        + "a context listener is not setup in your web.xml.\n"
                        + "A typical config looks like this;\n<listener>\n"
                        + "  <listener-class>org.apache.myfaces.webapp.StartupServletContextListener</listener-class>\n"
                        + "</listener>\n";
                throw new IllegalStateException(message);
            }

            if (!factoryClassNames.containsKey(factoryName))
            {
                throw new IllegalArgumentException("no factory " + factoryName + " configured for this application.");
            }

            factoryMap = factories.get(classLoader);

            if (factoryMap == null)
            {
                factoryMap = new HashMap<String, Object>();
                factories.put(classLoader, factoryMap);
            }
        }

        List beanEntryStorage;

        synchronized (factoryClassNames)
        {
            beanEntryStorage = (List)factoryMap.get(INJECTED_BEAN_STORAGE_KEY);

            if (beanEntryStorage == null)
            {
                beanEntryStorage = new CopyOnWriteArrayList();
                factoryMap.put(INJECTED_BEAN_STORAGE_KEY, beanEntryStorage);
            }
        }

        List<String> classNames;
        Object factory;
        Object injectionProvider;
        synchronized (factoryClassNames)
        {
            factory = factoryMap.get(factoryName);
            if (factory != null)
            {
                return factory;
            }

            classNames = factoryClassNames.get(factoryName);
            
            injectionProvider = factoryMap.get(INJECTION_PROVIDER_INSTANCE);
        }

        if (injectionProvider == null)
        {
            injectionProvider = getInjectionProvider();
            synchronized (factoryClassNames)
            {
                factoryMap.put(INJECTION_PROVIDER_INSTANCE, injectionProvider);
            }
        }

        // release lock while calling out
        factory = newFactoryInstance(ABSTRACT_FACTORY_CLASSES.get(factoryName), 
            classNames.iterator(), classLoader, injectionProvider, beanEntryStorage);

        synchronized (factoryClassNames)
        {
            // check if someone else already installed the factory
            if (factoryMap.get(factoryName) == null)
            {
                factoryMap.put(factoryName, factory);
            }
        }

        return factory;
    }
    
    private static Object getInjectionProvider()
    {
        try
        {
            // Remember the first call in a webapp over FactoryFinder.getFactory(...) comes in the 
            // initialization block, so there is a startup FacesContext active and
            // also a valid startup ExternalContext. Note after that, we need to cache
            // the injection provider for the classloader, because in a normal
            // request there is no active FacesContext in the moment and this call will
            // surely fail.
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null)
            {
                Object injectionProviderFactory =
                    _FactoryFinderProviderFactory.INJECTION_PROVIDER_FACTORY_GET_INSTANCE_METHOD
                        .invoke(_FactoryFinderProviderFactory.INJECTION_PROVIDER_CLASS);
                Object injectionProvider = 
                    _FactoryFinderProviderFactory.INJECTION_PROVIDER_FACTORY_GET_INJECTION_PROVIDER_METHOD
                        .invoke(injectionProviderFactory, facesContext.getExternalContext());
                return injectionProvider;
            }
        }
        catch (Exception e)
        {
        }
        return null;
    }
    
    private static void injectAndPostConstruct(Object injectionProvider, Object instance, List injectedBeanStorage)
    {
        if (injectionProvider != null)
        {
            try
            {
                Object creationMetaData = _FactoryFinderProviderFactory.INJECTION_PROVIDER_INJECT_OBJECT_METHOD.invoke(
                    injectionProvider, instance);

                addBeanEntry(instance, creationMetaData, injectedBeanStorage);

                _FactoryFinderProviderFactory.INJECTION_PROVIDER_POST_CONSTRUCT_METHOD.invoke(
                    injectionProvider, instance, creationMetaData);
            }
            catch (Exception ex)
            {
                throw new FacesException(ex);
            }
        }
    }
    
    // injectANDPostConstruct based on a class added for CDI 1.2 support.
    private static Object injectAndPostConstruct(Object injectionProvider, Class Klass, List injectedBeanStorage)
    {
        Object instance = null;
        if (injectionProvider != null)
        {
            try
            {

                Object managedObject = _FactoryFinderProviderFactory.INJECTION_PROVIDER_INJECT_CLASS_METHOD.invoke(injectionProvider, Klass);

                instance = _FactoryFinderProviderFactory.MANAGED_OBJECT_GET_OBJECT_METHOD.invoke(managedObject, null);

                if (instance != null) {

                    Object creationMetaData = _FactoryFinderProviderFactory.MANAGED_OBJECT_GET_CONTEXT_DATA_METHOD.invoke
                                    (managedObject, CreationalContext.class);

                    addBeanEntry(instance, creationMetaData, injectedBeanStorage);

                    _FactoryFinderProviderFactory.INJECTION_PROVIDER_POST_CONSTRUCT_METHOD.invoke(
                                                                                                  injectionProvider, instance, creationMetaData);
                }
            } catch (Exception ex)
            {
                throw new FacesException(ex);
            }
        }
        return instance;
    }

    
    private static void preDestroy(Object injectionProvider, Object beanEntry)
    {
        if (injectionProvider != null)
        {
            try
            {
                _FactoryFinderProviderFactory.INJECTION_PROVIDER_PRE_DESTROY_METHOD.invoke(
                    injectionProvider, getInstance(beanEntry), getCreationMetaData(beanEntry));
            }
            catch (Exception ex)
            {
                throw new FacesException(ex);
            }
        }
    }

    private static Object getInstance(Object beanEntry)
    {
        try
        {
            Method getterMethod = getMethod(beanEntry, "getInstance");
            return getterMethod.invoke(beanEntry);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static Object getCreationMetaData(Object beanEntry)
    {
        try
        {
            Method getterMethod = getMethod(beanEntry, "getCreationMetaData");
            return getterMethod.invoke(beanEntry);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static Method getMethod(Object beanEntry, String methodName) throws NoSuchMethodException
    {
        return beanEntry.getClass().getDeclaredMethod(methodName);
    }

    private static void addBeanEntry(Object instance, Object creationMetaData, List injectedBeanStorage)
    {
        try
        {
            Class<?> beanEntryClass = _FactoryFinderProviderFactory.classForName(BEAN_ENTRY_CLASS_NAME);
            Constructor beanEntryConstructor = beanEntryClass.getDeclaredConstructor(Object.class, Object.class);

            Object result = beanEntryConstructor.newInstance(instance, creationMetaData);
            injectedBeanStorage.add(result);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Object newFactoryInstance(Class<?> interfaceClass, Iterator<String> classNamesIterator,
                                             ClassLoader classLoader, Object injectionProvider,
                                             List injectedBeanStorage)
    {
        try
        {
            Object current = null;
            
            while (classNamesIterator.hasNext())
            {
                String implClassName = classNamesIterator.next();
                Class<?> implClass = null;
                try
                {
                    implClass = classLoader.loadClass(implClassName);
                }
                catch (ClassNotFoundException e)
                {
                    implClass = MYFACES_CLASSLOADER.loadClass(implClassName);
                }

                // check, if class is of expected interface type
                if (!interfaceClass.isAssignableFrom(implClass))
                {
                    throw new IllegalArgumentException("Class " + implClassName + " is no " + interfaceClass.getName());
                }

                if (current == null)
                {
                    // nothing to decorate
                    // CDI 1.2: Use inject to create the new instance of the class.
                    current = injectAndPostConstruct(injectionProvider, implClass, injectedBeanStorage);
                    if (current == null) {
                        current = implClass.newInstance();
                        injectAndPostConstruct(injectionProvider, current, injectedBeanStorage);
                    }
                }
                else
                {
                    // let's check if class supports the decorator pattern
                    try
                    {
                        Constructor<?> delegationConstructor = implClass.getConstructor(new Class[] { interfaceClass });
                        // impl class supports decorator pattern,
                        try
                        {
                            // create new decorator wrapping current
                            // CDI 1.2 : Use new Instance because we are picking a specific constructor
                            // so no need to worry about constructor injection.
                            current = delegationConstructor.newInstance(new Object[] { current });
                            injectAndPostConstruct(injectionProvider, current, injectedBeanStorage);
                        }
                        catch (InstantiationException e)
                        {
                            throw new FacesException(e);
                        }
                        catch (IllegalAccessException e)
                        {
                            throw new FacesException(e);
                        }
                        catch (InvocationTargetException e)
                        {
                            throw new FacesException(e);
                        }
                    }
                    catch (NoSuchMethodException e)
                    {
                        // no decorator pattern support
                        // CDI 1.2: Use inject to create the new instance of the class.
                        current = injectAndPostConstruct(injectionProvider, implClass, injectedBeanStorage);
                        if (current == null) {
                            current = implClass.newInstance();
                            injectAndPostConstruct(injectionProvider, current, injectedBeanStorage);
                        }
                    }
                }
            }

            return current;
        }
        catch (ClassNotFoundException e)
        {
            throw new FacesException(e);
        }
        catch (InstantiationException e)
        {
            throw new FacesException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new FacesException(e);
        }
    }

    public static void setFactory(String factoryName, String implName)
    {
        if (factoryName == null)
        {
            throw new NullPointerException("factoryName may not be null");
        }
        
        initializeFactoryFinderProviderFactory();
        
        if (factoryFinderProviderFactoryInstance == null)
        {
            // Do the typical stuff
            _setFactory(factoryName, implName);
        }
        else
        {
            try
            {
                //Obtain the FactoryFinderProvider instance for this context.
                Object ffp = _FactoryFinderProviderFactory
                        .FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD
                        .invoke(factoryFinderProviderFactoryInstance, null);
                
                //Call getFactory method and pass the params
                _FactoryFinderProviderFactory
                        .FACTORY_FINDER_PROVIDER_SET_FACTORY_METHOD.invoke(ffp, factoryName, implName);
            }
            catch (InvocationTargetException e)
            {
                Throwable targetException = e.getCause();
                if (targetException instanceof NullPointerException)
                {
                    throw (NullPointerException) targetException;
                }
                else if (targetException instanceof FacesException)
                {
                    throw (FacesException) targetException;
                }
                else if (targetException instanceof IllegalArgumentException)
                {
                    throw (IllegalArgumentException) targetException;
                }
                else if (targetException == null)
                {
                    throw new FacesException(e);
                }
                else
                {
                    throw new FacesException(targetException);
                }
            }
            catch (Exception e)
            {
                //No Op
                throw new FacesException(e);
            }
            
        }
    }

    private static void _setFactory(String factoryName, String implName)
    {
        checkFactoryName(factoryName);

        ClassLoader classLoader = getClassLoader();
        Map<String, List<String>> factoryClassNames = null;
        synchronized (registeredFactoryNames)
        {
            Map<String, Object> factories = FactoryFinder.factories.get(classLoader);

            if (factories != null && factories.containsKey(factoryName))
            {
                // Javadoc says ... This method has no effect if getFactory() has already been
                // called looking for a factory for this factoryName.
                return;
            }

            factoryClassNames = registeredFactoryNames.get(classLoader);

            if (factoryClassNames == null)
            {
                factoryClassNames = new HashMap<String, List<String>>();
                registeredFactoryNames.put(classLoader, factoryClassNames);
            }
        }

        synchronized (factoryClassNames)
        {
            List<String> classNameList = factoryClassNames.get(factoryName);

            if (classNameList == null)
            {
                classNameList = new ArrayList<String>();
                factoryClassNames.put(factoryName, classNameList);
            }

            classNameList.add(implName);
        }
    }

    public static void releaseFactories() throws FacesException
    {
        initializeFactoryFinderProviderFactory();
        
        if (factoryFinderProviderFactoryInstance == null)
        {
            // Do the typical stuff
            _releaseFactories();
        }
        else
        {
            try
            {
                //Obtain the FactoryFinderProvider instance for this context.
                Object ffp = _FactoryFinderProviderFactory
                        .FACTORY_FINDER_PROVIDER_FACTORY_GET_FACTORY_FINDER_METHOD
                        .invoke(factoryFinderProviderFactoryInstance, null);
                
                //Call getFactory method and pass the params
                _FactoryFinderProviderFactory.FACTORY_FINDER_PROVIDER_RELEASE_FACTORIES_METHOD.invoke(ffp, null);
            }
            catch (InvocationTargetException e)
            {
                Throwable targetException = e.getCause();
                if (targetException instanceof FacesException)
                {
                    throw (FacesException) targetException;
                }
                else if (targetException == null)
                {
                    throw new FacesException(e);
                }
                else
                {
                    throw new FacesException(targetException);
                }
            }
            catch (Exception e)
            {
                //No Op
                throw new FacesException(e);
            }
            
        }
    }

    private static void _releaseFactories() throws FacesException
    {
        ClassLoader classLoader = getClassLoader();

        Map<String, Object> factoryMap;
        // This code must be synchronized
        synchronized (registeredFactoryNames)
        {
            factoryMap = factories.remove(classLoader);

            // _registeredFactoryNames has as value type Map<String,List> and this must
            // be cleaned before release (for gc).
            Map<String, List<String>> factoryClassNames = registeredFactoryNames.get(classLoader);
            if (factoryClassNames != null)
            {
                factoryClassNames.clear();
            }

            registeredFactoryNames.remove(classLoader);
        }

        if (factoryMap != null)
        {
            Object injectionProvider = factoryMap.remove(INJECTION_PROVIDER_INSTANCE);
            if (injectionProvider != null)
            {
                List injectedBeanStorage = (List)factoryMap.get(INJECTED_BEAN_STORAGE_KEY);

                FacesException firstException = null;
                for (Object entry : injectedBeanStorage)
                {
                    try
                    {
                        preDestroy(injectionProvider, entry);
                    }
                    catch (FacesException e)
                    {
                        LOGGER.log(Level.SEVERE, "#preDestroy failed", e);

                        if (firstException == null)
                        {
                            firstException = e; //all preDestroy callbacks need to get invoked
                        }
                    }
                }
                injectedBeanStorage.clear();

                if (firstException != null)
                {
                    throw firstException;
                }
            }
        }
    }

    private static void checkFactoryName(String factoryName)
    {
        if (!VALID_FACTORY_NAMES.contains(factoryName))
        {
            throw new IllegalArgumentException("factoryName '" + factoryName + "'");
        }
    }

    private static ClassLoader getClassLoader()
    {
        try
        {
            ClassLoader classLoader = null;
            if (System.getSecurityManager() != null)
            {
                classLoader = (ClassLoader) AccessController.doPrivileged(new java.security.PrivilegedExceptionAction()
                {
                    public Object run()
                    {
                        return Thread.currentThread().getContextClassLoader();
                    }
                });
            }
            else
            {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            
            if (classLoader == null)
            {
                throw new FacesException("web application class loader cannot be identified", null);
            }
            return classLoader;
        }
        catch (Exception e)
        {
            throw new FacesException("web application class loader cannot be identified", e);
        }
    }
}
